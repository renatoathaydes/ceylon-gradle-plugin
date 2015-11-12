package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.parse.CeylonModuleParser
import com.athaydes.gradle.ceylon.util.DependencyTree
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class ResolveCeylonDependenciesTask {

    static final Logger log = Logging.getLogger( ResolveCeylonDependenciesTask )

    static void run( Project project, CeylonConfig config ) {
        def moduleNameParts = config.modules.split( /\./ ).toList()
        def modulePath = ( [ config.sourceRoot ] +
                moduleNameParts +
                [ 'module.ceylon' ] ).join( '/' )

        def module = project.file( modulePath )
        log.info( "Parsing Ceylon module file at ${module.path}" )

        if ( !module.file ) {
            throw new GradleException( 'Ceylon module file does not exist.' +
                    ' Please make sure that you set "sourceRoot" and "modules"' +
                    ' correctly in the "ceylon" configuration.' )
        }

        def moduleDeclaration = parse module.path, module.text

        println "Detected the following module:\n$moduleDeclaration"
        def mavenDependencies = moduleDeclaration.imports.findAll { it.name.contains( ':' ) }
        println "Maven deps:\n$mavenDependencies"

        mavenDependencies.each { Map dependency ->
            addMavenDependency dependency, project
        }
    }

    static DependencyTree dependencyTreeOf( Project project ) {
        new DependencyTree( project.configurations.getByName( 'ceylonCompile' )
                .incoming.resolutionResult.root )
    }

    private static void addMavenDependency( Map dependency, Project project ) {
        project.dependencies.add( 'ceylonCompile', "${dependency.name}:${dependency.version}" )
    }

    private static Map parse( String name, String moduleText ) {
        new CeylonModuleParser().parse( name, moduleText )
    }

}
