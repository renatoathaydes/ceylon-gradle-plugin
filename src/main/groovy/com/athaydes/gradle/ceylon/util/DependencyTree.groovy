package com.athaydes.gradle.ceylon.util

import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult

/**
 * Tree of dependencies. The root of the tree will contain all project dependencies.
 */
class DependencyTree {

    private final ResolvedComponentResult node
    private final Collection<Map> imports

    DependencyTree( ResolvedComponentResult node, Collection<Map> imports ) {
        this.node = node
        this.imports = imports
    }

    Set<ResolvedDependencyResult> getResolvedDependencies() {
        node.dependencies.findAll {
            it instanceof ResolvedDependencyResult
        } as Set<ResolvedDependencyResult>
    }

    Set<UnresolvedDependencyResult> getUnresolvedDependencies() {
        node.dependencies.findAll {
            it instanceof UnresolvedDependencyResult
        } as Set<UnresolvedDependencyResult>
    }

    boolean isShared( String group, String name, String version ) {
        def dependency = imports.find {
            it.name == "$group:$name" && it.version == version
        }
        return dependency?.shared
    }

    static Set<DependencyResult> transitiveDependenciesOf(
            ResolvedDependencyResult dependencyResult ) {
        dependencyResult.selected.dependencies.collectMany { it ->
            [ it ] + ( it instanceof ResolvedDependencyResult ?
                    transitiveDependenciesOf( it ) : [ ] )
        }
    }

    static Set<DependencyResult> transitiveDependenciesOf(
            Collection<? extends ResolvedDependencyResult> dependencies ) {
        dependencies.collectMany { transitiveDependenciesOf( it ) }
    }

}
