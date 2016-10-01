package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonCommandOptions
import com.athaydes.gradle.ceylon.util.CeylonRunner
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction

@CompileStatic
class TestCeylonTask extends DefaultTask {

    static List inputFiles( Project project, CeylonConfig config ) {
        def tasks = { Class... types -> types.collect { Class type -> project.tasks.withType( type ) } }

        [ project.buildFile, project.files( tasks( CompileCeylonTask, CompileCeylonTestTask ) ) ]
    }

    static List outputDirectories( Project project, CeylonConfig config ) {
        if ( config.generateTestReport ) [ project.file( reportsDir( project, config ) ) ]
        else [ ]
    }

    @InputFiles
    List getInputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        inputFiles( project, config )
    }

    @OutputDirectories
    List getOutputDirs() {
        final config = project.extensions.getByType( CeylonConfig )
        outputDirectories( project, config )
    }

    @TaskAction
    void run() {
        final config = project.extensions.getByType( CeylonConfig )

        CeylonRunner.run 'test', config.testModule, project, config,
                CeylonCommandOptions.getTestOptions( project, config )

        if ( config.generateTestReport ) {
            moveTestReports( project, config )
        }
    }

    private void moveTestReports( Project project, CeylonConfig config ) {
        // Ceylon currently hard-codes the location of test reports to the reports/ directory
        def ceylonReportDir = project.file( 'reports' )

        if ( ceylonReportDir.isDirectory() ) {
            def destination = project.mkdir( reportsDir( project, config ) )
            if ( ceylonReportDir != destination ) {
                logger.info( "Moving Ceylon test reports from {} to {}", ceylonReportDir, destination )
                ceylonReportDir.renameTo destination
            } else {
                logger.debug( "Ceylon test reports are already in the expected location: {}", destination )
            }
        } else {
            logger.warn( 'Could not find the Ceylon test reports at the expected location: {}', ceylonReportDir )
        }
    }

    private static reportsDir( Project project, CeylonConfig config ) {
        config.testReportDestination ?: new File( project.buildDir, 'reports' )
    }
}
