package org.opendaylight.controller.sal.compability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.sal.action.*;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAddedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.VlanCfi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.action.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.address.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.address.address.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.arp.match.fields.ArpSourceHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.arp.match.fields.ArpTargetHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.ethernet.match.fields.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._4.match.SctpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.vlan.match.fields.VlanIdBuilder;

import com.google.common.net.InetAddresses;
import static org.opendaylight.controller.sal.compability.ProtocolConstants.ETHERNET_ARP;
import static org.opendaylight.controller.sal.compability.ProtocolConstants.SCTP;
import static org.opendaylight.controller.sal.compability.ProtocolConstants.TCP;
import static org.opendaylight.controller.sal.compability.ProtocolConstants.UDP;

public class TestToSalConversionsUtils {
    // prefix:
    // od|Od = Open Daylight
    private enum MtchType {
        other, ipv4, ipv6, arp, sctp, tcp, udp
    }

    @Test
    public void testToSalConversion() {
        FlowAddedBuilder odNodeFlowBuilder = new FlowAddedBuilder();
        odNodeFlowBuilder = prepareOdFlowCommon();

        Flow salFlow = ToSalConversionsUtils.flowFrom(prepareOdFlow(odNodeFlowBuilder, MtchType.other));
        checkSalMatch(salFlow.getMatch(), MtchType.other);

        salFlow = ToSalConversionsUtils.flowFrom(prepareOdFlow(odNodeFlowBuilder, MtchType.ipv4));
        checkSalMatch(salFlow.getMatch(), MtchType.ipv4);

        salFlow = ToSalConversionsUtils.flowFrom(prepareOdFlow(odNodeFlowBuilder, MtchType.ipv6));
        checkSalMatch(salFlow.getMatch(), MtchType.ipv6);

        salFlow = ToSalConversionsUtils.flowFrom(prepareOdFlow(odNodeFlowBuilder, MtchType.arp));
        checkSalMatch(salFlow.getMatch(), MtchType.arp);

        salFlow = ToSalConversionsUtils.flowFrom(prepareOdFlow(odNodeFlowBuilder, MtchType.sctp));
        checkSalMatch(salFlow.getMatch(), MtchType.sctp);

        salFlow = ToSalConversionsUtils.flowFrom(prepareOdFlow(odNodeFlowBuilder, MtchType.tcp));
        checkSalMatch(salFlow.getMatch(), MtchType.tcp);

        salFlow = ToSalConversionsUtils.flowFrom(prepareOdFlow(odNodeFlowBuilder, MtchType.udp));
        checkSalMatch(salFlow.getMatch(), MtchType.udp);

        checkSalFlow(salFlow);
    }

    private void checkSalMatch(org.opendaylight.controller.sal.match.Match match, MtchType mt) {
        switch (mt) {
        case other:
            assertEquals("DL_DST isn't equal.", "3C:A9:F4:00:E0:C8",
                    new String((byte[]) match.getField(MatchType.DL_DST).getValue()));
            assertEquals("DL_SRC isn't equal.", "24:77:03:7C:C5:F1",
                    new String((byte[]) match.getField(MatchType.DL_SRC).getValue()));
            assertEquals("DL_TYPE isn't equal.", (short) 0xffff, (short) match.getField(MatchType.DL_TYPE).getValue());
            assertEquals("NW_TOS isn't equal.", (byte) 0x33, (byte) match.getField(MatchType.NW_TOS).getValue());
            assertEquals("NW_PROTO isn't equal.", (byte) 0x3f, (byte) match.getField(MatchType.NW_PROTO).getValue());
            assertEquals("DL_VLAN isn't equal.", (short) 0xfff, (short) match.getField(MatchType.DL_VLAN).getValue());
            assertEquals("DL_VLAN_PR isn't equal.", (byte) 0x7, (byte) match.getField(MatchType.DL_VLAN_PR).getValue());
            break;
        case arp:
            assertEquals("DL_SRC isn't equal.", "22:44:66:88:AA:CC",
                    new String((byte[]) match.getField(MatchType.DL_SRC).getValue()));
            assertEquals("DL_DST isn't equal.", "11:33:55:77:BB:DD",
                    new String((byte[]) match.getField(MatchType.DL_DST).getValue()));
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
            assertEquals("NW_PROTO isn't equal.", SCTP, (byte) match.getField(MatchType.NW_PROTO).getValue());
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
        checkSalAction(actions, Output.class, 2, true);
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
            assertEquals("Wrong value for action PushVlan for pcp.", 7, ((PushVlan) action).getPcp());
            assertEquals("Wrong value for action PushVlan for cfi.", 1, ((PushVlan) action).getCfi());
            assertEquals("Wrong value for action PushVlan for vlanID.", 4095, ((PushVlan) action).getVlanId());
        } else if (action instanceof SetDlDst) {
            assertEquals("Wrong value for action SetDlDst for MAC address.", "3C:A9:F4:00:E0:C8", new String(
                    ((SetDlDst) action).getDlAddress()));
        } else if (action instanceof SetDlSrc) {
            assertEquals("Wrong value for action SetDlSrc for MAC address.", "24:77:03:7C:C5:F1", new String(
                    ((SetDlSrc) action).getDlAddress()));
        } else if (action instanceof SetDlType) {
            assertEquals("Wrong value for action SetDlType for.", 513l, ((SetDlType) action).getDlType());
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

        odNodeFlowBuilder.setCookie(new BigInteger("9223372036854775807"));
        odNodeFlowBuilder.setHardTimeout(32767);
        odNodeFlowBuilder.setIdleTimeout(32767);
        odNodeFlowBuilder.setPriority(32767);
        odNodeFlowBuilder.setAction(prepareOdActions());
        return odNodeFlowBuilder;
    }

    private NodeFlow prepareOdFlow(FlowAddedBuilder odNodeFlowBuilder, MtchType mt) {
        odNodeFlowBuilder.setMatch(prepOdMatch(mt));
        return odNodeFlowBuilder.build();
    }

    private List<Action> prepareOdActions() {
        List<Action> odActions = new ArrayList<>();

        ControllerActionBuilder controllerActionBuilder = new ControllerActionBuilder();
        DropActionBuilder dropActionBuilder = new DropActionBuilder();
        FloodActionBuilder floodActionBuilder = new FloodActionBuilder();
        FloodAllActionBuilder floodAllActionBuilder = new FloodAllActionBuilder();
        HwPathActionBuilder hwPathActionBuilder = new HwPathActionBuilder();
        LoopbackActionBuilder loopbackActionBuilder = new LoopbackActionBuilder();
        OutputActionBuilder outputActionBuilder = new OutputActionBuilder();
        PopMplsActionBuilder popMplsActionBuilder = new PopMplsActionBuilder();
        PopVlanActionBuilder popVlanActionBuilder = new PopVlanActionBuilder();
        PushMplsActionBuilder pushMplsActionBuilder = new PushMplsActionBuilder();
        PushPbbActionBuilder pushPbbActionBuilder = new PushPbbActionBuilder();
        PushVlanActionBuilder pushVlanActionBuilder = new PushVlanActionBuilder();
        SetDlDstActionBuilder setDlDstActionBuilder = new SetDlDstActionBuilder();
        SetDlSrcActionBuilder setDlSrcActionBuilder = new SetDlSrcActionBuilder();
        SetDlTypeActionBuilder setDlTypeActionBuilder = new SetDlTypeActionBuilder();
        SetMplsTtlActionBuilder setMplsTtlActionBuilder = new SetMplsTtlActionBuilder();
        SetNwTosActionBuilder setNwTosActionBuilder = new SetNwTosActionBuilder();
        SetNwTtlActionBuilder setNwTtlActionBuilder = new SetNwTtlActionBuilder();
        SetQueueActionBuilder setQueueActionBuilder = new SetQueueActionBuilder();
        SetTpDstActionBuilder setTpDstActionBuilder = new SetTpDstActionBuilder();
        SetTpSrcActionBuilder setTpSrcActionBuilder = new SetTpSrcActionBuilder();
        SetVlanCfiActionBuilder setVlanCfiActionBuilder = new SetVlanCfiActionBuilder();
        SetVlanIdActionBuilder setVlanIdActionBuilder = new SetVlanIdActionBuilder();
        SetVlanPcpActionBuilder setVlanPcpActionBuilder = new SetVlanPcpActionBuilder();
        SwPathActionBuilder swPathActionBuilder = new SwPathActionBuilder();

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

        return odActions;
    }

    private void prepareActionSetVlanPcp(SetVlanPcpActionBuilder setVlanPcpActionBuilder) {
        setVlanPcpActionBuilder.setVlanPcp(new VlanPcp((short) 7));
    }

    private void prepareActionSetVladId(SetVlanIdActionBuilder setVlanIdActionBuilder) {
        setVlanIdActionBuilder.setVlanId(new VlanId(4095));
    }

    private void prepareActionSetVlanCfi(SetVlanCfiActionBuilder setVlanCfiActionBuilder) {
        setVlanCfiActionBuilder.setVlanCfi(new VlanCfi(1));
    }

    private void prepareActionSetTpDst(SetTpDstActionBuilder setTpDstActionBuilder) {
        setTpDstActionBuilder.setPort(new PortNumber(65535));
    }

    private void prepareActionSetTpSrc(SetTpSrcActionBuilder setTpSrcActionBuilder) {
        setTpSrcActionBuilder.setPort(new PortNumber(65535));
    }

    private void prepareActionSetNwTos(SetNwTosActionBuilder setNwTosActionBuilder) {
        setNwTosActionBuilder.setTos(63);
    }

    private void prepareActionSetNwSrc(List<Action> odActions) {
        // test case for IPv4
        SetNwSrcActionBuilder setNwSrcActionBuilderIpv4 = new SetNwSrcActionBuilder();
        setNwSrcActionBuilderIpv4.setAddress(prapareIpv4Address("192.168.100.102"));
        odActions.add(new ActionBuilder().setAction(setNwSrcActionBuilderIpv4.build()).build());

        // test case for IPv6
        SetNwSrcActionBuilder setNwSrcActionBuilderIpv6 = new SetNwSrcActionBuilder();
        setNwSrcActionBuilderIpv6.setAddress(prapareIpv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7336"));
        odActions.add(new ActionBuilder().setAction(setNwSrcActionBuilderIpv6.build()).build());
    }

    private void prepareActionSetNwDst(List<Action> odActions) {
        // test case for IPv4
        SetNwDstActionBuilder setNwDstActionBuilderIpv4 = new SetNwDstActionBuilder();
        setNwDstActionBuilderIpv4.setAddress(prapareIpv4Address("192.168.100.101"));
        odActions.add(new ActionBuilder().setAction(setNwDstActionBuilderIpv4.build()).build());

        // test case for IPv6
        SetNwDstActionBuilder setNwDstActionBuilderIpv6 = new SetNwDstActionBuilder();
        setNwDstActionBuilderIpv6.setAddress(prapareIpv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7335"));
        odActions.add(new ActionBuilder().setAction(setNwDstActionBuilderIpv6.build()).build());
    }

    private void prepareActionNextHop(List<Action> odActions) {
        // test case for IPv4
        SetNextHopActionBuilder setNextHopActionBuilderIpv4 = new SetNextHopActionBuilder();
        setNextHopActionBuilderIpv4.setAddress(prapareIpv4Address("192.168.100.100"));
        odActions.add(new ActionBuilder().setAction(setNextHopActionBuilderIpv4.build()).build());

        // test case for IPv6
        SetNextHopActionBuilder setNextHopActionBuilderIpv6 = new SetNextHopActionBuilder();
        setNextHopActionBuilderIpv6.setAddress(prapareIpv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
        odActions.add(new ActionBuilder().setAction(setNextHopActionBuilderIpv6.build()).build());
    }

    private Address prapareIpv4Address(String ipv4Address) {
        Ipv4Builder ipv4Builder = new Ipv4Builder();
        ipv4Builder.setIpv4Address(new Ipv4Prefix(ipv4Address));
        return ipv4Builder.build();
    }

    private Address prapareIpv6Address(String ipv6Address) {
        Ipv6Builder ipv6Builder = new Ipv6Builder();
        ipv6Builder.setIpv6Address(new Ipv6Prefix(ipv6Address));
        return ipv6Builder.build();
    }

    private void prepareActionSetDlType(SetDlTypeActionBuilder setDlTypeActionBuilder) {
        setDlTypeActionBuilder.setDlType(new EtherType(513l));
    }

    private void prepareActionSetDlSrc(SetDlSrcActionBuilder setDlSrcActionBuilder) {
        setDlSrcActionBuilder.setAddress(new MacAddress("24:77:03:7C:C5:F1"));
    }

    private void prepareActionSetDlDst(SetDlDstActionBuilder setDlDstActionBuilder) {
        setDlDstActionBuilder.setAddress(new MacAddress("3C:A9:F4:00:E0:C8"));
    }

    private void prepareActionPushVlan(PushVlanActionBuilder pushVlanActionBuilder) {
        pushVlanActionBuilder.setPcp(7); // 3 bits
        pushVlanActionBuilder.setCfi(new VlanCfi(1)); // 1 bit
        pushVlanActionBuilder.setVlanId(new VlanId(4095));
        pushVlanActionBuilder.setTag(0x8100); // 12 bit
    }

    private void prepareActionOutput(OutputActionBuilder outputActionBuilder) {
        List<Uri> uris = new ArrayList<>();
        uris.add(new Uri("uri1"));
        uris.add(new Uri("uri2"));
        outputActionBuilder.setOutputNodeConnector(uris);
    }

    private Match prepOdMatch(MtchType mt) {
        MatchBuilder odMatchBuilder = new MatchBuilder();
        switch (mt) {
        case other:
            odMatchBuilder.setEthernetMatch(prepEthernetMatch());
            odMatchBuilder.setIpMatch(prepIpMatch());
            odMatchBuilder.setVlanMatch(prepVlanMatch());
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
        ipv4MatchBuilder.setIpv4Source(new Ipv4Prefix("192.168.1.104"));
        ipv4MatchBuilder.setIpv4Destination(new Ipv4Prefix("192.168.1.105"));
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
        arpMatchBuilder.setArpSourceTransportAddress(new Ipv4Prefix("192.168.1.101"));
        arpMatchBuilder.setArpTargetTransportAddress(new Ipv4Prefix("192.168.1.102"));

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
        vlanMatchBuilder.setVlanId(vlanIdBuilder.build());
        vlanMatchBuilder.setVlanPcp(new VlanPcp((short) 0x7));

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
        ethTypeBuild.setType(new EtherType(0xffffl));
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
