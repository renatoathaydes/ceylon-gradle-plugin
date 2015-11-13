package com.athaydes.gradle.ceylon

import groovy.transform.ToString

@ToString( includeNames = true )
class CeylonConfig {
    String ceylonLocation = '/usr/bin/ceylon'
    String sourceRoot = 'source'
    String resourceRoot = 'resource'
    String modules = ''
    String overrides = 'auto-generated/overrides.xml'
    Boolean flatClassPath = false
}
