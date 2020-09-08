package org.xbib.gradle.plugin.docker

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Nested

class DockerPushTask extends Exec {

    @Delegate
    @Nested
    DockerExtension dockerExtension

    DockerPushTask() {
        this.dockerExtension = project.extensions.findByName('docker') as DockerExtension
    }

    @Override
    void exec() {
        commandLine buildCommandLine()
        super.exec()
    }

    private List<String> buildCommandLine() {
        List<String> list = [ executableName, 'push' ]
        String fullImageName = imageName
        if (registry) {
            fullImageName = "${registry}/${imageName}".toString()
        }
        if (tag) {
            fullImageName = "${fullImageName}:${tag}".toString()
        }
        list <<  fullImageName.toString()
        list
    }
}
