package org.xbib.gradle.plugin.docker

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class DockerPushTask extends Exec {

    @Input
    boolean enabled = true

    @Input
    String executableName = 'docker'

    @Optional
    @Input
    String registry

    @Input
    String imageName

    @Optional
    @Input
    String tag

    DockerPushTask() {
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
