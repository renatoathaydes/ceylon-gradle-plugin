package com.athaydes.gradle.ceylon.util

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.task.FatJarTask
import com.athaydes.gradle.ceylon.task.GenerateOverridesFileTask
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class CeylonCommandOptions {

    static final Logger log = Logging.getLogger( CeylonCommandOptions )

    static List<CommandOption> getCommonOptions( Project project, CeylonConfig config, boolean includeFlatClasspath = true ) {
        List<CommandOption> options = [ ]
        def overrides = GenerateOverridesFileTask.overridesFile( project, config )
        if ( overrides.exists() ) {
            options << CommandOption.of( '--overrides', overrides.absolutePath )
        } else {
            log.warn( 'The overrides.xml file could not be located: {}', overrides.absolutePath )
        }

        if ( includeFlatClasspath && config.flatClasspath ) {
            options << CommandOption.of( '--flat-classpath' )
        }

        return options + getRepositoryOptions( project, config )
    }

    private static List<CommandOption> getRepositoryOptions( Project project, CeylonConfig config ) {
        def mavenSettings = MavenSettingsFileCreator.mavenSettingsFile( project, config ).absolutePath
        [ CommandOption.of( '--rep', /aether:$mavenSettings/ ),
          CommandOption.of( '--rep', project.file( config.output ).absolutePath ) ]
    }

    private static File getOut( Project project, CeylonConfig config ) {
        project.file( config.output )
    }

    static List<CommandOption> getTestCompileOptions( Project project, CeylonConfig config ) {
        List<CommandOption> options = [ ]

        options << CommandOption.of( '--out', getOut( project, config ).absolutePath )

        config.testRoots.each { options << CommandOption.of( '--source', it?.toString() ) }
        config.testResourceRoots.each { options << CommandOption.of( '--resource', it?.toString() ) }

        return getCommonOptions( project, config ) + options
    }

    static List<CommandOption> getCompileOptions( Project project, CeylonConfig config ) {
        List<CommandOption> options = [ ]

        options << CommandOption.of( '--out', getOut( project, config ).absolutePath )

        config.sourceRoots.each { options << CommandOption.of( '--source', it?.toString() ) }
        config.resourceRoots.each { options << CommandOption.of( '--resource', it?.toString() ) }

        return getCommonOptions( project, config ) + options
    }

    static List<CommandOption> getFatJarOptions( Project project, CeylonConfig config ) {
        List<CommandOption> options = [ ]
        def out = FatJarTask.outputJar( project, config )
        log.info "Creating fat-jar at: '$out.absolutePath'"

        options << CommandOption.of( '--out', out.absolutePath )

        if ( config.entryPoint ) {
            options << CommandOption.of( '--run', config.entryPoint )
        }

        return getCommonOptions( project, config, false ) + options
    }

    static List<CommandOption> getRunOptions( Project project, CeylonConfig config ) {
        List<CommandOption> options = [ ]

        if ( config.entryPoint ) {
            options << CommandOption.of( '--run', config.entryPoint )
        }

        getCommonOptions( project, config ) + options
    }

    static List<CommandOption> getTestOptions( Project project, CeylonConfig config ) {
        List<CommandOption> options = [ ]

        if ( config.generateTestReport ) {
            options << CommandOption.of( '--report' )
        }

        getCommonOptions( project, config ) + options
    }

    static List<CommandOption> getImportJarsOptions( Project project, CeylonConfig config, File moduleDescriptor ) {
        List<CommandOption> options = [ ]
        if ( config.verbose ) {
            options << CommandOption.of( '--verbose' )
        }
        if ( config.forceImports ) {
            options << CommandOption.of( '--force' )
        }

        options <<
                CommandOption.of( '--descriptor', moduleDescriptor.absolutePath ) <<
                CommandOption.of( '--out', getOut( project, config ).absolutePath )

        return options + getRepositoryOptions( project, config )
    }

}
