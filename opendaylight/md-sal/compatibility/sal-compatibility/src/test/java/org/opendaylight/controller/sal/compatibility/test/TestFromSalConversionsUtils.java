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
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.ETHERNET_ARP;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.TCP;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.UDP;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.FloodAll;
import org.opendaylight.controller.sal.action.HwPath;
import org.opendaylight.controller.sal.action.Loopback;
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
import org.opendaylight.controller.sal.compatibility.MDFlowMapping;
import org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodAllActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.HwPathActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.LoopbackActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlTypeActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNextHopActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTosActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanCfiActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanPcpActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SwPathActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ArpSourceHardwareAddressCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ArpSourceTransportAddressCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ArpTargetHardwareAddressCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ArpTargetTransportAddressCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.EthernetDestinationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.EthernetSourceCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.EthernetTypeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.InPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.IpDscpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.IpProtocolCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv4DestinationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv4SourceCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv6DestinationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv6SourceCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.TcpDestinationPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.TcpSourcePortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.UdpDestinationPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.UdpSourcePortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.VlanMatchCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.arp.source.hardware.address._case.ArpSourceHardwareAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.arp.source.transport.address._case.ArpSourceTransportAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.arp.target.hardware.address._case.ArpTargetHardwareAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.arp.target.transport.address._case.ArpTargetTransportAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ethernet.destination._case.EthernetDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ethernet.source._case.EthernetSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ethernet.type._case.EthernetType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.in.port._case.InPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ip.dscp._case.IpDscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ip.protocol._case.IpProtocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv4.destination._case.Ipv4Destination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv4.source._case.Ipv4Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv6.destination._case.Ipv6Destination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv6.source._case.Ipv6Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.tcp.destination.port._case.TcpDestinationPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.tcp.source.port._case.TcpSourcePort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.udp.destination.port._case.UdpDestinationPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.udp.source.port._case.UdpSourcePort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.vlan.match._case.VlanMatch;

import com.google.common.net.InetAddresses;

public class TestFromSalConversionsUtils {
    private enum MtchType {
        other, untagged, ipv4, ipv6, arp, tcp, udp
    }

    @Test
    public void testFromSalConversion() throws ConstructionException {

        Flow salFlow = prepareSalFlowCommon();
        NodeFlow odNodeFlow = MDFlowMapping.flowAdded(salFlow);

        checkOdFlow(odNodeFlow);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.other));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.other);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.untagged));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.untagged);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.arp));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.arp);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.ipv4));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.ipv4);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.ipv6));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.ipv6);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.tcp));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.tcp);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.udp));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.udp);
    }

    private void checkOdMatch(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match m,
            MtchType mt) {
        EthernetSource ethSrc = null;
        EthernetDestination ethDst = null;
        EthernetType ethType = null;
        Ipv4Source ipv4src = null;
        Ipv4Destination ipv4dst = null;
        Ipv6Source ipv6src = null;
        Ipv6Destination ipv6dst = null;
        ArpSourceTransportAddress arpSrc = null;
        ArpTargetTransportAddress arpDst = null;
        ArpSourceHardwareAddress arpHwSrc = null;
        ArpTargetHardwareAddress arpHwDst = null;
        TcpSourcePort tcpSrc = null;
        TcpDestinationPort tcpDst = null;
        UdpSourcePort udpSrc = null;
        UdpDestinationPort udpDst = null;
        VlanMatch vlan = null;
        IpProtocol proto = null;
        IpDscp dscp = null;
        InPort inPort = null;
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.Match> matches = m.getMatch();
        for(org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.Match match : matches) {
            if(match.getMatch() instanceof EthernetSourceCase) {
                assertEquals("More than one EthernetSourceCase Seen",null, ethSrc);
                ethSrc = ((EthernetSourceCase) match.getMatch()).getEthernetSource();
            } else if(match.getMatch() instanceof EthernetDestinationCase) {
                assertEquals("More than one EthernetDestinationCase Seen",null, ethDst);
                ethDst = ((EthernetDestinationCase) match.getMatch()).getEthernetDestination();
            } else if(match.getMatch() instanceof EthernetTypeCase) {
                assertEquals("More than one EthernetTypeCase Seen",null, ethType);
                ethType = ((EthernetTypeCase) match.getMatch()).getEthernetType();
            } else if (match.getMatch() instanceof VlanMatchCase) {
                assertEquals("More than one VlanMatchCase Seen",null, vlan);
                vlan = ((VlanMatchCase) match.getMatch()).getVlanMatch();
            } else if (match.getMatch() instanceof Ipv4SourceCase) {
                assertEquals("More than one Ipv4SourceCase Seen",null, ipv4src);
                ipv4src = ((Ipv4SourceCase) match.getMatch()).getIpv4Source();
            } else if (match.getMatch() instanceof Ipv4DestinationCase) {
                assertEquals("More than one Ipv4DestinationCase Seen",null, ipv4dst);
                ipv4dst = ((Ipv4DestinationCase) match.getMatch()).getIpv4Destination();
            } else if (match.getMatch() instanceof Ipv6SourceCase) {
                assertEquals("More than one Ipv6SourceCase Seen",null, ipv6src);
                ipv6src = ((Ipv6SourceCase) match.getMatch()).getIpv6Source();
            } else if (match.getMatch() instanceof Ipv6DestinationCase) {
                assertEquals("More than one Ipv6DestinationCase Seen",null, ipv6dst);
                ipv6dst = ((Ipv6DestinationCase) match.getMatch()).getIpv6Destination();
            } else if (match.getMatch() instanceof ArpSourceTransportAddressCase) {
                assertEquals("More than one ArpSourceTransportAddressCase Seen",null, arpSrc);
                arpSrc = ((ArpSourceTransportAddressCase) match.getMatch()).getArpSourceTransportAddress();
            } else if (match.getMatch() instanceof ArpTargetTransportAddressCase) {
                assertEquals("More than one ArpTargetTransportAddressCase Seen",null, arpDst);
                arpDst = ((ArpTargetTransportAddressCase) match.getMatch()).getArpTargetTransportAddress();
            } else if (match.getMatch() instanceof ArpSourceHardwareAddressCase) {
                assertEquals("More than one ArpSourceHardwareAddressCase Seen",null, arpHwSrc);
                arpHwSrc = ((ArpSourceHardwareAddressCase) match.getMatch()).getArpSourceHardwareAddress();
            } else if (match.getMatch() instanceof ArpTargetHardwareAddressCase) {
                assertEquals("More than one ArpTargetHardwareAddressCase Seen",null, arpHwDst);
                arpHwDst = ((ArpTargetHardwareAddressCase) match.getMatch()).getArpTargetHardwareAddress();
            } else if (match.getMatch() instanceof TcpSourcePortCase) {
                assertEquals("More than one TcpSourcePortCase Seen",null, tcpSrc);
                tcpSrc = ((TcpSourcePortCase) match.getMatch()).getTcpSourcePort();
            } else if (match.getMatch() instanceof TcpDestinationPortCase) {
                assertEquals("More than one TcpDestinationPortCase Seen",null, tcpDst);
                tcpDst = ((TcpDestinationPortCase) match.getMatch()).getTcpDestinationPort();
            } else if (match.getMatch() instanceof UdpSourcePortCase) {
                assertEquals("More than one UdpSourcePortCase Seen",null, udpSrc);
                udpSrc = ((UdpSourcePortCase) match.getMatch()).getUdpSourcePort();
            } else if (match.getMatch() instanceof UdpDestinationPortCase) {
                assertEquals("More than one TcpDestinationPortCase Seen",null, udpDst);
                udpDst = ((UdpDestinationPortCase) match.getMatch()).getUdpDestinationPort();
            } else if (match.getMatch() instanceof IpProtocolCase) {
                assertEquals("More than one IpProtocolCase Seen",null, proto);
                proto = ((IpProtocolCase) match.getMatch()).getIpProtocol();
            } else if (match.getMatch() instanceof IpDscpCase) {
                assertEquals("More than one IpDscpCase Seen",null, dscp);
                dscp = ((IpDscpCase) match.getMatch()).getIpDscp();
            } else if (match.getMatch() instanceof InPortCase) {
                assertEquals("More than one InPortCase Seen",null, inPort);
                inPort = ((InPortCase) match.getMatch()).getInPort();
            } else {
                assertTrue("Extra matches found", false);
            }
        }
        switch (mt) {

        case arp:
            boolean arpFound = false;
            assertEquals("Ether type is incorrect.", ETHERNET_ARP, (long) ethType.getType().getValue());
            assertEquals("Source IP address is wrong.", "192.168.100.100", arpSrc
                    .getArpSourceTransportAddress().getValue());
            assertEquals("Destination IP address is wrong.", "192.168.100.101", arpDst
                    .getArpTargetTransportAddress().getValue());
            assertEquals("Source MAC address is wrong.", "ff:ee:dd:cc:bb:aa", arpHwSrc
                    .getAddress().getValue());
            assertEquals("Destination MAC address is wrong.", "ff:ee:dd:cc:bb:aa", arpHwDst
                    .getAddress().getValue());
            arpFound = true;

            assertNotNull("Arp wasn't found", arpFound);
            break;
        case ipv4:
            assertEquals("Ether type is incorrect.", 0xffff, (long) ethType.getType().getValue());
            boolean ipv4Found = false;
            assertEquals("Source IP address is wrong.", "192.168.100.102", ipv4src
                    .getIpv4Source().getValue());
            assertEquals("Destination IP address is wrong.", "192.168.100.103", ipv4dst
                    .getIpv4Destination().getValue());
            ipv4Found = true;
            assertNotNull("Ipv4 wasn't found", ipv4Found);
            break;
        case ipv6:
            assertEquals("Ether type is incorrect.", 0xffff, (long) ethType.getType().getValue());
            boolean ipv6Found = false;
            assertEquals("Source IP address is wrong.", "2001:db8:85a3::8a2e:370:7335", ipv6src
                    .getIpv6Source().getValue());
            assertEquals("Destination IP address is wrong.", "2001:db8:85a3::8a2e:370:7336",
                    ipv6dst.getIpv6Destination().getValue());
            ipv6Found = true;
            assertNotNull("Ipv6 wasn't found", ipv6Found);
            break;
        case other:
            assertEquals("Incoming port is wrong.", "openflow:12345:10", inPort.getInPort().getValue());
            assertEquals("Source MAC address is wrong.", "ff:ee:dd:cc:bb:aa", ethSrc.getAddress().getValue());
            assertEquals("Destination MAC address is wrong.", "ff:ee:dd:cc:bb:aa", ethDst.getAddress().getValue());
            assertEquals("Vlan ID is not present.", Boolean.TRUE, vlan.getVlanId().isVlanIdPresent());
            assertEquals("Vlan ID is wrong.", (Integer) 0xfff, vlan.getVlanId().getVlanId().getValue());
            assertEquals("Vlan ID priority is wrong.", (short) 0x7, (short) vlan.getVlanPcp()
                    .getValue());
            assertEquals("DCSP is wrong.", (short) 0x3f, (short) dscp.getIpDscp().getValue());
            break;
        case untagged:
            assertEquals("Source MAC address is wrong.", "ff:ee:dd:cc:bb:aa", ethSrc.getAddress().getValue());
            assertEquals("Destinatio MAC address is wrong.", "ff:ee:dd:cc:bb:aa", ethDst.getAddress().getValue());
            assertEquals("Vlan ID is present.", Boolean.FALSE, vlan.getVlanId().isVlanIdPresent());
            assertEquals("Vlan ID is wrong.", Integer.valueOf(0), vlan.getVlanId().getVlanId().getValue());
            assertEquals("DCSP is wrong.", (short) 0x3f, (short) dscp.getIpDscp().getValue());
            break;
        case tcp:
            assertEquals("Wrong protocol", TCP, proto.getIpProtocol().byteValue());
            assertEquals("Tcp source port is incorrect.", 0xabcd, (int) tcpSrc
                    .getTcpSourcePort().getValue());
            assertEquals("Tcp dest port is incorrect.", 0xdcba, (int) tcpDst
                    .getTcpDestinationPort().getValue());
            break;
        case udp:
            assertEquals("Wrong protocol", UDP, proto.getIpProtocol().byteValue());
            assertEquals("Udp source port is incorrect.", 0xcdef, (int) udpSrc
                    .getUdpSourcePort().getValue());
            assertEquals("Udp dest port is incorrect.", 0xfedc, (int) udpDst
                    .getUdpDestinationPort().getValue());
            break;
        }

    }

    private void checkOdFlow(NodeFlow odNodeFlow) {
        assertEquals("Cookie is incorrect.", 9223372036854775807L, odNodeFlow.getCookie().getValue().longValue());
        assertEquals("Hard timeout is incorrect.", 32765, odNodeFlow.getHardTimeout().shortValue());
        assertEquals("Iddle timeout is incorrect.", 32766, odNodeFlow.getIdleTimeout().shortValue());
        assertEquals("Priority is incorrect.", 32767, odNodeFlow.getPriority().shortValue());

        checkOdActions(ToSalConversionsUtils.getAction(odNodeFlow));
    }

    private void checkOdActions(
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions) {
        checkOdAction(actions, FloodActionCase.class);
        checkOdAction(actions, FloodAllActionCase.class);
        checkOdAction(actions, HwPathActionCase.class);
        checkOdAction(actions, LoopbackActionCase.class);
        checkOdAction(actions, PopVlanActionCase.class);
        checkOdAction(actions, PushVlanActionCase.class);
        checkOdAction(actions, SetDlDstActionCase.class);
        checkOdAction(actions, SetDlSrcActionCase.class);
        checkOdAction(actions, SetDlTypeActionCase.class);
        checkOdAction(actions, SetNwTosActionCase.class);
        checkOdAction(actions, SetNwDstActionCase.class);
        checkOdAction(actions, SetNwSrcActionCase.class);
        checkOdAction(actions, SetNextHopActionCase.class);
        checkOdAction(actions, SetTpDstActionCase.class);
        checkOdAction(actions, SetTpSrcActionCase.class);
        checkOdAction(actions, SetVlanCfiActionCase.class);
        checkOdAction(actions, SetVlanIdActionCase.class);
        checkOdAction(actions, SetVlanPcpActionCase.class);
        checkOdAction(actions, SwPathActionCase.class);
    }

    private void checkOdAction(
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions, Class<?> cl) {
        int numOfFoundActions = 0;
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action action : actions) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action innerAction = action
                    .getAction();
            if (cl.isInstance(innerAction)) {
                numOfFoundActions++;
                if (innerAction instanceof PushVlanActionCase) {
                    assertEquals("Wrong value of cfi in PushVlanAction.", (Integer) 1, ((PushVlanActionCase) innerAction).getPushVlanAction()
                            .getCfi().getValue());
                    assertEquals("Wrong value of pcp in PushVlanAction.", (Integer) 7,
                            ((PushVlanActionCase) innerAction).getPushVlanAction().getPcp());
                    assertEquals("Wrong value of tag in PushVlanAction.", (Integer) 0x8100,
                            ((PushVlanActionCase) innerAction).getPushVlanAction().getTag());
                    assertEquals("Wrong value of vlad ID in PushVlanAction.", (Integer) 4095,
                            ((PushVlanActionCase) innerAction).getPushVlanAction().getVlanId().getValue());
                } else if (innerAction instanceof SetDlDstActionCase) {
                    assertEquals("Wrong MAC destination address in SetDlDstAction.", "ff:ee:dd:cc:bb:aa",
                            ((SetDlDstActionCase) innerAction).getSetDlDstAction().getAddress().getValue());
                } else if (innerAction instanceof SetDlSrcActionCase) {
                    assertEquals("Wrong MAC source address in SetDlDstAction.", "ff:ee:dd:cc:bb:aa",
                            ((SetDlSrcActionCase) innerAction).getSetDlSrcAction().getAddress().getValue());
                } else if (innerAction instanceof SetDlTypeActionCase) {
                    assertEquals("Wrong data link type in SetDlTypeAction.", 513,
                            (long) ((SetDlTypeActionCase) innerAction).getSetDlTypeAction().getDlType().getValue());
                } else if (innerAction instanceof SetNextHopActionCase) {
                    Address address = ((SetNextHopActionCase) innerAction).getSetNextHopAction().getAddress();
                    boolean ipv4AddressFound = false;
                    if (address instanceof Ipv4) {
                        ipv4AddressFound = true;
                        assertEquals("Wrong IP address type in SetNextHopAction.", "192.168.100.100", ((Ipv4) address)
                                .getIpv4Address().getValue());
                    }
                    assertTrue("Ipv4 address wasn't found.", ipv4AddressFound);
                } else if (innerAction instanceof SetNwTosActionCase) {
                    assertEquals("Wrong TOS in SetNwTosAction.", (Integer) 63, ((SetNwTosActionCase) innerAction).getSetNwTosAction().getTos());
                } else if (innerAction instanceof SetNwDstActionCase) {
                    Address address = ((SetNwDstActionCase) innerAction).getSetNwDstAction().getAddress();
                    boolean ipv4AddressFound = false;
                    if (address instanceof Ipv4) {
                        ipv4AddressFound = true;
                        assertEquals("Wrong IP address type in SetNwDstAction.", "192.168.100.101", ((Ipv4) address)
                                .getIpv4Address().getValue());
                    }
                    assertTrue("Ipv4 address wasn't found.", ipv4AddressFound);
                } else if (innerAction instanceof SetNwSrcActionCase) {
                    Address address = ((SetNwSrcActionCase) innerAction).getSetNwSrcAction().getAddress();
                    boolean ipv4AddressFound = false;
                    if (address instanceof Ipv4) {
                        ipv4AddressFound = true;
                        assertEquals("Wrong IP address type in SetNwSrcAction.", "192.168.100.102", ((Ipv4) address)
                                .getIpv4Address().getValue());
                    }
                    assertTrue("Ipv4 address wasn't found.", ipv4AddressFound);
                } else if (innerAction instanceof SetTpDstActionCase) {
                    assertEquals("Port number is incorrect in SetTpDstAction.", (Integer) 65534,
                            ((SetTpDstActionCase) innerAction).getSetTpDstAction().getPort().getValue());
                } else if (innerAction instanceof SetTpSrcActionCase) {
                    assertEquals("Port number is incorrect in SetTpSrcAction.", (Integer) 65535,
                            ((SetTpSrcActionCase) innerAction).getSetTpSrcAction().getPort().getValue());
                } else if (innerAction instanceof SetVlanCfiActionCase) {
                    assertEquals("Vlan cfi number is incorrect in SetVlanCfiAction.", (Integer) 1,
                            ((SetVlanCfiActionCase) innerAction).getSetVlanCfiAction().getVlanCfi().getValue());
                } else if (innerAction instanceof SetVlanIdActionCase) {
                    assertEquals("Vlan id number is incorrect in SetVlanIdAction.", (Integer) 4095,
                            ((SetVlanIdActionCase) innerAction).getSetVlanIdAction().getVlanId().getValue());
                } else if (innerAction instanceof SetVlanPcpActionCase) {
                    assertEquals("Vlan pcp number is incorrect in SetVlanPcpAction.", new Short((short) 7),
                            ((SetVlanPcpActionCase) innerAction).getSetVlanPcpAction().getVlanPcp().getValue());
                }
            }
        }
        assertEquals("Incorrrect number of action " + cl.getName() + ".", 1, numOfFoundActions);

    }

    private Flow prepareSalFlowCommon() {
        Flow salFlow = new Flow();
        salFlow.setId(9223372036854775807L);
        salFlow.setHardTimeout((short) 32765);
        salFlow.setIdleTimeout((short) 32766);
        salFlow.setPriority((short) 32767);
        salFlow.setActions(prepareSalActions());
        salFlow.setMatch(new Match());

        return salFlow;
    }

    private Flow prepareSalMatch(Flow salFlow, MtchType mt) throws ConstructionException {
        Match salMatch = new Match();
        switch (mt) {
        case arp:
            salMatch.setField(MatchType.DL_TYPE, ETHERNET_ARP);
            salMatch.setField(MatchType.NW_SRC, InetAddresses.forString("192.168.100.100"));
            salMatch.setField(MatchType.NW_DST, InetAddresses.forString("192.168.100.101"));
            salMatch.setField(MatchType.DL_SRC, new byte[]{(byte )0xff,(byte )0xee,(byte )0xdd,(byte )0xcc,(byte )0xbb,(byte )0xaa});
            salMatch.setField(MatchType.DL_DST, new byte[]{(byte )0xff,(byte )0xee,(byte )0xdd,(byte )0xcc,(byte )0xbb,(byte )0xaa});
            break;
        case ipv4:
            salMatch.setField(MatchType.DL_TYPE, (short) 0xffff);
            salMatch.setField(MatchType.NW_SRC, InetAddresses.forString("192.168.100.102"));
            salMatch.setField(MatchType.NW_DST, InetAddresses.forString("192.168.100.103"));
            break;
        case ipv6:
            salMatch.setField(MatchType.DL_TYPE, (short) 0xffff);
            salMatch.setField(MatchType.NW_SRC, InetAddresses.forString("2001:0db8:85a3:0000:0000:8a2e:0370:7335"));
            salMatch.setField(MatchType.NW_DST, InetAddresses.forString("2001:0db8:85a3:0000:0000:8a2e:0370:7336"));
            break;
        case other:
            Node node = new Node(NodeIDType.OPENFLOW, 12345L);
            NodeConnector port = new NodeConnector(NodeConnectorIDType.OPENFLOW, Short.valueOf((short)10), node);
            salMatch.setField(MatchType.IN_PORT, port);
            salMatch.setField(MatchType.DL_SRC, new byte[]{(byte )0xff,(byte )0xee,(byte )0xdd,(byte )0xcc,(byte )0xbb,(byte )0xaa});
            salMatch.setField(MatchType.DL_DST, new byte[]{(byte )0xff,(byte )0xee,(byte )0xdd,(byte )0xcc,(byte )0xbb,(byte )0xaa});
            salMatch.setField(MatchType.DL_VLAN, (short) 0xfff);
            salMatch.setField(MatchType.DL_VLAN_PR, (byte) 0x7);
            salMatch.setField(MatchType.NW_TOS, (byte) 0x3f);
            break;
        case untagged:
            salMatch.setField(MatchType.DL_SRC, new byte[]{(byte )0xff,(byte )0xee,(byte )0xdd,(byte )0xcc,(byte )0xbb,(byte )0xaa});
            salMatch.setField(MatchType.DL_DST, new byte[]{(byte )0xff,(byte )0xee,(byte )0xdd,(byte )0xcc,(byte )0xbb,(byte )0xaa});
            salMatch.setField(MatchType.DL_VLAN, MatchType.DL_VLAN_NONE);
            salMatch.setField(MatchType.NW_TOS, (byte) 0x3f);
            break;
        case tcp:
            salMatch.setField(MatchType.NW_PROTO, TCP);
            salMatch.setField(MatchType.TP_SRC, (short) 0xabcd);
            salMatch.setField(MatchType.TP_DST, (short) 0xdcba);
            break;
        case udp:
            salMatch.setField(MatchType.NW_PROTO, UDP);
            salMatch.setField(MatchType.TP_SRC, (short) 0xcdef);
            salMatch.setField(MatchType.TP_DST, (short) 0xfedc);
            break;
        default:
            break;

        }

        salFlow.setMatch(salMatch);
        return salFlow;
    }

    private List<Action> prepareSalActions() {
        List<Action> salActions = new ArrayList<>();
        salActions.add(new Flood());
        salActions.add(new FloodAll());
        salActions.add(new HwPath());
        salActions.add(new Loopback());
        // salActions.add(new Output //TODO: mapping is missing
        salActions.add(new PopVlan());
        salActions.add(new PushVlan(0x8100, 7, 1, 4095));
        salActions.add(new SetDlDst(new byte[]{(byte )0xff,(byte )0xee,(byte )0xdd,(byte )0xcc,(byte )0xbb,(byte )0xaa}));
        salActions.add(new SetDlSrc(new byte[]{(byte )0xff,(byte )0xee,(byte )0xdd,(byte )0xcc,(byte )0xbb,(byte )0xaa}));
        salActions.add(new SetDlType(513));
        salActions.add(new SetNextHop(InetAddresses.forString("192.168.100.100")));
        salActions.add(new SetNwDst(InetAddresses.forString("192.168.100.101")));
        salActions.add(new SetNwSrc(InetAddresses.forString("192.168.100.102")));
        salActions.add(new SetNwTos(63));
        salActions.add(new SetTpDst(65534));
        salActions.add(new SetTpSrc(65535));
        salActions.add(new SetVlanCfi(1));
        salActions.add(new SetVlanId(4095));
        salActions.add(new SetVlanPcp(7));
        salActions.add(new SwPath());

        return salActions;
    }

}
