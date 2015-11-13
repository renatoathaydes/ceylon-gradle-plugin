package com.athaydes.gradle.ceylon.util

import com.athaydes.gradle.ceylon.CeylonConfig
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class CeylonRunner {

    static final Logger log = Logging.getLogger( CeylonRunner )

    static void withCeylon( Project project, CeylonConfig config, Closure<?> ceylonConsumer ) {
        def ceylonFile = project.file( config.ceylonLocation )
        if ( ceylonFile.exists() ) {
            log.debug "Running Ceylon executable: ${ceylonFile.absolutePath}"
            try {
                ceylonConsumer ceylonFile
            } catch ( GradleException e ) {
                throw e
            } catch ( e ) {
                throw new GradleException(
                        'Problem running the ceylon command. Run with --stacktrace for the cause.', e )
            }
        } else {
            throw new GradleException( 'Unable to locate Ceylon. Please set ceylonLocation in the ceylon configuration' )
        }
    }

    static void consumeOutputOf( Process process ) {
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
