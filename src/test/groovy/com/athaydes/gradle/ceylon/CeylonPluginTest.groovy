package com.athaydes.gradle.ceylon

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class CeylonPluginTest {

    @Test
    void "All tasks added to project"() {
        Project project = ProjectBuilder.builder()
                .withName( 'test-project' )
                .build()

        project.apply plugin: 'com.athaydes.ceylon'

        assert project.tasks.cleanCeylon
        assert project.tasks.compileCeylon
        assert project.tasks.runCeylon
        assert project.tasks.testCeylon
        assert project.tasks.resolveCeylonDependencies
        assert project.tasks.generateOverridesFile
        assert project.tasks.importJars

        assert project.extensions.ceylon instanceof CeylonConfig

        assert project.configurations.ceylonCompile
        assert project.configurations.ceylonRuntime
    }

    @Test
    void "Can apply Java plugin, then Ceylon plugin"() {
        Project project = ProjectBuilder.builder()
                .withName( 'test-project' )
                .build()

        project.apply plugin: 'java'
        project.apply plugin: 'com.athaydes.ceylon'
    }

}
