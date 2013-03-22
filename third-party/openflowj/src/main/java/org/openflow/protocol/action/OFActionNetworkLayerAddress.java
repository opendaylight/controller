/**
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
package org.openflow.protocol.action;

import java.nio.ByteBuffer;

/**
 * Represents an ofp_action_nw_addr
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
public abstract class OFActionNetworkLayerAddress extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    protected int networkAddress;

    /**
     * @return the networkAddress
     */
    public int getNetworkAddress() {
        return networkAddress;
    }

    /**
     * @param networkAddress the networkAddress to set
     */
    public void setNetworkAddress(int networkAddress) {
        this.networkAddress = networkAddress;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.networkAddress = data.getInt();
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putInt(this.networkAddress);
    }

    @Override
    public int hashCode() {
        final int prime = 353;
        int result = super.hashCode();
        result = prime * result + networkAddress;
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
        if (!(obj instanceof OFActionNetworkLayerAddress)) {
            return false;
        }
        OFActionNetworkLayerAddress other = (OFActionNetworkLayerAddress) obj;
        if (networkAddress != other.networkAddress) {
            return false;
        }
        return true;
    }
}