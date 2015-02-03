/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   RawPacket.java
 *
 * @brief  Describe a raw Data Packet, this is how a packet is
 * received from the network and how it will be transmitted. It
 * essentially wraps the raw bytestream
 *
 */
package org.opendaylight.controller.sal.packet;

import java.util.Map;
import java.util.HashMap;

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.TimeStamp;

/**
 *
 * Describe a raw Data Packet, this is how a packet is received from the network
 * and how it will be transmitted. It essentially wraps the raw bytestream
 *
 */
@Deprecated
public class RawPacket {
    private byte[] packetData;
    private final LinkEncap encap;
    private final TimeStamp incomingTime;
    private final TimeStamp copyTime;
    private Map<Object, Object> props;
    private NodeConnector incomingNodeConnector;
    private NodeConnector outgoingNodeConnector;

    /**
     * If the packet is being sent this parameter tells where the packet is sent
     * toward
     *
     *
     * @return the NodeConnector toward where the packet is being sent
     */
    public NodeConnector getOutgoingNodeConnector() {
        return outgoingNodeConnector;
    }

    /**
     * Setter method for OutGoing NodeConnector
     *
     * @param outgoingNodeConnector
     *            NodeConnector toward where the packet is travelling
     */
    public void setOutgoingNodeConnector(NodeConnector outgoingNodeConnector) {
        this.outgoingNodeConnector = outgoingNodeConnector;
    }

    /**
     * Return the incoming NodeConnector if the packet was received
     *
     * @return NodeConnector where the packet was received from
     */
    public NodeConnector getIncomingNodeConnector() {
        return incomingNodeConnector;
    }

    /**
     * Setter for Incoming NodeConnector
     *
     * @param incomingNodeConnector
     *            NodeConnector to be used and incoming one
     */
    public void setIncomingNodeConnector(NodeConnector incomingNodeConnector) {
        this.incomingNodeConnector = incomingNodeConnector;
    }

    /**
     * Retrieve a given property attached to the packet, if exits of course
     *
     * @param key
     *            Key to retrieve the wanted property attached to the packet
     *
     * @return The property attached to the packet
     */
    public Object getProps(Object key) {
        if (this.props != null) {
            return this.props.get(key);
        }
        return null;
    }

    /**
     * Generic data associated to the data packet
     *
     * @param key
     *            key for the association
     * @param value
     *            value associated to the key
     */
    public void setProps(Object key, Object value) {
        if (this.props == null) {
            this.props = new HashMap<Object, Object>();
        }

        this.props.put(key, value);
    }

    /**
     * Constructor for RawPacket
     *
     * @param data
     *            content of the packet as bytestream
     * @param e
     *            datalink encapsulation for the packet
     *
     */
    public RawPacket(byte[] data, LinkEncap e) throws ConstructionException {
        if (data == null) {
            throw new ConstructionException("Empty data");
        }
        if (e == null) {
            throw new ConstructionException("Encap not known");
        }
        this.packetData = new byte[data.length];
        System.arraycopy(data, 0, this.packetData, 0, data.length);
        this.encap = e;
        this.incomingTime = new TimeStamp(System.currentTimeMillis(),
                "IncomingTime");
        this.copyTime = null;
    }

    /**
     * Copy Constructor for RawPacket, it performs a copy of the packet so each
     * packet can be modified independently without worrying that source packet
     * content is touched
     *
     * @param src
     *            packet to copy data from
     *
     */
    public RawPacket(RawPacket src) throws ConstructionException {
        if (src == null) {
            throw new ConstructionException("null source packet");
        }
        if (src.getPacketData() != null) {
            this.packetData = new byte[src.getPacketData().length];
            System.arraycopy(src.getPacketData(), 0, this.packetData, 0,
                    src.getPacketData().length);
        } else {
            throw new ConstructionException("Empty packetData");
        }
        this.encap = src.getEncap();
        this.incomingTime = src.getIncomingTime();
        this.incomingNodeConnector = src.getIncomingNodeConnector();
        this.outgoingNodeConnector = src.getOutgoingNodeConnector();
        this.props = (src.props == null ? null : new HashMap<Object, Object>(
                src.props));
        this.copyTime = new TimeStamp(System.currentTimeMillis(), "CopyTime");
    }

    /**
     * Constructor for RawPacket with Ethernet encapsulation
     *
     * @param data
     *            content of the packet as bytestream
     *
     */
    public RawPacket(byte[] data) throws ConstructionException {
        this(data, LinkEncap.ETHERNET);
    }

    /**
     * Read the time stamp when the packet has entered the system
     *
     * @return The time stamp when the packet has entered the system
     */
    public TimeStamp getIncomingTime() {
        return this.incomingTime;
    }

    /**
     * Read the packet encapsulation
     *
     * @return The encapsulation for the raw packet, necessary to start parsing
     *         the packet
     */
    public LinkEncap getEncap() {
        return this.encap;
    }

    /**
     * Get bytestream of the packet body
     *
     * @return The raw bytestream composing the packet
     */
    public byte[] getPacketData() {
        return this.packetData;
    }

    /**
     * Returns the time at which the current instance of RawPacket was created
     * as a copy of the original one.
     *
     * @return The time stamp at which this RawPacket instance was created. null
     *         if this is the original instance.
     */
    public TimeStamp getCopyTime() {
        return this.copyTime;
    }
}
