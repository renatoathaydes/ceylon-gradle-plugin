package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonCommandOptions
import com.athaydes.gradle.ceylon.util.CeylonRunner
import org.gradle.api.Project

class TestCeylonTask {


    static List inputs( Project project, CeylonConfig config ) {
        [ project.buildFile, CompileCeylonTask.outputs( project, config ) ]
    }

    static List outputs( Project project, CeylonConfig config ) {
        [ ]
    }

    static void run( Project project, CeylonConfig config ) {
        if ( config.module != config.testModule ) {
            CeylonRunner.run 'compile', config.testModule, project, config,
                    CeylonCommandOptions.getCompileOptions( project, config )
        }

        CeylonRunner.run 'test', config.module, project, config,
                CeylonCommandOptions.getTestOptions( project, config )
    }
}
