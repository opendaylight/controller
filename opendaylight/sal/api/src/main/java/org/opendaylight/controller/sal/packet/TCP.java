
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
 * Class that represents the TCP segment objects
 *
 *
 */
public class TCP extends Packet {

    private static final String SRCPORT = "SourcePort";
    private static final String DESTPORT = "DestinationPort";
    private static final String SEQNUMBER = "SequenceNumber";
    private static final String ACKNUMBER = "AcknoledgementNumber";
    private static final String DATAOFFSET = "DataOffset";
    private static final String RESERVED = "Reserved";
    private static final String HEADERLENFLAGS = "HeaderLenFlags";
    private static final String WINDOWSIZE = "WindowSize";
    private static final String CHECKSUM = "Checksum";
    private static final String URGENTPOINTER = "UrgentPointer";

    private static Map<String, Pair<Integer, Integer>> fieldCoordinates = new LinkedHashMap<String, Pair<Integer, Integer>>() {
        private static final long serialVersionUID = 1L;
        {
            put(SRCPORT, new ImmutablePair<Integer, Integer>(0, 16));
            put(DESTPORT, new ImmutablePair<Integer, Integer>(16, 16));
            put(SEQNUMBER, new ImmutablePair<Integer, Integer>(32, 32));
            put(ACKNUMBER, new ImmutablePair<Integer, Integer>(64, 32));
            put(DATAOFFSET, new ImmutablePair<Integer, Integer>(96, 4));
            put(RESERVED, new ImmutablePair<Integer, Integer>(100, 3));
            put(HEADERLENFLAGS, new ImmutablePair<Integer, Integer>(103, 9));
            put(WINDOWSIZE, new ImmutablePair<Integer, Integer>(112, 16));
            put(CHECKSUM, new ImmutablePair<Integer, Integer>(128, 16));
            put(URGENTPOINTER, new ImmutablePair<Integer, Integer>(144, 16));
        }
    };

    private Map<String, byte[]> fieldValues;

    /**
     * Default constructor that sets all the header fields to zero
     */
    public TCP() {
        super();
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
        /* Setting all remaining header field values to
         * default value of 0.  These maybe changed as needed
         */
        setSourcePort((short) 0);
        setDestinationPort((short) 0);
        setSequenceNumber(0);
        setAckNumber(0);
        setDataOffset((byte) 0);
        setReserved((byte) 0);
        setWindowSize((short) 0);
        setUrgentPointer((short) 0);
        setChecksum((short) 0);
    }

    /**
     * Constructor that sets the access level for the packet and
     * sets all the header fields to zero.
     */
    public TCP(boolean writeAccess) {
        super(writeAccess);
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
        /* Setting all remaining header field values to
         * default value of 0.  These maybe changed as needed
         */
        setSourcePort((short) 0);
        setDestinationPort((short) 0);
        setSequenceNumber(0);
        setAckNumber(0);
        setDataOffset((byte) 0);
        setReserved((byte) 0);
        setWindowSize((short) 0);
        setUrgentPointer((short) 0);
        setChecksum((short) 0);
    }

    @Override
    /**
     * Stores the value read from data stream
     */
    public void setHeaderField(String headerField, byte[] readValue) {
        hdrFieldsMap.put(headerField, readValue);
    }

    /**
     * Sets the TCP source port for the current TCP object instance
     * @param short tcpSourcePort
     * @return TCP
     */
    public TCP setSourcePort(short tcpSourcePort) {
        byte[] sourcePort = BitBufferHelper.toByteArray(tcpSourcePort);
        fieldValues.put(SRCPORT, sourcePort);
        return this;
    }

    /**
     * Sets the TCP destination port for the current TCP object instance
     * @param short tcpDestinationPort
     * @return TCP
     */
    public TCP setDestinationPort(short tcpDestinationPort) {
        byte[] destinationPort = BitBufferHelper
                .toByteArray(tcpDestinationPort);
        fieldValues.put(DESTPORT, destinationPort);
        return this;
    }

    /**
     * Sets the TCP sequence number for the current TCP object instance
     * @param int tcpSequenceNumber
     * @return TCP
     */
    public TCP setSequenceNumber(int tcpSequenceNumber) {
        byte[] sequenceNumber = BitBufferHelper.toByteArray(tcpSequenceNumber);
        fieldValues.put(SEQNUMBER, sequenceNumber);
        return this;
    }

    /**
     * Sets the TCP data offset for the current TCP object instance
     * @param byte tcpDataOffset
     * @return TCP
     */
    public TCP setDataOffset(byte tcpDataOffset) {
        byte[] offset = BitBufferHelper.toByteArray(tcpDataOffset);
        fieldValues.put("DataOffset", offset);
        return this;
    }

    /**
     * Sets the TCP reserved bits for the current TCP object instance
     * @param byte tcpReserved
     * @return TCP
     */
    public TCP setReserved(byte tcpReserved) {
        byte[] reserved = BitBufferHelper.toByteArray(tcpReserved);
        fieldValues.put("Reserved", reserved);
        return this;
    }

    /**
     * Sets the TCP Ack number for the current TCP object instance
     * @param int tcpAckNumber
     * @return TCP
     */
    public TCP setAckNumber(int tcpAckNumber) {
        byte[] ackNumber = BitBufferHelper.toByteArray(tcpAckNumber);
        fieldValues.put(ACKNUMBER, ackNumber);
        return this;
    }

    /**
     * Sets the TCP flags for the current TCP object instance
     * @param short tcpFlags
     * @return TCP
     */
    public TCP setHeaderLenFlags(short tcpFlags) {
        byte[] headerLenFlags = BitBufferHelper.toByteArray(tcpFlags);
        fieldValues.put(HEADERLENFLAGS, headerLenFlags);
        return this;
    }

    /**
     * Sets the TCP window size for the current TCP object instance
     * @param short tcpWsize
     * @return TCP
     */
    public TCP setWindowSize(short tcpWsize) {
        byte[] wsize = BitBufferHelper.toByteArray(tcpWsize);
        fieldValues.put(WINDOWSIZE, wsize);
        return this;
    }

    /**
     * Sets the TCP checksum for the current TCP object instance
     * @param short tcpChecksum
     * @return TCP
     */
    public TCP setChecksum(short tcpChecksum) {
        byte[] checksum = BitBufferHelper.toByteArray(tcpChecksum);
        fieldValues.put(CHECKSUM, checksum);
        return this;
    }

    /**
     * Sets the TCP Urgent Pointer for the current TCP object instance
     * @param short tcpUrgentPointer
     * @return TCP
     */
    public TCP setUrgentPointer(short tcpUrgentPointer) {
        byte[] urgentPointer = BitBufferHelper.toByteArray(tcpUrgentPointer);
        fieldValues.put(URGENTPOINTER, urgentPointer);
        return this;
    }

    /**
     * Gets the stored source port value of TCP header
     * @return the sourcePort
     */
    public short getSourcePort() {
        return (BitBufferHelper.getShort(fieldValues.get(SRCPORT)));
    }

    /**
     * Gets the stored destination port value of TCP header
     * @return the destinationPort
     */
    public short getDestinationPort() {
        return (BitBufferHelper.getShort(fieldValues.get(DESTPORT)));
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
