package org.openflow.protocol.queue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.openflow.protocol.factory.OFQueuePropertyFactory;
import org.openflow.protocol.factory.OFQueuePropertyFactoryAware;
import org.openflow.util.U16;

/**
 * Corresponds to the struct ofp_packet_queue OpenFlow structure
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFPacketQueue implements Cloneable, OFQueuePropertyFactoryAware {
    public static int MINIMUM_LENGTH = 8;

    protected OFQueuePropertyFactory queuePropertyFactory;

    protected int queueId;
    protected short length;
    protected List<OFQueueProperty> properties;

    /**
     * @return the queueId
     */
    public int getQueueId() {
        return queueId;
    }

    /**
     * @param queueId the queueId to set
     */
    public OFPacketQueue setQueueId(int queueId) {
        this.queueId = queueId;
        return this;
    }

    /**
     * @return the length
     */
    public short getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(short length) {
        this.length = length;
    }

    /**
     * @return the properties
     */
    public List<OFQueueProperty> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    public OFPacketQueue setProperties(List<OFQueueProperty> properties) {
        this.properties = properties;
        return this;
    }

    public void readFrom(ByteBuffer data) {
        this.queueId = data.getInt();
        this.length = data.getShort();
        data.getShort(); // pad
        if (this.queuePropertyFactory == null)
            throw new RuntimeException("OFQueuePropertyFactory not set");
        this.properties = queuePropertyFactory.parseQueueProperties(data,
                U16.f(this.length) - MINIMUM_LENGTH);
    }

    public void writeTo(ByteBuffer data) {
        data.putInt(this.queueId);
        data.putShort(this.length);
        data.putShort((short) 0); // pad
        if (this.properties != null) {
            for (OFQueueProperty queueProperty : this.properties) {
                queueProperty.writeTo(data);
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 6367;
        int result = 1;
        result = prime * result + length;
        result = prime * result
                + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + queueId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof OFPacketQueue))
            return false;
        OFPacketQueue other = (OFPacketQueue) obj;
        if (length != other.length)
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (queueId != other.queueId)
            return false;
        return true;
    }

    @Override
    public OFPacketQueue clone() {
        try {
            OFPacketQueue clone = (OFPacketQueue) super.clone();
            if (this.properties != null) {
                List<OFQueueProperty> queueProps = new ArrayList<OFQueueProperty>();
                for (OFQueueProperty prop : this.properties) {
                    queueProps.add(prop.clone());
                }
                clone.setProperties(queueProps);
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setQueuePropertyFactory(
            OFQueuePropertyFactory queuePropertyFactory) {
        this.queuePropertyFactory = queuePropertyFactory;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OFPacketQueue [queueId=" + queueId + ", properties="
                + properties + "]";
    }
}
