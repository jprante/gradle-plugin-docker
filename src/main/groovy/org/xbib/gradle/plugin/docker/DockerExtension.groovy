package org.xbib.gradle.plugin.docker

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec

import java.text.MessageFormat

class DockerExtension {

    private static final String DEFAULT_DOCKERFILE_PATH = 'Dockerfile'

    private String name = null

    private File dockerfile = null

    private String dockerComposeTemplate = 'docker-compose.yml.template'

    private String dockerComposeFile = 'docker-compose.yml'

    private Set<Task> dependencies = new LinkedHashSet<>()

    private Set<String> tags = new LinkedHashSet<>()

    private Map<String, String> labels = new LinkedHashMap<>()

    private Map<String, String> buildArgs = new LinkedHashMap<>()

    private boolean pull = false

    private boolean noCache = false

    private File resolvedDockerfile = null

    private File resolvedDockerComposeTemplate = null

    private File resolvedDockerComposeFile = null

    private final Project project

    private final CopySpec copySpec

    DockerExtension(Project project) {
        this.project = project
        this.copySpec = project.copySpec()
    }

    void resolvePathsAndValidate() {
        if (dockerfile != null) {
            resolvedDockerfile = dockerfile
        } else {
            resolvedDockerfile = project.file(DEFAULT_DOCKERFILE_PATH)
        }
        resolvedDockerComposeFile = project.file(dockerComposeFile)
        resolvedDockerComposeTemplate = project.file(dockerComposeTemplate)
    }

    void setName(String name) {
        this.name = name
    }

    String getName() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name is a required docker configuration item")
        }
        name
    }

    void setDockerfile(File dockerfile) {
        this.dockerfile = dockerfile
    }

    void setDockerComposeTemplate(String dockerComposeTemplate) {
        this.dockerComposeTemplate = dockerComposeTemplate
        if (!project.file(dockerComposeTemplate).exists()) {
            throw new IllegalArgumentException(MessageFormat.format("could not find specified template file: %s",
                    project.file(dockerComposeTemplate)))
        }
    }

    void setDockerComposeFile(String dockerComposeFile) {
        this.dockerComposeFile = dockerComposeFile
    }

    void dependsOn(Task... args) {
        this.dependencies = new LinkedHashSet<>(Arrays.asList(args))
    }

    Set<Task> getDependencies() {
        dependencies
    }

    void files(Object... files) {
        copySpec.from(files)
    }

    Set<String> getTags() {
        tags
    }

    void tags(String... args) {
        this.tags = new LinkedHashSet<>(Arrays.asList(args))
    }

    Map<String, String> getLabels() {
        labels
    }

    void labels(Map<String, String> labels) {
        this.labels = new LinkedHashMap<>(labels)
    }

    File getResolvedDockerfile() {
        return resolvedDockerfile
    }

    File getResolvedDockerComposeTemplate() {
        resolvedDockerComposeTemplate
    }

    File getResolvedDockerComposeFile() {
        resolvedDockerComposeFile
    }

    CopySpec getCopySpec() {
        copySpec
    }

    Map<String, String> getBuildArgs() {
        buildArgs
    }

    void buildArgs(Map<String, String> buildArgs) {
        this.buildArgs = new LinkedHashMap<>(buildArgs)
    }

    boolean getPull() {
        pull
    }

    void pull(boolean pull) {
        this.pull = pull
    }

    boolean getNoCache() {
        noCache
    }

    void noCache(boolean noCache) {
        this.noCache = noCache
    }
}
