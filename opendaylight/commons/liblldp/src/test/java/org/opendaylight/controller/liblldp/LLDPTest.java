/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.liblldp;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;

import java.util.List;
import org.junit.internal.ArrayComparisonFailure;
import org.junit.Test;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Test of {@link LLDP} serialization feature (TODO: and deserialization)
 */
public class LLDPTest {

    private static final byte[] CHASSIS_ID_VALUE = "chassis".getBytes();
    private static final short CHASSIS_ID_LENGTH = (short) CHASSIS_ID_VALUE.length;

    private static final byte[] TTL_VALUE = new byte[] { (byte) 0, (byte) 100 };
    private static final short TTL_LENGTH = (short) TTL_VALUE.length;

    private static final byte[] PORT_VALUE = "dummy port id".getBytes();
    private static final short PORT_LENGTH = (short) PORT_VALUE.length;

    private static final byte[] SYSTEM_NAME_VALUE = "dummy system name".getBytes();
    private static final short SYSTEM_NAME_LENGTH = (short) SYSTEM_NAME_VALUE.length;

    private static final byte SYSTEM_CAPABILITIES_TLV = 8;
    private static final byte[] SYSTEM_CAPABILITIES_VALUE = "dummy system capabilities".getBytes();
    private static final short SYSTEM_CAPABILITIES_LENGTH = (short) SYSTEM_CAPABILITIES_VALUE.length;

    private static final byte[] OUI = new byte[] { (byte) 0X11, (byte) 0X22, (byte) 0X33 };

    private static final byte[] OUI_SUBTYPE_A = new byte[] { (byte) 0 };
    private static final byte[] CUSTOM_SUBTYPE_A_VALUE = "first custom value A".getBytes();
    private static final short CUSTOM_SUBTYPE_A_LENGTH = (short) (OUI.length + OUI_SUBTYPE_A.length + CUSTOM_SUBTYPE_A_VALUE.length);

    private static final byte[] OUI_SUBTYPE_B = new byte[] { (byte) 1 };
    private static final byte[] CUSTOM_SUBTYPE_B_VALUE = "second custom value B".getBytes();
    private static final short CUSTOM_SUBTYPE_B_LENGTH = (short) (OUI.length + OUI_SUBTYPE_B.length + CUSTOM_SUBTYPE_B_VALUE.length);

    /**
     * Tests whether serialization of LLDP packet is correct
     * @see LLDP#serialize()
     * @throws PacketException
     */
    @Test
    public void testSerialize() throws PacketException {
        LLDP lldpBuilder = new LLDP();

        lldpBuilder.setChassisId(dummyTlv(LLDPTLV.TLVType.ChassisID.getValue(), CHASSIS_ID_LENGTH, CHASSIS_ID_VALUE));
        lldpBuilder.setTtl(dummyTlv(LLDPTLV.TLVType.TTL.getValue(), TTL_LENGTH, TTL_VALUE));
        lldpBuilder.setPortId(dummyTlv(LLDPTLV.TLVType.PortID.getValue(), PORT_LENGTH, PORT_VALUE));
        lldpBuilder.setSystemNameId(dummyTlv(LLDPTLV.TLVType.SystemName.getValue(), SYSTEM_NAME_LENGTH,
                SYSTEM_NAME_VALUE));

        // adding optional TLVs for which doesn't exist special set* methods in LLDP
        final List<LLDPTLV> optionalTLVs = new ArrayList<>();
        // System Capabilities TLV (type = 7)
        optionalTLVs.add(dummyTlv(SYSTEM_CAPABILITIES_TLV, SYSTEM_CAPABILITIES_LENGTH, SYSTEM_CAPABILITIES_VALUE));
        lldpBuilder.setOptionalTLVList(optionalTLVs);

        // // adding custom TLVs
         final List<LLDPTLV> customTLVs = new ArrayList<>();
         customTLVs.add(dummyCustomTlv(LLDPTLV.TLVType.Custom.getValue(), OUI, OUI_SUBTYPE_A,
         CUSTOM_SUBTYPE_A_LENGTH, CUSTOM_SUBTYPE_A_VALUE));
         customTLVs.add(dummyCustomTlv(LLDPTLV.TLVType.Custom.getValue(), OUI, OUI_SUBTYPE_B,
         CUSTOM_SUBTYPE_B_LENGTH, CUSTOM_SUBTYPE_B_VALUE));
         lldpBuilder.setCustomTLVList(customTLVs);

        byte[] serialized = lldpBuilder.serialize();

        int offset = 0;
        offset = checkTLV(serialized, offset, (byte) 0b00000010, "ChassisID", CHASSIS_ID_LENGTH, CHASSIS_ID_VALUE);
        offset = checkTLV(serialized, offset, (byte) 0b00000110, "TTL", TTL_LENGTH, TTL_VALUE);
        offset = checkTLV(serialized, offset, (byte) 0b00000100, "PortID", PORT_LENGTH, PORT_VALUE);
        offset = checkTLV(serialized, offset, (byte) 0b00001010, "SystemName", SYSTEM_NAME_LENGTH, SYSTEM_NAME_VALUE);
        offset = checkTLV(serialized, offset, (byte) 0b00010000, "System capabilities", SYSTEM_CAPABILITIES_LENGTH,
                SYSTEM_CAPABILITIES_VALUE);
        offset = checkTLV(serialized, offset, (byte) 0b11111110, "Custom subtype A", CUSTOM_SUBTYPE_A_LENGTH,
                CUSTOM_SUBTYPE_A_VALUE, OUI[0], OUI[1], OUI[2], OUI_SUBTYPE_A[0]);
        offset = checkTLV(serialized, offset, (byte) 0b11111110, "Custom subtype B", CUSTOM_SUBTYPE_B_LENGTH,
                CUSTOM_SUBTYPE_B_VALUE, OUI[0], OUI[1], OUI[2], OUI_SUBTYPE_B[0]);

    }

    private static int checkTLV(byte[] serializedData, int offset, byte typeTLVBits, String typeTLVName, short lengthTLV,
            byte[] valueTLV, byte... bytesBeforeValue) throws ArrayComparisonFailure {
        byte[] concreteTlvAwaited = awaitedBytes(typeTLVBits, lengthTLV, valueTLV, bytesBeforeValue);
        int concreteTlvAwaitLength = concreteTlvAwaited.length;
        assertArrayEquals("Serialization problem " + typeTLVName, concreteTlvAwaited,
                ArrayUtils.subarray(serializedData, offset, offset + concreteTlvAwaitLength));
        return offset + concreteTlvAwaitLength;
    }

    private static byte[] awaitedBytes(byte typeTLV, short length, byte[] value, byte[] bytesBeforeValue) {
        byte[] awaited = ArrayUtils.EMPTY_BYTE_ARRAY;

        // 0 - the less meaning byte (right), 1 most meaning byte (left)
        byte lengthByte0 = (byte) length;
        byte lengthByte1 = (byte) (length >> 8);

        awaited = ArrayUtils.addAll(awaited, (byte) (typeTLV | lengthByte1), lengthByte0);
        awaited = ArrayUtils.addAll(awaited, bytesBeforeValue);
        awaited = ArrayUtils.addAll(awaited, value);
        return awaited;
    }

    private static LLDPTLV dummyCustomTlv(final byte tlvType, byte[] oui, byte[] ouiSubtype, short customLength,
            byte[] subtypeValue) {
        byte[] fullCustomValue = new byte[0];
        fullCustomValue = ArrayUtils.addAll(fullCustomValue, oui);
        fullCustomValue = ArrayUtils.addAll(fullCustomValue, ouiSubtype);
        fullCustomValue = ArrayUtils.addAll(fullCustomValue, subtypeValue);
        return dummyTlv(tlvType, customLength, fullCustomValue);
    }

    private static LLDPTLV dummyTlv(final byte concreteTlv, final short concreteLength, final byte[] concreteValue) {
        LLDPTLV tlv = new LLDPTLV();
        tlv.setType(concreteTlv).setLength(concreteLength).setValue(concreteValue);
        return tlv;
    }
}
