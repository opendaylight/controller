
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.packet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;

/**
 * Abstract class which represents the generic network packet object
 * It provides the basic methods which are common for all the packets,
 * like serialize and deserialize
 *
 *
 */

public abstract class Packet {
    // Access level granted to this packet
    protected boolean writeAccess;
    // When deserialized from wire, packet could result corrupted
    protected boolean corrupted;
    // The packet that encapsulate this packet
    protected Packet parent;
    // The packet encapsulated by this packet
    protected Packet payload;
    // Bit coordinates of packet header fields
    protected Map<String, Pair<Integer, Integer>> hdrFieldCoordMap;
    // Header fields values: Map<FieldName,Value>
    protected Map<String, byte[]> hdrFieldsMap;
    // The class of the encapsulated packet object
    protected Class<? extends Packet> payloadClass;

    public Packet() {
        writeAccess = false;
        corrupted = false;
    }

    public Packet(boolean writeAccess) {
        this.writeAccess = writeAccess;
        this.corrupted = false;
    }

    public Packet getParent() {
        return parent;
    }

    public Packet getPayload() {
        return payload;
    }

    public void setParent(Packet parent) {
        this.parent = parent;
    }

    public void setPayload(Packet payload) {
        this.payload = payload;
    }

    public void setHeaderField(String headerField, byte[] readValue) {
        hdrFieldsMap.put(headerField, readValue);
    }

    /**
     * This method deserializes the data bits obtained from the wire
     * into the respective header and payload which are of type Packet
     * @param byte[] data - data from wire to deserialize
     * @param int bitOffset bit position where packet header starts in data array
     * @param int size of packet in bits
     * @return Packet
     * @throws Exception
     */

    public Packet deserialize(byte[] data, int bitOffset, int size)
            throws Exception {
        String hdrField;
        Integer startOffset = 0, numBits = 0;
        byte[] hdrFieldBytes;

        for (Entry<String, Pair<Integer, Integer>> pairs : hdrFieldCoordMap
                .entrySet()) {
            hdrField = pairs.getKey();
            startOffset = bitOffset + this.getfieldOffset(hdrField);
            numBits = this.getfieldnumBits(hdrField);

            hdrFieldBytes = BitBufferHelper.getBits(data, startOffset, numBits);
            /*
             * Store the raw read value, checks the payload type and
             * set the payloadClass accordingly
             */
            this.setHeaderField(hdrField, hdrFieldBytes);
        }

        postDeserializeCustomOperation(data, startOffset);

        int payloadStart = startOffset + numBits;
        //int payloadSize = size - payloadStart;
        int payloadSize = data.length * NetUtils.NumBitsInAByte - payloadStart;

        if (payloadClass != null) {
            try {
                payload = payloadClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Error parsing payload for Ethernet packet", e);
            }
            payload.deserialize(data, payloadStart, payloadSize);
            payload.setParent(this);
        } else {
            // For now let's discard unparsable payload
        }
        return this;
    }

    /**
     * This method serializes the header and payload bytes from
     * the respective packet class, into a single stream of bytes
     * to be sent on the wire
     * @return byte[] - serialized bytes
     * @throws Exception
     */

    public byte[] serialize() throws Exception {
        byte[] payloadBytes = null;
        int payloadSize = 0;
        int headerSize = this.getHeaderSize();
        int payloadByteOffset = headerSize / NetUtils.NumBitsInAByte;
        int size = 0;

        if (payload != null) {
            payloadBytes = payload.serialize();
            payloadSize = payloadBytes.length * NetUtils.NumBitsInAByte;
        }

        size = headerSize + payloadSize;
        int length = size / NetUtils.NumBitsInAByte;
        byte headerBytes[] = new byte[length];

        if (payload != null) {
            System.arraycopy(payloadBytes, 0, headerBytes, payloadByteOffset,
                    payloadBytes.length);
        }

        String field;
        byte[] fieldBytes;
        Integer startOffset, numBits;

        for (Map.Entry<String, Pair<Integer, Integer>> pairs : hdrFieldCoordMap
                .entrySet()) {
            field = pairs.getKey();
            fieldBytes = hdrFieldsMap.get(field);
            // Let's skip optional fields when not set
            if (fieldBytes != null) {
                startOffset = this.getfieldOffset(field);
                numBits = this.getfieldnumBits(field);
                BitBufferHelper.setBytes(headerBytes, fieldBytes, startOffset,
                        numBits);
            }
        }
        postSerializeCustomOperation(headerBytes);

        return headerBytes;
    }

    /**
     * This method gets called at the end of the serialization process
     * It is intended for the child packets to insert some custom data
     * into the output byte stream which cannot be done or cannot be done
     * efficiently during the normal Packet.serialize() path.
     * An example is the checksum computation for IPv4
     * @param byte[] - serialized bytes
     */
    protected void postSerializeCustomOperation(byte[] myBytes)
            throws Exception {
        // no op
    }

    /**
     * This method re-computes the checksum of the bits received on the
     * wire and validates it with the checksum in the bits received
     * Since the computation of checksum varies based on the protocol,
     * this method is overridden
     * Currently only IPv4 does checksum computation and validation
     * TCP and UDP need to implement these if required
     * @param byte[] data
     * @param int endBitOffset
     * @return void
     */
    protected void postDeserializeCustomOperation(byte[] data, int endBitOffset)
            throws Exception {
        // 		no op
    }

    /**
     * Gets the header length in bits
     * @return int
     * @throws Exception
     */
    public int getHeaderSize() throws Exception {
        int size = 0;
        /*
         *  We need to iterate over the fields that were read in the frame (hdrFieldsMap)
         *  not all the possible ones described in hdrFieldCoordMap.
         *  For ex, 802.1Q may or may not be there
         */
        for (Map.Entry<String, byte[]> fieldEntry : hdrFieldsMap.entrySet()) {
            if (fieldEntry.getValue() != null) {
                String field = fieldEntry.getKey();
                size += getfieldnumBits(field);
            }
        }
        return size;
    }

    /**
     * This method fetches the start bit offset for header field specified by
     * 'fieldname'.  The offset is present in the hdrFieldCoordMap of the respective
     * packet class
     * @param String fieldName
     * @return Integer - startOffset of the requested field
     */
    public int getfieldOffset(String fieldName) {
        return (((Pair<Integer, Integer>) hdrFieldCoordMap.get(fieldName))
                .getLeft());
    }

    /**
     * This method fetches the number of bits for header field specified by
     * 'fieldname'.  The numBits are present in the hdrFieldCoordMap of the respective
     * packet class
     * @param String fieldName
     * @return Integer - number of bits of the requested field
     */
    public int getfieldnumBits(String fieldName) throws Exception {
        return (((Pair<Integer, Integer>) hdrFieldCoordMap.get(fieldName))
                .getRight());
    }

    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer();
        for (Map.Entry<String, byte[]> entry : hdrFieldsMap.entrySet()) {
            ret.append(entry.getKey() + ": ");
            if (entry.getValue().length == 6) {
                ret.append(HexEncode.bytesToHexString(entry.getValue()) + " ");
            } else if (entry.getValue().length == 4) {
                try {
                    ret.append(InetAddress.getByAddress(entry.getValue())
                            .getHostAddress()
                            + " ");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            } else {
                ret.append(((Long) BitBufferHelper.getLong(entry.getValue()))
                        .toString()
                        + " ");
            }
        }
        return ret.toString();
    }

    /**
     * Returns true if the packet is corrupted
     * @return boolean
     */
    protected boolean isPacketCorrupted() {
        return corrupted;
    }
}
