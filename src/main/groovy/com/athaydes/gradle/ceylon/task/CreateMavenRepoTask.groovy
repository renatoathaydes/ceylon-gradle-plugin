package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.DependencyTree
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency

class CreateMavenRepoTask {

    static void run( Project project, CeylonConfig config ) {
        def rootDir = project.file( config.output )

        def dependencyTree = project.extensions
                .getByName( ResolveCeylonDependenciesTask.CEYLON_DEPENDENCIES ) as DependencyTree

        dependencyTree.allDependencies.each { dependency ->
            def destinationDir = destinationFor dependency, rootDir
            copyDependency dependency, project, destinationDir
            movePom dependency, project, destinationDir
        }
    }

    private static File destinationFor( ResolvedDependency dependency, File rootDir ) {
        def groupPath = dependency.moduleGroup.replace( '.', '/' )
        new File( rootDir, "$groupPath/" +
                "${dependency.moduleName}/" +
                "${dependency.moduleVersion}" )
    }

    private static void copyDependency( ResolvedDependency dependency, Project project, File destinationDir ) {
        project.copy {
            from dependency.moduleArtifacts.collect { it.file }
            into destinationDir
        }
    }

    private static void movePom( ResolvedDependency dependency, Project project, File destinationDir ) {
        def pom = CreateDependenciesPomsTask.pomTempLocation( dependency, project )
        pom.renameTo( new File( destinationDir, pom.name ) )
    }

}