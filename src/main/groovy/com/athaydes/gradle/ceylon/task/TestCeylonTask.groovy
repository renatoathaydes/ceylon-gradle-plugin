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
        def tasks = { Class... types -> types.collect { Class type -> project.tasks.withType( type ) } }

        [ project.buildFile, project.files( tasks( CompileCeylonTask, CompileCeylonTestTask ) ) ]
    }

    static List outputFiles( Project project, CeylonConfig config ) {
        [ ]
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
