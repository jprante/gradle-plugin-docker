package org.xbib.gradle.plugin.docker

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional

class DockerBuildTask extends Exec {

    @Input
    final Property<String> executableName

    @Input
    final Property<String> imageName

    @Input
    @Optional
    final Property<String> tag

    @InputDirectory
    File basePath = new File('.')

    @Input
    @Optional
    List<String> buildArgs = []

    @Input
    @Optional
    List<String> instructions

    @Input
    @Optional
    String baseImage

    @Input
    @Optional
    String maintainer

    Dockerfile dockerfile

    DockerBuildTask() {
        this.executableName = project.getObjects().property(String)
        this.imageName = project.getObjects().property(String)
        this.tag = project.getObjects().property(String)
        this.instructions = []
        def resolveClosure = { path -> project.file(path) }
        def copyClosure = { Closure copyClosure -> project.copy(copyClosure) }
        File file = new File(project.buildDir, 'docker-' + name)
        if (!file.exists()) {
            file.mkdirs()
        }
        this.dockerfile = new Dockerfile(file, resolveClosure, copyClosure)
        setWorkingDir(file)
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

    List<String> buildCommandLine() {
        dockerfile.stagingBacklog.each() { closure -> closure() }
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
        if (!file.exists()) {
            dockerfile.writeToFile(file)
        }
        List<String> list = [ executableName.get().toString(), 'build', basePath.path ]
        list << '-f' << file.getName()
        String name = imageName.getOrElse('empty')
        if (tag.isPresent()) {
            list << '-t' << "${name}:${tag.get()}".toString()
        } else {
            list << '-t' << "${name}".toString()
        }
        buildArgs.each {
            list << '--build-arg' << it
        }
        list
    }
}
