package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.MavenSettingsFileCreator
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskAction

@CompileStatic
class CleanTask extends Delete {

    static List filesToDelete( Project project, CeylonConfig config ) {
        def tasks = { Class... types -> types.collect { Class type -> project.tasks.withType( type ) } }
        [ project.buildDir,
          MavenSettingsFileCreator.mavenSettingsFile( project, config ),
          project.files( tasks(
                  CompileCeylonTask, GenerateOverridesFileTask, CreateMavenRepoTask,
                  CompileCeylonTestTask, CreateDependenciesPomsTask,
                  CreateJavaRuntimeTask, CreateModuleDescriptorsTask ) ) ]
    }

    @TaskAction
    @Override
    void clean() {
        final config = project.extensions.getByType( CeylonConfig )
        delete filesToDelete( project, config )
        super.clean()
    }

}
