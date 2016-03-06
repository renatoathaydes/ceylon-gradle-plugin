package com.athaydes.gradle.ceylon

import groovy.transform.ToString

@ToString( includeNames = true )
class CeylonConfig {
    def ceylonLocation = null
    List sourceRoots = [ 'source' ]
    List resourceRoots = [ 'resource' ]
    List testResourceRoots = [ 'test-resource' ]
    List testRoots = [ 'source' ]
    String output = 'modules'
    String module = ''
    List moduleExclusions = [ ]
    String overrides
    String mavenSettings
    Boolean flatClasspath = true
    Boolean importJars = false
    Boolean forceImports = false
    Boolean verbose = false

    String testModule = ''

    String getTestModule() {
        this.@testModule ?: module
    }

}
