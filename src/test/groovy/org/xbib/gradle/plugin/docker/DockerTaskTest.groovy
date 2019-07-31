package org.xbib.gradle.plugin.docker

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.xbib.gradle.plugin.docker.task.DockerBuildTask

import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.instanceOf
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.isIn
import static org.junit.Assert.assertThat

class DockerTaskTest {

    private static final String TASK_NAME = 'dockerTask'

    private static final ArrayList<String> TEST_ENV = ['foo', 'bar']

    private static final String TEST_TARGET_DIR = 'testTargetDir'

    private static final String TEST_MAINTAINER = 'john doe'

    private static final ArrayList<String> TEST_INSTRUCTIONS = [
            'FROM ubuntu:14.04',
            'ADD foo/bar /',
            'RUN echo hello world'
    ]

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder()

    @Test
    void addTaskToProject() {
        def task = createTask(createProject())
        assertThat task, is(instanceOf(DockerBuildTask))
        assertThat task.name, is(equalTo(TASK_NAME))
    }

    @Test
    void testExposePort() {
        def task = createTask(createProject())
        task.exposePort(99)
        assertThat task.buildDockerfile().instructions[1], equalTo('EXPOSE 99')
    }

    @Test
    void testExposeMultiplePorts() {
        def task = createTask(createProject())
        task.exposePort(99, 100, 101)
        assertThat task.buildDockerfile().instructions[1], equalTo('EXPOSE 99 100 101')
    }

    @Test
    void testExposePortUdp() {
        def task = createTask(createProject())
        task.exposePort("162/udp")
        assertThat task.buildDockerfile().instructions[1], equalTo('EXPOSE 162/udp')
    }

    @Test
    void nonJavaDefaultBaseImage() {
        def project = createProject()
        def task = createTask(project)
        assertThat task.baseImage, is(equalTo(DockerBuildTask.DEFAULT_IMAGE))
    }

    @Test
    void projectBaseImageHasPrecedence() {
        def project = createProject()
        project.extensions.findByName(DockerPlugin.EXTENSION_NAME).baseImage = 'dummyImage'
        project.apply plugin: 'java'
        def task = createTask(project)
        assertThat task.baseImage, is(equalTo('dummyImage'))
    }

    @Test
    void overrideBaseImageInTask() {
        def task = createTask(createProject())
        task.baseImage = "taskBase"
        assertThat task.baseImage, is(equalTo("taskBase"))
    }

    @Test
    void testAddFileWithDir() {
        def project = createProject()
        def task = createTask(project)
        URL dir_url = getClass().getResource(TEST_TARGET_DIR)
        File dir = new File(dir_url.toURI())
        assertThat(dir.isDirectory(), equalTo(true))
        task.addFile(dir)
        task.setupStageDir()
        File targetDir = new File(task.stageDir, TEST_TARGET_DIR)
        assertThat(targetDir.exists(), is(true))
        assertThat(targetDir.isDirectory(), is(true))
        assertThat(targetDir.list().length, is(equalTo(dir.list().length)))
    }

    @Test
    void buildDockerfileFromFileAndExtend() {
        def project = createProject()
        def task = createTask(project)
        def externalDockerfile = testFolder.newFile('Dockerfile')
        externalDockerfile.withWriter { out ->
            TEST_INSTRUCTIONS.each { out.writeLine(it) }
        }
        task.dockerfile externalDockerfile
        task.maintainer = TEST_MAINTAINER
        task.setEnvironment(*TEST_ENV)
        def actual = task.buildDockerfile().instructions
        assertThat(actual, contains(*TEST_INSTRUCTIONS,
                        "ENV ${TEST_ENV.join(' ')}".toString(),
                        "MAINTAINER ${TEST_MAINTAINER}".toString()))
    }

    @Test
    void switchUser() {
        def task = createTask(createProject())
        task.switchUser('junit')
        assertThat "USER junit".toString(), isIn(task.buildDockerfile().instructions)
    }

    @Test
    void defineLabel() {
       def task = createTask(createProject())
       task.label(foo: 'bar')
       assertThat 'LABEL "foo"="bar"', isIn(task.buildDockerfile().instructions)
    }

    @Test
    void defineMultipleLabels() {
       def task = createTask(createProject())
       task.label(foo1: 'bar1', foo2: 'bar2')
       assertThat 'LABEL "foo1"="bar1" "foo2"="bar2"', isIn(task.buildDockerfile().instructions)
    }

    private static Project createProject() {
        def project = ProjectBuilder.builder().build()
        project.extensions.create(DockerPlugin.EXTENSION_NAME, DockerPluginExtension)
        project
    }

    private static Task createTask(Project project) {
        project.task(TASK_NAME, type: DockerBuildTask).configure()
    }
}
