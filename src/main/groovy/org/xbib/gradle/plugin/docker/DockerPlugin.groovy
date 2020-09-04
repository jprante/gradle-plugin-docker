package org.xbib.gradle.plugin.docker

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.util.GradleVersion

class DockerPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        checkVersion()
        project.plugins.apply(BasePlugin)
        project.plugins.apply(PublishingPlugin)
        project.ext.DockerBuildTask = DockerBuildTask.class
        project.ext.DockerPushTask = DockerPushTask.class
        def extension = project.extensions.create('docker', DockerPluginExtension)
        if (project.properties.dockerExecutable) {
            extension.executableName = project.properties.dockerExecutable
        }
        if (project.properties.dockerRegistry) {
            extension.registry = project.properties.dockerRegistry
        }
        project.tasks.withType(DockerBuildTask) { DockerBuildTask buildTask ->
            project.tasks.assemble.dependsOn(buildTask)
            buildTask.executableName.set(project.provider { extension.executableName })
        }
        project.afterEvaluate {
            project.tasks.withType(DockerBuildTask) { DockerBuildTask buildTask ->
                buildTask.with {
                    if (extension.registry) {
                        imageName.set("${extension.registry}/${imageName.get()}")
                    }
                    commandLine it.buildCommandLine()
                }
                String pushTaskName = pushTaskName(buildTask)
                def pushTask = project.task(pushTaskName, type: DockerPushTask) {
                    executableName.set(project.provider { buildTask.executableName.get() })
                    imageName.set(project.provider { buildTask.imageName.get() })
                    tag.set(project.provider { buildTag })
                }
                pushTask.dependsOn(buildTask)
                project.tasks.publish.dependsOn(pushTask)
            }
        }
    }

    private static void checkVersion() {
        String version = '6.4'
        if (GradleVersion.current() < GradleVersion.version(version)) {
            throw new GradleException("need Gradle ${version} or higher")
        }
    }

    private static String pushTaskName(DockerBuildTask task) {

        return "push" +
                task.imageName.isPresent() ? clean(task.imageName.get()).capitalize() : '' +
                task.tag.isPresent() ? clean(task.tag.get()) : ''
    }

    private static String clean(String string) {
        FORBIDDEN_TASKNAME_CHARACTERS.each { c ->
            string = string.replace(c, '_' as char) }
        string
    }

    private static final char[] FORBIDDEN_TASKNAME_CHARACTERS = [
            '/', '\\', ':', '<', '>', '"', '?', '*', '|'
    ] as char[]
}
