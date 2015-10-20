/**
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
package org.openflow.protocol.action;

import java.nio.ByteBuffer;

/**
 * Represents an ofp_action_enqueue
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
public class OFActionNetworkTypeOfService extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    protected byte networkTypeOfService;

    public OFActionNetworkTypeOfService() {
        super.setType(OFActionType.SET_NW_TOS);
        super.setLength((short) MINIMUM_LENGTH);
    }

    /**
     * @return the networkTypeOfService
     */
    public byte getNetworkTypeOfService() {
        return networkTypeOfService;
    }

    /**
     * @param networkTypeOfService the networkTypeOfService to set
     */
    public void setNetworkTypeOfService(byte networkTypeOfService) {
        this.networkTypeOfService = networkTypeOfService;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.networkTypeOfService = data.get();
        data.getShort();
        data.get();
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.put(this.networkTypeOfService);
        data.putShort((short) 0);
        data.put((byte) 0);
    }

    @Override
    public int hashCode() {
        final int prime = 359;
        int result = super.hashCode();
        result = prime * result + networkTypeOfService;
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
        if (!(obj instanceof OFActionNetworkTypeOfService)) {
            return false;
        }
        OFActionNetworkTypeOfService other = (OFActionNetworkTypeOfService) obj;
        if (networkTypeOfService != other.networkTypeOfService) {
            return false;
        }
        return true;
    }
}