package org.openflow.protocol.statistics;

import java.nio.ByteBuffer;

/**
 * Represents an ofp_queue_stats_request structure
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFQueueStatisticsRequest implements OFStatistics {
    protected short portNumber;
    protected int queueId;

    /**
     * @return the portNumber
     */
    public short getPortNumber() {
        return portNumber;
    }

    /**
     * @param portNumber the portNumber to set
     */
    public void setPortNumber(short portNumber) {
        this.portNumber = portNumber;
    }

    /**
     * @return the queueId
     */
    public int getQueueId() {
        return queueId;
    }

    /**
     * @param queueId the queueId to set
     */
    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    @Override
    public int getLength() {
        return 8;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        this.portNumber = data.getShort();
        data.getShort(); // pad
        this.queueId = data.getInt();
    }

    @Override
    public void writeTo(ByteBuffer data) {
        data.putShort(this.portNumber);
        data.putShort((short) 0); // pad
        data.putInt(this.queueId);
    }

    @Override
    public int hashCode() {
        final int prime = 443;
        int result = 1;
        result = prime * result + portNumber;
        result = prime * result + queueId;
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
        if (!(obj instanceof OFQueueStatisticsRequest)) {
            return false;
        }
        OFQueueStatisticsRequest other = (OFQueueStatisticsRequest) obj;
        if (portNumber != other.portNumber) {
            return false;
        }
        if (queueId != other.queueId) {
            return false;
        }
        return true;
    }
}
