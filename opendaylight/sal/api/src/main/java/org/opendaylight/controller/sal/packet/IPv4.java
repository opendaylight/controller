
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 *
 */
package org.opendaylight.controller.sal.packet;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NetUtils;

/**
 * Class that represents the IPv4  packet objects
 *
 *
 */

public class IPv4 extends Packet {
    private static final String VERSION = "Version";
    private static final String HEADERLENGTH = "HeaderLength";
    private static final String DIFFSERV = "DiffServ";
    private static final String ECN = "ECN";
    private static final String TOTLENGTH = "TotalLength";
    private static final String IDENTIFICATION = "Identification";
    private static final String FLAGS = "Flags";
    private static final String FRAGOFFSET = "FragmentOffset";
    private static final String TTL = "TTL";
    private static final String PROTOCOL = "Protocol";
    private static final String CHECKSUM = "Checksum";
    private static final String SIP = "SourceIPAddress";
    private static final String DIP = "DestinationIPAddress";
    private static final String OPTIONS = "Options";

    public static Map<Byte, Class<? extends Packet>> protocolClassMap;
    static {
        protocolClassMap = new HashMap<Byte, Class<? extends Packet>>();
        protocolClassMap.put(IPProtocols.ICMP.byteValue(), ICMP.class);
        protocolClassMap.put(IPProtocols.UDP.byteValue(), UDP.class);
        protocolClassMap.put(IPProtocols.TCP.byteValue(), TCP.class);
    }
    private static Map<String, Pair<Integer, Integer>> fieldCoordinates = new LinkedHashMap<String, Pair<Integer, Integer>>() {
        private static final long serialVersionUID = 1L;
        {
            put(VERSION, new ImmutablePair<Integer, Integer>(0, 4));
            put(HEADERLENGTH, new ImmutablePair<Integer, Integer>(4, 4));
            put(DIFFSERV, new ImmutablePair<Integer, Integer>(8, 6));
            put(ECN, new ImmutablePair<Integer, Integer>(14, 2));
            put(TOTLENGTH, new ImmutablePair<Integer, Integer>(16, 16));
            put(IDENTIFICATION, new ImmutablePair<Integer, Integer>(32, 16));
            put(FLAGS, new ImmutablePair<Integer, Integer>(48, 3));
            put(FRAGOFFSET, new ImmutablePair<Integer, Integer>(51, 13));
            put(TTL, new ImmutablePair<Integer, Integer>(64, 8));
            put(PROTOCOL, new ImmutablePair<Integer, Integer>(72, 8));
            put(CHECKSUM, new ImmutablePair<Integer, Integer>(80, 16));
            put(SIP, new ImmutablePair<Integer, Integer>(96, 32));
            put(DIP, new ImmutablePair<Integer, Integer>(128, 32));
            put(OPTIONS, new ImmutablePair<Integer, Integer>(160, 0));
        }
    };

    private Map<String, byte[]> fieldValues;

    /**
     * Default constructor that sets the version to 4, headerLength to 5,
     * and flags to 2.  The default value for the identification is set to a
     * random number and the remaining fields are set to 0.
     */
    public IPv4() {
        super();
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;

        setVersion((byte) 4);
        setHeaderLength((byte) 5);
        setDiffServ((byte) 0);
        setIdentification(generateId());
        setFlags((byte) 2);
        setFragmentOffset((short) 0);
        setECN((byte) 0);
    }

    /**
     * The write access to the packet is set in this constructor.
     * Constructor that sets the version to 4, headerLength to 5,
     * and flags to 2.  The default value for the identification is set to a
     * random number and the remaining fields are set to 0.
     * @param boolean
     */
    public IPv4(boolean writeAccess) {
        super(writeAccess);
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;

        setVersion((byte) 4);
        setHeaderLength((byte) 5);
        setDiffServ((byte) 0);
        setIdentification(generateId());
        setFlags((byte) 2);
        setFragmentOffset((short) 0);
        setECN((byte) 0);
    }

    /**
     * Gets the IP version stored
     * @return the version
     */
    public byte getVersion() {
        return (BitBufferHelper.getByte(fieldValues.get(VERSION)));
    }

    /**
     * Gets the IP header length stored
     * @return the headerLength in bytes
     */
    public int getHeaderLen() {
        return (4 * BitBufferHelper.getByte(fieldValues.get(HEADERLENGTH)));
    }

    /**
     * Gets the header length in bits, from the header length stored and options if any
     * @return HeaderLength to serialize code
     */
    @Override
    public int getHeaderSize() {
        int headerLen = this.getHeaderLen();
        if (headerLen == 0)
            headerLen = 20;

        byte[] options = hdrFieldsMap.get(OPTIONS);
        if (options != null)
            headerLen += options.length;

        return headerLen * NetUtils.NumBitsInAByte;

    }

    /**
     * Gets the differential services value stored
     * @return the diffServ
     */
    public byte getDiffServ() {
        return BitBufferHelper.getByte(fieldValues.get(DIFFSERV));
    }

    /**
     * Gets the ecn bits stored
     * @return the ecn bits
     */
    public byte getECN() {
        return BitBufferHelper.getByte(fieldValues.get(ECN));
    }

    /**
     * Gets the total length of the IP header in bytes
     * @return the totalLength
     */
    public short getTotalLength() {
        return (BitBufferHelper.getShort(fieldValues.get(TOTLENGTH)));
    }

    /**
     * Gets the identification value stored
     * @return the identification
     */
    public short getIdentification() {
        return (BitBufferHelper.getShort(fieldValues.get(IDENTIFICATION)));
    }

    /**
     * Gets the flag values stored
     * @return the flags
     */
    public byte getFlags() {
        return (BitBufferHelper.getByte(fieldValues.get(FLAGS)));
    }

    /**
     * Gets the TTL value stored
     * @return the ttl
     */
    public byte getTtl() {
        return (BitBufferHelper.getByte(fieldValues.get(TTL)));
    }

    /**
     * Gets the protocol value stored
     * @return the protocol
     */
    public byte getProtocol() {
        return (BitBufferHelper.getByte(fieldValues.get(PROTOCOL)));
    }

    /**
     * Gets the checksum value stored
     * @return the checksum
     */
    public short getChecksum() {
        return (BitBufferHelper.getShort(fieldValues.get(CHECKSUM)));
    }

    /**
     * Gets the fragment offset stored
     * @return the fragmentOffset
     */
    public short getFragmentOffset() {
        return (BitBufferHelper.getShort(fieldValues.get(FRAGOFFSET)));
    }

    /**
     * Gets the source IP address stored
     * @return the sourceAddress
     */
    public int getSourceAddress() {
        return (BitBufferHelper.getInt(fieldValues.get(SIP)));
    }

    /**
     * Gets the destination IP address stored
     * @return the destinationAddress
     */
    public int getDestinationAddress() {
        return (BitBufferHelper.getInt(fieldValues.get(DIP)));
    }

    /**
     * gets the Options stored
     * @return the options
     */
    public byte[] getOptions() {
        return (fieldValues.get(OPTIONS));
    }

    @Override
    /**
     * Stores the value of fields read from data stream
     * Variable header value like payload protocol, is stored here
     */
    public void setHeaderField(String headerField, byte[] readValue) {
        if (headerField.equals(PROTOCOL)) {
            payloadClass = protocolClassMap.get(readValue[0]);
        }
        hdrFieldsMap.put(headerField, readValue);
    }

    /**
     * Stores the IP version from the header
     * @param version the version to set
     * @return @IPv4
     */
    public IPv4 setVersion(byte ipVersion) {
        byte[] version = BitBufferHelper.toByteArray(ipVersion);
        fieldValues.put(VERSION, version);
        return this;
    }

    /**
     * Stores the length of IP header in words (2 bytes)
     * @param headerLength the headerLength to set
     * @return IPv4
     */
    public IPv4 setHeaderLength(byte ipheaderLength) {
        byte[] headerLength = BitBufferHelper.toByteArray(ipheaderLength);
        fieldValues.put(HEADERLENGTH, headerLength);
        return this;
    }

    /**
     * Stores the differential services value from the IP header
     * @param diffServ the diffServ to set
     * @return IPv4
     */
    public IPv4 setDiffServ(byte ipdiffServ) {
        byte[] diffServ = BitBufferHelper.toByteArray(ipdiffServ);
        fieldValues.put(DIFFSERV, diffServ);
        return this;
    }

    /**
     * Stores the ECN bits from the header
     * @param ECN bits to set
     * @return IPv4
     */
    public IPv4 setECN(byte ecn) {
        byte[] ecnbytes = BitBufferHelper.toByteArray(ecn);
        fieldValues.put(ECN, ecnbytes);
        return this;
    }

    /**
     * Stores the total length of IP header in bytes
     * @param totalLength the totalLength to set
     * @return IPv4
     */
    public IPv4 setTotalLength(short iptotalLength) {
        byte[] totalLength = BitBufferHelper.toByteArray(iptotalLength);
        fieldValues.put(TOTLENGTH, totalLength);
        return this;
    }

    /**
     * Stores the identification number from the header
     * @param identification the identification to set
     * @return IPv4
     */
    public IPv4 setIdentification(short ipIdentification) {
        byte[] identification = BitBufferHelper.toByteArray(ipIdentification);
        fieldValues.put(IDENTIFICATION, identification);
        return this;
    }

    /**
     * Stores the IP flags value
     * @param flags the flags to set
     * @return IPv4
     */
    public IPv4 setFlags(byte ipFlags) {
        byte[] flags = { ipFlags };
        fieldValues.put(FLAGS, flags);
        return this;
    }

    /**
     * Stores the IP fragmentation offset value
     * @param fragmentOffset the fragmentOffset to set
     * @return IPv4
     */
    public IPv4 setFragmentOffset(short ipFragmentOffset) {
        byte[] fragmentOffset = BitBufferHelper.toByteArray(ipFragmentOffset);
        fieldValues.put(FRAGOFFSET, fragmentOffset);
        return this;
    }

    /**
     * Stores the TTL value
     * @param ttl the ttl to set
     * @return IPv4
     */
    public IPv4 setTtl(byte ipTtl) {
        byte[] ttl = BitBufferHelper.toByteArray(ipTtl);
        fieldValues.put(TTL, ttl);
        return this;
    }

    /**
     * Stores the protocol value of the IP payload
     * @param protocol the protocol to set
     * @return IPv4
     */
    public IPv4 setProtocol(byte ipProtocol) {
        byte[] protocol = BitBufferHelper.toByteArray(ipProtocol);
        fieldValues.put(PROTOCOL, protocol);
        return this;
    }

    /**
     * @param checksum the checksum to set
     */
    /*public IPv4 setChecksum() {
    	short ipChecksum = computeChecksum();
        byte[] checksum = BitBufferHelper.toByteArray(ipChecksum);
    	fieldValues.put(CHECKSUM, checksum);
        return this;
    }*/

    /**
     * Stores the IP source address from the header
     * @param sourceAddress the sourceAddress to set
     * @return IPv4
     */
    public IPv4 setSourceAddress(InetAddress ipSourceAddress) {
        byte[] sourceAddress = ipSourceAddress.getAddress();
        fieldValues.put(SIP, sourceAddress);
        return this;
    }

    /**
     * Stores the IP destination address from the header
     * @param the destination Address to set
     * @return IPv4
     */
    public IPv4 setDestinationAddress(InetAddress ipDestinationAddress) {
        byte[] sourceAddress = ipDestinationAddress.getAddress();
        fieldValues.put(DIP, sourceAddress);
        return this;
    }

    /**
     * Stores the IP destination address from the header
     * @param destinationAddress the destinationAddress to set
     * @return IPv4
     */
    public IPv4 setDestinationAddress(int ipDestinationAddress) {
        byte[] destinationAddress = BitBufferHelper
                .toByteArray(ipDestinationAddress);
        fieldValues.put(DIP, destinationAddress);
        return this;
    }

    /**
     * Generate a random number to set the Identification field
     * in IPv4 Header
     * @return short
     */
    private short generateId() {
        Random randomgen = new Random();
        return (short) (randomgen.nextInt(Short.MAX_VALUE + 1));
    }

    /**
     * Store the options from IP header
     * @param options - byte[]
     * @return IPv4
     */
    public IPv4 setOptions(byte[] options) {
        fieldValues.put(OPTIONS, options);
        byte newIHL = (byte) (5 + options.length);
        setHeaderLength(newIHL);

        return this;
    }

    /**
     * Computes the header checksum
     * @param byte[] hdrBytes - serialized bytes
     * @param int endBitOffset - end bit Offset
     * @return short - the computed checksum
     */
    private short computeChecksum(byte[] hdrBytes, int endByteOffset) {
        int startByteOffset = endByteOffset - getHeaderLen();
        short checkSum = (short) 0;
        int sum = 0, carry = 0, finalSum = 0;
        int parsedHex = 0;
        int checksumStartByte = startByteOffset + getfieldOffset(CHECKSUM)
                / NetUtils.NumBitsInAByte;

        for (int i = startByteOffset; i <= (endByteOffset - 1); i = i + 2) {
            //Skip, if the current bytes are checkSum bytes
            if (i == checksumStartByte)
                continue;
            StringBuffer sbuffer = new StringBuffer();
            sbuffer.append(String.format("%02X", hdrBytes[i]));
            if (i < (hdrBytes.length - 1))
                sbuffer.append(String.format("%02X", hdrBytes[i + 1]));

            parsedHex = Integer.valueOf(sbuffer.toString(), 16);
            sum += parsedHex;
        }
        carry = (sum >> 16) & 0xFF;
        finalSum = (sum & 0xFFFF) + carry;
        checkSum = (short) ~((short) finalSum & 0xFFFF);
        return checkSum;
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
    /**
     * Gets the number of bits for the fieldname specified
     * If the fieldname has variable length like "Options", then this value is computed using the
     * options length and the header length
     * @param fieldname - String
     * @return number of bits for fieldname - int
     */
    public int getfieldnumBits(String fieldName) {
        if (fieldName.equals(OPTIONS)) {
            byte[] options = getOptions();
            return ((options == null) ? 0 : (options.length - getHeaderLen()));
        }
        return (((Pair<Integer, Integer>) hdrFieldCoordMap.get(fieldName))
                .getRight());
    }

    @Override
    /**
     * Method to perform post serialization - like computation of checksum of serialized header
     * @param serializedBytes
     * @return void
     * @Exception throws exception
     */
    protected void postSerializeCustomOperation(byte[] serializedBytes)
            throws Exception {
        int startOffset = this.getfieldOffset(CHECKSUM);
        int numBits = this.getfieldnumBits(CHECKSUM);
        byte[] checkSum = BitBufferHelper.toByteArray(computeChecksum(
                serializedBytes, serializedBytes.length));
        BitBufferHelper.setBytes(serializedBytes, checkSum, startOffset,
                numBits);
        return;
    }

    @Override
    /**
     * Stores the payload of IP, serializes it and stores the length of serialized payload
     * bytes in Total Length
     * @param payload - Packet
     */
    public void setPayload(Packet payload) {
        this.payload = payload;
        /*
         * Deriving the Total Lenght here
         * TODO: See if we can derive the total length during
         * another phase (during serialization/deserialization)
         * */
        int payloadLength = 0;
        try {
            payloadLength = payload.serialize().length;
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.setTotalLength((short) (this.getHeaderLen() + payloadLength));
    }

    @Override
    /**
     * Method to perform post deserialization - like compare computed checksum with
     * the one obtained from IP header
     */
    protected void postDeserializeCustomOperation(byte[] data, int endBitOffset) {
        int endByteOffset = endBitOffset / NetUtils.NumBitsInAByte;
        int computedChecksum = computeChecksum(data, endByteOffset);
        int actualChecksum = BitBufferHelper.getInt(fieldValues.get(CHECKSUM));
        if (computedChecksum != actualChecksum)
            corrupted = true;
    }
}
