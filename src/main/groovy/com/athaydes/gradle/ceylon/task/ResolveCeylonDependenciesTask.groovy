package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.parse.CeylonModuleParser
import com.athaydes.gradle.ceylon.util.DependencyTree
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

@CompileStatic
class ResolveCeylonDependenciesTask extends DefaultTask {

    static final Logger log = Logging.getLogger( ResolveCeylonDependenciesTask )
    public static final String CEYLON_DEPENDENCIES = 'CeylonDependencies'

    static List inputFiles( Project project, CeylonConfig config ) {
        [ moduleFile( project, config ), project.allprojects.collect { Project p -> p.buildFile } ].flatten()
    }

    @InputFiles
    List getFileInputs() {
        final config = project.extensions.getByType( CeylonConfig )
        inputFiles( project, config )
    }

    @TaskAction
    void run() {
        final config = project.extensions.getByType( CeylonConfig )
        File module = moduleFile( project, config )
        log.info( "Parsing Ceylon module file at ${module.path}" )

        if ( !module.file ) {
            throw new GradleException( 'Ceylon module file does not exist.' +
                    ' Please make sure that you set "sourceRoot" and "module"' +
                    ' correctly in the "ceylon" configuration.' )
        }

        def moduleDeclaration = parse module.path, module.text

        def mavenDependencies = moduleDeclaration.imports.findAll { Map imp -> imp.namespace == 'maven' } as List<Map>

        def existingDependencies = project.configurations.getByName( 'ceylonCompile' ).dependencies.collect {
            Dependency dep -> "${dep.group}:${dep.name}:${dep.version}"
        }

        log.debug "Project existing dependencies: {}", existingDependencies

        mavenDependencies.each { Map dependency ->
            if ( !existingDependencies.contains(
                    "${dependency.name}:${dependency.version}" ) ) {
                addMavenDependency dependency, project
            } else {
                log.info "Not adding transitive dependencies of module " +
                        "$dependency as it already existed in the project"
            }
        }

        project.configurations*.resolve()

        def dependencyTree = dependencyTreeOf( project, moduleDeclaration )

        log.info( 'No dependency problems found!' )

        project.extensions.add( CEYLON_DEPENDENCIES, dependencyTree )
    }

    static File moduleFile( Project project, CeylonConfig config ) {
        if ( !config.module ) {
            log.error( '''|The Ceylon module has not been specified.
                          |To specify the name of your Ceylon module, add a declaration like
                          |the following to your build.gradle file:
                          |
                          |ceylon {
                          |  module = 'name.of.ceylon.module'
                          |}
                          |
                          |If you prefer, you can set the default module in the Ceylon config file instead, 
                          |and that will be used by Gradle.'''.stripMargin() )

            throw new GradleException( "The Ceylon module must be specified" )
        }

        def moduleNameParts = config.module.split( /\./ ).toList()

        List locations = [ ]
        for ( root in config.sourceRoots ) {
            def rootPath = project.file( root ).absolutePath
            def modulePath = ( [ rootPath ] +
                    moduleNameParts +
                    [ 'module.ceylon' ] ).join( '/' )

            locations << modulePath
            def module = project.file( modulePath )
            if ( module.exists() ) return module
        }

        throw new GradleException( "Module file cannot be located. " +
                "Looked at the following locations: $locations" )
    }

    static DependencyTree dependencyTreeOf( Project project, Map moduleDeclaration ) {
        new DependencyTree( project, moduleDeclaration )
    }

    private static void addMavenDependency( Map dependency, Project project ) {
        log.info "Adding dependency: ${dependency.name}:${dependency.version}"
        project.dependencies.add( 'ceylonCompile', "${dependency.name}:${dependency.version}" )
    }

    private static Map parse( String name, String moduleText ) {
        new CeylonModuleParser().parse( name, moduleText )
    }

}
