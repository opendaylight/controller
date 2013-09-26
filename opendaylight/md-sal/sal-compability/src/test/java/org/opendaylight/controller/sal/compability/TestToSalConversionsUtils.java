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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.EthernetMatchBuilder;

import com.google.common.net.InetAddresses;

public class TestToSalConversionsUtils {
    // prefix:
    // od|Od = Open Daylight

    @Test
    public void testToSalConversion() {
        Flow salFlow = ToSalConversionsUtils.flowFrom(prepareOdFlow());
        checkSalFlow(salFlow);
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

    private NodeFlow prepareOdFlow() {
        FlowAddedBuilder odNodeFlowBuilder = new FlowAddedBuilder();
        odNodeFlowBuilder.setCookie(new BigInteger("9223372036854775807"));
        odNodeFlowBuilder.setHardTimeout(32767);
        odNodeFlowBuilder.setIdleTimeout(32767);
        odNodeFlowBuilder.setPriority(32767);
        odNodeFlowBuilder.setAction(prepareOdActions());
        odNodeFlowBuilder.setMatch(prepareOdMatch());

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

    private Match prepareOdMatch() {
        MatchBuilder odMatchBuilder = new MatchBuilder();
        EthernetMatchBuilder odEthernetMatchBuilder = new EthernetMatchBuilder();
        odMatchBuilder.setEthernetMatch(odEthernetMatchBuilder.build());

        return odMatchBuilder.build();
    }
}
