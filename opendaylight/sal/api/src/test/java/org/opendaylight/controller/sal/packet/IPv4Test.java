/*
 * Copyright (c) 2013-2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.packet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.IPProtocols;
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

        ip.setTotalLength((short)84);
        totalLength = ip.hdrFieldsMap.get("TotalLength");
        Assert.assertTrue(totalLength[0] == 0);
        Assert.assertTrue(totalLength[1] == 84);
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
    public void testOptions() throws Exception {
        IPv4 ip = new IPv4();
        Assert.assertEquals(20, ip.getHeaderLen());
        Assert.assertEquals(160, ip.getHeaderSize());
        Assert.assertEquals(0, ip.getfieldnumBits("Options"));

        byte[][] options = {
            new byte[] {
                (byte)0x01,
            },
            new byte[] {
                (byte)0x01, (byte)0x02,
            },
            new byte[] {
                (byte)0x01, (byte)0x02, (byte)0x03,
            },
            new byte[] {
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
            },
            null,
            new byte[] {
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
                (byte)0x05,
            },
            new byte[] {
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
                (byte)0x05, (byte)0x06,
            },
            new byte[] {
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
                (byte)0x05, (byte)0x06, (byte)0x07,
            },
            new byte[] {
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
                (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08,
            },
            new byte[0],
        };

        byte[][] expected = {
            new byte[] {
                (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
            },
            new byte[] {
                (byte)0x01, (byte)0x02, (byte)0x00, (byte)0x00,
            },
            new byte[] {
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x00,
            },
            new byte[] {
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
            },
            null,
            new byte[] {
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
                (byte)0x05, (byte)0x00, (byte)0x00, (byte)0x00,
            },
            new byte[] {
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
                (byte)0x05, (byte)0x06, (byte)0x00, (byte)0x00,
            },
            new byte[] {
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
                (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x00,
            },
            new byte[] {
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
                (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08,
            },
            null,
        };

        byte[] echo = {
            (byte)0x11, (byte)0x22, (byte)0x33, (byte)0x44,
            (byte)0x55, (byte)0x66, (byte)0x77, (byte)0x88,
            (byte)0x99, (byte)0xaa,
        };
        ICMP icmp = new ICMP();
        icmp.setType((byte)8);
        icmp.setCode((byte)0);
        icmp.setIdentifier((short)0xabcd);
        icmp.setSequenceNumber((short)7777);
        icmp.setRawPayload(echo);

        ip.setSourceAddress(InetAddress.getByName("192.168.10.20"));
        ip.setDestinationAddress(InetAddress.getByName("192.168.30.40"));
        ip.setProtocol(IPProtocols.ICMP.byteValue());

        for (int i = 0; i < options.length; i++) {
            byte[] opts = options[i];
            byte[] exp = expected[i];

            // Set IPv4 options.
            int hlen = 20;
            int optlen;
            if (exp != null) {
                optlen = exp.length;
                hlen += optlen;
            } else {
                optlen = 0;
            }
            ip.setOptions(opts);
            Assert.assertTrue(Arrays.equals(exp, ip.getOptions()));
            Assert.assertEquals(hlen, ip.getHeaderLen());
            Assert.assertEquals(hlen * 8, ip.getHeaderSize());
            Assert.assertEquals(optlen * 8, ip.getfieldnumBits("Options"));

            // Serialize/Deserialize test.
            ip.setPayload(icmp);

            byte[] raw = ip.serialize();
            IPv4 newip = new IPv4();
            newip.deserialize(raw, 0, raw.length * 8);
            Assert.assertEquals(ip, newip);
            Assert.assertEquals(icmp, newip.getPayload());
            Assert.assertTrue(Arrays.equals(exp, newip.getOptions()));
        }
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
                0)) == 0xB1E6);
        Assert.assertTrue(NetUtils.getUnsignedShort(ip.computeChecksum(header2,
                0)) == 0xb861);
        Assert.assertTrue(NetUtils.getUnsignedShort(ip.computeChecksum(header3,
                0)) == 0xa2c4);
        Assert.assertTrue(NetUtils.getUnsignedShort(ip.computeChecksum(header4,
                0)) == 0xf08e);
        Assert.assertTrue(NetUtils.getUnsignedShort(ip.computeChecksum(header5,
                0)) == 0xef8d);
        Assert.assertTrue(NetUtils.getUnsignedShort(ip.computeChecksum(header6,
                0)) == 0x0b92);
        Assert.assertTrue(NetUtils.getUnsignedShort(ip.computeChecksum(header7,
                0)) == 0x0a91);
    }

    @Test
    public void testFullIP() throws UnknownHostException, PacketException {
        byte[] icmpRawPayload = new byte[] { (byte) 0x38, (byte) 0x26,
                (byte) 0x9e, (byte) 0x51, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x2e, (byte) 0x6a,
                (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x11,
                (byte) 0x12, (byte) 0x13, (byte) 0x14, (byte) 0x15,
                (byte) 0x16, (byte) 0x17, (byte) 0x18, (byte) 0x19,
                (byte) 0x1a, (byte) 0x1b, (byte) 0x1c, (byte) 0x1d,
                (byte) 0x1e, (byte) 0x1f, (byte) 0x20, (byte) 0x21,
                (byte) 0x22, (byte) 0x23, (byte) 0x24, (byte) 0x25,
                (byte) 0x26, (byte) 0x27, (byte) 0x28, (byte) 0x29,
                (byte) 0x2a, (byte) 0x2b, (byte) 0x2c, (byte) 0x2d,
                (byte) 0x2e, (byte) 0x2f, (byte) 0x30, (byte) 0x31,
                (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35,
                (byte) 0x36, (byte) 0x37 };
        ICMP icmp = new ICMP();
        icmp.setType((byte) 8);
        icmp.setCode((byte) 0);
        icmp.setIdentifier((short) 0x46f5);
        icmp.setSequenceNumber((short) 2);
        icmp.setRawPayload(icmpRawPayload);

        IPv4 ip = new IPv4();
        ip.setVersion((byte) 4);
        ip.setIdentification((short) 5);
        ip.setDiffServ((byte) 0);
        ip.setECN((byte) 0);
        ip.setTotalLength((short) 84);
        ip.setFlags((byte) 2);
        ip.setFragmentOffset((short) 0);
        ip.setTtl((byte) 64);
        ip.setProtocol(IPProtocols.ICMP.byteValue());
        ip.setDestinationAddress(InetAddress.getByName("192.168.100.100"));
        ip.setSourceAddress(InetAddress.getByName("192.168.100.101"));
        ip.setPayload(icmp);

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(new byte[] { (byte) 0x98, (byte) 0xfc,
                (byte) 0x11, (byte) 0x93, (byte) 0x5c, (byte) 0xb8 });
        eth.setSourceMACAddress(new byte[] { (byte) 0x00, (byte) 0x24,
                (byte) 0xd7, (byte) 0xa9, (byte) 0xa3, (byte) 0x50 });
        eth.setEtherType(EtherTypes.IPv4.shortValue());
        eth.setPayload(ip);

        byte[] stream = eth.serialize();

        Ethernet decEth = new Ethernet();
        decEth.deserialize(stream, 0, stream.length * NetUtils.NumBitsInAByte);

        IPv4 decIp = (IPv4) decEth.getPayload();
        Assert.assertFalse(decIp.isCorrupted());
        Assert.assertTrue(ip.equals(decIp));

        ICMP decIcmp = (ICMP) decIp.getPayload();
        Assert.assertFalse(decIcmp.isCorrupted());
        Assert.assertTrue(Arrays.equals(icmpRawPayload, decIcmp.getRawPayload()));
    }

    @Test
    public void testGetMatch() throws Exception {
        IPv4 ip = new IPv4();
        InetAddress sourceAddress = InetAddress.getByName("172.168.190.15");
        InetAddress destintationAddress = InetAddress.getByName("23.128.0.11");
        byte protocol = IPProtocols.TCP.byteValue();
        byte tos = 7;
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

        Match match = ip.getMatch();

        Assert.assertEquals(sourceAddress, match.getField(MatchType.NW_SRC).getValue());
        Assert.assertEquals(destintationAddress, match.getField(MatchType.NW_DST).getValue());
        Assert.assertEquals(protocol, (byte) match.getField(MatchType.NW_PROTO).getValue());
        Assert.assertEquals(tos, (byte) match.getField(MatchType.NW_TOS).getValue());
    }

    @Test
    public void testFragment() throws Exception {
        byte[] payload1 = new byte[0];
        byte[] payload2 = {
            (byte)0x61, (byte)0xd1, (byte)0x3d, (byte)0x51,
            (byte)0x1b, (byte)0x75, (byte)0xa7, (byte)0x83,
        };
        byte[] payload3 = {
            (byte)0xe7, (byte)0x0f, (byte)0x2d, (byte)0x7e,
            (byte)0x15, (byte)0xba, (byte)0xe7, (byte)0x6d,
            (byte)0xb5, (byte)0xc5, (byte)0xb5, (byte)0x37,
            (byte)0x59, (byte)0xbc, (byte)0x91, (byte)0x43,
            (byte)0xb5, (byte)0xb7, (byte)0xe4, (byte)0x28,
            (byte)0xec, (byte)0x62, (byte)0x6b, (byte)0x6a,
            (byte)0xd1, (byte)0xcb, (byte)0x79, (byte)0x1e,
            (byte)0xfc, (byte)0x82, (byte)0xf5, (byte)0xb4,
        };

        // Ensure that the payload is not deserialized if the fragment offset
        // is not zero.
        byte proto = IPProtocols.TCP.byteValue();
        fragmentTest(payload1, proto, (short)0xf250);
        fragmentTest(payload2, proto, (short)0xf248);
        fragmentTest(payload3, proto, (short)0xf230);

        proto = IPProtocols.UDP.byteValue();
        fragmentTest(payload1, proto, (short)0xf245);
        fragmentTest(payload2, proto, (short)0xf23d);
        fragmentTest(payload3, proto, (short)0xf225);

        proto = IPProtocols.ICMP.byteValue();
        fragmentTest(payload1, proto, (short)0xf255);
        fragmentTest(payload2, proto, (short)0xf24d);
        fragmentTest(payload3, proto, (short)0xf235);

        // Ensure that the protocol header in the first fragment is
        // deserialized.
        proto = IPProtocols.TCP.byteValue();
        TCP tcp = new TCP();
        tcp.setSourcePort((short)1234).setDestinationPort((short)32000).
            setSequenceNumber((int)0xd541f5f8).setAckNumber((int)0x58da787d).
            setDataOffset((byte)5).setReserved((byte)0).
            setHeaderLenFlags((short)0x18).setWindowSize((short)0x40e8).
            setUrgentPointer((short)0x15f7).setChecksum((short)0x0d4e);
        firstFragmentTest(tcp, payload1, proto, (short)0xdfe6);
        tcp.setChecksum((short)0xab2a);
        firstFragmentTest(tcp, payload2, proto, (short)0xdfde);
        tcp.setChecksum((short)0x1c75);
        firstFragmentTest(tcp, payload3, proto, (short)0xdfc6);

        proto = IPProtocols.UDP.byteValue();
        UDP udp = new UDP();
        udp.setSourcePort((short)53).setDestinationPort((short)45383).
            setLength((short)(payload1.length + 8)).setChecksum((short)0);
        firstFragmentTest(udp, payload1, proto, (short)0xdfe7);
        udp.setLength((short)(payload2.length + 8));
        firstFragmentTest(udp, payload2, proto, (short)0xdfdf);
        udp.setLength((short)(payload3.length + 8));
        firstFragmentTest(udp, payload3, proto, (short)0xdfc7);

        proto = IPProtocols.ICMP.byteValue();
        ICMP icmp = new ICMP();
        icmp.setType((byte)8).setCode((byte)0).setIdentifier((short)0x3d1e).
            setSequenceNumber((short)1);
        firstFragmentTest(icmp, payload1, proto, (short)0xdff7);
        firstFragmentTest(icmp, payload2, proto, (short)0xdfef);
        firstFragmentTest(icmp, payload3, proto, (short)0xdfd7);
    }

    private void fragmentTest(byte[] payload, byte proto, short checksum)
        throws Exception {
        // Construct a fragmented raw IPv4 packet.
        int ipv4Len = 20;
        byte[] rawIp = new byte[ipv4Len + payload.length];

        byte ipVersion = 4;
        byte dscp = 35;
        byte ecn = 2;
        byte tos = (byte)((dscp << 2) | ecn);
        short totalLen = (short)rawIp.length;
        short id = 22143;
        short offset = 0xb9;
        byte ttl = 64;
        byte[] srcIp = {(byte)0x0a, (byte)0x00, (byte)0x00, (byte)0x01};
        byte[] dstIp = {(byte)0xc0, (byte)0xa9, (byte)0x66, (byte)0x23};

        rawIp[0] = (byte)((ipVersion << 4) | (ipv4Len >> 2));
        rawIp[1] = tos;
        rawIp[2] = (byte)(totalLen >>> Byte.SIZE);
        rawIp[3] = (byte)totalLen;
        rawIp[4] = (byte)(id >>> Byte.SIZE);
        rawIp[5] = (byte)id;
        rawIp[6] = (byte)(offset >>> Byte.SIZE);
        rawIp[7] = (byte)offset;
        rawIp[8] = ttl;
        rawIp[9] = proto;
        rawIp[10] = (byte)(checksum >>> Byte.SIZE);
        rawIp[11] = (byte)checksum;
        System.arraycopy(srcIp, 0, rawIp, 12, srcIp.length);
        System.arraycopy(dstIp, 0, rawIp, 16, srcIp.length);
        System.arraycopy(payload, 0, rawIp, ipv4Len, payload.length);

        // Deserialize.
        IPv4 ipv4 = new IPv4();
        ipv4.deserialize(rawIp, 0, rawIp.length * Byte.SIZE);

        Assert.assertEquals(ipVersion, ipv4.getVersion());
        Assert.assertEquals(ipv4Len, ipv4.getHeaderLen());
        Assert.assertEquals(dscp, ipv4.getDiffServ());
        Assert.assertEquals(ecn, ipv4.getECN());
        Assert.assertEquals(totalLen, ipv4.getTotalLength());
        Assert.assertEquals(id, ipv4.getIdentification());
        Assert.assertEquals((byte)0, ipv4.getFlags());
        Assert.assertEquals(offset, ipv4.getFragmentOffset());
        Assert.assertEquals(ttl, ipv4.getTtl());
        Assert.assertEquals(proto, ipv4.getProtocol());
        Assert.assertEquals(checksum, ipv4.getChecksum());
        Assert.assertEquals(NetUtils.byteArray4ToInt(srcIp),
                            ipv4.getSourceAddress());
        Assert.assertEquals(NetUtils.byteArray4ToInt(dstIp),
                            ipv4.getDestinationAddress());
        Assert.assertFalse(ipv4.isCorrupted());

        // payloadClass should not be set if fragment offset is not zero.
        Assert.assertEquals(null, ipv4.getPayload());
        Assert.assertArrayEquals(payload, ipv4.getRawPayload());
    }

    private void firstFragmentTest(Packet payload, byte[] rawPayload,
                                   byte proto, short checksum)
        throws Exception {
        // Construct a raw IPv4 packet with MF flag.
        int ipv4Len = 20;
        payload.setRawPayload(rawPayload);
        byte[] payloadBytes = payload.serialize();
        byte[] rawIp = new byte[ipv4Len + payloadBytes.length];

        byte ipVersion = 4;
        byte dscp = 13;
        byte ecn = 1;
        byte tos = (byte)((dscp << 2) | ecn);
        short totalLen = (short)rawIp.length;
        short id = 19834;
        byte flags = 0x1;
        short offset = 0;
        short off = (short)(((short)flags << 13) | offset);
        byte ttl = 64;
        byte[] srcIp = {(byte)0xac, (byte)0x23, (byte)0x5b, (byte)0xfd};
        byte[] dstIp = {(byte)0xc0, (byte)0xa8, (byte)0x64, (byte)0x71};

        rawIp[0] = (byte)((ipVersion << 4) | (ipv4Len >> 2));
        rawIp[1] = tos;
        rawIp[2] = (byte)(totalLen >>> Byte.SIZE);
        rawIp[3] = (byte)totalLen;
        rawIp[4] = (byte)(id >>> Byte.SIZE);
        rawIp[5] = (byte)id;
        rawIp[6] = (byte)(off >>> Byte.SIZE);
        rawIp[7] = (byte)off;
        rawIp[8] = ttl;
        rawIp[9] = proto;
        rawIp[10] = (byte)(checksum >>> Byte.SIZE);
        rawIp[11] = (byte)checksum;
        System.arraycopy(srcIp, 0, rawIp, 12, srcIp.length);
        System.arraycopy(dstIp, 0, rawIp, 16, srcIp.length);
        System.arraycopy(payloadBytes, 0, rawIp, ipv4Len, payloadBytes.length);

        // Deserialize.
        IPv4 ipv4 = new IPv4();
        ipv4.deserialize(rawIp, 0, rawIp.length * Byte.SIZE);

        Assert.assertEquals(ipVersion, ipv4.getVersion());
        Assert.assertEquals(ipv4Len, ipv4.getHeaderLen());
        Assert.assertEquals(dscp, ipv4.getDiffServ());
        Assert.assertEquals(ecn, ipv4.getECN());
        Assert.assertEquals(totalLen, ipv4.getTotalLength());
        Assert.assertEquals(id, ipv4.getIdentification());
        Assert.assertEquals(flags, ipv4.getFlags());
        Assert.assertEquals(offset, ipv4.getFragmentOffset());
        Assert.assertEquals(ttl, ipv4.getTtl());
        Assert.assertEquals(proto, ipv4.getProtocol());
        Assert.assertEquals(checksum, ipv4.getChecksum());
        Assert.assertEquals(NetUtils.byteArray4ToInt(srcIp),
                            ipv4.getSourceAddress());
        Assert.assertEquals(NetUtils.byteArray4ToInt(dstIp),
                            ipv4.getDestinationAddress());
        Assert.assertFalse(ipv4.isCorrupted());

        // Protocol header in the first fragment should be deserialized.
        Assert.assertEquals(null, ipv4.getRawPayload());

        Packet desPayload = ipv4.getPayload();
        Assert.assertEquals(payload, desPayload);
        Assert.assertFalse(desPayload.isCorrupted());
        Assert.assertArrayEquals(rawPayload, desPayload.getRawPayload());
    }
}
