/*
 * Copyright (c) 2013-2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.packet;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class which represents the generic network packet object It provides
 * the basic methods which are common for all the packets, like serialize and
 * deserialize
 */
@Deprecated
public abstract class Packet {
    protected static final Logger logger = LoggerFactory
            .getLogger(Packet.class);
    // Access level granted to this packet
    protected boolean writeAccess;
    // When deserialized from wire, packet could result corrupted
    protected boolean corrupted;
    // The packet that encapsulate this packet
    protected Packet parent;
    // The packet encapsulated by this packet
    protected Packet payload;
    // The unparsed raw payload carried by this packet
    protected byte[] rawPayload;
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
        corrupted = false;
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
     * This method deserializes the data bits obtained from the wire into the
     * respective header and payload which are of type Packet
     *
     * @param byte[] data - data from wire to deserialize
     * @param int bitOffset bit position where packet header starts in data
     *        array
     * @param int size of packet in bits
     * @return Packet
     * @throws PacketException
     */
    public Packet deserialize(byte[] data, int bitOffset, int size)
            throws PacketException {

        // Deserialize the header fields one by one
        int startOffset = 0, numBits = 0;
        for (Entry<String, Pair<Integer, Integer>> pairs : hdrFieldCoordMap
                .entrySet()) {
            String hdrField = pairs.getKey();
            startOffset = bitOffset + this.getfieldOffset(hdrField);
            numBits = this.getfieldnumBits(hdrField);

            byte[] hdrFieldBytes = null;
            try {
                hdrFieldBytes = BitBufferHelper.getBits(data, startOffset,
                        numBits);
            } catch (BufferException e) {
                throw new PacketException(e.getMessage());
            }

            /*
             * Store the raw read value, checks the payload type and set the
             * payloadClass accordingly
             */
            this.setHeaderField(hdrField, hdrFieldBytes);

            if (logger.isTraceEnabled()) {
                logger.trace("{}: {}: {} (offset {} bitsize {})",
                        new Object[] { this.getClass().getSimpleName(), hdrField,
                        HexEncode.bytesToHexString(hdrFieldBytes),
                        startOffset, numBits });
            }
        }

        // Deserialize the payload now
        int payloadStart = startOffset + numBits;
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
            /*
             *  The payload class was not set, it means no class for parsing
             *  this payload is present. Let's store the raw payload if any.
             */
            int start = payloadStart / NetUtils.NumBitsInAByte;
            int stop = start + payloadSize / NetUtils.NumBitsInAByte;
            rawPayload = Arrays.copyOfRange(data, start, stop);
        }


        // Take care of computation that can be done only after deserialization
        postDeserializeCustomOperation(data, payloadStart - getHeaderSize());

        return this;
    }

    /**
     * This method serializes the header and payload from the respective
     * packet class, into a single stream of bytes to be sent on the wire
     *
     * @return The byte array representing the serialized Packet
     * @throws PacketException
     */
    public byte[] serialize() throws PacketException {

        // Acquire or compute the serialized payload
        byte[] payloadBytes = null;
        if (payload != null) {
            payloadBytes = payload.serialize();
        } else if (rawPayload != null) {
            payloadBytes = rawPayload;
        }
        int payloadSize = (payloadBytes == null) ? 0 : payloadBytes.length;

        // Allocate the buffer to contain the full (header + payload) packet
        int headerSize = this.getHeaderSize() / NetUtils.NumBitsInAByte;
        byte packetBytes[] = new byte[headerSize + payloadSize];
        if (payloadBytes != null) {
            System.arraycopy(payloadBytes, 0, packetBytes, headerSize, payloadSize);
        }

        // Serialize this packet header, field by field
        for (Map.Entry<String, Pair<Integer, Integer>> pairs : hdrFieldCoordMap
                .entrySet()) {
            String field = pairs.getKey();
            byte[] fieldBytes = hdrFieldsMap.get(field);
            // Let's skip optional fields when not set
            if (fieldBytes != null) {
                try {
                    BitBufferHelper.setBytes(packetBytes, fieldBytes,
                            getfieldOffset(field), getfieldnumBits(field));
                } catch (BufferException e) {
                    throw new PacketException(e.getMessage());
                }
            }
        }

        // Perform post serialize operations (like checksum computation)
        postSerializeCustomOperation(packetBytes);

        if (logger.isTraceEnabled()) {
            logger.trace("{}: {}", this.getClass().getSimpleName(),
                    HexEncode.bytesToHexString(packetBytes));
        }

        return packetBytes;
    }

    /**
     * This method gets called at the end of the serialization process It is
     * intended for the child packets to insert some custom data into the output
     * byte stream which cannot be done or cannot be done efficiently during the
     * normal Packet.serialize() path. An example is the checksum computation
     * for IPv4
     *
     * @param byte[] - serialized bytes
     * @throws PacketException
     */
    protected void postSerializeCustomOperation(byte[] myBytes)
            throws PacketException {
        // no op
    }

    /**
     * This method re-computes the checksum of the bits received on the wire and
     * validates it with the checksum in the bits received Since the computation
     * of checksum varies based on the protocol, this method is overridden.
     * Currently only IPv4 and ICMP do checksum computation and validation. TCP
     * and UDP need to implement these if required.
     *
     * @param byte[] data The byte stream representing the Ethernet frame
     * @param int startBitOffset The bit offset from where the byte array corresponding to this Packet starts in the frame
     * @throws PacketException
     */
    protected void postDeserializeCustomOperation(byte[] data, int startBitOffset)
            throws PacketException {
        // no op
    }

    /**
     * Gets the header length in bits
     *
     * @return int the header length in bits
     */
    public int getHeaderSize() {
        int size = 0;
        /*
         * We need to iterate over the fields that were read in the frame
         * (hdrFieldsMap) not all the possible ones described in
         * hdrFieldCoordMap. For ex, 802.1Q may or may not be there
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
     * 'fieldname'. The offset is present in the hdrFieldCoordMap of the
     * respective packet class
     *
     * @param String
     *            fieldName
     * @return Integer - startOffset of the requested field
     */
    public int getfieldOffset(String fieldName) {
        return hdrFieldCoordMap.get(fieldName).getLeft();
    }

    /**
     * This method fetches the number of bits for header field specified by
     * 'fieldname'. The numBits are present in the hdrFieldCoordMap of the
     * respective packet class
     *
     * @param String
     *            fieldName
     * @return Integer - number of bits of the requested field
     */
    public int getfieldnumBits(String fieldName) {
        return hdrFieldCoordMap.get(fieldName).getRight();
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(this.getClass().getSimpleName());
        ret.append(": [");
        for (String field : hdrFieldCoordMap.keySet()) {
            byte[] value = hdrFieldsMap.get(field);
            ret.append(field);
            ret.append(": ");
            ret.append(HexEncode.bytesToHexString(value));
            ret.append(", ");
        }
        ret.replace(ret.length()-2, ret.length()-1, "]");
        return ret.toString();
    }

    /**
     * Returns the raw payload carried by this packet in case payload was not
     * parsed. Caller can call this function in case the getPaylod() returns null.
     *
     * @return The raw payload if not parsable as an array of bytes, null otherwise
     */
    public byte[] getRawPayload() {
        return rawPayload;
    }

    /**
     * Set a raw payload in the packet class
     *
     * @param payload The raw payload as byte array
     */
    public void setRawPayload(byte[] payload) {
        this.rawPayload = Arrays.copyOf(payload, payload.length);
    }

    /**
     * Return whether the deserialized packet is to be considered corrupted.
     * This is the case when the checksum computed after reconstructing the
     * packet received from wire is not equal to the checksum read from the
     * stream. For the Packet class which do not have a checksum field, this
     * function will always return false.
     *
     *
     * @return true if the deserialized packet's recomputed checksum is not
     *         equal to the packet carried checksum
     */
    public boolean isCorrupted() {
        return corrupted;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((this.hdrFieldsMap == null) ? 0 : hdrFieldsMap.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Packet other = (Packet) obj;
        if (hdrFieldsMap == other.hdrFieldsMap) {
            return true;
        }
        if (hdrFieldsMap == null || other.hdrFieldsMap == null) {
            return false;
        }
        if (hdrFieldsMap != null && other.hdrFieldsMap != null) {
            for (String field : hdrFieldsMap.keySet()) {
                if (!Arrays.equals(hdrFieldsMap.get(field), other.hdrFieldsMap.get(field))) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Adds to the passed Match this packet's header fields
     *
     * @param match
     *            The Match object to populate
     */
    public void populateMatch(Match match) {
        // To be overridden by derived packet classes which have well known
        // header fields so that Packet.getMatch would return desired result
    }

    /**
     * Returns the Match object containing this packet and its payload
     * encapsulated packets' header fields
     *
     * @return The Match containing the header fields of this packet and of its
     *         payload encapsulated packets
     */
    public Match getMatch() {
        Match match = new Match();
        Packet packet = this;
        while (packet != null) {
            packet.populateMatch(match);
            packet = packet.getPayload();
        }
        return match;
    }
}
