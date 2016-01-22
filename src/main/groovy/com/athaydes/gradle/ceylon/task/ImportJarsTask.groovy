package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonRunner
import org.gradle.api.GradleException
import org.gradle.api.Nullable
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier

class ImportJarsTask {

    static final Logger log = Logging.getLogger( ImportJarsTask )

    static List inputs( Project project, CeylonConfig config ) {
        ResolveCeylonDependenciesTask.inputs( project, config )
    }

    static def outputs( Project project, CeylonConfig config ) {
        { ->
            allArtifactsIn( project ).collect { artifact ->
                artifactLocationInRepo( artifact, project.file( config.output ) )
            }.findAll { it != null }
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
            log.debug "Importing project dependencies files"
            importProjectDependencies( ceylon, project, repo )
        }
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
                importJar jarFile, ceylon, repo, module, project

                if ( !expectedInstallation.exists() ) {
                    log.warn( "Ceylon does not seem to have installed the artifact '${artifact.id}'" +
                            " in the expected location: $expectedInstallation" )
                }
            } else {
                throw new GradleException( "Dependency ${module} could not be installed in the Ceylon Repository" +
                        " because its jarFile could not be located: ${jarFile}" )
            }
        }
    }

    private static void importJar( File jarFile, String ceylon, File repo, String module, Project project ) {
        def command = "${ceylon} import-jar --force " +
                "--out=${repo.absolutePath} ${module} ${jarFile.absolutePath}"

        log.info "Running command: $command"
        def process = command.execute( [ ], project.file( '.' ) )

        CeylonRunner.consumeOutputOf process
    }

    private static void importProjectDependencies( String ceylon, Project project, File repo ) {
        allProjectDependenciesOf( project ).each { id ->
            importProjectDependency ceylon, project, repo, id
        }
    }

    private static List<ProjectComponentIdentifier> allProjectDependenciesOf( Project project ) {
        //noinspection GroovyAssignabilityCheck
        project.configurations.getByName( 'ceylonCompile' )
                .incoming.resolutionResult.root.dependencies.findAll {
            it instanceof ResolvedDependencyResult
        }.collectMany { ResolvedDependencyResult dependencyResult ->
            def id = dependencyResult.selected.id
            if ( id instanceof ProjectComponentIdentifier ) [ id ] else [ ]
        }.flatten() as List<ProjectComponentIdentifier>
    }

    private static void importProjectDependency( String ceylon,
                                                 Project project,
                                                 File repo,
                                                 ProjectComponentIdentifier id ) {
        log.info( 'Importing dependency: {}', id.displayName )

        def dependency = project.rootProject.allprojects.find {
            it.path == id.projectPath
        }

        def dependencyOutput = projectDependencyOutputDir( project, dependency )

        if ( dependencyOutput?.directory ) {
            log.info( "Copying output from {} to {}", dependencyOutput, repo )
            project.copy {
                into repo
                from dependencyOutput
            }
        }

        if ( dependency.hasProperty( 'jar' ) ) {
            importJarDependency ceylon, repo, project, dependency
        } else if ( !dependencyOutput ) {
            log.warn( "Dependency on {} cannot be satisfied because no Ceylon configuration or Jar archives have been found.\n" +
                    "Unable to import project dependency so the build may fail!\n" +
                    "Make sure to build all modules at the same time, or manually in the correct order.",
                    id.displayName )
        }
    }

    private static void importJarDependency( String ceylon,
                                             File repo,
                                             Project project,
                                             Project dependency ) {
        def archivePath = dependency.jar.archivePath as File
        if ( archivePath.exists() ) {
            log.info( "Dependency archive found at {}", archivePath )
            def module = "${dependency.group}:${dependency.name}/${dependency.version}"
            log.info( "Importing Jar dependency: $module" )
            importJar archivePath, ceylon, repo, module, project
        } else {
            log.warn( "Dependency ${dependency.name} has a JAR Archive configured, but the file does not exist.\n" +
                    "  * $archivePath\n" +
                    "Please make sure to build this archive before trying to build this project again!" )
        }
    }

    @Nullable
    private static File projectDependencyOutputDir( Project project,
                                                    Project dependency ) {
        if ( dependency == null ) {
            log.warn( "Project {} depends on project {}, but it cannot be located. " +
                    "Searched from root project {}", project.name, dependency.name, project.rootProject.name )
            return null
        }

        def dependencyProperties = dependency.extensions.properties

        def ceylonConfig = dependencyProperties.ceylon

        if ( ceylonConfig instanceof CeylonConfig ) {
            return dependency.file( ceylonConfig.output )
        } else {
            log.info( "Project {} has a dependency on {}, which does not seem to be a Ceylon Project.",
                    project.name, dependency.name )
            return null
        }
    }

    @Nullable
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
