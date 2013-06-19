/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.packet;

import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.utils.NetUtils;

public class IEEE8021QTest {

    @Test
    public void testGetPcp() throws Exception {
        IEEE8021Q vlan = new IEEE8021Q();
        byte pcp[] = { 5 };
        vlan.hdrFieldsMap.put("PriorityCodePoint", pcp);
        byte spcp = vlan.getPcp();
        Assert.assertTrue(spcp == 5);
    }

    @Test
    public void testGetCfi() throws Exception {
        IEEE8021Q vlan = new IEEE8021Q();
        byte cfi[] = { 0 };
        vlan.hdrFieldsMap.put("CanonicalFormatIndicator", cfi);
        byte scfi = vlan.getCfi();
        Assert.assertTrue(scfi == 0);
    }

    @Test
    public void testGetVid() throws Exception {
        IEEE8021Q vlan = new IEEE8021Q();
        byte vid[] = { (byte) 0xF, (byte) 0xFE }; // 4094
        vlan.hdrFieldsMap.put("VlanIdentifier", vid);
        short svid = vlan.getVid();
        Assert.assertTrue(svid == 4094);
    }

    @Test
    public void testGetEthertype() throws Exception {
        IEEE8021Q vlan = new IEEE8021Q();
        byte ethType[] = { 8, 6 };
        vlan.hdrFieldsMap.put("EtherType", ethType);
        short etherType = vlan.getEtherType();
        Assert.assertTrue(etherType == 2054);
    }

    @Test
    public void testSetPcp() throws Exception {
        IEEE8021Q vlan = new IEEE8021Q();
        byte pcp = 5;
        vlan.setPcp(pcp);
        byte[] bpcp = vlan.hdrFieldsMap.get("PriorityCodePoint");
        Assert.assertTrue(bpcp[0] == 5);
    }

    @Test
    public void testSetCfi() throws Exception {
        IEEE8021Q vlan = new IEEE8021Q();
        byte cfi = 0;
        vlan.setCfi(cfi);
        byte[] bcfi = vlan.hdrFieldsMap.get("CanonicalFormatIndicator");
        Assert.assertTrue(bcfi[0] == 0);
    }

    @Test
    public void testSetVid() throws Exception {
        IEEE8021Q vlan = new IEEE8021Q();
        short vid = 4094; // 0xFFE
        vlan.setVid(vid);
        byte[] bvid = vlan.hdrFieldsMap.get("VlanIdentifier");
        Assert.assertTrue(bvid[0] == (byte) 0xF);
        Assert.assertTrue(bvid[1] == (byte) 0xFE);
    }

    @Test
    public void testSetEthertype() throws Exception {
        Ethernet eth = new Ethernet();
        short ethType = 2054; // 0x806
        eth.setEtherType(ethType);
        byte[] etherType = eth.hdrFieldsMap.get("EtherType");
        Assert.assertTrue(etherType[0] == 8);
        Assert.assertTrue(etherType[1] == 6);
    }

    @Test
    public void testDeserialize() throws Exception {
        short startOffset, numBits;
        Ethernet eth = new Ethernet();
        byte[] data = {
                (byte) 0xA, (byte) 0xC, (byte) 0xE, (byte) 0x14, (byte) 0x37, (byte) 0x45, // Destination MAC
                (byte) 0xA6, (byte) 0xEC, (byte) 0x9C, (byte) 0xAE, (byte) 0xB2, (byte) 0x9F, // Source MAC
                (byte) 0x81, (byte) 0x00, // EtherType
                (byte) 0xAF, (byte) 0xFE, // PCP, CFI, VLAN ID
                8, 6, // EtherType
                0, 1, // Hardware Type
                8, 0, // Protocol Type
                6, // Hardware Address Length
                4, // Protocol Address Length
                0, 1, // opCode
                (byte) 0xA6, (byte) 0xEC, (byte) 0x9C, (byte) 0xAE, (byte) 0xB2, (byte) 0x9F, // Sender Hardware Address
                (byte) 0x9, (byte) 0x9, (byte) 0x9, (byte) 0x1, // Sender Protocol Address
                (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, // Target Hardware Address
                (byte) 0x9, (byte) 0x9, (byte) 0x9, (byte) 0xFE }; // Target Protocol Address

        startOffset = 0;
        numBits = (short) (data.length * 8);
        eth.deserialize(data, startOffset, numBits);

        short etherType = eth.getEtherType();
        Assert.assertTrue(NetUtils.getUnsignedShort(etherType) == 0x8100);

        IEEE8021Q vlanPacket = (IEEE8021Q) eth.getPayload();
        Assert.assertTrue(vlanPacket.getCfi() == 0);
        Assert.assertTrue(vlanPacket.getPcp() == 5);
        Assert.assertTrue(vlanPacket.getVid() == 4094);
        Assert.assertTrue(vlanPacket.getEtherType() == 2054); // 0x806

        Packet arpPkt = (vlanPacket).getPayload();
        Assert.assertTrue(arpPkt instanceof ARP);
    }

    @Test
    public void testSerialize() throws Exception {
        Ethernet eth = new Ethernet();

        byte[] dMac = { (byte) 0xA, (byte) 0xC, (byte) 0xE, (byte) 0x14, (byte) 0x37, (byte) 0x45 };
        byte[] sMac = { (byte) 0xA6, (byte) 0xEC, (byte) 0x9C, (byte) 0xAE, (byte) 0xB2, (byte) 0x9F };
        eth.setDestinationMACAddress(dMac);
        eth.setSourceMACAddress(sMac);
        eth.setEtherType((short) 33024);

        IEEE8021Q vlan = new IEEE8021Q();
        vlan.setCfi((byte) 0x0);
        vlan.setPcp((byte) 0x5);
        vlan.setVid((short) 4094);
        vlan.setEtherType((short) 2054);

        vlan.setParent(eth);
        eth.setPayload(vlan);

        ARP arp = new ARP();
        arp.setHardwareType((short) 1);
        arp.setProtocolType((short) 2048);
        arp.setHardwareAddressLength((byte) 0x6);
        arp.setProtocolAddressLength((byte) 0x4);
        arp.setOpCode((byte) 0x1);

        byte[] senderHardwareAddress = { (byte) 0xA6, (byte) 0xEC, (byte) 0x9C, (byte) 0xAE, (byte) 0xB2, (byte) 0x9F };
        byte[] senderProtocolAddress = { (byte) 0x9, (byte) 0x9, (byte) 0x9, (byte) 0x1 };
        byte[] targetProtocolAddress = { (byte) 0x9, (byte) 0x9, (byte) 0x9, (byte) 0xFE };
        byte[] targetHardwareAddress = { (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0 };
        arp.setSenderHardwareAddress(senderHardwareAddress);
        arp.setSenderProtocolAddress(senderProtocolAddress);
        arp.setTargetHardwareAddress(targetHardwareAddress);
        arp.setTargetProtocolAddress(targetProtocolAddress);

        arp.setParent(vlan);
        vlan.setPayload(arp);

        byte[] data = eth.serialize();

        Assert.assertTrue(data[0] == (byte) 0x0A); // Destination MAC
        Assert.assertTrue(data[1] == (byte) 0x0C);
        Assert.assertTrue(data[2] == (byte) 0x0E);
        Assert.assertTrue(data[3] == (byte) 0x14);
        Assert.assertTrue(data[4] == (byte) 0x37);
        Assert.assertTrue(data[5] == (byte) 0x45);
        Assert.assertTrue(data[6] == (byte) 0xA6); // Source MAC
        Assert.assertTrue(data[7] == (byte) 0xEC);
        Assert.assertTrue(data[8] == (byte) 0x9C);
        Assert.assertTrue(data[9] == (byte) 0xAE);
        Assert.assertTrue(data[10] == (byte) 0xB2);
        Assert.assertTrue(data[11] == (byte) 0x9F);
        Assert.assertTrue(data[12] == (byte) 0x81); // EtherType
        Assert.assertTrue(data[13] == (byte) 0x00);
        Assert.assertTrue(data[14] == (byte) 0xAF); // PCP, CFI, VLAN ID
        Assert.assertTrue(data[15] == (byte) 0xFE);
        Assert.assertTrue(data[16] == (byte) 0x08); // EtherType
        Assert.assertTrue(data[17] == (byte) 0x06);
        Assert.assertTrue(data[18] == (byte) 0x00); // Hardware Type
        Assert.assertTrue(data[19] == (byte) 0x01);
        Assert.assertTrue(data[20] == (byte) 0x08); // Protocol Type
        Assert.assertTrue(data[21] == (byte) 0x0);
        Assert.assertTrue(data[22] == (byte) 0x6); // Hardware Address Length
        Assert.assertTrue(data[23] == (byte) 0x4); // Protocol Address Length
        Assert.assertTrue(data[24] == (byte) 0x0); // opCode
        Assert.assertTrue(data[25] == (byte) 0x1); // opCode
        Assert.assertTrue(data[26] == (byte) 0xA6); // Source MAC
        Assert.assertTrue(data[27] == (byte) 0xEC);
        Assert.assertTrue(data[28] == (byte) 0x9C);
        Assert.assertTrue(data[29] == (byte) 0xAE);
        Assert.assertTrue(data[30] == (byte) 0xB2);
        Assert.assertTrue(data[31] == (byte) 0x9F);
        Assert.assertTrue(data[32] == (byte) 0x09); // Sender Protocol Address
        Assert.assertTrue(data[33] == (byte) 0x09);
        Assert.assertTrue(data[34] == (byte) 0x09);
        Assert.assertTrue(data[35] == (byte) 0x01); // Target Hardware Address
        Assert.assertTrue(data[36] == (byte) 0x00);
        Assert.assertTrue(data[37] == (byte) 0x00);
        Assert.assertTrue(data[38] == (byte) 0x00);
        Assert.assertTrue(data[39] == (byte) 0x00);
        Assert.assertTrue(data[40] == (byte) 0x00);
        Assert.assertTrue(data[41] == (byte) 0x00);
        Assert.assertTrue(data[42] == (byte) 0x09); // Target Protocol Address
        Assert.assertTrue(data[43] == (byte) 0x09);
        Assert.assertTrue(data[44] == (byte) 0x09);
        Assert.assertTrue(data[45] == (byte) 0xFE);
    }
}
