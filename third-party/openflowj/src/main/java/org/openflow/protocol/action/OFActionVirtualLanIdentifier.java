/**
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
package org.openflow.protocol.action;

import java.nio.ByteBuffer;

/**
 * Represents an ofp_action_vlan_vid
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
public class OFActionVirtualLanIdentifier extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    protected short virtualLanIdentifier;

    public OFActionVirtualLanIdentifier() {
        super.setType(OFActionType.SET_VLAN_VID);
        super.setLength((short) MINIMUM_LENGTH);
    }

    /**
     * @return the virtualLanIdentifier
     */
    public short getVirtualLanIdentifier() {
        return virtualLanIdentifier;
    }

    /**
     * @param virtualLanIdentifier the virtualLanIdentifier to set
     */
    public void setVirtualLanIdentifier(short virtualLanIdentifier) {
        this.virtualLanIdentifier = virtualLanIdentifier;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.virtualLanIdentifier = data.getShort();
        data.getShort();
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putShort(this.virtualLanIdentifier);
        data.putShort((short) 0);
    }

    @Override
    public int hashCode() {
        final int prime = 383;
        int result = super.hashCode();
        result = prime * result + virtualLanIdentifier;
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
        if (!(obj instanceof OFActionVirtualLanIdentifier)) {
            return false;
        }
        OFActionVirtualLanIdentifier other = (OFActionVirtualLanIdentifier) obj;
        if (virtualLanIdentifier != other.virtualLanIdentifier) {
            return false;
        }
        return true;
    }
}