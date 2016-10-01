package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonCommandOptions
import com.athaydes.gradle.ceylon.util.CeylonRunner
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

@CompileStatic
class TestCeylonTask extends DefaultTask {

    static List inputFiles( Project project, CeylonConfig config ) {
        [ project.buildFile ]
    }

    static List outputFiles( Project project, CeylonConfig config ) {
        [ ]
    }

    TestCeylonTask() {
        final config = project.extensions.getByType( CeylonConfig )
        final compilationDir = CompileCeylonTask.outputDir( project, config )
        final testCompilationDir = CompileCeylonTestTask.outputDir( project, config )

        // Gradle does not support giving a List of Directories as inputs with a @InputDirectory method,
        // so this workaround is needed
        [ compilationDir, testCompilationDir ].each { dir -> inputs.dir( dir ) }
    }

    @InputFiles
    List getInputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        inputFiles( project, config )
    }

    @OutputFiles
    List getOutputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        outputFiles( project, config )
    }

    @TaskAction
    void run() {
        final config = project.extensions.getByType( CeylonConfig )

        CeylonRunner.run 'test', config.testModule, project, config,
                CeylonCommandOptions.getTestOptions( project, config )
    }
}
