package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonRunner
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class ImportJarsTask {

    static final Logger log = Logging.getLogger( ImportJarsTask )

    static List inputs( Project project, CeylonConfig config ) {
        ResolveCeylonDependenciesTask.inputs( project, config )
    }

    static List outputs( Project project, CeylonConfig config ) {
        // no outputs
        [ ]
    }

    static void run( Project project, CeylonConfig config ) {
        // run this task manually as Gradle wouldn't run it even when needed
        ResolveCeylonDependenciesTask.run( project, config )

        log.debug "Importing jars"

        def dependencyTree = ResolveCeylonDependenciesTask.dependencyTreeOf( project )

        CeylonRunner.withCeylon( project, config ) { File ceylon ->
            dependencyTree.allResolvedTransitiveDependencies.each { dep ->
                importDependency ceylon, dep
            }
        }
    }

    static void importDependency( File ceylon, ResolvedDependencyResult dependency ) {
        log.warn( "Will try to install ${dependency.selected}" )
    }

}
