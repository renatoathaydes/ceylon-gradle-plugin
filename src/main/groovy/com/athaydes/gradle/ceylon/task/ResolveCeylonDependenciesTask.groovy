package com.athaydes.gradle.ceylon.task

import com.athaydes.gradle.ceylon.CeylonConfig
import com.athaydes.gradle.ceylon.parse.CeylonModuleParser
import com.athaydes.gradle.ceylon.util.DependencyTree
import groovy.transform.Memoized
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class ResolveCeylonDependenciesTask {

    static final Logger log = Logging.getLogger( ResolveCeylonDependenciesTask )

    static List inputs( Project project, CeylonConfig config ) {
        // lazily-evaluated elements
        [ { moduleFile( config, project ) }, { project.buildFile } ]
    }

    static List outputs() {
        [ ] // no outputs
    }

    static void run( Project project, CeylonConfig config ) {
        File module = moduleFile( config, project )
        log.info( "Parsing Ceylon module file at ${module.path}" )

        if ( !module.file ) {
            throw new GradleException( 'Ceylon module file does not exist.' +
                    ' Please make sure that you set "sourceRoot" and "modules"' +
                    ' correctly in the "ceylon" configuration.' )
        }

        def moduleDeclaration = parse module.path, module.text

        def mavenDependencies = moduleDeclaration.imports.findAll { it.name.contains( ':' ) }

        mavenDependencies.each { Map dependency ->
            addMavenDependency dependency, project
        }

        checkForProblems dependencyTreeOf( project )

        log.info( 'No dependency problems found!' )
    }

    private static File moduleFile( CeylonConfig config, Project project ) {
        def moduleNameParts = config.modules.split( /\./ ).toList()
        def modulePath = ( [ config.sourceRoot ] +
                moduleNameParts +
                [ 'module.ceylon' ] ).join( '/' )

        project.file( modulePath )
    }

    @Memoized
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

    private static void checkForProblems( DependencyTree dependencyTree ) {
        def unresolvedTransDeps = ( DependencyTree
                .transitiveDependenciesOf( dependencyTree.resolvedDependencies )
                .findAll { it instanceof UnresolvedDependencyResult }
                as Set<UnresolvedDependencyResult> )

        def problems = dependencyTree.unresolvedDependencies + unresolvedTransDeps

        if ( problems ) {
            def problemDescription = problems.collect {
                "  * ${it.attempted.displayName} (${it.attemptedReason.description})"
            }.join( '\n' )
            log.error "Unable to resolve the following dependencies:\n" + problemDescription
            throw new GradleException( 'Module has unresolved dependencies' )
        }
    }

}
