/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.ForwardingMode;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.Tier;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

/**
 * The class describes a switch configuration
 */
public class SwitchConfig implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    private final String nodeId;
    private final Map<String, Property> nodeProperties;

    public SwitchConfig(String nodeId, Map<String, Property> nodeProperties) {
        this.nodeId = nodeId;
        this.nodeProperties = (nodeProperties == null) ? new HashMap<String, Property>()
                : new HashMap<String, Property>(nodeProperties);
    }

    @Deprecated
    public SwitchConfig(String nodeId, String description, String tier, String mode) {
        this.nodeId = nodeId;
        this.nodeProperties = new HashMap<String, Property>();
        Property desc = new Description(description);
        this.nodeProperties.put(desc.getName(), desc);
        Property nodeTier = new Tier(Integer.valueOf(tier));
        this.nodeProperties.put(nodeTier.getName(), nodeTier);
        Property forwardingMode = new ForwardingMode(Integer.valueOf(mode));
        this.nodeProperties.put(forwardingMode.getName(), forwardingMode);
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public Map<String, Property> getNodeProperties() {
        return new HashMap<String, Property>(this.nodeProperties);
    }

    public Property getProperty(String PropName) {
        return nodeProperties.get(PropName);
    }

    /**
     * This method returns the configured description of the node
     *
     * @return Configured description
     *
     * @deprecated replaced by getProperty(Description.propertyName)
     */
    @Deprecated
    public String getNodeDescription() {
        Description description = (Description) getProperty(Description.propertyName);
        return (description == null) ? null : description.getValue();
    }

    /**
     * This method returns the configured Tier of a node
     *
     * @return Configured tier
     *
     * @deprecated replaced by getProperty(Tier.TierPropName)
     */
    @Deprecated
    public String getTier() {
        Tier tier = (Tier) getProperty(Tier.TierPropName);
        return (tier == null) ? null : String.valueOf(tier.getValue());
    }

    /**
     * This method returns the configured Forwarding Mode of a node
     *
     * @return Configured Forwarding Mode
     *
     * @deprecated replaced by getProperty(ForwardingMode.name)
     */
    @Deprecated
    public String getMode() {
        ForwardingMode forwardingMode = (ForwardingMode) getProperty(ForwardingMode.name);
        return (forwardingMode == null) ? null : String.valueOf(forwardingMode.getValue());
    }

    /**
     * This method returns true, if the configured forwarding mode is proactive,
     * else false
     *
     * @return true, if the configured forwarding mode is proactive, else false
     *
     * @deprecated replaced by isProactive() API of ForwardingMode property
     */
    @Deprecated
    public boolean isProactive() {
        return Integer.parseInt(getMode()) == ForwardingMode.PROACTIVE_FORWARDING;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public Status validate() {
        Status validCheck = validateNodeId();
        if (validCheck.isSuccess()) {
            validCheck = validateNodeProperties();
        }
        return validCheck;
    }

    private Status validateNodeId() {
        if (nodeId == null || nodeId.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "NodeId cannot be empty");
        }
        return new Status(StatusCode.SUCCESS);
    }

    private Status validateNodeProperties() {
        if (nodeProperties == null) {
            return new Status(StatusCode.BADREQUEST, "nodeProperties cannot be null");
        }
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
        result = prime * result + ((nodeProperties == null) ? 0 : nodeProperties.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SwitchConfig other = (SwitchConfig) obj;
        if (nodeId == null) {
            if (other.nodeId != null) {
                return false;
            }
        } else if (!nodeId.equals(other.nodeId)) {
            return false;
        }
        if (nodeProperties == null) {
            if (other.nodeProperties != null) {
                return false;
            }
        } else if (!nodeProperties.equals(other.nodeProperties)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return ("SwitchConfig [Node=" + nodeId + ", Properties=" + nodeProperties + "]");
    }

    /**
     * Implement clonable interface
     */
    @Override
    public SwitchConfig clone() {
        Map<String, Property> nodeProperties = (this.nodeProperties == null) ? null : new HashMap<String, Property>(
                this.nodeProperties);
        return new SwitchConfig(this.nodeId, nodeProperties);
    }

}
