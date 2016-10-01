package com.athaydes.gradle.ceylon

import com.athaydes.gradle.ceylon.task.CleanTask
import com.athaydes.gradle.ceylon.task.CompileCeylonTask
import com.athaydes.gradle.ceylon.task.CompileCeylonTestTask
import com.athaydes.gradle.ceylon.task.CreateDependenciesPomsTask
import com.athaydes.gradle.ceylon.task.CreateJavaRuntimeTask
import com.athaydes.gradle.ceylon.task.CreateMavenRepoTask
import com.athaydes.gradle.ceylon.task.CreateModuleDescriptorsTask
import com.athaydes.gradle.ceylon.task.GenerateOverridesFileTask
import com.athaydes.gradle.ceylon.task.ImportJarsTask
import com.athaydes.gradle.ceylon.task.ResolveCeylonDependenciesTask
import com.athaydes.gradle.ceylon.task.RunCeylonTask
import com.athaydes.gradle.ceylon.task.TestCeylonTask
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Delete
import org.gradle.language.base.plugins.LanguageBasePlugin

@CompileStatic
class CeylonPlugin implements Plugin<Project> {

    static final Logger log = Logging.getLogger( CeylonPlugin )

    @Override
    void apply( Project project ) {
        try {
            project.pluginManager.apply( LanguageBasePlugin )
        } catch ( PluginApplicationException e ) {
            log.debug( "Failed to apply LanguageBasePlugin - it had already been applied", e )
        }

        log.debug( "CeylonPlugin being applied to project: ${project.name}" )

        final config = project.extensions
                .create( 'ceylon', CeylonConfig ) as CeylonConfig

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
                type: ResolveCeylonDependenciesTask,
                group: 'Build tasks',
                description: 'Resolves all legacy dependencies declared in the Ceylon' +
                        ' module file as well as directly in the Gradle build file.',
                'resolveCeylonDependencies' )

        project.task(
                type: GenerateOverridesFileTask,
                group: 'Build tasks',
                dependsOn: 'resolveCeylonDependencies',
                description: 'Generates the overrides.xml file based on the Gradle project dependencies.\n' +
                        ' All Java legacy dependencies declared in the Ceylon module file are checked so' +
                        ' that if they require transitive dependencies, they are added to the auto-generated' +
                        ' overrides file.',
                'generateOverridesFile' )

        project.task(
                type: CreateDependenciesPomsTask,
                group: 'Build tasks',
                dependsOn: 'resolveCeylonDependencies',
                description: 'Creates Maven pom files for all transitive dependencies.\n' +
                        'The transitive dependencies are resolved by Gradle, then for each dependency, a pom ' +
                        'is created containing only its direct dependencies as reported by Gradle.\n' +
                        'This allows Ceylon to import jars without considering optional Maven dependencies, ' +
                        'for example, as Gradle does not resolve optional dependencies by default.',
                'createDependenciesPoms' )

        project.task(
                type: CreateModuleDescriptorsTask,
                group: 'Build tasks',
                dependsOn: 'resolveCeylonDependencies',
                description: 'Creates module descriptors (properties files) for all transitive dependencies.\n' +
                        'The transitive dependencies are resolved by Gradle, then for each dependency, a module ' +
                        'descriptor is created containing all its own transitive dependencies. The module descriptors ' +
                        'are used when, and if, the dependencies Jar files get imported into the Ceylon repository.',
                'createModuleDescriptors' )

        Task createMavenRepoTask = project.task(
                type: CreateMavenRepoTask,
                group: 'Build tasks',
                dependsOn: 'createDependenciesPoms',
                description: 'Creates a local Maven repository containing all transitive dependencies.\n' +
                        'The repository uses the default Maven repository format and is used by all Ceylon commands ' +
                        'so that Ceylon is not required to search for any Maven dependency in a remote repository, ' +
                        'ensuring that Gradle is used as the main Maven dependency resolver.',
                'createMavenRepo' )

        project.rootProject.getTasksByName( 'jar', true ).each { jar ->
            createMavenRepoTask.dependsOn jar
        }

        project.task(
                type: ImportJarsTask,
                group: 'Build tasks',
                dependsOn: [ 'resolveCeylonDependencies', 'createMavenRepo', 'createModuleDescriptors' ],
                description: 'Import transitive Maven dependencies and copies the output from dependent Ceylon ' +
                        'projects into the local Ceylon repository .\n' +
                        'To enable importing Maven dependencies, the Ceylon config property "importJars" must ' +
                        'be set to true.',
                'importJars' )

        Task compileTask = project.task(
                type: CompileCeylonTask,
                group: 'Build tasks',
                dependsOn: [ 'generateOverridesFile', 'importJars', 'createMavenRepo' ],
                description: 'Compiles Ceylon and Java source code and directly' +
                        ' produces module and source archives in a module repository.',
                'compileCeylon' )

        project.task(
                type: RunCeylonTask,
                group: 'Verification tasks',
                dependsOn: 'compileCeylon',
                description: 'Runs a Ceylon module.',
                'runCeylon' )

        project.task(
                type: CompileCeylonTestTask,
                group: 'Build tasks',
                dependsOn: [ 'compileCeylon' ],
                description: 'Compiles Ceylon and Java test code and directly' +
                        ' produces module and source archives in a module repository.',
                'compileCeylonTest' )

        Task testTask = project.task(
                type: TestCeylonTask,
                group: 'Verification tasks',
                dependsOn: [ 'compileCeylonTest' ],
                description: 'Runs all tests in a Ceylon module.',
                'testCeylon' )

        project.task(
                type: CreateJavaRuntimeTask,
                group: 'Build tasks',
                dependsOn: [ 'compileCeylon' ],
                description: 'Creates a Java runtime containing the full classpath' +
                        ' as well as bash and bat scripts that can be used to invoke Java to run the Ceylon module' +
                        ' in a JVM without any Ceylon tool.',
                'createJavaRuntime' )

        def cleanTask = project.task(
                type: CleanTask,
                description: 'Removes the output of all tasks of the Ceylon plugin.',
                'cleanCeylon' )

        project.tasks.withType( Delete ) { Delete clean ->
            if ( clean != cleanTask ) {
                clean.dependsOn cleanTask
            }
        }

        project.getTasksByName( 'dependencies', false )*.dependsOn resolveDepsTask
        project.getTasksByName( 'assemble', false )*.dependsOn compileTask
        project.getTasksByName( 'check', false )*.dependsOn testTask
    }

}
