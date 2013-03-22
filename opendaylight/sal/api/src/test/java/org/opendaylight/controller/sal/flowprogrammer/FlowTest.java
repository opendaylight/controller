
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.flowprogrammer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.Loopback;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.action.PushVlan;
import org.opendaylight.controller.sal.action.SetDlDst;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;

public class FlowTest {

    @Test
    public void testFlowEquality() throws Exception {
        Node node = NodeCreator.createOFNode(1055l);
        Flow flow1 = getSampleFlowV6(node);
        Flow flow2 = getSampleFlowV6(node);
        Flow flow3 = getSampleFlow(node);

        // Check Match equality
        Assert.assertTrue(flow1.getMatch().equals(flow2.getMatch()));

        // Check Actions equality
        for (int i = 0; i < flow1.getActions().size(); i++) {
            Action a = flow1.getActions().get(i);
            Action b = flow2.getActions().get(i);
            Assert.assertTrue(a != b);
            Assert.assertTrue(a.equals(b));
        }

        Assert.assertTrue(flow1.equals(flow2));
        Assert.assertFalse(flow2.equals(flow3));

        // Check Flow equality where Flow has null action list (pure match)
        List<Action> emptyList = new ArrayList<Action>(1);
        Flow x = flow1.clone();
        x.setActions(emptyList);
        Assert.assertFalse(flow1.equals(x));
        flow1.setActions(emptyList);
        Assert.assertTrue(flow1.equals(x));
    }

    @Test
    public void testFlowCloning() throws UnknownHostException {
        Node node = NodeCreator.createOFNode(55l);
        Flow flow1 = getSampleFlowV6(node);
        Flow flow2 = flow1.clone();

        Assert.assertTrue(flow1.equals(flow2));
        Assert.assertTrue(flow1.getMatch().equals(flow2.getMatch()));
        Assert.assertTrue(flow1.getActions() != flow2.getActions());
        Assert.assertTrue(flow1.getActions().equals(flow2.getActions()));
    }

    @Test
    public void testFlowActions() throws UnknownHostException {
        Node node = NodeCreator.createOFNode(55l);
        Flow flow = getSampleFlowV6(node);

        List<Action> actions = flow.getActions();
        actions.add(new Loopback());

        Assert.assertTrue(flow.getActions() != actions);
        Assert.assertTrue(!flow.getActions().equals(actions));

        flow.addAction(new Loopback());
        Assert.assertTrue(flow.getActions().equals(actions));

        actions.remove(new Loopback());
        flow.removeAction(new Loopback());
        Assert.assertTrue(flow.getActions().equals(actions));

        // Add a malformed action
        Assert.assertFalse(flow.addAction(new PushVlan(EtherTypes.CISCOQINQ, 3,
                3, 8000)));
    }

    private Flow getSampleFlow(Node node) throws UnknownHostException {
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector(
                (short) 24, node);
        NodeConnector oport = NodeConnectorCreator.createOFNodeConnector(
                (short) 30, node);
        byte srcMac[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0x9a, (byte) 0xbc };
        byte dstMac[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d,
                (byte) 0x5e, (byte) 0x6f };
        InetAddress srcIP = InetAddress.getByName("172.28.30.50");
        InetAddress dstIP = InetAddress.getByName("171.71.9.52");
        InetAddress newIP = InetAddress.getByName("200.200.100.1");
        InetAddress ipMask = InetAddress.getByName("255.255.255.0");
        InetAddress ipMask2 = InetAddress.getByName("255.240.0.0");
        short ethertype = EtherTypes.IPv4.shortValue();
        short vlan = (short) 27;
        byte vlanPr = 3;
        Byte tos = 4;
        byte proto = IPProtocols.TCP.byteValue();
        short src = (short) 55000;
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
        match.setField(MatchType.NW_SRC, srcIP, ipMask);
        match.setField(MatchType.NW_DST, dstIP, ipMask2);
        match.setField(MatchType.NW_TOS, tos);
        match.setField(MatchType.NW_PROTO, proto);
        match.setField(MatchType.TP_SRC, src);
        match.setField(MatchType.TP_DST, dst);

        List<Action> actions = new ArrayList<Action>();
        actions.add(new SetNwDst(newIP));
        actions.add(new Output(oport));
        actions.add(new PopVlan());
        actions.add(new Flood());
        actions.add(new Controller());

        Flow flow = new Flow(match, actions);
        flow.setPriority((short) 100);
        flow.setHardTimeout((short) 360);

        return flow;
    }

    private Flow getSampleFlowV6(Node node) throws UnknownHostException {
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector(
                (short) 24, node);
        NodeConnector oport = NodeConnectorCreator.createOFNodeConnector(
                (short) 30, node);
        byte srcMac[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0x9a, (byte) 0xbc };
        byte dstMac[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d,
                (byte) 0x5e, (byte) 0x6f };
        byte newMac[] = { (byte) 0x11, (byte) 0xaa, (byte) 0xbb, (byte) 0x34,
                (byte) 0x9a, (byte) 0xee };
        InetAddress srcIP = InetAddress
                .getByName("2001:420:281:1004:407a:57f4:4d15:c355");
        InetAddress dstIP = InetAddress
                .getByName("2001:420:281:1004:e123:e688:d655:a1b0");
        InetAddress ipMask = InetAddress
                .getByName("ffff:ffff:ffff:ffff:0:0:0:0");
        InetAddress ipMask2 = InetAddress
                .getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:0");
        InetAddress newIP = InetAddress.getByName("2056:650::a1b0");
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
        match.setField(MatchType.NW_SRC, srcIP, ipMask);
        match.setField(MatchType.NW_DST, dstIP, ipMask2);
        match.setField(MatchType.NW_TOS, tos);
        match.setField(MatchType.NW_PROTO, proto);
        match.setField(MatchType.TP_SRC, src);
        match.setField(MatchType.TP_DST, dst);

        List<Action> actions = new ArrayList<Action>();
        actions.add(new Controller());
        actions.add(new SetVlanId(5));
        actions.add(new SetDlDst(newMac));
        actions.add(new SetNwDst(newIP));
        actions.add(new Output(oport));
        actions.add(new PopVlan());
        actions.add(new Flood());

        actions.add(new Controller());

        Flow flow = new Flow(match, actions);
        flow.setPriority((short) 300);
        flow.setHardTimeout((short) 240);

        return flow;
    }
}
