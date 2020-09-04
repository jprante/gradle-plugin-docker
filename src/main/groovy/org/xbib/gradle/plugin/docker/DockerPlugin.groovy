package org.xbib.gradle.plugin.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.xbib.gradle.plugin.docker.task.DockerBuildTask
import org.xbib.gradle.plugin.docker.task.DockerRunTask

class DockerPlugin implements Plugin<Project> {

    private static final String DEFAULT_DOCKER_BINARY = "docker"

    static final String EXTENSION_NAME = "docker"

    DockerPluginExtension extension

    void apply(Project project) {
        this.extension = createExtension(project)
        project.ext.Docker = DockerBuildTask
        project.ext.DockerRun = DockerRunTask
        project.plugins.withType(ApplicationPlugin).all {
            addDistDockerTask(project)
        }
        configureDockerTasks(project)
        configureDockerRunTasks(project)
    }

    private static void addDistDockerTask(Project project) {
        project.task('distDocker', type: DockerBuildTask) {
            group = 'docker'
            description = "Publish the project as a Docker image"
            inputs.files project.distTar.outputs
            doFirst {
                applicationName = project.applicationName
                dockerfile {
                    ADD(project.distTar.outputs.files.singleFile)
                    if (project.applicationName) {
                        def installDir = "/${project.distTar.archiveName}-${project.distTar.extension}"
                        ENTRYPOINT(["$installDir/bin/${project.applicationName}"])
                    }
                }
            }
        }
    }

    private static createExtension(Project project) {
        def extension = project.extensions.create(EXTENSION_NAME, DockerPluginExtension)
        extension.with {
            maintainer = ''
            dockerBinaryPath = DEFAULT_DOCKER_BINARY
            registry = ''
        }
        extension
    }

    private void configureDockerTasks(Project project) {
        project.tasks.withType(DockerBuildTask).all { task ->
            if (task.conventionMapping) {
                task.conventionMapping.with {
                    dockerBinaryPath = { extension.dockerBinaryPath }
                    maintainer = { extension.maintainer }
                    registry = { extension.registry }
                    baseImage = { extension.baseImage }
                }
            }
        }
    }

    private void configureDockerRunTasks(Project project) {
        project.tasks.withType(DockerRunTask).all { task ->
            if (task.conventionMapping) {
                task.conventionMapping.with {
                    dockerBinaryPath = { extension.dockerBinaryPath }
                    registry = { extension.registry }
                    baseImage = { extension.baseImage }
                }
            }
        }
    }
}
