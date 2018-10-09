
package org.xbib.gradle.plugin.docker

import org.gradle.api.internal.artifacts.mvnsettings.DefaultLocalMavenRepositoryLocator
import org.gradle.api.internal.artifacts.mvnsettings.DefaultMavenFileLocations
import org.gradle.api.internal.artifacts.mvnsettings.DefaultMavenSettingsProvider
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Ignore

class DockerPluginTests extends AbstractPluginTest {

    def 'fail when missing docker configuration'() {
        given:
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = with('docker').buildAndFail()

        then:
        buildResult.output.contains("name is a required docker configuration item")
    }

    def 'fail with empty container name'() {
        given:
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }
            docker {
                name ''
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = with('docker').buildAndFail()

        then:
        buildResult.output.contains("name is a required docker configuration item")
    }

    def 'check plugin creates a docker container with default configuration'() {
        given:
        String id = 'id1'
        file('Dockerfile') << """
            FROM alpine:3.2
            MAINTAINER ${id}
        """.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name '${id}'
            }
        """.stripIndent()

        when:
        BuildResult buildResult = with('docker').build()

        then:
        buildResult.task(':dockerPrepare').outcome == TaskOutcome.SUCCESS
        buildResult.task(':docker').outcome == TaskOutcome.SUCCESS
        exec("docker inspect --format '{{.Author}}' ${id}") == "'${id}'\n"
        execCond("docker rmi -f ${id}")
    }

    def 'check plugin creates a docker container with non-standard Dockerfile name'() {
        given:
        String id = 'id2'
        file('foo') << """
            FROM alpine:3.2
            MAINTAINER ${id}
        """.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name '${id}'
                dockerfile project.file("foo")
            }
        """.stripIndent()

        when:
        BuildResult buildResult = with('docker').build()

        then:
        buildResult.task(':dockerPrepare').outcome == TaskOutcome.SUCCESS
        buildResult.task(':docker').outcome == TaskOutcome.SUCCESS
        exec("docker inspect --format '{{.Author}}' ${id}") == "'${id}'\n"
        execCond("docker rmi -f ${id}")
    }

    def 'check files are correctly added to docker context'() {
        given:
        String id = 'id3'
        String filename = "foo.txt"
        file('Dockerfile') << """
            FROM alpine:3.2
            MAINTAINER ${id}
            ADD ${filename} /tmp/
        """.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name '${id}'
                files "${filename}"
            }
        """.stripIndent()
        new File(projectDir, filename).createNewFile()
        when:
        BuildResult buildResult = with('docker').build()

        then:
        buildResult.task(':dockerPrepare').outcome == TaskOutcome.SUCCESS
        buildResult.task(':docker').outcome == TaskOutcome.SUCCESS
        exec("docker inspect --format '{{.Author}}' ${id}") == "'${id}'\n"
        execCond("docker rmi -f ${id}")
    }

    def 'Publishes "docker" dependencies via "docker" component'() {
        given:
        file('Dockerfile') << "Foo"
        buildFile << '''
            plugins {
                id 'java'
                id 'maven-publish'
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name 'foo'
            }

            group 'testgroup'
            version '2.3.4'

            dependencies {
                // Should *not* get published to the docker maven publication
                compile 'com.google.guava:guava:18.0'

                // Should get published to the docker maven publication
                docker 'foogroup:barmodule:0.1.2'
            }

            publishing {
                publications {
                    dockerPublication(MavenPublication) {
                        from components.docker
                        artifactId project.name + "-docker"
                    }
                    javaPublication(MavenPublication) {
                        from components.java
                    }
                }
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = with('publishToMavenLocal','--stacktrace').build()
        then:
        buildResult.task(':publishToMavenLocal').outcome == TaskOutcome.SUCCESS
        def publishFolder = new DefaultLocalMavenRepositoryLocator(
            new DefaultMavenSettingsProvider(new DefaultMavenFileLocations())).localMavenRepository.toPath()
            .resolve("testgroup")

        // Check java publication has the right dependencies
        def javaPublishFolder = publishFolder.resolve(projectDir.name).resolve("2.3.4")
        def javaPomFile = javaPublishFolder.resolve(projectDir.name + "-2.3.4.pom")
        def javaPom = javaPomFile.toFile().text
        ["com.google.guava", "guava", "18.0"].each { javaPom.contains(it) }
        ["foogroup", "barmodule"].each { !javaPom.contains(it) }

        // Check docker publication has the right dependencies
        def dockerPublishFolder = publishFolder.resolve(projectDir.name + "-docker").resolve("2.3.4")
        def zipFile = dockerPublishFolder.resolve(projectDir.name + "-docker-2.3.4.zip")
        zipFile.toFile().exists()
        def dockerPomFile = dockerPublishFolder.resolve(projectDir.name + "-docker-2.3.4.pom")
        def dockerPom = dockerPomFile.toFile().text
        ["foogroup", "barmodule", "0.1.2", projectDir.name, "2.3.4"].each { dockerPom.contains(it) }
        !dockerPom.contains("guava")
    }

    def 'tag and push tasks created for each tag'() {
        given:
        String id = 'id5'
        file('Dockerfile') << """
            FROM alpine:3.2
            MAINTAINER ${id}
        """.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name '${id}'
                tags 'latest', 'another'
            }
        """.stripIndent()

        when:
        BuildResult buildResult = with('tasks').build()

        then:
        buildResult.output.contains('dockerTagLatest')
        buildResult.output.contains('dockerTagAnother')
        buildResult.output.contains('dockerPushLatest')
        buildResult.output.contains('dockerPushAnother')
    }


    def 'does not throw if name is configured after evaluation phase'() {
        given:
        String id = 'id6'
        file('Dockerfile') << """
            FROM alpine:3.2
            MAINTAINER ${id}
        """.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                tags 'latest', 'another'
            }

            afterEvaluate {
                docker.name = '${id}'
            }
        """.stripIndent()

        when:
        BuildResult buildResult = with('dockerTag').build()

        then:
        buildResult.task(':dockerPrepare').outcome == TaskOutcome.SUCCESS
        buildResult.task(':docker').outcome == TaskOutcome.SUCCESS
        buildResult.task(':dockerTag').outcome == TaskOutcome.SUCCESS
        exec("docker inspect --format '{{.Author}}' ${id}") == "'${id}'\n"
        exec("docker inspect --format '{{.Author}}' ${id}:latest") == "'${id}'\n"
        exec("docker inspect --format '{{.Author}}' ${id}:another") == "'${id}'\n"
        execCond("docker rmi -f ${id}")
        execCond("docker rmi -f ${id}:another")
        execCond("docker rmi -f ${id}:latest")
        
    }

    def 'running tag task creates images with specified tags'() {
        given:
        String id = 'id6'
        file('Dockerfile') << """
            FROM alpine:3.2
            MAINTAINER ${id}
        """.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name 'fake-service-name'
                tags 'latest', 'another'
            }

            afterEvaluate {
                docker.name = '${id}'
            }

            task printInfo {
                doLast {
                    println "LATEST: \${tasks.dockerTagLatest.commandLine}"
                    println "ANOTHER: \${tasks.dockerTagAnother.commandLine}"
                }
            }
        """.stripIndent()

        when:
        BuildResult buildResult = with('dockerTag', 'printInfo').build()

        then:
        buildResult.output.contains("LATEST: [docker, tag, id6, id6:latest]")
        buildResult.output.contains("ANOTHER: [docker, tag, id6, id6:another]")
        buildResult.task(':dockerPrepare').outcome == TaskOutcome.SUCCESS
        buildResult.task(':docker').outcome == TaskOutcome.SUCCESS
        buildResult.task(':dockerTag').outcome == TaskOutcome.SUCCESS
        exec("docker inspect --format '{{.Author}}' ${id}") == "'${id}'\n"
        exec("docker inspect --format '{{.Author}}' ${id}:latest") == "'${id}'\n"
        exec("docker inspect --format '{{.Author}}' ${id}:another") == "'${id}'\n"
        execCond("docker rmi -f ${id}")
        execCond("docker rmi -f ${id}:latest")
        execCond("docker rmi -f ${id}:another")
    }

    def 'build args are correctly processed'() {
        given:
        String id = 'id7'
        file('Dockerfile') << '''
            FROM alpine:3.2
            ARG BUILD_ARG_NO_DEFAULT
            ARG BUILD_ARG_WITH_DEFAULT=defaultBuildArg
            ENV ENV_BUILD_ARG_NO_DEFAULT $BUILD_ARG_NO_DEFAULT
            ENV ENV_BUILD_ARG_WITH_DEFAULT $BUILD_ARG_WITH_DEFAULT
        '''.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name '${id}'
                buildArgs([BUILD_ARG_NO_DEFAULT: 'gradleBuildArg', BUILD_ARG_WITH_DEFAULT: 'gradleOverrideBuildArg'])
            }
        """.stripIndent()

        when:
        BuildResult buildResult = with('docker').build()

        then:
        buildResult.task(':docker').outcome == TaskOutcome.SUCCESS
        exec("docker inspect --format '{{.Config.Env}}' ${id}").contains('ENV_BUILD_ARG_NO_DEFAULT=gradleBuildArg')
        exec("docker inspect --format '{{.Config.Env}}' ${id}").contains('BUILD_ARG_WITH_DEFAULT=gradleOverrideBuildArg')
        execCond("docker rmi -f ${id}")
    }

    def 'rebuilding an image does it from scratch when "noCache" parameter is set'() {
        given:
        String id = 'id66'
        String filename = "bar.txt"
        file('Dockerfile') << """
            FROM alpine:3.2
            ADD ${filename} /tmp/
        """.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name '${id}'
                files "${filename}"
                noCache true
            }
        """.stripIndent()
        createFile(filename)

        when:
        BuildResult buildResult1 = with('--info', 'docker').build()
        def imageID1 = exec("docker inspect --format=\"{{.Id}}\" ${id}")
        BuildResult buildResult2 = with('--info', 'docker').build()
        def imageID2 = exec("docker inspect --format=\"{{.Id}}\" ${id}")

        then:
        buildResult1.task(':docker').outcome == TaskOutcome.SUCCESS
        buildResult2.task(':docker').outcome == TaskOutcome.SUCCESS
        imageID1 != imageID2
        execCond("docker rmi -f ${id}")
    }

    def 'base image is pulled when "pull" parameter is set'() {
        given:
        String id = 'id8'
        file('Dockerfile') << '''
            FROM alpine:3.2
        '''.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name '${id}'
                pull true
            }
        """.stripIndent()

        when:
        execCond("docker pull alpine:3.2")
        BuildResult buildResult = with('-i', 'docker').build()

        then:
        buildResult.task(':docker').outcome == TaskOutcome.SUCCESS
        buildResult.output.contains 'Pulling from library/alpine'
        execCond("docker rmi -f ${id}")
    }

    def 'can add files from project directory to build context'() {
        given:
        String id = 'id9'
        String filename = "bar.txt"
        file('Dockerfile') << """
            FROM alpine:3.2
            MAINTAINER ${id}
            ADD ${filename} /tmp/
        """.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name '${id}'
                files "bar.txt"
            }
        """.stripIndent()
        createFile(filename)
        when:
        BuildResult buildResult = with('docker').build()

        then:
        buildResult.task(':dockerPrepare').outcome == TaskOutcome.SUCCESS
        buildResult.task(':docker').outcome == TaskOutcome.SUCCESS
        exec("docker inspect --format '{{.Author}}' ${id}") == "'${id}'\n"
        execCond("docker rmi -f ${id}")
    }

    def 'when adding a project-dir file and a Tar file, then they both end up (unzipped) in the docker image'() {
        given:
        String id = 'id10'
        createFile('from_project')
        createFile('from_tgz')

        file('Dockerfile') << """
            FROM alpine:3.2
            MAINTAINER id
            ADD foo.tgz /tmp/
            ADD from_project /tmp/
        """.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            task myTgz(type: Tar) {
                destinationDir project.buildDir
                baseName 'foo'
                extension = 'tgz'
                compression = Compression.GZIP
                into('.') {
                    from 'from_tgz'
                }
            }

            docker {
                name '${id}'
                files tasks.myTgz.outputs, 'from_project'
            }
        """.stripIndent()
        when:
        BuildResult buildResult = with('docker').build()

        then:
        buildResult.task(':myTgz').outcome == TaskOutcome.SUCCESS
        buildResult.task(':dockerPrepare').outcome == TaskOutcome.SUCCESS
        buildResult.task(':docker').outcome == TaskOutcome.SUCCESS
        execCond("docker rmi -f ${id}")
    }

    def 'can build Docker image from standard Gradle distribution plugin'() {
        given:
        String id = 'id11'

        file('Dockerfile') << """
            FROM alpine:3.2
            MAINTAINER id
            ADD * /tmp/
        """.stripIndent()

        file('src/main/java/test/Test.java') << '''
        package test;
        public class Test { public static void main(String[] args) {} }
        '''.stripIndent()

        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
                id 'java'
                id 'application'
            }
            mainClassName = 'test.Test'

            docker {
                name '${id}'
                files tasks.distTar.outputs
            }
        """.stripIndent()
        when:
        BuildResult buildResult = with('docker').build()

        then:
        buildResult.task(':distTar').outcome == TaskOutcome.SUCCESS
        buildResult.task(':dockerPrepare').outcome == TaskOutcome.SUCCESS
        buildResult.task(':docker').outcome == TaskOutcome.SUCCESS
        execCond("docker rmi -f ${id}")
    }

    def 'check labels are correctly applied to image'() {
        given:
        String id = 'id10'
        file('Dockerfile') << """
            FROM alpine:3.2
        """.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name '${id}'
                labels 'test-label': 'test-value', 'another.label': 'another.value'
            }
        """.stripIndent()
        when:
        BuildResult buildResult = with('docker').build()

        then:
        buildResult.task(':docker').outcome == TaskOutcome.SUCCESS
        exec("docker inspect --format '{{.Config.Labels}}' ${id}").contains("test-label")
        execCond("docker rmi -f ${id}")
    }

    def 'fail with bad label key character'() {
        given:
        file('Dockerfile') << """
            FROM alpine:3.2
        """.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name 'test-bad-labels'
                labels 'test_label': 'test_value'
            }
        """.stripIndent()
        when:
        BuildResult buildResult = with('docker').buildAndFail()

        then:
        buildResult.output.contains("Docker label 'test_label' contains illegal characters. Label keys " +
            "must only contain lowercase alphanumberic, `.`, or `-` characters (must match " +
            "^[a-z0-9.-]*\$).")
    }

    def 'check if compute name replaces the name correctly'() {
        expect:
        DockerPlugin.computeName(name, tag) == result

        where:
        name             | tag      | result
        "v1"             | "latest" | "v1:latest"
        "v1:1"           | "latest" | "v1:latest"
        "host/v1"        | "latest" | "host/v1:latest"
        "host/v1:1"      | "latest" | "host/v1:latest"
        "host:port/v1"   | "latest" | "host:port/v1:latest"
        "host:port/v1:1" | "latest" | "host:port/v1:latest"
    }

    def 'can add entire directories via copyspec'() {
        given:
        String id = 'id1'
        createFile("myDir/bar")
        file('Dockerfile') << """
            FROM alpine:3.2
            MAINTAINER ${id}
            ADD myDir /myDir/
        """.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name '${id}'
                copySpec.from("myDir").into("myDir")
            }
        """.stripIndent()
        when:
        BuildResult buildResult = with('docker').build()

        then:
        buildResult.task(':dockerPrepare').outcome == TaskOutcome.SUCCESS
        file("build/docker/myDir/bar").exists()
    }

    def 'Generates docker-compose.yml from template with version strings replaced'() {
        given:
        file('Dockerfile') << "Foo"
        file("docker-compose.yml.template") << '''
            service1:
              image: 'repository/service1:{{com.google.guava:guava}}'
            service2:
              image: 'repository/service2:{{org.slf4j:slf4j-api}}'
            current-service:
              image: '{{currentImageName}}'
        '''.stripIndent()
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            repositories {
                jcenter()
            }

            dockerCompose {
                templateTokens(['currentImageName': 'snapshot.docker.registry/current-service:1.0.0-1-gabcabcd'])
            }

            dependencies {
                docker 'io.dropwizard:dropwizard-jackson:0.8.2'
                  // transitive dependencies: com.google.guava:guava:18.0, org.slf4j:slf4j-api:1.7.10
                docker 'com.google.guava:guava:17.0'  // should bump to 18.0 via the above
            }

        '''.stripIndent()

        when:
        BuildResult buildResult = with('generateDockerCompose', "--stacktrace").build()
        then:
        buildResult.task(':generateDockerCompose').outcome == TaskOutcome.SUCCESS
        def dockerComposeText = file("docker-compose.yml").text
        dockerComposeText.contains("repository/service1:18.0")
        dockerComposeText.contains("repository/service2:1.7.10")
        dockerComposeText.contains("image: 'snapshot.docker.registry/current-service:1.0.0-1-gabcabcd'")
    }

    def 'Fails if docker-compose.yml.template has unmatched version tokens'() {
        given:
        file('Dockerfile') << "Foo"
        file("docker-compose.yml.template") << '''
            service1:
              image: 'repository/service1:{{foo:bar}}'
        '''.stripIndent()
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            repositories {
                jcenter()
            }

            dependencies {
                docker 'com.google.guava:guava:17.0'  // should bump to 18.0 via the above
            }

        '''.stripIndent()

        when:
        BuildResult buildResult = with('generateDockerCompose', '--stacktrace').buildAndFail()
        then:
        buildResult.output.contains("failed to resolve Docker dependencies declared in")
        buildResult.output.contains("{{foo:bar}}")
    }

    def 'docker-compose template and file can have custom locations'() {
        given:
        file('Dockerfile') << "Foo"
        file("templates/customTemplate.yml") << '''
            nothing
        '''.stripIndent()
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            repositories {
                jcenter()
            }

            dockerCompose {
                template 'templates/customTemplate.yml'
                dockerComposeFile 'compose-files/customDockerCompose.yml'
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = with('generateDockerCompose', "--stacktrace").build()
        then:
        buildResult.task(':generateDockerCompose').outcome == TaskOutcome.SUCCESS
        file("compose-files/customDockerCompose.yml").exists()
    }

    def 'Fails if template is configured but does not exist'() {
        given:
        file('Dockerfile') << "Foo"
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            dockerCompose {
                template 'templates/customTemplate.yml'
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = with('generateDockerCompose').buildAndFail()
        then:
        buildResult.output.contains("could not find specified template file")
    }

    def 'docker-compose is executed and fails on invalid file'() {
        given:
        file('docker-compose.yml') << "FOO"
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }
        '''.stripIndent()
        when:
        BuildResult buildResult = with('dockerComposeUp', '--stacktrace').buildAndFail()
        then:
        buildResult.output.contains("Top level")
    }

    def 'docker-compose successfully creates docker image'() {
        given:
        execCond('docker rm helloworld')
        file('docker-compose.yml') << '''
            version: "2"
            services:
              hello:
                container_name: "helloworld"
                image: "alpine"
                command: touch /test/foobarbaz
                volumes:
                  - ./:/test
        '''.stripIndent()
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }
        '''.stripIndent()
        when:
        with('dockerComposeUp','--stacktrace').build()
        then:
        file("foobarbaz").exists()
    }

    def 'docker-compose successfully creates docker image from custom file'() {
        given:
        execCond('docker rm helloworld2')
        file('test-file.yml') << '''
            version: "2"
            services:
              hello:
                container_name: "helloworld2"
                image: "alpine"
                command: touch /test/qux
                volumes:
                  - ./:/test
        '''.stripIndent()
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            dockerCompose {
              dockerComposeFile "test-file.yml"
            }
        '''.stripIndent()
        when:
        with('dockerComposeUp').build()
        then:
        file("qux").exists()
    }

    def 'can run, status, and stop a container made by the docker plugin' () {
        given:
        file('Dockerfile') << '''
            FROM alpine:3.2
            CMD sleep 1000
        '''.stripIndent()
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name 'foo-image:latest'
            }

            dockerRun {
                name 'foo'
                image 'foo-image:latest'
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = with('docker', 'dockerRemoveContainer', 'dockerRun', 'dockerRunStatus', 'dockerStop').build()
        BuildResult offline = with('dockerRunStatus', 'dockerRemoveContainer').build()

        then:
        buildResult.task(':docker').outcome == TaskOutcome.SUCCESS
        buildResult.task(':dockerRemoveContainer').outcome == TaskOutcome.SUCCESS

        buildResult.task(':dockerRun').outcome == TaskOutcome.SUCCESS
        // CircleCI build nodes print a WARNING
        buildResult.output =~ /(?m):dockerRun(WARNING:.*\n)?\n[A-Za-z0-9]+/

        buildResult.task(':dockerRunStatus').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ /(?m):dockerRunStatus\nDocker container 'foo' is RUNNING./

        buildResult.task(':dockerStop').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ /(?m):dockerStop\nfoo/

        offline.task(':dockerRunStatus').outcome == TaskOutcome.SUCCESS
        offline.output =~ /(?m):dockerRunStatus\nDocker container 'foo' is STOPPED./

        execCond('docker rmi -f foo-image')
    }

    def 'can run, status, and stop a container' () {
        given:
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            dockerRun {
                name 'bar'
                image 'alpine:3.2'
                ports '8080'
                command 'sleep', '1000'
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = with('dockerRemoveContainer', 'dockerRun', 'dockerRunStatus', 'dockerStop').build()
        BuildResult offline = with('dockerRunStatus', 'dockerRemoveContainer').build()

        then:
        buildResult.task(':dockerRemoveContainer').outcome == TaskOutcome.SUCCESS

        buildResult.task(':dockerRun').outcome == TaskOutcome.SUCCESS
        // CircleCI build nodes print a WARNING
        buildResult.output =~ /(?m):dockerRun(WARNING:.*\n)?\n[A-Za-z0-9]+/

        buildResult.task(':dockerRunStatus').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ /(?m):dockerRunStatus\nDocker container 'bar' is RUNNING./

        buildResult.task(':dockerStop').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ /(?m):dockerStop\nbar/

        offline.task(':dockerRunStatus').outcome == TaskOutcome.SUCCESS
        offline.output =~ /(?m):dockerRunStatus\nDocker container 'bar' is STOPPED./
    }

    def 'can run container with configured network' () {
        given:
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }
            dockerRun {
                name 'bar-hostnetwork'
                image 'alpine:3.2'
                network 'host'
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = with('dockerRemoveContainer', 'dockerRun', 'dockerNetworkModeStatus').build()

        then:
        buildResult.task(':dockerRemoveContainer').outcome == TaskOutcome.SUCCESS

        buildResult.task(':dockerRun').outcome == TaskOutcome.SUCCESS

        buildResult.output =~ /(?m):dockerNetworkModeStatus\nDocker container 'bar-hostnetwork' is configured to run with 'host' network mode./
    }

    def 'can optionally not daemonize'() {
        given:
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            dockerRun {
                name 'bar-nodaemonize'
                image 'alpine:3.2'
                ports '8080'
                command 'echo', '"hello world"'
                daemonize false
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = with('dockerRemoveContainer', 'dockerRun', 'dockerRunStatus').build()

        then:
        buildResult.task(':dockerRemoveContainer').outcome == TaskOutcome.SUCCESS

        buildResult.task(':dockerRun').outcome == TaskOutcome.SUCCESS

        buildResult.task(':dockerRunStatus').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ /(?m):dockerRunStatus\nDocker container 'bar-nodaemonize' is STOPPED./
    }

    def 'can mount volumes'() {

        given:
        File testFolder = directory("test")
        file('Dockerfile') << '''
            FROM alpine:3.2

            RUN mkdir /test
            VOLUME /test
            CMD cat /test/testfile
        '''.stripIndent()
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name 'foo-image:latest'
            }

            dockerRun {
                name 'foo'
                image 'foo-image:latest'
                volumes "test": "/test"
                daemonize false
            }
        '''.stripIndent()

        when:
        new File(testFolder, "testfile").text = "HELLO WORLD\n"
        BuildResult buildResult = with('docker', 'dockerRemoveContainer', 'dockerRun', 'dockerRunStatus').build()

        then:
        buildResult.task(':dockerRemoveContainer').outcome == TaskOutcome.SUCCESS

        buildResult.task(':dockerRun').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ /(?m)HELLO WORLD/
        buildResult.task(':dockerRunStatus').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ /(?m):dockerRunStatus\nDocker container 'foo' is STOPPED./
    }

    @Ignore
    // The path /var/folders/v5/vvl6jhqs0cz_krl674_yb6980000gn/T/groovy-generated-2890137687884187545-tmpdir/test
    // is not shared from OS X and is not known to Docker.
    // You can configure shared paths from Docker -> Preferences... -> File Sharing.
    // See https://docs.docker.com/docker-for-mac/osxfs/#namespaces for more info.
    def 'can mount volumes specified with an absolute path'() {
        given:
        File testFolder = directory("test")
        file('Dockerfile') << '''
            FROM alpine:3.2

            RUN mkdir /test
            VOLUME /test
            CMD cat /test/testfile
        '''.stripIndent()
        buildFile << """
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name 'foo-image:latest'
            }

            dockerRun {
                name 'foo'
                image 'foo-image:latest'
                volumes "${testFolder.absolutePath}": "/test"
                daemonize false
            }
        """.stripIndent()

        when:
        new File(testFolder, "testfile").text = "HELLO WORLD\n"
        BuildResult buildResult = with('docker', 'dockerRemoveContainer', 'dockerRun', 'dockerRunStatus').build()

        then:
        buildResult.task(':dockerRemoveContainer').outcome == TaskOutcome.SUCCESS

        buildResult.task(':dockerRun').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ /(?m)HELLO WORLD/
        buildResult.task(':dockerRunStatus').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ /(?m):dockerRunStatus\nDocker container 'foo' is STOPPED./
    }

    def 'can run with environment variables'() {
        given:
        file('Dockerfile') << '''
            FROM alpine:3.2

            RUN mkdir /test
            VOLUME /test
            ENV MYVAR1 QUUW
            ENV MYVAR2 QUUX
            ENV MYVAR3 QUUY
            ENV MYVAR4 QUUZ
            CMD echo "\$MYVAR1 = \$MYVAR2 = \$MYVAR3 = \$MYVAR4"
        '''.stripIndent()
        buildFile << '''
            plugins {
                id 'org.xbib.gradle.plugin.docker'
            }

            docker {
                name 'foo-image:latest'
            }

            dockerRun {
                name 'foo-envvars'
                image 'foo-image:latest'
                env 'MYVAR1': 'FOO', 'MYVAR2': 'BAR', 'MYVAR4': 'ZIP'
                daemonize false
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = with('docker', 'dockerRemoveContainer', 'dockerRun', 'dockerRunStatus').build()

        then:
        buildResult.task(':dockerRemoveContainer').outcome == TaskOutcome.SUCCESS

        buildResult.task(':dockerRun').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ /(?m)FOO = BAR = QUUY = ZIP/
        buildResult.task(':dockerRunStatus').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ /(?m):dockerRunStatus\nDocker container 'foo-envvars' is STOPPED./
    }

    def isLinux() {
        return System.getProperty("os.name") =~ /(?i).*linux.*/
    }
}
