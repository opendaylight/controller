/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.CRUDP;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.ETHERNET_ARP;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.TCP;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.UDP;
import static org.opendaylight.controller.sal.match.MatchType.DL_DST;
import static org.opendaylight.controller.sal.match.MatchType.DL_SRC;
import static org.opendaylight.controller.sal.match.MatchType.DL_TYPE;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.source.hardware.address.ArpSourceHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.target.hardware.address.ArpTargetHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.EthernetDestinationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.EthernetSourceCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.EthernetTypeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.InPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.IpDscpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.IpProtocolCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.Layer3MatchCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.Layer3MatchCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.Layer4MatchCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.VlanMatchCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.VlanMatchCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.ethernet.destination._case.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.ethernet.source._case.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.ethernet.type._case.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.in.port._case.InPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.ip.dscp._case.IpDscpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.ip.protocol._case.IpProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.layer._3.match._case.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.layer._3.match._case.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.layer._3.match._case.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.layer._3.match._case.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.layer._4.match._case.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.layer._4.match._case.layer._4.match.SctpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.layer._4.match._case.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.layer._4.match._case.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.match.vlan.match._case.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;

import com.google.common.net.InetAddresses;

public class FromSalConversionsUtils {

    private FromSalConversionsUtils() {

    }

    @SuppressWarnings("unused")
    private static Address addressFromAction(InetAddress inetAddress) {
        String strInetAddresss = InetAddresses.toAddrString(inetAddress);
        if (inetAddress instanceof Inet4Address) {
            Ipv4Builder ipv4Builder = new Ipv4Builder();
            ipv4Builder.setIpv4Address(new Ipv4Prefix(strInetAddresss));
            return ipv4Builder.build();
        } else if (inetAddress instanceof Inet6Address) {
            Ipv6Builder ipv6Builder = new Ipv6Builder();
            ipv6Builder.setIpv6Address(new Ipv6Prefix(strInetAddresss));
            return ipv6Builder.build();
        }
        return null;
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match toMatch(
            Match sourceMatch) {
        if (sourceMatch != null) {
            MatchBuilder targetBuilder = new MatchBuilder();
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.Match> matches = new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.Match>();
            int i = 0;
            addMatch(matches,ethernetSource(sourceMatch));
            addMatch(matches,ethernetDestination(sourceMatch));
            addMatch(matches,ethernetType(sourceMatch));
            addMatch(matches,ipProto(sourceMatch));
            addMatch(matches,ipDscp(sourceMatch));

            org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder vlan = vlanMatch(sourceMatch);
            if(vlan != null) {
                vlan.setOrder(i++);
                matches.add(vlan.build());
            }
            org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder l3 = layer3Match(sourceMatch);
            if(l3 != null) {
                l3.setOrder(i++);
                matches.add(l3.build());
            }
            org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder l4 = layer4Match(sourceMatch);
            if(l4 != null) {
                l4.setOrder(i++);
                matches.add(l4.build());
            }
            org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder inPort = inPortMatch(sourceMatch);
            if(inPort != null) {
                inPort.setOrder(i++);
                matches.add(inPort.build());
            }
            targetBuilder.setMatch(matches);
            return targetBuilder.build();
        }
        return null;

    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder inPortMatch(Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
        MatchField inPort = sourceMatch.getField(MatchType.IN_PORT);
        if(inPort != null && inPort.getValue() != null && (inPort.getValue() instanceof NodeConnector)) {
            NodeConnector port = (NodeConnector)inPort.getValue();
            InPortCaseBuilder ipc = new InPortCaseBuilder();
            InPortBuilder ipb = new InPortBuilder();
            ipb.setInPort((NodeConnectorId)MDFlowMapping.toUri(port));
            ipc.setInPort(ipb.build());
            mb.setMatch(ipc.build());
            return mb;
        }
        return null;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder layer4Match(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
        MatchField nwProto = sourceMatch.getField(MatchType.NW_PROTO);
        Short nwProtocolSource = null;
        if (nwProto != null && nwProto.getValue() != null) {
            nwProtocolSource = (short) ((byte) nwProto.getValue());
            Layer4MatchCaseBuilder l4 = new Layer4MatchCaseBuilder();
            switch (nwProtocolSource) {
            case TCP:
                return mb.setMatch(l4.setLayer4Match(Layer4MatchAsTcp(sourceMatch)).build());
            case UDP:
                return mb.setMatch(l4.setLayer4Match(Layer4MatchAsUdp(sourceMatch)).build());
            case CRUDP:
                return mb.setMatch(l4.setLayer4Match(Layer4MatchAsSctp(sourceMatch)).build());
            }
        }
        return null;
    }

    private static Layer4Match Layer4MatchAsSctp(final Match sourceMatch) {
        SctpMatchBuilder sctpMatchBuilder = new SctpMatchBuilder();

        Integer sourcePort = transportPort(sourceMatch, MatchType.TP_SRC);
        Integer destinationPort = transportPort(sourceMatch,
                MatchType.TP_DST);

        if (sourcePort != null) {
            sctpMatchBuilder.setSctpSourcePort(new PortNumber(sourcePort));
        }
        if (destinationPort != null) {
            sctpMatchBuilder.setSctpDestinationPort(new PortNumber(
                    destinationPort));
        }
        if(sourcePort != null || destinationPort != null) {
            return sctpMatchBuilder.build();
        }
        return null;
    }

    private static Layer4Match Layer4MatchAsUdp(final Match sourceMatch) {
        UdpMatchBuilder udpMatchBuilder = new UdpMatchBuilder();

        Integer sourcePort = transportPort(sourceMatch, MatchType.TP_SRC);
        Integer destinationPort = transportPort(sourceMatch,
                MatchType.TP_DST);

        if (sourcePort != null) {
            udpMatchBuilder.setUdpSourcePort(new PortNumber(sourcePort));
        }

        if (destinationPort != null) {
            udpMatchBuilder.setUdpDestinationPort(new PortNumber(
                    destinationPort));
        }
        if(sourcePort != null || destinationPort != null) {
            return udpMatchBuilder.build();
        }
        return null;
    }

    private static Layer4Match Layer4MatchAsTcp(final Match sourceMatch) {
        TcpMatchBuilder tcpMatchBuilder = new TcpMatchBuilder();

        Integer sourcePort = transportPort(sourceMatch, MatchType.TP_SRC);
        Integer destinationPort = transportPort(sourceMatch,
                MatchType.TP_DST);

        if (sourcePort != null) {
            tcpMatchBuilder.setTcpSourcePort(new PortNumber(sourcePort));
        }
        if (destinationPort != null) {
            tcpMatchBuilder.setTcpDestinationPort(new PortNumber(
                    destinationPort));
        }
        if(sourcePort != null || destinationPort != null) {
            return tcpMatchBuilder.build();
        }
        return null;
    }

    private static Integer transportPort(final Match sourceMatch,
            final MatchType matchType) {
        MatchField transportPort = sourceMatch.getField(matchType);
        if (transportPort != null && transportPort.getValue() != null
                && transportPort.getValue().getClass().equals(Short.class)) {
            return new Integer(NetUtils.getUnsignedShort((short) transportPort
                    .getValue()));
        }
        return null;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder vlanMatch(final Match sourceMatch) {
        VlanMatchBuilder vlanMatchBuild = new VlanMatchBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
        MatchField vlan = sourceMatch.getField(MatchType.DL_VLAN);
        if (vlan != null && vlan.getValue() != null) {
            VlanIdBuilder vlanIDBuilder = new VlanIdBuilder();
            short vid = (short)vlan.getValue();
            boolean present = (vid != MatchType.DL_VLAN_NONE);
            vlanIDBuilder.setVlanId(new VlanId((NetUtils
                    .getUnsignedShort(vid))));
            vlanIDBuilder.setVlanIdPresent(present);
            vlanMatchBuild.setVlanId(vlanIDBuilder.build());
        }

        MatchField vlanPriority = sourceMatch.getField(MatchType.DL_VLAN_PR);
        if (vlanPriority != null && vlanPriority.getValue() != null) {
            vlanMatchBuild.setVlanPcp(new VlanPcp((short) ((byte) vlanPriority
                    .getValue())));
        }
        if((vlan != null && vlan.getValue() != null) || (vlanPriority != null && vlanPriority.getValue() != null)) {
            VlanMatchCase vlanCase =  new VlanMatchCaseBuilder().setVlanMatch(vlanMatchBuild.build()).build();
            return mb.setMatch(vlanCase);
        }
        return null;
    }
    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder ipDscp(
            Match sourceMatch) {
        MatchField networkTos = sourceMatch.getField(MatchType.NW_TOS);
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;
        if (networkTos != null && networkTos.getValue() != null) {
            Dscp dscp = new Dscp(
                    (short) (NetUtils.getUnsignedByte((Byte) networkTos
                            .getValue())));
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            IpDscpBuilder ipDscp = new IpDscpBuilder().setIpDscp(dscp);
            mb.setMatch(new IpDscpCaseBuilder().setIpDscp(ipDscp.build()).build());
        }
        return mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder ipProto(
            Match sourceMatch) {
        MatchField protocol = sourceMatch.getField(MatchType.NW_PROTO);
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;
        if (protocol != null && protocol.getValue() != null) {
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            IpProtocolBuilder ipProto = new IpProtocolBuilder().setIpProtocol((short) ((byte) protocol
                    .getValue()));
            mb.setMatch(new IpProtocolCaseBuilder().setIpProtocol(ipProto.build()).build());
        }
        return mb;
    }

    private static void addMatch(List<org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.Match> matches,org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder match) {
        if(match != null) {
            match.setOrder(matches.size());
            matches.add(match.getOrder(), match.build());
        }
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder ethernetSource(Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;
        if(sourceMatch.getField(DL_SRC) != null && sourceMatch.getField(DL_SRC).getValue() != null) {
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            EthernetSourceBuilder ethSrc = new EthernetSourceBuilder().setAddress(ethernetSourceAddress(sourceMatch));
            mb.setMatch(new EthernetSourceCaseBuilder().setEthernetSource(ethSrc.build()).build());
        }
        return  mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder ethernetDestination(Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;
        if(sourceMatch.getField(DL_DST) != null && sourceMatch.getField(DL_DST).getValue() != null) {
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            EthernetDestinationBuilder ethDst = new EthernetDestinationBuilder().setAddress(ethernetDestAddress(sourceMatch));
            mb.setMatch(new EthernetDestinationCaseBuilder().setEthernetDestination(ethDst.build()).build());
        }
        return  mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder ethernetType(Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;
        final MatchField dataLinkType = sourceMatch.getField(MatchType.DL_TYPE);
        if (dataLinkType != null && dataLinkType.getValue() != null) {
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            EtherType etherType = new EtherType(new Long(
                    NetUtils.getUnsignedShort((Short) dataLinkType.getValue())));
            EthernetTypeBuilder ethType = new EthernetTypeBuilder().setType(etherType);
            mb.setMatch(new EthernetTypeCaseBuilder().setEthernetType(ethType.build()).build());
        }
        return  mb;
    }

    private static MacAddress ethernetSourceAddress(final Match sourceMatch) {
        final MatchField dataLinkSource = sourceMatch.getField(DL_SRC);
        if (dataLinkSource != null && dataLinkSource.getValue() != null) {
            return MDFlowMapping.toMacAddress((byte[])dataLinkSource.getValue());
        }
        return null;

    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder layer3Match(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
        InetAddress inetSourceAddress = null;
        MatchField netSource = sourceMatch.getField(MatchType.NW_SRC);
        if (netSource != null && netSource.getValue() != null) {
            inetSourceAddress = (InetAddress) (netSource.getValue());
        }

        InetAddress inetDestAddress = null;
        MatchField netDest = sourceMatch.getField(MatchType.NW_DST);
        if (netDest != null && netDest.getValue() != null) {
            inetDestAddress = (InetAddress) (netDest.getValue());
        }

        if ((inetSourceAddress instanceof Inet4Address)
                || (inetDestAddress instanceof Inet4Address)) {
            MatchField dataLinkType = sourceMatch.getField(DL_TYPE);
            Short dLType = null;
            if (dataLinkType != null && dataLinkType.getValue() != null) {
                dLType = (Short) (dataLinkType.getValue());
            }
            if (dLType != null && dLType.equals(ETHERNET_ARP)) {
                Layer3MatchCase l3 =  new Layer3MatchCaseBuilder().setLayer3Match(setLayer3MatchAsArp(sourceMatch,
                        (Inet4Address) inetSourceAddress,
                        (Inet4Address) inetDestAddress)).build();
                return mb.setMatch(l3);
            } else {
                Layer3MatchCase l3 = new Layer3MatchCaseBuilder().setLayer3Match(setLayer3MatchAsIpv4((Inet4Address) inetSourceAddress,
                        (Inet4Address) inetDestAddress)).build();
                return mb.setMatch(l3);
            }
        } else if ((inetSourceAddress instanceof Inet6Address)
                || (inetDestAddress instanceof Inet6Address)) {
            Layer3MatchCase l3 = new Layer3MatchCaseBuilder().setLayer3Match(setLayer3MatchAsIpv6((Inet6Address) inetSourceAddress,
                    (Inet6Address) inetDestAddress)).build();
            return mb.setMatch(l3);
        }

        return null;

    }

    private static Layer3Match setLayer3MatchAsArp(final Match sourceMatch,
            final Inet4Address inetSourceAddress,
            final Inet4Address inetDestAddress) {
        String inetSourceAddressStr = InetAddresses
                .toAddrString(inetSourceAddress);
        Ipv4Prefix ipv4SourcePrefix = new Ipv4Prefix(inetSourceAddressStr);

        String inetDestAddressValue = InetAddresses
                .toAddrString(inetDestAddress);
        Ipv4Prefix ipv4DestPrefix = new Ipv4Prefix(inetDestAddressValue);

        ArpMatchBuilder arpMatchBuilder = new ArpMatchBuilder();

        arpMatchBuilder.setArpSourceTransportAddress(ipv4SourcePrefix);
        arpMatchBuilder.setArpTargetTransportAddress(ipv4DestPrefix);

        ArpSourceHardwareAddressBuilder arpSourceHardwareAddressBuilder = new ArpSourceHardwareAddressBuilder();
        arpSourceHardwareAddressBuilder
                .setAddress(ethernetSourceAddress(sourceMatch));
        arpMatchBuilder
                .setArpSourceHardwareAddress(arpSourceHardwareAddressBuilder
                        .build());

        ArpTargetHardwareAddressBuilder arpTargetHardwareAddressBuilder = new ArpTargetHardwareAddressBuilder();
        arpTargetHardwareAddressBuilder
                .setAddress(ethernetDestAddress(sourceMatch));
        arpMatchBuilder
                .setArpTargetHardwareAddress(arpTargetHardwareAddressBuilder
                        .build());

        return arpMatchBuilder.build();

    }

    private static MacAddress ethernetDestAddress(final Match sourceMatch) {
        final MatchField dataLinkDest = sourceMatch.getField(DL_DST);
        if (dataLinkDest != null && dataLinkDest.getValue() != null) {
            return MDFlowMapping.toMacAddress((byte[]) dataLinkDest.getValue());
        }
        return null;
    }

    private static Layer3Match setLayer3MatchAsIpv4(
            final Inet4Address inetSourceAddress,
            final Inet4Address inetDestAddress) {
        Ipv4MatchBuilder layer4MatchBuild = new Ipv4MatchBuilder();
        if(inetSourceAddress != null) {
            String inetSrcAddressString = InetAddresses
                    .toAddrString(inetSourceAddress);
            layer4MatchBuild.setIpv4Source(new Ipv4Prefix(inetSrcAddressString));
        }
        if(inetDestAddress != null) {
            String inetDstAddressString = InetAddresses
                    .toAddrString(inetDestAddress);
            layer4MatchBuild
            .setIpv4Destination(new Ipv4Prefix(inetDstAddressString));
        }
        return layer4MatchBuild.build();

    }

    private static Layer3Match setLayer3MatchAsIpv6(
            final Inet6Address inetSourceAddress,
            final Inet6Address inetDestAddress) {
        Ipv6MatchBuilder layer6MatchBuild = new Ipv6MatchBuilder();
        if(inetSourceAddress != null) {
            String inetSrcAddressString = InetAddresses
                    .toAddrString(inetSourceAddress);
            layer6MatchBuild.setIpv6Source(new Ipv6Prefix(inetSrcAddressString));
        }
        if(inetDestAddress != null) {
            String inetDstAddressString = InetAddresses
                    .toAddrString(inetDestAddress);
            layer6MatchBuild
                    .setIpv6Destination(new Ipv6Prefix(inetDstAddressString));
        }
        return layer6MatchBuild.build();
    }

    public static boolean flowEquals(Flow statsFlow, Flow storedFlow) {
        if (statsFlow.getClass() != storedFlow.getClass()) {
            return false;
        }
        if (statsFlow.getBufferId()== null) {
            if (storedFlow.getBufferId() != null) {
                return false;
            }
        } else if(!statsFlow.getBufferId().equals(storedFlow.getBufferId())) {
            return false;
        }
        if (statsFlow.getContainerName()== null) {
            if (storedFlow.getContainerName()!= null) {
                return false;
            }
        } else if(!statsFlow.getContainerName().equals(storedFlow.getContainerName())) {
            return false;
        }
        if (statsFlow.getCookie()== null) {
            if (storedFlow.getCookie()!= null) {
                return false;
            }
        } else if(!statsFlow.getCookie().equals(storedFlow.getCookie())) {
            return false;
        }
        if (statsFlow.getMatch()== null) {
            if (storedFlow.getMatch() != null) {
                return false;
            }
        } else if(!statsFlow.getMatch().equals(storedFlow.getMatch())) {
            return false;
        }
        if (statsFlow.getCookie()== null) {
            if (storedFlow.getCookie()!= null) {
                return false;
            }
        } else if(!statsFlow.getCookie().equals(storedFlow.getCookie())) {
            return false;
        }
        if (statsFlow.getHardTimeout() == null) {
            if (storedFlow.getHardTimeout() != null) {
                return false;
            }
        } else if(!statsFlow.getHardTimeout().equals(storedFlow.getHardTimeout() )) {
            return false;
        }
        if (statsFlow.getIdleTimeout()== null) {
            if (storedFlow.getIdleTimeout() != null) {
                return false;
            }
        } else if(!statsFlow.getIdleTimeout().equals(storedFlow.getIdleTimeout())) {
            return false;
        }
        if (statsFlow.getPriority() == null) {
            if (storedFlow.getPriority() != null) {
                return false;
            }
        } else if(!statsFlow.getPriority().equals(storedFlow.getPriority())) {
            return false;
        }
        if (statsFlow.getTableId() == null) {
            if (storedFlow.getTableId() != null) {
                return false;
            }
        } else if(!statsFlow.getTableId().equals(storedFlow.getTableId())) {
            return false;
        }
        return true;
    }


}
