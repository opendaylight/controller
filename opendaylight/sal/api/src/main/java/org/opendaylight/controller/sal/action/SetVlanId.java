
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Set vlan id action
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class SetVlanId extends Action {
	@XmlElement
    private int vlanId;

	private SetVlanId() {
		
	}
	
    public SetVlanId(int vlanId) {
        type = ActionType.SET_VLAN_ID;
        this.vlanId = vlanId;
        checkValue(vlanId);
    }

    /**
     * Returns the vlan id this action will set
     *
     * @return int
     */
    public int getVlanId() {
        return vlanId;
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return type + "[vlanId = " + vlanId + "]";
    }
}
