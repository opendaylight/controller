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

/**
 * Set network destination address action
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class SetNwDst extends Action {
    private static final long serialVersionUID = 1L;
    InetAddress address;

    /* Dummy constructor for JAXB */
    @SuppressWarnings("unused")
    private SetNwDst() {
    }

    public SetNwDst(InetAddress address) {
        type = ActionType.SET_NW_DST;
        this.address = address;
    }

    /**
     * Returns the network address this action will set
     *
     * @return InetAddress
     */
    public InetAddress getAddress() {
        return address;
    }

    @XmlElement(name = "address")
    public String getAddressAsString() {
        return address.getHostAddress();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SetNwDst other = (SetNwDst) obj;
        if (address == null) {
            if (other.address != null) {
                return false;
            }
        } else if (!address.equals(other.address)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return type + "[address = " + address + "]";
    }
}
