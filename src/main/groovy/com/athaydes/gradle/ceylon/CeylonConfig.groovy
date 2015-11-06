package com.athaydes.gradle.ceylon

import groovy.transform.ToString

@ToString( includeNames = true )
class CeylonConfig {
    String ceylonLocation
    String sourceRoot
    String resourceRoot
    String modules
    String overrides
    Boolean flatClassPath
}
