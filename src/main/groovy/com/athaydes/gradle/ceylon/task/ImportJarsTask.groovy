package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonRunner
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class ImportJarsTask {

    static final Logger log = Logging.getLogger( ImportJarsTask )

    static List inputs( Project project, CeylonConfig config ) {
        ResolveCeylonDependenciesTask.inputs( project, config )
    }

    static List outputs( Project project, CeylonConfig config ) {
        // no outputs
        [ ]
    }

    static void run( Project project, CeylonConfig config ) {
        // run this task manually as Gradle wouldn't run it even when needed
        ResolveCeylonDependenciesTask.run( project, config )

        log.debug "Importing jars"

        def dependencyTree = ResolveCeylonDependenciesTask.dependencyTreeOf( project )

        CeylonRunner.withCeylon( project, config ) { File ceylon ->
            dependencyTree.allResolvedTransitiveDependencies.each { dep ->
                importDependency ceylon, dep, project, config
            }
        }
    }

    static void importDependency( File ceylon,
                                  ResolvedDependencyResult dependency,
                                  Project project,
                                  CeylonConfig config ) {
        log.debug( "Will try to install ${dependency.selected} into the Ceylon repository" )

        def repo = project.file( config.output )

        if ( !repo.directory && !repo.mkdirs() ) {
            throw new GradleException( "Output repository does not exist and cannot be created ${repo.absolutePath}." )
        }

        final depId = dependency.selected.id
        if ( depId instanceof ModuleComponentIdentifier ) {
            def module = "${depId.group}:${depId.module}/${depId.version}"
            def jarFile = project.configurations.getByName( 'ceylonCompile' )
                    .resolvedConfiguration
                    .resolvedArtifacts.find {
                def id = it.id.componentIdentifier
                id instanceof ModuleComponentIdentifier &&
                        id.group == depId.group &&
                        id.module == depId.module &&
                        id.version == depId.version
            }?.file

            if ( jarFile?.exists() ) {
                def command = "${ceylon.absolutePath} import-jar --force " +
                        "--out=\"${repo.absolutePath}\" $module $jarFile"

                log.info "Running command: $command"
                def process = command.execute( [ ], project.file( '.' ) )

                CeylonRunner.consumeOutputOf process
            } else {
                throw new GradleException( "Dependency ${module} could not be installed in the Ceylon Repository" +
                        " because its jarFile could not be located: ${jarFile}" )
            }
        }
    }

}
