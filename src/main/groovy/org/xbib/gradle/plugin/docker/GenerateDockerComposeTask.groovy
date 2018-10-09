package org.xbib.gradle.plugin.docker

import groovy.transform.Memoized
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.text.MessageFormat

class GenerateDockerComposeTask extends DefaultTask {

    DockerComposeExtension ext

    Configuration configuration

    GenerateDockerComposeTask() {
        group = 'Docker'
    }

    @TaskAction
    void run() {
        if (!template.file) {
            throw new IllegalStateException("could not find specified template file ${template.file}")
        }
        def templateTokens = moduleDependencies.collectEntries {
            [("{{${it.group}:${it.name}}}"): it.version]
        }
        templateTokens.putAll(ext.templateTokens.collectEntries {
            [("{{${it.key}}}"): it.value]
        })
        dockerComposeFile.withPrintWriter { writer ->
            template.eachLine { line ->
                writer.println this.replaceAll(line, templateTokens)
            }
        }
    }

    @Override
    String getDescription() {
        def defaultDescription = "Populates ${ext.template.name} file with versions" +
                " of dependencies from the '${configuration.name}' configuration"
        super.description ?: defaultDescription
    }

    @Input
    @Memoized
    Set<ModuleVersionIdentifier> getModuleDependencies() {
        return configuration.resolvedConfiguration
            .resolvedArtifacts
            *.moduleVersion
            *.id
            .toSet()
    }

    @InputFiles
    File getTemplate() {
        ext.template
    }

    @OutputFile
    File getDockerComposeFile() {
        ext.dockerComposeFile
    }

    protected String replaceAll(String line, Map<String, String> templateTokens) {
        templateTokens.each { mapping -> line = line.replace(mapping.key, mapping.value) }
        def unmatchedTokens = line.findAll(/\{\{.*\}\}/)
        if (!unmatchedTokens.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format("failed to resolve Docker dependencies declared in {0}: {1}. Known dependencies: {2}",
                    [template, unmatchedTokens, templateTokens] as Object[]))
        }
        line
    }
}
