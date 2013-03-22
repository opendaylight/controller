
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
 * Class that represents the UDP datagram objects
 *
 *
 */

public class UDP extends Packet {

    private static final String SRCPORT = "SourcePort";
    private static final String DESTPORT = "DestinationPort";
    private static final String LENGTH = "Length";
    private static final String CHECKSUM = "Checksum";

    private static Map<String, Pair<Integer, Integer>> fieldCoordinates = new LinkedHashMap<String, Pair<Integer, Integer>>() {
        private static final long serialVersionUID = 1L;
        {
            put(SRCPORT, new ImmutablePair<Integer, Integer>(0, 16));
            put(DESTPORT, new ImmutablePair<Integer, Integer>(16, 16));
            put(LENGTH, new ImmutablePair<Integer, Integer>(32, 16));
            put(CHECKSUM, new ImmutablePair<Integer, Integer>(48, 16));
        }
    };

    public UDP() {
        super();
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
        /* Setting all remaining header field values to
         * default value of 0.  These maybe changed as needed
         */
        setSourcePort((short) 0);
        setDestinationPort((short) 0);
        setChecksum((short) 0);
    }

    public UDP(boolean writeAccess) {
        super(writeAccess);
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
        /* Setting all remaining header field values to
         * default value of 0.  These maybe changed as needed
         */
        setSourcePort((short) 0);
        setDestinationPort((short) 0);
        setChecksum((short) 0);
    }

    private Map<String, byte[]> fieldValues;

    /* public static Map<Short, Class<? extends Packet>> decodeMap;

      static {
          decodeMap = new HashMap<Short, Class<? extends Packet>>();
          UDP.decodeMap.put((short)67, DHCP.class);
          UDP.decodeMap.put((short)68, DHCP.class);
      }*/
    /**
     * Get the stored source port
     * @return short - the sourcePort
     */
    public short getSourcePort() {
        return (BitBufferHelper.getShort(fieldValues.get(SRCPORT)));
    }

    /**
     * Get the stored destination port
     * @return short - the destinationPort
     */
    public short getDestinationPort() {
        return (BitBufferHelper.getShort(fieldValues.get(DESTPORT)));
    }

    /**
     * Gets the stored length of UDP header
     * @return short - the length
     */
    public short getLength() {
        return (BitBufferHelper.getShort(fieldValues.get(LENGTH)));
    }

    /**
     * Get the stored checksum value of the UDP header
     * @return short - the checksum
     */
    public short getChecksum() {
        return (BitBufferHelper.getShort(fieldValues.get(CHECKSUM)));
    }

    @Override
    /**
     * Store the value read from data stream in hdrFieldMap
     */
    public void setHeaderField(String headerField, byte[] readValue) {
        /*if (headerField.equals("Protocol")) {
        	payloadClass = decodeMap.get(readValue);
        }*/
        hdrFieldsMap.put(headerField, readValue);
    }

    /**
     * Sets the sourcePort value for the current UDP object instance
     * @param short - the sourcePort to set
     * @return UDP
     */
    public UDP setSourcePort(short udpSourcePort) {
        byte[] sourcePort = BitBufferHelper.toByteArray(udpSourcePort);
        fieldValues.put(SRCPORT, sourcePort);
        return this;
    }

    /**
     * Sets the destinationPort value for the current UDP object instance
     * @param short - the destinationPort to set
     * @return UDP
     */
    public UDP setDestinationPort(short udpDestinationPort) {
        byte[] destinationPort = BitBufferHelper
                .toByteArray(udpDestinationPort);
        fieldValues.put(DESTPORT, destinationPort);
        return this;
    }

    /**
     * Set the UDP header length value for the current UDP object instance
     * @param short - the length to set
     * @return UDP
     */
    public UDP setLength(short udpLength) {
        byte[] length = BitBufferHelper.toByteArray(udpLength);
        fieldValues.put(LENGTH, length);
        return this;
    }

    /**
     * Set the checksum for the current UDP object instance
     * @param short - the checksum to set
     * @return UDP
     */
    public UDP setChecksum(short udpChecksum) {
        byte[] checksum = BitBufferHelper.toByteArray(udpChecksum);
        fieldValues.put(CHECKSUM, checksum);
        return this;
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
