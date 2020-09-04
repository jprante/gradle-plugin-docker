package org.xbib.gradle.plugin.docker

import java.nio.file.Files
import java.nio.file.Path

class Dockerfile {

    final List<String> instructions

    final File workingDir

    final List<Closure> stagingBacklog

    final Closure copyCallback

    final Object resolvePathCallback

    List<String> baseInstructions

    Dockerfile(File workingDir) {
        this(workingDir, { String path -> new File(path) }, { -> })
    }

    Dockerfile(File workingDir, resolvePathCallback, copyCallback) {
        this.workingDir = workingDir
        this.resolvePathCallback = resolvePathCallback
        this.copyCallback = copyCallback
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

    void add(URL source, String destination='/') {
        append("ADD ${source.toString()} ${destination}")
    }

    void add(String source, String destination='/') {
        if(isUrl(source)) {
            append("ADD ${source} ${destination}")
        } else {
            add(resolvePathCallback(source), destination)
        }
    }

    void add(File source, String destination='/') {
        File target
        if (source.isDirectory()) {
            target = new File(workingDir, source.name)
        }
        else {
            target = workingDir
        }
        stagingBacklog.add { ->
            copyCallback {
                from source
                into target
            }
        }
        append("ADD ${source.name} ${destination}")
    }

    void add(Closure copySpec) {
        File tarFile = new File(workingDir, "add_${instructions.size()+1}.tar")
        stagingBacklog.add { ->
            createTarArchive(tarFile, copySpec)
        }
        instructions.add("ADD ${tarFile.name} ${'/'}")
    }

    void copy(String source, String destination='/') {
        copy(resolvePathCallback(source), destination)
    }

    void copy(File source, String destination='/') {
        File target
        if (source.isDirectory()) {
            target = new File(workingDir, source.name)
        }
        else {
            target = workingDir
        }
        stagingBacklog.add { ->
            copyCallback {
                from source
                into target
            }
        }
        this.append("COPY ${source.name} ${destination}")
    }

    void copy(Closure copySpec) {
        File tarFile = new File(workingDir, "copy_${instructions.size()+1}.tar")
        stagingBacklog.add { ->
            createTarArchive(tarFile, copySpec)
        }
        instructions.add("COPY ${tarFile.name} ${'/'}")
    }

    private void createTarArchive(File tarFile, Closure copySpec) {
        Path tmpDir = Files.createTempDirectory('gradle-plugin-docker')
        try {
            copyCallback {
                with {
                    into('/') {
                        with copySpec
                    }
                }
                into tmpDir.toFile()
            }
            new AntBuilder().tar(destfile: tarFile, basedir: tmpDir.toFile())
        } finally {
            tmpDir.deleteDir()
        }
    }

    void label(Map labels) {
         if(labels) {
             instructions.add("LABEL " + labels.collect { k,v -> "\"$k\"=\"$v\"" }.join(' '))
         }
    }

    List<String> getInstructions() {
        (baseInstructions + instructions)*.toString()
    }

    boolean hasBase() {
        baseInstructions.size() > 0
    }

    private static boolean isUrl(String url) {
        try {
            new URL(url)
        } catch (MalformedURLException e) {
            return false
        }
        return true;
    }
}
