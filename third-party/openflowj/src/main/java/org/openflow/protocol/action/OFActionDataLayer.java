/**
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
package org.openflow.protocol.action;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.openflow.protocol.OFPhysicalPort;

/**
 * Represents an ofp_action_dl_addr
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
public abstract class OFActionDataLayer extends OFAction {
    public static int MINIMUM_LENGTH = 16;

    protected byte[] dataLayerAddress;

    /**
     * @return the dataLayerAddress
     */
    public byte[] getDataLayerAddress() {
        return dataLayerAddress;
    }

    /**
     * @param dataLayerAddress the dataLayerAddress to set
     */
    public void setDataLayerAddress(byte[] dataLayerAddress) {
        this.dataLayerAddress = dataLayerAddress;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        if (this.dataLayerAddress == null)
            this.dataLayerAddress = new byte[OFPhysicalPort.OFP_ETH_ALEN];
        data.get(this.dataLayerAddress);
        data.getInt();
        data.getShort();
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.put(this.dataLayerAddress);
        data.putInt(0);
        data.putShort((short) 0);
    }

    @Override
    public int hashCode() {
        final int prime = 347;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(dataLayerAddress);
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
        if (!(obj instanceof OFActionDataLayer)) {
            return false;
        }
        OFActionDataLayer other = (OFActionDataLayer) obj;
        if (!Arrays.equals(dataLayerAddress, other.dataLayerAddress)) {
            return false;
        }
        return true;
    }
}