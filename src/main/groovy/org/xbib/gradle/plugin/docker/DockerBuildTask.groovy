package org.xbib.gradle.plugin.docker

import org.gradle.api.provider.Property
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import javax.annotation.Nullable

class DockerBuildTask extends AbstractExecTask {

    @Input
    final Property<String> executableName

    @Input
    final Property<String> imageName

    @Input
    List<String> tags = []

    @InputDirectory
    File basePath = new File('.')

    @InputFile
    @Nullable
    @Optional
    File dockerFile

    @Input
    @Optional
    List<String> buildArgs = []

    DockerBuildTask() {
        super(DockerBuildTask)
        executableName = project.getObjects().property(String)
        imageName = project.getObjects().property(String)
    }

    @Override
    void exec() {
        executable(executableName.get())
        args 'build', basePath.path
        if (dockerFile) {
            args '-f', dockerFile.path
        }
        tags.each {
            args '-t', "${imageName.getOrElse('imagename')}:$it"
        }
        buildArgs.each {
            args '--build-arg', it
        }
        super.exec()
    }
}
