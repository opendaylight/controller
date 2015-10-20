/**
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
package org.openflow.protocol.action;

import java.nio.ByteBuffer;

/**
 * Represents an ofp_action_vlan_pcp
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
public class OFActionVirtualLanPriorityCodePoint extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    protected byte virtualLanPriorityCodePoint;

    public OFActionVirtualLanPriorityCodePoint() {
        super.setType(OFActionType.SET_VLAN_PCP);
        super.setLength((short) MINIMUM_LENGTH);
    }

    /**
     * @return the virtualLanPriorityCodePoint
     */
    public byte getVirtualLanPriorityCodePoint() {
        return virtualLanPriorityCodePoint;
    }

    /**
     * @param virtualLanPriorityCodePoint the virtualLanPriorityCodePoint to set
     */
    public void setVirtualLanPriorityCodePoint(byte virtualLanPriorityCodePoint) {
        this.virtualLanPriorityCodePoint = virtualLanPriorityCodePoint;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.virtualLanPriorityCodePoint = data.get();
        data.getShort(); // pad
        data.get(); // pad
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.put(this.virtualLanPriorityCodePoint);
        data.putShort((short) 0);
        data.put((byte) 0);
    }

    @Override
    public int hashCode() {
        final int prime = 389;
        int result = super.hashCode();
        result = prime * result + virtualLanPriorityCodePoint;
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
        if (!(obj instanceof OFActionVirtualLanPriorityCodePoint)) {
            return false;
        }
        OFActionVirtualLanPriorityCodePoint other = (OFActionVirtualLanPriorityCodePoint) obj;
        if (virtualLanPriorityCodePoint != other.virtualLanPriorityCodePoint) {
            return false;
        }
        return true;
    }
}