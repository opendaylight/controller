
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.packet;

import org.junit.Test;
import org.opendaylight.controller.sal.packet.ARP;

import junit.framework.Assert;

public class ARPTest {

    @Test
    public void testGetHardwareType() {
        ARP arp = new ARP();
        byte[] hardwaretype = { 8, 6 };
        arp.hdrFieldsMap.put("HardwareType", hardwaretype);
        short hwtype = arp.getHardwareType();
        Assert.assertTrue(hwtype == 2054);
    }

    @Test
    public void testGetProtocolType() {
        ARP arp = new ARP();
        byte[] protocoltype = { 8, 0 };
        arp.hdrFieldsMap.put("ProtocolType", protocoltype);
        short ptype = arp.getProtocolType();
        Assert.assertTrue(ptype == 2048);
    }

    @Test
    public void testGetHardwareAddressLength() {
        ARP arp = new ARP();
        byte[] hardwareaddresslength = { 48 };
        arp.hdrFieldsMap.put("HardwareAddressLength", hardwareaddresslength);
        byte hwaddrlength = arp.getHardwareAddressLength();
        Assert.assertTrue(hwaddrlength == 48);
    }

    @Test
    public void testGetProtocolAddressLength() {
        ARP arp = new ARP();
        byte[] protocoladdresslength = { 32 };
        arp.hdrFieldsMap.put("ProtocolAddressLength", protocoladdresslength);
        byte paddrlength = arp.getProtocolAddressLength();
        Assert.assertTrue(paddrlength == 32);
    }

    @Test
    public void testGetOpCode() {
        ARP arp = new ARP();
        byte[] opcode = { 0, 2 };
        arp.hdrFieldsMap.put("OpCode", opcode);
        short opCode = arp.getOpCode();
        Assert.assertTrue(opCode == 2);
    }

    @Test
    public void testGetSenderHardwareAddress() {
        ARP arp = new ARP();
        byte[] hardwareaddress = { 48, 50, 120, 15, 66, 80 };
        arp.hdrFieldsMap.put("SenderHardwareAddress", hardwareaddress);
        byte[] hwAddress = arp.getSenderHardwareAddress();
        Assert.assertTrue(hwAddress[0] == 48);
        Assert.assertTrue(hwAddress[1] == 50);
        Assert.assertTrue(hwAddress[2] == 120);
        Assert.assertTrue(hwAddress[3] == 15);
        Assert.assertTrue(hwAddress[4] == 66);
        Assert.assertTrue(hwAddress[5] == 80);
    }

    @Test
    public void testGetSenderProtocolAddress() {
        ARP arp = new ARP();
        byte[] protocoladdress = { 50, 100, 10, 20, 40, 80 };
        arp.hdrFieldsMap.put("SenderProtocolAddress", protocoladdress);
        byte[] pAddress = arp.getSenderProtocolAddress();
        Assert.assertTrue(pAddress[0] == 50);
        Assert.assertTrue(pAddress[1] == 100);
        Assert.assertTrue(pAddress[2] == 10);
        Assert.assertTrue(pAddress[3] == 20);
        Assert.assertTrue(pAddress[4] == 40);
        Assert.assertTrue(pAddress[5] == 80);
    }

    @Test
    public void testGetTargetHardwareAddress() {
        ARP arp = new ARP();
        byte[] hardwareaddress = { 48, 50, 120, 15, 66, 80 };
        arp.hdrFieldsMap.put("TargetHardwareAddress", hardwareaddress);
        byte[] hwAddress = arp.getTargetHardwareAddress();
        Assert.assertTrue(hwAddress[0] == 48);
        Assert.assertTrue(hwAddress[1] == 50);
        Assert.assertTrue(hwAddress[2] == 120);
        Assert.assertTrue(hwAddress[3] == 15);
        Assert.assertTrue(hwAddress[4] == 66);
        Assert.assertTrue(hwAddress[5] == 80);
    }

    @Test
    public void testGetTargetProtocolAddress() {
        ARP arp = new ARP();
        byte[] protocoladdress = { 50, 100, 10, 20, 40, 80 };
        arp.hdrFieldsMap.put("TargetProtocolAddress", protocoladdress);
        byte[] pAddress = arp.getTargetProtocolAddress();
        Assert.assertTrue(pAddress[0] == 50);
        Assert.assertTrue(pAddress[1] == 100);
        Assert.assertTrue(pAddress[2] == 10);
        Assert.assertTrue(pAddress[3] == 20);
        Assert.assertTrue(pAddress[4] == 40);
        Assert.assertTrue(pAddress[5] == 80);
    }

    @Test
    public void testSetHardwareType() {
        ARP arp = new ARP();
        short hwtype = 2054;
        arp.setHardwareType(hwtype);
        byte[] hardwaretype = arp.hdrFieldsMap.get("HardwareType");
        Assert.assertTrue(hardwaretype[0] == 8);
        Assert.assertTrue(hardwaretype[1] == 6);
    }

    @Test
    public void testSetProtocolType() {
        ARP arp = new ARP();
        short ptype = 2048;
        arp.setProtocolType(ptype);
        byte[] protocoltype = arp.hdrFieldsMap.get("ProtocolType");
        Assert.assertTrue(protocoltype[0] == 8);
        Assert.assertTrue(protocoltype[1] == 0);
    }

    @Test
    public void testSetHardwareAddressLength() {
        ARP arp = new ARP();
        byte hwaddrlength = 48;
        arp.setHardwareAddressLength(hwaddrlength);
        byte[] hardwareaddresslength = arp.hdrFieldsMap
                .get("HardwareAddressLength");
        Assert.assertTrue(hardwareaddresslength[0] == 48);
    }

    @Test
    public void testSetProtocolAddressLength() {
        ARP arp = new ARP();
        byte PAddrlength = 32;
        arp.setProtocolAddressLength(PAddrlength);
        byte[] protocoladdresslength = arp.hdrFieldsMap
                .get("ProtocolAddressLength");
        Assert.assertTrue(protocoladdresslength[0] == 32);
    }

    @Test
    public void testSetOpCode() {
        ARP arp = new ARP();
        short opCode = (short) 2;
        arp.setOpCode(opCode);
        byte[] opcode = arp.hdrFieldsMap.get("OpCode");
        //System.out.println(opCode);
        Assert.assertTrue(opcode[0] == 0);
        Assert.assertTrue(opcode[1] == 2);
    }

    @Test
    public void testSetSenderHardwareAddress() {
        ARP arp = new ARP();
        byte[] hardwareaddress = { 48, 50, 120, 15, 66, 80 };
        arp.setSenderHardwareAddress(hardwareaddress);
        byte[] hwAddress = arp.hdrFieldsMap.get("SenderHardwareAddress");
        Assert.assertTrue(hwAddress[0] == 48);
        Assert.assertTrue(hwAddress[1] == 50);
        Assert.assertTrue(hwAddress[2] == 120);
        Assert.assertTrue(hwAddress[3] == 15);
        Assert.assertTrue(hwAddress[4] == 66);
        Assert.assertTrue(hwAddress[5] == 80);
    }

    @Test
    public void testSetSenderProtocolAddress() {
        ARP arp = new ARP();
        byte[] protocoladdress = { 50, 100, 10, 20, 40, 80 };
        arp.setSenderProtocolAddress(protocoladdress);
        byte[] pAddress = arp.hdrFieldsMap.get("SenderProtocolAddress");
        Assert.assertTrue(pAddress[0] == 50);
        Assert.assertTrue(pAddress[1] == 100);
        Assert.assertTrue(pAddress[2] == 10);
        Assert.assertTrue(pAddress[3] == 20);
        Assert.assertTrue(pAddress[4] == 40);
        Assert.assertTrue(pAddress[5] == 80);
    }

    @Test
    public void testSetTargetHardwareAddress() {
        ARP arp = new ARP();
        byte[] hardwareaddress = { 48, 50, 120, 15, 66, 80 };
        arp.setTargetHardwareAddress(hardwareaddress);
        byte[] hwAddress = arp.hdrFieldsMap.get("TargetHardwareAddress");
        Assert.assertTrue(hwAddress[0] == 48);
        Assert.assertTrue(hwAddress[1] == 50);
        Assert.assertTrue(hwAddress[2] == 120);
        Assert.assertTrue(hwAddress[3] == 15);
        Assert.assertTrue(hwAddress[4] == 66);
        Assert.assertTrue(hwAddress[5] == 80);
    }

    @Test
    public void testSetTargetProtocolAddress() {
        ARP arp = new ARP();
        byte[] protocoladdress = { 50, 100, 10, 20, 40, 80 };
        arp.setTargetProtocolAddress(protocoladdress);
        byte[] pAddress = arp.hdrFieldsMap.get("TargetProtocolAddress");
        Assert.assertTrue(pAddress[0] == 50);
        Assert.assertTrue(pAddress[1] == 100);
        Assert.assertTrue(pAddress[2] == 10);
        Assert.assertTrue(pAddress[3] == 20);
        Assert.assertTrue(pAddress[4] == 40);
        Assert.assertTrue(pAddress[5] == 80);
    }

}
