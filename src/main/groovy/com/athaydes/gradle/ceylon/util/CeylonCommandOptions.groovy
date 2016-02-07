package com.athaydes.gradle.ceylon.util

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.task.GenerateOverridesFileTask
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class CeylonCommandOptions {

    static final Logger log = Logging.getLogger( CeylonCommandOptions )

    private static List getCommonOptions( Project project, CeylonConfig config ) {
        def options = [ ]
        def overrides = GenerateOverridesFileTask.overridesFile( project, config )
        if ( overrides.exists() ) {
            options << "--overrides ${overrides.absolutePath}"
        } else {
            log.warn( 'The overrides.xml file could not be located: {}', overrides.absolutePath )
        }

        if ( config.flatClassPath ) {
            options << '--flat-classpath'
        }

        options << "--rep=aether:${MavenSettingsFileCreator.mavenSettingsFile( project, config ).absolutePath}" <<
                "--rep=${project.file( config.output ).absolutePath}"

        return options
    }

    static List getCompileOptions( Project project, CeylonConfig config ) {
        def options = [ ]

        def output = project.file( config.output )
        options << "--out=${output.absolutePath}"

        config.sourceRoots.each { options << "--source $it" }
        config.resourceRoots.each { options << "--resource $it" }

        return getCommonOptions( project, config ) + options
    }

    static List getRunOptions( Project project, CeylonConfig config ) {
        // no specific run options yet
        getCommonOptions( project, config )
    }

    static List getTestOptions( Project project, CeylonConfig config ) {
        // no specific run options yet
        getCommonOptions( project, config )
    }

}
