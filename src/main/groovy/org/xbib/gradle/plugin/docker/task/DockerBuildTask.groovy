package org.xbib.gradle.plugin.docker.task

import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.xbib.gradle.plugin.docker.DockerClient
import org.xbib.gradle.plugin.docker.DockerPlugin
import org.xbib.gradle.plugin.docker.Dockerfile

class DockerBuildTask extends DockerTaskBase {

    public static final String DEFAULT_IMAGE = 'ubi8/ubi'

    String maintainer

    Boolean dryRun

    Boolean push

    Boolean pull

    Dockerfile dockerfile

    String baseImage

    def instructions

    File stageDir

    DockerBuildTask() {
        instructions = []
        stageDir = new File(project.buildDir, "docker")
    }

    @Override
    Task configure(Closure configureClosure) {
        def resolveClosure = { path -> project.file(path) }
        def copyClosure = { Closure copyClosure -> project.copy(copyClosure) }
        this.dockerfile = new Dockerfile(stageDir, resolveClosure, copyClosure)
        super.configure(configureClosure)
    }

    void dockerfile(Closure closure) {
        dockerfile.with(closure)
    }

    void dockerfile(String path) {
        dockerfile(project.file(path))
    }

    void dockerfile(File baseFile) {
        dockerfile.extendDockerfile(baseFile)
    }

    String getBaseImage() {
        def image = project.extensions.findByName(DockerPlugin.EXTENSION_NAME).baseImage
        if (image) {
            return image
        }
        baseImage ? baseImage : DEFAULT_IMAGE
    }

    void setupStageDir() {
        if (!stageDir.exists()) {
            stageDir.mkdirs()
        }
        dockerfile.stagingBacklog.each() { closure -> closure() }
    }

    Dockerfile buildDockerfile() {
        if (!dockerfile.hasBase()) {
            dockerfile.from(getBaseImage())
        }
        if (getMaintainer()) {
            dockerfile.maintainer(getMaintainer())
        }
        return dockerfile.appendAll(instructions)
    }

    @TaskAction
    void build() {
        setupStageDir()
        buildDockerfile().writeToFile(new File(stageDir, 'Dockerfile'))
        tag = getImageTag()
        if (!dryRun) {
            DockerClient client = getClient()
            println client.buildImage(stageDir, tag, pull)
            if (push) {
                println client.pushImage(tag)
            }
        }
    }
}
