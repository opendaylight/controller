
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

import org.opendaylight.controller.sal.flowprogrammer.Flow;

/**
 * Represents the flow that is installed on the network node
 * along with the table location, hit counters and timers
 */

@XmlRootElement (name="FlowStat")
@XmlAccessorType(XmlAccessType.NONE)
public class FlowOnNode {
    @XmlElement
    private Flow flow; // Flow descriptor
    @XmlElement
    private byte tableId;
    @XmlElement
    private int durationSeconds;
    @XmlElement
    private int durationNanoseconds;
    @XmlElement
    private long packetCount;
    @XmlElement
    private long byteCount;

    /* Dummy constructor for JAXB */
    private FlowOnNode () {
    }
    
    public FlowOnNode(Flow flow) {
        this.flow = flow;
    }

    /**
     * Returns the description of the flow which statistics are about
     * @return
     */
    public Flow getFlow() {
        return flow;
    }

    /**
     * Set the packet count's value
     * @param count
     */
    public void setPacketCount(long count) {
        packetCount = count;
    }

    /**
     * Set the byte count's value
     * @param count
     */
    public void setByteCount(long count) {
        byteCount = count;
    }

    /**
     * Returns the packet count for the flow
     * @return
     */
    public long getPacketCount() {
        return packetCount;
    }

    /**
     * Return the byte count for the flow
     * @return
     */
    public long getByteCount() {
        return byteCount;
    }

    public byte getTableId() {
        return tableId;
    }

    public void setTableId(byte tableId) {
        this.tableId = tableId;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getDurationNanoseconds() {
        return durationNanoseconds;
    }

    public void setDurationNanoseconds(int durationNanoseconds) {
        this.durationNanoseconds = durationNanoseconds;
    }

    @Override
    public String toString() {
        return "FlowOnNode[flow =" + flow + ", tableId = " + tableId
                + ", sec = " + durationSeconds + ", nsec = "
                + durationNanoseconds + ", pkt = " + packetCount + ", byte = "
                + byteCount + "]";
    }
}
