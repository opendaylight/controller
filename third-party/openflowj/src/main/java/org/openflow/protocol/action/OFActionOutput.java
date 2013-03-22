/**
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
package org.openflow.protocol.action;

import java.nio.ByteBuffer;

import org.openflow.util.U16;

/**
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 */
public class OFActionOutput extends OFAction implements Cloneable {
    public static int MINIMUM_LENGTH = 8;

    protected short port;
    protected short maxLength;

    public OFActionOutput() {
        super.setType(OFActionType.OUTPUT);
        super.setLength((short) MINIMUM_LENGTH);
    }

    public OFActionOutput(short port, short maxLength) {
        super();
        super.setType(OFActionType.OUTPUT);
        super.setLength((short) MINIMUM_LENGTH);
        this.port = port;
        this.maxLength = maxLength;
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
    public OFActionOutput setPort(short port) {
        this.port = port;
        return this;
    }

    /**
     * Get the max length to send to the controller
     * @return
     */
    public short getMaxLength() {
        return this.maxLength;
    }

    /**
     * Set the max length to send to the controller
     * @param maxLength
     */
    public OFActionOutput setMaxLength(short maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.port = data.getShort();
        this.maxLength = data.getShort();
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putShort(port);
        data.putShort(maxLength);
    }

    @Override
    public int hashCode() {
        final int prime = 367;
        int result = super.hashCode();
        result = prime * result + maxLength;
        result = prime * result + port;
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
        if (!(obj instanceof OFActionOutput)) {
            return false;
        }
        OFActionOutput other = (OFActionOutput) obj;
        if (maxLength != other.maxLength) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OFActionOutput [maxLength=" + maxLength + ", port=" + U16.f(port)
                + ", length=" + length + ", type=" + type + "]";
    }
}