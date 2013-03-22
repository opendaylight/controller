package org.openflow.protocol.statistics;

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.openflow.protocol.factory.OFActionFactory;
import org.openflow.protocol.factory.OFActionFactoryAware;

/**
 * The base class for vendor implemented statistics
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFVendorStatistics implements OFStatistics, OFActionFactoryAware, Serializable {
    protected transient OFActionFactory actionFactory;
    protected int vendor;
    protected byte[] body;

    // non-message fields
    protected int length = 0;

    @Override
    public void readFrom(ByteBuffer data) {
        this.vendor = data.getInt();
        if (body == null)
            body = new byte[length - 4];
        data.get(body);
    }

    @Override
    public void writeTo(ByteBuffer data) {
        data.putInt(this.vendor);
        if (body != null)
            data.put(body);
    }

    @Override
    public int hashCode() {
        final int prime = 457;
        int result = 1;
        result = prime * result + vendor;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OFVendorStatistics)) {
            return false;
        }
        OFVendorStatistics other = (OFVendorStatistics) obj;
        if (vendor != other.vendor) {
            return false;
        }
        return true;
    }

    @Override
    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    /**
     * @param actionFactory the actionFactory to set
     */
    @Override
    public void setActionFactory(OFActionFactory actionFactory) {
        this.actionFactory = actionFactory;
    }

    public OFActionFactory getActionFactory() {
        return this.actionFactory;
    }

}
