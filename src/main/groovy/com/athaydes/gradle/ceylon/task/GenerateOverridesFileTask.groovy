package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.DependencyTree
import groovy.xml.MarkupBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class GenerateOverridesFileTask {

    static final Logger log = Logging.getLogger( GenerateOverridesFileTask )

    static void run( Project project, CeylonConfig config ) {
        generateOverridesFile( project, project.file( config.overrides ) )
    }

    private static void generateOverridesFile( Project project, File overridesFile ) {
        if ( overridesFile.exists() ) overridesFile.delete()
        overridesFile.parentFile.mkdirs()

        if ( !overridesFile.parentFile.directory ) {
            throw new GradleException( "Directory of overrides.xml file does not exist " +
                    "and could not be created. Check access rights to this location: " +
                    "${overridesFile.parentFile.absolutePath}" )
        }

        log.info( "Generating Ceylon overrides.xml file at {}", overridesFile )

        def dependencyTree = ResolveCeylonDependenciesTask.dependencyTreeOf( project )

        checkForProblems dependencyTree

        writeOverridesFile overridesFile, dependencyTree
    }

    private static writeOverridesFile( File overridesFile, DependencyTree dependencyTree ) {
        overridesFile.withWriter { writer ->
            def xml = new MarkupBuilder( writer )

            xml.overrides {
                dependencyTree.resolvedDependencies.each { dep ->
                    def id = dep.selected.id
                    if ( id instanceof ModuleComponentIdentifier ) {
                        log.debug "Adding transitive dependencies for ${id.group}:${id.module}:${id.version}"
                        addTransitiveDependencies dep, xml, id
                    } else {
                        log.warn( "Dependency will be ignored as it is of a type not supported " +
                                "by the Ceylon plugin: $id TYPE: ${id?.class?.name}" )
                    }
                }
            }
        }
    }

    private static void checkForProblems( DependencyTree dependencyTree ) {
        def unresolvedTransDeps = ( DependencyTree
                .transitiveDependenciesOf( dependencyTree.resolvedDependencies )
                .findAll { it instanceof UnresolvedDependencyResult }
                as Set<UnresolvedDependencyResult> )

        def problems = dependencyTree.unresolvedDependencies + unresolvedTransDeps

        if ( problems ) {
            def problemDescription = problems.collect {
                "  * ${it.attempted.displayName} (${it.attemptedReason.description})"
            }.join( '\n' )
            log.error "Unable to resolve the following dependencies:\n" + problemDescription
            throw new GradleException( 'Module has unresolved dependencies' )
        }
    }

    protected static void addTransitiveDependencies(
            ResolvedDependencyResult dep,
            MarkupBuilder xml,
            ModuleComponentIdentifier id ) {
        def transitiveDeps = DependencyTree.transitiveDependenciesOf( dep )
        if ( transitiveDeps ) {
            xml.artifact( coordinatesOf( id ) ) {
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
                                " by the Ceylon plugin: ${transId.displayName}"
                    }
                }
            }
        }
    }

    protected static Map coordinatesOf( ModuleComponentIdentifier id, boolean shared = false ) {
        def result = [ groupId: id.group, artifactId: id.module, version: id.version ]
        if ( shared ) result.shared = true
        result
    }

}
