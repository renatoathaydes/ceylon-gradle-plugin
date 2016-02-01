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
    String mavenSettings = 'auto-generated/settings.xml'
    Boolean flatClassPath = false
    ModuleImportOverrides moduleImportOverrides

    String testModule = ''

    String getTestModule() {
        this.@testModule ?: module
    }

}

class ModuleImportOverrides {
    Boolean forceModuleImports = false
    Boolean showSuggestions = false
    Map<String, List<String>> extraImports = [ : ]
    List<String> excludeImports = [ ]
}
