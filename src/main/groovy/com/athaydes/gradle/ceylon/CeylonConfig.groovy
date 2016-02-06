package com.athaydes.gradle.ceylon

import groovy.transform.ToString

@ToString( includeNames = true )
class CeylonConfig {
    def ceylonLocation = null
    List sourceRoots = [ 'source' ]
    List resourceRoots = [ 'resource' ]
    String output = 'modules'
    String module = ''
    List moduleExclusions = [ ]
    String overrides
    String mavenSettings
    Boolean flatClassPath = false
    Boolean forceImports = false
    Boolean verbose = false

    String testModule = ''

    String getTestModule() {
        this.@testModule ?: module
    }

}
