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

@CompileStatic
class FatJarTask extends DefaultTask {

    static List inputFiles( Project project ) {
        [project.tasks.withType( CompileCeylonTask )]
    }

    static File outputDir( Project project, CeylonConfig config ) {
        project.file( config.output )
    }

    @InputFiles
    List getInputFiles() {
        inputFiles( project )
    }

    @OutputDirectory
    File getOutputDir() {
        final config = project.extensions.getByType( CeylonConfig )
        outputDir( project, config )
    }

    @TaskAction
    void run() {
        final config = project.extensions.getByType( CeylonConfig )

        CeylonRunner.run 'fat-jar', config.module, project, config,
                CeylonCommandOptions.getFatJarOptions( project, config )
    }

}
