package com.athaydes.gradle.ceylon.util

import com.athaydes.gradle.ceylon.CeylonConfig
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class ModuleDescriptorWriter {

    static final Logger log = Logging.getLogger( ModuleDescriptorWriter )

    private final Project project
    private final CeylonConfig config
    private final File moduleDescriptorsDir
    private final Map<String, Set<String>> depsCache = [ : ]

    ModuleDescriptorWriter( Project project, CeylonConfig config ) {
        this.project = project
        this.config = config
        this.moduleDescriptorsDir = new File( project.buildDir, 'module-descriptors' )
    }

    void writeBasicModuleDescriptor() {
        def moduleDescriptor = basicModuleDescriptor()
        moduleDescriptor.delete() // start with the empty file
        def javaVersion = resolveJavaVersion()
        moduleDescriptor << "+java.base=$javaVersion\n"
    }

    String writeModuleDescriptor( String module,
                                  List<ModuleComponentIdentifier> moduleDependencies ) {
        def extraImports = ( config.moduleImportOverrides.extraImports.find { moduleRegex, v ->
            module.matches( moduleRegex )
        }?.value ?: [ ] ) as List<String>

        log.info( "Extra imports of '{}': {}", module, extraImports )

        if ( moduleDependencies.empty && !extraImports ) {
            return basicModuleDescriptor().absolutePath
        } else {
            def moduleName = module.replace( '/', ':' )
            def tmpFileName = moduleName + '-deps.properties'
            def excludes = config.moduleImportOverrides.excludeImports
            def moduleDescriptor = new File( moduleDescriptorsDir, tmpFileName )
            moduleDescriptor.delete()

            def dependenciesNames = depsCache.computeIfAbsent( moduleName, { _ ->
                log.info( "Computing dependencies of module {}", module )
                ( moduleDependencies.collectMany { dep ->
                    def name = "${dep.group}.${dep.module}:${dep.version}".toString() // force Java String
                    if ( excludes.any { name.matches( it ) } ) {
                        log.warn( "Will not add dependency on {} to module descriptor because it is excluded", name )
                        return [ ]
                    } else {
                        def allDeps = [ name ] + depsCache.get( name, [ ] as Set<String> )
                        log.debug( "Computed dependencies for {}: {}", module, allDeps )
                        return allDeps
                    }
                } as Set<String> ) + ( extraImports.collect { '$' + it } as Set<String> )// mark extra-imports with '$'
            } )

            moduleDescriptor.withWriter( 'UTF-8' ) { writer ->
                writer.write basicModuleDescriptor().text
                dependenciesNames.unique().each { dep ->
                    if ( dep.startsWith( '$' ) ) { // extra-import
                        dep = dep.substring( 1 )
                    } else if ( !dep.startsWith( '+' ) ) { // if not an extra-import, we must always export it
                        dep = '+' + dep
                    }
                    writer.write( dep.replace( ':', '=' ) + '\n' )
                }
            }

            return moduleDescriptor.absolutePath
        }
    }

    private File basicModuleDescriptor() {
        moduleDescriptorsDir.mkdirs()
        new File( moduleDescriptorsDir, 'basic-module-descriptor.properties' )
    }

    private String resolveJavaVersion() {
        def version = project.properties.targetCompatibility ?: '8'
        def versionParts = version.toString().split( '/.' )
        if ( versionParts.size() < 3 ) {
            return versionParts.first()
        } else {
            throw new GradleException( "Invalid Java Version: ${version}. " +
                    "Set 'targetCompatibility' to a valid Java Version" )
        }
    }


}
