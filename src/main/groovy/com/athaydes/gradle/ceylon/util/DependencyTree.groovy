package com.athaydes.gradle.ceylon.util

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
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

    final String moduleName
    final String moduleVersion

    DependencyTree( ResolvedComponentResult node, Map moduleDeclaration ) {
        this.node = node
        this.imports = moduleDeclaration.imports.findAll { it.name.contains( ':' ) }
        this.moduleName = moduleDeclaration.moduleName
        this.moduleVersion = moduleDeclaration.version
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

    boolean isShared( ModuleComponentIdentifier id ) {
        def dependency = moduleDeclaration( id )
        return dependency?.shared
    }

    private Map moduleDeclaration( ModuleComponentIdentifier id ) {
        imports.find {
            it.name == "${id.group}:${id.module}" && it.version == id.version
        }
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
