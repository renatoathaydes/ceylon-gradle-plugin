package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.DependencyTree
import com.athaydes.gradle.ceylon.util.ModuleDescriptorCreator
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CreateModuleDescriptorsTask extends DefaultTask {

    static inputs( Project project, CeylonConfig config ) {
        ResolveCeylonDependenciesTask.inputs( project, config )
    }

    static outputs( Project project, CeylonConfig config ) {
        { ->
            [ rootDir( project ) ]
        }
    }

    @InputFiles
    def getInputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        inputs( project, config )
    }

    @OutputFile
    def getOutputFile() {
        final config = project.extensions.getByType( CeylonConfig )
        outputs( project, config )
    }

    @TaskAction
    void run() {
        def dependencyTree = project.extensions
                .getByName( ResolveCeylonDependenciesTask.CEYLON_DEPENDENCIES ) as DependencyTree

        for ( dependency in dependencyTree.jarDependencies ) {
            def descriptor = descriptorTempLocation dependency, project
            if ( !descriptor.parentFile.exists() ) {
                descriptor.parentFile.mkdirs()
            }
            descriptor.withWriter { writer ->
                ModuleDescriptorCreator.createModuleDescriptorFor dependency, writer
            }
        }
    }

    static File rootDir( Project project ) {
        new File( project.buildDir, 'module-descriptors' )
    }

    static File descriptorTempLocation( ResolvedDependency dependency, Project project ) {
        def destinationDir = rootDir project
        def fileName = "${dependency.moduleName}-${dependency.moduleVersion}.properties"
                .replace( '-', '_' )
        new File( destinationDir, fileName )
    }

}
