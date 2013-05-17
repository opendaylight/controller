
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * The class contains the controller MAC address and node MAC address.
 *
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class MacAddress extends Property {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private byte[] controllerMacAddress;
    @XmlElement
    private byte[] nodeMacAddress;
    public static final String MacPropName = "macAddress";

    /*
     * Private constructor used for JAXB mapping
     */
    private MacAddress() {
        super(MacPropName);
        this.controllerMacAddress = null;
        this.nodeMacAddress = null;
    }

	/**
	 * Constructor to create DatalinkAddress property which contains the
	 * controller MAC address and node MAC address. The property will be
	 * attached to a {@link org.opendaylight.controller.sal.core.Node}.
	 * 
	 * @param controllerMacAddress Data Link Address for the controller
	 * @param nodeMacAddress Data Link Address for the node
	 * 
	 * @return the constructed object
	 */
    public MacAddress(byte[] controllerMacAddress, byte[] nodeMacAddress) {
        super(MacPropName);
    	
        this.controllerMacAddress = controllerMacAddress;
        this.nodeMacAddress = nodeMacAddress;
    }

    /**
     * @return the controller MAC address
     */
    public byte[] getControllerMacAddress() {
        return this.controllerMacAddress;
    }

    /**
     * @return the node MAC address
     */
    public byte[] getNodeMacAddress() {
        return this.nodeMacAddress;
    }

    public MacAddress clone() {
    	return new MacAddress(this.controllerMacAddress, this.nodeMacAddress);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(controllerMacAddress);
        result = prime * result + Arrays.hashCode(nodeMacAddress);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        MacAddress other = (MacAddress) obj;
        if (!Arrays.equals(controllerMacAddress, other.controllerMacAddress))
            return false;
        if (!Arrays.equals(nodeMacAddress, other.nodeMacAddress))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MacAddress[" + ReflectionToStringBuilder.toString(this) + "]";
    }
}
