package com.athaydes.gradle.ceylon.util

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency

/**
 * Tree of dependencies. The root of the tree will contain all project dependencies.
 */
class DependencyTree {

    private final Collection<Map> imports

    final String moduleName
    final String moduleVersion
    final Collection<ResolvedDependency> allDependencies

    DependencyTree( Project project, Map moduleDeclaration ) {
        this.imports = moduleDeclaration.imports.findAll { it.name.contains( ':' ) }
        this.moduleName = moduleDeclaration.moduleName
        this.moduleVersion = moduleDeclaration.version

        allDependencies = collectDependenciesOf project
    }

    private Collection<ResolvedDependency> collectDependenciesOf( Project project ) {
        def depsById = [ : ] as Map<String, ResolvedDependency>
        for ( dependency in directDependenciesOf( project ) ) {
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

    static Collection<ResolvedDependency> directDependenciesOf( Project project ) {
        onlyJars project.configurations.ceylonRuntime
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
            Collection<ResolvedDependency> dependencies ) {
        dependencies.findAll { ResolvedDependency dep ->
            dep.moduleArtifacts.any { it.type == 'jar' }
        }
    }

}
