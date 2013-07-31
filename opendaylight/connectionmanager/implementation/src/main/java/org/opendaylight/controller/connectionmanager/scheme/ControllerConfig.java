package org.opendaylight.controller.connectionmanager.scheme;

import java.net.InetAddress;

/**
 * Configuration object that can be used to prioritize or add weight to a given controller.
 * This can be potentially used by the Connection management scheme algorithms.
 *
 * This is currently not used.
 *
 */
public class ControllerConfig {
    private InetAddress controllerId;
    private int priority;
    private int weight;

    public ControllerConfig(InetAddress controllerId, int priority, int weight) {
        this.controllerId = controllerId;
        this.priority = priority;
        this.weight = weight;
    }

    public InetAddress getControllerId() {
        return controllerId;
    }
    public int getPriority() {
        return priority;
    }
    public int getWeight() {
        return weight;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((controllerId == null) ? 0 : controllerId.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ControllerConfig other = (ControllerConfig) obj;
        if (controllerId == null) {
            if (other.controllerId != null)
                return false;
        } else if (!controllerId.equals(other.controllerId))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "ControllerConfig [controllerId=" + controllerId + ", priority="
                + priority + ", weight=" + weight + "]";
    }
}
