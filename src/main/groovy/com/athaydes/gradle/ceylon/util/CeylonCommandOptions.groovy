package com.athaydes.gradle.ceylon.util

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.task.GenerateOverridesFileTask
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static com.athaydes.gradle.ceylon.task.ResolveCeylonDependenciesTask.CEYLON_DEPENDENCIES

class CeylonCommandOptions {

    static final Logger log = Logging.getLogger( CeylonCommandOptions )

    static List getCommonOptions( Project project, CeylonConfig config ) {
        def options = [ ]
        def overrides = GenerateOverridesFileTask.overridesFile( project, config )
        if ( overrides.exists() ) {
            options << "--overrides ${overrides.absolutePath}"
        } else {
            log.warn( 'The overrides.xml file could not be located: {}', overrides.absolutePath )
        }

        if ( config.flatClasspath ) {
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

    private static File getFatJarOut( Project project, CeylonConfig config ) {
        def fatJarDestination = config.fatJarDestination ?: project.buildDir.absolutePath
        def dependencyTree = project.extensions.getByName( CEYLON_DEPENDENCIES ) as DependencyTree
        def jarPath = "${fatJarDestination}/$config.module-${dependencyTree.moduleVersion}.jar"
        return project.file( jarPath )
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
        def out = getFatJarOut( project, config )
        print "fat Jar output at: $out.absolutePath"
        options << "--out=${out.absolutePath}"

        return options
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
