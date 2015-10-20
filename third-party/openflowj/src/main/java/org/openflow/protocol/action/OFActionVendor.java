package org.openflow.protocol.action;

import java.nio.ByteBuffer;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFActionVendor extends OFAction {
    public static int MINIMUM_LENGTH = 8;
    
    protected int vendor;

	public enum ActionVendorID {
		AVI_CISCO(0xC);
    	private int value;
    	private ActionVendorID(int value) {
    		this.value = value;
    	}
    	public int getValue() {
    		return this.value;
    	}
	}	

    public OFActionVendor() {
        super();
        super.setType(OFActionType.VENDOR);
        super.setLength((short) MINIMUM_LENGTH);
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
        if (this.vendor == ActionVendorID.AVI_CISCO.getValue()) {
        	ActionVendorOutputNextHop nh = new ActionVendorOutputNextHop();
        	nh.readFrom(data);
        }
        	
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putInt(this.vendor);
    }

    @Override
    public int hashCode() {
        final int prime = 379;
        int result = super.hashCode();
        result = prime * result + vendor;
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
        if (!(obj instanceof OFActionVendor)) {
            return false;
        }
        OFActionVendor other = (OFActionVendor) obj;
        if (vendor != other.vendor) {
            return false;
        }
        return true;
    }
}
