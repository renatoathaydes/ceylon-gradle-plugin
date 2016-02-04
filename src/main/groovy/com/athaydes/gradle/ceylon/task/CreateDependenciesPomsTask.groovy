package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.DependencyTree
import com.athaydes.gradle.ceylon.util.MavenPomCreator
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency

class CreateDependenciesPomsTask {

    static void run( Project project, CeylonConfig config ) {
        def dependencyTree = project.extensions
                .getByName( ResolveCeylonDependenciesTask.CEYLON_DEPENDENCIES ) as DependencyTree

        for ( dependency in dependencyTree.allDependencies ) {
            def pom = pomTempLocation dependency, project
            if ( !pom.parentFile.exists() ) {
                pom.parentFile.mkdirs()
            }
            pom.withWriter { writer ->
                MavenPomCreator.createJarFor( dependency, writer )
            }
        }
    }

    static File pomTempLocation( ResolvedDependency dependency, Project project ) {
        def destinationDir = new File( project.buildDir, 'dependency-poms' )
        new File( destinationDir, "${dependency.moduleName}-${dependency.moduleVersion}.pom" )
    }

}
