package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class CompileCeylonTask {

    static final Logger log = Logging.getLogger( CompileCeylonTask )

    static void runCeylon( Project project, CeylonConfig config ) {
        run 'run', project, config
    }

    static void compileCeylon( Project project, CeylonConfig config ) {
        run 'compile', project, config
    }

    private static void run( String directive, Project project, CeylonConfig config ) {
        log.info "Executing ceylon '$directive' in project ${project.name}"
        def ceylonFile = project.file( config.ceylonLocation )
        if ( ceylonFile.exists() ) {
            log.debug "Running executable ceylon: ${config.ceylonLocation}"

            def options = ''
            def overrides = project.file( config.overrides )
            if ( overrides.exists() ) {
                options += " --overrides ${overrides.absolutePath}"
            }

            def command = "${ceylonFile.absolutePath} ${directive} ${options} ${config.modules}"
            log.info( "Running command: $command" )
            def process = command.execute( [ ], project.file( '.' ) )
            delegateProcessTo process
        } else {
            throw new GradleException( 'Unable to locate Ceylon. Please set ceylonLocation in the ceylon configuration' )
        }

    }

    private static void delegateProcessTo( Process process ) {
        consume process.in, System.out
        consume process.err, System.err

        def exitCode = -1
        try {
            exitCode = process.waitFor()
            log.debug "Ceylon process finished with code $exitCode"
        } catch ( e ) {
            log.warn "Ceylon process did not die gracefully. $e"
        }

        if ( exitCode != 0 ) {
            throw new GradleException( "Ceylon process exited with code $exitCode. " +
                    "See output for details." )
        }
    }

    private static void consume( InputStream stream, PrintStream writer ) {
        Thread.startDaemon {
            byte[] bytes = new byte[64]
            while ( true ) {
                def len = stream.read( bytes )
                if ( len > 0 ) writer.write bytes, 0, len
                else break
            }
        }
    }

}
