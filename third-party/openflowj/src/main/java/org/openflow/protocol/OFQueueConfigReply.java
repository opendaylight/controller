package org.openflow.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.openflow.util.U16;
import org.openflow.protocol.factory.OFQueuePropertyFactory;
import org.openflow.protocol.factory.OFQueuePropertyFactoryAware;
import org.openflow.protocol.queue.OFPacketQueue;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFQueueConfigReply extends OFMessage implements Cloneable, OFQueuePropertyFactoryAware {
    public static int MINIMUM_LENGTH = 16;

    protected OFQueuePropertyFactory queuePropertyFactory;

    protected short port;
    protected List<OFPacketQueue> queues;

    /**
     * 
     */
    public OFQueueConfigReply() {
        super();
        this.type = OFType.QUEUE_CONFIG_REPLY;
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
    public OFQueueConfigReply setPort(short port) {
        this.port = port;
        return this;
    }

    /**
     * @return the queues
     */
    public List<OFPacketQueue> getQueues() {
        return queues;
    }

    /**
     * @param queues the queues to set
     */
    public void setQueues(List<OFPacketQueue> queues) {
        this.queues = queues;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.port = data.getShort();
        data.getShort(); // pad
        data.getInt(); // pad
        int remaining = this.getLengthU() - MINIMUM_LENGTH;
        if (data.remaining() < remaining)
            remaining = data.remaining();
        this.queues = new ArrayList<OFPacketQueue>();
        while (remaining >= OFPacketQueue.MINIMUM_LENGTH) {
            OFPacketQueue queue = new OFPacketQueue();
            queue.setQueuePropertyFactory(this.queuePropertyFactory);
            queue.readFrom(data);
            remaining -= U16.f(queue.getLength());
            this.queues.add(queue);
        }
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putShort(this.port);
        data.putShort((short) 0); // pad
        data.putInt(0); // pad
        if (this.queues != null) {
            for (OFPacketQueue queue : this.queues) {
                queue.writeTo(data);
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 4549;
        int result = super.hashCode();
        result = prime * result + port;
        result = prime * result + ((queues == null) ? 0 : queues.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof OFQueueConfigReply))
            return false;
        OFQueueConfigReply other = (OFQueueConfigReply) obj;
        if (port != other.port)
            return false;
        if (queues == null) {
            if (other.queues != null)
                return false;
        } else if (!queues.equals(other.queues))
            return false;
        return true;
    }

    @Override
    public void setQueuePropertyFactory(
            OFQueuePropertyFactory queuePropertyFactory) {
        this.queuePropertyFactory = queuePropertyFactory;
    }

    @Override
    public OFQueueConfigReply clone() {
        try {
            OFQueueConfigReply clone = (OFQueueConfigReply) super.clone();
            if (this.queues != null) {
                List<OFPacketQueue> queues = new ArrayList<OFPacketQueue>();
                for (OFPacketQueue queue : this.queues) {
                    queues.add(queue.clone());
                }
                clone.setQueues(queues);
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "OFQueueConfigReply [port=" + U16.f(port) + ", queues=" + queues
                + ", xid=" + xid + "]";
    }
}
