package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.DependencyTree
import com.athaydes.gradle.ceylon.util.MavenSettingsFileCreator
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency

class CreateMavenRepoTask {

    static inputs( Project project, CeylonConfig config ) {
        ResolveCeylonDependenciesTask.inputs( project, config )
    }

    static outputs( Project project, CeylonConfig config ) {
        { ->
            [ MavenSettingsFileCreator.mavenSettingsFile( project, config ),
              rootDir( project, config ) ]
        }
    }

    static void run( Project project, CeylonConfig config ) {
        def rootDir = rootDir project, config

        MavenSettingsFileCreator.createMavenSettingsFile project, config

        def dependencyTree = project.extensions
                .getByName( ResolveCeylonDependenciesTask.CEYLON_DEPENDENCIES ) as DependencyTree

        dependencyTree.jarDependencies.each { dependency ->
            def destinationDir = destinationFor dependency, rootDir
            copyDependency dependency, project, destinationDir
            copyPom dependency, project, destinationDir
        }
    }

    static File rootDir( Project project, CeylonConfig config ) {
        new File( project.buildDir, 'maven-repository' )
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

    private static void copyPom( ResolvedDependency dependency, Project project, File destinationDir ) {
        def pom = CreateDependenciesPomsTask.pomTempLocation( dependency, project )
        project.copy {
            from pom
            into destinationDir
        }
    }

}