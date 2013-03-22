package org.openflow.protocol.queue;

import java.nio.ByteBuffer;

import org.openflow.util.U16;

/**
 * Corresponds to the struct ofp_queue_prop_header OpenFlow structure
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFQueueProperty implements Cloneable {
    public static int MINIMUM_LENGTH = 8;

    protected OFQueuePropertyType type;
    protected short length;

    /**
     * @return the type
     */
    public OFQueuePropertyType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(OFQueuePropertyType type) {
        this.type = type;
    }

    /**
     * @return the length
     */
    public short getLength() {
        return length;
    }

    /**
     * Returns the unsigned length
     *
     * @return the length
     */
    public int getLengthU() {
        return U16.f(length);
    }

    /**
     * @param length the length to set
     */
    public void setLength(short length) {
        this.length = length;
    }

    public void readFrom(ByteBuffer data) {
        this.type = OFQueuePropertyType.valueOf(data.getShort());
        this.length = data.getShort();
        data.getInt(); // pad
    }

    public void writeTo(ByteBuffer data) {
        data.putShort(this.type.getTypeValue());
        data.putShort(this.length);
        data.putInt(0); // pad
    }

    @Override
    public int hashCode() {
        final int prime = 2777;
        int result = 1;
        result = prime * result + length;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof OFQueueProperty))
            return false;
        OFQueueProperty other = (OFQueueProperty) obj;
        if (length != other.length)
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    protected OFQueueProperty clone() {
        try {
            return (OFQueueProperty) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
