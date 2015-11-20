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

    static def inputs( Project project, CeylonConfig config ) {
        // lazily-evaluated elements
        [ { moduleFile( project, config ) }, { project.buildFile } ]
    }

    static def run( Project project, CeylonConfig config ) {
        File module = moduleFile( project, config )
        log.info( "Parsing Ceylon module file at ${module.path}" )

        if ( !module.file ) {
            throw new GradleException( 'Ceylon module file does not exist.' +
                    ' Please make sure that you set "sourceRoot" and "module"' +
                    ' correctly in the "ceylon" configuration.' )
        }

        def moduleDeclaration = parse module.path, module.text

        def mavenDependencies = moduleDeclaration.imports.findAll { it.name.contains( ':' ) }

        mavenDependencies.each { Map dependency ->
            addMavenDependency dependency, project
        }

        project.configurations*.resolve()

        checkForProblems dependencyTreeOf( project )

        log.info( 'No dependency problems found!' )
    }

    static File moduleFile( Project project, CeylonConfig config ) {
        def moduleNameParts = config.module.split( /\./ ).toList()

        List locations = [ ]
        for ( root in config.sourceRoots ) {
            def modulePath = ( [ root ] +
                    moduleNameParts +
                    [ 'module.ceylon' ] ).join( '/' )

            locations << modulePath
            def module = project.file( modulePath )
            if ( module.exists() ) return module
        }

        throw new GradleException( "Module file cannot be located. " +
                "Looked at the following locations: $locations" )
    }

    @Memoized
    static DependencyTree dependencyTreeOf( Project project ) {
        new DependencyTree( project.configurations.getByName( 'ceylonCompile' )
                .incoming.resolutionResult.root )
    }

    private static void addMavenDependency( Map dependency, Project project ) {
        log.info "Adding dependency: ${dependency.name}:${dependency.version}"
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
