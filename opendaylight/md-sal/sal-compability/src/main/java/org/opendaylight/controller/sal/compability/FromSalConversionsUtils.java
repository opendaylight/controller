package org.opendaylight.controller.sal.compability;

import static org.opendaylight.controller.sal.match.MatchType.DL_DST;
import static org.opendaylight.controller.sal.match.MatchType.DL_SRC;
import static org.opendaylight.controller.sal.match.MatchType.DL_TYPE;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opendaylight.controller.sal.action.*;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAddedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.VlanCfi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.action.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.address.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.address.address.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.arp.match.fields.ArpSourceHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.arp.match.fields.ArpTargetHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._4.match.SctpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.vlan.match.fields.VlanIdBuilder;

public class FromSalConversionsUtils {

    // source: http://en.wikipedia.org/wiki/Ethertype
    private static final Short ETHERNET_ARP = new Short((short) 0x0806);

    // source: http://en.wikipedia.org/wiki/List_of_IP_protocol_numbers
    private static final short TCP = (short) 0x06;
    private static final short UDP = (short) 0x11;
    private static final short SCTP = (short) 0x84;

    private FromSalConversionsUtils() {

    }

    public static FlowAdded flowFrom(Flow sourceFlow) {
        if (sourceFlow != null) {
            final FlowAddedBuilder targetFlow = new FlowAddedBuilder();

            targetFlow.setHardTimeout(new Integer(sourceFlow.getHardTimeout()));
            targetFlow.setIdleTimeout(new Integer(sourceFlow.getIdleTimeout()));
            targetFlow.setPriority(new Integer(sourceFlow.getPriority()));
            targetFlow.setCookie(new BigInteger(String.valueOf(sourceFlow.getId())));

            List<org.opendaylight.controller.sal.action.Action> sourceActions = sourceFlow.getActions();
            List<Action> targetActions = new ArrayList<>();
            for (org.opendaylight.controller.sal.action.Action sourceAction : sourceActions) {
                targetActions.add(actionFrom(sourceAction));
            }
            targetFlow.setAction(targetActions);

            targetFlow.setMatch(matchFrom(sourceFlow.getMatch()));

            return targetFlow.build();
        }
        return null;
    }

    private static Action actionFrom(org.opendaylight.controller.sal.action.Action sourceAction) {

        ActionBuilder targetActionBuilder = new ActionBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.Action targetAction = null;

        if (sourceAction instanceof Controller) {
            targetAction = new ControllerActionBuilder().build();
        } else if (sourceAction instanceof Drop) {
            targetAction = new DropActionBuilder().build();
        } else if (sourceAction instanceof Flood) {
            targetAction = new FloodActionBuilder().build();
        } else if (sourceAction instanceof FloodAll) {
            targetAction = new FloodAllActionBuilder().build();
        } else if (sourceAction instanceof HwPath) {
            targetAction = new HwPathActionBuilder().build();
        } else if (sourceAction instanceof Loopback) {
            targetAction = new LoopbackActionBuilder().build();
        } else if (sourceAction instanceof Output) {
            NodeConnector nodeConnector = ((Output) sourceAction).getPort();

            OutputActionBuilder outputActionBuilder = new OutputActionBuilder();
            outputActionBuilder.setOutputNodeConnector(nodeConnectorToUri(nodeConnector));
            targetAction = outputActionBuilder.build();

        } else if (sourceAction instanceof PopVlan) {
            targetAction = new PopVlanActionBuilder().build();
        } else if (sourceAction instanceof PushVlan) {
            PushVlan pushVlan = (PushVlan) sourceAction;
            PushVlanActionBuilder pushVlanActionBuilder = new PushVlanActionBuilder();

            pushVlanActionBuilder.setCfi(new VlanCfi(pushVlan.getCfi()));
            pushVlanActionBuilder.setVlanId(new VlanId(pushVlan.getVlanId()));
            pushVlanActionBuilder.setPcp(pushVlan.getPcp());
            pushVlanActionBuilder.setTag(pushVlan.getTag());
            targetAction = pushVlanActionBuilder.build();
        } else if (sourceAction instanceof SetDlDst) {
            SetDlDst setDlDst = (SetDlDst) sourceAction;
            SetDlDstActionBuilder setDlDstActionBuilder = new SetDlDstActionBuilder();

            setDlDstActionBuilder.setAddress(new MacAddress(Arrays.toString(setDlDst.getDlAddress())));
            targetAction = setDlDstActionBuilder.build();
        } else if (sourceAction instanceof SetDlSrc) {
            SetDlSrc setDlSrc = (SetDlSrc) sourceAction;
            SetDlSrcActionBuilder setDlSrcActionBuilder = new SetDlSrcActionBuilder();

            setDlSrcActionBuilder.setAddress(new MacAddress(Arrays.toString(setDlSrc.getDlAddress())));
            targetAction = setDlSrcActionBuilder.build();
        } else if (sourceAction instanceof SetDlType) {
            SetDlType setDlType = (SetDlType) sourceAction;
            SetDlTypeActionBuilder setDlTypeActionBuilder = new SetDlTypeActionBuilder();

            setDlTypeActionBuilder.setDlType(new EtherType(new Long(setDlType.getDlType())));
            targetAction = setDlTypeActionBuilder.build();
        } else if (sourceAction instanceof SetNextHop) {
            SetNextHop setNextHop = (SetNextHop) sourceAction;
            SetNextHopActionBuilder setNextHopActionBuilder = new SetNextHopActionBuilder();

            InetAddress inetAddress = setNextHop.getAddress();
            setNextHopActionBuilder.setAddress(addressFromAction(inetAddress));

            targetAction = setNextHopActionBuilder.build();
        } else if (sourceAction instanceof SetNwDst) {
            SetNwDst setNwDst = (SetNwDst) sourceAction;
            SetNwDstActionBuilder setNwDstActionBuilder = new SetNwDstActionBuilder();

            InetAddress inetAddress = setNwDst.getAddress();
            setNwDstActionBuilder.setAddress(addressFromAction(inetAddress));

            targetAction = setNwDstActionBuilder.build();
        } else if (sourceAction instanceof SetNwSrc) {
            SetNwSrc setNwSrc = (SetNwSrc) sourceAction;
            SetNwSrcActionBuilder setNwSrcActionBuilder = new SetNwSrcActionBuilder();

            InetAddress inetAddress = setNwSrc.getAddress();
            setNwSrcActionBuilder.setAddress(addressFromAction(inetAddress));

            targetAction = setNwSrcActionBuilder.build();
        } else if (sourceAction instanceof SetNwTos) {
            SetNwTos setNwTos = (SetNwTos) sourceAction;
            SetNwTosActionBuilder setNwTosActionBuilder = new SetNwTosActionBuilder();

            setNwTosActionBuilder.setTos(setNwTos.getNwTos());
            targetAction = setNwTosActionBuilder.build();
        } else if (sourceAction instanceof SetTpDst) {
            SetTpDst setTpDst = (SetTpDst) sourceAction;
            SetTpDstActionBuilder setTpDstActionBuilder = new SetTpDstActionBuilder();

            setTpDstActionBuilder.setPort(new PortNumber(setTpDst.getPort()));

            targetAction = setTpDstActionBuilder.build();
        } else if (sourceAction instanceof SetTpSrc) {
            SetTpSrc setTpSrc = (SetTpSrc) sourceAction;
            SetTpSrcActionBuilder setTpSrcActionBuilder = new SetTpSrcActionBuilder();

            setTpSrcActionBuilder.setPort(new PortNumber(setTpSrc.getPort()));

            targetAction = setTpSrcActionBuilder.build();
        } else if (sourceAction instanceof SetVlanCfi) {
            SetVlanCfi setVlanCfi = (SetVlanCfi) sourceAction;
            SetVlanCfiActionBuilder setVlanCfiActionBuilder = new SetVlanCfiActionBuilder();

            setVlanCfiActionBuilder.setVlanCfi(new VlanCfi(setVlanCfi.getCfi()));

            targetAction = setVlanCfiActionBuilder.build();
        } else if (sourceAction instanceof SetVlanId) {
            SetVlanId setVlanId = (SetVlanId) sourceAction;
            SetVlanIdActionBuilder setVlanIdActionBuilder = new SetVlanIdActionBuilder();

            setVlanIdActionBuilder.setVlanId(new VlanId(setVlanId.getVlanId()));

            targetAction = setVlanIdActionBuilder.build();
        } else if (sourceAction instanceof SetVlanPcp) {
            SetVlanPcp setVlanPcp = (SetVlanPcp) sourceAction;
            SetVlanPcpActionBuilder setVlanPcpActionBuilder = new SetVlanPcpActionBuilder();

            setVlanPcpActionBuilder.setVlanPcp(new VlanPcp((short) setVlanPcp.getPcp()));

            targetAction = setVlanPcpActionBuilder.build();
        } else if (sourceAction instanceof SwPath) {
            targetAction = new SwPathActionBuilder().build();
        }

        targetActionBuilder.setAction(targetAction);

        return targetActionBuilder.build();
    }

    private static Address addressFromAction(InetAddress inetAddress) {
        byte[] byteInetAddresss = inetAddress.getAddress();
        if (inetAddress instanceof Inet4Address) {
            Ipv4Builder ipv4Builder = new Ipv4Builder();
            ipv4Builder.setIpv4Address(new Ipv4Prefix(Arrays.toString(byteInetAddresss)));
            return ipv4Builder.build();
        } else if (inetAddress instanceof Inet6Address) {
            Ipv6Builder ipv6Builder = new Ipv6Builder();
            ipv6Builder.setIpv6Address(new Ipv6Prefix(Arrays.toString(byteInetAddresss)));
            return ipv6Builder.build();
        }
        return null;
    }

    private static List<Uri> nodeConnectorToUri(NodeConnector nodeConnector) {
        // TODO Define mapping
        return null;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.Match matchFrom(
            Match sourceMatch) {
        if (sourceMatch != null) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.MatchBuilder targetBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.MatchBuilder();

            targetBuilder.setEthernetMatch(ethernetMatchFrom(sourceMatch));
            targetBuilder.setIpMatch(ipMatchFrom(sourceMatch));
            targetBuilder.setVlanMatch(vlanMatchFrom(sourceMatch));
            targetBuilder.setLayer3Match(layer3Match(sourceMatch));
            targetBuilder.setLayer4Match(layer4Match(sourceMatch));

            return targetBuilder.build();
        }
        return null;

    }

    private static Layer4Match layer4Match(final Match sourceMatch) {
        MatchField nwProto = sourceMatch.getField(MatchType.NW_PROTO);
        Short nwProtocolSource = null;
        if (nwProto != null && nwProto.getValue() != null) {
            nwProtocolSource = (Short) (nwProto.getValue());
        }

        switch (nwProtocolSource) {
        case TCP:
            return Layer4MatchAsTcp(sourceMatch);
        case UDP:
            return Layer4MatchAsUdp(sourceMatch);
        case SCTP:
            return Layer4MatchAsSctp(sourceMatch);
        }
        return null;
    }

    private static Layer4Match Layer4MatchAsSctp(final Match sourceMatch) {
        SctpMatchBuilder sctpMatchBuilder = new SctpMatchBuilder();

        Integer sourcePort = transportPortFrom(sourceMatch, MatchType.TP_SRC);
        Integer destinationPort = transportPortFrom(sourceMatch, MatchType.TP_DST);

        if (sourcePort != null) {
            sctpMatchBuilder.setSctpSourcePort(new PortNumber(sourcePort));
        }
        if (destinationPort != null) {
            sctpMatchBuilder.setSctpDestinationPort(new PortNumber(destinationPort));
        }

        return sctpMatchBuilder.build();
    }

    private static Layer4Match Layer4MatchAsUdp(final Match sourceMatch) {
        UdpMatchBuilder udpMatchBuilder = new UdpMatchBuilder();

        Integer sourcePort = transportPortFrom(sourceMatch, MatchType.TP_SRC);
        Integer destinationPort = transportPortFrom(sourceMatch, MatchType.TP_DST);

        if (sourcePort != null) {
            udpMatchBuilder.setUdpSourcePort(new PortNumber(sourcePort));
        }

        if (destinationPort != null) {
            udpMatchBuilder.setUdpDestinationPort(new PortNumber(destinationPort));
        }

        return udpMatchBuilder.build();
    }

    private static Layer4Match Layer4MatchAsTcp(final Match sourceMatch) {
        TcpMatchBuilder tcpMatchBuilder = new TcpMatchBuilder();

        Integer sourcePort = transportPortFrom(sourceMatch, MatchType.TP_SRC);
        Integer destinationPort = transportPortFrom(sourceMatch, MatchType.TP_DST);

        if (sourcePort != null) {
            tcpMatchBuilder.setTcpSourcePort(new PortNumber(sourcePort));
        }
        if (destinationPort != null) {
            tcpMatchBuilder.setTcpDestinationPort(new PortNumber(destinationPort));
        }

        return tcpMatchBuilder.build();
    }

    private static Integer transportPortFrom(final Match sourceMatch, final MatchType matchType) {
        MatchField transportPort = sourceMatch.getField(matchType);
        if (transportPort != null && transportPort.getValue() != null) {
            return (Integer) (transportPort.getValue());
        }
        return null;
    }

    private static VlanMatch vlanMatchFrom(final Match sourceMatch) {
        VlanMatchBuilder vlanMatchBuild = new VlanMatchBuilder();

        MatchField vlan = sourceMatch.getField(MatchType.DL_VLAN);
        if (vlan != null && vlan.getValue() != null) {
            VlanIdBuilder vlanIDBuilder = new VlanIdBuilder();
            vlanIDBuilder.setVlanId(new VlanId((Integer) (vlan.getValue())));
            vlanMatchBuild.setVlanId(vlanIDBuilder.build());
        }

        MatchField vlanPriority = sourceMatch.getField(MatchType.DL_VLAN_PR);
        if (vlanPriority != null && vlanPriority.getValue() != null) {
            vlanMatchBuild.setVlanPcp(new VlanPcp((Short) (vlanPriority.getValue())));
        }

        return vlanMatchBuild.build();
    }

    private static IpMatch ipMatchFrom(final Match sourceMatch) {
        IpMatchBuilder targetIpMatchBuild = new IpMatchBuilder();
        MatchField networkTos = sourceMatch.getField(MatchType.NW_TOS);
        if (networkTos != null && networkTos.getValue() != null) {
            Dscp dscp = new Dscp((Short) (networkTos.getValue()));
            targetIpMatchBuild.setIpDscp(dscp);
        }

        MatchField protocol = sourceMatch.getField(MatchType.NW_PROTO);
        if (protocol != null && protocol.getValue() != null) {
            targetIpMatchBuild.setIpProtocol((Short) (protocol.getValue()));
        }

        return targetIpMatchBuild.build();

    }

    private static EthernetMatch ethernetMatchFrom(final Match sourceMatch) {
        final EthernetMatchBuilder targetEthMatchBuild = new EthernetMatchBuilder();

        EthernetSourceBuilder ethSourBuild = new EthernetSourceBuilder()
                .setAddress(ethernetSourceAddressFrom(sourceMatch));
        targetEthMatchBuild.setEthernetSource(ethSourBuild.build());

        EthernetDestinationBuilder ethDestBuild = new EthernetDestinationBuilder()
                .setAddress(ethernetDestAddressFrom(sourceMatch));
        targetEthMatchBuild.setEthernetDestination(ethDestBuild.build());

        final MatchField dataLinkType = sourceMatch.getField(MatchType.DL_TYPE);
        if (dataLinkType != null && dataLinkType.getValue() != null) {
            EtherType etherType = new EtherType((Long) (dataLinkType.getValue()));
            EthernetTypeBuilder ethType = new EthernetTypeBuilder().setType(etherType);
            targetEthMatchBuild.setEthernetType(ethType.build());
        }
        return targetEthMatchBuild.build();
    }

    private static MacAddress ethernetSourceAddressFrom(final Match sourceMatch) {
        final MatchField dataLinkSource = sourceMatch.getField(DL_SRC);
        if (dataLinkSource != null && dataLinkSource.getValue() != null) {
            return new MacAddress(new MacAddress((String) (dataLinkSource.getValue())));
        }
        return null;

    }

    private static Layer3Match layer3Match(final Match sourceMatch) {
        InetAddress inetSourceAddress = null;
        MatchField netSource = sourceMatch.getField(MatchType.NW_SRC);
        if (netSource != null && netSource.getValue() != null) {
            inetSourceAddress = (InetAddress) (netSource.getValue());
        }

        InetAddress inetDestAddress = null;
        MatchField netDest = sourceMatch.getField(MatchType.NW_DST);
        if (netSource != null && netSource.getValue() != null) {
            inetDestAddress = (InetAddress) (netDest.getValue());
        }

        if ((inetSourceAddress instanceof Inet4Address) && (inetDestAddress instanceof Inet4Address)) {
            MatchField dataLinkType = sourceMatch.getField(DL_TYPE);
            Short dLType = null;
            if (dataLinkType != null && dataLinkType.getValue() != null) {
                dLType = (Short) (dataLinkType.getValue());
            }
            if (dLType.equals(ETHERNET_ARP)) {
                return setLayer3MatchAsArp(sourceMatch, (Inet4Address) inetSourceAddress,
                        (Inet4Address) inetDestAddress);
            } else {
                return setLayer3MatchAsIpv4((Inet4Address) inetSourceAddress, (Inet4Address) inetDestAddress);
            }
        } else if ((inetSourceAddress instanceof Inet6Address) && (inetDestAddress instanceof Inet6Address)) {
            return setLayer3MatchAsIpv6((Inet6Address) inetSourceAddress, (Inet6Address) inetDestAddress);
        }

        return null;

    }

    private static Layer3Match setLayer3MatchAsArp(final Match sourceMatch, final Inet4Address inetSourceAddress,
            final Inet4Address inetDestAddress) {
        byte[] inetSourceAddressValue = inetSourceAddress.getAddress();
        Ipv4Prefix ipv4SourcePrefix = new Ipv4Prefix(Arrays.toString(inetSourceAddressValue));

        byte[] inetDestAddressValue = inetDestAddress.getAddress();
        Ipv4Prefix ipv4DestPrefix = new Ipv4Prefix(Arrays.toString(inetDestAddressValue));

        ArpMatchBuilder arpMatchBuilder = new ArpMatchBuilder();

        arpMatchBuilder.setArpSourceTransportAddress(ipv4SourcePrefix);
        arpMatchBuilder.setArpSourceTransportAddress(ipv4DestPrefix);

        ArpSourceHardwareAddressBuilder arpSourceHardwareAddressBuilder = new ArpSourceHardwareAddressBuilder();
        arpSourceHardwareAddressBuilder.setAddress(ethernetSourceAddressFrom(sourceMatch));
        arpMatchBuilder.setArpSourceHardwareAddress(arpSourceHardwareAddressBuilder.build());

        ArpTargetHardwareAddressBuilder arpTargetHardwareAddressBuilder = new ArpTargetHardwareAddressBuilder();
        arpTargetHardwareAddressBuilder.setAddress(ethernetDestAddressFrom(sourceMatch));
        arpMatchBuilder.setArpTargetHardwareAddress(arpTargetHardwareAddressBuilder.build());

        return arpMatchBuilder.build();

    }

    private static MacAddress ethernetDestAddressFrom(final Match sourceMatch) {
        final MatchField dataLinkDest = sourceMatch.getField(DL_DST);
        if (dataLinkDest != null && dataLinkDest.getValue() != null) {
            return new MacAddress((String) (dataLinkDest.getValue()));
        }
        return null;
    }

    private static Layer3Match setLayer3MatchAsIpv4(final Inet4Address inetSourceAddress,
            final Inet4Address inetDestAddress) {
        byte[] inetAddressValue = inetSourceAddress.getAddress();

        Ipv4MatchBuilder layer4MatchBuild = new Ipv4MatchBuilder();
        layer4MatchBuild.setIpv4Source(new Ipv4Prefix(Arrays.toString(inetAddressValue)));
        return layer4MatchBuild.build();

    }

    private static Layer3Match setLayer3MatchAsIpv6(final Inet6Address inetSourceAddress,
            final Inet6Address inetDestAddress) {
        byte[] inetAddressValue = inetSourceAddress.getAddress();
        Ipv6MatchBuilder layer6MatchBuild = new Ipv6MatchBuilder();

        layer6MatchBuild.setIpv6Source(new Ipv6Prefix(Arrays.toString(inetAddressValue)));
        return layer6MatchBuild.build();
    }

}
