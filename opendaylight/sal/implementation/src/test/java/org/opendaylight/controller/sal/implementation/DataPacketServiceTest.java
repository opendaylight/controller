
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.implementation;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.implementation.internal.DataPacketService;
import org.opendaylight.controller.sal.packet.ARP;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.RawPacket;

public class DataPacketServiceTest {

	@Test
	public void DataPacketServiceDecodeTest() throws ConstructionException, InstantiationException, IllegalAccessException {
		
		DataPacketService dService = new DataPacketService();
		RawPacket rawPkt = null;
		
		Assert.assertTrue(dService.decodeDataPacket(rawPkt) == null);
		
        byte[] data = { 10, 12, 14, 20, 55, 69, //DMAC
                -90, -20, -100, -82, -78, -97, //SMAC
                8, 6, //ethype
                0, 1, // hw type
                8, 0, // proto (ip)
                6, // hw addr len
                4, // proto addr len
                0, 1, // op codes
                -90, -20, -100, -82, -78, -97, //src hw addr
                9, 9, 9, 1, // src proto
                0, 0, 0, 0, 0, 0, // target hw addr
                9, 9, 9, -2 }; // target proto
        
        rawPkt = new RawPacket(data);
        
        Packet decodedPkt = dService.decodeDataPacket(rawPkt);
        Class<? extends Packet> payloadClass = ARP.class;
        Assert.assertTrue(payloadClass == decodedPkt.getPayload().getClass());
                
        ARP arpPacket = (ARP) decodedPkt.getPayload();
        
        Assert.assertTrue(arpPacket.getHardwareType() == (byte)0x1);
        Assert.assertTrue(arpPacket.getProtocolType() == 2048);
        Assert.assertTrue(arpPacket.getHardwareAddressLength() == (byte)0x6);
        Assert.assertTrue(arpPacket.getProtocolAddressLength() == (byte)0x4);
        Assert.assertTrue(arpPacket.getOpCode() == 1);
        
        byte[] senderHwAddress = arpPacket.getSenderHardwareAddress();
        byte[] senderProtocolAddress = arpPacket.getSenderProtocolAddress(); 
        
        byte[] targetHwAddress = arpPacket.getTargetHardwareAddress();
        byte[] targetProtocolAddress = arpPacket.getTargetProtocolAddress(); 

        
        Assert.assertTrue(senderHwAddress[0] == (byte)0xA6);
        Assert.assertTrue(senderHwAddress[1] == (byte)0xEC);
        Assert.assertTrue(senderHwAddress[2] == (byte)0x9C);
        Assert.assertTrue(senderHwAddress[3] == (byte)0xAE);
        Assert.assertTrue(senderHwAddress[4] == (byte)0xB2);
        Assert.assertTrue(senderHwAddress[5] == (byte)0x9F);
        
        Assert.assertTrue(senderProtocolAddress[0] == (byte)0x9);
        Assert.assertTrue(senderProtocolAddress[1] == (byte)0x9);
        Assert.assertTrue(senderProtocolAddress[2] == (byte)0x9);
        Assert.assertTrue(senderProtocolAddress[3] == (byte)0x1);

        Assert.assertTrue(targetHwAddress[0] == (byte)0x0);
        Assert.assertTrue(targetHwAddress[1] == (byte)0x0);
        Assert.assertTrue(targetHwAddress[2] == (byte)0x0);
        Assert.assertTrue(targetHwAddress[3] == (byte)0x0);
        Assert.assertTrue(targetHwAddress[4] == (byte)0x0);
        Assert.assertTrue(targetHwAddress[5] == (byte)0x0);

        Assert.assertTrue(senderProtocolAddress[0] == (byte)0x9);
        Assert.assertTrue(senderProtocolAddress[1] == (byte)0x9);
        Assert.assertTrue(senderProtocolAddress[2] == (byte)0x9);
        Assert.assertTrue(senderProtocolAddress[3] == (byte)0x1);

        Assert.assertTrue(targetProtocolAddress[0] == (byte)0x9);
        Assert.assertTrue(targetProtocolAddress[1] == (byte)0x9);
        Assert.assertTrue(targetProtocolAddress[2] == (byte)0x9);
        Assert.assertTrue(targetProtocolAddress[3] == (byte)0xFE);      
	}
	
	@Test
	public void DataPacketServiceEncodeTest() throws ConstructionException, InstantiationException, IllegalAccessException {
		
		DataPacketService dService = new DataPacketService();
		Ethernet eth = new Ethernet();
        ARP arp = new ARP();

		byte[] data = null;
		RawPacket rawPkt;


        byte[] dMAC = { 10, 12, 14, 20, 55, 69 };
        byte[] sMAC = { 82, 97, 109, 117, 127, -50 };
        short etherType = 2054;
        
        eth.setDestinationMACAddress(dMAC);
        eth.setSourceMACAddress(sMAC);
        eth.setEtherType(etherType);
               
        arp.setHardwareType((short)1);
        arp.setProtocolType((short)2048);
        arp.setHardwareAddressLength((byte)0x6);
        arp.setProtocolAddressLength((byte)0x4);
        arp.setOpCode((byte)0x1);
        
        byte[] senderHardwareAddress = {(byte)0xA6, (byte)0xEC, (byte)0x9C, (byte)0xAE,
        								(byte)0xB2, (byte)0x9F};
        byte[] senderProtocolAddress = {(byte)0x09, (byte)0x09, (byte)0x09, (byte)0x01};
        byte[] targetProtocolAddress = {(byte)0x09, (byte)0x09, (byte)0x09, (byte)0xFE};
        byte[] targetHardwareAddress = {(byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0};
        arp.setSenderHardwareAddress(senderHardwareAddress);
        arp.setSenderProtocolAddress(senderProtocolAddress);
        arp.setTargetHardwareAddress(targetHardwareAddress);
        arp.setTargetProtocolAddress(targetProtocolAddress);
                
        arp.setParent(eth);
        eth.setPayload(arp);
        
        rawPkt = dService.encodeDataPacket(eth);
        data = rawPkt.getPacketData();
        
        Assert.assertTrue(data[0] == (byte)0x0A);//Destination MAC
        Assert.assertTrue(data[1] == (byte)0x0C);
        Assert.assertTrue(data[2] == (byte)0x0E);
        Assert.assertTrue(data[3] == (byte)0x14);
        Assert.assertTrue(data[4] == (byte)0x37);
        Assert.assertTrue(data[5] == (byte)0x45);
        Assert.assertTrue(data[6] == (byte)0x52);//Source MAC
        Assert.assertTrue(data[7] == (byte)0x61);
        Assert.assertTrue(data[8] == (byte)0x6D);
        Assert.assertTrue(data[9] == (byte)0x75);
        Assert.assertTrue(data[10] == (byte)0x7F);
        Assert.assertTrue(data[11] == (byte)0xCE);
        Assert.assertTrue(data[12] == (byte)0x08);//EtherType
        Assert.assertTrue(data[13] == (byte)0x06);
        Assert.assertTrue(data[14] == (byte)0x00);//Hardware Type
        Assert.assertTrue(data[15] == (byte)0x01);
        Assert.assertTrue(data[16] == (byte)0x08);//Protocol Type
        Assert.assertTrue(data[17] == (byte)0x0);
        Assert.assertTrue(data[18] == (byte)0x6);//Hardware Address Length
        Assert.assertTrue(data[19] == (byte)0x4);//Protocol Address Length
        Assert.assertTrue(data[20] == (byte)0x0);//Opcode
        Assert.assertTrue(data[21] == (byte)0x1);//Opcode
        Assert.assertTrue(data[22] == (byte)0xA6);//Sender Hardware Address
        Assert.assertTrue(data[23] == (byte)0xEC);
        Assert.assertTrue(data[24] == (byte)0x9C);
        Assert.assertTrue(data[25] == (byte)0xAE);
        Assert.assertTrue(data[26] == (byte)0xB2);
        Assert.assertTrue(data[27] == (byte)0x9F);
        Assert.assertTrue(data[28] == (byte)0x09);//Sender Protocol Address
        Assert.assertTrue(data[29] == (byte)0x09);
        Assert.assertTrue(data[30] == (byte)0x09);
        Assert.assertTrue(data[31] == (byte)0x01);//Target Hardware Address
        Assert.assertTrue(data[32] == (byte)0x00);
        Assert.assertTrue(data[33] == (byte)0x00);
        Assert.assertTrue(data[34] == (byte)0x00);
        Assert.assertTrue(data[35] == (byte)0x00);
        Assert.assertTrue(data[36] == (byte)0x00);
        Assert.assertTrue(data[37] == (byte)0x00);
        Assert.assertTrue(data[38] == (byte)0x09);//Target Protocol Address
        Assert.assertTrue(data[39] == (byte)0x09);
        Assert.assertTrue(data[40] == (byte)0x09);
        Assert.assertTrue(data[41] == (byte)0xFE);    
	}

}
