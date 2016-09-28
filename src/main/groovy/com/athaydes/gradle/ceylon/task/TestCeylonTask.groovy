package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonCommandOptions
import com.athaydes.gradle.ceylon.util.CeylonRunner
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

class TestCeylonTask extends DefaultTask {

    static List inputs( Project project, CeylonConfig config ) {
        [ project.buildFile,
          CompileCeylonTask.outputs( project, config ),
          CompileCeylonTestTask.outputs( project, config ) ]
    }

    static List outputs( Project project, CeylonConfig config ) {
        [ ]
    }

    @InputFiles
    def getInputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        inputs( project, config )
    }

    @OutputFiles
    def getOutputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        outputs( project, config )
    }

    @TaskAction
    void run() {
        final config = project.extensions.getByType( CeylonConfig )

        CeylonRunner.run 'test', config.testModule, project, config,
                CeylonCommandOptions.getTestOptions( project, config )
    }
}
