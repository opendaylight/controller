package org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openflow.protocol.OFError;

public class V6Error extends OFError {


    private static final long serialVersionUID = 1L;
    public static int MINIMUM_LENGTH = 20;//OfHdr(8) + NXET_VENDOR(2) + NXEC_VENDOR_ERROR(2) + struct nx_vendor_error(8)
    public static final short NICIRA_VENDOR_ERRORTYPE = (short)0xb0c2;
    protected int V6VendorId;
    protected short V6VendorErrorType;
    protected short V6VendorErrorCode;
    protected byte[] V6ErrorData;

    public V6Error(OFError e) {
        this.length = (short)e.getLengthU();
        this.errorType = e.getErrorType();
        this.errorCode = e.getErrorCode();
        this.xid = e.getXid();
    }

    /*
    @Override
    public void readFrom(ByteBuffer data) {
        this.V6VendorId = data.getInt();
        this.V6VendorErrorType = data.getShort();
        this.V6VendorErrorCode = data.getShort();
        int dataLength = this.getLengthU() - MINIMUM_LENGTH;
        if (dataLength > 0) {
            this.V6ErrorData = new byte[dataLength];
            data.get(this.V6ErrorData);
        }
    }
    */

    /**
     * @return the V6VendorId
     */
    public int getVendorId() {
        return V6VendorId;
    }



    /**
     * @return the V6VendorErrorType
     */
    /*
    public short getVendorErrorType() {
        return V6VendorErrorType;
    }
    */

    /**
     * @return the VendorErrorType
     */
    public short getVendorErrorCode() {
        return V6VendorErrorCode;
    }

    /**
     * @return the Error Bytes
     */
    public byte[] getError() {
        return V6ErrorData;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return "V6Error[" + ReflectionToStringBuilder.toString(this) + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }


}
