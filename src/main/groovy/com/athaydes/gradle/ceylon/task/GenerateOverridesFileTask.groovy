package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.DependencyTree
import groovy.xml.MarkupBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class GenerateOverridesFileTask {

    static final Logger log = Logging.getLogger( GenerateOverridesFileTask )

    static List inputs( Project project, CeylonConfig config ) {
        // this task's inputs are  exactly the same as the resolve task
        ResolveCeylonDependenciesTask.inputs( project, config )
    }

    static List outputs( Project project, CeylonConfig config ) {
        // lazily-evaluated elements
        [ { project.file( config.overrides ) } ]
    }

    static void run( Project project, CeylonConfig config ) {
        def overridesFile = project.file( config.overrides )
        def moduleExclusions = processedModuleExclusions config.moduleExclusions
        generateOverridesFile( project, overridesFile, moduleExclusions )
    }

    static List<Map> processedModuleExclusions( List moduleExclusions ) {
        moduleExclusions.collect { item ->
            switch ( item ) {
                case String: return [ module: item ]
                case Map: return item
                default: return [ module: item?.toString() ]
            }
        } as List<Map>
    }

    private static void generateOverridesFile( Project project,
                                               File overridesFile,
                                               List<Map> moduleExclusions ) {
        if ( overridesFile.exists() ) overridesFile.delete()
        overridesFile.parentFile.mkdirs()

        if ( !overridesFile.parentFile.directory ) {
            throw new GradleException( "Directory of overrides.xml file does not exist " +
                    "and could not be created. Check access rights to this location: " +
                    "${overridesFile.parentFile.absolutePath}" )
        }

        log.info( "Generating Ceylon overrides.xml file at {}", overridesFile )

        def dependencyTree = project.extensions
                .getByName( ResolveCeylonDependenciesTask.CEYLON_DEPENDENCIES ) as DependencyTree

        writeOverridesFile overridesFile, dependencyTree, moduleExclusions
    }

    private static writeOverridesFile( File overridesFile,
                                       DependencyTree dependencyTree,
                                       List<Map> moduleExclusions ) {
        overridesFile.withWriter { writer ->
            def xml = new MarkupBuilder( writer )

            xml.overrides {
                writeExclusions moduleExclusions, xml
                dependencyTree.resolvedDependencies.each { dep ->
                    def id = dep.selected.id
                    if ( id instanceof ModuleComponentIdentifier ) {
                        def name = "${id.group}:${id.module}"
                        if ( moduleExclusions.find { it.module == name } ) {
                            log.info "Skipping transitive dependencies of module $name because it is excluded"
                        } else {
                            def shared = dependencyTree.isShared( id )
                            log.info "Adding transitive dependencies for ${name}:${id.version}" +
                                    " - shared? $shared"
                            addTransitiveDependencies dep, xml, id, shared
                        }
                    } else if ( !( id instanceof ProjectComponentIdentifier ) ) {
                        log.warn( "Dependency will be ignored as it is of a type not supported " +
                                "by the Ceylon plugin: $id TYPE: ${id?.class?.name}" )
                    }
                }
            }
        }
    }

    protected static void writeExclusions( List<Map> moduleExclusions, MarkupBuilder xml ) {
        moduleExclusions.each { item ->
            xml.remove( item )
        }
    }

    protected static void addTransitiveDependencies(
            ResolvedDependencyResult dep,
            MarkupBuilder xml,
            ModuleComponentIdentifier id,
            boolean shared ) {
        def transitiveDeps = DependencyTree.transitiveDependenciesOf( dep )
        if ( transitiveDeps ) {
            xml.artifact( coordinatesOf( id, shared ) ) {
                for ( trans in transitiveDeps ) {
                    if ( trans instanceof ResolvedDependencyResult ) {
                        def transId = trans.selected.id
                        if ( transId instanceof ModuleComponentIdentifier ) {
                            xml.add( coordinatesOf( transId, true ) )
                        } else {
                            log.warn "Ignoring transitive dependency as its selected ID type is not supported" +
                                    " by the Ceylon plugin: ${transId.displayName}"
                        }
                    } else {
                        log.warn "Ignoring transitive dependency as its type is not supported" +
                                " by the Ceylon plugin: ${trans.requested.displayName}"
                    }
                }
            }
        }
    }

    protected static Map coordinatesOf( ModuleComponentIdentifier id, boolean shared ) {
        def result = [ groupId: id.group, artifactId: id.module, version: id.version ]
        if ( shared ) result.shared = true
        result
    }

}
