package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonCommandOptions
import com.athaydes.gradle.ceylon.util.CeylonRunner
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

@CompileStatic
class RunCeylonTask extends DefaultTask {

    @TaskAction
    void run() {
        final config = project.extensions.getByType( CeylonConfig )
        final List<String> finalArgs = [ ]

        if ( project.hasProperty( 'app-args' ) ) {
            finalArgs << project.property( 'app-args' )?.toString()
        }

        CeylonRunner.run 'run', config.module, project, config,
                CeylonCommandOptions.getRunOptions( project, config ), finalArgs
    }

}