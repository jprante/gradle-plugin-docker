package org.xbib.gradle.plugin.docker

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

class DockerBuildTask extends Exec {

    @Delegate
    @Nested
    DockerExtension dockerExtension

    @InputDirectory
    File basePath = new File('.')

    @Input
    @Optional
    List<String> buildArgs = []

    @Input
    @Optional
    List<String> instructions = []

    @Input
    @Optional
    String baseImage

    @Input
    @Optional
    String maintainer

    private Dockerfile dockerfile

    DockerBuildTask() {
        this.dockerExtension = project.extensions.findByName('docker') as DockerExtension
        File file = new File(project.buildDir, 'docker-' + name)
        if (!file.exists()) {
            file.mkdirs()
        }
        this.dockerfile = new Dockerfile(project, file)
        setWorkingDir(dockerfile.workingDir)
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

    @Override
    void exec() {
        File file = createDockerFile()
        executeStagingBacklog()
        commandLine buildCommandLine(file)
        super.exec()
    }

    private File createDockerFile() {
        if (!dockerfile.hasBase()) {
            dockerfile.from(baseImage ? baseImage : 'scratch')
        }
        if (maintainer) {
            dockerfile.maintainer(maintainer)
        }
        if (instructions) {
            dockerfile.appendAll(instructions)
        }
        File file = new File(workingDir, 'Dockerfile')
        dockerfile.writeToFile(file)
        logger.info "${dockerfile.instructions}"
        file
    }

    private void executeStagingBacklog() {
        dockerfile.stagingBacklog.each() { closure -> closure() }
    }

    private List<String> buildCommandLine(File dockerfilename) {
        List<String> list = [ executableName, 'build', basePath.path ]
        list << '-f' << dockerfilename.absoluteFile.toString()
        String fullImageName = imageName
        if (registry) {
            fullImageName = "${registry}/${imageName}".toString()
        }
        if (tag) {
            fullImageName = "${fullImageName}:${tag}".toString()
        }
        list << '-t' << fullImageName
        buildArgs.each {
            list << '--build-arg' << it
        }
        list
    }
}
