package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonRunner
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static com.athaydes.gradle.ceylon.util.CeylonRunner.consumeOutputOf

@CompileStatic
class CompileCeylonTask {

    static final Logger log = Logging.getLogger( CompileCeylonTask )

    static void runCeylon( Project project, CeylonConfig config ) {
        run 'run', project, config
    }

    static void compileCeylon( Project project, CeylonConfig config ) {
        run 'compile', project, config
    }

    private static void run( String ceylonDirective, Project project, CeylonConfig config ) {
        log.info "Executing ceylon '$ceylonDirective' in project ${project.name}"

        CeylonRunner.withCeylon( project, config ) { File ceylon ->
            def options = ''
            def overrides = project.file( config.overrides )
            if ( overrides.exists() ) {
                options += " --overrides ${overrides.absolutePath}"
            }

            def command = "${ceylon.absolutePath} ${ceylonDirective} ${options} ${config.modules}"
            log.info( "Running command: $command" )
            def process = command.execute( [ ], project.file( '.' ) )

            consumeOutputOf process

            log.debug( "Ceylon process completed." )
        }
    }

}
