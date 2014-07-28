package org.opendaylight.controller.featuresservice.northbound;

public class FeatureJson {
    String id;
    String name;
    String description;
    String details;
    String version;
    Boolean hasVersion;
    String resolver;
    Boolean isInstall;

    public FeatureJson(String id, String name, String description, String details,
            String version, Boolean hasVersion, String resolver,
            Boolean isInstall) {
        super();
        this.id = id;
        this.name = name;
        this.description = description;
        this.details = details;
        this.version = version;
        this.hasVersion = hasVersion;
        this.resolver = resolver;
        this.isInstall = isInstall;
    }
    public Boolean getIsInstall() {
        return isInstall;
    }
    public void setIsInstall(Boolean isInstall) {
        this.isInstall = isInstall;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getDetails() {
        return details;
    }
    public void setDetails(String details) {
        this.details = details;
    }
    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public Boolean getHasVersion() {
        return hasVersion;
    }
    public void setHasVersion(Boolean hasVersion) {
        this.hasVersion = hasVersion;
    }
    public String getResolver() {
        return resolver;
    }
    public void setResolver(String resolver) {
        this.resolver = resolver;
    }
}