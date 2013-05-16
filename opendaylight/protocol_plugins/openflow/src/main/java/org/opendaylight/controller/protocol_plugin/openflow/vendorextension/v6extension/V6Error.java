package org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension;

import java.nio.ByteBuffer;
import java.util.Arrays;

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
    
    /**
     * @return the V6VendorId
     */
    public int getVendorId() {
        return V6VendorId;
    }
    
    /**
     * @return the V6VendorErrorType
     */
    public short getVendorErrorType() {
        return V6VendorErrorType;
    }
    
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
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(V6ErrorData);
        result = prime * result + V6VendorErrorCode;
        result = prime * result + V6VendorErrorType;
        result = prime * result + V6VendorId;
        return result;
    }

    @Override
    public String toString() {
        return "V6Error [V6VendorId=" + V6VendorId + ", V6VendorErrorType="
                + V6VendorErrorType + ", V6VendorErrorCode="
                + V6VendorErrorCode + ", V6ErrorData="
                + Arrays.toString(V6ErrorData) + ", errorType=" + errorType
                + ", errorCode=" + errorCode + ", factory=" + factory
                + ", error=" + Arrays.toString(error) + ", errorIsAscii="
                + errorIsAscii + ", version=" + version + ", type=" + type
                + ", length=" + length + ", xid=" + xid + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        V6Error other = (V6Error) obj;
        if (!Arrays.equals(V6ErrorData, other.V6ErrorData))
            return false;
        if (V6VendorErrorCode != other.V6VendorErrorCode)
            return false;
        if (V6VendorErrorType != other.V6VendorErrorType)
            return false;
        if (V6VendorId != other.V6VendorId)
            return false;
        return true;
    }
}
