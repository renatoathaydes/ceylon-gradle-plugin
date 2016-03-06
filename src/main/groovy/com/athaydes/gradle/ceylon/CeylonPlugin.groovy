package com.athaydes.gradle.ceylon

import com.athaydes.gradle.ceylon.task.CleanTask
import com.athaydes.gradle.ceylon.task.CompileCeylonTask
import com.athaydes.gradle.ceylon.task.CompileCeylonTestTask
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
                group: 'Build tasks',
                description: 'Resolves all legacy dependencies declared in the Ceylon' +
                        ' module file as well as directly in the Gradle build file.',
                'resolveCeylonDependencies' ) << {
            ResolveCeylonDependenciesTask.run( project, config )
        }

        resolveDepsTask.inputs.files ResolveCeylonDependenciesTask.inputs( project, config )

        Task generateOverridesFile = project.task(
                group: 'Build tasks',
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
                group: 'Build tasks',
                dependsOn: 'resolveCeylonDependencies',
                description: 'Creates Maven pom files for all transitive dependencies.\n' +
                        'The transitive dependencies are resolved by Gradle, then for each dependency, a pom ' +
                        'is created containing only its direct dependencies as reported by Gradle.\n' +
                        'This allows Ceylon to import jars without considering optional Maven dependencies, ' +
                        'for example, as Gradle does not resolve optional dependencies by default.',
                'createDependenciesPoms' ) << {
            CreateDependenciesPomsTask.run( project, config )
        }

        createDependenciesPomsTask.inputs.files( CreateDependenciesPomsTask.inputs( project, config ) )
        createDependenciesPomsTask.outputs.files( CreateDependenciesPomsTask.outputs( project, config ) )

        Task createModuleDescriptorsTask = project.task(
                group: 'Build tasks',
                dependsOn: 'resolveCeylonDependencies',
                description: 'Creates module descriptors (properties files) for all transitive dependencies.\n' +
                        'The transitive dependencies are resolved by Gradle, then for each dependency, a module ' +
                        'descriptor is created containing all its own transitive dependencies. The module descriptors ' +
                        'are used when, and if, the dependencies Jar files get imported into the Ceylon repository.',
                'createModuleDescriptors' ) << {
            CreateModuleDescriptorsTask.run( project, config )
        }

        createModuleDescriptorsTask.inputs.files( CreateModuleDescriptorsTask.inputs( project, config ) )
        createModuleDescriptorsTask.outputs.files( CreateModuleDescriptorsTask.outputs( project, config ) )

        Task createMavenRepoTask = project.task(
                group: 'Build tasks',
                dependsOn: 'createDependenciesPoms',
                description: 'Creates a local Maven repository containing all transitive dependencies.\n' +
                        'The repository uses the default Maven repository format and is used by all Ceylon commands ' +
                        'so that Ceylon is not required to search for any Maven dependency in a remote repository, ' +
                        'ensuring that Gradle is used as the main Maven dependency resolver.',
                'createMavenRepo' ) << {
            CreateMavenRepoTask.run( project, config )
        }

        project.rootProject.getTasksByName( 'jar', true ).each { jar ->
            createMavenRepoTask.dependsOn jar
        }
        createMavenRepoTask.inputs.files( CreateMavenRepoTask.inputs( project, config ) )
        createMavenRepoTask.outputs.files( CreateMavenRepoTask.outputs( project, config ) )

        Task importJarsTask = project.task(
                group: 'Build tasks',
                dependsOn: [ 'resolveCeylonDependencies', 'createMavenRepo', 'createModuleDescriptors' ],
                description: 'Import transitive Maven dependencies and copies the output from dependent Ceylon ' +
                        'projects into the local Ceylon repository .\n' +
                        'To enable importing Maven dependencies, the Ceylon config property "importJars" must ' +
                        'be set to true.',
                'importJars' ) << {
            ImportJarsTask.run( project, config )
        }

        importJarsTask.inputs.files( ImportJarsTask.inputs( project, config ) )
        importJarsTask.outputs.files( ImportJarsTask.outputs( project, config ) )

        Task compileTask = project.task(
                group: 'Build tasks',
                dependsOn: [ 'generateOverridesFile', 'importJars', 'createMavenRepo' ],
                description: 'Compiles Ceylon and Java source code and directly' +
                        ' produces module and source archives in a module repository.',
                'compileCeylon' ) << {
            CompileCeylonTask.run( project, config )
        }

        compileTask.inputs.files( CompileCeylonTask.inputs( project, config ) )
        compileTask.outputs.files( CompileCeylonTask.outputs( project, config ) )

        project.task(
                group: 'Verification tasks',
                dependsOn: 'compileCeylon',
                description: 'Runs a Ceylon module.',
                'runCeylon' ) << {
            RunCeylonTask.run( project, config )
        }

        Task compileTestTask = project.task(
                group: 'Build tasks',
                dependsOn: [ 'compileCeylon' ],
                description: 'Compiles Ceylon and Java test code and directly' +
                        ' produces module and source archives in a module repository.',
                'compileCeylonTest' ) << {
            CompileCeylonTestTask.run( project, config )
        }

        compileTestTask.inputs.files( CompileCeylonTestTask.inputs( project, config ) )
        compileTestTask.outputs.files( CompileCeylonTestTask.outputs( project, config ) )

        Task testTask = project.task(
                group: 'Verification tasks',
                dependsOn: [ 'compileCeylonTest' ],
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
