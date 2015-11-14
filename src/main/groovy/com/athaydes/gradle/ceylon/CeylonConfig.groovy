package com.athaydes.gradle.ceylon

import groovy.transform.ToString

@ToString( includeNames = true )
class CeylonConfig {
    def ceylonLocation = '/usr/bin/ceylon'
    List sourceRoots = [ 'source' ]
    List resourceRoots = [ 'resource' ]
    String output = 'modules'
    String module = ''
    String overrides = 'auto-generated/overrides.xml'
    Boolean flatClassPath = false
}
