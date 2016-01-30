package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonRunner
import org.gradle.api.GradleException
import org.gradle.api.Nullable
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
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

    static emptyModuleDescriptor( Project project ) {
        def tmpDir = new File( project.buildDir, 'tmp' )
        tmpDir.mkdirs()
        new File( tmpDir, 'empty-module-descriptor.properties' )
    }

    static void run( Project project, CeylonConfig config ) {
        log.debug "Importing artifact jars"

        emptyModuleDescriptor( project ).createNewFile()

        createMavenSettingsFile project, config

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

    private static File createMavenSettingsFile( Project project, CeylonConfig config ) {
        def settingsFile = project.file( config.mavenSettings )

        // do not overwrite file if already there
        if ( settingsFile.exists() ) {
            log.debug( "Maven settings file already exists. Will not overwrite it." )
            return settingsFile
        }

        log.info( "Creating Maven settings file for Ceylon" )

        settingsFile << """\
            |<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
            |    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            |    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
            |                        http://maven.apache.org/xsd/settings-1.0.0.xsd">
            |    <localRepository>${project.file( config.output ).absolutePath}</localRepository>
            |    <offline>true</offline>
            |</settings>
            |""".stripMargin()
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

            assert expectedInstallation, "Could not define location of artifacts for $artifactId"

            if ( expectedInstallation.exists() ) {
                log.info( "Skipping installation of $artifact as it seems to be " +
                        "already installed at $expectedInstallation" )
                return
            }

            def id = artifactId.componentIdentifier
            def module = "${id.group}.${id.module}/${id.version}"

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

    private static void importJar( File jarFile, String ceylon, File repo, String module,
                                   Project project, List<ModuleComponentIdentifier> moduleDependencies = [ ] ) {
        def moduleDescriptor = writeModuleDescriptor project, moduleDependencies
        def command = "${ceylon} import-jar --descriptor=${moduleDescriptor} " +
                "--out=${repo.absolutePath} ${module} ${jarFile.absolutePath}"

        log.info "Running command: {}", command
        def process = command.execute( [ ], project.file( '.' ) )

        CeylonRunner.consumeOutputOf process
    }

    private static writeModuleDescriptor( Project project, List<ModuleComponentIdentifier> moduleDependencies ) {
        if ( moduleDependencies.empty ) emptyModuleDescriptor( project ).absolutePath
        else {
            def moduleDescriptor = new File( project.buildDir, "tmp/${project.name}-deps.properties" )
            moduleDescriptor.delete() // make sure the file is empty
            moduleDependencies.each { dep ->
                moduleDescriptor << "${dep.group}.${dep.module}=${dep.version}"
            }
            return moduleDescriptor.absolutePath
        }
    }

    private static void importProjectDependencies( String ceylon, Project project, File repo ) {
        allProjectDependenciesOf( project ).each { id ->
            def dependency = project.rootProject.allprojects.find {
                it.path == id.projectPath
            }
            def transitiveDeps = transitiveCompileDependenciesOf dependency
            def moduleDependencies = importConfigurationJars ceylon, project, repo, transitiveDeps
            importProjectDependency ceylon, project, repo, dependency, moduleDependencies
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
                                                 Project dependency,
                                                 List<ModuleComponentIdentifier> moduleDependencies = [ ] ) {
        log.info( 'Importing dependency: {}', dependency.name )

        def dependencyOutput = projectDependencyOutputDir( project, dependency )

        if ( dependencyOutput?.directory ) {
            log.info( "Copying output from {} to {}", dependencyOutput, repo )
            project.copy {
                into repo
                from dependencyOutput
            }
        }

        if ( dependency.hasProperty( 'jar' ) ) {
            importJarDependency ceylon, repo, project, dependency, moduleDependencies
        } else if ( !dependencyOutput ) {
            log.warn( "Dependency on {} cannot be satisfied because no Ceylon configuration or Jar archives have been found.\n" +
                    "Unable to import project dependency so the build may fail!\n" +
                    "Make sure to build all modules at the same time, or manually in the correct order.",
                    dependency.name )
        }
    }

    @Nullable
    private static ResolvedConfiguration transitiveCompileDependenciesOf( Project project ) {
        project.configurations.findByName( 'compile' )?.resolvedConfiguration
    }

    private static List<ModuleComponentIdentifier> importConfigurationJars(
            String ceylon, Project project, File repo,
            @Nullable ResolvedConfiguration resolvedConfiguration ) {
        if ( !resolvedConfiguration ) return [ ]
        resolvedConfiguration.resolvedArtifacts.collect { dep ->
            def id = dep.id.componentIdentifier
            if ( dep.type == 'jar' && id instanceof ModuleComponentIdentifier ) {
                def module = "${id.group}.${id.module}/${id.version}"
                log.info "Importing JAR transitive dependency: {}", module
                importJar dep.file, ceylon, repo, module, project
                return id
            } else {
                log.warn( "Unable to import dependency [{}]. Not a default JAR dependency.", id )
                return null
            }
        }.findAll { it != null }
    }

    private static void importJarDependency( String ceylon,
                                             File repo,
                                             Project project,
                                             Project dependency,
                                             List<ModuleComponentIdentifier> moduleDependencies ) {
        def archivePath = dependency.jar.archivePath as File
        if ( archivePath.exists() ) {
            log.debug( "Dependency archive found at {}", archivePath )
            def module = "${dependency.group}.${dependency.name}/${dependency.version}"
            log.info( "Importing Jar dependency: $module" )
            importJar archivePath, ceylon, repo, module, project, moduleDependencies
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
            log.warn( "Project {} depends on project [{}], but it cannot be located. " +
                    "Searched from root project [{}].", project.name, dependency.name, project.rootProject.name )
            return null
        }

        def ceylonConfig = dependency.extensions.findByName( 'ceylon' )

        if ( ceylonConfig instanceof CeylonConfig ) {
            return dependency.file( ceylonConfig.output )
        } else {
            log.debug( "Project [{}] has a dependency on [{}], which does not seem to be a Ceylon Project." +
                    " If it is a Java project, its archives will be imported into the Ceylon repository.",
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
            def location = "$groupPath/$name/$version/$group.${name}-${version}.$ext"
            return new File( repo, location )
        } else {
            return null
        }
    }

}
