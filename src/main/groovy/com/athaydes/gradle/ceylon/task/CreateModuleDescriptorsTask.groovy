package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.DependencyTree
import com.athaydes.gradle.ceylon.util.ModuleDescriptorCreator
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency

class CreateModuleDescriptorsTask {
    static inputs( Project project, CeylonConfig config ) {
        ResolveCeylonDependenciesTask.inputs( project, config )
    }

    static outputs( Project project, CeylonConfig config ) {
        { ->
            [ rootDir( project ) ]
        }
    }

    static void run( Project project, CeylonConfig config ) {
        def dependencyTree = project.extensions
                .getByName( ResolveCeylonDependenciesTask.CEYLON_DEPENDENCIES ) as DependencyTree

        for ( dependency in dependencyTree.allDependencies ) {
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
        new File( destinationDir, "${dependency.moduleName}-${dependency.moduleVersion}.properties" )
    }

}
