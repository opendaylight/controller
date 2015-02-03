
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

import org.opendaylight.controller.sal.packet.address.DataLinkAddress;

@XmlRootElement(name="host")
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
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
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((dataLayerAddress == null) ? 0 : dataLayerAddress.hashCode());
        result = prime * result
                + ((networkAddress == null) ? 0 : networkAddress.hashCode());
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
        Host other = (Host) obj;
        if (dataLayerAddress == null) {
            if (other.dataLayerAddress != null)
                return false;
        } else if (!dataLayerAddress.equals(other.dataLayerAddress))
            return false;
        if (networkAddress == null) {
            if (other.networkAddress != null)
                return false;
        } else if (!networkAddress.equals(other.networkAddress))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Host [dataLayerAddress=" + dataLayerAddress
                + ", networkAddress=" + networkAddress + "]";
    }
}
