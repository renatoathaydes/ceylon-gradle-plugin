package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.DependencyTree
import com.athaydes.gradle.ceylon.util.MavenSettingsFileCreator
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

@CompileStatic
class CreateMavenRepoTask extends DefaultTask {

    static List inputs( Project project, CeylonConfig config ) {
        ResolveCeylonDependenciesTask.inputFiles( project, config )
    }

    static List outputFiles( Project project, CeylonConfig config ) {
        [ MavenSettingsFileCreator.mavenSettingsFile( project, config ) ]
    }

    static File outputDir( Project project, CeylonConfig config ) {
        rootDir( project, config )
    }

    @InputFiles
    List getInputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        inputs( project, config )
    }

    @OutputDirectory
    File getOutDir() {
        final config = project.extensions.getByType( CeylonConfig )
        outputDir( project, config )
    }

    @OutputFiles
    List getOutputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        outputFiles( project, config )
    }

    @TaskAction
    void run() {
        final config = project.extensions.getByType( CeylonConfig )

        def rootDir = rootDir project, config

        MavenSettingsFileCreator.createMavenSettingsFile project, config

        def dependencyTree = project.extensions
                .getByName( ResolveCeylonDependenciesTask.CEYLON_DEPENDENCIES ) as DependencyTree

        dependencyTree.jarDependencies.each { ResolvedDependency dependency ->
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

    @CompileDynamic
    private static void copyDependency( ResolvedDependency dependency, Project project, File destinationDir ) {
        project.copy {
            from dependency.moduleArtifacts.collect { it.file }
            into destinationDir
        }
    }

    @CompileDynamic
    private static void copyPom( ResolvedDependency dependency, Project project, File destinationDir ) {
        def pom = CreateDependenciesPomsTask.pomTempLocation( dependency, project )
        project.copy {
            from pom
            into destinationDir
        }
    }

}