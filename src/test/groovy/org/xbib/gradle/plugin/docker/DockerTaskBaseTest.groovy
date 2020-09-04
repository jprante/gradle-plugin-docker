package org.xbib.gradle.plugin.docker

import org.xbib.gradle.plugin.docker.task.DockerTaskBase
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import static org.hamcrest.Matchers.equalToIgnoringCase
import static org.hamcrest.Matchers.isA
import static org.hamcrest.MatcherAssert.assertThat

class DockerTaskBaseTest {

    private static final String PROJECT_GROUP = 'mygroup'

    private static final String REGISTRY = 'myregistry'

    private static final String PROJECT_VERSION = 'myversion'

    private static final String TAG_VERSION = 'tagVersion'
    
    static class DummyTask extends DockerTaskBase {
    }

    @Test
    void getCommandLineClient() {
        def project = createProject()
        DockerClient client = project.dummyTask.getClient()
        assertThat(client, isA(CommandLineDockerClient))
    }
    
    @Test
    void getImageTag() {
        def project = createProject()
        def imageTag = project.dummyTask.imageTag
        assertThat(imageTag, equalToIgnoringCase("${project.name}:${DockerTaskBase.LATEST}"))
        project.group = PROJECT_GROUP
        imageTag = project.dummyTask.imageTag
        assertThat(imageTag, equalToIgnoringCase("${PROJECT_GROUP}/${project.name}:${DockerTaskBase.LATEST}"))
        project.group = null
        project.dummyTask.registry = REGISTRY
        imageTag = project.dummyTask.imageTag
        assertThat(imageTag, equalToIgnoringCase("${REGISTRY}/${project.name}:${DockerTaskBase.LATEST}"))
        project.group = PROJECT_GROUP
        project.dummyTask.registry = REGISTRY
        imageTag = project.dummyTask.imageTag
        assertThat(imageTag, equalToIgnoringCase("${REGISTRY}/${PROJECT_GROUP}/${project.name}:${DockerTaskBase.LATEST}"))
        project.version = PROJECT_VERSION
        imageTag = project.dummyTask.imageTag
        assertThat(imageTag, equalToIgnoringCase("${REGISTRY}/${PROJECT_GROUP}/${project.name}:${PROJECT_VERSION}"))
        project.dummyTask.tagVersion = TAG_VERSION
        imageTag = project.dummyTask.imageTag
        assertThat(imageTag, equalToIgnoringCase("${REGISTRY}/${PROJECT_GROUP}/${project.name}:${TAG_VERSION}"))
        project.dummyTask.setTagVersionToLatest()
        imageTag = project.dummyTask.imageTag
        assertThat(imageTag, equalToIgnoringCase("${REGISTRY}/${PROJECT_GROUP}/${project.name}:${DockerTaskBase.LATEST}"))
    }

    private static Project createProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.extensions.create(DockerPlugin.EXTENSION_NAME, DockerPluginExtension)
        project.task('dummyTask', type: DummyTask)
        project.dummyTask.dockerBinaryPath = 'true'
        return project
    }
}
