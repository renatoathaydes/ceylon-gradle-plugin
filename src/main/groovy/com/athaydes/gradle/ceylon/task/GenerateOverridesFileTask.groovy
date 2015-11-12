package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.DependencyTree
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class GenerateOverridesFileTask {

    static final Logger log = Logging.getLogger( GenerateOverridesFileTask )

    static void run( Project project, CeylonConfig config ) {
        generateOverridesFile( project, project.file( config.overrides ) )
    }

    private static void generateOverridesFile( Project project, File overridesFile ) {
        if ( overridesFile.exists() ) overridesFile.delete()
        overridesFile.parentFile.mkdirs()

        if ( !overridesFile.parentFile.directory ) {
            throw new GradleException( "Directory of overrides.xml file does not exist " +
                    "and could not be created. Check access rights to this location: " +
                    "${overridesFile.parentFile.absolutePath}" )
        }

        log.info( "Generating Ceylon overrides.xml file at {}", overridesFile )

        def dependencyTree = ResolveCeylonDependenciesTask.dependencyTreeOf( project )

        def problems = dependencyTree.unresolvedDependencies +
                ( DependencyTree.transitiveDependenciesOf( dependencyTree.resolvedDependencies ).findAll {
                    it instanceof UnresolvedDependencyResult
                } as Set<UnresolvedDependencyResult> )

        if ( problems ) {
            def problemDescription = problems.collect {
                "  * ${it.attempted.displayName} (${it.attemptedReason.description})"
            }.join( '\n' )
            log.error "Unable to resolve the following dependencies:\n" + problemDescription
            throw new GradleException( 'Module has unresolved dependencies' )
        }

        dependencyTree.resolvedDependencies.each { dep ->
            def id = dep.selected.id
            if ( id instanceof ModuleComponentIdentifier ) {
                println "Dep name: ${id.group}:${id.module}:${id.version}"
                println "    ${DependencyTree.transitiveDependenciesOf( dep )}"
            } else {
                log.warn( "Dependency will be ignored as it is of a type not supported " +
                        "by the Ceylon plugin: $id TYPE: ${id?.class?.name}" )
            }
        }

        overridesFile.withWriter { writer ->
            writer.println( '<overrides>' )
            writer.println( '</overrides>' )
        }
    }

}
