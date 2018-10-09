package org.xbib.gradle.plugin.docker

class DockerRunExtension {

    private String name

    private String image

    private String network

    private List<String> command = new ArrayList<>()

    private Set<String> ports = new LinkedHashSet<>()

    private Map<String, String> env = new LinkedHashMap<>()

    private Map<Object, String> volumes = new LinkedHashMap<>()

    private boolean daemonize = true

    private boolean clean = false

    String getName() {
        return name
    }

    void setName(String name) {
        this.name = name
    }

    boolean getDaemonize() {
        return daemonize
    }

    void setDaemonize(boolean daemonize) {
        this.daemonize = daemonize
    }

    boolean getClean() {
        return clean
    }

    void setClean(boolean clean) {
        this.clean = clean
    }

    String getImage() {
        return image
    }

    void setImage(String image) {
        this.image = image
    }

    Set<String> getPorts() {
        return ports
    }

    List<String> getCommand() {
        return command
    }

    Map<Object, String> getVolumes() {
        return volumes
    }

    void command(String... command) {
        this.command = Arrays.asList(command)
    }

    void setNetwork(String network) {
        this.network = network
    }

    String getNetwork() {
        return network
    }

    private void setEnvSingle(String key, String value) {
        this.env.put(key, value)
    }

    void env(Map<String, String> env) {
        this.env = new LinkedHashMap<String, String>(env)
    }

    Map<String, String> getEnv() {
        return env
    }

    void ports(String... ports) {
        for (String port : ports) {
            String[] mapping = port.split(':', 2)
            if (mapping.length == 1) {
                checkPortIsValid(mapping[0])
                this.ports.add("${mapping[0]}:${mapping[0]}")
            } else {
                checkPortIsValid(mapping[0])
                checkPortIsValid(mapping[1])
                this.ports.add("${mapping[0]}:${mapping[1]}")
            }
        }
    }

    void volumes(Map<Object, String> volumes) {
      this.volumes = new LinkedHashMap<>(volumes)
    }

    private static void checkPortIsValid(String port) {
        int val = Integer.parseInt(port)
        if (!(0 < val && val <= 65536)) {
            throw new IllegalArgumentException("port must be in the range [1,65536]")
        }
    }

}
