/*
 * Copyright (c) 2013-2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;

/**
 * Class that represents the IEEE 802.1Q objects
 */
@Deprecated
public class IEEE8021Q extends Packet {
    private static final String PCP = "PriorityCodePoint";
    private static final String CFI = "CanonicalFormatIndicator";
    private static final String VID = "VlanIdentifier";
    private static final String ETHT = "EtherType";

    private static Map<String, Pair<Integer, Integer>> fieldCoordinates = new LinkedHashMap<String, Pair<Integer, Integer>>() {
        private static final long serialVersionUID = 1L;
        {
            put(PCP, new ImmutablePair<Integer, Integer>(0, 3));
            put(CFI, new ImmutablePair<Integer, Integer>(3, 1));
            put(VID, new ImmutablePair<Integer, Integer>(4, 12));
            put(ETHT, new ImmutablePair<Integer, Integer>(16, 16));
        }
    };
    private final Map<String, byte[]> fieldValues;

    /**
     * Default constructor that creates and sets the HashMap
     */
    public IEEE8021Q() {
        super();
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    /**
     * Constructor that sets the access level for the packet and creates and
     * sets the HashMap
     */
    public IEEE8021Q(boolean writeAccess) {
        super(writeAccess);
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    @Override
    /**
     * Store the value read from data stream in hdrFieldMap
     */
    public void setHeaderField(String headerField, byte[] readValue) {
        if (headerField.equals(ETHT)) {
            payloadClass = Ethernet.etherTypeClassMap.get(BitBufferHelper.getShort(readValue));
        }
        hdrFieldsMap.put(headerField, readValue);
    }

    /**
     * Gets the priority code point(PCP) stored
     *
     * @return byte - the PCP
     */
    public byte getPcp() {
        return BitBufferHelper.getByte(fieldValues.get(PCP));
    }

    /**
     * Gets the canonical format indicator(CFI) stored
     *
     * @return byte - the CFI
     */
    public byte getCfi() {
        return BitBufferHelper.getByte(fieldValues.get(CFI));
    }

    /**
     * Gets the VLAN identifier(VID) stored
     *
     * @return short - the VID
     */
    public short getVid() {
        return BitBufferHelper.getShort(fieldValues.get(VID));
    }

    /**
     * Gets the etherType stored
     *
     * @return short - the etherType
     */
    public short getEtherType() {
        return BitBufferHelper.getShort(fieldValues.get(ETHT));
    }

    /**
     * Sets the priority code point(PCP) for the current IEEE 802.1Q object
     * instance
     *
     * @param byte - the PCP to set
     */
    public IEEE8021Q setPcp(byte pcp) {
        byte[] priorityCodePoint = BitBufferHelper.toByteArray(pcp);
        fieldValues.put(PCP, priorityCodePoint);
        return this;
    }

    /**
     * Sets the canonical format indicator(CFI) for the current IEEE 802.1Q
     * object instance
     *
     * @param byte - the CFI to set
     */
    public IEEE8021Q setCfi(byte cfi) {
        byte[] canonicalFormatIndicator = BitBufferHelper.toByteArray(cfi);
        fieldValues.put(CFI, canonicalFormatIndicator);
        return this;
    }

    /**
     * Sets the VLAN identifier(VID) for the current IEEE 802.1Q instance
     *
     * @param short - the VID to set
     */
    public IEEE8021Q setVid(short vid) {
        byte[] vlanIdentifier = BitBufferHelper.toByteArray(vid);
        fieldValues.put(VID, vlanIdentifier);
        return this;
    }

    /**
     * Sets the etherType for the current IEEE 802.1Q object instance
     *
     * @param short - the etherType to set
     */
    public IEEE8021Q setEtherType(short etherType) {
        byte[] ethType = BitBufferHelper.toByteArray(etherType);
        fieldValues.put(ETHT, ethType);
        return this;
    }

    @Override
    public void populateMatch(Match match) {
        match.setField(MatchType.DL_VLAN, this.getVid());
        match.setField(MatchType.DL_VLAN_PR, this.getPcp());
        match.setField(MatchType.DL_TYPE, this.getEtherType());
    }

    /**
     * Gets the header size in bits
     * @return The .1Q header size in bits
     */
    @Override
    public int getHeaderSize() {
        return 32;
    }

}
