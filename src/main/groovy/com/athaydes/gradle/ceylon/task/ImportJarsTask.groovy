package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonRunner
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier

class ImportJarsTask {

    static final Logger log = Logging.getLogger( ImportJarsTask )

    static List inputs( Project project, CeylonConfig config ) {
        ResolveCeylonDependenciesTask.inputs( project, config )
    }

    static List outputs( Project project, CeylonConfig config ) {
        [ { project.file( config.output ) } ]
    }

    static void run( Project project, CeylonConfig config ) {
        // run this task manually as Gradle wouldn't run it even when needed
        ResolveCeylonDependenciesTask.run( project, config )

        log.debug "Importing jars"

        def repo = project.file( config.output )

        if ( !repo.directory && !repo.mkdirs() ) {
            throw new GradleException( "Output repository does not exist and cannot be created ${repo.absolutePath}." )
        }

        CeylonRunner.withCeylon( project, config ) { File ceylon ->
            project.configurations.ceylonCompile
                    .resolvedConfiguration
                    .resolvedArtifacts.each { artifact ->
                importDependency ceylon, repo, artifact, project
            }
        }
    }

    static void importDependency( File ceylon,
                                  File repo,
                                  ResolvedArtifact artifact,
                                  Project project ) {
        log.debug( "Will try to install ${artifact} into the Ceylon repository" )

        def jarFile = artifact.file
        def artifactId = artifact.id

        if ( artifactId instanceof ModuleComponentArtifactIdentifier ) {
            def id = artifactId.componentIdentifier
            def module = "${id.group}:${id.module}/${id.version}"

            if ( jarFile?.exists() ) {
                def command = "${ceylon.absolutePath} import-jar --force " +
                        "--out=${repo.absolutePath} ${module} ${jarFile.absolutePath}"

                log.info "Running command: $command"
                def process = command.execute( [ ], project.file( '.' ) )

                CeylonRunner.consumeOutputOf process
            } else {
                throw new GradleException( "Dependency ${module} could not be installed in the Ceylon Repository" +
                        " because its jarFile could not be located: ${jarFile}" )
            }
        } else {
            log.warn( "Artifact being ignored as it is not a module artifact: ${artifact}" )
        }
    }

}
