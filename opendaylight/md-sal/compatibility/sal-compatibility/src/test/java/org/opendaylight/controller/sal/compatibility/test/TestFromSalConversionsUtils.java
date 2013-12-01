/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
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
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.CRUDP;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.TCP;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.UDP;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.sal.action.*;
import org.opendaylight.controller.sal.compatibility.MDFlowMapping;
import org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatch;

import com.google.common.net.InetAddresses;

public class TestFromSalConversionsUtils {
    private enum MtchType {
        other, ipv4, ipv6, arp, sctp, tcp, udp
    }

    @Test
    public void testFromSalConversion() {

        Flow salFlow = prepareSalFlowCommon();
        NodeFlow odNodeFlow = MDFlowMapping.flowAdded(salFlow);

        checkOdFlow(odNodeFlow);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.other));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.other);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.arp));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.arp);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.ipv4));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.ipv4);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.ipv6));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.ipv6);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.sctp));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.sctp);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.tcp));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.tcp);

        odNodeFlow = MDFlowMapping.flowAdded(prepareSalMatch(salFlow, MtchType.udp));
        checkOdMatch(odNodeFlow.getMatch(), MtchType.udp);
    }

    private void checkOdMatch(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match match,
            MtchType mt) {
        switch (mt) {
        case arp:
            assertEquals("Ether type is incorrect.", ETHERNET_ARP, (long) match.getEthernetMatch().getEthernetType()
                    .getType().getValue());
            Layer3Match layer3Match = match.getLayer3Match();
            boolean arpFound = false;
            if (layer3Match instanceof ArpMatch) {
                assertEquals("Source IP address is wrong.", "192.168.100.100", ((ArpMatch) layer3Match)
                        .getArpSourceTransportAddress().getValue());
                assertEquals("Destination IP address is wrong.", "192.168.100.101", ((ArpMatch) layer3Match)
                        .getArpTargetTransportAddress().getValue());
                assertEquals("Source MAC address is wrong.", "ff:ee:dd:cc:bb:aa", ((ArpMatch) layer3Match)
                        .getArpSourceHardwareAddress().getAddress().getValue());
                assertEquals("Destination MAC address is wrong.", "ff:ee:dd:cc:bb:aa", ((ArpMatch) layer3Match)
                        .getArpTargetHardwareAddress().getAddress().getValue());
                arpFound = true;
            }
            assertNotNull("Arp wasn't found", arpFound);
            break;
        case ipv4:
            assertEquals("Ether type is incorrect.", 0xffff, (long) match.getEthernetMatch().getEthernetType()
                    .getType().getValue());
            boolean ipv4Found = false;
            layer3Match = match.getLayer3Match();
            if (layer3Match instanceof Ipv4Match) {
                assertEquals("Source IP address is wrong.", "192.168.100.102", ((Ipv4Match) layer3Match)
                        .getIpv4Source().getValue());
                assertEquals("Destination IP address is wrong.", "192.168.100.103", ((Ipv4Match) layer3Match)
                        .getIpv4Destination().getValue());
            }
            assertNotNull("Ipv4 wasn't found", ipv4Found);
            break;
        case ipv6:
            assertEquals("Ether type is incorrect.", 0xffff, (long) match.getEthernetMatch().getEthernetType()
                    .getType().getValue());
            boolean ipv6Found = false;
            layer3Match = match.getLayer3Match();
            if (layer3Match instanceof Ipv6Match) {
                assertEquals("Source IP address is wrong.", "2001:db8:85a3::8a2e:370:7335", ((Ipv6Match) layer3Match)
                        .getIpv6Source().getValue());
                assertEquals("Destination IP address is wrong.", "2001:db8:85a3::8a2e:370:7336",
                        ((Ipv6Match) layer3Match).getIpv6Destination().getValue());
            }
            assertNotNull("Ipv6 wasn't found", ipv6Found);
            break;
        case other:
            assertEquals("Source MAC address is wrong.", "ff:ee:dd:cc:bb:aa", match.getEthernetMatch()
                    .getEthernetSource().getAddress().getValue());
            assertEquals("Destinatio MAC address is wrong.", "ff:ee:dd:cc:bb:aa", match.getEthernetMatch()
                    .getEthernetDestination().getAddress().getValue());
            assertEquals("Vlan ID is wrong.", (Integer) 0xfff, match.getVlanMatch().getVlanId().getVlanId().getValue());
            assertEquals("Vlan ID priority is wrong.", (short) 0x7, (short) match.getVlanMatch().getVlanPcp()
                    .getValue());
            assertEquals("DCSP is wrong.", (short) 0x3f, (short) match.getIpMatch().getIpDscp().getValue());
            break;
        case sctp:
            boolean sctpFound = false;
            assertEquals("Wrong protocol", CRUDP, match.getIpMatch().getIpProtocol().byteValue());
            Layer4Match layer4Match = match.getLayer4Match();
            if (layer4Match instanceof SctpMatch) {
                assertEquals("Sctp source port is incorrect.", 0xffff, (int) ((SctpMatch) layer4Match)
                        .getSctpSourcePort().getValue());
                assertEquals("Sctp dest port is incorrect.", (int) 0xfffe, (int) ((SctpMatch) layer4Match)
                        .getSctpDestinationPort().getValue());
                sctpFound = true;
            }
            assertNotNull("Sctp wasn't found", sctpFound);
            break;
        case tcp:
            boolean tcpFound = false;
            assertEquals("Wrong protocol", TCP, match.getIpMatch().getIpProtocol().byteValue());
            layer4Match = match.getLayer4Match();
            if (layer4Match instanceof TcpMatch) {
                assertEquals("Tcp source port is incorrect.", (int) 0xabcd, (int) ((TcpMatch) layer4Match)
                        .getTcpSourcePort().getValue());
                assertEquals("Tcp dest port is incorrect.", (int) 0xdcba, (int) ((TcpMatch) layer4Match)
                        .getTcpDestinationPort().getValue());
                sctpFound = true;
            }
            assertNotNull("Tcp wasn't found", tcpFound);
            break;
        case udp:
            boolean udpFound = false;
            assertEquals("Wrong protocol", UDP, match.getIpMatch().getIpProtocol().byteValue());
            layer4Match = match.getLayer4Match();
            if (layer4Match instanceof UdpMatch) {
                assertEquals("Udp source port is incorrect.", (int) 0xcdef, (int) ((UdpMatch) layer4Match)
                        .getUdpSourcePort().getValue());
                assertEquals("Udp dest port is incorrect.", (int) 0xfedc, (int) ((UdpMatch) layer4Match)
                        .getUdpDestinationPort().getValue());
                sctpFound = true;
            }
            assertNotNull("Udp wasn't found", udpFound);
            break;
        }

    }

    private void checkOdFlow(NodeFlow odNodeFlow) {
        assertEquals("Cookie is incorrect.", 9223372036854775807L, odNodeFlow.getCookie().longValue());
        assertEquals("Hard timeout is incorrect.", 32765, odNodeFlow.getHardTimeout().shortValue());
        assertEquals("Iddle timeout is incorrect.", 32766, odNodeFlow.getIdleTimeout().shortValue());
        assertEquals("Priority is incorrect.", 32767, odNodeFlow.getPriority().shortValue());

        checkOdActions(ToSalConversionsUtils.getAction(odNodeFlow));
    }

    private void checkOdActions(
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions) {
        checkOdAction(actions, FloodActionCase.class, false);
        checkOdAction(actions, FloodAllActionCase.class, false);
        checkOdAction(actions, HwPathActionCase.class, false);
        checkOdAction(actions, LoopbackActionCase.class, false);
        checkOdAction(actions, PopVlanActionCase.class, false);
        checkOdAction(actions, PushVlanActionCase.class, true);
        checkOdAction(actions, SetDlDstActionCase.class, true);
        checkOdAction(actions, SetDlSrcActionCase.class, true);
        checkOdAction(actions, SetDlTypeActionCase.class, true);
        checkOdAction(actions, SetNwTosActionCase.class, true);
        checkOdAction(actions, SetNwDstActionCase.class, true);
        checkOdAction(actions, SetNwSrcActionCase.class, true);
        checkOdAction(actions, SetNextHopActionCase.class, true);
        checkOdAction(actions, SetTpDstActionCase.class, true);
        checkOdAction(actions, SetTpSrcActionCase.class, true);
        checkOdAction(actions, SetVlanCfiActionCase.class, true);
        checkOdAction(actions, SetVlanIdActionCase.class, true);
        checkOdAction(actions, SetVlanPcpActionCase.class, true);
        checkOdAction(actions, SwPathActionCase.class, false);
    }

    private void checkOdAction(
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions, Class<?> cl,
            boolean b) {
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
                    assertEquals("Wrong data link type in SetDlTypeAction.", (long) 513,
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

    private Flow prepareSalMatch(Flow salFlow, MtchType mt) {
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
            salMatch.setField(MatchType.DL_SRC, new byte[]{(byte )0xff,(byte )0xee,(byte )0xdd,(byte )0xcc,(byte )0xbb,(byte )0xaa});
            salMatch.setField(MatchType.DL_DST, new byte[]{(byte )0xff,(byte )0xee,(byte )0xdd,(byte )0xcc,(byte )0xbb,(byte )0xaa});
            salMatch.setField(MatchType.DL_VLAN, (short) 0xfff);
            salMatch.setField(MatchType.DL_VLAN_PR, (byte) 0x7);
            salMatch.setField(MatchType.NW_TOS, (byte) 0x3f);
            break;
        case sctp:
            salMatch.setField(MatchType.NW_PROTO, CRUDP);
            salMatch.setField(MatchType.TP_SRC, (short) 0xffff);
            salMatch.setField(MatchType.TP_DST, (short) 0xfffe);
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
