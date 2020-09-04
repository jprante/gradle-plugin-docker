package org.xbib.gradle.plugin.docker

import org.gradle.api.provider.Property
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input

class DockerPushTask extends AbstractExecTask {

    @Input
    final Property<String> executableName

    @Input
    final Property<String> imageName

    @Input
    final Property<String> tag

    DockerPushTask() {
        super(DockerPushTask)
        executableName = project.getObjects().property(String)
        imageName = project.getObjects().property(String)
        tag = project.getObjects().property(String)
    }

    @Override
    void exec() {
        executable(executableName.get())
        args 'push', "${imageName.get()}${tag.get() ? ":${tag.get()}" : ''}"
        super.exec()
    }
}
