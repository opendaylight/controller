
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
import org.opendaylight.controller.sal.utils.EtherTypes;

/**
 * Class that represents the Ethernet frame objects
 *
 *
 */
public class Ethernet extends Packet {
    private static final String DMAC = "DestinationMACAddress";
    private static final String SMAC = "SourceMACAddress";
    private static final String ETHT = "EtherType";

    // TODO: This has to be outside and it should be possible for osgi
    // to add new coming packet classes
    public static Map<Short, Class<? extends Packet>> etherTypeClassMap;
    static {
        etherTypeClassMap = new HashMap<Short, Class<? extends Packet>>();
        etherTypeClassMap.put(EtherTypes.ARP.shortValue(), ARP.class);
        etherTypeClassMap.put(EtherTypes.IPv4.shortValue(), IPv4.class);
        etherTypeClassMap.put(EtherTypes.LLDP.shortValue(), LLDP.class);
    }
    private static Map<String, Pair<Integer, Integer>> fieldCoordinates = new LinkedHashMap<String, Pair<Integer, Integer>>() {
        private static final long serialVersionUID = 1L;
        {
            put(DMAC, new ImmutablePair<Integer, Integer>(0, 48));
            put(SMAC, new ImmutablePair<Integer, Integer>(48, 48));
            put(ETHT, new ImmutablePair<Integer, Integer>(96, 16));
        }
    };
    private Map<String, byte[]> fieldValues;

    /**
     * Default constructor that creates and sets the HashMap
     */
    public Ethernet() {
        super();
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    /**
     * Constructor that sets the access level for the packet and
     * creates and sets the HashMap
     */
    public Ethernet(boolean writeAccess) {
        super(writeAccess);
        fieldValues = new HashMap<String, byte[]>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    @Override
    public void setHeaderField(String headerField, byte[] readValue) {
        if (headerField.equals(ETHT)) {
            payloadClass = etherTypeClassMap.get(BitBufferHelper
                    .getShort(readValue));
        }
        hdrFieldsMap.put(headerField, readValue);
    }

    /**
     * Gets the destination MAC address stored
     * @return byte[] - the destinationMACAddress
     */
    public byte[] getDestinationMACAddress() {
        return fieldValues.get(DMAC);
    }

    /**
     * Gets the source MAC address stored
     * @return byte[] - the sourceMACAddress
     */
    public byte[] getSourceMACAddress() {
        return fieldValues.get(SMAC);
    }

    /**
     * Gets the etherType stored
     * @return short - the etherType
     */
    public short getEtherType() {
        return BitBufferHelper.getShort(fieldValues.get(ETHT));
    }

    /**
     * Sets the destination MAC address for the current Ethernet object instance
     * @param byte[] - the destinationMACAddress to set
     */
    public Ethernet setDestinationMACAddress(byte[] destinationMACAddress) {
        fieldValues.put(DMAC, destinationMACAddress);
        return this;
    }

    /**
     * Sets the source MAC address for the current Ethernet object instance
     * @param byte[] - the sourceMACAddress to set
     */
    public Ethernet setSourceMACAddress(byte[] sourceMACAddress) {
        fieldValues.put(SMAC, sourceMACAddress);
        return this;
    }

    /**
     * Sets the etherType for the current Ethernet object instance
     * @param short - the etherType to set
     */
    public Ethernet setEtherType(short etherType) {
        byte[] ethType = BitBufferHelper.toByteArray(etherType);
        fieldValues.put(ETHT, ethType);
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
