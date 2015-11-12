package com.athaydes.gradle.ceylon

import com.athaydes.gradle.ceylon.task.CompileCeylonTask
import com.athaydes.gradle.ceylon.task.GenerateOverridesFileTask
import com.athaydes.gradle.ceylon.task.ResolveCeylonDependenciesTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class CeylonPlugin implements Plugin<Project> {

    static final Logger log = Logging.getLogger( CeylonPlugin )

    @Override
    void apply( Project project ) {
        CeylonConfig config = project.extensions
                .create( 'ceylon', CeylonConfig )

        createConfigs project, config
        createTasks project, config
    }

    private static createConfigs( Project project, CeylonConfig config ) {
        project.configurations.create 'ceylonCompile'
        project.configurations.create 'ceylonRuntime'
    }

    private static createTasks( Project project, CeylonConfig config ) {
        project.task(
                description: 'Resolves all legacy dependencies declared in the Ceylon' +
                        ' module file as well as directly in the Gradle build file.',
                'resolveCeylonDependencies' ) << {
            log.debug "Raw config: $config"
            applyDefaultsTo config
            log.info "Effective config: $config"

            ResolveCeylonDependenciesTask.run( project, config )
        }
        project.task(
                dependsOn: 'resolveCeylonDependencies',
                description: 'Generates the overrides.xml file based on the Gradle project dependencies.\n' +
                        ' All Java legacy dependencies declared in the Ceylon module file are checked so' +
                        ' that if they require transitive dependencies, they are added to the auto-generated' +
                        ' overrides file.',
                'generateOverridesFile' ) << {
            GenerateOverridesFileTask.run( project, config )
        }
        project.task(
                dependsOn: 'generateOverridesFile',
                description: 'Compiles Ceylon and Java source code and directly' +
                        ' produces module and source archives in a module repository.',
                'compileCeylon' ) << {
            CompileCeylonTask.compileCeylon( project, config )
        }
        project.task(
                dependsOn: 'compileCeylon',
                description: 'Runs a Ceylon module.',
                'runCeylon' ) << {
            CompileCeylonTask.runCeylon( project, config )
        }
        project.getTasksByName( 'dependencies', false )*.dependsOn( 'resolveCeylonDependencies' )
    }

    private static void applyDefaultsTo( CeylonConfig config ) {
        if ( !config.ceylonLocation ) config.ceylonLocation = '/usr/bin/ceylon'
        if ( !config.overrides ) config.overrides = 'auto-generated/overrides.xml'
        if ( config.flatClassPath == null ) config.flatClassPath = false
        if ( !config.sourceRoot ) config.sourceRoot = 'source'
        if ( !config.resourceRoot ) config.resourceRoot = 'resource'

        if ( !config.modules ) throw new GradleException( 'No Ceylon modules have been specified.' +
                ' Please set "modules" in the "ceylon" configuration' )
    }

}
