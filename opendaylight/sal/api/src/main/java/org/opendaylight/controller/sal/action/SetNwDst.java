
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import java.net.InetAddress;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Set network destination address action
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class SetNwDst extends Action {
    InetAddress address;

    /* Dummy constructor for JAXB */
    private SetNwDst  () {
    }

    public SetNwDst(InetAddress address) {
        type = ActionType.SET_NW_DST;
        this.address = address;
    }

    /**
     * Returns the network address this action will set
     *
     * @return	InetAddress
     */
    public InetAddress getAddress() {
        return address;
    }
    
    @XmlElement (name="address")
    public String getAddressAsString() {
    	return address.getHostAddress();
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
        return type + "[address = " + address + "]";
    }
}
