package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonCommandOptions
import com.athaydes.gradle.ceylon.util.CeylonRunner
import org.gradle.api.Project

class RunCeylonTask {

    static void run( Project project, CeylonConfig config ) {
        CeylonRunner.run 'run', config.module, project, config,
                CeylonCommandOptions.getRunOptions( project, config )
    }

}