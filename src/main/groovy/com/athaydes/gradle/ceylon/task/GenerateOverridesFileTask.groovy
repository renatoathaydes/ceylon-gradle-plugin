package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.DependencyTree
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

@CompileStatic
class GenerateOverridesFileTask extends DefaultTask {

    static final Logger log = Logging.getLogger( GenerateOverridesFileTask )

    static List inputFiles( Project project, CeylonConfig config ) {
        // this task's inputs are  exactly the same as the resolve task
        ResolveCeylonDependenciesTask.inputFiles( project, config )
    }

    static List outputFiles( Project project, CeylonConfig config ) {
        [ overridesFile( project, config ) ]
    }

    @InputFiles
    List getInputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        inputFiles( project, config )
    }

    @OutputFiles
    List getOutputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        outputFiles( project, config )
    }

    @TaskAction
    void run() {
        final config = project.extensions.getByType( CeylonConfig )

        def overridesFile = overridesFile( project, config )
        def moduleExclusions = processedModuleExclusions config.moduleExclusions
        generateOverridesFile( project, overridesFile, moduleExclusions )
    }

    static File overridesFile( Project project, CeylonConfig config ) {
        config.overrides ?
                project.file( config.overrides ) :
                new File( project.buildDir, "overrides.xml" )
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

    @CompileDynamic
    private static writeOverridesFile( File overridesFile,
                                       DependencyTree dependencyTree,
                                       List<Map> moduleExclusions ) {
        overridesFile.withWriter { writer ->
            def xml = new MarkupBuilder( writer )

            xml.overrides {
                writeExclusions moduleExclusions, xml
                dependencyTree.moduleDeclaredDependencies.each { ResolvedDependency dep ->
                    def name = "${dep.moduleGroup}:${dep.moduleName}"
                    if ( moduleExclusions.find { Map exc -> exc.module == name } ) {
                        log.info "Skipping transitive dependencies of module {} because it is excluded", name
                    } else {
                        def shared = dependencyTree.isShared( dep )
                        def transitiveDeps = DependencyTree.transitiveDependenciesOf( dep )
                        if ( transitiveDeps ) {
                            writeDependency( dep, name, shared, transitiveDeps, xml )
                        }
                    }
                }
            }
        }
    }

    @CompileDynamic
    private static void writeDependency( ResolvedDependency dep, String name, boolean shared,
                                         Collection<ResolvedDependency> transitiveDeps,
                                         MarkupBuilder xml ) {
        def id = "$name/${dep.moduleVersion}"
        xml.artifact( coordinatesOf( dep, shared ) ) {
            log.info "Writing overrides with transitive dependencies for {} - shared? {}", id, shared
            for ( transitiveDep in transitiveDeps ) {
                add( coordinatesOf( transitiveDep, true ) )
            }
        }
    }

    @CompileDynamic
    protected static void writeExclusions( List<Map> moduleExclusions, MarkupBuilder xml ) {
        moduleExclusions.each { item ->
            xml.remove( item )
        }
    }

    protected static Map coordinatesOf( ResolvedDependency dep, boolean shared ) {
        def result = [
                groupId   : dep.moduleGroup,
                artifactId: dep.moduleName,
                version   : dep.moduleVersion ]
        if ( shared ) result.shared = true
        result
    }

}
