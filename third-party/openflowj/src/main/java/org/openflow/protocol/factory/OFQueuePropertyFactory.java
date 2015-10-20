package org.openflow.protocol.factory;

import java.nio.ByteBuffer;
import java.util.List;

import org.openflow.protocol.queue.OFQueueProperty;
import org.openflow.protocol.queue.OFQueuePropertyType;


/**
 * The interface to factories used for retrieving OFQueueProperty instances. All
 * methods are expected to be thread-safe.
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface OFQueuePropertyFactory {
    /**
     * Retrieves an OFQueueProperty instance corresponding to the specified
     * OFQueuePropertyType
     * @param t the type of the OFQueueProperty to be retrieved
     * @return an OFQueueProperty instance
     */
    public OFQueueProperty getQueueProperty(OFQueuePropertyType t);

    /**
     * Attempts to parse and return all OFQueueProperties contained in the given
     * ByteBuffer, beginning at the ByteBuffer's position, and ending at
     * position+length.
     * @param data the ByteBuffer to parse for OpenFlow OFQueueProperties
     * @param length the number of Bytes to examine for OpenFlow OFQueueProperties
     * @return a list of OFQueueProperty instances
     */
    public List<OFQueueProperty> parseQueueProperties(ByteBuffer data, int length);

    /**
     * Attempts to parse and return all OFQueueProperties contained in the given
     * ByteBuffer, beginning at the ByteBuffer's position, and ending at
     * position+length.
     * @param data the ByteBuffer to parse for OpenFlow OFQueueProperties
     * @param length the number of Bytes to examine for OpenFlow OFQueueProperties
     * @param limit the maximum number of OFQueueProperties to return, 0 means no limit
     * @return a list of OFQueueProperty instances
     */
    public List<OFQueueProperty> parseQueueProperties(ByteBuffer data, int length, int limit);
}
