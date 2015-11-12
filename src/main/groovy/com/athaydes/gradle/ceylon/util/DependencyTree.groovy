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

    DependencyTree( ResolvedComponentResult node ) {
        this.node = node
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
