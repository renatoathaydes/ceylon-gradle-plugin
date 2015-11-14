package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class CleanTask {

    static final Logger log = Logging.getLogger( CleanTask )

    static List inputs( Project project, CeylonConfig config ) {
        def result = GenerateOverridesFileTask.outputs( project, config )

        log.info( "The clean task has the following inputs: $result" )

        return result
    }

}
