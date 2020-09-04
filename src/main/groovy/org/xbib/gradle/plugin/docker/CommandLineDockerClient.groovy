package org.xbib.gradle.plugin.docker

import org.gradle.api.GradleException

class CommandLineDockerClient implements DockerClient {

    private final String binary

    CommandLineDockerClient(String binary) {
        this.binary = binary
    }

    @Override
    String buildImage(File buildDir, String tag, Boolean pull) {
        def cmdLine = pull ?
                [binary, "build", "--pull=${pull}", "-t", tag, buildDir.toString() ] :
                [binary, "build", "-t", tag, buildDir.toString()]
        executeAndWait(cmdLine)
    }

    @Override
    String pushImage(String tag) {
        def cmdLine = [binary, "push", tag]
        executeAndWait(cmdLine)
    }

    private static String executeAndWait(List<String> cmdLine) {
        def process = cmdLine.execute()
        process.waitForProcessOutput(System.out, System.err)
        if (process.exitValue()) {
            throw new GradleException("Docker execution failed\nCommand line [${cmdLine}]")
        }
        return "Done"
    }

    @Override
    String run(String tag, String containerName, Boolean detached, Boolean autoRemove,
            Map<String, String> env,
            Map<String, String> ports, Map<String, String> volumes, List<String> volumesFrom,
            List<String> links) {
        def detachedArg = detached ? '-d' : ''
        def removeArg = autoRemove ? '--rm' : ''
        List<String> cmdLine = [binary, "run", detachedArg, removeArg, "--name" , containerName]
        cmdLine = appendArguments(cmdLine, env, "--env", '=')
        cmdLine = appendArguments(cmdLine, ports, "--publish")
        cmdLine = appendArguments(cmdLine, volumes, "--volume")
        cmdLine = appendArguments(cmdLine, volumesFrom, "--volumes-from")
        cmdLine = appendArguments(cmdLine, links, "--link")
        cmdLine.add(tag)
        executeAndWait(cmdLine)
    }

    private static List<String> appendArguments(List<String> cmdLine, Map<String, String> map, String option,
            String separator = ':') {
        map.each { key, value ->
            cmdLine.add(option);
            cmdLine.add("${key}${separator}${value}")
        }
        return cmdLine
    }

    private static List<String> appendArguments(List<String> cmdLine, List<String> list, String option) {
        list.each {
            cmdLine.add(option);
            cmdLine.add(it);
        }
        return cmdLine
    }
}
