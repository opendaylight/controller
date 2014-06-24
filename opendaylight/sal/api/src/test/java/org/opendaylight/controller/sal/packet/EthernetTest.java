
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.packet;

import java.net.InetAddress;
import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.IPProtocols;

public class EthernetTest {

    @Test
    public void testGetDestinationMACAddress() {
        Ethernet eth = new Ethernet();
        byte mac[] = { 10, 12, 14, 20, 55, 69 };
        eth.hdrFieldsMap.put("DestinationMACAddress", mac);
        byte[] dMAC = eth.getDestinationMACAddress();
        Assert.assertTrue(dMAC[0] == 10);
        Assert.assertTrue(dMAC[1] == 12);
        Assert.assertTrue(dMAC[2] == 14);
        Assert.assertTrue(dMAC[3] == 20);
        Assert.assertTrue(dMAC[4] == 55);
        Assert.assertTrue(dMAC[5] == 69);

    }

    @Test
    public void testSourceMACAddress() {
        Ethernet eth = new Ethernet();
        byte mac[] = { 120, 30, 25, 80, 66, 99 };
        eth.hdrFieldsMap.put("SourceMACAddress", mac);
        byte[] sMAC = eth.getSourceMACAddress();
        Assert.assertTrue(sMAC[0] == 120);
        Assert.assertTrue(sMAC[1] == 30);
        Assert.assertTrue(sMAC[2] == 25);
        Assert.assertTrue(sMAC[3] == 80);
        Assert.assertTrue(sMAC[4] == 66);
        Assert.assertTrue(sMAC[5] == 99);

    }

    @Test
    public void testGetEthertype() throws Exception {
        Ethernet eth = new Ethernet();
        byte ethType[] = { 8, 6 };
        eth.hdrFieldsMap.put("EtherType", ethType);
        short etherType = eth.getEtherType();
        Assert.assertTrue(etherType == 2054);
    }

    @Test
    public void testSetDestinationMACAddress() {
        Ethernet eth = new Ethernet();
        byte mac[] = { 10, 12, 14, 20, 55, 69 };
        eth.setDestinationMACAddress(mac);
        byte[] dMAC = eth.hdrFieldsMap.get("DestinationMACAddress");
        Assert.assertTrue(dMAC[0] == 10);
        Assert.assertTrue(dMAC[1] == 12);
        Assert.assertTrue(dMAC[2] == 14);
        Assert.assertTrue(dMAC[3] == 20);
        Assert.assertTrue(dMAC[4] == 55);
        Assert.assertTrue(dMAC[5] == 69);

    }

    @Test
    public void testSetSourceMACAddress() {
        Ethernet eth = new Ethernet();
        byte mac[] = { 120, 30, 25, 80, 66, 99 };
        eth.setSourceMACAddress(mac);
        byte[] sMAC = eth.hdrFieldsMap.get("SourceMACAddress");
        Assert.assertTrue(sMAC[0] == 120);
        Assert.assertTrue(sMAC[1] == 30);
        Assert.assertTrue(sMAC[2] == 25);
        Assert.assertTrue(sMAC[3] == 80);
        Assert.assertTrue(sMAC[4] == 66);
        Assert.assertTrue(sMAC[5] == 99);

    }

    @Test
    public void testSetEthertype() throws Exception {
        Ethernet eth = new Ethernet();
        short ethType = 2054;
        eth.setEtherType(ethType);
        byte[] etherType = eth.hdrFieldsMap.get("EtherType");
        Assert.assertTrue(etherType[0] == 8);
        Assert.assertTrue(etherType[1] == 6);

    }

    @Test
    public void testGetMatch() throws Exception {
        Ethernet eth = new Ethernet();
        byte smac[] = { (byte) 0xf0, (byte) 0xde, (byte) 0xf1, (byte) 0x71, (byte) 0x72, (byte) 0x8d };
        byte dmac[] = { (byte) 0xde, (byte) 0x28, (byte) 0xdb, (byte) 0xb3, (byte) 0x7c, (byte) 0xf8 };
        short ethType = EtherTypes.IPv4.shortValue();
        eth.setDestinationMACAddress(dmac);
        eth.setSourceMACAddress(smac);
        eth.setEtherType(ethType);

        Match match = eth.getMatch();

        Assert.assertTrue(Arrays.equals(smac, (byte[]) match.getField(MatchType.DL_SRC).getValue()));
        Assert.assertTrue(Arrays.equals(dmac, (byte[]) match.getField(MatchType.DL_DST).getValue()));
        Assert.assertEquals(ethType, (short) match.getField(MatchType.DL_TYPE).getValue());

    }

    @Test
    public void testGetMatchFullPacket() throws Exception {
        TCP tcp = new TCP();
        short sport = (short) 11093;
        short dport = (short) 23;
        tcp.setSourcePort(sport);
        tcp.setDestinationPort(dport);

        IPv4 ip = new IPv4();
        InetAddress sourceAddress = InetAddress.getByName("192.168.100.100");
        InetAddress destintationAddress = InetAddress.getByName("192.168.100.101");
        byte protocol = IPProtocols.TCP.byteValue();
        byte tos = 5;
        ip.setVersion((byte) 4);
        ip.setIdentification((short) 5);
        ip.setDiffServ(tos);
        ip.setECN((byte) 0);
        ip.setTotalLength((short) 84);
        ip.setFlags((byte) 2);
        ip.setFragmentOffset((short) 0);
        ip.setTtl((byte) 64);
        ip.setProtocol(protocol);
        ip.setDestinationAddress(destintationAddress);
        ip.setSourceAddress(sourceAddress);
        ip.setPayload(tcp);

        IEEE8021Q dot1q = new IEEE8021Q();
        byte priority = 4;
        short vlanId = 59;
        short ethType = EtherTypes.IPv4.shortValue();
        dot1q.setPcp(priority);
        dot1q.setVid(vlanId);
        dot1q.setEtherType(ethType);
        dot1q.setPayload(ip);

        Ethernet eth = new Ethernet();
        byte smac[] = { (byte) 0xf0, (byte) 0xde, (byte) 0xf1, (byte) 0x71, (byte) 0x72, (byte) 0x8d };
        byte dmac[] = { (byte) 0xde, (byte) 0x28, (byte) 0xdb, (byte) 0xb3, (byte) 0x7c, (byte) 0xf8 };
        eth.setDestinationMACAddress(dmac);
        eth.setSourceMACAddress(smac);
        eth.setEtherType(EtherTypes.VLANTAGGED.shortValue());
        eth.setPayload(dot1q);

        Match match = eth.getMatch();

        Assert.assertTrue(Arrays.equals(smac, (byte[]) match.getField(MatchType.DL_SRC).getValue()));
        Assert.assertTrue(Arrays.equals(dmac, (byte[]) match.getField(MatchType.DL_DST).getValue()));
        Assert.assertEquals(priority, (byte) match.getField(MatchType.DL_VLAN_PR).getValue());
        Assert.assertEquals(vlanId, (short) match.getField(MatchType.DL_VLAN).getValue());
        Assert.assertEquals(ethType, (short) match.getField(MatchType.DL_TYPE).getValue());
        Assert.assertEquals(sourceAddress, match.getField(MatchType.NW_SRC).getValue());
        Assert.assertEquals(destintationAddress, match.getField(MatchType.NW_DST).getValue());
        Assert.assertEquals(protocol, (byte) match.getField(MatchType.NW_PROTO).getValue());
        Assert.assertEquals(tos, (byte) match.getField(MatchType.NW_TOS).getValue());
        Assert.assertEquals(sport, (short) match.getField(MatchType.TP_SRC).getValue());
        Assert.assertEquals(dport, (short) match.getField(MatchType.TP_DST).getValue());
    }
}
