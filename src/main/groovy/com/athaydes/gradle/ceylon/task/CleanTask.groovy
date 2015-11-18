package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import org.gradle.api.Project

class CleanTask {

    static List inputs( Project project, CeylonConfig config ) {
        GenerateOverridesFileTask.outputs( project, config ) +
                CompileCeylonTask.outputs( project, config )
    }

}
