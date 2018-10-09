package org.xbib.gradle.plugin.docker

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.internal.attributes.ImmutableAttributesFactory
import org.gradle.api.logging.LogLevel
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory

import javax.inject.Inject
import java.util.regex.Pattern

class DockerPlugin implements Plugin<Project> {

    private static final Pattern LABEL_KEY_PATTERN = Pattern.compile('^[a-z0-9.-]*$')

    private final ObjectFactory objectFactory

    private final ImmutableAttributesFactory attributesFactory

    @Inject
    DockerPlugin(ObjectFactory objectFactory, ImmutableAttributesFactory attributesFactory) {
        this.objectFactory = objectFactory
        this.attributesFactory = attributesFactory
    }

    @Override
    void apply(Project project) {
        DockerExtension ext = project.extensions.create('docker', DockerExtension, project)
        if (!project.configurations.findByName('docker')) {
            project.configurations.create('docker')
        }

        Delete clean = project.tasks.create('dockerClean', Delete, {
            group = 'Docker'
            description = 'Cleans Docker build directory.'
        })

        Copy prepare = project.tasks.create('dockerPrepare', Copy, {
            group = 'Docker'
            description = 'Prepares Docker build directory.'
            dependsOn clean
        })

        Exec exec = project.tasks.create('docker', Exec, {
            group = 'Docker'
            description = 'Builds Docker image.'
            dependsOn prepare
        })

        Task tag = project.tasks.create('dockerTag', {
          group = 'Docker'
          description = 'Applies all tags to the Docker image.'
          dependsOn exec
        })

        Exec push = project.tasks.create('dockerPush', Exec, {
            group = 'Docker'
            description = 'Pushes named Docker image to configured Docker Hub.'
            dependsOn tag
        })

        Zip dockerfileZip = project.tasks.create('dockerfileZip', Zip, {
            group = 'Docker'
            description = 'Bundles the configured Dockerfile in a zip file'
        })

        PublishArtifact dockerArtifact = new ArchivePublishArtifact(dockerfileZip)
        Configuration dockerConfiguration = project.getConfigurations().getByName('docker')
        project.getComponents().add(new DockerComponent(dockerArtifact, dockerConfiguration.getAllDependencies(),
                objectFactory, attributesFactory))

        DockerComposeExtension composeExtension = project.extensions.create('dockerCompose', DockerComposeExtension, project)

        project.tasks.create('generateDockerCompose', GenerateDockerComposeTask, {
            it.ext = composeExtension
            it.configuration = dockerConfiguration
        })

        project.tasks.create('dockerComposeUp', DockerComposeUpTask, {
            it.ext = composeExtension
            it.configuration = dockerConfiguration
        })

        DockerRunExtension runExtension = project.extensions.create('dockerRun', DockerRunExtension)

        Exec dockerRunStatus = project.tasks.create('dockerRunStatus', Exec, {
            group = 'Docker Run'
            description = 'Checks the run status of the container'
        })

        Exec dockerRun = project.tasks.create('dockerRun', Exec, {
            group = 'Docker Run'
            description = 'Runs the specified container with port mappings'
        })

        Exec dockerStop = project.tasks.create('dockerStop', Exec, {
            group = 'Docker Run'
            description = 'Stops the named container if it is running'
            ignoreExitValue = true
        })

        Exec dockerRemoveContainer = project.tasks.create('dockerRemoveContainer', Exec, {
            group = 'Docker Run'
            description = 'Removes the persistent container associated with the Docker Run tasks'
            ignoreExitValue = true
        })

        Exec dockerNetworkModeStatus = project.tasks.create('dockerNetworkModeStatus', Exec, {
            group = 'Docker Run'
            description = 'Checks the network configuration of the container'
        })

        project.afterEvaluate {
            ext.resolvePathsAndValidate()
            String dockerDir = "${project.buildDir}/docker"
            clean.delete dockerDir

            prepare.with {
                with ext.copySpec
                from(ext.resolvedDockerfile) {
                    rename { fileName ->
                        fileName.replace(ext.resolvedDockerfile.getName(), 'Dockerfile')
                    }
                }
                into dockerDir
            }

            exec.with {
                workingDir dockerDir
                commandLine buildCommandLine(ext)
                dependsOn ext.getDependencies()
                logging.captureStandardOutput LogLevel.INFO
                logging.captureStandardError LogLevel.ERROR
            }

            if (!ext.tags.isEmpty()) {

                ext.tags.each { tagName ->
                    String taskTagName = ucfirst(tagName)
                    Exec subTask = project.tasks.create('dockerTag' + taskTagName, Exec, {
                        group = 'Docker'
                        description = "Tags Docker image with tag '${tagName}'"
                        workingDir dockerDir
                        commandLine 'docker', 'tag', "${ -> ext.name}", "${ -> computeName(ext.name, tagName)}"
                        dependsOn exec
                    })
                    tag.dependsOn subTask

                    project.tasks.create('dockerPush' + taskTagName, Exec, {
                        group = 'Docker'
                        description = "Pushes the Docker image with tag '${tagName}' to configured Docker Hub"
                        workingDir dockerDir
                        commandLine 'docker', 'push', "${ -> computeName(ext.name, tagName)}"
                        dependsOn subTask
                    })
                }
            }

            push.with {
                workingDir dockerDir
                commandLine 'docker', 'push', "${ -> ext.name}"
            }

            dockerfileZip.with {
                from(ext.resolvedDockerfile)
            }

            dockerRunStatus.with {
                standardOutput = new ByteArrayOutputStream()
                commandLine 'docker', 'inspect', '--format={{.State.Running}}', runExtension.name
                doLast {
                    if (standardOutput.toString().trim() != 'true') {
                        println "Docker container '${runExtension.name}' is STOPPED."
                        return 1
                    } else {
                        println "Docker container '${runExtension.name}' is RUNNING."
                    }
                }
            }

            dockerNetworkModeStatus.with {
                standardOutput = new ByteArrayOutputStream()
                commandLine 'docker', 'inspect', '--format={{.HostConfig.NetworkMode}}', runExtension.name
                doLast {
                    def networkMode = standardOutput.toString().trim()
                    if (networkMode == 'default') {
                        println "Docker container '${runExtension.name}' has default network configuration (bridge)."
                    }
                    else {
                        if (networkMode == runExtension.network) {
                            println "Docker container '${runExtension.name}' is configured to run with '${runExtension.network}' network mode."
                        }
                        else {
                            println "Docker container '${runExtension.name}' runs with '${networkMode}' network mode instead of the configured '${runExtension.network}'."
                            return 1
                        }
                    }
                }
            }

            dockerRun.with {
                List<?> args = new ArrayList<>()
                args.addAll(['docker', 'run'])
                if (runExtension.daemonize) {
                    args.add('-d')
                }
                if (runExtension.clean) {
                    args.add('--rm')
                } else {
                    finalizedBy dockerRunStatus
                }
                if (runExtension.network) {
                    args.addAll(['--network', runExtension.network])
                }
                for (String port : runExtension.ports) {
                    args.add('-p')
                    args.add(port)
                }
                for (Map.Entry<Object,String> volume : runExtension.volumes.entrySet()) {
                    File localFile = project.file(volume.key)

                    if (!localFile.exists()) {
                        StyledTextOutput o = project.services.get(StyledTextOutputFactory.class).create(DockerRunPlugin)
                        o.withStyle(StyledTextOutput.Style.Error).println("ERROR: Local folder ${localFile} doesn't exist. Mounted volume will not be visible to container")
                        throw new IllegalStateException("Local folder ${localFile} doesn't exist.")
                    }
                    args.add('-v')
                    args.add("${localFile.absolutePath}:${volume.value}")
                }
                args.addAll(runExtension.env.collect{ k, v -> ['-e', "${k}=${v}"] }.flatten())
                args.addAll(['--name', runExtension.name, runExtension.image])
                if (!runExtension.command.isEmpty()) {
                    args.addAll(runExtension.command)
                }
                commandLine args
            }

            dockerStop.with {
                commandLine 'docker', 'stop', runExtension.name
            }

            dockerRemoveContainer.with {
                commandLine 'docker', 'rm', runExtension.name
            }
        }

    }

    private List<String> buildCommandLine(DockerExtension ext) {
        List<String> buildCommandLine = ['docker', 'build']
        if (ext.noCache) {
            buildCommandLine.add '--no-cache'
        }
        if (!ext.buildArgs.isEmpty()) {
            for (Map.Entry<String, String> buildArg : ext.buildArgs.entrySet()) {
                buildCommandLine.addAll('--build-arg', "${buildArg.getKey()}=${buildArg.getValue()}")
            }
        }
        if (!ext.labels.isEmpty()) {
            for (Map.Entry<String, String> label : ext.labels.entrySet()) {
                if (!label.getKey().matches(LABEL_KEY_PATTERN)) {
                    throw new GradleException(String.format("Docker label '%s' contains illegal characters. " +
                            "Label keys must only contain lowercase alphanumberic, `.`, or `-` characters (must match %s).",
                            label.getKey(), LABEL_KEY_PATTERN.pattern()))
                }
                buildCommandLine.addAll('--label', "${label.getKey()}=${label.getValue()}")
            }
        }
        if (ext.pull) {
            buildCommandLine.add '--pull'
        }
        buildCommandLine.addAll(['-t', "${ -> ext.name}", '.'])
        buildCommandLine
    }

    static String computeName(String name, String tag) {
        int lastColon = name.lastIndexOf(':')
        int lastSlash = name.lastIndexOf('/')
        int endIndex
        if (lastColon > lastSlash) {
            endIndex = lastColon
        } else {
            endIndex = name.length()
        }
        name.substring(0, endIndex) + ":" + tag
    }

    private static String ucfirst(String str) {
        StringBuilder sb = new StringBuilder(str)
        sb.replace(0, 1, str.substring(0, 1).toUpperCase())
        sb.toString()
    }
}
