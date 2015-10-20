package org.openflow.protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.openflow.util.U16;

/**
 * Represents ofp_vendor_header
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFVendor extends OFMessage {
    public static int MINIMUM_LENGTH = 12;

    protected int vendor;
    protected byte[] data;

    public OFVendor() {
        super();
        this.type = OFType.VENDOR;
        this.length = U16.t(MINIMUM_LENGTH);
    }

    /**
     * @return the vendor
     */
    public int getVendor() {
        return vendor;
    }

    /**
     * @param vendor the vendor to set
     */
    public void setVendor(int vendor) {
        this.vendor = vendor;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.vendor = data.getInt();
        if (this.length > MINIMUM_LENGTH) {
            this.data = new byte[this.length - MINIMUM_LENGTH];
            data.get(this.data);
        }
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putInt(this.vendor);
        if (this.data != null)
            data.put(this.data);
    }

    /**
     * @return the data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 337;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(data);
        result = prime * result + vendor;
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        OFVendor other = (OFVendor) obj;
        if (!Arrays.equals(data, other.data))
            return false;
        if (vendor != other.vendor)
            return false;
        return true;
    }
}
