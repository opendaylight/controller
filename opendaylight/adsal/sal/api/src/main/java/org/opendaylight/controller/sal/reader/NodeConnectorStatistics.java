/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.reader;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.NodeConnector;

/**
 * Represents the statistics for a node connector
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class NodeConnectorStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (collisionCount ^ (collisionCount >>> 32));
        result = prime * result + ((nodeConnector == null) ? 0 : nodeConnector.hashCode());
        result = prime * result + (int) (receiveBytes ^ (receiveBytes >>> 32));
        result = prime * result + (int) (receiveCrcError ^ (receiveCrcError >>> 32));
        result = prime * result + (int) (receiveDrops ^ (receiveDrops >>> 32));
        result = prime * result + (int) (receiveErrors ^ (receiveErrors >>> 32));
        result = prime * result + (int) (receiveFrameError ^ (receiveFrameError >>> 32));
        result = prime * result + (int) (receiveOverRunError ^ (receiveOverRunError >>> 32));
        result = prime * result + (int) (receivePackets ^ (receivePackets >>> 32));
        result = prime * result + (int) (transmitBytes ^ (transmitBytes >>> 32));
        result = prime * result + (int) (transmitDrops ^ (transmitDrops >>> 32));
        result = prime * result + (int) (transmitErrors ^ (transmitErrors >>> 32));
        result = prime * result + (int) (transmitPackets ^ (transmitPackets >>> 32));
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
        if (!(obj instanceof NodeConnectorStatistics)) {
            return false;
        }
        NodeConnectorStatistics other = (NodeConnectorStatistics) obj;
        if (collisionCount != other.collisionCount) {
            return false;
        }
        if (nodeConnector == null) {
            if (other.nodeConnector != null) {
                return false;
            }
        } else if (!nodeConnector.equals(other.nodeConnector)) {
            return false;
        }
        if (receiveBytes != other.receiveBytes) {
            return false;
        }
        if (receiveCrcError != other.receiveCrcError) {
            return false;
        }
        if (receiveDrops != other.receiveDrops) {
            return false;
        }
        if (receiveErrors != other.receiveErrors) {
            return false;
        }
        if (receiveFrameError != other.receiveFrameError) {
            return false;
        }
        if (receiveOverRunError != other.receiveOverRunError) {
            return false;
        }
        if (receivePackets != other.receivePackets) {
            return false;
        }
        if (transmitBytes != other.transmitBytes) {
            return false;
        }
        if (transmitDrops != other.transmitDrops) {
            return false;
        }
        if (transmitErrors != other.transmitErrors) {
            return false;
        }
        if (transmitPackets != other.transmitPackets) {
            return false;
        }
        return true;
    }

    // To Satisfy JAXB
    public NodeConnectorStatistics() {

    }

    /**
     * Set the node connector
     *
     * @param port
     */
    public void setNodeConnector(NodeConnector port) {
        this.nodeConnector = port;
    }

    /**
     * Returns the node connector
     *
     * @return
     */
    public NodeConnector getNodeConnector() {
        return nodeConnector;
    }

    /**
     * Set the rx packet count's value
     *
     * @param count
     */
    public void setReceivePacketCount(long count) {
        receivePackets = count;
    }

    /**
     * Returns the rx packet count for the port
     *
     * @return
     */
    public long getReceivePacketCount() {
        return receivePackets;
    }

    /**
     * Set the tx packet count's value
     *
     * @param count
     */
    public void setTransmitPacketCount(long count) {
        transmitPackets = count;
    }

    /**
     * Returns the tx packet count for the port
     *
     * @return
     */
    public long getTransmitPacketCount() {
        return transmitPackets;
    }

    /**
     * Set the rx byte count's value
     *
     * @param count
     */
    public void setReceiveByteCount(long count) {
        receiveBytes = count;
    }

    /**
     * Return the rx byte count for the port
     *
     * @return
     */
    public long getReceiveByteCount() {
        return receiveBytes;
    }

    /**
     * Set the tx byte count's value
     *
     * @param count
     */
    public void setTransmitByteCount(long count) {
        transmitBytes = count;
    }

    /**
     * Return the tx byte count for the port
     *
     * @return
     */
    public long getTransmitByteCount() {
        return transmitBytes;
    }

    /**
     * Set the rx drop count's value
     *
     * @param count
     */
    public void setReceiveDropCount(long count) {
        receiveDrops = count;
    }

    /**
     * Returns the rx drop count for the port
     *
     * @return
     */
    public long getReceiveDropCount() {
        return receiveDrops;
    }

    /**
     * Set the tx drop count's value
     *
     * @param count
     */
    public void setTransmitDropCount(long count) {
        transmitDrops = count;
    }

    /**
     * Returns the tx drop count for the port
     *
     * @return
     */
    public long getTransmitDropCount() {
        return transmitDrops;
    }

    /**
     * Set the rx error count's value
     *
     * @param count
     */
    public void setReceiveErrorCount(long count) {
        receiveErrors = count;
    }

    /**
     * Return the rx error count for the port
     *
     * @return
     */
    public long getReceiveErrorCount() {
        return receiveErrors;
    }

    /**
     * Set the tx error count's value
     *
     * @param count
     */
    public void setTransmitErrorCount(long count) {
        transmitErrors = count;
    }

    /**
     * Return the tx error count for the port
     *
     * @return
     */
    public long getTransmitErrorCount() {
        return transmitErrors;
    }

    /**
     * Set the rx frame error value
     *
     * @param count
     */
    public void setReceiveFrameErrorCount(long count) {
        receiveFrameError = count;
    }

    /**
     * Returns the rx frame error for the port
     *
     * @return
     */
    public long getReceiveFrameErrorCount() {
        return receiveFrameError;
    }

    /**
     * Set the rx overrun error value
     *
     * @param count
     */
    public void setReceiveOverRunErrorCount(long count) {
        receiveOverRunError = count;
    }

    /**
     * Return the rx overrun error for the port
     *
     * @return
     */
    public long getReceiveOverRunErrorCount() {
        return receiveOverRunError;
    }

    /**
     * Set the rx CRC Error value
     *
     * @param count
     */
    public void setReceiveCRCErrorCount(long count) {
        receiveCrcError = count;
    }

    /**
     * Return the rx CRC error for the port
     *
     * @return
     */
    public long getReceiveCRCErrorCount() {
        return receiveCrcError;
    }

    /**
     * Set the collisionCount count's value
     *
     * @param count
     */
    public void setCollisionCount(long count) {
        collisionCount = count;
    }

    /**
     * Return the collisionCount count for the port
     *
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
