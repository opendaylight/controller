package org.opendaylight.controller.configuration;

public enum ConfigurationEvent {
    SAVE("Save"),
    BACKUP("Backup"),
    RESTORE("Restore"),
    DELETE("Delete");

    private ConfigurationEvent(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }

    public static ConfigurationEvent fromString(String pName) {
        for(ConfigurationEvent p:ConfigurationEvent.values()) {
            if (p.toString().equals(pName)) {
                return p;
            }
        }
        return null;
    }
}
