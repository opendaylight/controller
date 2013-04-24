
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
