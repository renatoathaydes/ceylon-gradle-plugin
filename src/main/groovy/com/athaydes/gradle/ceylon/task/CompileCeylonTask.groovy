package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonCommandOptions
import com.athaydes.gradle.ceylon.util.CeylonRunner
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

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

    static void run( Project project, CeylonConfig config ) {
        CeylonRunner.run 'compile', config.module, project, config,
                CeylonCommandOptions.getCompileOptions( project, config )
    }

}
