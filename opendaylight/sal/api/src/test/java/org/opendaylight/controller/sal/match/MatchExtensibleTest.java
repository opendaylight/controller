/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.match;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.match.extensible.DlDst;
import org.opendaylight.controller.sal.match.extensible.DlSrc;
import org.opendaylight.controller.sal.match.extensible.DlType;
import org.opendaylight.controller.sal.match.extensible.DlVlan;
import org.opendaylight.controller.sal.match.extensible.DlVlanPriority;
import org.opendaylight.controller.sal.match.extensible.InPort;
import org.opendaylight.controller.sal.match.extensible.Match;
import org.opendaylight.controller.sal.match.extensible.MatchField;
import org.opendaylight.controller.sal.match.extensible.NwDst;
import org.opendaylight.controller.sal.match.extensible.NwProtocol;
import org.opendaylight.controller.sal.match.extensible.NwSrc;
import org.opendaylight.controller.sal.match.extensible.NwTos;
import org.opendaylight.controller.sal.match.extensible.TpDst;
import org.opendaylight.controller.sal.match.extensible.TpSrc;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;

public class MatchExtensibleTest {
    @Test
    public void testMatchCreation() {
        Node node = NodeCreator.createOFNode(7L);
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector((short) 6, node);
        MatchField<?> field = new InPort(port);

        Assert.assertTrue(field != null);
        Assert.assertEquals(field.getType(), InPort.TYPE);
        Assert.assertEquals(field.getValue(), port);
        Assert.assertTrue(field.isValid());


        byte mac[] = { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 11, (byte) 22 };
        field = null;
        field = new DlSrc(mac);
        Assert.assertNotNull(field.getValue());

        field = null;
        field = new NwTos((byte) 0x22);
        Assert.assertNotNull(field.getValue());
    }

    @Test
    public void testMatchSetGet() {
        Match x = new Match();
        short val = 2346;
        NodeConnector inPort = NodeConnectorCreator.createOFNodeConnector(val, NodeCreator.createOFNode(1L));
        x.setField(new InPort(inPort));
        Assert.assertEquals(x.getField(InPort.TYPE).getValue(), inPort);
        Assert.assertTrue((Short) ((NodeConnector) x.getField(InPort.TYPE).getValue()).getID() == val);
    }

    @Test
    public void testMatchSetGetMAC() {
        Match x = new Match();
        byte mac[] = { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 11, (byte) 22 };
        byte mac2[] = { (byte) 0xaa, (byte) 0xbb, 0, 0, 0, (byte) 0xbb };

        x.setField(new DlSrc(mac));
        x.setField(new DlDst(mac2));
        Assert.assertArrayEquals(mac, (byte[]) x.getField(DlSrc.TYPE).getValue());
        Assert.assertFalse(Arrays.equals((byte[]) x.getField(DlSrc.TYPE).getValue(), (byte[]) x.getField(DlDst.TYPE)
                .getValue()));

        x.setField(new DlDst(mac.clone()));
        Assert.assertArrayEquals((byte[]) x.getField(DlSrc.TYPE).getValue(), (byte[]) x.getField(DlDst.TYPE).getValue());
    }

    @Test
    public void testMatchSetGetNWAddr() throws UnknownHostException {
        Match x = new Match();
        String ip = "172.20.231.23";
        InetAddress address = InetAddress.getByName(ip);
        InetAddress mask = InetAddress.getByName("255.255.0.0");

        x.setField(new NwSrc(address, mask));
        Assert.assertEquals(address, x.getField(NwSrc.TYPE).getValue());
        Assert.assertEquals(x.getField(NwSrc.TYPE).getMask(), mask);
    }

    @Test
    public void testMatchSetGetEtherType() throws UnknownHostException {
        Match x = new Match();

        x.setField(new DlType(EtherTypes.QINQ.shortValue()));
        Assert.assertEquals(x.getField(DlType.TYPE).getValue(), EtherTypes.QINQ.shortValue());

        x.setField(new DlType(EtherTypes.LLDP.shortValue()));
        Assert.assertEquals(x.getField(DlType.TYPE).getValue(), EtherTypes.LLDP.shortValue());
        Assert.assertFalse(x.getField(DlType.TYPE).equals(EtherTypes.LLDP.intValue()));
    }

    @Test
    public void testSetGetNwTos() {
        Match x = new Match();
        x.setField(new NwTos((byte) 0xb));

        Byte t = new Byte((byte) 0xb);

        Object o = x.getField(NwTos.TYPE).getValue();
        Assert.assertEquals(o, t);
        Assert.assertEquals(o, Byte.valueOf((byte)0xb));
    }

    @Test
    public void testSetGetNwProto() {
        Match x = new Match();
        Byte proto = (byte) 199;
        x.setField(new NwProtocol(proto));

        Byte o = (Byte) x.getField(NwProtocol.TYPE).getValue();
        Assert.assertEquals(o, proto);
    }

    @Test
    public void testSetTpSrc() {
        // Minimum value validation.
        Match match = new Match();
        short tp_src = 0;
        match.setField(new TpSrc(tp_src));

        Object o = match.getField(TpSrc.TYPE).getValue();
        Assert.assertEquals(o, tp_src);

        // Maximum value validation.
        match = new Match();
        tp_src = (short) 0xffff;
        match.setField(new TpSrc(tp_src));

        o = match.getField(TpSrc.TYPE).getValue();
        Assert.assertEquals(o, tp_src);
    }

    @Test
    public void testSetTpDst() {
        // Minimum value validation.
        Match match = new Match();
        short tp_dst = 0;
        match.setField(new TpDst(tp_dst));

        Object o = match.getField(TpDst.TYPE).getValue();
        Assert.assertTrue(o.equals(tp_dst));

        // Maximum value validation.
        match = new Match();
        tp_dst = (short) 0xffff;
        match.setField(new TpDst(tp_dst));

        o = match.getField(TpDst.TYPE).getValue();
        Assert.assertEquals(o, tp_dst);
    }

    @Test
    public void testEquality() throws Exception {
        Node node = NodeCreator.createOFNode(7L);
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector((short) 24, node);
        NodeConnector port2 = NodeConnectorCreator.createOFNodeConnector((short) 24, node);
        byte srcMac[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a, (byte) 0xbc };
        byte dstMac[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d, (byte) 0x5e, (byte) 0x6f };
        byte srcMac2[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a, (byte) 0xbc };
        byte dstMac2[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d, (byte) 0x5e, (byte) 0x6f };
        InetAddress srcIP = InetAddress.getByName("2001:420:281:1004:407a:57f4:4d15:c355");
        InetAddress dstIP = InetAddress.getByName("2001:420:281:1004:e123:e688:d655:a1b0");
        InetAddress ipMask = InetAddress.getByName("ffff:ffff:ffff:ffff:0:0:0:0");
        InetAddress ipMaskd = InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:0");
        InetAddress ipMask2 = InetAddress.getByName("ffff:ffff:ffff:ffff:0:0:0:0");
        InetAddress ipMaskd2 = InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:0");
        short ethertype = EtherTypes.IPv6.shortValue();
        short ethertype2 = EtherTypes.IPv6.shortValue();
        short vlan = (short) 27, vlan2 = (short) 27;
        byte vlanPr = (byte) 3, vlanPr2 = (byte) 3;
        Byte tos = 4, tos2 = 4;
        byte proto = IPProtocols.UDP.byteValue(), proto2 = IPProtocols.UDP.byteValue();
        short src = (short) 5500, src2 = (short) 5500;
        short dst = 80, dst2 = 80;

        /*
         * Create a SAL Flow aFlow
         */
        Match match1 = new Match();
        Match match2 = new Match();
        match1.setField(new InPort(port));
        match1.setField(new DlSrc(srcMac));
        match1.setField(new DlDst(dstMac));
        match1.setField(new DlType(ethertype));
        match1.setField(new DlVlan(vlan));
        match1.setField(new DlVlanPriority(vlanPr));
        match1.setField(new NwSrc(srcIP, ipMask));
        match1.setField(new NwDst(dstIP, ipMaskd));
        match1.setField(new NwTos(tos));
        match1.setField(new NwProtocol(proto));
        match1.setField(new TpSrc(src));
        match1.setField(new TpDst(dst));

        match2.setField(new InPort(port2));
        match2.setField(new DlSrc(srcMac2));
        match2.setField(new DlDst(dstMac2));
        match2.setField(new DlType(ethertype2));
        match2.setField(new DlVlan(vlan2));
        match2.setField(new DlVlanPriority(vlanPr2));
        match2.setField(new NwSrc(srcIP, ipMask2));
        match2.setField(new NwDst(dstIP, ipMaskd2));
        match2.setField(new NwTos(tos2));
        match2.setField(new NwProtocol(proto2));
        match2.setField(new TpSrc(src2));
        match2.setField(new TpDst(dst2));

        Assert.assertTrue(match1.equals(match2));

        Set<String> allFields = new HashSet<String>(match1.getMatchesList());
        allFields.addAll(match2.getMatchesList());
        // Make sure all values are equals
        for (String type : allFields) {
            if (match1.isPresent(type)) {
                Assert.assertEquals(match1.getField(type), match2.getField(type));
            }
        }

        // Make none of the fields couples are pointing to the same reference
        MatchField<?> a = null, b = null;
        for (String type : allFields) {
            a = match1.getField(type);
            b = match2.getField(type);
            if (a != null && b != null) {
                Assert.assertFalse(a == b);
            }
        }
    }

    @Test
    public void testEqualityNetMask() throws Exception {

        InetAddress srcIP = InetAddress.getByName("1.1.1.1");
        InetAddress ipMask = InetAddress.getByName("255.255.255.255");
        InetAddress srcIP2 = InetAddress.getByName("1.1.1.1");
        InetAddress ipMask2 = null;
        short ethertype = EtherTypes.IPv4.shortValue();
        short ethertype2 = EtherTypes.IPv4.shortValue();

        /*
         * Create a SAL Flow aFlow
         */
        Match match1 = new Match();
        Match match2 = new Match();

        match1.setField(new DlType(ethertype));
        match1.setField(new NwSrc(srcIP, ipMask));

        match2.setField(new DlType(ethertype2));
        match2.setField(new NwSrc(srcIP2, ipMask2));

        Assert.assertTrue(match1.equals(match2));

        ipMask2 = InetAddress.getByName("255.255.255.255");
        match2.setField(new NwSrc(srcIP2, ipMask2));

        srcIP = InetAddress.getByName("2001:420:281:1004:407a:57f4:4d15:c355");
        srcIP2 = InetAddress.getByName("2001:420:281:1004:407a:57f4:4d15:c355");
        ipMask = null;
        ipMask2 = InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        ethertype = EtherTypes.IPv6.shortValue();
        ethertype2 = EtherTypes.IPv6.shortValue();

        match1.setField(new DlType(ethertype));
        match1.setField(new NwSrc(srcIP, ipMask));

        match2.setField(new DlType(ethertype2));
        match2.setField(new NwSrc(srcIP2, ipMask2));

        Assert.assertEquals(match1, match2);
    }

    @Test
    public void testHashCodeWithReverseMatch() throws Exception {
        InetAddress srcIP1 = InetAddress.getByName("1.1.1.1");
        InetAddress ipMask1 = InetAddress.getByName("255.255.255.255");
        InetAddress srcIP2 = InetAddress.getByName("2.2.2.2");
        InetAddress ipMask2 = InetAddress.getByName("255.255.255.255");
        MatchField<?> field1 = new NwSrc(srcIP1, ipMask1);
        MatchField<?> field2 = new NwDst(srcIP2, ipMask2);
        Match match1 = new Match();
        match1.setField(field1);
        match1.setField(field2);
        Match match2 = match1.reverse();
        Assert.assertFalse(match1.hashCode() == match2.hashCode());
    }

    @Test
    public void testHashCode() throws Exception {
        byte srcMac1[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a, (byte) 0xbc };
        byte srcMac2[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a, (byte) 0xbc };
        byte dstMac1[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d, (byte) 0x5e, (byte) 0x6f };
        byte dstMac2[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d, (byte) 0x5e, (byte) 0x6f };
        short ethertype = EtherTypes.IPv4.shortValue();
        short ethertype2 = EtherTypes.IPv4.shortValue();
        InetAddress srcIP1 = InetAddress.getByName("1.1.1.1");
        InetAddress ipMask1 = InetAddress.getByName("255.255.255.255");
        InetAddress srcIP2 = InetAddress.getByName("1.1.1.1");
        InetAddress ipMask2 = InetAddress.getByName("255.255.255.255");

        Match match1 = new Match();
        Match match2 = new Match();

        MatchField<?> field1 = new DlSrc(srcMac1);
        MatchField<?> field2 = new DlSrc(srcMac2);
        Assert.assertTrue(field1.hashCode() == field2.hashCode());

        match1.setField(field1);
        match2.setField(field2);
        Assert.assertTrue(match1.hashCode() == match2.hashCode());

        MatchField<?> field3 = new DlDst(dstMac1);
        MatchField<?> field4 = new DlDst(dstMac2);
        Assert.assertTrue(field3.hashCode() == field4.hashCode());

        match1.setField(field3);
        match2.setField(field4);
        Assert.assertTrue(match1.hashCode() == match2.hashCode());

        MatchField<?> field5 = new DlType(ethertype);
        MatchField<?> field6 = new DlType(ethertype2);
        Assert.assertTrue(field5.hashCode() == field6.hashCode());

        match1.setField(field5);
        match2.setField(field6);
        Assert.assertTrue(match1.hashCode() == match2 .hashCode());

        MatchField<?> field7 = new NwSrc(srcIP1, ipMask1);
        MatchField<?> field8 = new NwSrc(srcIP2, ipMask2);
        Assert.assertTrue(field7.hashCode() == field8.hashCode());

        match1.setField(field7);
        match2.setField(field8);
        Assert.assertTrue(match1.hashCode() == match2.hashCode());

    }

    @Test
    public void testCloning() throws Exception {
        Node node = NodeCreator.createOFNode(7L);
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector((short) 24, node);
        byte srcMac[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a, (byte) 0xbc };
        byte dstMac[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d, (byte) 0x5e, (byte) 0x6f };
        InetAddress srcIP = InetAddress.getByName("2001:420:281:1004:407a:57f4:4d15:c355");
        InetAddress dstIP = InetAddress.getByName("2001:420:281:1004:e123:e688:d655:a1b0");
        InetAddress ipMasks = InetAddress.getByName("ffff:ffff:ffff:ffff:0:0:0:0");
        InetAddress ipMaskd = InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:0");
        short ethertype = EtherTypes.IPv6.shortValue();
        short vlan = (short) 27;
        byte vlanPr = (byte) 3;
        Byte tos = 4;
        byte proto = IPProtocols.UDP.byteValue();
        short src = (short) 5500;
        short dst = 80;

        /*
         * Create a SAL Flow aFlow
         */
        Match match = new Match();
        match.setField(new InPort(port));
        match.setField(new DlSrc(srcMac));
        match.setField(new DlDst(dstMac));
        match.setField(new DlType(ethertype));
        match.setField(new DlVlan(vlan));
        match.setField(new DlVlanPriority(vlanPr));
        match.setField(new NwSrc(srcIP, ipMasks));
        match.setField(new NwDst(dstIP, ipMaskd));
        match.setField(new NwTos(tos));
        match.setField(new NwProtocol(proto));
        match.setField(new TpSrc(src));
        match.setField(new TpDst(dst));

        Match cloned = match.clone();

        // Make sure all values are equals
        for (String type : match.getMatchesList()) {
            if (match.isPresent(type)) {
                if (!match.getField(type).equals(cloned.getField(type))) {
                    Assert.assertEquals(match.getField(type), cloned.getField(type));
                }
            }
        }

        // Make sure none of the fields couples are pointing to the same
        // reference
        MatchField<?> a = null, b = null;
        for (String type : match.getMatchesList()) {
            a = match.getField(type);
            b = cloned.getField(type);
            if (a != null && b != null) {
                Assert.assertFalse(a == b);
            }
        }

        Assert.assertTrue(match.equals(cloned));

        Assert.assertEquals(match.getField(DlSrc.TYPE), cloned.getField(DlSrc.TYPE));
        Assert.assertEquals(match.getField(NwDst.TYPE), cloned.getField(NwDst.TYPE));
        Assert.assertEquals(match.getField(NwDst.TYPE).getMask(), cloned.getField(NwDst.TYPE).getMask());
        Assert.assertEquals(match.hashCode(), cloned.hashCode());
    }

    @Test
    public void testFlip() throws Exception {
        Node node = NodeCreator.createOFNode(7L);
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector((short) 24, node);
        byte srcMac[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a, (byte) 0xbc };
        byte dstMac[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d, (byte) 0x5e, (byte) 0x6f };
        InetAddress srcIP = InetAddress.getByName("2001:420:281:1004:407a:57f4:4d15:c355");
        InetAddress dstIP = InetAddress.getByName("2001:420:281:1004:e123:e688:d655:a1b0");
        InetAddress ipMasks = InetAddress.getByName("ffff:ffff:ffff:ffff:0:0:0:0");
        InetAddress ipMaskd = InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:0");
        short ethertype = EtherTypes.IPv6.shortValue();
        short vlan = (short) 27;
        byte vlanPr = (byte) 3;
        Byte tos = 4;
        byte proto = IPProtocols.UDP.byteValue();
        short src = (short) 5500;
        short dst = 80;

        /*
         * Create a SAL Flow aFlow
         */
        Match match = new Match();
        match.setField(new InPort(port));
        match.setField(new DlSrc(srcMac));
        match.setField(new DlDst(dstMac));
        match.setField(new DlType(ethertype));
        match.setField(new DlVlan(vlan));
        match.setField(new DlVlanPriority(vlanPr));
        match.setField(new NwSrc(srcIP, ipMasks));
        match.setField(new NwDst(dstIP, ipMaskd));
        match.setField(new NwTos(tos));
        match.setField(new NwProtocol(proto));
        match.setField(new TpSrc(src));
        match.setField(new TpDst(dst));

        Match flipped = match.reverse();

        Assert.assertEquals(match.getField(DlType.TYPE), flipped.getField(DlType.TYPE));
        Assert.assertEquals(match.getField(DlVlan.TYPE), flipped.getField(DlVlan.TYPE));

        Assert.assertArrayEquals((byte[]) match.getField(DlDst.TYPE).getValue(), (byte[]) flipped.getField(DlSrc.TYPE)
                .getValue());

        Assert.assertEquals(match.getField(NwDst.TYPE).getValue(), flipped.getField(NwSrc.TYPE).getValue());

        Assert.assertEquals(match.getField(TpDst.TYPE).getValue(), flipped.getField(TpSrc.TYPE).getValue());

        Match flipflip = flipped.reverse().reverse();
        Assert.assertEquals(flipflip, flipped);

    }

    @Test
    public void testVlanNone() throws Exception {
        // The value 0 is used to indicate that no VLAN ID is set
        short vlan = (short) 0;
        MatchField<?> field = new DlVlan(vlan);

        Assert.assertTrue(field != null);
        Assert.assertEquals(field.getValue(), new Short(vlan));
        Assert.assertTrue(field.isValid());
    }

    @Test
    public void testIntersection() throws UnknownHostException {
        Short ethType = Short.valueOf((short)0x800);
        InetAddress ip1 = InetAddress.getByName("1.1.1.1");
        InetAddress ip2 = InetAddress.getByName("1.1.1.0");
        InetAddress ipm2 = InetAddress.getByName("255.255.255.0");
        InetAddress ip3 = InetAddress.getByName("1.3.0.0");
        InetAddress ipm3 = InetAddress.getByName("255.255.0.0");
        InetAddress ip4 = InetAddress.getByName("1.3.4.4");
        InetAddress ipm4 = InetAddress.getByName("255.255.255.0");

        Match m1 = new Match();
        m1.setField(new DlType(ethType));
        m1.setField(new NwSrc(ip1));

        Match m2 = new Match();
        m2.setField(new DlType(ethType));
        m2.setField(new NwSrc(ip2, ipm2));

        Match m3 = new Match();
        m3.setField(new DlType(ethType));
        m3.setField(new NwSrc(ip3, ipm3));
        m3.setField(new NwProtocol(IPProtocols.TCP.byteValue()));

        Match m3r = m3.reverse();
        Assert.assertTrue(m3.intersetcs(m3r));

        Assert.assertTrue(m1.intersetcs(m2));
        Assert.assertTrue(m2.intersetcs(m1));
        Assert.assertFalse(m1.intersetcs(m3));
        Assert.assertTrue(m1.intersetcs(m3r));
        Assert.assertFalse(m3.intersetcs(m1));
        Assert.assertTrue(m3.intersetcs(m1.reverse()));
        Assert.assertFalse(m2.intersetcs(m3));
        Assert.assertFalse(m3.intersetcs(m2));
        Assert.assertTrue(m2.intersetcs(m3r));


        Match i = m1.getIntersection(m2);
        Assert.assertTrue(((Short)i.getField(DlType.TYPE).getValue()).equals(ethType));
        // Verify intersection of IP addresses is correct
        Assert.assertTrue(((InetAddress)i.getField(NwSrc.TYPE).getValue()).equals(ip1));
        Assert.assertNull(i.getField(NwSrc.TYPE).getMask());

        // Empty set
        i = m2.getIntersection(m3);
        Assert.assertNull(i);

        Match m4 = new Match();
        m4.setField(new DlType(ethType));
        m4.setField(new NwProtocol(IPProtocols.TCP.byteValue()));
        m3.setField(new NwSrc(ip4, ipm4));
        Assert.assertTrue(m4.intersetcs(m3));

        // Verify intersection of IP and IP mask addresses is correct
        Match ii = m3.getIntersection(m4);
        Assert.assertTrue(((InetAddress)ii.getField(NwSrc.TYPE).getValue()).equals(ip4));
        Assert.assertTrue(((InetAddress)ii.getField(NwSrc.TYPE).getMask()).equals(ipm4));

        Match m5 = new Match();
        m5.setField(new DlType(ethType));
        m3.setField(new NwSrc(ip3, ipm3));
        m5.setField(new NwProtocol(IPProtocols.UDP.byteValue()));
        Assert.assertFalse(m5.intersetcs(m3));
        Assert.assertFalse(m5.intersetcs(m4));
        Assert.assertTrue(m5.intersetcs(m5));
        Assert.assertFalse(m3.intersetcs(m5));
        Assert.assertFalse(m4.intersetcs(m5));


        Match i2 = m4.getIntersection(m3);
        Assert.assertFalse(i2.isEmpty());
        Assert.assertFalse(i2.getMatchesList().isEmpty());
        Assert.assertTrue(((InetAddress)i2.getField(NwSrc.TYPE).getValue()).equals(ip3));
        Assert.assertTrue(((InetAddress)i2.getField(NwSrc.TYPE).getMask()).equals(ipm3));
        Assert.assertTrue(((Byte)i2.getField(NwProtocol.TYPE).getValue()).equals(IPProtocols.TCP.byteValue()));

        byte src[] = {(byte)0, (byte)0xab,(byte)0xbc,(byte)0xcd,(byte)0xde,(byte)0xef};
        byte dst[] = {(byte)0x10, (byte)0x11,(byte)0x12,(byte)0x13,(byte)0x14,(byte)0x15};
        Short srcPort = (short)1024;
        Short dstPort = (short)80;

        // Check identity
        Match m6 = new Match();
        m6.setField(new DlSrc(src));
        m6.setField(new DlDst(dst));
        m6.setField(new NwSrc(ip2, ipm2));
        m6.setField(new NwDst(ip3, ipm3));
        m6.setField(new NwProtocol(IPProtocols.UDP.byteValue()));
        m6.setField(new TpSrc(srcPort));
        m6.setField(new TpDst(dstPort));
        Assert.assertTrue(m6.intersetcs(m6));
        Assert.assertTrue(m6.getIntersection(m6).equals(m6));

        // Empty match, represents the universal set (all packets)
        Match u = new Match();
        Assert.assertEquals(m6.getIntersection(u), m6);
        Assert.assertEquals(u.getIntersection(m6), m6);

        // No intersection with null match, empty set
        Assert.assertNull(m6.getIntersection(null));
    }
}
