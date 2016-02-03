package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.DependencyTree
import com.athaydes.gradle.ceylon.util.JarCreator
import org.gradle.api.Project

class CreateDependenciesPomsTask {

    static void run( Project project, CeylonConfig config ) {
        def dependencyTree = project.extensions
                .getByName( ResolveCeylonDependenciesTask.CEYLON_DEPENDENCIES ) as DependencyTree

        project.buildDir.mkdirs()

        for ( dependency in dependencyTree.allDependencies ) {
            def pom = new File( project.buildDir, dependency.moduleName + '.xml' )
            pom.withWriter { writer ->
                JarCreator.createJarFor( dependency, writer )
            }
        }
    }

}
