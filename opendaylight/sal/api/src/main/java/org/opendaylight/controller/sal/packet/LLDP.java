
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.packet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.sal.utils.NetUtils;

/**
 * Class that represents the LLDP frame objects
 */

public class LLDP extends Packet {
	private static final String CHASSISID = "ChassisId";
	private static final String PORTID = "PortId";
	private static final String TTL = "TTL";
	private static final int LLDPDefaultTlvs = 3;
	private static LLDPTLV emptyTLV = new LLDPTLV().setLength((short)0).setType((byte)0);
	public static final byte[] LLDPMulticastMac = {1,(byte)0x80,(byte)0xc2, 0, 0,(byte)0xe};
	private Map<Byte, LLDPTLV> tlvList;

	/**
	 * Default constructor that creates the tlvList LinkedHashMap
	 */
	public LLDP() {
		super();
		tlvList = new LinkedHashMap<Byte,LLDPTLV>(LLDPDefaultTlvs);
	}

	/**
	 * Constructor that creates the tlvList LinkedHashMap and sets
	 * the write access for the same
	 */
	public LLDP (boolean writeAccess) {
		super(writeAccess);
		tlvList = new LinkedHashMap<Byte,LLDPTLV>(LLDPDefaultTlvs);	// Mandatory TLVs	
	}
	
	/**
	 * @param String - description of the type of TLV
	 * @return byte - type of TLV
	 */
	private byte getType(String typeDesc) {
		if (typeDesc.equals(CHASSISID)) {
			return LLDPTLV.TLVType.ChassisID.getValue();
		} else if (typeDesc.equals(PORTID)) {
			return LLDPTLV.TLVType.PortID.getValue();
		} else if (typeDesc.equals(TTL)) {
			return LLDPTLV.TLVType.TTL.getValue();
		} else {
			return LLDPTLV.TLVType.Unknown.getValue();
		}			
	}

	/**
	 * @param String - description of the type of TLV
	 * @return LLDPTLV - full TLV
	 */
    public LLDPTLV getTLV(String type) {
    	return tlvList.get(getType(type));
    }

	/**
	 * @param String - description of the type of TLV
	 * @param LLDPTLV - tlv to set
	 * @return void
	 */
    public void setTLV(String type, LLDPTLV tlv) {
    	tlvList.put(getType(type), tlv);
    }

    /**
     * @return the chassisId TLV
     */
    public LLDPTLV getChassisId() {
    	return getTLV(CHASSISID);
    }

    /**
     * @param LLDPTLV - the chassisId to set
     */
    public LLDP setChassisId(LLDPTLV chassisId) {
    	tlvList.put(getType(CHASSISID), chassisId);
        return this;
    }
    
    /**
     * @return LLDPTLV - the portId TLV
     */
    public LLDPTLV getPortId() {
    	return tlvList.get(getType(PORTID));
    }

    /**
     * @param LLDPTLV - the portId to set
     * @return LLDP
     */
    public LLDP setPortId(LLDPTLV portId) {
    	tlvList.put(getType(PORTID), portId);
        return this;
    }

    /**
     * @return LLDPTLV - the ttl TLV
     */
    public LLDPTLV getTtl() {
    	return tlvList.get(getType(TTL));
    }

    /**
     * @param LLDPTLV - the ttl to set
     * @return LLDP
     */
    public LLDP setTtl(LLDPTLV ttl) {
    	tlvList.put(getType(TTL), ttl);
        return this;
    }

    /**
     * @return the optionalTLVList
     */
    public List<LLDPTLV> getOptionalTLVList() {
    	List<LLDPTLV> list = new ArrayList<LLDPTLV>();
    	for (Map.Entry<Byte,LLDPTLV> entry : tlvList.entrySet()) {
    		byte type = entry.getKey();
    		if ((type == LLDPTLV.TLVType.ChassisID.getValue()) ||
    			(type == LLDPTLV.TLVType.PortID.getValue()) ||
    			(type == LLDPTLV.TLVType.TTL.getValue())) {
    			continue;
    		} else {
    			list.add(entry.getValue());
    		}
    	}
        return list;
    }

    /**
     * @param optionalTLVList the optionalTLVList to set
     * @return LLDP
     */
    public LLDP setOptionalTLVList(List<LLDPTLV> optionalTLVList)
    {
    	for (LLDPTLV tlv : optionalTLVList) {
    		tlvList.put(tlv.getType(), tlv);
    	}
        return this;
    }

    @Override
    public Packet deserialize (byte[] data, int bitOffset, int size) throws Exception {
    	int lldpOffset = bitOffset; // LLDP start 
    	int lldpSize = size; // LLDP size

    	/*
    	 * Deserialize the TLVs until we reach the end of the packet
    	 */
    	
    	while (lldpSize > 0) {
    		LLDPTLV tlv = new LLDPTLV();
    		tlv.deserialize(data, lldpOffset, lldpSize);
    		lldpOffset += tlv.getTLVSize(); //Size of current TLV in bits
    		lldpSize -= tlv.getTLVSize();
    		this.tlvList.put(tlv.getType(), tlv);
    	}
		return this;
	}
    
    @Override
    public byte[] serialize() throws Exception {
		int startOffset = 0;
		byte[] serializedBytes = new byte[getLLDPPacketLength()];
		
		for (Map.Entry<Byte, LLDPTLV> entry : tlvList.entrySet()) {
			LLDPTLV tlv = entry.getValue();
			int numBits = tlv.getTLVSize();
			BitBufferHelper.setBytes(serializedBytes, tlv.serialize(), startOffset, numBits);
			startOffset += numBits;
		}
		// Now add the empty LLDPTLV at the end
		BitBufferHelper.setBytes(serializedBytes, LLDP.emptyTLV.serialize(), startOffset, LLDP.emptyTLV.getTLVSize());
		
		return serializedBytes;
    }
    
    /**
     * Returns the size of LLDP packet in bytes
     * @return int - LLDP Packet size in bytes
     * @throws Exception 
     */
    private int getLLDPPacketLength() throws Exception {
    	int len = 0;
    	LLDPTLV tlv;
    	
    	for (Map.Entry<Byte, LLDPTLV> entry : this.tlvList.entrySet()) {
    		tlv = entry.getValue();
    		len += tlv.getTLVSize();
    	}
    	len += LLDP.emptyTLV.getTLVSize();
    	
    	return len/NetUtils.NumBitsInAByte;
    }
}
