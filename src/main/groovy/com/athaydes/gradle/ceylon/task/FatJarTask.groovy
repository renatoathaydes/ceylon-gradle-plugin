package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonCommandOptions
import com.athaydes.gradle.ceylon.util.CeylonRunner
import com.athaydes.gradle.ceylon.util.DependencyTree
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import static com.athaydes.gradle.ceylon.task.ResolveCeylonDependenciesTask.CEYLON_DEPENDENCIES

@CompileStatic
class FatJarTask extends DefaultTask {

    static List inputFiles( Project project ) {
        [ project.tasks.withType( CompileCeylonTask ) ]
    }

    static File outputJar( Project project, CeylonConfig config ) {
        def fatJarDestination = config.fatJarDestination ?: project.buildDir.absolutePath
        def dependencyTree = project.extensions.getByName( CEYLON_DEPENDENCIES ) as DependencyTree
        def jarPath = "${fatJarDestination}/$config.module-${dependencyTree.moduleVersion}.jar"
        return project.file( jarPath )
    }

    @InputFiles
    List getInputFiles() {
        inputFiles( project )
    }

    @OutputFile
    File getOutputFile() {
        final config = project.extensions.getByType( CeylonConfig )
        outputJar( project, config )
    }

    @TaskAction
    void run() {
        final config = project.extensions.getByType( CeylonConfig )

        CeylonRunner.run 'fat-jar', config.module, project, config,
                CeylonCommandOptions.getFatJarOptions( project, config )
    }

}
