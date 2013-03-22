
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.packet;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;

/**
 * Class that represents the LLDPTLV objects
 */

public class LLDPTLV extends Packet {
    private static final String TYPE = "Type";
    private static final String LENGTH = "Length";
    private static final String VALUE = "Value";
    private static final int LLDPTLVFields = 3;
	public static final byte[] OFOUI = new byte[] {(byte)0x00, (byte)0x26, (byte)0xe1};	// OpenFlow OUI
	public static final byte[] customTlvSubType = new byte[] {0};
	public static final int customTlvOffset = OFOUI.length + customTlvSubType.length;
	public static final byte chassisIDSubType[] = new byte[] {4}; 	// MAC address for the system
	public static final byte portIDSubType[] = new byte[] {7}; 	// locally assigned

	public enum TLVType {
		Unknown		((byte)0),
		ChassisID	((byte)1),
		PortID		((byte)2),
		TTL			((byte)3),
		PortDesc	((byte)4),
		SystemName	((byte)5),
		SystemDesc	((byte)6),
		Custom		((byte)127);
		
		private byte value;
		private TLVType(byte value) {
			this.value = value;
		}
		public byte getValue() {
			return value;
		}
	}

    private static Map<String, Pair<Integer, Integer>> fieldCoordinates = new LinkedHashMap<String, Pair<Integer, Integer>>() {
        private static final long serialVersionUID = 1L;

        {
            put(TYPE, new MutablePair<Integer, Integer>(0, 7));
            put(LENGTH, new MutablePair<Integer, Integer>(7, 9));
            put(VALUE, new MutablePair<Integer, Integer>(16, 0));
        }
    };

    protected Map<String, byte[]> fieldValues;

    /**
     * Default constructor that creates and sets the hash map values
     * and sets the payload to null
     */
    public LLDPTLV() {
        payload = null;
        fieldValues = new HashMap<String, byte[]>(LLDPTLVFields);
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    /**
     * Constructor that writes the passed LLDPTLV values to the
     * hdrFieldsMap
     */
    public LLDPTLV(LLDPTLV other) {
        for (Map.Entry<String, byte[]> entry : other.hdrFieldsMap.entrySet()) {
            this.hdrFieldsMap.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * @return int - the length of TLV
     */
    public int getLength() {
        return (int) BitBufferHelper.toNumber(fieldValues.get(LENGTH),
                fieldCoordinates.get(LENGTH).getRight().intValue());
    }

    /**
     * @return byte - the type of TLV
     */
    public byte getType() {
        return BitBufferHelper.getByte(fieldValues.get(TYPE));
    }

    /**
     * @return byte[] - the value field of TLV
     */
    public byte[] getValue() {
        return fieldValues.get(VALUE);
    }

    /**
     * @param byte - the type to set
     * @return LLDPTLV
     */
    public LLDPTLV setType(byte type) {
        byte[] lldpTLVtype = { type };
        fieldValues.put(TYPE, lldpTLVtype);
        return this;
    }

    /**
     * @param short - the length to set
     * @return LLDPTLV
     */
    public LLDPTLV setLength(short length) {
        fieldValues.put(LENGTH, BitBufferHelper.toByteArray(length));
        return this;
    }

    /**
     * @param byte[] - the value to set
     * @return LLDPTLV
     */
    public LLDPTLV setValue(byte[] value) {
        fieldValues.put(VALUE, value);
        return this;
    }

    @Override
    public void setHeaderField(String headerField, byte[] readValue) {
        hdrFieldsMap.put(headerField, readValue);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int getfieldnumBits(String fieldName) throws Exception {
        if (fieldName.equals(VALUE)) {
            return (NetUtils.NumBitsInAByte * (int) BitBufferHelper.getShort(
                    fieldValues.get(LENGTH), fieldCoordinates.get(LENGTH)
                            .getRight().intValue()));
        }
        return fieldCoordinates.get(fieldName).getRight();
    }

    /**
     * Returns the size in bits of the whole TLV
     * @return int - size in bits of full TLV
     * @throws Exception
     */
    public int getTLVSize() throws Exception {
        return (LLDPTLV.fieldCoordinates.get(TYPE).getRight() + // static
                LLDPTLV.fieldCoordinates.get(LENGTH).getRight() + // static
        getfieldnumBits(VALUE)); // variable
    }
    
    /**
     * Creates the ChassisID TLV value including the subtype and ChassisID string
     * 
     * @param nodeId node identifier string
     * @return the ChassisID TLV value in byte array
     */
    static public byte[] createChassisIDTLVValue(String nodeId) {
        byte[] cid = HexEncode.bytesFromHexString(nodeId);
        byte[] cidValue = new byte[cid.length + chassisIDSubType.length];

        System.arraycopy(chassisIDSubType, 0, cidValue, 0, chassisIDSubType.length);
        System.arraycopy(cid, 0, cidValue, chassisIDSubType.length, cid.length);

    	return cidValue;
    }

    /**
     * Creates the PortID TLV value including the subtype and PortID string
     * 
     * @param portId port identifier string
     * @return the PortID TLV value in byte array
     */
    static public byte[] createPortIDTLVValue(String portId) {
        byte[] pid = portId.getBytes();
        byte[] pidValue = new byte[pid.length + portIDSubType.length];

        System.arraycopy(portIDSubType, 0, pidValue, 0, portIDSubType.length);
        System.arraycopy(pid, 0, pidValue, portIDSubType.length, pid.length);

    	return pidValue;
    }

    /**
     * Creates the custom TLV value including OUI, subtype and custom string
     * 
     * @param portId port identifier string
     * @return the custom TLV value in byte array
     */
    static public byte[] createCustomTLVValue(String customString) {
        byte[] customArray = customString.getBytes();
        byte[] customValue = new byte[customTlvOffset + customArray.length];

        System.arraycopy(OFOUI, 0, customValue, 0, OFOUI.length);
        System.arraycopy(customTlvSubType, 0, customValue, OFOUI.length,
                customTlvSubType.length);
        System.arraycopy(customArray, 0, customValue, customTlvOffset,
                customArray.length);

    	return customValue;
    }

    /**
     * Retrieves the string from TLV value and returns it in HexString format
     * 
     * @param tlvValue the TLV value
     * @param tlvLen the TLV length
     * @return the HexString
     */
    static public String getHexStringValue(byte[] tlvValue, int tlvLen) {
    	byte[] cidBytes = new byte[tlvLen - chassisIDSubType.length];    	     
        System.arraycopy(tlvValue, chassisIDSubType.length, cidBytes, 0, cidBytes.length);
    	return HexEncode.bytesToHexStringFormat(cidBytes);
    }

    /**
     * Retrieves the string from TLV value
     * 
     * @param tlvValue the TLV value
     * @param tlvLen the TLV length
     * @return the string
     */
    static public String getStringValue(byte[] tlvValue, int tlvLen) {
    	byte[] pidBytes = new byte[tlvLen - portIDSubType.length];
        System.arraycopy(tlvValue, portIDSubType.length, pidBytes, 0, pidBytes.length);
    	return (new String(pidBytes));
    }

    /**
     * Retrieves the custom string from the Custom TLV value which includes OUI, subtype and custom string
     * 
     * @param customTlvValue the custom TLV value
     * @param customTlvLen the custom TLV length
     * @return the custom string
     */
    static public String getCustomString(byte[] customTlvValue, int customTlvLen) {
        String customString = "";
        byte[] vendor = new byte[3];
        System.arraycopy(customTlvValue, 0, vendor, 0, vendor.length);
        if (Arrays.equals(vendor, LLDPTLV.OFOUI)) {
            int customArrayLength = customTlvLen - customTlvOffset;
            byte[] customArray = new byte[customArrayLength];
            System.arraycopy(customTlvValue, customTlvOffset,
                    customArray, 0, customArrayLength);
            try {
            	customString = new String(customArray, "UTF-8");
            } catch (UnsupportedEncodingException e) {
            }
        }
        
    	return customString;
    }    
}
