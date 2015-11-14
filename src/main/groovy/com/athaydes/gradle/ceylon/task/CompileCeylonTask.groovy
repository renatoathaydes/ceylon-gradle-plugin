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
        [ { ResolveCeylonDependenciesTask.moduleFile( project, config ).parentFile } ]
    }

    static List outputs( Project project, CeylonConfig config ) {
        [ { project.file( config.output ) } ]
    }

    static void runCeylon( Project project, CeylonConfig config ) {
        run 'run', project, config
    }

    static void compileCeylon( Project project, CeylonConfig config ) {
        run 'compile', project, config
    }

    private static void run( String ceylonDirective, Project project, CeylonConfig config ) {
        log.info "Executing ceylon '$ceylonDirective' in project ${project.name}"

        CeylonRunner.withCeylon( project, config ) { File ceylon ->
            def options = getOptions( ceylonDirective, project, config )

            def command = "${ceylon.absolutePath} ${ceylonDirective} ${options} ${config.module}"
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
}
