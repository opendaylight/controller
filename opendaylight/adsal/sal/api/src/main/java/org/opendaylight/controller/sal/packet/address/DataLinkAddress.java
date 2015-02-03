
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.packet.address;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @file   DataLinkAddress.java
 *
 * @brief  Abstract base class for a Datalink Address
 *
 */

/**
 * Abstract base class for a Datalink Address
 *
 */
@XmlRootElement
@Deprecated
abstract public class DataLinkAddress implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;

    public DataLinkAddress() {

    }

    /**
     * Constructor of super class
     *
     * @param name Create a new DataLink, not for general use but
     * available only for sub classes
     *
     * @return constructed object
     */
    protected DataLinkAddress(String name) {
        this.name = name;
    }

    /**
     * Used to copy the DataLinkAddress in a polymorphic way
     *
     *
     * @return A clone of this DataLinkAddress
     */
    @Override
    abstract public DataLinkAddress clone();

    /**
     * Allow to distinguish among different data link addresses
     *
     *
     * @return Name of the DataLinkAdress we are working on
     */
    public String getName() {
        return this.name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        DataLinkAddress other = (DataLinkAddress) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DataLinkAddress [name=" + name + "]";
    }
}
