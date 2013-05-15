/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.action;

import java.net.InetAddress;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Set network source address action
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetNwSrc extends ActionRTO {

    private InetAddress address;

    @SuppressWarnings("unused")
    private SetNwSrc() {
    }

    public SetNwSrc(InetAddress address) {
        this.address = (address);
    }

    /**
     * Returns the network address this action will set
     * 
     * @return InetAddress
     */
    public InetAddress getAddress() {
        return this.address;
    }

    @Deprecated
    @XmlElement(name = "address")
    public String getAddressAsString() {
        return this.address.getHostAddress();
    }

    @Override
    public String toString() {
        return "setNwSrc" + "[address = " + this.address + "]";
    }
}
