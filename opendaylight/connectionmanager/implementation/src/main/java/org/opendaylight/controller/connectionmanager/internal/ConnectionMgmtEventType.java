package org.opendaylight.controller.connectionmanager.internal;

public enum ConnectionMgmtEventType {
    NODE_DISCONNECTED_FROM_MASTER("Node is disconnected from master"),
    CLUSTER_VIEW_CHANGED("Cluster Composition changed");

    private ConnectionMgmtEventType(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }

    public static ConnectionMgmtEventType fromString(String pName) {
        for(ConnectionMgmtEventType p:ConnectionMgmtEventType.values()) {
            if (p.toString().equals(pName)) {
                return p;
            }
        }
        return null;
    }
}
