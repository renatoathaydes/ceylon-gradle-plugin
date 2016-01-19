package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonRunner
import com.athaydes.gradle.ceylon.util.DependencyTree
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier

class ImportJarsTask {

    static final Logger log = Logging.getLogger( ImportJarsTask )

    static def inputs( Project project, CeylonConfig config ) {
        ResolveCeylonDependenciesTask.inputs( project, config )
    }

    static def outputs( Project project, CeylonConfig config ) {
        { ->
            allArtifactsIn( project ).collect { artifact ->
                artifactLocationInRepo( artifact, project.file( config.output ) )
            }
        }
    }

    static void run( Project project, CeylonConfig config ) {
        log.debug "Importing artifact jars"

        def repo = project.file( config.output )

        if ( !repo.directory && !repo.mkdirs() ) {
            throw new GradleException( "Output repository does not exist and cannot be created ${repo.absolutePath}." )
        }

        CeylonRunner.withCeylon( config ) { String ceylon ->
            allArtifactsIn( project ).each { artifact ->
                importDependency ceylon, repo, artifact, project
            }
        }

        log.debug "Importing project dependencies files"
        importProjectDependencies( project, repo )
    }

    private static Set<ResolvedArtifact> allArtifactsIn( Project project ) {
        project.configurations.ceylonCompile
                .resolvedConfiguration
                .resolvedArtifacts
    }

    private static void importDependency( String ceylon,
                                          File repo,
                                          ResolvedArtifact artifact,
                                          Project project ) {
        log.debug( "Will try to install ${artifact} into the Ceylon repository" )

        def jarFile = artifact.file
        def artifactId = artifact.id

        if ( artifactId instanceof ModuleComponentArtifactIdentifier ) {
            def expectedInstallation = artifactLocationInRepo( artifact, repo )

            if ( expectedInstallation.exists() ) {
                log.info( "Skipping installation of $artifact as it seems to be " +
                        "already installed at $expectedInstallation" )
                return
            }

            def id = artifactId.componentIdentifier
            def module = "${id.group}:${id.module}/${id.version}"

            if ( jarFile?.exists() ) {
                def command = "${ceylon} import-jar --force " +
                        "--out=${repo.absolutePath} ${module} ${jarFile.absolutePath}"

                log.info "Running command: $command"
                def process = command.execute( [ ], project.file( '.' ) )

                CeylonRunner.consumeOutputOf process

                if ( !expectedInstallation.exists() ) {
                    log.warn( "Ceylon does not seem to have installed the artifact '${artifact.id}'" +
                            " in the expected location: $expectedInstallation" )
                }
            } else {
                throw new GradleException( "Dependency ${module} could not be installed in the Ceylon Repository" +
                        " because its jarFile could not be located: ${jarFile}" )
            }
        } else {
            log.warn( "Artifact being ignored as it is not a module artifact: ${artifact}" )
        }
    }

    private static void importProjectDependencies( Project project, File repo ) {
        def dependencyTree = project.extensions
                .getByName( ResolveCeylonDependenciesTask.CEYLON_DEPENDENCIES ) as DependencyTree

        dependencyTree.resolvedDependencies.each { dep ->
            def id = dep.selected.id

            if ( id instanceof ProjectComponentIdentifier ) {
                importProjectDependency project, repo, id
            }
        }

    }

    private static void importProjectDependency( Project project,
                                                 File repo,
                                                 ProjectComponentIdentifier id ) {
        log.info( 'Importing dependency: {}', id.displayName )

        def dependency = project.rootProject.allprojects.find {
            it.path == id.projectPath
        }

        if ( dependency == null ) {
            log.warn( "Project {} depends on project {}, but it cannot be located. " +
                    "Searched from root project {}", project.name, dependency.name, project.rootProject.name )
            return
        }

        CeylonConfig dependencyModulesLocation

        try {
            dependencyModulesLocation = dependency.extensions.getByName( 'ceylon' ) as CeylonConfig
        } catch ( UnknownDomainObjectException e ) {
            log.info( "Project {} has a dependency on {}, which does not seem to be a Ceylon Project. " +
                    "Ignoring dependency.", project.name, dependency.name )
            return
        }

        def dependencyOutput = dependency.file( dependencyModulesLocation.output )

        if ( dependencyOutput.directory ) {
            log.info( "Copying output from {} to {}", dependencyOutput, repo )
            project.copy {
                into repo
                from dependencyOutput
            }
        } else {
            log.warn( "Dependency on project {} cannot be satisfies because its output dir does not exist: {}.\n" +
                    "Unable to import project dependency!\n" +
                    "Make sure to build all projects.",
                    dependency.name, dependencyOutput.absolutePath )
        }
    }

    private static File artifactLocationInRepo(
            ResolvedArtifact artifact, File repo ) {
        def artifactId = artifact.id
        if ( artifactId instanceof ModuleComponentArtifactIdentifier ) {
            def id = artifactId.componentIdentifier
            def group = id.group
            def groupPath = group.replace( '.', '/' )
            def name = id.module
            def version = id.version
            def ext = artifact.extension

            // use the default Ceylon pattern
            def location = "$groupPath:$name/$version/$group:${name}-${version}.$ext"
            return new File( repo, location )
        } else {
            return null
        }
    }

}
