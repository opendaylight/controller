
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
 * Set vlan CFI action
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class SetVlanCfi extends Action {
	@XmlElement
    private int cfi;

    /* Dummy constructor for JAXB */
    private SetVlanCfi () {
    }

    public SetVlanCfi(int cfi) {
        type = ActionType.SET_VLAN_CFI;
        this.cfi = cfi;
        checkValue(cfi);
    }

    /**
     * Returns the 802.1q CFI value that this action will set
     * @return
     */
    public int getCfi() {
        return cfi;
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
        return type + "[cfi = " + Integer.toHexString(cfi) + "]";
    }
}
