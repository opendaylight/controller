package org.openflow.protocol.factory;

import java.nio.ByteBuffer;
import java.util.List;

import org.openflow.protocol.OFType;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;


/**
 * The interface to factories used for retrieving OFStatistics instances. All
 * methods are expected to be thread-safe.
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface OFStatisticsFactory {
    /**
     * Retrieves an OFStatistics instance corresponding to the specified
     * OFStatisticsType
     * @param t the type of the containing OFMessage, only accepts statistics
     *           request or reply
     * @param st the type of the OFStatistics to be retrieved
     * @return an OFStatistics instance
     */
    public OFStatistics getStatistics(OFType t, OFStatisticsType st);

    /**
     * Attempts to parse and return all OFStatistics contained in the given
     * ByteBuffer, beginning at the ByteBuffer's position, and ending at
     * position+length.
     * @param t the type of the containing OFMessage, only accepts statistics
     *           request or reply
     * @param st the type of the OFStatistics to be retrieved
     * @param data the ByteBuffer to parse for OpenFlow Statistics
     * @param length the number of Bytes to examine for OpenFlow Statistics
     * @return a list of OFStatistics instances
     */
    public List<OFStatistics> parseStatistics(OFType t,
            OFStatisticsType st, ByteBuffer data, int length);

    /**
     * Attempts to parse and return all OFStatistics contained in the given
     * ByteBuffer, beginning at the ByteBuffer's position, and ending at
     * position+length.
     * @param t the type of the containing OFMessage, only accepts statistics
     *           request or reply
     * @param st the type of the OFStatistics to be retrieved
     * @param data the ByteBuffer to parse for OpenFlow Statistics
     * @param length the number of Bytes to examine for OpenFlow Statistics
     * @param limit the maximum number of messages to return, 0 means no limit
     * @return a list of OFStatistics instances
     */
    public List<OFStatistics> parseStatistics(OFType t,
            OFStatisticsType st, ByteBuffer data, int length, int limit);
}
