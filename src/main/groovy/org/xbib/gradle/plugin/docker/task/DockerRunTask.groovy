package org.xbib.gradle.plugin.docker.task

import org.gradle.api.tasks.TaskAction
import org.xbib.gradle.plugin.docker.DockerClient

class DockerRunTask extends DockerTaskBase {

    String containerName
    
    boolean detached = false

    boolean autoRemove = false
    
    Map<String, String> env
    
    Map<String, String> ports
    
    Map<String, String> volumes
    
    List<String> volumesFrom
    
    List<String> links
    
    DockerRunTask() {
        env = [:]
        ports = [:]
        volumes = [:]
        volumesFrom = []
        links = []
    }
    
    @TaskAction
    void run() {
        DockerClient client = getClient()
        client.run(getImageTag(), getContainerName(), getDetached(), getAutoRemove(), getEnv(), 
            getPorts(), getVolumes(), getVolumesFrom(), getLinks())
    }
    
    void env(String key, String value) {
        env.put(key, value)
    }
    
    void publish(String host, String container) {
        ports.put(host, container)
    }
    
    void volume(String host, String container) {
        volumes.put(host, container)
    }
    
    void volumesFrom(String containerName) {
        volumesFrom.add(containerName)
    }
    
    void link(String containerName) {
        links.add(containerName)
    }
}
