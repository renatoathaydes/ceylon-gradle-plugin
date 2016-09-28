package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.DependencyTree
import com.athaydes.gradle.ceylon.util.MavenPomCreator
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CreateDependenciesPomsTask extends DefaultTask {

    static inputs( Project project, CeylonConfig config ) {
        ResolveCeylonDependenciesTask.inputs( project, config )
    }

    static outputs( Project project, CeylonConfig config ) {
        { ->
            [ rootDir( project ) ]
        }
    }

    @InputFiles
    List getInputFiles() {
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
            def pom = pomTempLocation dependency, project
            if ( !pom.parentFile.exists() ) {
                pom.parentFile.mkdirs()
            }
            pom.withWriter { writer ->
                MavenPomCreator.createPomFor( dependency, writer )
            }
        }
    }

    static File rootDir( Project project ) {
        new File( project.buildDir, 'dependency-poms' )
    }

    static File pomTempLocation( ResolvedDependency dependency, Project project ) {
        def destinationDir = rootDir project
        new File( destinationDir, "${dependency.moduleName}-${dependency.moduleVersion}.pom" )
    }

}
