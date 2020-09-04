package org.xbib.gradle.plugin.docker

import groovy.transform.CompileStatic

@CompileStatic
class DockerPluginExtension {

    String executableName = "docker"

    String registry

    String imageName

    String getFullImageName() {
        registry ? "$registry/$imageName" : imageName
    }
}
