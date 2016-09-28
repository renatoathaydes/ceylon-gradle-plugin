package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonCommandOptions
import com.athaydes.gradle.ceylon.util.CeylonRunner
import com.athaydes.gradle.ceylon.util.DependencyTree
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Nullable
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

class ImportJarsTask extends DefaultTask {

    static final Logger log = Logging.getLogger( ImportJarsTask )

    static List inputs( Project project, CeylonConfig config ) {
        // TODO jars from other projects should be added here
        ResolveCeylonDependenciesTask.inputs( project, config )
    }

    static def outputs( Project project, CeylonConfig config ) {
        { -> ceylonRepo( project, config ) }
    }

    @InputFiles
    def getInputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        inputs( project, config )
    }

    @OutputFiles
    def getOutputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        outputs( project, config )
    }

    @TaskAction
    void run() {
        log.debug "Importing artifact jars"

        final config = project.extensions.getByType( CeylonConfig )

        def repo = ceylonRepo( project, config )

        if ( !repo.directory && !repo.mkdirs() ) {
            throw new GradleException( "Output repository does not exist and cannot be created ${repo.absolutePath}." )
        }

        def dependencyTree = project.extensions
                .getByName( ResolveCeylonDependenciesTask.CEYLON_DEPENDENCIES ) as DependencyTree

        if ( config.importJars ) {
            log.info "Importing Jar dependencies"
            for ( jarDependency in dependencyTree.jarDependencies ) {
                importDependency project, jarDependency, config
            }
        } else {
            log.info( "Skipping Jar imports" )
        }

        log.info "Importing Ceylon dependencies"
        for ( ceylonDependency in dependencyTree.ceylonDependencies ) {
            importCeylonProject project, repo, ceylonDependency
        }
    }

    private static File ceylonRepo( Project project, CeylonConfig config ) {
        project.file( config.output )
    }

    private static void importCeylonProject( Project project, File repo, Project dependency ) {
        log.info( 'Trying to import dependency {} as a Ceylon project', dependency.name )

        def dependencyOutput = projectDependencyOutputDir( dependency )

        if ( dependencyOutput?.directory ) {
            log.info( "Copying output from {} to {}", dependencyOutput, repo )
            project.copy {
                into repo
                from dependencyOutput
            }
        } else {
            log.info( "Dependency {} is not a Ceylon project", dependency.name )
        }
    }

    private static void importDependency( Project project,
                                          ResolvedDependency dependency,
                                          CeylonConfig config ) {
        def artifact = dependency.allModuleArtifacts.find { it.type == 'jar' }
        if ( artifact ) {
            if ( artifact.name == dependency.moduleName ) {
                importArtifact project, dependency, artifact, config
            } else {
                log.warn( "Unable to install dependency. Module name '{}' != Artifact name '{}",
                        dependency.moduleName, artifact.name )
            }
        } else {
            log.info( "Dependency {} will not be installed as it has no jar artifacts.", artifact.name )
        }
    }

    private static void importArtifact( Project project, ResolvedDependency dependency,
                                        ResolvedArtifact artifact, CeylonConfig config ) {
        log.info( "Will try to install {} into the Ceylon repository", artifact.name )
        def jarFile = artifact.file

        def module = "${dependency.moduleGroup}.${dependency.moduleName}/${dependency.moduleVersion}"

        if ( module.contains( '-' ) ) {
            log.warn( "Importing module with illegal character '-' in name: $module. Will replace '-' with '_'." )
            module = module.replace( '-', '_' )
        }

        def moduleDescriptor = CreateModuleDescriptorsTask.descriptorTempLocation( dependency, project )

        if ( jarFile?.exists() ) {
            importJar jarFile, module, project, moduleDescriptor, config
        } else {
            throw new GradleException( "Dependency ${module} could not be installed in the Ceylon Repository" +
                    " because its jarFile could not be located: ${jarFile}" )
        }
    }

    @Nullable
    private static File projectDependencyOutputDir( Project dependency ) {
        def ceylonConfig = dependency.extensions.findByName( 'ceylon' )

        if ( ceylonConfig instanceof CeylonConfig ) {
            return dependency.file( ceylonConfig.output )
        } else {
            return null
        }
    }

    private static void importJar( File jarFile, String module, Project project, File moduleDescriptor,
                                   CeylonConfig config ) {
        log.debug( "Jar: {}, Module Name: {}", jarFile.name, module )

        CeylonRunner.run( 'import-jar', module, project, config,
                CeylonCommandOptions.getImportJarsOptions( project, config, moduleDescriptor ),
                [ jarFile.absolutePath ] )
    }

}
