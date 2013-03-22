
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

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;

public class MatchTest {
    @Test
    public void testMatchCreation() {
        Node node = NodeCreator.createOFNode(7l);
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector(
                (short) 6, node);
        MatchField field = new MatchField(MatchType.IN_PORT, port);

        Assert.assertTrue(field != null);
        Assert.assertTrue(field.getType() == MatchType.IN_PORT);
        Assert.assertTrue((NodeConnector) field.getValue() == port);
        Assert.assertTrue(field.isValid());

        field = null;
        field = new MatchField(MatchType.TP_SRC, Long.valueOf(23));
        Assert.assertFalse(field.isValid());

        field = null;
        field = new MatchField(MatchType.TP_SRC, (long) 45);
        Assert.assertFalse(field.isValid());

        field = null;
        field = new MatchField(MatchType.TP_SRC, 120000);
        Assert.assertFalse(field.isValid());

        byte mac[] = { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd,
                (byte) 11, (byte) 22 };
        byte mask[] = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff };
        field = null;
        field = new MatchField(MatchType.DL_SRC, mac, mask);
        Assert.assertFalse(field.getValue() == null);

        field = null;
        field = new MatchField(MatchType.NW_TOS, (byte) 0x22, (byte) 0x3);
        Assert.assertFalse(field.getValue() == null);
    }

    @Test
    public void testMatchSetGet() {
        Match x = new Match();
        short val = 2346;
        NodeConnector inPort = NodeConnectorCreator.createOFNodeConnector(val,
                NodeCreator.createOFNode(1l));
        x.setField(MatchType.IN_PORT, inPort);
        Assert.assertTrue(((NodeConnector) x.getField(MatchType.IN_PORT)
                .getValue()).equals(inPort));
        Assert
                .assertTrue((Short) ((NodeConnector) x.getField(
                        MatchType.IN_PORT).getValue()).getID() == val);
    }

    @Test
    public void testMatchSetGetMAC() {
        Match x = new Match();
        byte mac[] = { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd,
                (byte) 11, (byte) 22 };
        byte mac2[] = { (byte) 0xaa, (byte) 0xbb, 0, 0, 0, (byte) 0xbb };
        byte mask1[] = { (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44,
                (byte) 0x55, (byte) 0x66 };
        byte mask2[] = { (byte) 0xff, (byte) 0xff, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0xff };

        x.setField(MatchType.DL_SRC, mac.clone(), mask1);
        x.setField(MatchType.DL_DST, mac2.clone(), mask2);
        Assert.assertTrue(Arrays.equals(mac, (byte[]) x.getField(
                MatchType.DL_SRC).getValue()));
        Assert.assertFalse(Arrays.equals((byte[]) x.getField(MatchType.DL_SRC)
                .getValue(), (byte[]) x.getField(MatchType.DL_DST).getValue()));
        Assert.assertFalse(x.getField(MatchType.DL_SRC).getBitMask() == x
                .getField(MatchType.DL_DST).getBitMask());

        x.setField(new MatchField(MatchType.DL_DST, mac.clone(), mask1));
        Assert.assertTrue(Arrays.equals((byte[]) x.getField(MatchType.DL_SRC)
                .getValue(), (byte[]) x.getField(MatchType.DL_DST).getValue()));
    }

    @Test
    public void testMatchSetGetNWAddr() throws UnknownHostException {
        Match x = new Match();
        String ip = "172.20.231.23";
        InetAddress address = InetAddress.getByName(ip);
        InetAddress mask = InetAddress.getByName("255.255.0.0");

        x.setField(MatchType.NW_SRC, address, mask);
        Assert.assertTrue(ip.equals(((InetAddress) x.getField(MatchType.NW_SRC)
                .getValue()).getHostAddress()));
        Assert.assertTrue(x.getField(MatchType.NW_SRC).getMask().equals(mask));
    }

    @Test
    public void testMatchSetGetEtherType() throws UnknownHostException {
        Match x = new Match();

        x.setField(MatchType.DL_TYPE, EtherTypes.QINQ.shortValue(),
                (short) 0xffff);
        Assert.assertTrue(((Short) x.getField(MatchType.DL_TYPE).getValue())
                .equals(EtherTypes.QINQ.shortValue()));
        Assert
                .assertFalse(x.getField(MatchType.DL_TYPE).getValue() == EtherTypes.QINQ);
        Assert.assertFalse(x.getField(MatchType.DL_TYPE).getValue().equals(
                EtherTypes.QINQ));

        x.setField(MatchType.DL_TYPE, EtherTypes.LLDP.shortValue(),
                (short) 0xffff);
        Assert.assertTrue(((Short) x.getField(MatchType.DL_TYPE).getValue())
                .equals(EtherTypes.LLDP.shortValue()));
        Assert.assertFalse(x.getField(MatchType.DL_TYPE).equals(
                EtherTypes.LLDP.intValue()));
    }

    @Test
    public void testSetGetNwTos() {
        Match x = new Match();
        x.setField(MatchType.NW_TOS, (byte) 0xb, (byte) 0xf);

        Byte t = new Byte((byte) 0xb);

        Object o = x.getField(MatchType.NW_TOS).getValue();
        Assert.assertTrue(o.equals(t));
        Assert.assertTrue(o.equals((byte) 0xb));
    }

    @Test
    public void testSetGetNwProto() {
        Match x = new Match();
        byte proto = (byte) 199;
        x.setField(MatchType.NW_PROTO, proto, (byte) 0xff);

        Object o = x.getField(MatchType.NW_PROTO).getValue();
        Assert.assertTrue(o.equals(proto));
    }

    @Test
    public void testMatchMask() {
        Match x = new Match();
        NodeConnector inPort = NodeConnectorCreator.createOFNodeConnector(
                (short) 6, NodeCreator.createOFNode(3l));
        x.setField(MatchType.IN_PORT, inPort);
        x.setField(MatchType.DL_VLAN, (short) 28, (short) 0xfff);
        Assert.assertFalse(x.getMatches() == 0);
        Assert
                .assertTrue(x.getMatches() == (MatchType.IN_PORT.getIndex() | MatchType.DL_VLAN
                        .getIndex()));
    }

    @Test
    public void testMatchBitMask() {
        byte mac[] = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 22, (byte) 12 };
        byte mask[] = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0 };
        NodeConnector inPort = NodeConnectorCreator.createOFNodeConnector(
                (short) 4095, NodeCreator.createOFNode(7l));

        MatchField x = new MatchField(MatchType.IN_PORT, inPort);
        Assert.assertTrue((x.getMask()) == null);

        x = new MatchField(MatchType.DL_VLAN, (short) 255, (short) 0xff);
        Assert.assertTrue(x.getBitMask() == 0xff);

        x = new MatchField(MatchType.DL_SRC, mac, mask);
        Assert.assertTrue(x.getMask().equals(mask));
        Assert.assertTrue(x.getBitMask() == 0xffffffffff00L);
    }

    @Test
    public void testNullMask() {
        byte mac[] = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 22, (byte) 12 };
        NodeConnector inPort = NodeConnectorCreator.createOFNodeConnector(
                (short) 2000, NodeCreator.createOFNode(7l));

        MatchField x = new MatchField(MatchType.IN_PORT, inPort);
        Assert.assertTrue(x.getBitMask() == 0);

        x = new MatchField(MatchType.NW_PROTO, (byte) 17);
        Assert.assertTrue(x.getBitMask() == 0xff);

        x = new MatchField(MatchType.DL_VLAN, (short) 255);
        Assert.assertTrue(x.getBitMask() == 0xfff);

        x = new MatchField(MatchType.DL_SRC, mac);
        Assert.assertTrue(x.getBitMask() == 0xffffffffffffL);
    }

    @Test
    public void testEquality() throws Exception {
        Node node = NodeCreator.createOFNode(7l);
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector(
                (short) 24, node);
        NodeConnector port2 = NodeConnectorCreator.createOFNodeConnector(
                (short) 24, node);
        byte srcMac[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0x9a, (byte) 0xbc };
        byte dstMac[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d,
                (byte) 0x5e, (byte) 0x6f };
        byte srcMac2[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0x9a, (byte) 0xbc };
        byte dstMac2[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d,
                (byte) 0x5e, (byte) 0x6f };
        InetAddress srcIP = InetAddress
                .getByName("2001:420:281:1004:407a:57f4:4d15:c355");
        InetAddress dstIP = InetAddress
                .getByName("2001:420:281:1004:e123:e688:d655:a1b0");
        InetAddress ipMask = InetAddress
                .getByName("ffff:ffff:ffff:ffff:0:0:0:0");
        InetAddress ipMaskd = InetAddress
                .getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:0");
        InetAddress srcIP2 = InetAddress
                .getByName("2001:420:281:1004:407a:57f4:4d15:c355");
        InetAddress dstIP2 = InetAddress
                .getByName("2001:420:281:1004:e123:e688:d655:a1b0");
        InetAddress ipMask2 = InetAddress
                .getByName("ffff:ffff:ffff:ffff:0:0:0:0");
        InetAddress ipMaskd2 = InetAddress
                .getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:0");
        short ethertype = EtherTypes.IPv6.shortValue();
        short ethertype2 = EtherTypes.IPv6.shortValue();
        short vlan = (short) 27, vlan2 = (short) 27;
        byte vlanPr = (byte) 3, vlanPr2 = (byte) 3;
        Byte tos = 4, tos2 = 4;
        byte proto = IPProtocols.UDP.byteValue(), proto2 = IPProtocols.UDP
                .byteValue();
        short src = (short) 5500, src2 = (short) 5500;
        short dst = 80, dst2 = 80;

        /*
         * Create a SAL Flow aFlow
         */
        Match match1 = new Match();
        Match match2 = new Match();
        match1.setField(MatchType.IN_PORT, port);
        match1.setField(MatchType.DL_SRC, srcMac);
        match1.setField(MatchType.DL_DST, dstMac);
        match1.setField(MatchType.DL_TYPE, ethertype);
        match1.setField(MatchType.DL_VLAN, vlan);
        match1.setField(MatchType.DL_VLAN_PR, vlanPr);
        match1.setField(MatchType.NW_SRC, srcIP, ipMask);
        match1.setField(MatchType.NW_DST, dstIP, ipMaskd);
        match1.setField(MatchType.NW_TOS, tos);
        match1.setField(MatchType.NW_PROTO, proto);
        match1.setField(MatchType.TP_SRC, src);
        match1.setField(MatchType.TP_DST, dst);

        match2.setField(MatchType.IN_PORT, port2);
        match2.setField(MatchType.DL_SRC, srcMac2);
        match2.setField(MatchType.DL_DST, dstMac2);
        match2.setField(MatchType.DL_TYPE, ethertype2);
        match2.setField(MatchType.DL_VLAN, vlan2);
        match2.setField(MatchType.DL_VLAN_PR, vlanPr2);
        match2.setField(MatchType.NW_SRC, srcIP2, ipMask2);
        match2.setField(MatchType.NW_DST, dstIP2, ipMaskd2);
        match2.setField(MatchType.NW_TOS, tos2);
        match2.setField(MatchType.NW_PROTO, proto2);
        match2.setField(MatchType.TP_SRC, src2);
        match2.setField(MatchType.TP_DST, dst2);

        Assert.assertTrue(match1.equals(match2));

        // Make sure all values are equals
        for (MatchType type : MatchType.values()) {
            if (match1.isPresent(type)) {
                Assert.assertTrue(match1.getField(type).equals(
                        match2.getField(type)));
            }
        }

        // Make none of the fields couples are pointing to the same reference
        MatchField a = null, b = null;
        for (MatchType type : MatchType.values()) {
            a = match1.getField(type);
            b = match2.getField(type);
            if (a != null && b != null) {
                Assert.assertFalse(a == b);
            }
        }
    }

    @Test
    public void testCloning() throws Exception {
        Node node = NodeCreator.createOFNode(7l);
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector(
                (short) 24, node);
        byte srcMac[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0x9a, (byte) 0xbc };
        byte dstMac[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d,
                (byte) 0x5e, (byte) 0x6f };
        InetAddress srcIP = InetAddress
                .getByName("2001:420:281:1004:407a:57f4:4d15:c355");
        InetAddress dstIP = InetAddress
                .getByName("2001:420:281:1004:e123:e688:d655:a1b0");
        InetAddress ipMasks = InetAddress
                .getByName("ffff:ffff:ffff:ffff:0:0:0:0");
        InetAddress ipMaskd = InetAddress
                .getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:0");
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
        match.setField(MatchType.IN_PORT, port);
        match.setField(MatchType.DL_SRC, srcMac);
        match.setField(MatchType.DL_DST, dstMac);
        match.setField(MatchType.DL_TYPE, ethertype);
        match.setField(MatchType.DL_VLAN, vlan);
        match.setField(MatchType.DL_VLAN_PR, vlanPr);
        match.setField(MatchType.NW_SRC, srcIP, ipMasks);
        match.setField(MatchType.NW_DST, dstIP, ipMaskd);
        match.setField(MatchType.NW_TOS, tos);
        match.setField(MatchType.NW_PROTO, proto);
        match.setField(MatchType.TP_SRC, src);
        match.setField(MatchType.TP_DST, dst);

        Match cloned = match.clone();

        // Make sure all values are equals
        for (MatchType type : MatchType.values()) {
            if (match.isPresent(type)) {
                if (!match.getField(type).equals(cloned.getField(type))) {
                    Assert.assertTrue(match.getField(type).equals(
                            cloned.getField(type)));
                }
            }
        }

        // Make sure none of the fields couples are pointing to the same reference
        MatchField a = null, b = null;
        for (MatchType type : MatchType.values()) {
            a = match.getField(type);
            b = cloned.getField(type);
            if (a != null && b != null) {
                Assert.assertFalse(a == b);
            }
        }

        Assert.assertTrue(match.equals(cloned));

        Assert.assertFalse(match.getField(MatchType.DL_SRC) == cloned
                .getField(MatchType.DL_SRC));
        Assert.assertFalse(match.getField(MatchType.NW_DST) == cloned
                .getField(MatchType.NW_DST));
        Assert.assertTrue(match.getField(MatchType.NW_DST).getMask().equals(
                cloned.getField(MatchType.NW_DST).getMask()));
    }

    @Test
    public void testFlip() throws Exception {
        Node node = NodeCreator.createOFNode(7l);
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector(
                (short) 24, node);
        byte srcMac[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0x9a, (byte) 0xbc };
        byte dstMac[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d,
                (byte) 0x5e, (byte) 0x6f };
        InetAddress srcIP = InetAddress
                .getByName("2001:420:281:1004:407a:57f4:4d15:c355");
        InetAddress dstIP = InetAddress
                .getByName("2001:420:281:1004:e123:e688:d655:a1b0");
        InetAddress ipMasks = InetAddress
                .getByName("ffff:ffff:ffff:ffff:0:0:0:0");
        InetAddress ipMaskd = InetAddress
                .getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:0");
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
        match.setField(MatchType.IN_PORT, port);
        match.setField(MatchType.DL_SRC, srcMac);
        match.setField(MatchType.DL_DST, dstMac);
        match.setField(MatchType.DL_TYPE, ethertype);
        match.setField(MatchType.DL_VLAN, vlan);
        match.setField(MatchType.DL_VLAN_PR, vlanPr);
        match.setField(MatchType.NW_SRC, srcIP, ipMasks);
        match.setField(MatchType.NW_DST, dstIP, ipMaskd);
        match.setField(MatchType.NW_TOS, tos);
        match.setField(MatchType.NW_PROTO, proto);
        match.setField(MatchType.TP_SRC, src);
        match.setField(MatchType.TP_DST, dst);

        Match flipped = match.reverse();

        Assert.assertTrue(match.getField(MatchType.DL_TYPE).equals(
                flipped.getField(MatchType.DL_TYPE)));
        Assert.assertTrue(match.getField(MatchType.DL_VLAN).equals(
                flipped.getField(MatchType.DL_VLAN)));

        Assert.assertTrue(match.getField(MatchType.DL_DST).getValue().equals(
                flipped.getField(MatchType.DL_SRC).getValue()));
        Assert.assertTrue(match.getField(MatchType.DL_DST).getMask() == flipped
                .getField(MatchType.DL_SRC).getMask());

        Assert.assertTrue(match.getField(MatchType.NW_DST).getValue().equals(
                flipped.getField(MatchType.NW_SRC).getValue()));
        Assert.assertTrue(match.getField(MatchType.NW_DST).getMask() == flipped
                .getField(MatchType.NW_SRC).getMask());

        Assert.assertTrue(match.getField(MatchType.TP_DST).getValue().equals(
                flipped.getField(MatchType.TP_SRC).getValue()));
        Assert.assertTrue(match.getField(MatchType.TP_DST).getMask() == flipped
                .getField(MatchType.TP_SRC).getMask());

        Match flipflip = flipped.reverse().reverse();
        Assert.assertTrue(flipflip.equals(flipped));

    }
}
