package org.xbib.gradle.plugin.docker

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.xbib.gradle.plugin.docker.task.DockerBuildTask

import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

class DockerPluginTest {

    @Test
    void pluginAddsExtensionToProject() {
        Project project = createProjectAndApplyPlugin()
        assertTrue(project.docker instanceof DockerPluginExtension)
    }

    @Test
    void taskTypeIsAvailableInPluginNamespace() {
        Project project = createProjectAndApplyPlugin()
        assertTrue(project.Docker == DockerBuildTask.class)
    }

    @Test
    void pluginInjectsTaskMaintainerFromExtension() {
        Project project = createProjectAndApplyPlugin()
        def testMaintainer = "PluginTest Maintainer"
        project.docker.maintainer = testMaintainer
        def task = project.task('docker', type: DockerBuildTask)
        assertThat task.maintainer, equalTo(testMaintainer)
    }

    private static Project createProjectAndApplyPlugin() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'org.xbib.gradle.plugin.docker'
        project
    }
}
