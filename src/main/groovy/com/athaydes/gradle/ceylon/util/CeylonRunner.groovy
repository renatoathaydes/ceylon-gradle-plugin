package com.athaydes.gradle.ceylon.util

import com.athaydes.gradle.ceylon.CeylonConfig
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class CeylonRunner {

    static final Logger log = Logging.getLogger( CeylonRunner )

    static void withCeylon( CeylonConfig config, Closure<?> ceylonConsumer ) {
        String ceylon = CeylonToolLocator.findCeylon( config.ceylonLocation )
        log.debug "Running Ceylon executable: ${ceylon}"
        try {
            ceylonConsumer ceylon
        } catch ( GradleException e ) {
            throw e
        } catch ( e ) {
            throw new GradleException(
                    'Problem running the ceylon command. Run with --stacktrace for the cause.', e )
        }
    }

    static void run( String ceylonDirective, String module, Project project, CeylonConfig config,
                     List<String> options, List<String> finalArgs = [ ] ) {
        log.info "Executing ceylon '$ceylonDirective' in project ${project.name}"

        withCeylon( config ) { String ceylon ->
            def command = "${ceylon} ${ceylonDirective} ${options.join( ' ' )} ${module} ${finalArgs.join( ' ' )}"

            if ( project.hasProperty( 'get-ceylon-command' ) ) {
                println command
            } else {
                log.info( "Running command: $command" )
                def process = command.execute( ( List ) null, project.file( '.' ) )

                consumeOutputOf process

                log.debug( "Ceylon process completed." )
            }
        }
    }

    static void consumeOutputOf( Process process, PrintStream out = System.out, PrintStream err = System.err ) {
        consume process.in, out
        consume process.err, err

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
