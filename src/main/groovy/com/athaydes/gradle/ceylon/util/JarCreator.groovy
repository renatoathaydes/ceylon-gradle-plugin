package com.athaydes.gradle.ceylon.util

import groovy.xml.MarkupBuilder
import org.gradle.api.artifacts.ResolvedDependency

class JarCreator {

    static void createJarFor( ResolvedDependency dependency, Writer writer ) {
        new MarkupBuilder( writer ).with { xml ->
            xml.mkp.xmlDeclaration( version: "1.0", encoding: "UTF-8" )
            xml.project(
                    'xsi:schemaLocation': 'http://www.w3.org/2001/XMLSchema-instance ' +
                            'http://maven.apache.org/xsd/maven-4.0.0.xsd',
                    'xmlns': 'http://maven.apache.org/POM/4.0.0' ) {
                modelVersion '4.0.0'
                groupId dependency.moduleGroup
                artifactId dependency.moduleName
                version dependency.moduleVersion

                def dependencies = DependencyTree.directDependenciesOf( dependency )

                if ( dependencies ) {
                    xml.dependencies {
                        writeDependencies xml, dependencies
                    }
                }

            }
        }
    }

    private static void writeDependencies( MarkupBuilder xml,
                                           Collection<ResolvedDependency> dependencies ) {
        for ( dependency in dependencies ) {
            xml.dependency {
                groupId dependency.moduleGroup
                artifactId dependency.moduleName
                version dependency.moduleVersion
            }
        }
    }

}
