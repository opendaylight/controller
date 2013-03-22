package org.openflow.protocol.statistics;

import java.nio.ByteBuffer;

import org.openflow.protocol.OFMatch;

/**
 * Represents an ofp_aggregate_stats_request structure
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFAggregateStatisticsRequest implements OFStatistics {
    protected OFMatch match;
    protected byte tableId;
    protected short outPort;

    /**
     * @return the match
     */
    public OFMatch getMatch() {
        return match;
    }

    /**
     * @param match the match to set
     */
    public void setMatch(OFMatch match) {
        this.match = match;
    }

    /**
     * @return the tableId
     */
    public byte getTableId() {
        return tableId;
    }

    /**
     * @param tableId the tableId to set
     */
    public void setTableId(byte tableId) {
        this.tableId = tableId;
    }

    /**
     * @return the outPort
     */
    public short getOutPort() {
        return outPort;
    }

    /**
     * @param outPort the outPort to set
     */
    public void setOutPort(short outPort) {
        this.outPort = outPort;
    }

    @Override
    public int getLength() {
        return 44;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        if (this.match == null)
            this.match = new OFMatch();
        this.match.readFrom(data);
        this.tableId = data.get();
        data.get(); // pad
        this.outPort = data.getShort();
    }

    @Override
    public void writeTo(ByteBuffer data) {
        this.match.writeTo(data);
        data.put(this.tableId);
        data.put((byte) 0);
        data.putShort(this.outPort);
    }

    @Override
    public int hashCode() {
        final int prime = 401;
        int result = 1;
        result = prime * result + ((match == null) ? 0 : match.hashCode());
        result = prime * result + outPort;
        result = prime * result + tableId;
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
        if (!(obj instanceof OFAggregateStatisticsRequest)) {
            return false;
        }
        OFAggregateStatisticsRequest other = (OFAggregateStatisticsRequest) obj;
        if (match == null) {
            if (other.match != null) {
                return false;
            }
        } else if (!match.equals(other.match)) {
            return false;
        }
        if (outPort != other.outPort) {
            return false;
        }
        if (tableId != other.tableId) {
            return false;
        }
        return true;
    }
}
