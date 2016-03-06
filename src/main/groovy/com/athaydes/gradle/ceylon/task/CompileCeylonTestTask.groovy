package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonCommandOptions
import com.athaydes.gradle.ceylon.util.CeylonRunner
import groovy.transform.CompileStatic
import org.gradle.api.Project

/**
 * Compiles ceylon tests.
 */
@CompileStatic
class CompileCeylonTestTask {

    static List inputs( Project project, CeylonConfig config ) {
        [ project.buildFile,
          { project.files( config.testRoots ) },
          { project.files( config.testResourceRoots ) },
          GenerateOverridesFileTask.outputs( project, config ) ].flatten()
    }

    static List outputs( Project project, CeylonConfig config ) {
        [ { project.file( config.output ) } ]
    }

    static void run( Project project, CeylonConfig config ) {
        CeylonRunner.run 'compile', config.testModule, project, config,
                CeylonCommandOptions.getTestCompileOptions( project, config )
    }

}
