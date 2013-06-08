
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.reader;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.NodeConnector;

/**
 * Represents the statistics for the node conenctor
 *
 *
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NodeConnectorStatistics {
	@XmlElement
    private NodeConnector nodeConnector;
	@XmlElement
    private long receivePackets;
	@XmlElement
    private long transmitPackets;
	@XmlElement
    private long receiveBytes;
	@XmlElement
    private long transmitBytes;
	@XmlElement
    private long receiveDrops;
	@XmlElement
    private long transmitDrops;
	@XmlElement
    private long receiveErrors;
	@XmlElement
    private long transmitErrors;
	@XmlElement
    private long receiveFrameError;
	@XmlElement
    private long receiveOverRunError;
	@XmlElement
    private long receiveCrcError;
	@XmlElement
    private long collisionCount;

	//To Satisfy JAXB
	public NodeConnectorStatistics() {
		
	}
    /**
     * Set the node connector
     * @param port
     */
    public void setNodeConnector(NodeConnector port) {
        this.nodeConnector = port;
    }

    /**
     * Returns the node connector
     * @return
     */
    public NodeConnector getNodeConnector() {
        return nodeConnector;
    }

    /**
     * Set the rx packet count's value
     * @param count
     */
    public void setReceivePacketCount(long count) {
        receivePackets = count;
    }

    /**
     * Returns the rx packet count for the port
     * @return
     */
    public long getReceivePacketCount() {
        return receivePackets;
    }

    /**
     * Set the tx packet count's value
     * @param count
     */
    public void setTransmitPacketCount(long count) {
        transmitPackets = count;
    }

    /**
     * Returns the tx packet count for the port
     * @return
     */
    public long getTransmitPacketCount() {
        return transmitPackets;
    }

    /**
     * Set the rx byte count's value
     * @param count
     */
    public void setReceiveByteCount(long count) {
        receiveBytes = count;
    }

    /**
     * Return the rx byte count for the port
     * @return
     */
    public long getReceiveByteCount() {
        return receiveBytes;
    }

    /**
     * Set the tx byte count's value
     * @param count
     */
    public void setTransmitByteCount(long count) {
        transmitBytes = count;
    }

    /**
     * Return the tx byte count for the port
     * @return
     */
    public long getTransmitByteCount() {
        return transmitBytes;
    }

    /**
     * Set the rx drop count's value
     * @param count
     */
    public void setReceiveDropCount(long count) {
        receiveDrops = count;
    }

    /**
     * Returns the rx drop count for the port
     * @return
     */
    public long getReceiveDropCount() {
        return receiveDrops;
    }

    /**
     * Set the tx drop count's value
     * @param count
     */
    public void setTransmitDropCount(long count) {
        transmitDrops = count;
    }

    /**
     * Returns the tx drop count for the port
     * @return
     */
    public long getTransmitDropCount() {
        return transmitDrops;
    }

    /**
     * Set the rx error count's value
     * @param count
     */
    public void setReceiveErrorCount(long count) {
        receiveErrors = count;
    }

    /**
     * Return the rx error count for the port
     * @return
     */
    public long getReceiveErrorCount() {
        return receiveErrors;
    }

    /**
     * Set the tx error count's value
     * @param count
     */
    public void setTransmitErrorCount(long count) {
        transmitErrors = count;
    }

    /**
     * Return the tx error count for the port
     * @return
     */
    public long getTransmitErrorCount() {
        return transmitErrors;
    }

    /**
     * Set the rx frame error value
     * @param count
     */
    public void setReceiveFrameErrorCount(long count) {
        receiveFrameError = count;
    }

    /**
     * Returns the rx frame error for the port
     * @return
     */
    public long getReceiveFrameErrorCount() {
        return receiveFrameError;
    }

    /**
     * Set the rx overrun error value
     * @param count
     */
    public void setReceiveOverRunErrorCount(long count) {
        receiveOverRunError = count;
    }

    /**
     * Return the rx overrun error for the port
     * @return
     */
    public long getReceiveOverRunErrorCount() {
        return receiveOverRunError;
    }

    /**
     * Set the rx CRC Error value
     * @param count
     */
    public void setReceiveCRCErrorCount(long count) {
        receiveCrcError = count;
    }

    /**
     * Return the rx CRC error for the port
     * @return
     */
    public long getReceiveCRCErrorCount() {
        return receiveCrcError;
    }

    /**
     * Set the collisionCount count's value
     * @param count
     */
    public void setCollisionCount(long count) {
        collisionCount = count;
    }

    /**
     * Return the collisionCount count for the port
     * @return
     */
    public long getCollisionCount() {
        return collisionCount;
    }

    @Override
    public String toString() {
        return "NodeConnectorStats[portNumber = " + nodeConnector
                + ", receivePackets = " + receivePackets
                + ", transmitPackets = " + transmitPackets
                + ", receiveBytes = " + receiveBytes + ", transmitBytes = "
                + transmitBytes + ", receiveDrops = " + receiveDrops
                + ", transmitDrops = " + transmitDrops + ", receiveErrors = "
                + receiveErrors + ", transmitErrors = " + transmitErrors
                + ", receiveFrameError = " + receiveFrameError
                + ", receiveOverRunError = " + receiveOverRunError
                + ", receiveCrcError = " + receiveCrcError
                + ", collisionCount = " + collisionCount + "]";
    }

}
