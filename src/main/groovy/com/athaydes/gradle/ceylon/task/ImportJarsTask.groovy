package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonRunner
import com.athaydes.gradle.ceylon.util.ModuleDescriptorWriter
import com.athaydes.gradle.ceylon.util.ProjectDependencyTree
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import org.gradle.api.GradleException
import org.gradle.api.Nullable
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
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
            new ProjectDependencyTree( project, config ).allDependencies( 'ceylonRuntime' ).collect { dependency ->
                artifactLocationsInRepo( dependency, project.file( config.output ) )
            }.flatten()
        }
    }

    private final Project project
    private final CeylonConfig config
    private final ModuleDescriptorWriter moduleDescriptorWriter

    ImportJarsTask( Project project, CeylonConfig config ) {
        this.project = project
        this.config = config
        this.moduleDescriptorWriter = new ModuleDescriptorWriter( project, config )
    }

    void run() {
        log.info "Importing required jars into the local Ceylon Repository"

        moduleDescriptorWriter.writeBasicModuleDescriptor()
        createMavenSettingsFile()

        def repo = project.file( config.output )

        if ( !repo.directory && !repo.mkdirs() ) {
            throw new GradleException( "Output repository does not exist and cannot be created ${repo.absolutePath}." )
        }

        def dependencyTree = new ProjectDependencyTree( project, config )

        CeylonRunner.withCeylon( config ) { String ceylon ->
            Map<ResolvedDependency, Integer> toRetry = [ : ]
            def install = { ResolvedDependency dependency ->
                try {
                    importDependency ceylon, repo, dependency
                } catch ( GradleException ignored ) {
                    def retries = toRetry.computeIfAbsent( dependency, { 2 } )
                    toRetry[ dependency ] = retries - 1
                    log.warn( "Failed to import {}. Will retry {} times", dependency.name, retries )
                    if ( !retries ) {
                        throw new GradleException( "Unable to install ${dependency.name}" )
                    }
                }
            }

            dependencyTree.allDependencies( 'ceylonRuntime' ).each( install )

            while ( toRetry.values().any { it > 0 } ) {
                log.warn( "Attempting to install again" )
                toRetry.findAll { dep, retries -> retries > 0 }
                        .each { dep, retries -> install dep }
            }

            log.debug "Importing project dependencies files"
            importProjectDependencies ceylon, repo
        }
    }

    private File createMavenSettingsFile() {
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

    private void importDependency( String ceylon,
                                   File repo,
                                   ResolvedDependency dependency ) {
        if ( dependency.name.startsWith( 'java' ) ) {
            log.warn( "Skipping installation of dependency [{}]. JDK dependencies cannot be imported" +
                    " into the Ceylon repository", dependency )
            return
        }

        log.info( "Will try to install {} into the Ceylon repository", dependency )

        def expectedInstallations = artifactLocationsInRepo( dependency, repo )

        for ( artifact in jarArtifactsFor( dependency ) ) {

            def jarFile = artifact.artifact.file
            def artifactId = artifact.id

            def expectedInstallation = expectedInstallations.find { it.name.endsWith jarFile.name }

            assert expectedInstallation, "Could not define location of artifacts for $artifactId"

            if ( expectedInstallation.exists() ) {
                log.info( "Skipping installation of $artifact as it seems to be " +
                        "already installed at $expectedInstallation" )
                continue
            }

            def module = "${artifactId.group}.${artifactId.module}/${artifactId.version}"

            if ( jarFile?.exists() ) {
                def children = ProjectDependencyTree.buildDependencyTreeRecursively( dependency.children, config )
                        .collectMany { jarArtifactsFor( it ).collect { it.id } }
                importJar jarFile, ceylon, repo, module, children

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

    @CompileStatic
    private void importJar( File jarFile, String ceylon, File repo, String module,
                            List<ModuleComponentIdentifier> moduleDependencies ) {
        def moduleDescriptor = moduleDescriptorWriter.writeModuleDescriptor module, moduleDependencies
        def force = config.moduleImportOverrides.forceModuleImports ? ' --force' : ''
        def suggestions = config.moduleImportOverrides.showSuggestions ? ' --show-suggestions' : ''
        def command = "${ceylon} import-jar${force}${suggestions} --descriptor=${moduleDescriptor} " +
                "--out=${repo.absolutePath} ${module} ${jarFile.absolutePath}"

        log.info "Running command: {}", command
        def process = command.execute( [ ], project.file( '.' ) )

        CeylonRunner.consumeOutputOf process
    }

    private void importProjectDependencies( String ceylon,
                                            File repo ) {
        allProjectDependenciesOf( project ).each { id ->
            def dependency = project.rootProject.allprojects.find {
                it.path == id.projectPath
            }
            assert dependency, "Project dependency does not exist under the root project: $dependency"
            def transitiveDeps = new ProjectDependencyTree( dependency, config ).allDependencies( 'compile' )
            def moduleDependencies = importConfigurationJars ceylon, repo, transitiveDeps
            importProjectDependency ceylon, repo, dependency, moduleDependencies
        }
    }

    private static List<ProjectComponentIdentifier> allProjectDependenciesOf( Project project ) {
        project.configurations.getByName( 'ceylonCompile' )
                .incoming.resolutionResult.root.dependencies.findAll {
            it instanceof ResolvedDependencyResult
        }.collectMany { dependencyResult ->
            def id = ( dependencyResult as ResolvedDependencyResult ).selected.id
            if ( id instanceof ProjectComponentIdentifier ) [ id ] else [ ]
        } as List<ProjectComponentIdentifier>
    }

    private void importProjectDependency( String ceylon,
                                          File repo,
                                          Project dependency,
                                          List<ModuleComponentIdentifier> moduleDependencies ) {
        log.info( 'Importing dependency: {}', dependency.name )

        def dependencyOutput = projectDependencyOutputDir dependency

        if ( dependencyOutput?.directory ) {
            log.info( "Copying output from {} to {}", dependencyOutput, repo )
            project.copy {
                into repo
                from dependencyOutput
            }
        }

        if ( dependency.hasProperty( 'jar' ) ) {
            importJarDependency ceylon, repo, dependency, moduleDependencies
        } else if ( !dependencyOutput ) {
            log.warn( "Dependency on {} cannot be satisfied because no Ceylon configuration or Jar archives have been found.\n" +
                    "Unable to import project dependency so the build may fail!\n" +
                    "Make sure to build all modules at the same time, or manually in the correct order.",
                    dependency.name )
        }
    }

    @CompileStatic
    private static Set<ModuleArtifact> jarArtifactsFor(
            ResolvedDependency dependency ) {
        dependency.moduleArtifacts.collect { artifact ->
            def id = artifact.id.componentIdentifier

            if ( artifact.type == 'jar' && id instanceof ModuleComponentIdentifier ) {
                assert id instanceof ModuleComponentIdentifier
                new ModuleArtifact( id, artifact )
            } else {
                log.warn( "Unable to import dependency [{}]. Not a default JAR dependency.", id )
                null
            }
        }.findAll { it != null } as Set<ModuleArtifact>
    }

    private List<ModuleComponentIdentifier> importConfigurationJars(
            String ceylon, File repo,
            Set<ResolvedDependency> dependencies ) {
        dependencies.collectMany { dep ->
            def children = ProjectDependencyTree.buildDependencyTreeRecursively( dep.children, config )
                    .collectMany { jarArtifactsFor( it ).collect { it.id } }

            jarArtifactsFor( dep ).collect { moduleArtifact ->
                def artifact = moduleArtifact.artifact
                def id = moduleArtifact.id
                def module = "${id.group}.${id.module}/${id.version}"
                log.info "Importing JAR transitive dependency: {}", module
                importJar artifact.file, ceylon, repo, module, children
                return id
            }
        } as List<ModuleComponentIdentifier>
    }

    private void importJarDependency( String ceylon,
                                      File repo,
                                      Project dependency,
                                      List<ModuleComponentIdentifier> moduleDependencies ) {
        def archivePath = dependency.jar.archivePath as File
        if ( archivePath.exists() ) {
            log.debug( "Dependency archive found at {}", archivePath )
            def module = "${dependency.group}.${dependency.name}/${dependency.version}"
            log.info( "Importing Jar dependency: $module" )
            importJar archivePath, ceylon, repo, module, moduleDependencies
        } else {
            log.warn( "Dependency ${dependency.name} has a JAR Archive configured, but the file does not exist.\n" +
                    "  * $archivePath\n" +
                    "Please make sure to build this archive before trying to build this project again!" )
        }
    }

    @Nullable
    private File projectDependencyOutputDir( Project dependency ) {
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

    private static List<File> artifactLocationsInRepo(
            ResolvedDependency dependency, File repo ) {
        dependency.moduleArtifacts.collect { artifact ->
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
        }.findAll { it != null }
    }

    @EqualsAndHashCode( includes = [ 'id' ] )
    static class ModuleArtifact {
        final ModuleComponentIdentifier id
        final ResolvedArtifact artifact

        ModuleArtifact( ModuleComponentIdentifier id, ResolvedArtifact artifact ) {
            this.id = id
            this.artifact = artifact
        }
    }

}
