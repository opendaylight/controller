package org.openflow.protocol.statistics;

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.openflow.util.StringByteSerializer;

/**
 * Represents an ofp_table_stats structure
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFTableStatistics implements OFStatistics, Serializable {
    public static int MAX_TABLE_NAME_LEN = 32;

    protected byte tableId;
    protected String name;
    protected int wildcards;
    protected int maximumEntries;
    protected int activeCount;
    protected long lookupCount;
    protected long matchedCount;

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
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the wildcards
     */
    public int getWildcards() {
        return wildcards;
    }

    /**
     * @param wildcards the wildcards to set
     */
    public void setWildcards(int wildcards) {
        this.wildcards = wildcards;
    }

    /**
     * @return the maximumEntries
     */
    public int getMaximumEntries() {
        return maximumEntries;
    }

    /**
     * @param maximumEntries the maximumEntries to set
     */
    public void setMaximumEntries(int maximumEntries) {
        this.maximumEntries = maximumEntries;
    }

    /**
     * @return the activeCount
     */
    public int getActiveCount() {
        return activeCount;
    }

    /**
     * @param activeCount the activeCount to set
     */
    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }

    /**
     * @return the lookupCount
     */
    public long getLookupCount() {
        return lookupCount;
    }

    /**
     * @param lookupCount the lookupCount to set
     */
    public void setLookupCount(long lookupCount) {
        this.lookupCount = lookupCount;
    }

    /**
     * @return the matchedCount
     */
    public long getMatchedCount() {
        return matchedCount;
    }

    /**
     * @param matchedCount the matchedCount to set
     */
    public void setMatchedCount(long matchedCount) {
        this.matchedCount = matchedCount;
    }

    @Override
    public int getLength() {
        return 64;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        this.tableId = data.get();
        data.get(); // pad
        data.get(); // pad
        data.get(); // pad
        this.name = StringByteSerializer.readFrom(data, MAX_TABLE_NAME_LEN);
        this.wildcards = data.getInt();
        this.maximumEntries = data.getInt();
        this.activeCount = data.getInt();
        this.lookupCount = data.getLong();
        this.matchedCount = data.getLong();
    }

    @Override
    public void writeTo(ByteBuffer data) {
        data.put(this.tableId);
        data.put((byte) 0); // pad
        data.put((byte) 0); // pad
        data.put((byte) 0); // pad
        StringByteSerializer.writeTo(data, MAX_TABLE_NAME_LEN, this.name);
        data.putInt(this.wildcards);
        data.putInt(this.maximumEntries);
        data.putInt(this.activeCount);
        data.putLong(this.lookupCount);
        data.putLong(this.matchedCount);
    }

    @Override
    public int hashCode() {
        final int prime = 449;
        int result = 1;
        result = prime * result + activeCount;
        result = prime * result + (int) (lookupCount ^ (lookupCount >>> 32));
        result = prime * result + (int) (matchedCount ^ (matchedCount >>> 32));
        result = prime * result + maximumEntries;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + tableId;
        result = prime * result + wildcards;
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
        if (!(obj instanceof OFTableStatistics)) {
            return false;
        }
        OFTableStatistics other = (OFTableStatistics) obj;
        if (activeCount != other.activeCount) {
            return false;
        }
        if (lookupCount != other.lookupCount) {
            return false;
        }
        if (matchedCount != other.matchedCount) {
            return false;
        }
        if (maximumEntries != other.maximumEntries) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (tableId != other.tableId) {
            return false;
        }
        if (wildcards != other.wildcards) {
            return false;
        }
        return true;
    }
}
