package com.athaydes.gradle.ceylon

import com.athaydes.gradle.ceylon.task.CleanTask
import com.athaydes.gradle.ceylon.task.CompileCeylonTask
import com.athaydes.gradle.ceylon.task.CreateDependenciesPomsTask
import com.athaydes.gradle.ceylon.task.CreateMavenRepoTask
import com.athaydes.gradle.ceylon.task.CreateModuleDescriptorsTask
import com.athaydes.gradle.ceylon.task.GenerateOverridesFileTask
import com.athaydes.gradle.ceylon.task.ImportJarsTask
import com.athaydes.gradle.ceylon.task.ResolveCeylonDependenciesTask
import com.athaydes.gradle.ceylon.task.RunCeylonTask
import com.athaydes.gradle.ceylon.task.TestCeylonTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Delete

class CeylonPlugin implements Plugin<Project> {

    static final Logger log = Logging.getLogger( CeylonPlugin )

    @Override
    void apply( Project project ) {
        log.debug( "CeylonPlugin being applied to project: ${project.name}" )

        CeylonConfig config = project.extensions
                .create( 'ceylon', CeylonConfig )

        createConfigs project, config
        createTasks project, config
    }

    private static createConfigs( Project project, CeylonConfig config ) {
        // there must be a default configuration or other projects cannot depend on this one
        project.configurations.maybeCreate( 'default' )

        def compileConfig = project.configurations.create 'ceylonCompile'
        project.configurations.create( 'ceylonRuntime' ).extendsFrom compileConfig
    }

    private static createTasks( Project project, CeylonConfig config ) {
        Task resolveDepsTask = project.task(
                description: 'Resolves all legacy dependencies declared in the Ceylon' +
                        ' module file as well as directly in the Gradle build file.',
                'resolveCeylonDependencies' ) << {
            ResolveCeylonDependenciesTask.run( project, config )
        }

        resolveDepsTask.inputs.files ResolveCeylonDependenciesTask.inputs( project, config )

        Task generateOverridesFile = project.task(
                dependsOn: 'resolveCeylonDependencies',
                description: 'Generates the overrides.xml file based on the Gradle project dependencies.\n' +
                        ' All Java legacy dependencies declared in the Ceylon module file are checked so' +
                        ' that if they require transitive dependencies, they are added to the auto-generated' +
                        ' overrides file.',
                'generateOverridesFile' ) << {
            GenerateOverridesFileTask.run( project, config )
        }

        generateOverridesFile.inputs.files GenerateOverridesFileTask.inputs( project, config )
        generateOverridesFile.outputs.files GenerateOverridesFileTask.outputs( project, config )

        Task createDependenciesPomsTask = project.task(
                dependsOn: 'resolveCeylonDependencies',
                description: '',
                'createDependenciesPoms' ) << {
            CreateDependenciesPomsTask.run( project, config )
        }

        createDependenciesPomsTask.inputs.files( CreateDependenciesPomsTask.inputs( project, config ) )
        createDependenciesPomsTask.outputs.files( CreateDependenciesPomsTask.outputs( project, config ) )

        Task createModuleDescriptorsTask = project.task(
                dependsOn: 'resolveCeylonDependencies',
                description: '',
                'createModuleDescriptors' ) << {
            CreateModuleDescriptorsTask.run( project, config )
        }

        createModuleDescriptorsTask.inputs.files( CreateModuleDescriptorsTask.inputs( project, config ) )
        createModuleDescriptorsTask.outputs.files( CreateModuleDescriptorsTask.outputs( project, config ) )

        Task createMavenRepoTask = project.task(
                dependsOn: 'createDependenciesPoms',
                description: '',
                'createMavenRepo' ) << {
            CreateMavenRepoTask.run( project, config )
        }

        project.rootProject.getTasksByName( 'jar', true ).each { jar ->
            createMavenRepoTask.dependsOn jar
        }
        createMavenRepoTask.inputs.files( CreateMavenRepoTask.inputs( project, config ) )
        createMavenRepoTask.outputs.files( CreateMavenRepoTask.outputs( project, config ) )

        Task importJarsTask = project.task(
                dependsOn: [ 'resolveCeylonDependencies', 'createMavenRepo', 'createModuleDescriptors' ],
                description: 'Import transitive dependencies into the Ceylon local repository if needed.',
                'importJars' ) << {
            ImportJarsTask.run( project, config )
        }

        importJarsTask.inputs.files( ImportJarsTask.inputs( project, config ) )
        importJarsTask.outputs.files( ImportJarsTask.outputs( project, config ) )

        Task compileTask = project.task(
                dependsOn: [ 'generateOverridesFile', 'importJars', 'createMavenRepo' ],
                description: 'Compiles Ceylon and Java source code and directly' +
                        ' produces module and source archives in a module repository.',
                'compileCeylon' ) << {
            CompileCeylonTask.run( project, config )
        }

        compileTask.inputs.files( CompileCeylonTask.inputs( project, config ) )
        compileTask.outputs.files( CompileCeylonTask.outputs( project, config ) )

        project.task(
                dependsOn: 'compileCeylon',
                description: 'Runs a Ceylon module.',
                'runCeylon' ) << {
            RunCeylonTask.run( project, config )
        }

        Task testTask = project.task(
                dependsOn: [ 'compileCeylon' ],
                description: 'Runs all tests in a Ceylon module.',
                'testCeylon' ) << {
            TestCeylonTask.run( project, config )
        }

        testTask.inputs.files( TestCeylonTask.inputs( project, config ) )
        testTask.outputs.files( TestCeylonTask.outputs( project, config ) )

        def cleanTask = project.task(
                type: Delete,
                description: 'Removes the output of all tasks of the Ceylon plugin.',
                'cleanCeylon' ) as Delete

        cleanTask.delete( CleanTask.inputs( project, config ) )

        project.tasks.withType( Delete ) { clean ->
            if ( clean != cleanTask ) {
                clean.dependsOn cleanTask
            }
        }

        project.getTasksByName( 'dependencies', false )*.dependsOn resolveDepsTask
    }

}
