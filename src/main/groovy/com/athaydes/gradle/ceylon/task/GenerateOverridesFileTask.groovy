package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class GenerateOverridesFileTask {

    static final Logger log = Logging.getLogger( GenerateOverridesFileTask )

    static void run( Project project, CeylonConfig config ) {
        def moduleNameParts = config.modules.split( /\./ ).toList()
        def modulePath = ( [ config.sourceRoot ] +
                moduleNameParts +
                [ 'module.ceylon' ] ).join( '/' )

        def module = project.file( modulePath )
        log.info( "Parsing Ceylon module file at ${module.path}" )

        if ( !module.file ) {
            throw new GradleException( 'Ceylon module file does not exist.' +
                    ' Please make sure that you set "sourceRoot" and "modules"' +
                    ' correctly in the "ceylon" configuration.' )
        }

        def dependencies = parse module.text

        generateOverridesFile( dependencies, project.file( config.overrides ) )
    }

    static Map parse( String moduleText ) {
        // TODO parse the module

        [ : ]
    }

    static void generateOverridesFile( Map dependencies, File overridesFile ) {
        if ( overridesFile.exists() ) overridesFile.delete()
        overridesFile.parentFile.mkdirs()

        overridesFile.withWriter { writer ->
            writer.println( '<overrides>' )
            dependencies.each { key, val ->

            }
            writer.println( '</overrides>' )
        }
    }

}
