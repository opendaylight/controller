/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.liblldp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;

/**
 * Class that represents the LLDP frame objects
 */

public class LLDP extends Packet {
    private static final String CHASSISID = "ChassisId";
    private static final String SYSTEMNAMEID = "SystemNameID";
    private static final String PORTID = "PortId";
    private static final String TTL = "TTL";
    private static final int LLDPDefaultTlvs = 3;
    private static LLDPTLV emptyTLV = new LLDPTLV().setLength((short) 0).setType((byte) 0);
    public static final byte[] LLDPMulticastMac = { 1, (byte) 0x80, (byte) 0xc2, 0, 0, (byte) 0xe };

    private Map<Byte, LLDPTLV> mandatoryTLVs;
    private Map<Byte, LLDPTLV> optionalTLVs;
    private Map<CustomTLVKey, LLDPTLV> customTLVs;

    /**
     * Default constructor that creates the tlvList LinkedHashMap
     */
    public LLDP() {
        super();
        init();
    }

    /**
     * Constructor that creates the tlvList LinkedHashMap and sets the write access for the same
     */
    public LLDP(boolean writeAccess) {
        super(writeAccess);
        init();
    }

    private void init() {
        mandatoryTLVs = new LinkedHashMap<>(LLDPDefaultTlvs);
        optionalTLVs = new LinkedHashMap<>();
        customTLVs = new LinkedHashMap<>();
    }

    /**
     * @param String
     *            - description of the type of TLV
     * @return byte - type of TLV
     */
    private byte getType(String typeDesc) {
        if (typeDesc.equals(CHASSISID)) {
            return LLDPTLV.TLVType.ChassisID.getValue();
        } else if (typeDesc.equals(PORTID)) {
            return LLDPTLV.TLVType.PortID.getValue();
        } else if (typeDesc.equals(TTL)) {
            return LLDPTLV.TLVType.TTL.getValue();
        } else if (typeDesc.equals(SYSTEMNAMEID)) {
            return LLDPTLV.TLVType.SystemName.getValue();
        } else {
            return LLDPTLV.TLVType.Unknown.getValue();
        }
    }

    private LLDPTLV getFromTLVs(Byte type) {
        LLDPTLV tlv = null;
        tlv = mandatoryTLVs.get(type);
        if (tlv == null) {
            tlv = optionalTLVs.get(type);
        }
        return tlv;
    }

    private void putToTLVs(final Byte type, final LLDPTLV tlv) {
        if (type == LLDPTLV.TLVType.ChassisID.getValue() || type == LLDPTLV.TLVType.PortID.getValue()
                || type == LLDPTLV.TLVType.TTL.getValue()) {
            mandatoryTLVs.put(type, tlv);
        } else if (type != LLDPTLV.TLVType.Custom.getValue()) {
            optionalTLVs.put(type, tlv);
        }
    }

    /**
     * @param String
     *            - description of the type of TLV
     * @return LLDPTLV - full TLV
     */
    public LLDPTLV getTLV(String type) {
        return getFromTLVs(getType(type));
    }

    public LLDPTLV getCustomTLV(CustomTLVKey key) {
        return customTLVs.get(key);
    }

    /**
     * @param String
     *            - description of the type of TLV
     * @param LLDPTLV
     *            - tlv to set
     * @return void
     */
    public void setTLV(String type, LLDPTLV tlv) {
        putToTLVs(getType(type), tlv);
    }

    /**
     * @return the chassisId TLV
     */
    public LLDPTLV getChassisId() {
        return getTLV(CHASSISID);
    }

    /**
     * @param LLDPTLV
     *            - the chassisId to set
     */
    public LLDP setChassisId(LLDPTLV chassisId) {
        setTLV(CHASSISID, chassisId);
        return this;
    }

    /**
     * @return the SystemName TLV
     */
    public LLDPTLV getSystemNameId() {
        return getTLV(SYSTEMNAMEID);
    }

    /**
     * @param LLDPTLV
     *            - the chassisId to set
     */
    public LLDP setSystemNameId(LLDPTLV systemNameId) {
        setTLV(SYSTEMNAMEID, systemNameId);
        return this;
    }

    /**
     * @return LLDPTLV - the portId TLV
     */
    public LLDPTLV getPortId() {
        return getTLV(PORTID);
    }

    /**
     * @param LLDPTLV
     *            - the portId to set
     * @return LLDP
     */
    public LLDP setPortId(LLDPTLV portId) {
        setTLV(PORTID, portId);
        return this;
    }

    /**
     * @return LLDPTLV - the ttl TLV
     */
    public LLDPTLV getTtl() {
        return getTLV(TTL);
    }

    /**
     * @param LLDPTLV
     *            - the ttl to set
     * @return LLDP
     */
    public LLDP setTtl(LLDPTLV ttl) {
        setTLV(TTL, ttl);
        return this;
    }

    /**
     * @return the optionalTLVList
     */
    public Iterable<LLDPTLV> getOptionalTLVList() {
        return optionalTLVs.values();
    }

    /**
     * @return the customTlvList
     */
    public Iterable<LLDPTLV> getCustomTlvList() {
        return customTLVs.values();
    }

    /**
     * @param optionalTLVList
     *            the optionalTLVList to set
     * @return LLDP
     */
    public LLDP setOptionalTLVList(List<LLDPTLV> optionalTLVList) {
        for (LLDPTLV tlv : optionalTLVList) {
            optionalTLVs.put(tlv.getType(), tlv);
        }
        return this;
    }

    /**
     * @param customTLVList
     *            the list of custom TLVs to set
     * @return this LLDP
     */
    public LLDP addCustomTLV(final LLDPTLV customTLV) {
        CustomTLVKey key = new CustomTLVKey(LLDPTLV.extractCustomOUI(customTLV),
                LLDPTLV.extractCustomSubtype(customTLV));
        customTLVs.put(key, customTLV);

        return this;
    }

    @Override
    public Packet deserialize(byte[] data, int bitOffset, int size) throws PacketException {
        int lldpOffset = bitOffset; // LLDP start
        int lldpSize = size; // LLDP size

        if (logger.isTraceEnabled()) {
            logger.trace("LLDP: {} (offset {} bitsize {})", new Object[] { HexEncode.bytesToHexString(data),
                    lldpOffset, lldpSize });
        }
        /*
         * Deserialize the TLVs until we reach the end of the packet
         */
        while (lldpSize > 0) {
            LLDPTLV tlv = new LLDPTLV();
            tlv.deserialize(data, lldpOffset, lldpSize);
            if (tlv.getType() == 0 && tlv.getLength() == 0) {
                break;
            }
            int tlvSize = tlv.getTLVSize(); // Size of current TLV in bits
            lldpOffset += tlvSize;
            lldpSize -= tlvSize;
            if (tlv.getType() == LLDPTLV.TLVType.Custom.getValue()) {
                addCustomTLV(tlv);
            } else {
                this.putToTLVs(tlv.getType(), tlv);
            }
        }
        return this;
    }

    @Override
    public byte[] serialize() throws PacketException {
        int startOffset = 0;
        byte[] serializedBytes = new byte[getLLDPPacketLength()];

        final Iterable<LLDPTLV> allTlvs = Iterables.concat(mandatoryTLVs.values(), optionalTLVs.values(), customTLVs.values());
        for (LLDPTLV tlv : allTlvs) {
            int numBits = tlv.getTLVSize();
            try {
                BitBufferHelper.setBytes(serializedBytes, tlv.serialize(), startOffset, numBits);
            } catch (BufferException e) {
                throw new PacketException(e.getMessage());
            }
            startOffset += numBits;
        }
        // Now add the empty LLDPTLV at the end
        try {
            BitBufferHelper.setBytes(serializedBytes, LLDP.emptyTLV.serialize(), startOffset,
                    LLDP.emptyTLV.getTLVSize());
        } catch (BufferException e) {
            throw new PacketException(e.getMessage());
        }

        if (logger.isTraceEnabled()) {
            logger.trace("LLDP: serialized: {}", HexEncode.bytesToHexString(serializedBytes));
        }
        return serializedBytes;
    }

    /**
     * Returns the size of LLDP packet in bytes
     *
     * @return int - LLDP Packet size in bytes
     */
    private int getLLDPPacketLength() {
        int len = 0;

        for (LLDPTLV lldptlv : Iterables.concat(mandatoryTLVs.values(), optionalTLVs.values(), customTLVs.values())) {
            len += lldptlv.getTLVSize();
        }

        len += LLDP.emptyTLV.getTLVSize();

        return len / NetUtils.NumBitsInAByte;
    }
}
