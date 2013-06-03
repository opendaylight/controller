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
 * The class contains MAC address property.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class MacAddress extends Property implements Cloneable {
    private static final long serialVersionUID = 1L;
    @XmlElement(name="macAddress")
    private final byte[] address;
    public static final String name = "macAddress";

    /*
     * Private constructor used for JAXB mapping
     */
    private MacAddress() {
        super(name);
        this.address = null;
    }

    /**
     * Constructor to create DatalinkAddress property which contains the MAC
     * address. The property will be attached to a
     * {@link org.opendaylight.controller.sal.core.Node}.
     *
     *
     * @param nodeMacAddress
     *            Data Link Address for the node
     *
     * @return the constructed object
     */
    public MacAddress(byte[] nodeMacAddress) {
        super(name);
        this.address = nodeMacAddress.clone();
    }

    /**
     * @return the node MAC address
     */
    public byte[] getMacAddress() {
        return this.address.clone();
    }

    @Override
    public MacAddress clone() {
        return new MacAddress(this.address);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(address);
        return result;
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
        MacAddress other = (MacAddress) obj;
        if (!Arrays.equals(address, other.address)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MacAddress[" + ReflectionToStringBuilder.toString(this) + "]";
    }
}
