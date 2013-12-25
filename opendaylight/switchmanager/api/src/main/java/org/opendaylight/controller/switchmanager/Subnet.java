
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.sal.core.NodeConnector;

/**
 * The class describes subnet information including L3 address, vlan and set of
 * ports associated with the subnet.
 */
public class Subnet implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    // Key fields
    private InetAddress networkAddress;
    private transient InetAddress subnetPrefix;
    private short subnetMaskLength;
    // Property fields
    private short vlan;
    private final Set<NodeConnector> nodeConnectors;

    public Subnet(InetAddress ip, short maskLen, short vlan) {
        this.networkAddress = ip;
        this.subnetMaskLength = maskLen;
        this.vlan = vlan;
        this.nodeConnectors = new HashSet<NodeConnector>();
    }

    public Subnet(SubnetConfig conf) {
        networkAddress = conf.getIPAddress();
        subnetMaskLength = conf.getIPMaskLen();
        nodeConnectors = conf.getNodeConnectors();
    }

    public Subnet(Subnet subnet) {
        networkAddress = subnet.networkAddress;
        subnetMaskLength = subnet.subnetMaskLength;
        vlan = subnet.vlan;
        nodeConnectors = new HashSet<NodeConnector>(subnet.nodeConnectors);
    }

    /**
     * Add NodeConnectors to a subnet
     *
     * @param sp Set of NodeConnectors to add to the subnet
     */
    public void addNodeConnectors(Set<NodeConnector> sp) {
        if (sp != null) {
            this.nodeConnectors.addAll(sp);
        }
    }

    /**
     * Delete NodeConnectors from subnet
     *
     * @param sp Set of NodeConnectors to add to the subnet
     */
    public void deleteNodeConnectors(Set<NodeConnector> sp) {
        if (sp == null) {
            return;
        }
        for (NodeConnector p : sp) {
            this.nodeConnectors.remove(p);
        }
    }

    /**
     * Return the list of NodeConnectors configured for this subnet,
     * could be also an empty set in case of all the known
     * nodeconnectors.
     *
     *
     * @return The list of NodeConnectors attached to the subnet
     */
    public Set<NodeConnector> getNodeConnectors() {
        return this.nodeConnectors;
    }

    /**
     * If the subnet has no node connectors attached to it then it
     * means that is a whole L2 flat domain
     *
     *
     * @return true if there are no node connectors configured for the
     * subnet else false
     */
    public boolean isFlatLayer2() {
        return nodeConnectors.isEmpty();
    }

    /**
     * getter method
     *
     *
     * @return the Network Address part of the subnet
     */
    public InetAddress getNetworkAddress() {
        return networkAddress;
    }

    /**
     * @param networkAddress the networkAddress to set
     */
    public Subnet setNetworkAddress(InetAddress networkAddress) {
        this.networkAddress = networkAddress;
        this.subnetPrefix = null;
        return this;
    }

    /**
     * getter method
     *
     *
     * @return the subnet mask length
     */
    public short getSubnetMaskLength() {
        return this.subnetMaskLength;
    }

    public Subnet setSubnetMaskLength(short m) {
        this.subnetMaskLength = m;
        return this;
    }

    /*
     * returns the prefix of a given IP by applying this subnet's mask
     */
    private InetAddress getPrefixForAddress(InetAddress ip) {
        int bytes = this.subnetMaskLength / 8;
        int bits = this.subnetMaskLength % 8;
        byte modifiedByte;
        byte[] sn = ip.getAddress();
        if (bits > 0) {
            modifiedByte = (byte) (sn[bytes] >> (8 - bits));
            sn[bytes] = (byte) (modifiedByte << (8 - bits));
            bytes++;
        }
        for (; bytes < sn.length; bytes++) {
            sn[bytes] = (byte) (0);
        }
        try {
            return InetAddress.getByAddress(sn);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public boolean isSubnetOf(InetAddress ip) {
        if (ip == null) {
            return false;
        }
        if(subnetPrefix == null) {
            subnetPrefix = getPrefixForAddress(this.networkAddress);
        }
        InetAddress otherPrefix = getPrefixForAddress(ip);
        boolean isSubnetOf = true;
        if (((subnetPrefix == null) || (otherPrefix == null)) || (!subnetPrefix.equals(otherPrefix)) ) {
            isSubnetOf = false;
        }
        return isSubnetOf;
    }

    public short getVlan() {
        return this.vlan;
    }

    public Subnet setVlan(short i) {
        this.vlan = i;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((networkAddress == null) ? 0 : networkAddress.hashCode());
        result = prime * result
                + ((nodeConnectors == null) ? 0 : nodeConnectors.hashCode());
        result = prime * result + subnetMaskLength;
        result = prime * result + vlan;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Subnet other = (Subnet) obj;
        if (networkAddress == null) {
            if (other.networkAddress != null) {
                return false;
            }
        } else if (!networkAddress.equals(other.networkAddress)) {
            return false;
        }
        if (nodeConnectors == null) {
            if (other.nodeConnectors != null) {
                return false;
            }
        } else if (!nodeConnectors.equals(other.nodeConnectors)) {
            return false;
        }
        if (subnetMaskLength != other.subnetMaskLength) {
            return false;
        }
        if (vlan != other.vlan) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return ("Subnet [networkAddress=" + networkAddress.getHostAddress()
                + "/" + subnetMaskLength
                + ((vlan == 0) ? "" : (", vlan=" + vlan)) + ", "
                + ((isFlatLayer2()) ? "{[*, *]}" : nodeConnectors.toString()) + "]");
    }

    public boolean hasNodeConnector(NodeConnector p) {
        if (p == null) {
            return false;
        }
        return isFlatLayer2() || nodeConnectors.contains(p);
    }

    public boolean isMutualExclusive(Subnet otherSubnet) {
        return !(isSubnetOf(otherSubnet.getNetworkAddress()) || otherSubnet.isSubnetOf(getNetworkAddress()));
    }

    /**
     * Implement clonable interface
     */
    @Override
    public Subnet clone() {
        return new Subnet(this);
    }

}
