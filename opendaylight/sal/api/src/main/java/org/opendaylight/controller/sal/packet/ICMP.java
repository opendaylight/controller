
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
 * Class that represents the ICMP packet objects
 *
 *
 */

public class ICMP extends Packet {
    private static final String TYPECODE = "TypeCode";
    private static final String CODE = "Code";
    private static final String HEADERCHECKSUM = "HeaderChecksum";
    private static final String IDENTIFIER = "Identifier";
    private static final String SEQNUMBER = "SequenceNumber";

    private static Map<String, Pair<Integer, Integer>> fieldCoordinates = new LinkedHashMap<String, Pair<Integer, Integer>>() {
        private static final long serialVersionUID = 1L;

        {
            put(TYPECODE, new ImmutablePair<Integer, Integer>(0, 8));
            put(CODE, new ImmutablePair<Integer, Integer>(8, 8));
            put(HEADERCHECKSUM, new ImmutablePair<Integer, Integer>(16, 16));
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

    private Map<String, byte[]> fieldValues;

    @Override
    public void setHeaderField(String headerField, byte[] readValue) {
        hdrFieldsMap.put(headerField, readValue);
    }

    /**
     * Sets the TypeCode of ICMP  for the current ICMP object instance
     * @param short - typeCode
     * @return ICMP
     */
    public ICMP setTypeCode(short typeCode) {
        byte[] icmpTypeCode = BitBufferHelper.toByteArray(typeCode);
        fieldValues.put(TYPECODE, icmpTypeCode);
        return this;
    }

    /**
     * Sets the ICMP checksum  for the current ICMP object instance
     * @param short - checksum
     * @return ICMP
     */
    public ICMP setChecksum(short checksum) {
        byte[] icmpChecksum = BitBufferHelper.toByteArray(checksum);
        fieldValues.put(HEADERCHECKSUM, icmpChecksum);
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

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
