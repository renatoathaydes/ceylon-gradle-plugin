package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier

class GenerateOverridesFileTask {

    static final Logger log = Logging.getLogger( GenerateOverridesFileTask )

    static void run( Project project, CeylonConfig config ) {
        generateOverridesFile( project, project.file( config.overrides ) )
    }

    private static void generateOverridesFile( Project project, File overridesFile ) {
        if ( overridesFile.exists() ) overridesFile.delete()
        overridesFile.parentFile.mkdirs()

        log.info( "Generating Ceylon overrides.xml file at {}", overridesFile )

        def dependencies = ResolveCeylonDependenciesTask.allCompileDeps( project )

        dependencies.each { dep ->
            def depId = dep.id
            if ( depId instanceof ModuleComponentArtifactIdentifier ) {
                def id = depId.componentIdentifier
                println "Dep name: ${id.group}.${id.module}.${id.version}"
            } else {
                log.warn( "Dependency will be ignored as it is of a type not supported " +
                        "by the Ceylon plugin: $depId TYPE: ${depId?.class?.name}" )
            }
        }

        overridesFile.withWriter { writer ->
            writer.println( '<overrides>' )
            writer.println( '</overrides>' )
        }
    }

}
