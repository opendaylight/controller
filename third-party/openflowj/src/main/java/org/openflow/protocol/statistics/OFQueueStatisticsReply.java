package org.openflow.protocol.statistics;

import java.nio.ByteBuffer;

/**
 * Represents an ofp_queue_stats structure
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFQueueStatisticsReply implements OFStatistics {
    protected short portNumber;
    protected int queueId;
    protected long transmitBytes;
    protected long transmitPackets;
    protected long transmitErrors;

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

    /**
     * @return the transmitBytes
     */
    public long getTransmitBytes() {
        return transmitBytes;
    }

    /**
     * @param transmitBytes the transmitBytes to set
     */
    public void setTransmitBytes(long transmitBytes) {
        this.transmitBytes = transmitBytes;
    }

    /**
     * @return the transmitPackets
     */
    public long getTransmitPackets() {
        return transmitPackets;
    }

    /**
     * @param transmitPackets the transmitPackets to set
     */
    public void setTransmitPackets(long transmitPackets) {
        this.transmitPackets = transmitPackets;
    }

    /**
     * @return the transmitErrors
     */
    public long getTransmitErrors() {
        return transmitErrors;
    }

    /**
     * @param transmitErrors the transmitErrors to set
     */
    public void setTransmitErrors(long transmitErrors) {
        this.transmitErrors = transmitErrors;
    }

    @Override
    public int getLength() {
        return 32;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        this.portNumber = data.getShort();
        data.getShort(); // pad
        this.queueId = data.getInt();
        this.transmitBytes = data.getLong();
        this.transmitPackets = data.getLong();
        this.transmitErrors = data.getLong();
    }

    @Override
    public void writeTo(ByteBuffer data) {
        data.putShort(this.portNumber);
        data.putShort((short) 0); // pad
        data.putInt(this.queueId);
        data.putLong(this.transmitBytes);
        data.putLong(this.transmitPackets);
        data.putLong(this.transmitErrors);
    }

    @Override
    public int hashCode() {
        final int prime = 439;
        int result = 1;
        result = prime * result + portNumber;
        result = prime * result + queueId;
        result = prime * result
                + (int) (transmitBytes ^ (transmitBytes >>> 32));
        result = prime * result
                + (int) (transmitErrors ^ (transmitErrors >>> 32));
        result = prime * result
                + (int) (transmitPackets ^ (transmitPackets >>> 32));
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
        if (!(obj instanceof OFQueueStatisticsReply)) {
            return false;
        }
        OFQueueStatisticsReply other = (OFQueueStatisticsReply) obj;
        if (portNumber != other.portNumber) {
            return false;
        }
        if (queueId != other.queueId) {
            return false;
        }
        if (transmitBytes != other.transmitBytes) {
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
}
