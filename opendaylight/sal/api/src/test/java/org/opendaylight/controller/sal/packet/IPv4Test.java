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

public class IPv4Test {

    @Test
    public void testGetVersion() {
        IPv4 ip = new IPv4();
        byte[] ipVersion = { (byte) 4 };
        ip.hdrFieldsMap.put("Version", ipVersion);
        byte version = ip.getVersion();
        Assert.assertTrue(version == (byte) 4);
    }

    @Test
    public void testGetHeaderLength() {
        IPv4 ip = new IPv4();
        byte[] ipHeaderLength = { 5 };
        ip.hdrFieldsMap.put("HeaderLength", ipHeaderLength);
        byte headerLength = (byte) ip.getHeaderLen();
        Assert.assertTrue(headerLength == 20);
    }

    @Test
    public void testGetDiffServ() {
        IPv4 ip = new IPv4();
        byte[] ipDiffServ = { 20 };
        ip.hdrFieldsMap.put("DiffServ", ipDiffServ);
        byte diffServ = ip.getDiffServ();
        Assert.assertTrue(diffServ == 20);
    }

    @Test
    public void testGetTotalLength() {
        IPv4 ip = new IPv4();
        byte[] iptotLength = { 3, -24 };
        ip.hdrFieldsMap.put("TotalLength", iptotLength);
        short totalLength = ip.getTotalLength();
        Assert.assertTrue(totalLength == 1000);
    }

    @Test
    public void testGetIdentification() {
        IPv4 ip = new IPv4();
        byte[] ipIdentification = { 7, -48 };
        ip.hdrFieldsMap.put("Identification", ipIdentification);
        short identification = ip.getIdentification();
        Assert.assertTrue(identification == 2000);
    }

    @Test
    public void testGetFlags() {
        IPv4 ip = new IPv4();
        byte[] ipFlags = { 7 };
        ip.hdrFieldsMap.put("Flags", ipFlags);
        byte flags = ip.getFlags();
        Assert.assertTrue(flags == 7);
    }

    @Test
    public void testGetTtl() {
        IPv4 ip = new IPv4();
        byte[] ipTtl = { 100 };
        ip.hdrFieldsMap.put("TTL", ipTtl);
        byte ttl = ip.getTtl();
        Assert.assertTrue(ttl == 100);
    }

    @Test
    public void testGetProtocol() {
        IPv4 ip = new IPv4();
        byte[] ipProtocol = { 1 };
        ip.hdrFieldsMap.put("Protocol", ipProtocol);
        byte protocol = ip.getProtocol();
        Assert.assertTrue(protocol == 1);

        Class<? extends Packet> clazz = IPv4.protocolClassMap.get(protocol);
        Assert.assertTrue(clazz == ICMP.class);
    }

    @Test
    public void testGetFragmentOffset() {
        IPv4 ip = new IPv4();
        byte[] ipFragmentOffset = { 6, -35 };
        ip.hdrFieldsMap.put("FragmentOffset", ipFragmentOffset);
        short fragmentOffset = ip.getFragmentOffset();
        Assert.assertTrue(fragmentOffset == 1757);
    }

    @Test
    public void testGetSourceAddress() {
        IPv4 ip = new IPv4();
        byte[] ipSourceAddress = { 10, 110, 31, 55 };
        ip.hdrFieldsMap.put("SourceIPAddress", ipSourceAddress);
        int sourceAddress = ip.getSourceAddress();
        Assert.assertTrue(sourceAddress == 174989111);
    }

    @Test
    public void testGetDestinationAddress() {
        IPv4 ip = new IPv4();
        byte[] ipDestinationAddress = { 20, 55, 62, 110 };
        ip.hdrFieldsMap.put("DestinationIPAddress", ipDestinationAddress);
        int destinationAddress = ip.getDestinationAddress();
        Assert.assertTrue(destinationAddress == 339164782);
    }

    @Test
    public void testSetVersion() {
        IPv4 ip = new IPv4();
        byte ipVersion = (byte) 4;
        ip.setVersion(ipVersion);
        byte[] version = ip.hdrFieldsMap.get("Version");
        Assert.assertTrue(version[0] == (byte) 4);
    }

    @Test
    public void testSetHeaderLength() {
        IPv4 ip = new IPv4();
        byte ipHeaderLength = 5;
        ip.setHeaderLength(ipHeaderLength);
        byte[] headerLength = ip.hdrFieldsMap.get("HeaderLength");
        Assert.assertTrue(headerLength[0] == 5);
    }

    @Test
    public void testSetDiffServ() {
        IPv4 ip = new IPv4();
        byte ipDiffServ = 20;
        ip.setDiffServ(ipDiffServ);
        byte[] diffServ = ip.hdrFieldsMap.get("DiffServ");
        Assert.assertTrue(diffServ[0] == 20);
    }

    @Test
    public void testSetTotalLength() {
        IPv4 ip = new IPv4();
        short iptotLength = 1000;
        ip.setTotalLength(iptotLength);
        byte[] totalLength = ip.hdrFieldsMap.get("TotalLength");
        Assert.assertTrue(totalLength[0] == 3);
        Assert.assertTrue(totalLength[1] == -24);
    }

    @Test
    public void testSetIdentification() {
        IPv4 ip = new IPv4();
        short ipIdentification = 2000;
        ip.setIdentification(ipIdentification);
        byte[] identification = ip.hdrFieldsMap.get("Identification");
        Assert.assertTrue(identification[0] == 7);
        Assert.assertTrue(identification[1] == -48);
    }

    @Test
    public void testSetFlags() {
        IPv4 ip = new IPv4();
        byte ipFlags = 7;
        ip.setFlags(ipFlags);
        byte[] flags = ip.hdrFieldsMap.get("Flags");
        Assert.assertTrue(flags[0] == 7);
    }

    @Test
    public void testSetTtl() {
        IPv4 ip = new IPv4();
        byte ipTtl = 100;
        ip.setTtl(ipTtl);
        byte[] ttl = ip.hdrFieldsMap.get("TTL");
        Assert.assertTrue(ttl[0] == 100);
    }

    @Test
    public void testSetProtocol() {
        IPv4 ip = new IPv4();
        byte ipProtocol = 11;
        ip.setProtocol(ipProtocol);
        byte[] protocol = ip.hdrFieldsMap.get("Protocol");
        Assert.assertTrue(protocol[0] == 11);
    }

    @Test
    public void testSetFragmentOffset() {
        IPv4 ip = new IPv4();
        short ipFragmentOffset = 1757;
        ip.setFragmentOffset(ipFragmentOffset);
        byte[] fragmentOffset = ip.hdrFieldsMap.get("FragmentOffset");
        Assert.assertTrue(fragmentOffset[0] == 6);
        Assert.assertTrue(fragmentOffset[1] == -35);
    }

    @Test
    public void testSetDestinationAddress() {
        IPv4 ip = new IPv4();
        int ipDestinationAddress = 339164782;
        ip.setDestinationAddress(ipDestinationAddress);
        byte[] destinationAddress = ip.hdrFieldsMap.get("DestinationIPAddress");
        Assert.assertTrue(destinationAddress[0] == 20);
        Assert.assertTrue(destinationAddress[1] == 55);
        Assert.assertTrue(destinationAddress[2] == 62);
        Assert.assertTrue(destinationAddress[3] == 110);
    }

    @Test
    public void testChecksum() {
        byte header[] = { (byte) 0x45, 00, 00, (byte) 0x3c, (byte) 0x1c,
                (byte) 0x46, (byte) 0x40, 00, (byte) 0x40, 06, (byte) 0xb1,
                (byte) 0xe6, (byte) 0xac, (byte) 0x10, (byte) 0x0a,
                (byte) 0x63, (byte) 0xac, (byte) 0x10, (byte) 0x0a, (byte) 0x0c };
        byte header2[] = { (byte) 0x45, 00, 00, (byte) 0x73, 00, 00,
                (byte) 0x40, 00, (byte) 0x40, (byte) 0x11, (byte) 0xb8,
                (byte) 0x61, (byte) 0xc0, (byte) 0xa8, 00, 01, (byte) 0xc0,
                (byte) 0xa8, 00, (byte) 0xc7 };
        byte header3[] = { (byte) 0x45, 00, 00, (byte) 0x47, (byte) 0x73,
                (byte) 0x88, (byte) 0x40, 00, (byte) 0x40, 06, (byte) 0xA2,
                (byte) 0xC4, (byte) 0x83, (byte) 0x9F, (byte) 0x0E,
                (byte) 0x85, (byte) 0x83, (byte) 0x9F, (byte) 0x0E, (byte) 0xA1 };
        byte header4[] = { (byte) 0x45, 00, 00, (byte) 0x54, 00, 00,
                (byte) 0x40, 00, (byte) 0x40, 01, (byte) 0xf0, (byte) 0x8e,
                (byte) 0xc0, (byte) 0xa8, (byte) 0x64, (byte) 0x65,
                (byte) 0xc0, (byte) 0xa8, (byte) 0x64, (byte) 0x64 };
        byte header5[] = { (byte) 0x45, 00, 00, (byte) 0x54, 00, 00,
                (byte) 0x40, 00, (byte) 0x40, 01, (byte) 0xef, (byte) 0x8d,
                (byte) 0xc0, (byte) 0xa8, (byte) 0x64, (byte) 0x65,
                (byte) 0xc0, (byte) 0xa8, (byte) 0x65, (byte) 0x65 };
        byte header6[] = { (byte) 0x45, 00, 00, (byte) 0x54, 00, 00,
                (byte) 0x40, 00, (byte) 0x40, 01, (byte) 0x0b, (byte) 0x92,
                (byte) 0xc0, (byte) 0xa8, (byte) 0x64, (byte) 0x65, (byte) 0x9,
                (byte) 0x9, (byte) 0x1, (byte) 0x1 };
        byte header7[] = { (byte) 0x45, 00, 00, (byte) 0x54, 00, 00,
                (byte) 0x40, 00, (byte) 0x40, 01, (byte) 0, (byte) 0,
                (byte) 0xc0, (byte) 0xa8, (byte) 0x64, (byte) 0x65, (byte) 0x9,
                (byte) 0x9, (byte) 0x2, (byte) 0x2 };

        IPv4 ip = new IPv4();

        Assert.assertTrue(NetUtils.getUnsignedShort(ip.computeChecksum(header,
                header.length)) == 0xB1E6);
        Assert.assertTrue(NetUtils.getUnsignedShort(ip.computeChecksum(header2,
                header.length)) == 0xb861);
        Assert.assertTrue(NetUtils.getUnsignedShort(ip.computeChecksum(header3,
                header.length)) == 0xa2c4);
        Assert.assertTrue(NetUtils.getUnsignedShort(ip.computeChecksum(header4,
                header.length)) == 0xf08e);
        Assert.assertTrue(NetUtils.getUnsignedShort(ip.computeChecksum(header5,
                header.length)) == 0xef8d);
        Assert.assertTrue(NetUtils.getUnsignedShort(ip.computeChecksum(header6,
                header.length)) == 0x0b92);
        Assert.assertTrue(NetUtils.getUnsignedShort(ip.computeChecksum(header7,
                header.length)) == 0x0a91);
    }

}
