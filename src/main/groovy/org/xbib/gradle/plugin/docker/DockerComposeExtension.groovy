package org.xbib.gradle.plugin.docker

import org.gradle.api.Project

class DockerComposeExtension {

    private Project project

    private File template

    private File dockerComposeFile

    private Map<String, String> templateTokens

    DockerComposeExtension(Project project) {
        this.project = project
        this.template = project.file('docker-compose.yml.template')
        this.dockerComposeFile = project.file('docker-compose.yml')
        this.templateTokens = new HashMap()
    }

    void setTemplate(Object dockerComposeTemplate) {
        this.template = project.file(dockerComposeTemplate)
    }

    File getTemplate() {
        template
    }

    void setDockerComposeFile(Object dockerComposeFile) {
        this.dockerComposeFile = project.file(dockerComposeFile)
    }

    File getDockerComposeFile() {
        dockerComposeFile
    }

    void setTemplateTokens(Map<String, String> templateTokens) {
        this.templateTokens = templateTokens
    }

    Map<String, String> getTemplateTokens() {
        templateTokens
    }

    void templateToken(String key, String value) {
        this.templateTokens.put(key, value)
    }
}
