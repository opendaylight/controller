package org.opendaylight.controller.featuresservice.northbound;

public class FeatureName {
    String name;

    public FeatureName() {
    }

    public FeatureName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}