package org.openflow.protocol;

import java.nio.ByteBuffer;

import org.openflow.util.U16;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFQueueConfigRequest extends OFMessage implements Cloneable {
    public static int MINIMUM_LENGTH = 12;

    protected short port;

    /**
     * 
     */
    public OFQueueConfigRequest() {
        super();
        this.type = OFType.QUEUE_CONFIG_REQUEST;
        this.length = U16.t(MINIMUM_LENGTH);
    }

    /**
     * @return the port
     */
    public short getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(short port) {
        this.port = port;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.port = data.getShort();
        data.get(); // pad
        data.get(); // pad
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putShort(this.port);
        data.putShort((short) 0); // pad
    }

    @Override
    public int hashCode() {
        final int prime = 7211;
        int result = super.hashCode();
        result = prime * result + port;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof OFQueueConfigRequest))
            return false;
        OFQueueConfigRequest other = (OFQueueConfigRequest) obj;
        if (port != other.port)
            return false;
        return true;
    }

    @Override
    public OFQueueConfigRequest clone() {
        try {
            return (OFQueueConfigRequest) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
