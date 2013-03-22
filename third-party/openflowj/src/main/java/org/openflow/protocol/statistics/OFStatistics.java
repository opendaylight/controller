package org.openflow.protocol.statistics;

import java.nio.ByteBuffer;

/**
 * The base class for all OpenFlow statistics.
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
public interface OFStatistics {
    /**
     * Returns the wire length of this message in bytes
     * @return the length
     */
    public int getLength();

    /**
     * Read this message off the wire from the specified ByteBuffer
     * @param data
     */
    public void readFrom(ByteBuffer data);

    /**
     * Write this message's binary format to the specified ByteBuffer
     * @param data
     */
    public void writeTo(ByteBuffer data);
}
