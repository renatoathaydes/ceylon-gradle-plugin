package com.athaydes.gradle.ceylon.util

import org.gradle.api.artifacts.ResolvedDependency

class ModuleDescriptorCreator {

    static void createModuleDescriptorFor( ResolvedDependency dependency, Writer writer ) {
        def dependencies = DependencyTree.transitiveDependenciesOf( dependency )

        if ( dependencies ) {
            for ( child in dependencies ) {
                def entry = "+${child.moduleGroup}\\:${child.moduleName}=${child.moduleVersion}"
                writer.write entry
                writer.write '\n'
            }
        } else {
            writer.write '' // create an empty file
        }
    }

}
