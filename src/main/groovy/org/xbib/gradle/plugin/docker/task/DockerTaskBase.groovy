package org.xbib.gradle.plugin.docker.task

import org.gradle.api.DefaultTask
import org.xbib.gradle.plugin.docker.CommandLineDockerClient
import org.xbib.gradle.plugin.docker.DockerClient

abstract class DockerTaskBase extends DefaultTask {

    static final String LATEST = 'latest'
    
    String applicationName

    String tag

    String tagVersion

    String registry

    String dockerBinaryPath
    
    DockerTaskBase() {
        applicationName = project.name
    }

    void setTagVersion(String version) {
        tagVersion = version;
    }

    void setTagVersionToLatest() {
        tagVersion = LATEST;
    }

    String getImageTag() {
        String tag = this.tag ?: getDefaultImageTag()
        appendImageTagVersion(tag)
    }

    String getDefaultImageTag() {
        String tag
        if (registry) {
            def group = project.group ? "${project.group}/" : ''
            tag = "${-> registry}/${group}${-> applicationName}"
        } else if (project.group) {
            tag = "${-> project.group}/${-> applicationName}"
        } else {
            tag = "${-> applicationName}"
        }
        return tag
    }

    private String appendImageTagVersion(String tag) {
        def version = tagVersion ?: project.version
        if(version == 'unspecified') {
            version = LATEST
        }
        return "${tag}:${version}"
    }

    DockerClient getClient() {
        new CommandLineDockerClient(getDockerBinaryPath())
    }
}
