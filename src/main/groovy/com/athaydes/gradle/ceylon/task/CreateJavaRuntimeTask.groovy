package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.util.CeylonCommandOptions
import com.athaydes.gradle.ceylon.util.CeylonRunner
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

/**
 * Task to create a Java runtime that can be run without Ceylon, using only the JVM.
 */
@CompileStatic
class CreateJavaRuntimeTask extends DefaultTask {

    static final Logger log = Logging.getLogger( CreateJavaRuntimeTask )

    private static def javaRuntimeDir( Project project, CeylonConfig config ) {
        config.javaRuntimeDestination ?: new File( project.buildDir, 'java-runtime' )
    }

    static List inputFiles( Project project, CeylonConfig config ) {
        [ project.buildFile,
          GenerateOverridesFileTask.outputFiles( project, config ),
          CreateMavenRepoTask.outputFiles( project, config ),
        ].flatten()
    }

    static File inputDir( Project project, CeylonConfig config ) {
        CompileCeylonTask.outputDir( project, config )
    }

    static File outputDir( Project project, CeylonConfig config ) {
        project.file( javaRuntimeDir( project, config ) )
    }

    @InputDirectory
    File getInputDir() {
        final config = project.extensions.getByType( CeylonConfig )
        inputDir( project, config )
    }

    @InputFiles
    List getInputFiles() {
        final config = project.extensions.getByType( CeylonConfig )
        inputFiles( project, config )
    }

    @OutputDirectory
    File getOutputDir() {
        final config = project.extensions.getByType( CeylonConfig )
        outputDir( project, config )
    }

    @TaskAction
    void run() {
        final config = project.extensions.getByType( CeylonConfig )

        def destination = project.file( javaRuntimeDir( project, config ) )

        log.debug( "Destination of JVM runtime: ${destination.absolutePath}" )

        destination.mkdirs()
        if ( !destination.directory ) {
            throw new GradleException( "Unable to create Java runtime as directory can't be created: ${destination}" )
        }

        CeylonRunner.withCeylon( config ) { String ceylon ->
            def options = CeylonCommandOptions.getCommonOptions( project, config, false )

            if ( project.hasProperty( 'get-ceylon-command' ) ) {
                def command = "${ceylon} classpath " +
                        "${options.collect { it.withQuotedArgument() }.join( ' ' )} ${config.module}"
                println command
            } else {
                def commandList = [ ceylon, 'classpath' ] +
                        options.collect { it.toString() } +
                        [ config.module ]

                log.info( "Running command: $commandList" )
                def process = commandList.execute( ( List ) null, project.file( '.' ) )

                ByteArrayOutputStream processConsumer = new ByteArrayOutputStream( 2048 )
                CeylonRunner.consumeOutputOf process, new PrintStream( processConsumer )

                def classpath = processConsumer.toString( 'UTF-8' )

                log.debug( "Got the classpath:\n$classpath" )

                def fileNames = moveFilesToDestination project, classpath, destination

                def main = findMain config

                createUnixScript( main, fileNames, destination )
                createWindowsScript( main, fileNames, destination )
            }
        }
    }

    private static String findMain( CeylonConfig config ) {
        def main = ( config.entryPoint ?: "${config.module}::run" ).toString()
        def startNameIndex = main.indexOf( '::' ) + 2
        if ( startNameIndex < 2 ) {
            throw new GradleException( "Invalid entry point: '$main'. Must be of form pkg.name::functionName or pkg.name::ClassName" )
        }

        if ( Character.isLowerCase( main[ startNameIndex ] as char ) ) {
            // ceylon function name ends with '_' in the JVM
            main += '_'
        }

        // turn main name into the equivalent JVM name
        return main.replace( '::', '.' )
    }

    @CompileDynamic
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

    private static void createUnixScript( String main,
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
                |"\$JAVA" -cp ${unixClasspath} ${main} "\$@"
                |""".stripMargin().replaceAll( Pattern.quote( '\r\n' ), '\n' )

        unixScriptFile.write unixScript, 'UTF-8'

        log.info( 'Saved Unix/Mac run script at {}', unixScriptFile.absolutePath )
    }

    private static void createWindowsScript( String main,
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
                |%JAVA% -cp ${windowsClasspath} ${main} %*
                |""".stripMargin().replaceAll( Pattern.quote( '\r\n' ), '\n' )

        windowsScriptFile.write windowsScript, 'UTF-8'

        log.info( 'Saved Windows run script at {}', windowsScriptFile.absolutePath )
    }

}
