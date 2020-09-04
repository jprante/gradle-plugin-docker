package org.xbib.gradle.plugin.docker

import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input

class DockerPushTask extends Exec {

    @Input
    final Property<String> executableName

    @Input
    final Property<String> imageName

    @Input
    final Property<String> tag

    DockerPushTask() {
        executableName = project.getObjects().property(String)
        imageName = project.getObjects().property(String)
        tag = project.getObjects().property(String)
    }

    @Override
    void exec() {
        environment super.getEnvironment()
        executable executableName.get().toString()
        args 'push', "${imageName.get()}${tag.get() ? ":${tag.get()}" : ''}"
        super.exec()
    }
}
