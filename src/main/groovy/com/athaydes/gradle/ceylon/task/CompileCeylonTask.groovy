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

    static List inputs( Project project, CeylonConfig config ) {
        [ project.buildFile,
          { project.files( config.sourceRoots ) },
          { project.files( config.resourceRoots ) },
          GenerateOverridesFileTask.outputs( project, config ) ].flatten()
    }

    static List outputs( Project project, CeylonConfig config ) {
        [ { project.file( config.output ) } ]
    }

    static List testInputs( Project project, CeylonConfig config ) {
        [ project.buildFile, outputs( project, config ) ]
    }

    static void runCeylon( Project project, CeylonConfig config ) {
        run 'run', config.module, project, config
    }

    static void compileCeylon( Project project, CeylonConfig config ) {
        run 'compile', config.module, project, config
    }

    static void testCeylon( Project project, CeylonConfig config ) {
        run 'compile', config.testModule, project, config
        run 'test', config.testModule, project, config
    }

    private static void run( String ceylonDirective, String module, Project project, CeylonConfig config ) {
        log.info "Executing ceylon '$ceylonDirective' in project ${project.name}"

        CeylonRunner.withCeylon( config ) { String ceylon ->
            def options = getOptions( ceylonDirective, project, config )
            def command = "${ceylon} ${ceylonDirective} ${options} ${module}"
            log.info( "Running command: $command" )
            def process = command.execute( [ ], project.file( '.' ) )

            consumeOutputOf process

            log.debug( "Ceylon process completed." )
        }
    }

    private static String getOptions( String ceylonDirective, Project project, CeylonConfig config ) {
        def options = [ ]
        def overrides = project.file( config.overrides )
        if ( overrides.exists() ) {
            options << "--overrides ${overrides.absolutePath}"
        } else {
            log.warn( 'The overrides.xml file could not be located: {}', overrides.absolutePath )
        }

        if ( config.flatClassPath ) {
            options << '--flat-classpath'
        }

        switch ( ceylonDirective ) {
            case 'compile':
                options += compileOptions( project, config )
                break
            case 'run':
                options += runOptions( project, config )
                break
            case 'test':
                options += testOptions( project, config )
        }

        return options.join( ' ' )
    }

    static List compileOptions( Project project, CeylonConfig config ) {
        def options = [ ]

        def output = project.file( config.output )
        options << "--out ${output.absolutePath}"

        config.sourceRoots.each { options << "--source $it" }
        config.resourceRoots.each { options << "--resource $it" }
        return options
    }

    static List runOptions( Project project, CeylonConfig config ) {
        // no specific run options yet
        [ ]
    }

    static List testOptions( Project project, CeylonConfig config ) {
        // no specific run options yet
        [ ]
    }
}
