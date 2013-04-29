
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker.hostAware;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.packet.address.EthernetAddress;

@XmlRootElement(name="host")
@XmlAccessorType(XmlAccessType.NONE)
public class HostNodeConnector extends Host {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private NodeConnector nodeConnector;
    @XmlElement
    private short vlan;
    @XmlElement
    private boolean staticHost;
    private transient short arpSendCountDown;

    /**
     * Private constructor used for JAXB mapping
     */
    private HostNodeConnector() {
    }

    public HostNodeConnector(InetAddress ip) throws ConstructionException {
        this(ip, null);
    }

    public HostNodeConnector(InetAddress ip, NodeConnector nc)
            throws ConstructionException {
        this(new EthernetAddress(new byte[] { (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }), ip, nc,
                (short) 0);
    }

    public HostNodeConnector(byte[] mac, InetAddress ip, NodeConnector nc,
            short vlan) throws ConstructionException {
        this(new EthernetAddress(mac.clone()), ip, nc, vlan);
    }

    public HostNodeConnector(EthernetAddress eaddr, InetAddress naddr,
            NodeConnector nc, short vlan) throws ConstructionException {
        super(eaddr, naddr);
        this.nodeConnector = nc;
        this.vlan = vlan;
    }

    /**
     * @return the NodeConnector
     */
    public NodeConnector getnodeConnector() {
        return this.nodeConnector;
    }

    /**
     * @return the Node
     */
    public Node getnodeconnectorNode() {
        return this.nodeConnector.getNode();
    }

    /**
     * @return the NodeId
     */
    public Long getnodeconnectornodeId() {
        return (Long) this.nodeConnector.getNode().getID();
    }

    /**
     * @return the port
     */
    public Short getnodeconnectorportId() {
        return (Short) this.nodeConnector.getID();
    }

    /**
     * @return the DataLayerAddress
     */
    public byte[] getDataLayerAddressBytes() {
        byte[] macaddr = null;
        if (getDataLayerAddress() instanceof EthernetAddress) {
            EthernetAddress e = (EthernetAddress) getDataLayerAddress();
            macaddr = e.getValue();
        }
        return macaddr;
    }

    /**
     * @return the vlan
     */
    public short getVlan() {
        return this.vlan;
    }

    public boolean isStaticHost() {
        return this.staticHost;
    }

    public HostNodeConnector setStaticHost(boolean statically_learned) {
        this.staticHost = statically_learned;
        return this;
    }

    public HostNodeConnector initArpSendCountDown() {
        this.arpSendCountDown = 24;
        return this;
    }

    public short getArpSendCountDown() {
        return (this.arpSendCountDown);
    }

    public HostNodeConnector setArpSendCountDown(short cntdown) {
        this.arpSendCountDown = cntdown;
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    public boolean equalsByIP(InetAddress networkAddress) {
        return (this.getNetworkAddress().equals(networkAddress));
    }

    public boolean isRewriteEnabled() {
        byte[] emptyArray = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00 };
        byte[] macaddr = null;
        if (getDataLayerAddress() instanceof EthernetAddress) {
            EthernetAddress e = (EthernetAddress) getDataLayerAddress();
            macaddr = e.getValue();
        }
        if (macaddr == null)
            return false;
        return !Arrays.equals(emptyArray, macaddr);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "HostNodeConnector[" + ReflectionToStringBuilder.toString(this)
                + "]";
    }

    public boolean isV4Host() {
        return (getNetworkAddress() instanceof Inet4Address);
    }

    public boolean isV6Host() {
        return (getNetworkAddress() instanceof Inet6Address);
    }

    public String toJson() {
        return "{\"host\":\"" + super.toString() + "\", " + "\"vlan\":\""
                + String.valueOf(vlan) + "\",\"NodeConnector\":\""
                + nodeConnector.toString() + "\"," + "\"static\":\""
                + String.valueOf(isStaticHost()) + "\"}";
    }

}
