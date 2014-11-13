/*
 * Copyright (c) 2013-2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.CRUDP;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.ETHERNET_ARP;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.TCP;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.UDP;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.FloodAll;
import org.opendaylight.controller.sal.action.HwPath;
import org.opendaylight.controller.sal.action.Loopback;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.action.PushVlan;
import org.opendaylight.controller.sal.action.SetDlDst;
import org.opendaylight.controller.sal.action.SetDlSrc;
import org.opendaylight.controller.sal.action.SetDlType;
import org.opendaylight.controller.sal.action.SetNextHop;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetNwSrc;
import org.opendaylight.controller.sal.action.SetNwTos;
import org.opendaylight.controller.sal.action.SetTpDst;
import org.opendaylight.controller.sal.action.SetTpSrc;
import org.opendaylight.controller.sal.action.SetVlanCfi;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.action.SetVlanPcp;
import org.opendaylight.controller.sal.action.SwPath;
import org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.VlanCfi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.ControllerActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodAllActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.HwPathActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.LoopbackActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushPbbActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlTypeActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetMplsTtlActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNextHopActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTosActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTtlActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetQueueActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanCfiActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanPcpActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SwPathActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.type.action._case.SetDlTypeActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.next.hop.action._case.SetNextHopActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.tos.action._case.SetNwTosActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.tp.dst.action._case.SetTpDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.tp.src.action._case.SetTpSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.cfi.action._case.SetVlanCfiActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.id.action._case.SetVlanIdActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.pcp.action._case.SetVlanPcpActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAddedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpSourceHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpTargetHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;

import com.google.common.net.InetAddresses;

public class TestToSalConversionsUtils {
    // prefix:
    // od|Od = Open Daylight
    private enum MtchType {
        other, untagged, ipv4, ipv6, arp, sctp, tcp, udp
    }

    @Test
    public void testToSalConversion() throws ConstructionException {
        FlowAddedBuilder odNodeFlowBuilder = new FlowAddedBuilder();
        odNodeFlowBuilder = prepareOdFlowCommon();

        Node node = new Node(NodeIDType.OPENFLOW,(long)1);

        Flow salFlow = ToSalConversionsUtils.toFlow(prepareOdFlow(odNodeFlowBuilder, MtchType.other), node);
        checkSalMatch(salFlow.getMatch(), MtchType.other);

        salFlow = ToSalConversionsUtils.toFlow(prepareOdFlow(odNodeFlowBuilder, MtchType.untagged), node);
        checkSalMatch(salFlow.getMatch(), MtchType.untagged);

        salFlow = ToSalConversionsUtils.toFlow(prepareOdFlow(odNodeFlowBuilder, MtchType.ipv4), node);
        checkSalMatch(salFlow.getMatch(), MtchType.ipv4);

        salFlow = ToSalConversionsUtils.toFlow(prepareOdFlow(odNodeFlowBuilder, MtchType.ipv6), node);
        checkSalMatch(salFlow.getMatch(), MtchType.ipv6);

        salFlow = ToSalConversionsUtils.toFlow(prepareOdFlow(odNodeFlowBuilder, MtchType.arp), node);
        checkSalMatch(salFlow.getMatch(), MtchType.arp);

        salFlow = ToSalConversionsUtils.toFlow(prepareOdFlow(odNodeFlowBuilder, MtchType.sctp), node);
        checkSalMatch(salFlow.getMatch(), MtchType.sctp);

        salFlow = ToSalConversionsUtils.toFlow(prepareOdFlow(odNodeFlowBuilder, MtchType.tcp), node);
        checkSalMatch(salFlow.getMatch(), MtchType.tcp);

        salFlow = ToSalConversionsUtils.toFlow(prepareOdFlow(odNodeFlowBuilder, MtchType.udp), node);
        checkSalMatch(salFlow.getMatch(), MtchType.udp);

        checkSalFlow(salFlow);
    }

    /**
     * test of {@link ToSalConversionsUtils#fromNodeConnectorRef(Uri, Node)}
     * @throws ConstructionException
     */
    @Test
    public void testFromNodeConnectorRef() throws ConstructionException {
        Node node = new Node(NodeIDType.OPENFLOW, 42L);
        NodeConnector nodeConnector = ToSalConversionsUtils.fromNodeConnectorRef(new Uri("1"), node);
        assertEquals("OF|1@OF|00:00:00:00:00:00:00:2a", nodeConnector.toString());
    }

    @Test
    public void testActionFrom() throws ConstructionException {
        // Bug 2021: Convert AD-SAL notation into MD-SAL notation before calling NodeConnector
        Node node = new Node(NodeIDType.OPENFLOW, 42L);
        List<Action> odActions = new ArrayList<>();

        OutputActionBuilder outputActionBuilder = new OutputActionBuilder();
        outputActionBuilder.setOutputNodeConnector(new Uri("CONTROLLER"));
        OutputActionCaseBuilder outputActionCaseBuilder = new OutputActionCaseBuilder();
        outputActionCaseBuilder.setOutputAction(outputActionBuilder.build());
        odActions.add(new ActionBuilder().setAction(outputActionCaseBuilder.build()).build());

        List<org.opendaylight.controller.sal.action.Action> targetAction =
                ToSalConversionsUtils.actionFrom(odActions, node);
        assertNotNull(targetAction);
        assertTrue( Output.class.isInstance(targetAction.get(0)) );
        Output targetActionOutput = (Output) targetAction.get(0);
        NodeConnector port = targetActionOutput.getPort();
        assertNotNull(port);
        assertEquals(port.getType(), NodeConnectorIDType.CONTROLLER);
        assertEquals(port.getID(), org.opendaylight.controller.sal.core.NodeConnector.SPECIALNODECONNECTORID);
    }

    private void checkSalMatch(org.opendaylight.controller.sal.match.Match match, MtchType mt) throws ConstructionException {
        switch (mt) {
        case other:
            /*assertNotNull("DL_DST isn't equal.", "3C:A9:F4:00:E0:C8",
                    new String((byte[]) match.getField(MatchType.DL_DST).getValue()));
            assertEquals("DL_SRC isn't equal.", "24:77:03:7C:C5:F1",
                    new String((byte[]) match.getField(MatchType.DL_SRC).getValue()));
            */
            Node node = new Node(NodeIDType.OPENFLOW, 12L);
            NodeConnector port = new NodeConnector(NodeConnectorIDType.OPENFLOW, Short.valueOf((short)345), node);
            assertEquals("IN_PORT isn't equal.", port, match.getField(MatchType.IN_PORT).getValue());
            assertEquals("DL_TYPE isn't equal.", (short) 0xffff, (short) match.getField(MatchType.DL_TYPE).getValue());
            assertEquals("NW_TOS isn't equal.", (byte) 0x33, (byte) match.getField(MatchType.NW_TOS).getValue());
            assertEquals("NW_PROTO isn't equal.", (byte) 0x3f, (byte) match.getField(MatchType.NW_PROTO).getValue());
            assertEquals("DL_VLAN isn't equal.", (short) 0xfff, (short) match.getField(MatchType.DL_VLAN).getValue());
            assertEquals("DL_VLAN_PR isn't equal.", (byte) 0x7, (byte) match.getField(MatchType.DL_VLAN_PR).getValue());
            break;
        case untagged:
            assertEquals("DL_TYPE isn't equal.", (short) 0xffff, (short) match.getField(MatchType.DL_TYPE).getValue());
            assertEquals("NW_TOS isn't equal.", (byte) 0x33, (byte) match.getField(MatchType.NW_TOS).getValue());
            assertEquals("NW_PROTO isn't equal.", (byte) 0x3f, (byte) match.getField(MatchType.NW_PROTO).getValue());
            assertEquals("DL_VLAN isn't equal.", MatchType.DL_VLAN_NONE, (short) match.getField(MatchType.DL_VLAN).getValue());
            break;
        case arp:
            /*
            assertEquals("DL_SRC isn't equal.", "22:44:66:88:AA:CC",
                    new String((byte[]) match.getField(MatchType.DL_SRC).getValue()));
            assertEquals("DL_DST isn't equal.", "11:33:55:77:BB:DD",
                    new String((byte[]) match.getField(MatchType.DL_DST).getValue()));
            */
            assertEquals("NW_SRC isn't equal.", "192.168.1.101",
                    InetAddresses.toAddrString((InetAddress) match.getField(MatchType.NW_SRC).getValue()));
            assertEquals("NW_DST isn't equal.", "192.168.1.102",
                    InetAddresses.toAddrString((InetAddress) match.getField(MatchType.NW_DST).getValue()));
            assertEquals("DL_TYPE isn't equal.", ETHERNET_ARP, match.getField(MatchType.DL_TYPE).getValue());
            break;
        case ipv4:
            assertEquals("NW_SRC isn't equal.", "192.168.1.104",
                    InetAddresses.toAddrString((InetAddress) match.getField(MatchType.NW_SRC).getValue()));
            assertEquals("NW_DST isn't equal.", "192.168.1.105",
                    InetAddresses.toAddrString((InetAddress) match.getField(MatchType.NW_DST).getValue()));
            break;
        case ipv6:
            assertEquals("NW_SRC isn't equal.", "3001:db8:85a3::8a2e:370:7334",
                    InetAddresses.toAddrString((InetAddress) match.getField(MatchType.NW_SRC).getValue()));
            assertEquals("NW_DST isn't equal.", "3001:db8:85a3::8a2e:370:7335",
                    InetAddresses.toAddrString((InetAddress) match.getField(MatchType.NW_DST).getValue()));
            break;
        case sctp:
            assertEquals("TP_SRC isn't equal.", 31, (short) match.getField(MatchType.TP_SRC).getValue());
            assertEquals("TP_DST isn't equal.", 32, (short) match.getField(MatchType.TP_DST).getValue());
            assertEquals("NW_PROTO isn't equal.", CRUDP, (byte) match.getField(MatchType.NW_PROTO).getValue());
            break;
        case tcp:
            assertEquals("TP_SRC isn't equal.", 21, (short) match.getField(MatchType.TP_SRC).getValue());
            assertEquals("TP_DST isn't equal.", 22, (short) match.getField(MatchType.TP_DST).getValue());
            assertEquals("NW_PROTO isn't equal.", TCP, (byte) match.getField(MatchType.NW_PROTO).getValue());
            break;
        case udp:
            assertEquals("TP_SRC isn't equal.", 11, (short) match.getField(MatchType.TP_SRC).getValue());
            assertEquals("TP_DST isn't equal.", 12, (short) match.getField(MatchType.TP_DST).getValue());
            assertEquals("NW_PROTO isn't equal.", UDP, (byte) match.getField(MatchType.NW_PROTO).getValue());
            break;
        default:
            break;

        }

    }

    private void checkSalFlow(Flow salFlow) {
        assertTrue("Id value is incorrect.", salFlow.getId() == 9223372036854775807L);
        assertTrue("Hard timeout is incorrect.", salFlow.getHardTimeout() == 32767);
        assertTrue("Iddle timeout is incorrect.", salFlow.getIdleTimeout() == 32767);
        assertTrue("Priority value is incorrect.", salFlow.getPriority() == 32767);

        checkSalActions(salFlow.getActions());
    }

    private void checkSalActions(List<org.opendaylight.controller.sal.action.Action> actions) {
        checkSalAction(actions, Flood.class, 1);
        checkSalAction(actions, FloodAll.class, 1);
        checkSalAction(actions, HwPath.class, 1);
        checkSalAction(actions, Loopback.class, 1);
        checkSalAction(actions, Output.class, 1, true);
        checkSalAction(actions, PopVlan.class, 1);
        checkSalAction(actions, PushVlan.class, 1, true);
        checkSalAction(actions, SetDlDst.class, 1, true);
        checkSalAction(actions, SetDlSrc.class, 1, true);
        checkSalAction(actions, SetDlType.class, 1, true);
        checkSalAction(actions, SetNextHop.class, 2, true);
        checkSalAction(actions, SetNwDst.class, 2, true);
        checkSalAction(actions, SetNwSrc.class, 2, true);
        checkSalAction(actions, SetNwTos.class, 1, true);
        checkSalAction(actions, SetTpDst.class, 1, true);
        checkSalAction(actions, SetTpSrc.class, 1, true);
        checkSalAction(actions, SetVlanCfi.class, 1, true);
        checkSalAction(actions, SetVlanId.class, 1, true);
        checkSalAction(actions, SetVlanPcp.class, 1, true);
        checkSalAction(actions, SwPath.class, 1);
    }

    private void checkSalAction(List<org.opendaylight.controller.sal.action.Action> actions, Class<?> cls,
            int numOfActions) {
        checkSalAction(actions, cls, numOfActions, false);
    }

    private void checkSalAction(List<org.opendaylight.controller.sal.action.Action> actions, Class<?> cls,
            int numOfActions, boolean additionalCheck) {
        int numOfEqualClass = 0;
        for (org.opendaylight.controller.sal.action.Action action : actions) {
            if (action.getClass().equals(cls)) {
                if (additionalCheck) {
                    additionalActionCheck(action);
                }
                numOfEqualClass++;
            }
        }
        assertEquals("Incorrect number of actions of type " + cls.getName() + " was found.", numOfActions,
                numOfEqualClass);
    }

    // implement special checks
    private void additionalActionCheck(org.opendaylight.controller.sal.action.Action action) {
        if (action instanceof Output) {
            // ((Output)action).getPort() //TODO finish check when mapping will
            // be defined
        } else if (action instanceof PushVlan) {
            assertEquals("Wrong value for action PushVlan for tag.", 0x8100, ((PushVlan) action).getTag());
        } else if (action instanceof SetDlDst) {
            //assertEquals("Wrong value for action SetDlDst for MAC address.", "3C:A9:F4:00:E0:C8", new String(
            //        ((SetDlDst) action).getDlAddress()));
        } else if (action instanceof SetDlSrc) {
            //assertEquals("Wrong value for action SetDlSrc for MAC address.", "24:77:03:7C:C5:F1", new String(
            //      ((SetDlSrc) action).getDlAddress()));
        } else if (action instanceof SetDlType) {
            assertEquals("Wrong value for action SetDlType for.", 513L, ((SetDlType) action).getDlType());
        } else if (action instanceof SetNextHop) {
            InetAddress inetAddress = ((SetNextHop) action).getAddress();
            checkIpAddresses(inetAddress, "192.168.100.100", "2001:db8:85a3::8a2e:370:7334");
        } else if (action instanceof SetNwDst) {
            InetAddress inetAddress = ((SetNwDst) action).getAddress();
            checkIpAddresses(inetAddress, "192.168.100.101", "2001:db8:85a3::8a2e:370:7335");
        } else if (action instanceof SetNwSrc) {
            InetAddress inetAddress = ((SetNwSrc) action).getAddress();
            checkIpAddresses(inetAddress, "192.168.100.102", "2001:db8:85a3::8a2e:370:7336");
        } else if (action instanceof SetNwTos) {
            assertEquals("Wrong value for action SetNwTos for tos.", 63, ((SetNwTos) action).getNwTos());
        } else if (action instanceof SetTpDst) {
            assertEquals("Wrong value for action SetTpDst for port.", 65535, ((SetTpDst) action).getPort());
        } else if (action instanceof SetTpSrc) {
            assertEquals("Wrong value for action SetTpSrc for port.", 65535, ((SetTpSrc) action).getPort());
        } else if (action instanceof SetVlanCfi) {
            assertEquals("Wrong value for action SetVlanCfi for port.", 1, ((SetVlanCfi) action).getCfi());
        } else if (action instanceof SetVlanId) {
            assertEquals("Wrong value for action SetVlanId for vlan ID.", 4095, ((SetVlanId) action).getVlanId());
        } else if (action instanceof SetVlanPcp) {
            assertEquals("Wrong value for action SetVlanPcp for vlan ID.", 7, ((SetVlanPcp) action).getPcp());
        }
    }

    private void checkIpAddresses(InetAddress inetAddress, String ipv4, String ipv6) {
        if (inetAddress instanceof Inet4Address) {
            assertEquals("Wrong value for IP address.", ipv4, InetAddresses.toAddrString(inetAddress));
        } else if (inetAddress instanceof Inet6Address) {
            assertEquals("Wrong value for IP address.", ipv6, InetAddresses.toAddrString(inetAddress));
        }
    }

    private FlowAddedBuilder prepareOdFlowCommon() {
        FlowAddedBuilder odNodeFlowBuilder = new FlowAddedBuilder();

        odNodeFlowBuilder.setCookie(new FlowCookie(new BigInteger("9223372036854775807")));
        odNodeFlowBuilder.setHardTimeout(32767);
        odNodeFlowBuilder.setIdleTimeout(32767);
        odNodeFlowBuilder.setPriority(32767);
        odNodeFlowBuilder.setInstructions(prepareOdActions());
        return odNodeFlowBuilder;
    }

    private NodeFlow prepareOdFlow(FlowAddedBuilder odNodeFlowBuilder, MtchType mt) {
        odNodeFlowBuilder.setMatch(prepOdMatch(mt));
        return odNodeFlowBuilder.build();
    }

    private Instructions prepareOdActions() {
        List<Action> odActions = new ArrayList<>();

        ControllerActionCaseBuilder controllerActionBuilder = new ControllerActionCaseBuilder();
        DropActionCaseBuilder dropActionBuilder = new DropActionCaseBuilder();
        FloodActionCaseBuilder floodActionBuilder = new FloodActionCaseBuilder();
        FloodAllActionCaseBuilder floodAllActionBuilder = new FloodAllActionCaseBuilder();
        HwPathActionCaseBuilder hwPathActionBuilder = new HwPathActionCaseBuilder();
        LoopbackActionCaseBuilder loopbackActionBuilder = new LoopbackActionCaseBuilder();
        OutputActionCaseBuilder outputActionBuilder = new OutputActionCaseBuilder();
        PopMplsActionCaseBuilder popMplsActionBuilder = new PopMplsActionCaseBuilder();
        PopVlanActionCaseBuilder popVlanActionBuilder = new PopVlanActionCaseBuilder();
        PushMplsActionCaseBuilder pushMplsActionBuilder = new PushMplsActionCaseBuilder();
        PushPbbActionCaseBuilder pushPbbActionBuilder = new PushPbbActionCaseBuilder();
        PushVlanActionCaseBuilder pushVlanActionBuilder = new PushVlanActionCaseBuilder();
        SetDlDstActionCaseBuilder setDlDstActionBuilder = new SetDlDstActionCaseBuilder();
        SetDlSrcActionCaseBuilder setDlSrcActionBuilder = new SetDlSrcActionCaseBuilder();
        SetDlTypeActionCaseBuilder setDlTypeActionBuilder = new SetDlTypeActionCaseBuilder();
        SetMplsTtlActionCaseBuilder setMplsTtlActionBuilder = new SetMplsTtlActionCaseBuilder();
        SetNwTosActionCaseBuilder setNwTosActionBuilder = new SetNwTosActionCaseBuilder();
        SetNwTtlActionCaseBuilder setNwTtlActionBuilder = new SetNwTtlActionCaseBuilder();
        SetQueueActionCaseBuilder setQueueActionBuilder = new SetQueueActionCaseBuilder();
        SetTpDstActionCaseBuilder setTpDstActionBuilder = new SetTpDstActionCaseBuilder();
        SetTpSrcActionCaseBuilder setTpSrcActionBuilder = new SetTpSrcActionCaseBuilder();
        SetVlanCfiActionCaseBuilder setVlanCfiActionBuilder = new SetVlanCfiActionCaseBuilder();
        SetVlanIdActionCaseBuilder setVlanIdActionBuilder = new SetVlanIdActionCaseBuilder();
        SetVlanPcpActionCaseBuilder setVlanPcpActionBuilder = new SetVlanPcpActionCaseBuilder();
        SwPathActionCaseBuilder swPathActionBuilder = new SwPathActionCaseBuilder();

        prepareActionOutput(outputActionBuilder);
        prepareActionPushVlan(pushVlanActionBuilder);
        prepareActionSetDlDst(setDlDstActionBuilder);
        prepareActionSetDlSrc(setDlSrcActionBuilder);
        prepareActionSetDlType(setDlTypeActionBuilder);
        prepareActionNextHop(odActions);
        prepareActionSetNwDst(odActions);
        prepareActionSetNwSrc(odActions);
        prepareActionSetNwTos(setNwTosActionBuilder);
        prepareActionSetTpDst(setTpDstActionBuilder);
        prepareActionSetTpSrc(setTpSrcActionBuilder);
        prepareActionSetVlanCfi(setVlanCfiActionBuilder);
        prepareActionSetVladId(setVlanIdActionBuilder);
        prepareActionSetVlanPcp(setVlanPcpActionBuilder);

        odActions.add(new ActionBuilder().setAction(controllerActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(dropActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(floodActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(floodAllActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(hwPathActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(loopbackActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(outputActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(popMplsActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(popVlanActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(pushMplsActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(pushPbbActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(pushVlanActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(setDlDstActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(setDlSrcActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(setDlTypeActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(setMplsTtlActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(setNwTosActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(setNwTtlActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(setQueueActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(setTpDstActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(setTpSrcActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(setVlanCfiActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(setVlanIdActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(setVlanPcpActionBuilder.build()).build());
        odActions.add(new ActionBuilder().setAction(swPathActionBuilder.build()).build());


        ApplyActionsCase innerInst = new ApplyActionsCaseBuilder().setApplyActions(new ApplyActionsBuilder().setAction(odActions).build()).build();
        Instruction applyActions = new InstructionBuilder().setInstruction(innerInst).build();
        List<Instruction> instructions = Collections.singletonList(applyActions );
        InstructionsBuilder instBuilder = new InstructionsBuilder();

        instBuilder.setInstruction(instructions);

        return instBuilder.build();
    }

    private void prepareActionSetVlanPcp(SetVlanPcpActionCaseBuilder wrapper) {
        SetVlanPcpActionBuilder setVlanPcpActionBuilder = new SetVlanPcpActionBuilder();
        setVlanPcpActionBuilder.setVlanPcp(new VlanPcp((short) 7));
        wrapper.setSetVlanPcpAction(setVlanPcpActionBuilder.build());
    }

    private void prepareActionSetVladId(SetVlanIdActionCaseBuilder wrapper) {
        SetVlanIdActionBuilder setVlanIdActionBuilder = new SetVlanIdActionBuilder();
        setVlanIdActionBuilder.setVlanId(new VlanId(4095));
        wrapper.setSetVlanIdAction(setVlanIdActionBuilder.build());
    }

    private void prepareActionSetVlanCfi(SetVlanCfiActionCaseBuilder wrapper) {
        SetVlanCfiActionBuilder setVlanCfiActionBuilder = new SetVlanCfiActionBuilder();
        setVlanCfiActionBuilder.setVlanCfi(new VlanCfi(1));
        wrapper.setSetVlanCfiAction(setVlanCfiActionBuilder.build());
    }

    private void prepareActionSetTpDst(SetTpDstActionCaseBuilder wrapper) {
        SetTpDstActionBuilder setTpDstActionBuilder = new SetTpDstActionBuilder();
        setTpDstActionBuilder.setPort(new PortNumber(65535));
        wrapper.setSetTpDstAction(setTpDstActionBuilder.build());
    }

    private void prepareActionSetTpSrc(SetTpSrcActionCaseBuilder wrapper) {
        SetTpSrcActionBuilder setTpSrcActionBuilder = new SetTpSrcActionBuilder();
        setTpSrcActionBuilder.setPort(new PortNumber(65535));
        wrapper.setSetTpSrcAction(setTpSrcActionBuilder.build());
    }

    private void prepareActionSetNwTos(SetNwTosActionCaseBuilder wrapper) {
        SetNwTosActionBuilder setNwTosActionBuilder = new SetNwTosActionBuilder();
        setNwTosActionBuilder.setTos(252);
        wrapper.setSetNwTosAction(setNwTosActionBuilder.build());
    }

    private void prepareActionSetNwSrc(List<Action> odActions) {
        // test case for IPv4
        SetNwSrcActionBuilder setNwSrcActionBuilderIpv4 = new SetNwSrcActionBuilder();
        setNwSrcActionBuilderIpv4.setAddress(prapareIpv4Address("192.168.100.102"));
        odActions.add(new ActionBuilder().setAction(new SetNwSrcActionCaseBuilder().setSetNwSrcAction(setNwSrcActionBuilderIpv4.build()).build()).build());

        // test case for IPv6
        SetNwSrcActionBuilder setNwSrcActionBuilderIpv6 = new SetNwSrcActionBuilder();
        setNwSrcActionBuilderIpv6.setAddress(prapareIpv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7336"));
        odActions.add(new ActionBuilder().setAction(new SetNwSrcActionCaseBuilder().setSetNwSrcAction(setNwSrcActionBuilderIpv6.build()).build()).build());
    }

    private void prepareActionSetNwDst(List<Action> odActions) {
        // test case for IPv4

        SetNwDstActionBuilder setNwDstActionBuilderIpv4 = new SetNwDstActionBuilder();
        setNwDstActionBuilderIpv4.setAddress(prapareIpv4Address("192.168.100.101"));
        odActions.add(new ActionBuilder().setAction(new SetNwDstActionCaseBuilder().setSetNwDstAction(setNwDstActionBuilderIpv4.build()).build()).build());

        // test case for IPv6
        SetNwDstActionBuilder setNwDstActionBuilderIpv6 = new SetNwDstActionBuilder();
        setNwDstActionBuilderIpv6.setAddress(prapareIpv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7335"));
        odActions.add(new ActionBuilder().setAction(new SetNwDstActionCaseBuilder().setSetNwDstAction(setNwDstActionBuilderIpv6.build()).build()).build());
    }

    private void prepareActionNextHop(List<Action> odActions) {
        // test case for IPv4
        SetNextHopActionBuilder setNextHopActionBuilderIpv4 = new SetNextHopActionBuilder();
        setNextHopActionBuilderIpv4.setAddress(prapareIpv4Address("192.168.100.100"));
        odActions.add(new ActionBuilder().setAction(new SetNextHopActionCaseBuilder().setSetNextHopAction(setNextHopActionBuilderIpv4.build()).build()).build());

        // test case for IPv6
        SetNextHopActionBuilder setNextHopActionBuilderIpv6 = new SetNextHopActionBuilder();
        setNextHopActionBuilderIpv6.setAddress(prapareIpv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
        odActions.add(new ActionBuilder().setAction(new SetNextHopActionCaseBuilder().setSetNextHopAction(setNextHopActionBuilderIpv6.build()).build()).build());
    }

    private Address prapareIpv4Address(String ipv4Address) {
        Ipv4Builder ipv4Builder = new Ipv4Builder();
        ipv4Builder.setIpv4Address(new Ipv4Prefix(ipv4Address + "/32"));
        return ipv4Builder.build();
    }

    private Address prapareIpv6Address(String ipv6Address) {
        Ipv6Builder ipv6Builder = new Ipv6Builder();
        ipv6Builder.setIpv6Address(new Ipv6Prefix(ipv6Address));
        return ipv6Builder.build();
    }

    private void prepareActionSetDlType(SetDlTypeActionCaseBuilder wrapper) {
        SetDlTypeActionBuilder setDlTypeActionBuilder = new SetDlTypeActionBuilder();
        setDlTypeActionBuilder.setDlType(new EtherType(513L));
        wrapper.setSetDlTypeAction(setDlTypeActionBuilder.build());
    }

    private void prepareActionSetDlSrc(SetDlSrcActionCaseBuilder wrapper) {
        SetDlSrcActionBuilder setDlSrcActionBuilder = new SetDlSrcActionBuilder();
        setDlSrcActionBuilder.setAddress(new MacAddress("24:77:03:7C:C5:F1"));
        wrapper.setSetDlSrcAction(setDlSrcActionBuilder.build());
    }

    private void prepareActionSetDlDst(SetDlDstActionCaseBuilder wrapper) {
        SetDlDstActionBuilder setDlDstActionBuilder = new SetDlDstActionBuilder();
        setDlDstActionBuilder.setAddress(new MacAddress("3C:A9:F4:00:E0:C8"));
        wrapper.setSetDlDstAction(setDlDstActionBuilder.build());
    }

    private void prepareActionPushVlan(PushVlanActionCaseBuilder wrapper) {
        PushVlanActionBuilder pushVlanActionBuilder = new PushVlanActionBuilder();
        pushVlanActionBuilder.setTag(0x8100); // 12 bit
        wrapper.setPushVlanAction(pushVlanActionBuilder.build());
    }

    private void prepareActionOutput(OutputActionCaseBuilder wrapper) {
        OutputActionBuilder outputActionBuilder = new OutputActionBuilder();
        outputActionBuilder.setOutputNodeConnector(new Uri("1"));
        wrapper.setOutputAction(outputActionBuilder.build());
    }

    private Match prepOdMatch(MtchType mt) {
        MatchBuilder odMatchBuilder = new MatchBuilder();
        switch (mt) {
        case other:
            odMatchBuilder.setInPort(new NodeConnectorId("openflow:12:345"));
            odMatchBuilder.setEthernetMatch(prepEthernetMatch());
            odMatchBuilder.setIpMatch(prepIpMatch());
            odMatchBuilder.setVlanMatch(prepVlanMatch());
            break;
        case untagged:
            odMatchBuilder.setEthernetMatch(prepEthernetMatch());
            odMatchBuilder.setIpMatch(prepIpMatch());
            odMatchBuilder.setVlanMatch(prepVlanNoneMatch());
            break;
        case ipv4:
            odMatchBuilder.setLayer3Match(prepLayer3MatchIpv4());
            break;
        case ipv6:
            odMatchBuilder.setLayer3Match(prepLayer3MatchIpv6());
            break;
        case arp:
            odMatchBuilder.setLayer3Match(prepLayer3MatchArp());
            break;
        case sctp:
            odMatchBuilder.setLayer4Match(prepLayer4MatchSctp());
            break;
        case tcp:
            odMatchBuilder.setLayer4Match(prepLayer4MatchTcp());
            break;
        case udp:
            odMatchBuilder.setLayer4Match(prepLayer4MatchUdp());
            break;
        }
        return odMatchBuilder.build();
    }

    private Layer4Match prepLayer4MatchUdp() {
        UdpMatchBuilder udpMatchBuilder = new UdpMatchBuilder();

        udpMatchBuilder.setUdpSourcePort(new PortNumber(11));
        udpMatchBuilder.setUdpDestinationPort(new PortNumber(12));

        return udpMatchBuilder.build();
    }

    private Layer4Match prepLayer4MatchTcp() {
        TcpMatchBuilder tcpMatchBuilder = new TcpMatchBuilder();

        tcpMatchBuilder.setTcpSourcePort(new PortNumber(21));
        tcpMatchBuilder.setTcpDestinationPort(new PortNumber(22));

        return tcpMatchBuilder.build();
    }

    private Layer4Match prepLayer4MatchSctp() {
        SctpMatchBuilder sctpMatchBuilder = new SctpMatchBuilder();

        sctpMatchBuilder.setSctpSourcePort(new PortNumber(31));
        sctpMatchBuilder.setSctpDestinationPort(new PortNumber(32));

        return sctpMatchBuilder.build();
    }

    private Layer3Match prepLayer3MatchIpv4() {
        Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder();
        ipv4MatchBuilder.setIpv4Source(new Ipv4Prefix("192.168.1.104/32"));
        ipv4MatchBuilder.setIpv4Destination(new Ipv4Prefix("192.168.1.105/32"));
        return ipv4MatchBuilder.build();
    }

    private Layer3Match prepLayer3MatchIpv6() {
        Ipv6MatchBuilder ipv6MatchBuilder = new Ipv6MatchBuilder();
        ipv6MatchBuilder.setIpv6Source(new Ipv6Prefix("3001:0db8:85a3:0000:0000:8a2e:0370:7334"));
        ipv6MatchBuilder.setIpv6Destination(new Ipv6Prefix("3001:0db8:85a3:0000:0000:8a2e:0370:7335"));
        return ipv6MatchBuilder.build();
    }

    private Layer3Match prepLayer3MatchArp() {
        ArpMatchBuilder arpMatchBuilder = new ArpMatchBuilder();
        arpMatchBuilder.setArpSourceTransportAddress(new Ipv4Prefix("192.168.1.101/32"));
        arpMatchBuilder.setArpTargetTransportAddress(new Ipv4Prefix("192.168.1.102/32"));

        ArpSourceHardwareAddressBuilder arpSourAddressBuild = new ArpSourceHardwareAddressBuilder();
        arpSourAddressBuild.setAddress(new MacAddress("22:44:66:88:AA:CC"));
        arpMatchBuilder.setArpSourceHardwareAddress(arpSourAddressBuild.build());

        ArpTargetHardwareAddressBuilder arpTarAddressBuild = new ArpTargetHardwareAddressBuilder();
        arpTarAddressBuild.setAddress(new MacAddress("11:33:55:77:BB:DD"));
        arpMatchBuilder.setArpTargetHardwareAddress(arpTarAddressBuild.build());
        return arpMatchBuilder.build();
    }

    private VlanMatch prepVlanMatch() {
        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();

        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder().setVlanId(new VlanId(0xfff));
        vlanMatchBuilder.setVlanId(vlanIdBuilder.setVlanIdPresent(true).build());
        vlanMatchBuilder.setVlanPcp(new VlanPcp((short) 0x7));

        return vlanMatchBuilder.build();
    }

    private VlanMatch prepVlanNoneMatch() {
        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();

        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder().
            setVlanIdPresent(false);
        vlanMatchBuilder.setVlanId(vlanIdBuilder.build());

        return vlanMatchBuilder.build();
    }

    private IpMatch prepIpMatch() {
        IpMatchBuilder ipMatchBuilder = new IpMatchBuilder();
        ipMatchBuilder.setIpDscp(new Dscp((short) 0x33));
        ipMatchBuilder.setIpProtocol((short) 0x3f);
        return ipMatchBuilder.build();
    }

    private EthernetMatch prepEthernetMatch() {
        EthernetMatchBuilder odEthernetMatchBuilder = new EthernetMatchBuilder();
        odEthernetMatchBuilder.setEthernetDestination(prepEthDest());
        odEthernetMatchBuilder.setEthernetSource(prepEthSour());
        odEthernetMatchBuilder.setEthernetType(prepEthType());
        return odEthernetMatchBuilder.build();
    }

    private EthernetType prepEthType() {
        EthernetTypeBuilder ethTypeBuild = new EthernetTypeBuilder();
        ethTypeBuild.setType(new EtherType(0xffffL));
        return ethTypeBuild.build();
    }

    private EthernetSource prepEthSour() {
        EthernetSourceBuilder ethSourBuild = new EthernetSourceBuilder();
        ethSourBuild.setAddress(new MacAddress("24:77:03:7C:C5:F1"));
        return ethSourBuild.build();
    }

    private EthernetDestination prepEthDest() {
        EthernetDestinationBuilder ethDestBuild = new EthernetDestinationBuilder();
        ethDestBuild.setAddress(new MacAddress("3C:A9:F4:00:E0:C8"));
        return ethDestBuild.build();
    }
}
