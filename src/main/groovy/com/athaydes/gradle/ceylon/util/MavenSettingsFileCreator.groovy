package com.athaydes.gradle.ceylon.util

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.task.CreateMavenRepoTask
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class MavenSettingsFileCreator {

    static final Logger log = Logging.getLogger( MavenSettingsFileCreator )

    static File mavenSettingsFile( Project project, CeylonConfig config ) {
        config.mavenSettings ?
                project.file( config.mavenSettings ) :
                new File( project.buildDir, 'maven-settings.xml' )
    }

    static File createMavenSettingsFile( Project project, CeylonConfig config ) {
        def settingsFile = mavenSettingsFile project, config

        // do not overwrite file if already there
        if ( settingsFile.exists() ) {
            log.debug( "Maven settings file already exists. Will not overwrite it." )
            return settingsFile
        }

        log.info( "Creating Maven settings file for Ceylon" )

        def mavenRepo = CreateMavenRepoTask.rootDir( project, config )

        settingsFile.parentFile.mkdirs()

        settingsFile << """\
            |<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
            |    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            |    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
            |                        http://maven.apache.org/xsd/settings-1.0.0.xsd">
            |    <localRepository>${mavenRepo.absolutePath}</localRepository>
            |    <offline>true</offline>
            |</settings>
            |""".stripMargin()
    }

}
