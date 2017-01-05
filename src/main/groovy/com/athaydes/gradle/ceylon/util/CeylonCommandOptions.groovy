package com.athaydes.gradle.ceylon.util

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.task.FatJarTask
import com.athaydes.gradle.ceylon.task.GenerateOverridesFileTask
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class CeylonCommandOptions {

    static final Logger log = Logging.getLogger( CeylonCommandOptions )

    static List getCommonOptions( Project project, CeylonConfig config, boolean includeFlatClasspath = true ) {
        def options = [ ]
        def overrides = GenerateOverridesFileTask.overridesFile( project, config )
        if ( overrides.exists() ) {
            options << "--overrides ${overrides.absolutePath}"
        } else {
            log.warn( 'The overrides.xml file could not be located: {}', overrides.absolutePath )
        }

        if ( includeFlatClasspath && config.flatClasspath ) {
            options << '--flat-classpath'
        }

        return options + getRepositoryOptions( project, config )
    }

    private static List getRepositoryOptions( Project project, CeylonConfig config ) {
        [ "--rep=aether:${MavenSettingsFileCreator.mavenSettingsFile( project, config ).absolutePath}",
          "--rep=${project.file( config.output ).absolutePath}" ]
    }

    private static File getOut( Project project, CeylonConfig config ) {
        project.file( config.output )
    }

    static List getTestCompileOptions( Project project, CeylonConfig config ) {
        def options = [ ]

        options << "--out=${getOut( project, config ).absolutePath}"

        config.testRoots.each { options << "--source $it" }
        config.testResourceRoots.each { options << "--resource $it" }

        return getCommonOptions( project, config ) + options
    }

    static List getCompileOptions( Project project, CeylonConfig config ) {
        def options = [ ]

        options << "--out=${getOut( project, config ).absolutePath}"

        config.sourceRoots.each { options << "--source $it" }
        config.resourceRoots.each { options << "--resource $it" }

        return getCommonOptions( project, config ) + options
    }

    static List getFatJarOptions( Project project, CeylonConfig config ) {
        def options = [ ]
        def out = FatJarTask.outputJar( project, config )
        log.info "Creating fat-jar at: $out.absolutePath"

        options << "--out=${out.absolutePath}"

        if ( config.entryPoint ) {
            options << "--run=${config.entryPoint}"
        }

        return getCommonOptions( project, config, false ) + options
    }

    static List getRunOptions( Project project, CeylonConfig config ) {
        def options = [ ]

        if ( config.entryPoint ) {
            options << "--run=${config.entryPoint}"
        }

        getCommonOptions( project, config ) + options
    }

    static List getTestOptions( Project project, CeylonConfig config ) {
        def options = [ ]

        if ( config.generateTestReport ) {
            options << '--report'
        }

        getCommonOptions( project, config ) + options
    }

    static List getImportJarsOptions( Project project, CeylonConfig config, File moduleDescriptor ) {
        [ "${config.verbose ? '--verbose ' : ' '}",
          "${config.forceImports ? '--force ' : ' '}",
          "--descriptor=${moduleDescriptor.absolutePath} ",
          "--out=${getOut( project, config ).absolutePath}" ] +
                getRepositoryOptions( project, config )
    }

}
