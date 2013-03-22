
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import java.io.Serializable;
import java.net.InetAddress;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opendaylight.controller.sal.packet.address.DataLinkAddress;

@XmlRootElement(name="host")
@XmlAccessorType(XmlAccessType.NONE)
public class Host implements Serializable {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private DataLinkAddress dataLayerAddress;
    private InetAddress networkAddress;

    public Host() {

    }

    /**
     * Create an Host representation from the combination Data Link
     * layer/Network layer address, both are needed to construct the
     * object. Fake value can also be provided in case are not
     * existent.
     *
     * @param dataLayerAddress Data Link Address for the host
     * @param networkAddress Network Address for the host
     *
     * @return the constructed object
     */
    public Host(DataLinkAddress dataLayerAddress, InetAddress networkAddress)
            throws ConstructionException {
        if (dataLayerAddress == null) {
            throw new ConstructionException("Passed null datalink address");
        }
        if (networkAddress == null) {
            throw new ConstructionException("Passed null network address");
        }
        this.dataLayerAddress = dataLayerAddress;
        this.networkAddress = networkAddress;
    }

    /**
     * Copy constructor
     *
     * @param h Host to copy values from
     *
     * @return constructed copy
     */
    public Host(Host h) throws ConstructionException {
        if (h == null) {
            throw new ConstructionException("Passed null host");
        }
        this.dataLayerAddress = h.getDataLayerAddress();
        this.networkAddress = h.getNetworkAddress();
    }

    /**
     * @return the dataLayerAddress
     */
    public DataLinkAddress getDataLayerAddress() {
        return this.dataLayerAddress;
    }

    /**
     * @return the networkAddress
     */
    public InetAddress getNetworkAddress() {
        return networkAddress;
    }

    @XmlElement(name = "networkAddress")
    public String getNetworkAddressAsString() {
        return networkAddress.getHostAddress();
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return "Host[" + ReflectionToStringBuilder.toString(this) + "]";
    }
}
