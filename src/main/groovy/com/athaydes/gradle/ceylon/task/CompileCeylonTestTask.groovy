package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonCommandOptions
import com.athaydes.gradle.ceylon.util.CeylonRunner
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Compiles ceylon tests.
 */
@CompileStatic
class CompileCeylonTestTask extends DefaultTask {

    static List inputFiles( Project project, CeylonConfig config ) {
        [ project.buildFile,
          GenerateOverridesFileTask.outputFiles( project, config ) ].flatten()
    }

    static File outputDir( Project project, CeylonConfig config ) {
        project.file( config.output )
    }

    CompileCeylonTestTask() {
        final config = project.extensions.getByType( CeylonConfig )
        final compilationDir = CompileCeylonTask.outputDir( project, config )

        // Gradle does not support giving a List of Directories as inputs with a @InputDirectory method,
        // so this workaround is needed
        ( config.testRoots + config.testResourceRoots + compilationDir ).each { dir -> inputs.dir( dir ) }
    }

    @InputFiles
    List getInputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        inputFiles( project, config )
    }

    @OutputDirectory
    File getOutputDir() {
        final config = project.extensions.getByType( CeylonConfig )
        outputDir( project, config )
    }

    @TaskAction
    void run() {
        final config = project.extensions.getByType( CeylonConfig )

        CeylonRunner.run 'compile', config.testModule, project, config,
                CeylonCommandOptions.getTestCompileOptions( project, config )
    }

}
