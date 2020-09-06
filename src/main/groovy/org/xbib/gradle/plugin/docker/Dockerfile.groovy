package org.xbib.gradle.plugin.docker

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class Dockerfile {

    private static final Logger logger = Logging.getLogger(Dockerfile)

    final Project project

    final List<String> instructions

    final File workingDir

    final List<Closure> stagingBacklog

    final Closure copyCallback

    List<String> baseInstructions

    Dockerfile(Project project, File workingDir) {
        this.project = project
        this.workingDir = workingDir
        this.copyCallback = { Closure copyClosure -> project.copy(copyClosure) }
        this.baseInstructions = []
        this.instructions = []
        this.stagingBacklog = []
    }

    def methodMissing(String name, args) {
        if (name.toLowerCase() != name) {
            return callWithLowerCaseName(name, args)
        }
        append("${name.toUpperCase()} ${args.join(' ')}")
    }

    File file(Object name) {
        project.file(name)
    }

    Dockerfile append(def instruction) {
        instructions.add(instruction.toString())
        this
    }

    Dockerfile appendAll(List instructions) {
        instructions.addAll(instructions*.toString())
        this
    }

    void writeToFile(File destination) {
        destination.withWriter { out ->
            instructions.each() { line ->
                out.writeLine(line)
            }
        }
    }

    def callWithLowerCaseName(String name, args) {
        String s = name.toLowerCase()
        this."$s"(*args)
    }

    void extendDockerfile(File baseFile) {
        baseInstructions = baseFile as String[]
    }

    void from(String baseImage) {
        baseInstructions = ["FROM ${baseImage}"]
    }

    void cmd(List cmd) {
        append('CMD ["' + cmd.join('", "') + '"]')
    }

    void entrypoint(List cmd) {
        append('ENTRYPOINT ["' + cmd.join('", "') + '"]')
    }

    void add(File source, String destination='/') {
        File target
        if (source.isDirectory()) {
            target = new File(workingDir, source.name)
        }
        else {
            target = workingDir
        }
        if (source.absoluteFile != target.absoluteFile) {
            logger.info "source = ${source} target = ${target}"
            stagingBacklog.add { ->
                copyCallback {
                    from source
                    into target
                }
            }
        }
        append("ADD ${source.name} ${destination}")
    }

    void copy(File source, String destination='/') {
        File target
        if (source.isDirectory()) {
            target = new File(workingDir, source.name)
        }
        else {
            target = workingDir
        }
        if (source.absoluteFile != target.absoluteFile) {
            stagingBacklog.add { ->
                copyCallback {
                    from source
                    into target
                }
            }
        }
        append("COPY ${source.name} ${destination}")
    }

    void label(Map labels) {
        if (labels) {
            instructions.add("LABEL " + labels.collect { k,v -> "\"$k\"=\"$v\"" }.join(' '))
        }
    }

    List<String> getInstructions() {
        (baseInstructions + instructions)*.toString()
    }

    boolean hasBase() {
        baseInstructions.size() > 0
    }
}
