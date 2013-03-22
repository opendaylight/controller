package org.openflow.protocol.action;

	import java.net.Inet4Address;
	import java.net.InetAddress;
	import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.openflow.util.HexString;


	public class ActionVendorOutputNextHop extends OFActionVendor {
		private static final long serialVersionUID = 1L;
		private static int VENDOR_CISCO = 0xC;
		private enum ONHLength {
			ONH_LEN_MIN(16),
			ONH_LEN_P2P(16),
			ONH_LEN_IPV4(24),
			ONH_LEN_MAC(32),
			ONH_LEN_IPV6(40);
	    	private int value;
	    	private ONHLength(int value) {
	    		this.value = value;
	    	}
	    	public int getValue() {
	    		return this.value;
	    	}
		}	
		private enum ONHActionType {
			ONH_ACTION_NONE(0),
			ONH_ACTION_OUTPUT_NH(1),
			ONH_ACTION_NETFLOW(2);
	    	private int value;
	    	private ONHActionType(int value) {
	    		this.value = value;
	    	}
	    	public int getValue() {
	    		return this.value;
	    	}
		}
		private enum ONHAddressType {
	    	ONH_ADDRTYPE_NONE(0),
	    	ONH_ADDRTYPE_P2P(1),
	    	ONH_ADDRTYPE_IPV4(2),
	    	ONH_ADDRTYPE_IPV6(3),
	    	ONH_ADDRTYPE_MAC48(4);
	    	private int value;
	    	private ONHAddressType(int value) {
	    		this.value = value;
	    	}
	    	public int getValue() {
	    		return this.value;
	    	}
	    }
	    private enum ONHXAddressType {
	    	ONH_XADDRTYPE_NONE(0),
	    	ONH_XADDRTYPE_PORT(1),
	    	ONH_XADDRTYPE_VPNID(2);
	    	private int value;
	    	private ONHXAddressType(int value) {
	    		this.value = value;
	    	}
	    	public int getValue() {
	    		return this.value;
	    	}
	   }
		protected InetAddress address; 
			
		public ActionVendorOutputNextHop() {
	        super();
	        super.setLength((short)ONHLength.ONH_LEN_MIN.getValue());
			super.setVendor(VENDOR_CISCO);
			this.address = null;
		}
		
		public void setNextHop(InetAddress address) {
			short actionLen;
			if (address instanceof Inet4Address) 
				actionLen = (short)ONHLength.ONH_LEN_IPV4.getValue();
			else
				actionLen = (short)ONHLength.ONH_LEN_IPV6.getValue();
			super.setLength(actionLen);
			this.address = address;
		}
		public InetAddress getNextHop() {
			return this.address;
		}
	    @Override
	    public void readFrom(ByteBuffer data) {
	    	/*
	    	 * For now, only contains the next hop address
	    	 */
	    	//super.readFrom(data); don't need this
	    	
	    	if (data.remaining() < super.getLength()-8) {
	    		/*
	    		 * malformed element, skip over 
	    		 */
	    		data.position(data.remaining());
	    		return;
	    	}
	    	if ((super.getLength() !=  (short)ONHLength.ONH_LEN_IPV4.getValue()) &&
	    		(super.getLength() !=  (short)ONHLength.ONH_LEN_IPV6.getValue())) {
	    		/*
	    		 * mal-formed element, skip over 
	    		 */
	    		data.position(super.getLength());
	    		return;
	    	}
	    	data.getShort();  // skip the ONH_ACTION_OUTPUT_NH
	    	data.getShort(); // skip address and xtraaddress types
	    	data.getInt();     // skip the extra address (8 bytes)
	    	data.getInt();
	    	byte[] a;
	    	if (super.getLength() == (short)ONHLength.ONH_LEN_IPV4.getValue()) {
	    		a = new byte[4];
	    		data.get(a);
	    	} else {
	    		a = new byte[16];
	    		data.get(a);
	    		data.getInt(); //4 bytes pad
	    	}
	    	try {
	    		this.address = InetAddress.getByAddress(a);
	    	} catch (UnknownHostException e) {
	    		e.printStackTrace();
	    	}
	    }

	    @Override
	    public void writeTo(ByteBuffer data) {
	    	byte atype = (byte)(ONHAddressType.ONH_ADDRTYPE_NONE.getValue());
	    	byte xatype = (byte)(ONHXAddressType.ONH_XADDRTYPE_NONE.getValue());
	    	if (address instanceof Inet4Address) 
	    		atype =  (byte)(ONHAddressType.ONH_ADDRTYPE_IPV4.getValue());
	    	else
	    		atype = (byte)(ONHAddressType.ONH_ADDRTYPE_IPV6.getValue());
	    	super.writeTo(data); // this writes the standard  8byte ofp_action_vendor_header
	    	data.putShort((short)(ONHActionType.ONH_ACTION_OUTPUT_NH.getValue()));
	    	data.put(atype);
	    	data.put(xatype);
	    	/*
	    	 * write the xtra address. For now it is all 0
	    	 */
	    	data.putInt(0); // 8-byte pad
	    	data.putInt(0);
	    	/* 
	    	 * write the address only when address type is not P2P
	    	 */
	    	if (atype == (byte)(ONHAddressType.ONH_ADDRTYPE_IPV4.getValue())) {
	    		data.put(address.getAddress()); // no need to pad
	    		//avnh.put(address.getAddress()); 
	    	} else if (atype == (byte)(ONHAddressType.ONH_ADDRTYPE_IPV6.getValue())) {
	    		data.put(address.getAddress());
	    		//avnh.put(address.getAddress());
	    		data.putInt(0); // 4-byte pad
	    		//avnh.putInt(0);
	    	}
	    	ActionVendorOutputNextHop a = new ActionVendorOutputNextHop();
	    	a.setLength((short)24);
	    }

	    @Override
	    public int hashCode() {
	    	final int prime = 347;
	    	int result = super.hashCode();
	    	result = prime * result + address.hashCode();
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
	        if (!(obj instanceof ActionVendorOutputNextHop)) {
	            return false;
	        }
	        ActionVendorOutputNextHop other = (ActionVendorOutputNextHop) obj;
	        if (!other.address.equals(this.address))
	        	return false;
	        return true;
	    }

		public String toString() {
			return ("OutputNextHop: " + address.getHostAddress());
		}

	}

