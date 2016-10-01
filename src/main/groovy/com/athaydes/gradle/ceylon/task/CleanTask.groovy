package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.MavenSettingsFileCreator
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.InputFiles

@CompileStatic
class CleanTask extends Delete {

    static List inputFiles( Project project, CeylonConfig config ) {
        GenerateOverridesFileTask.outputFiles( project, config ) +
                CreateMavenRepoTask.outputFiles( project, config ) +
                MavenSettingsFileCreator.mavenSettingsFile( project, config )
    }

    CleanTask() {
        final config = project.extensions.getByType( CeylonConfig )

        // Gradle does not support giving a List of Directories as inputs with a @InputDirectory method,
        // so this workaround is needed
        [
                project.buildDir,
                CompileCeylonTask.outputDir( project, config ),
                CreateMavenRepoTask.outputDir( project, config ),
                CompileCeylonTestTask.outputDir( project, config ),
                CreateDependenciesPomsTask.outputDir( project, config ),
                CreateJavaRuntimeTask.outputDir( project, config ),
                CreateModuleDescriptorsTask.outputDir( project, config )
        ].each { File dir -> inputs.dir( dir ) }
    }

    @InputFiles
    List getInputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        inputFiles( project, config )
    }

}
