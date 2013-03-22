package org.openflow.protocol.factory;

import java.nio.ByteBuffer;
import java.util.List;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;


/**
 * The interface to factories used for retrieving OFMessage instances. All
 * methods are expected to be thread-safe.
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface OFMessageFactory {
    /**
     * Retrieves an OFMessage instance corresponding to the specified OFType
     * @param t the type of the OFMessage to be retrieved
     * @return an OFMessage instance
     */
    public OFMessage getMessage(OFType t);

    /**
     * Attempts to parse and return all OFMessages contained in the given
     * ByteBuffer, beginning at the ByteBuffer's position, and ending at the
     * ByteBuffer's limit.
     * @param data the ByteBuffer to parse for an OpenFlow message
     * @return a list of OFMessage instances
     */
    public List<OFMessage> parseMessages(ByteBuffer data);

    /**
     * Attempts to parse and return all OFMessages contained in the given
     * ByteBuffer, beginning at the ByteBuffer's position, and ending at the
     * ByteBuffer's limit.
     * @param data the ByteBuffer to parse for an OpenFlow message
     * @param limit the maximum number of messages to return, 0 means no limit
     * @return a list of OFMessage instances
     */
    public List<OFMessage> parseMessages(ByteBuffer data, int limit);

    /**
     * Retrieves an OFActionFactory
     * @return an OFActionFactory
     */
    public OFActionFactory getActionFactory();
}
