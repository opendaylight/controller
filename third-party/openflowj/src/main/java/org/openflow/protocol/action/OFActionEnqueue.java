/**
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
package org.openflow.protocol.action;

import java.nio.ByteBuffer;

/**
 * Represents an ofp_action_enqueue
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
public class OFActionEnqueue extends OFAction {
    public static int MINIMUM_LENGTH = 16;

    protected short port;
    protected int queueId;

    public OFActionEnqueue() {
        super.setType(OFActionType.OPAQUE_ENQUEUE);
        super.setLength((short) MINIMUM_LENGTH);
    }

    /**
     * Get the output port
     * @return
     */
    public short getPort() {
        return this.port;
    }

    /**
     * Set the output port
     * @param port
     */
    public void setPort(short port) {
        this.port = port;
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
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.port = data.getShort();
        data.getShort();
        data.getInt();
        this.queueId = data.getInt();
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putShort(this.port);
        data.putShort((short) 0);
        data.putInt(0);
        data.putInt(this.queueId);
    }

    @Override
    public int hashCode() {
        final int prime = 349;
        int result = super.hashCode();
        result = prime * result + port;
        result = prime * result + queueId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof OFActionEnqueue)) {
            return false;
        }
        OFActionEnqueue other = (OFActionEnqueue) obj;
        if (port != other.port) {
            return false;
        }
        if (queueId != other.queueId) {
            return false;
        }
        return true;
    }
}