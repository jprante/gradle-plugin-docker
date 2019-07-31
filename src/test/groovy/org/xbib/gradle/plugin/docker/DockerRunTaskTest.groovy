package org.xbib.gradle.plugin.docker

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.xbib.gradle.plugin.docker.task.DockerRunTask

import static org.junit.Assert.assertTrue
import static org.mockito.Mockito.anyString
import static org.mockito.Mockito.argThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.eq

class DockerRunTaskTest {
    
    static class TestDockerRunTask extends DockerRunTask {
        
        DockerClient mockClient
        
        TestDockerRunTask() {
            mockClient = mock(DockerClient.class)
        }

        @Override
        DockerClient getClient() {
            mockClient
        }
    }
    
    private static ArgumentMatcher<Collection<?>> isEmptyCollection = 
        new ArgumentMatcher<Collection<?>>() {
            @Override
            boolean matches(Object collection) {
                return (collection != null) && ((Collection) collection).size() == 0
            }
    }
        
    private static ArgumentMatcher<Collection<?>> isSingletonCollection = 
        new ArgumentMatcher<Collection<?>>() {
            @Override
            boolean matches(Object collection) {
                return (collection != null) && ((Collection) collection).size() == 1
            }
    }
        
    private static ArgumentMatcher<Map<?, ?>> isEmptyMap = 
        new ArgumentMatcher<Map<?, ?>>() {
            @Override
            boolean matches(Object map) {
                return (map != null) && ((Map) map).size() == 0
            }
    }
        
    private static ArgumentMatcher<Map<?, ?>> isSingletonMap = 
        new ArgumentMatcher<Map<?, ?>>() {
            @Override
            boolean matches(Object map) {
                return (map != null) && ((Map) map).size() == 1
            }
    }

    @Test
    void addTaskToProject() {
        def task = ProjectBuilder.builder().build().task('dockerTask', type: DockerRunTask)
        assertTrue(task instanceof DockerRunTask)
    }
    
    @Test
    void runDefault() {
        def project = createProject()
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(false), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }
    
    @Test
    void runNamedContainer() {
        def project = createProject()
        project.dockerRunTask.containerName = CONTAINER_NAME
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(CONTAINER_NAME),
            eq(false), eq(false), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }

    @Test
    void runDetached() {
        def project = createProject()
        project.dockerRunTask.detached = true
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(true), eq(false), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }
    
    @Test
    void runAutoRemove() {
        def project = createProject()
        project.dockerRunTask.autoRemove = true
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(true), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }
    
    @Test
    void runWithEnv() {
        def project = createProject()
        project.dockerRunTask.env("foo", "bar")
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(false), argThat(isSingletonMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }
    
    @Test
    void runWithPorts() {
        def project = createProject()
        project.dockerRunTask.publish("foo", "bar")
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(false), argThat(isEmptyMap), argThat(isSingletonMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }
    
    @Test
    void runWithVolumes() {
        def project = createProject()
        project.dockerRunTask.volume("foo", "bar")
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(false), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isSingletonMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }
    
    @Test
    void runWithVolumesFrom() {
        def project = createProject()
        project.dockerRunTask.volumesFrom("foo")
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(false), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isSingletonCollection), argThat(isEmptyCollection))
    }
    
    @Test
    void runWithLinks() {
        def project = createProject()
        project.dockerRunTask.link("foo")
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(false), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isSingletonCollection))
    }
    
    @Test(expected = NullPointerException.class)
    void runIllegalTaskState() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.extensions.create(DockerPlugin.EXTENSION_NAME, DockerPluginExtension)
        project.task('dockerRunTask', type: DockerRunTask)
        project.dockerRunTask.detached = true
        project.dockerRunTask.autoRemove = true
        project.dockerRunTask.run()
    }

    private static final String CONTAINER_NAME = 'mycontainer'

    private static Project createProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.extensions.create(DockerPlugin.EXTENSION_NAME, DockerPluginExtension)
        project.task('dockerRunTask', type: TestDockerRunTask)
        return project
    }
}
