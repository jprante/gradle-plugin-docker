package org.xbib.gradle.plugin.docker

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

class DockerComposeUpTask extends DefaultTask {

    DockerComposeExtension ext

    Configuration configuration

    DockerComposeUpTask() {
        this.group = 'Docker'
    }

    @TaskAction
    void run() {
        project.exec {
            it.executable "docker-compose"
            it.args "-f", getDockerComposeFile(), "up", "-d"
        }
    }

    @Override
    String getDescription() {
        def defaultDescription = "Executes `docker-compose` using ${ext.dockerComposeFile.name}"
        super.description ?: defaultDescription
    }

    @InputFiles
    File getDockerComposeFile() {
        ext.dockerComposeFile
    }

    void setExtension(DockerComposeExtension ext) {
        this.ext = ext
    }

    void setConfiguration(Configuration configuration) {
        this.configuration = configuration
    }
}
