package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonCommandOptions
import com.athaydes.gradle.ceylon.util.CeylonRunner
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.regex.Pattern

/**
 * Task to create a Java runtime that can be run without Ceylon, using only the JVM.
 */
class CreateJavaRuntimeTask {

    static final Logger log = Logging.getLogger( CreateJavaRuntimeTask )

    private static String javaRuntimeDir( Project project, CeylonConfig config ) {
        config.javaRuntimeDestination ?: new File( project.buildDir, 'java-runtime' ).absolutePath
    }

    static List inputs( Project project, CeylonConfig config ) {
        [ project.buildFile,
          GenerateOverridesFileTask.outputs( project, config ),
          CreateMavenRepoTask.outputs( project, config ),
          CompileCeylonTask.outputs( project, config )
        ].flatten()
    }

    static List outputs( Project project, CeylonConfig config ) {
        [ javaRuntimeDir( project, config ) ]
    }

    static void run( Project project, CeylonConfig config ) {
        def destination = new File( javaRuntimeDir( project, config ) )

        log.debug( "Destination of JVM runtime: ${destination.absolutePath}" )

        destination.mkdirs()
        if ( !destination.directory ) {
            throw new GradleException( "Unable to create Java runtime as directory can't be created: ${destination}" )
        }

        CeylonRunner.withCeylon( config ) { String ceylon ->
            def options = CeylonCommandOptions.getCommonOptions( project, config )

            // unnecessary option
            options.remove( '--flat-classpath' )

            def command = "${ceylon} classpath ${options.join( ' ' )} ${config.module}"

            if ( project.hasProperty( 'get-ceylon-command' ) ) {
                println command
            } else {
                log.info( "Running command: $command" )
                def process = command.execute( ( List ) null, project.file( '.' ) )

                ByteArrayOutputStream processConsumer = new ByteArrayOutputStream( 2048 )
                CeylonRunner.consumeOutputOf process, new PrintStream( processConsumer )

                def classpath = processConsumer.toString( 'UTF-8' )

                log.debug( "Got the classpath:\n$classpath" )

                def fileNames = moveFilesToDestination project, classpath, destination
                createUnixScript( config, fileNames, destination )
                createWindowsScript( config, fileNames, destination )
            }
        }
    }

    private static List<String> moveFilesToDestination( Project project, String classpath, File destination ) {
        def separator = System.getProperty( 'path.separator' )
        def paths = classpath.split( Pattern.quote( separator ) )
        def fileNames = [ ]
        for ( path in paths ) {
            def file = new File( path )
            fileNames << file.name
            if ( !file.exists() ) {
                throw new GradleException( "Expected file to be in classpath, but file does not exist: $file" )
            }

            log.debug( "Copying file '{}' to '{}'", file, destination )

            project.copy {
                from file
                into destination
            }
        }

        return fileNames
    }

    private static void createUnixScript( CeylonConfig config,
                                          List<String> fileNames,
                                          File destination ) {
        def unixScriptFile = new File( destination, 'run.sh' )
        def unixClasspath = fileNames.join( ':' )

        def unixScript = """|#!/bin/sh
                |
                |cd "\$( dirname "\${BASH_SOURCE[ 0 ]}" )"
                |
                |JAVA="java"
                |
                |# if JAVA_HOME exists, use it
                |if [ -x "\$JAVA_HOME/bin/java" ]
                |then
                |  JAVA="\$JAVA_HOME/bin/java"
                |else
                |  if [ -x "\$JAVA_HOME/jre/bin/java" ]
                |  then
                |    JAVA="\$JAVA_HOME/jre/bin/java"
                |  fi
                |fi
                |
                |"\$JAVA" -cp ${unixClasspath} ${config.module}.run_ "\$@"
                |""".stripMargin().replaceAll( Pattern.quote( '\r\n' ), '\n' )

        unixScriptFile.write unixScript, 'UTF-8'

        log.info( 'Saved Unix/Mac run script at {}', unixScriptFile.absolutePath )
    }

    private static void createWindowsScript( CeylonConfig config,
                                             List<String> fileNames,
                                             File destination ) {
        def windowsScriptFile = new File( destination, 'run.bat' )
        def windowsClasspath = fileNames.join( ';' )

        def windowsScript = """\
                |@ECHO OFF
                |
                |cd /d %~dp0
                |
                |set JAVA="java"
                |
                |REM if JAVA_HOME exists, use it
                |if exist "%JAVA_HOME%/bin/java" (
                |  set JAVA="%JAVA_HOME%/bin/java"
                |) else (
                |  if exist "%JAVA_HOME%/jre/bin/java" (
                |    set JAVA="%JAVA_HOME%/jre/bin/java"
                |  )
                |)
                |
                |%JAVA% -cp ${windowsClasspath} ${config.module}.run_ %*
                |""".stripMargin().replaceAll( Pattern.quote( '\r\n' ), '\n' )

        windowsScriptFile.write windowsScript, 'UTF-8'

        log.info( 'Saved Windows run script at {}', windowsScriptFile.absolutePath )
    }

}
