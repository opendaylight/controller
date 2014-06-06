package org.opendaylight.controller.versionapi.northbound;

public class Version {
    private String name;
    private String version;
    private String timestamp;
    private String stream;

    public Version(){}

    public Version(String name, String version, String timestamp) {
        super();
        this.name = name;
        this.version = version;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }
}
