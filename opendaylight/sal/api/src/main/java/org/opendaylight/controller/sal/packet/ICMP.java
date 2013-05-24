
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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.sal.utils.NetUtils;

/**
 * Class that represents the ICMP packet objects
 */

public class ICMP extends Packet {
    private static final String TYPE = "Type";
    private static final String CODE = "Code";
    private static final String CHECKSUM = "Checksum";
    private static final String IDENTIFIER = "Identifier";
    private static final String SEQNUMBER = "SequenceNumber";

    private static Map<String, Pair<Integer, Integer>> fieldCoordinates = new LinkedHashMap<String, Pair<Integer, Integer>>() {
        private static final long serialVersionUID = 1L;
        {
            put(TYPE, new ImmutablePair<Integer, Integer>(0, 8));
            put(CODE, new ImmutablePair<Integer, Integer>(8, 8));
            put(CHECKSUM, new ImmutablePair<Integer, Integer>(16, 16));
            put(IDENTIFIER, new ImmutablePair<Integer, Integer>(32, 16));
            put(SEQNUMBER, new ImmutablePair<Integer, Integer>(48, 16));
        }
    };

    /**
     * Default constructor that creates and sets the hash map values
     */
    public ICMP() {
        super();
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    /**
     * Constructor that sets the access level for the packet
     */
    public ICMP(boolean writeAccess) {
        super(writeAccess);
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    private final Map<String, byte[]> fieldValues;

    @Override
    public void setHeaderField(String headerField, byte[] readValue) {
        hdrFieldsMap.put(headerField, readValue);
    }

    /**
     * Sets the type for the current ICMP message
     * 
     * @param type
     *            The ICMP message type
     * @return This ICMP object
     */
    public ICMP setType(byte type) {
        byte[] icmpType = BitBufferHelper.toByteArray(type);
        fieldValues.put(TYPE, icmpType);
        return this;
    }

    /**
     * Sets the ICMP code (type subtype) for the current ICMP object instance
     * 
     * @param code
     *            The ICMP message type subtype
     * @return This ICMP object
     */
    public ICMP setCode(byte code) {
        byte[] icmpCode = BitBufferHelper.toByteArray(code);
        fieldValues.put(CODE, icmpCode);
        return this;
    }

    /**
     * Sets the ICMP checksum  for the current ICMP object instance
     * @param short - checksum
     * @return ICMP
     */
    public ICMP setChecksum(short checksum) {
        byte[] icmpChecksum = BitBufferHelper.toByteArray(checksum);
        fieldValues.put(CHECKSUM, icmpChecksum);
        return this;
    }

    /**
     * Sets the ICMP identifier  for the current ICMP object instance
     * @param short - identifier
     * @return ICMP
     */
    public ICMP setIdentifier(short identifier) {
        byte[] icmpIdentifier = BitBufferHelper.toByteArray(identifier);
        fieldValues.put(IDENTIFIER, icmpIdentifier);
        return this;
    }

    /**
     * Sets the ICMP sequence number for the current ICMP object instance
     * @param short - seqNumber
     * @return ICMP
     */
    public ICMP setSequenceNumber(short seqNumber) {
        byte[] icmpSeqNumber = BitBufferHelper.toByteArray(seqNumber);
        fieldValues.put(SEQNUMBER, icmpSeqNumber);
        return this;
    }

    /**
     * Gets the header size in bits
     * @return The ICMP header size in bits
     */
    @Override
    public int getHeaderSize() {
        return 64;
    }

    /**
     * Computes the ICMP checksum on the serialized ICMP message
     * 
     * @param serialized
     *            The data stream
     * @param start
     *            The byte index on the data stream from which the ICMP packet
     *            starts
     * @return The checksum
     */
    short computeChecksum(byte[] data, int start) {
        int sum = 0, carry = 0, finalSum = 0;
        int end = start + this.getHeaderSize() / NetUtils.NumBitsInAByte
                + rawPayload.length;
        int checksumStartByte = start + getfieldOffset(CHECKSUM)
                / NetUtils.NumBitsInAByte;

        for (int i = start; i <= (end - 1); i = i + 2) {
            // Skip, if the current bytes are checkSum bytes
            if (i == checksumStartByte) {
                continue;
            }
            StringBuffer sbuffer = new StringBuffer();
            sbuffer.append(String.format("%02X", data[i]));
            if (i < (data.length - 1)) {
                sbuffer.append(String.format("%02X", data[i + 1]));
            }
            sum += Integer.valueOf(sbuffer.toString(), 16);
        }
        carry = (sum >> 16) & 0xFF;
        finalSum = (sum & 0xFFFF) + carry;
        return (short) ~((short) finalSum & 0xFFFF);
    }

    @Override
    protected void postSerializeCustomOperation(byte[] serializedBytes)
            throws PacketException {
        byte[] checkSum = BitBufferHelper
                .toByteArray(computeChecksum(serializedBytes, 0));
        try {
            BitBufferHelper.setBytes(serializedBytes, checkSum,
                    getfieldOffset(CHECKSUM), getfieldnumBits(CHECKSUM));
        } catch (BufferException e) {
            throw new PacketException(e.getMessage());
        }
    }

    @Override
    protected void postDeserializeCustomOperation(byte[] data, int endBitOffset) {
        short computedChecksum = computeChecksum(data, endBitOffset / NetUtils.NumBitsInAByte);
        short actualChecksum = BitBufferHelper.getShort(fieldValues.get(CHECKSUM));

        if (computedChecksum != actualChecksum) {
            corrupted = true;
        }
    }

    /**
     * Gets the checksum value stored
     * @return the checksum
     */
    public short getChecksum() {
        return (BitBufferHelper.getShort(fieldValues.get(CHECKSUM)));
    }
}
