
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
 * Set vlan PCP action
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class SetVlanPcp extends Action {
	@XmlElement
    private int pcp;

	private SetVlanPcp() {
		
	}
	
    public SetVlanPcp(int pcp) {
        type = ActionType.SET_VLAN_PCP;
        this.pcp = pcp;
        checkValue(pcp);
    }

    /**
     * Returns the value of the vlan PCP this action will set
     * @return int
     */
    public int getPcp() {
        return pcp;
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
        return type + "[pcp = " + Integer.toHexString(pcp) + "]";
    }
}
