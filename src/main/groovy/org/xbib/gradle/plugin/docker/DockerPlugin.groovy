package org.xbib.gradle.plugin.docker

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.BasePlugin
import org.gradle.util.GradleVersion

class DockerPlugin implements Plugin<Project> {

    private static final Logger logger = Logging.getLogger(DockerPlugin)

    @Override
    void apply(Project project) {
        checkVersion()
        logger.info "Docker plugin says hello"
        project.with {
            plugins.apply(BasePlugin)
        }
        project.ext.DockerBuildTask = DockerBuildTask.class
        project.ext.DockerPushTask = DockerPushTask.class
    }

    private static void checkVersion() {
        String version = '6.4'
        if (GradleVersion.current() < GradleVersion.version(version)) {
            throw new GradleException("need Gradle ${version} or higher")
        }
    }
}
