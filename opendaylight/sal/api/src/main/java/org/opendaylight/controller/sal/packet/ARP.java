
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.packet;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Class that represents the ARP packet objects
 *
 *
 */

public class ARP extends Packet {
    private static final String HWTYPE = "HardwareType";
    private static final String PTYPE = "ProtocolType";
    private static final String HWADDRLENGTH = "HardwareAddressLength";
    private static final String PADDRLENGTH = "ProtocolAddressLength";
    private static final String OPCODE = "OpCode";
    private static final String SENDERHWADDR = "SenderHardwareAddress";
    private static final String SENDERPADDR = "SenderProtocolAddress";
    private static final String TARGETHWADDR = "TargetHardwareAddress";
    private static final String TARGETPADDR = "TargetProtocolAddress";

    public static short HW_TYPE_ETHERNET = (short) 0x1;
    public static short REQUEST = (short) 0x1;
    public static short REPLY = (short) 0x2;

    private static Map<String, Pair<Integer, Integer>> fieldCoordinates = new LinkedHashMap<String, Pair<Integer, Integer>>() {
        private static final long serialVersionUID = 1L;
        {
            put(HWTYPE, new ImmutablePair<Integer, Integer>(0, 16));
            put(PTYPE, new ImmutablePair<Integer, Integer>(16, 16));
            put(HWADDRLENGTH, new ImmutablePair<Integer, Integer>(32, 8));
            put(PADDRLENGTH, new ImmutablePair<Integer, Integer>(40, 8));
            put(OPCODE, new ImmutablePair<Integer, Integer>(48, 16));
            put(SENDERHWADDR, new ImmutablePair<Integer, Integer>(64, 48));
            put(SENDERPADDR, new ImmutablePair<Integer, Integer>(112, 32));
            put(TARGETHWADDR, new ImmutablePair<Integer, Integer>(144, 48));
            put(TARGETPADDR, new ImmutablePair<Integer, Integer>(192, 32));

        }
    };
    private Map<String, byte[]> fieldValues;

    /**
     * Default constructor that creates and sets the HashMap
     */
    public ARP() {
        super();
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    /**
     * Constructor that sets the access level for the packet and
     * creates and sets the HashMap
     */
    public ARP(boolean writeAccess) {
        super(writeAccess);
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    /**
     * Gets the hardware type from the stored ARP header
     * @return short - the hardwareType
     */
    public short getHardwareType() {
        return (BitBufferHelper.getShort(fieldValues.get(HWTYPE)));

    }

    /**
     * Gets the protocol type from the stored ARP header
     * @return short - the protocolType
     */
    public short getProtocolType() {
        return (BitBufferHelper.getShort(fieldValues.get(PTYPE)));
    }

    /**
     * Gets the hardware address length from the stored ARP header
     * @return byte - the protocolAddressLength
     */
    public byte getHardwareAddressLength() {
        return (BitBufferHelper.getByte(fieldValues.get(HWADDRLENGTH)));
    }

    /**
     * Get the protocol address length from Protocol header
     * @return byte - the protocolAddressLength
     */
    public byte getProtocolAddressLength() {
        return (BitBufferHelper.getByte(fieldValues.get(PADDRLENGTH)));
    }

    /**
     * Gets the opCode from stored ARP header
     * @param short - the opCode to set
     */
    public short getOpCode() {
        return (BitBufferHelper.getShort(fieldValues.get(OPCODE)));
    }

    /**
     * Gets the sender hardware address from the stored ARP header
     * @return byte[] - the senderHardwareAddress
     */
    public byte[] getSenderHardwareAddress() {
        return (fieldValues.get(SENDERHWADDR));
    }

    /**
     * Gets the IP address from the stored ARP header
     * @return byte[] - the senderProtocolAddress
     */
    public byte[] getSenderProtocolAddress() {
        return (fieldValues.get(SENDERPADDR));
    }

    /**
     * Gets the hardware address from the stored ARP header
     * @return byte[] - the targetHardwareAddress
     */
    public byte[] getTargetHardwareAddress() {
        return (fieldValues.get(TARGETHWADDR));
    }

    /**
     * Sets the hardware Type for the current ARP object instance
     * @param short - hardwareType the hardwareType to set
     * @return ARP
     */
    public ARP setHardwareType(short hardwareType) {
        byte[] hwType = BitBufferHelper.toByteArray(hardwareType);
        fieldValues.put(HWTYPE, hwType);
        return this;
    }

    /**
     * Sets the protocol Type for the current ARP object instance
     * @param short - the protocolType to set
     * @return ARP
     */
    public ARP setProtocolType(short protocolType) {
        byte[] protType = BitBufferHelper.toByteArray(protocolType);
        fieldValues.put(PTYPE, protType);
        return this;
    }

    /**
     * Sets the hardware address length for the current ARP object instance
     * @param byte - the hardwareAddressLength to set
     * @return ARP
     */
    public ARP setHardwareAddressLength(byte hardwareAddressLength) {
        byte[] hwAddressLength = BitBufferHelper
                .toByteArray(hardwareAddressLength);
        fieldValues.put(HWADDRLENGTH, hwAddressLength);
        return this;
    }

    /**
     * Sets the Protocol address for the current ARP object instance
     * @param byte - the protocolAddressLength to set
     * @return ARP
     */
    public ARP setProtocolAddressLength(byte protocolAddressLength) {
        byte[] protocolAddrLength = BitBufferHelper
                .toByteArray(protocolAddressLength);
        fieldValues.put(PADDRLENGTH, protocolAddrLength);
        return this;
    }

    /**
     * Sets the opCode for the current ARP object instance
     * @param short - the opCode to set
     * @return ARP
     */
    public ARP setOpCode(short opCode) {
        byte[] operationCode = BitBufferHelper.toByteArray(opCode);
        fieldValues.put(OPCODE, operationCode);
        return this;
    }

    /**
     * Sets the sender hardware address for the current ARP object instance
     * @param byte[] - the senderHardwareAddress to set
     * @return ARP
     */
    public ARP setSenderHardwareAddress(byte[] senderHardwareAddress) {
        fieldValues.put(SENDERHWADDR, senderHardwareAddress);
        return this;
    }

    /**
     * Sets the target hardware address for the current ARP object instance
     * @param byte[] - the targetHardwareAddress to set
     * @return ARP
     */
    public ARP setTargetHardwareAddress(byte[] targetHardwareAddress) {
        fieldValues.put(TARGETHWADDR, targetHardwareAddress);
        return this;
    }

    /**
     * Sets the target protocol address for the current ARP object instance
     * @param byte[] - the targetProtocolAddress to set
     * @return ARP
     */
    public ARP setTargetProtocolAddress(byte[] targetProtocolAddress) {
        fieldValues.put(TARGETPADDR, targetProtocolAddress);
        return this;
    }

    /**
     * Sets the sender protocol address for the current ARP object instance
     * @param byte[] - senderIP
     * @return ARP
     */
    public ARP setSenderProtocolAddress(byte[] senderIP) {
        fieldValues.put(SENDERPADDR, senderIP);
        return this;
    }

    /**
     * Gets the target protocol address
     * @return - byte[] targetProtocolAddress
     */
    public byte[] getTargetProtocolAddress() {
        return fieldValues.get(TARGETPADDR);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
