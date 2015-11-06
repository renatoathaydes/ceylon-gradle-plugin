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

    static void run( Project project, CeylonConfig config ) {
        log.info "Compiling Ceylon code in project ${project.name}"
        def ceylonFile = project.file( config.ceylonLocation )
        if ( ceylonFile.exists() ) {
            log.debug "Running executable ceylon: ${config.ceylonLocation}"

            def options = ''
            def overrides = project.file( config.overrides )
            if ( overrides.exists() ) {
                options += " --overrides ${overrides.absolutePath}"
            }

            def command = "${ceylonFile.absolutePath} compile ${options} ${config.modules}"
            log.warn( "Running $command" )
            def process = command.execute( [ ], project.file( '.' ) )
            delegateProcessTo process
        } else {
            throw new GradleException( 'Unable to locate Ceylon. Please set ceylonLocation in the ceylon configuration' )
        }

    }

    private static void delegateProcessTo( Process process ) {
        consume process.in, System.out
        consume process.err, System.err

        try {
            def exitCode = process.waitFor()
            log.debug "Ceylon process finished with code $exitCode"
        } catch ( e ) {
            log.warn "Ceylon process did not die gracefully. $e"
        }
    }

    private static void consume( InputStream stream, PrintStream writer ) {
        // REALLY low-level code necessary here to fix issue #1 (no output in Windows)
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
