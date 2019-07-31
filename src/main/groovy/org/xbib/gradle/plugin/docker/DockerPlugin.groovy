package org.xbib.gradle.plugin.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin

class DockerPlugin implements Plugin<Project> {

    private static final String DEFAULT_DOCKER_BINARY = "docker"

    static final String EXTENSION_NAME = "docker"

    DockerPluginExtension extension

    void apply(Project project) {
        this.extension = createExtension(project)
        addDockerTaskType(project)
        addDockerRunTaskType(project)
        project.plugins.withType(ApplicationPlugin).all {
            addDistDockerTask(project)
        }
        configureDockerTasks(project)
        configureDockerRunTasks(project)
    }

    private void addDistDockerTask(Project project) {
        project.task('distDocker', type: org.xbib.gradle.plugin.docker.task.DockerBuildTask) {
            group = 'docker'
            description = "Publish the project as a Docker image"
            inputs.files project.distTar
            def installDir = "/" + project.distTar.archiveName - ".${project.distTar.extension}"
            doFirst {
                applicationName = project.applicationName
                dockerfile {
                    ADD(project.distTar.outputs.files.singleFile)
                    ENTRYPOINT(["$installDir/bin/${project.applicationName}"])
                }
            }
        }
    }

    private static void addDockerTaskType(Project project) {
        project.ext.Docker = org.xbib.gradle.plugin.docker.task.DockerBuildTask.class
    }

    private static void addDockerRunTaskType(Project project) {
        project.ext.DockerRun = org.xbib.gradle.plugin.docker.task.DockerRunTask.class
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
        project.tasks.withType(org.xbib.gradle.plugin.docker.task.DockerBuildTask.class).all { task ->
            applyTaskDefaults(task)
        }
    }

    private void configureDockerRunTasks(Project project) {
        project.tasks.withType(org.xbib.gradle.plugin.docker.task.DockerRunTask.class).all { task ->
            applyRunTaskDefaults(task)
        }
    }

    private void applyTaskDefaults(task) {
        task.conventionMapping.with {
            dockerBinaryPath = { extension.dockerBinaryPath }
            maintainer = { extension.maintainer }
            registry = { extension.registry }
            baseImage = { extension.baseImage }
        }
    }
    
    private void applyRunTaskDefaults(task) {
        task.conventionMapping.with {
            dockerBinaryPath = { extension.dockerBinaryPath }
            registry = { extension.registry }
            baseImage = { extension.baseImage }
        }
    }
}
