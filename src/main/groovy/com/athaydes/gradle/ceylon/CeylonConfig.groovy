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
    String overrides = 'auto-generated/overrides.xml'
    Boolean flatClassPath = false
}
