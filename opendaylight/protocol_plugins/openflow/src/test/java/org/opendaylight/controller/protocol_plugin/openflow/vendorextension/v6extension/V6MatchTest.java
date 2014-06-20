/*
 * Copyright (c) 2014 NEC Corporation
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.openflow.protocol.OFMatch.OFPFW_ALL;
import static org.openflow.protocol.OFMatch.OFPFW_DL_VLAN;
import static org.openflow.protocol.OFMatch.OFPFW_DL_VLAN_PCP;
import static org.openflow.protocol.OFMatch.OFPFW_IN_PORT;

import java.nio.ByteBuffer;
import org.junit.Test;

/**
 * JUnit test for {@link V6Match}.
 */
public class V6MatchTest {
    /**
     * Header of a match entry for input port field without a mask.
     * The vendor-specific value is 0, and the length of value is 2.
     */
    private static int  HEADER_INPUT_PORT = (0 << 9) | 2;

    /**
     * Header of a match entry for VLAN TCI field without a mask.
     * The vendor-specific value is 4, and the length of value is 2.
     */
    private static int  HEADER_VLAN_TCI= (4 << 9) | 2;

    /**
     * Header of a match entry for VLAN TCI field with a mask.
     * The vendor-specific value is 4, and the length of value is 4.
     */
    private static int  HEADER_VLAN_TCI_W = (4 << 9) | (1 << 8) | 4;

    /**
     * Length of a match entry for input port field.
     * Header (4 bytes) + value (2 bytes) = 6 bytes.
     */
    private static short  MATCH_LEN_INPUT_PORT = 6;

    /**
     * Length of a match entry for VLAN TCI field without a mask.
     * Header (4 bytes) + value (2 bytes) = 6 bytes.
     */
    private static short  MATCH_LEN_VLAN_TCI = 6;

    /**
     * Length of a match entry for VLAN TCI field with a mask.
     * Header (4 bytes) + value (2 bytes) + bitmask (2 bytes) = 8 bytes.
     */
    private static short  MATCH_LEN_VLAN_TCI_WITH_MASK = 8;

    /**
     * Value of OFP_VLAN_NONE defined by OpenFlow 1.0.
     */
    private static final short  OFP_VLAN_NONE = (short)0xffff;

    /**
     * CFI bit in VLAN TCI field.
     */
    private static final int  VLAN_TCI_CFI = 1 << 12;

    /**
     * Test case for {@link V6Match#fromString(String)} about VLAN TCI field.
     * This test passes values to "dl_vlan" and "dl_vpcp".
     */
    @Test
    public void testFromStringVlanTci() {
        // Test for "dl_vlan" using non OFP_VLAN_NONE values.
        short vlans[] = {1, 10, 1000, 4095};
        short mask = 0;
        for (short vlan: vlans) {
            V6Match match = new V6Match();
            match.fromString("dl_vlan=" + vlan);
            assertEquals(MATCH_LEN_VLAN_TCI_WITH_MASK, match.getIPv6MatchLen());
            assertEquals(vlan, match.getDataLayerVirtualLan());
            int wildcards = OFPFW_ALL & ~OFPFW_DL_VLAN;
            assertEquals(wildcards, match.getWildcards());
        }

        // Test for "dl_vpcp".
        byte pcps[] = {1, 3, 7};
        for (byte pcp: pcps) {
            V6Match match = new V6Match();
            match.fromString("dl_vpcp=" + pcp);
            assertEquals(MATCH_LEN_VLAN_TCI_WITH_MASK, match.getIPv6MatchLen());
            assertEquals(pcp, match.getDataLayerVirtualLanPriorityCodePoint());
            int wildcards = OFPFW_ALL & ~OFPFW_DL_VLAN_PCP;
            assertEquals(wildcards, match.getWildcards());
        }

        // Set "dl_vlan" field firstly, "dl_vpcp" field secondly.
        for (short vlan: vlans) {
            for (byte pcp: pcps) {
                V6Match match = new V6Match();
                match.fromString("dl_vlan=" + vlan);
                match.fromString("dl_vpcp=" + pcp);
                assertEquals(MATCH_LEN_VLAN_TCI, match.getIPv6MatchLen());
                assertEquals(vlan, match.getDataLayerVirtualLan());
                assertEquals(pcp,
                        match.getDataLayerVirtualLanPriorityCodePoint());
            }
        }

        // Set "dl_vpcp" field firstly, "dl_vlan" field secondly.
        for (short vlan: vlans) {
            for (byte pcp: pcps) {
                V6Match match = new V6Match();
                match.fromString("dl_vpcp=" + pcp);
                match.fromString("dl_vlan=" + vlan);
                assertEquals(MATCH_LEN_VLAN_TCI, match.getIPv6MatchLen());
                assertEquals(vlan, match.getDataLayerVirtualLan());
                assertEquals(pcp,
                        match.getDataLayerVirtualLanPriorityCodePoint());
            }
        }

        // Test for OFP_VLAN_NONE when VLAN PCP is not set.
        V6Match match = new V6Match();
        match.fromString("dl_vlan=" + OFP_VLAN_NONE);
        assertEquals(MATCH_LEN_VLAN_TCI, match.getIPv6MatchLen());
        assertEquals(OFP_VLAN_NONE, match.getDataLayerVirtualLan());

        // Test for OFP_VLAN_NONE when VLAN PCP is set.
        match = new V6Match();
        match.fromString("dl_vpcp=" + 1);
        try {
            match.fromString("dl_vlan=" + OFP_VLAN_NONE);
            fail("Throwing exception was expected.");
        } catch (IllegalArgumentException e) {
            // Throwing exception was expected.
        }
    }

    /**
     * Test case for {@link V6Match#writeTo(ByteBuffer)} for VLAN TCI field.
     */
    @Test
    public void testWriteToVlanTci() {
        byte mask = 0;

        // Set only VLAN ID.
        short vlans[] = {1, 10, 1000, 4095};
        for (short vlan: vlans) {
            V6Match match = new V6Match();
            match.setDataLayerVirtualLan(vlan, mask);
            ByteBuffer data = ByteBuffer.allocate(10);
            match.writeTo(data);
            assertEquals(MATCH_LEN_VLAN_TCI_WITH_MASK, data.position());
            data.flip();
            // Header
            assertEquals(HEADER_VLAN_TCI_W, data.getInt());
            // Value
            short expectedTci = (short) (VLAN_TCI_CFI | vlan);
            assertEquals(expectedTci, data.getShort());
            // Mask
            short expectedMask = 0x1fff;
            assertEquals(expectedMask, data.getShort());
        }

        // Set only VLAN PCP.
        byte pcps[] = {1, 3, 7};
        for (byte pcp: pcps) {
            V6Match match = new V6Match();
            match.setDataLayerVirtualLanPriorityCodePoint(pcp, mask);
            ByteBuffer data = ByteBuffer.allocate(10);
            match.writeTo(data);
            assertEquals(MATCH_LEN_VLAN_TCI_WITH_MASK, data.position());
            data.flip();
            // Header
            assertEquals(HEADER_VLAN_TCI_W, data.getInt());
            // Value
            short expectedTci = (short) (pcp << 13 | VLAN_TCI_CFI);
            assertEquals(expectedTci, data.getShort());
            // Mask
            short expectedMask = (short) 0xf000;
            assertEquals(expectedMask, data.getShort());
        }

        // Set both VLAN ID and PCP.
        for (short vlan: vlans) {
            for (byte pcp: pcps) {
                V6Match match = new V6Match();
                match.setDataLayerVirtualLan(vlan, mask);
                match.setDataLayerVirtualLanPriorityCodePoint(pcp, mask);
                ByteBuffer data = ByteBuffer.allocate(10);
                match.writeTo(data);
                assertEquals(MATCH_LEN_VLAN_TCI, data.position());
                data.flip();
                // Header
                assertEquals(HEADER_VLAN_TCI, data.getInt());
                // Value
                short expectedTci = (short) (pcp << 13 | VLAN_TCI_CFI | vlan);
                assertEquals(expectedTci, data.getShort());
            }
        }

        // Set OFP_VLAN_NONE.
        V6Match match = new V6Match();
        match.setDataLayerVirtualLan(OFP_VLAN_NONE, mask);
        ByteBuffer data = ByteBuffer.allocate(10);
        match.writeTo(data);
        assertEquals(MATCH_LEN_VLAN_TCI, data.position());
        data.flip();
        // Header
        assertEquals(HEADER_VLAN_TCI, data.getInt());
        // Value
        assertEquals(0, data.getShort());
    }

    /**
     * Test case for {@link V6Match#writeTo(ByteBuffer)} for input port field.
     */
    @Test
    public void testWriteToInputPort() {
        // Set input port.
        short ports[] = {1, 10, 100, 1000};
        for (short port: ports) {
            V6Match match = new V6Match();
            match.setInputPort(port, (short) 0);
            ByteBuffer data = ByteBuffer.allocate(10);
            match.writeTo(data);
            assertEquals(MATCH_LEN_INPUT_PORT, data.position());
            data.flip();
            // Header
            assertEquals(HEADER_INPUT_PORT, data.getInt());
            // Value
            assertEquals(port, data.getShort());
        }
    }

    /**
     * Test case for {@link V6Match#readFrom(ByteBuffer)} for VLAN TCI field.
     */
    @Test
    public void testReadFromVlanTci() {
        // Test for an exact match a TCI value with CFI=1.
        // It matches packets that have an 802.1Q header with a specified
        // VID and PCP.
        short vlans[] = {1, 10, 1000, 4095};
        byte pcps[] = {1, 3, 7};
        for (short vlan: vlans) {
            for (byte pcp: pcps) {
                ByteBuffer data = ByteBuffer.allocate(MATCH_LEN_VLAN_TCI);
                data.putInt(HEADER_VLAN_TCI);
                short tci = (short) (pcp << 13 | VLAN_TCI_CFI | vlan);
                data.putShort(tci);
                data.flip();

                V6Match match = new V6Match();
                match.readFrom(data);
                assertEquals(MATCH_LEN_VLAN_TCI, match.getIPv6MatchLen());
                assertEquals(pcp,
                        match.getDataLayerVirtualLanPriorityCodePoint());
                assertEquals(vlan, match.getDataLayerVirtualLan());
                int wildcards = OFPFW_ALL & ~OFPFW_DL_VLAN_PCP & ~OFPFW_DL_VLAN;
                assertEquals(wildcards, match.getWildcards());
            }
        }

        // Test with a specific VID and CFI=1 with mask=0x1fff.
        // It matches packets that have an 802.1Q header with that VID
        // and any PCP.
        for (short vlan: vlans) {
            ByteBuffer data = ByteBuffer.allocate(MATCH_LEN_VLAN_TCI_WITH_MASK);
            data.putInt(HEADER_VLAN_TCI_W);
            short tci = (short) (VLAN_TCI_CFI | vlan);
            data.putShort(tci);
            short mask = (short) 0x1fff;
            data.putShort(mask);
            data.flip();

            V6Match match = new V6Match();
            match.readFrom(data);
            assertEquals(MATCH_LEN_VLAN_TCI_WITH_MASK, match.getIPv6MatchLen());
            assertEquals(vlan, match.getDataLayerVirtualLan());
            int wildcards = OFPFW_ALL & ~OFPFW_DL_VLAN;
            assertEquals(wildcards, match.getWildcards());
        }

        // Test with a specific PCP and CFI=1 with mask=0xf000.
        // It matches packets that have an 802.1Q header with that PCP
        // and any VID.
        for (byte pcp: pcps) {
            ByteBuffer data = ByteBuffer.allocate(MATCH_LEN_VLAN_TCI_WITH_MASK);
            data.putInt(HEADER_VLAN_TCI_W);
            short tci = (short) (pcp << 13| VLAN_TCI_CFI);
            data.putShort(tci);
            short mask = (short) 0xf000;
            data.putShort(mask);
            data.flip();

            V6Match match = new V6Match();
            match.readFrom(data);
            assertEquals(MATCH_LEN_VLAN_TCI_WITH_MASK, match.getIPv6MatchLen());
            assertEquals(pcp, match.getDataLayerVirtualLanPriorityCodePoint());
            int wildcards = OFPFW_ALL & ~OFPFW_DL_VLAN_PCP;
            assertEquals(wildcards, match.getWildcards());
        }

        // Test for an exact match with 0.
        // It matches only packets without an 802.1Q header.
        ByteBuffer data = ByteBuffer.allocate(MATCH_LEN_VLAN_TCI);
        data.putInt(HEADER_VLAN_TCI);
        short tci = 0;
        data.putShort(tci);
        data.flip();

        V6Match match = new V6Match();
        match.readFrom(data);
        assertEquals(MATCH_LEN_VLAN_TCI, match.getIPv6MatchLen());
        assertEquals(OFP_VLAN_NONE, match.getDataLayerVirtualLan());
        int wildcards = OFPFW_ALL & ~OFPFW_DL_VLAN;
        assertEquals(wildcards, match.getWildcards());
    }

    /**
     * Test case for {@link V6Match#readFrom(ByteBuffer)} for input port field.
     */
    @Test
    public void testReadFromInputPort() {
        // Set input port.
        short ports[] = {1, 10, 100, 1000};
        for (short port: ports) {
            ByteBuffer data = ByteBuffer.allocate(MATCH_LEN_INPUT_PORT);
            data.putInt(HEADER_INPUT_PORT);
            data.putShort(port);
            data.flip();

            V6Match match = new V6Match();
            match.readFrom(data);
            assertEquals(MATCH_LEN_INPUT_PORT, match.getIPv6MatchLen());
            assertEquals(port, match.getInputPort());
            int wildcards = OFPFW_ALL & ~OFPFW_IN_PORT;
            assertEquals(wildcards, match.getWildcards());
        }
    }

    /**
     * Test case for {@link V6Match#setDataLayerVirtualLan(short, short)}.
     */
    @Test
    public void testSetDataLayerVirtualLan() {
        short vlans[] = {1, 10, 1000, 4095};
        short mask = 0;
        for (short vlan: vlans) {
            V6Match match = new V6Match();
            match.setDataLayerVirtualLan(vlan, mask);
            assertEquals(MATCH_LEN_VLAN_TCI_WITH_MASK, match.getIPv6MatchLen());
            assertEquals(vlan, match.getDataLayerVirtualLan());
        }

        // Test for OFP_VLAN_NONE.
        V6Match match = new V6Match();
        match.setDataLayerVirtualLan(OFP_VLAN_NONE, mask);
        assertEquals(MATCH_LEN_VLAN_TCI, match.getIPv6MatchLen());
        assertEquals(OFP_VLAN_NONE, match.getDataLayerVirtualLan());
    }

    /**
     * Test case for
     * {@link V6Match#setDataLayerVirtualLanPriorityCodePoint(byte, byte)}.
     */
    @Test
    public void testSetDataLayerVirtualLanPriorityCodePoint() {
        byte pcps[] = {1, 3, 7};
        byte mask = 0;
        for (byte pcp: pcps) {
            V6Match match = new V6Match();
            match.setDataLayerVirtualLanPriorityCodePoint(pcp, mask);
            assertEquals(MATCH_LEN_VLAN_TCI_WITH_MASK, match.getIPv6MatchLen());
            assertEquals(pcp, match.getDataLayerVirtualLanPriorityCodePoint());
        }
    }

    /**
     * Test case for setter methods for VLAN TCI field.
     *
     * This test case calls {@link V6Match#setDataLayerVirtualLan(short, short)}
     * and {@link V6Match#setDataLayerVirtualLanPriorityCodePoint(byte, byte)}.
     */
    @Test
    public void testSetVlanTCI() {
        short vlans[] = {1, 10, 1000, 4095};
        byte pcps[] = {1, 3, 7};
        byte mask = 0;

        // Call setDataLayerVirtualLan(short, short) firstly,
        // and setDataLayerVirtualLanPriorityCodePoint(byte, byte) secondly,
        for (short vlan: vlans) {
            for (byte pcp: pcps) {
                V6Match match = new V6Match();
                match.setDataLayerVirtualLan(vlan, mask);
                match.setDataLayerVirtualLanPriorityCodePoint(pcp, mask);
                assertEquals(MATCH_LEN_VLAN_TCI, match.getIPv6MatchLen());
                assertEquals(vlan, match.getDataLayerVirtualLan());
                assertEquals(pcp,
                             match.getDataLayerVirtualLanPriorityCodePoint());
            }
        }

        // Call setDataLayerVirtualLanPriorityCodePoint(byte, byte) firstly,
        // and setDataLayerVirtualLan(short, short) secondly.
        for (short vlan: vlans) {
            for (byte pcp: pcps) {
                V6Match match = new V6Match();
                match.setDataLayerVirtualLanPriorityCodePoint(pcp, mask);
                match.setDataLayerVirtualLan(vlan, mask);
                assertEquals(MATCH_LEN_VLAN_TCI, match.getIPv6MatchLen());
                assertEquals(vlan, match.getDataLayerVirtualLan());
                assertEquals(pcp,
                             match.getDataLayerVirtualLanPriorityCodePoint());
            }
        }

        // Test for setting OFP_VLAN_NONE when VLAN PCP is set.
        for (byte pcp: pcps) {
            V6Match match = new V6Match();
            match.setDataLayerVirtualLanPriorityCodePoint(pcp, mask);
            try {
                match.setDataLayerVirtualLan(OFP_VLAN_NONE, mask);
            } catch (IllegalStateException e) {
                // Throwing exception was expected.
            }
        }

        // Test for set VLAN PCP when OFP_VLAN_NONE is set to VLAN match.
        for (byte pcp: pcps) {
            V6Match match = new V6Match();
            match.setDataLayerVirtualLan(OFP_VLAN_NONE, mask);
            try {
                match.setDataLayerVirtualLanPriorityCodePoint(pcp, mask);
            } catch (IllegalStateException e) {
                // Throwing exception was expected.
            }
        }
    }

    /**
     * Test case for {@link V6Match#setInputPort(short, short)}.
     */
    @Test
    public void testSetInputPort() {
        short ports[] = {1, 10, 100, 1000};
        for (short port: ports) {
            V6Match match = new V6Match();
            match.setInputPort(port, (short) 0);
            assertEquals(MATCH_LEN_INPUT_PORT, match.getIPv6MatchLen());
            assertEquals(port, match.getInputPort());
        }
    }
}
