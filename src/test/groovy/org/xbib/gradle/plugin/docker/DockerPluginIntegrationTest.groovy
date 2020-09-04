package org.xbib.gradle.plugin.docker

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.assertEquals

class DockerPluginIntegrationTest {

    private File projectDir

    private File dockerFile

    private File settingsFile

    private File buildFile

    @BeforeEach
    void setup(@TempDir File testProjectDir) throws IOException {
        this.projectDir = testProjectDir
        this.settingsFile = new File(testProjectDir, "settings.gradle")
        this.buildFile = new File(testProjectDir, "build.gradle")
    }

    @Test
    void testPlugin() {
        String settingsFileContent = '''
rootProject.name = 'docker-test'
'''
        settingsFile.write(settingsFileContent)
        String buildFileContent = '''
plugins {
    id 'org.xbib.gradle.plugin.docker'
}

docker {
  registry = 'localhost'
}

task myDockerBuild(type: DockerBuildTask) {
  imageName = 'mytestimage'
  dockerfile {
      add('settings.gradle','/')
  }
}
'''
        buildFile.write(buildFileContent)
        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(":build", "--info", "--stacktrace")
                .forwardOutput()
                .build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":build").getOutcome())
    }
}
