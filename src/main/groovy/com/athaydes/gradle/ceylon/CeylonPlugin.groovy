package com.athaydes.gradle.ceylon

import com.athaydes.gradle.ceylon.task.CompileCeylonTask
import com.athaydes.gradle.ceylon.task.GenerateOverridesFileTask
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

        createTasks project, config
    }

    private static createTasks( Project project, CeylonConfig config ) {
        project.task(
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
            log.debug "Raw config: $config"
            applyDefaultsTo config
            log.debug "Effective config: $config"
            CompileCeylonTask.run( project, config )
        }
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
