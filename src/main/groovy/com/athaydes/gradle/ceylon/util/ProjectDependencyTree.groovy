package com.athaydes.gradle.ceylon.util

import com.athaydes.gradle.ceylon.CeylonConfig
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency

@CompileStatic
class ProjectDependencyTree {

    final Project project
    final CeylonConfig config

    ProjectDependencyTree( Project project, CeylonConfig config ) {
        this.project = project
        this.config = config
    }

    Set<ResolvedDependency> allDependencies( String configuration ) {
        def resolvedConfiguration = project.configurations[ configuration ].resolvedConfiguration
        buildDependencyTreeRecursively resolvedConfiguration.firstLevelModuleDependencies, config
    }

    static Set<ResolvedDependency> buildDependencyTreeRecursively(
            Set<ResolvedDependency> currentLevel,
            CeylonConfig config,
            Set<ResolvedDependency> result = [ ] ) {
        for ( next in currentLevel ) {
            buildDependencyTreeRecursively next.children, config, result
            next
            result << next
        }
        result
    }

}
