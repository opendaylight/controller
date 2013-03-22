/**
 *
 */
package org.openflow.io;

import java.util.List;
import org.openflow.protocol.OFMessage;

/**
 * Interface for writing OFMessages to a buffered stream
 *
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 *
 */
public interface OFMessageOutStream {
    /**
     * Write an OpenFlow message to the stream
     * @param m An OF Message
     */
    public void write(OFMessage m) throws java.io.IOException;

    /**
     * Write an OpenFlow message to the stream.
     *  Messages are sent in one large write() for efficiency
     * @param l A list of OF Messages
     */
    public void write(List<OFMessage> l) throws java.io.IOException;

    /**
     * Pushes buffered data out the Stream; this is NOT guranteed to flush all
     * data, multiple flush() calls may be required, until needFlush() returns
     * false.
     */
    public void flush() throws java.io.IOException;

    /**
     * Is there buffered data that needs to be flushed?
     * @return true if there is buffered data and flush() should be called
     */
    public boolean needsFlush();
}
