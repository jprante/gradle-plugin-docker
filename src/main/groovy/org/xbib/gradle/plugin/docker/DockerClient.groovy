package org.xbib.gradle.plugin.docker

interface DockerClient {
    /**
     * Build a Docker image from the contents of the given directory.
     * 
     * @param buildDir the directory from which to build the image
     * @param tag the tag to apply to the image
     * @param pull wether to pull latest image or not, true enables the pull, false disables pull
     * @return the output of the command
     */
    String buildImage(File buildDir, String tag, boolean pull)
    
    /**
     * Push the given image to the configured Docker registry.
     * 
     * @param tag the tag of the image to push
     * @return the output of the command
     */
    String pushImage(String tag)
    
    /**
     * Run the given image in a container with the given name.
     * 
     * @param tag the image to run
     * @param containerName the name of the container to create
     * @param detached should the container be run in the background (aka detached)
     * @param autoRemove should the container be removed when execution completes
     * @param env a map containing a collection of environment variables to set
     * @param ports a map containing the ports to publish
     * @param volumes a map containing the volumes to bind
     * @param volumesFrom a list of the containers whose volumes we should mount
     * @param links a list of the containers to which the newly created container should be linked
     * @return the output of the command
     */
    String run(String tag, String containerName, boolean detached, boolean autoRemove,
            Map<String, String> env, Map<String, String> ports, Map<String, String> volumes, 
            List<String> volumesFrom, List<String> links)
}
