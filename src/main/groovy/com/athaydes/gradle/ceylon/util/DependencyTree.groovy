package com.athaydes.gradle.ceylon.util

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency

/**
 * Tree of dependencies. The root of the tree will contain all project dependencies.
 */
@CompileStatic
class DependencyTree {

    private final Collection<Map> imports

    final String moduleName
    final String moduleVersion
    final Collection<ResolvedDependency> jarDependencies
    final Collection<Project> ceylonDependencies

    DependencyTree( Project project, Map moduleDeclaration ) {
        this.imports = moduleDeclaration.imports.findAll { Map imp -> imp.namespace == 'maven' }
        this.moduleName = moduleDeclaration.moduleName
        this.moduleVersion = moduleDeclaration.version

        jarDependencies = collectDependenciesOf project
        ceylonDependencies = directCeylonDependenciesOf project
    }

    private Collection<ResolvedDependency> collectDependenciesOf( Project project ) {
        def depsById = [ : ] as Map<String, ResolvedDependency>
        for ( dependency in directJarDependenciesOf( project ) ) {
            collectDependencies( dependency, depsById )
        }
        imports.each {
            def id = "${it.name}:${it.version}".toString()
            if ( depsById.containsKey( id ) ) {
                it.resolvedDependency = depsById[ id ]
            }
        }
        onlyJars depsById.values()
    }

    private static void collectDependencies( ResolvedDependency dependency,
                                             Map<String, ResolvedDependency> depById ) {
        depById[ dependency.name ] = dependency
        for ( child in dependency.children ) collectDependencies( child, depById )
    }

    static Collection<ResolvedDependency> transitiveDependenciesOf( ResolvedDependency dependency ) {
        def depsById = [ : ] as Map<String, ResolvedDependency>
        for ( child in dependency.children ) collectDependencies( child, depsById )
        onlyJars depsById.values()
    }

    Collection<ResolvedDependency> getModuleDeclaredDependencies() {
        onlyJars imports.findAll { it.resolvedDependency }.collect { it.resolvedDependency }
    }

    boolean isShared( ResolvedDependency dependency ) {
        def module = moduleDeclaration( dependency )
        return module?.shared
    }

    private Map moduleDeclaration( ResolvedDependency dependency ) {
        imports.find {
            it.name == "${dependency.moduleGroup}:${dependency.moduleName}" &&
                    it.version == dependency.moduleVersion
        }
    }

    static Collection<ResolvedDependency> directJarDependenciesOf( Project project ) {
        onlyJars directDependenciesOf( project )
    }

    static Collection<Project> directCeylonDependenciesOf( Project project ) {
        if ( !project.configurations.findByName( 'ceylonRuntime' ) ) return [ ]
        final deps = project.configurations.getByName( 'ceylonRuntime' )
                .allDependencies.withType( ProjectDependency )
        deps.collect { ProjectDependency p -> p.dependencyProject } as Collection<Project>
    }

    static Collection<ResolvedDependency> directDependenciesOf( Project project ) {
        if ( !project.configurations.findByName( 'ceylonRuntime' ) ) return [ ]
        project.configurations.getByName( 'ceylonRuntime' )
                .resolvedConfiguration.firstLevelModuleDependencies.collectEntries {
            [ it.name, it ]
        }.values()
    }

    static Collection<ResolvedDependency> directDependenciesOf( ResolvedDependency dependency ) {
        onlyJars dependency.children.collectEntries {
            [ it.name, it ]
        }.values()
    }

    private static Collection<ResolvedDependency> onlyJars(
            Collection dependencies ) {
        dependencies.findAll { dep ->
            dep instanceof ResolvedDependency &&
                    ( dep as ResolvedDependency ).moduleArtifacts
                            .any { ResolvedArtifact artifact -> artifact.type == 'jar' }
        } as Collection<ResolvedDependency>
    }

}
