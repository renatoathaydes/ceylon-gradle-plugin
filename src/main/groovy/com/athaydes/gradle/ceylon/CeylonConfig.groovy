package com.athaydes.gradle.ceylon

import com.redhat.ceylon.common.Backend
import com.redhat.ceylon.common.config.CeylonConfig as CeylonToolConfig
import com.redhat.ceylon.common.config.DefaultToolOptions
import groovy.transform.ToString

@ToString( includeNames = true )
class CeylonConfig {
    def ceylonLocation = null
    List sourceRoots
    List resourceRoots
    List testResourceRoots = [ 'test-resource' ]
    List testRoots
    String output
    String fatJarDestination
    String module = ''
    List moduleExclusions = [ ]
    String overrides
    String mavenSettings
    Boolean flatClasspath = true
    Boolean importJars = false
    Boolean forceImports = false
    Boolean verbose = false
    String javaRuntimeDestination
    String entryPoint
    String testModule = ''
    boolean generateTestReport = true
    String testReportDestination

    String getTestModule() {
        this.@testModule ?: module
    }

    void setModuleName( String name ) {
        this.module = name
    }

    /**
     * Set the default values from the Ceylon config file, if it is present.
     */
    CeylonConfig() {
        final config = CeylonToolConfig.get()

        sourceRoots = new ArrayList( DefaultToolOptions.compilerSourceDirs )
        resourceRoots = new ArrayList( DefaultToolOptions.compilerResourceDirs )
        testRoots = new ArrayList( DefaultToolOptions.compilerSourceDirs )
        output = DefaultToolOptions.compilerOutputRepo

        // TODO split comma-separated modules when passing option to Ceylon
        def configFileDefinesModule = ( config.getOption( DefaultToolOptions.COMPILER_MODULES ) != null )
        if ( configFileDefinesModule ) {
            module = DefaultToolOptions.getCompilerModules( Backend.Header ).join( ',' )
        }

        // This plugin differs from the Ceylon default regarding the flatClasspath option
        def configFileDefinesFlatClasspath = ( config.getBoolOption( DefaultToolOptions.DEFAULTS_FLAT_CLASSPATH ) != null )
        if ( configFileDefinesFlatClasspath ) {
            flatClasspath = DefaultToolOptions.defaultFlatClasspath
        }

        entryPoint = DefaultToolOptions.getRunToolRun( Backend.Java )
    }

}
