package org.openflow.protocol.statistics;

import java.nio.ByteBuffer;

/**
 * Represents an ofp_port_stats_request structure
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFPortStatisticsRequest implements OFStatistics {
    protected short portNumber;

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

    @Override
    public int getLength() {
        return 8;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        this.portNumber = data.getShort();
        data.getShort(); // pad
        data.getInt(); // pad
    }

    @Override
    public void writeTo(ByteBuffer data) {
        data.putShort(this.portNumber);
        data.putShort((short) 0); // pad
        data.putInt(0); // pad
    }

    @Override
    public int hashCode() {
        final int prime = 433;
        int result = 1;
        result = prime * result + portNumber;
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
        if (!(obj instanceof OFPortStatisticsRequest)) {
            return false;
        }
        OFPortStatisticsRequest other = (OFPortStatisticsRequest) obj;
        if (portNumber != other.portNumber) {
            return false;
        }
        return true;
    }
}
