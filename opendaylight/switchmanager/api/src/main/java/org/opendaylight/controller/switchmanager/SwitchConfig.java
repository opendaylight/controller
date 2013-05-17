
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager;

import java.io.Serializable;

/**
 * The class describes a switch configuration including node identifier, node
 * name, tier number and proactive/reactive mode.
 */
public class SwitchConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    String nodeId;
    String description;
    String tier;
    String mode;

    public SwitchConfig(String nodeId, String description, String tier, String mode) {
        super();
        this.nodeId = nodeId;
        this.description = description;
        this.tier = tier;
        this.mode = mode;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeDescription() {
        return description;
    }

    public String getTier() {
        return tier;
    }

    public String getMode() {
        return mode;
    }

    public boolean isProactive() {
    	return Integer.parseInt(mode) != 0;
    }
    
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((mode == null) ? 0 : mode.hashCode());
        result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
        result = prime * result + ((tier == null) ? 0 : tier.hashCode());
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
        SwitchConfig other = (SwitchConfig) obj;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (mode == null) {
            if (other.mode != null)
                return false;
        } else if (!mode.equals(other.mode))
            return false;
        if (nodeId == null) {
            if (other.nodeId != null)
                return false;
        } else if (!nodeId.equals(other.nodeId))
            return false;
        if (tier == null) {
            if (other.tier != null)
                return false;
        } else if (!tier.equals(other.tier))
            return false;
        return true;
    }
}
