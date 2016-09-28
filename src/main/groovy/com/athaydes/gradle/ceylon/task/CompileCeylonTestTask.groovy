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

/**
 * Compiles ceylon tests.
 */
@CompileStatic
class CompileCeylonTestTask extends DefaultTask {

    static List inputs( Project project, CeylonConfig config ) {
        [ project.buildFile,
          { project.files( config.testRoots ) },
          { project.files( config.testResourceRoots ) },
          GenerateOverridesFileTask.outputs( project, config ) ].flatten()
    }

    static List outputs( Project project, CeylonConfig config ) {
        [ { project.file( config.output ) } ]
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

        CeylonRunner.run 'compile', config.testModule, project, config,
                CeylonCommandOptions.getTestCompileOptions( project, config )
    }

}
