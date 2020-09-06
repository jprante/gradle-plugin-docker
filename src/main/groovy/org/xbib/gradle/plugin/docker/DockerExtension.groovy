package org.xbib.gradle.plugin.docker

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class DockerExtension {

    @Input
    boolean enabled = true

    @Input
    @Optional
    String executableName = 'docker'

    @Input
    @Optional
    String registry

    @Input
    String imageName

    @Input
    @Optional
    String tag

}
