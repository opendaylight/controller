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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpSourceHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpTargetHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
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

/**
 * MD-SAL to AD-SAL conversions collection
 */
public final class FromSalConversionsUtils {

    /** http://en.wikipedia.org/wiki/IPv4#Packet_structure (end of octet number 1, bit 14.+15.) */
    public static final int ENC_FIELD_BIT_SIZE = 2;

    private FromSalConversionsUtils() {
        throw new IllegalAccessError("forcing no instance for factory");
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

            targetBuilder.setEthernetMatch(ethernetMatch(sourceMatch));
            targetBuilder.setIpMatch(ipMatch(sourceMatch));
            targetBuilder.setVlanMatch(vlanMatch(sourceMatch));
            targetBuilder.setLayer3Match(layer3Match(sourceMatch));
            targetBuilder.setLayer4Match(layer4Match(sourceMatch));
            targetBuilder.setInPort(inPortMatch(sourceMatch));

            return targetBuilder.build();
        }
        return null;

    }

    private static NodeConnectorId inPortMatch(Match sourceMatch) {
        MatchField inPort = sourceMatch.getField(MatchType.IN_PORT);
        if(inPort != null && inPort.getValue() != null && (inPort.getValue() instanceof NodeConnector)) {
            NodeConnector port = (NodeConnector)inPort.getValue();
            return (NodeConnectorId)MDFlowMapping.toUri(port);
        }
        return null;
    }

    private static Layer4Match layer4Match(final Match sourceMatch) {
        MatchField nwProto = sourceMatch.getField(MatchType.NW_PROTO);
        Short nwProtocolSource = null;
        if (nwProto != null && nwProto.getValue() != null) {
            nwProtocolSource = (short) ((byte) nwProto.getValue());
            switch (nwProtocolSource) {
            case TCP:
                return Layer4MatchAsTcp(sourceMatch);
            case UDP:
                return Layer4MatchAsUdp(sourceMatch);
            case CRUDP:
                return Layer4MatchAsSctp(sourceMatch);
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

    private static VlanMatch vlanMatch(final Match sourceMatch) {
        VlanMatchBuilder vlanMatchBuild = new VlanMatchBuilder();

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
            return vlanMatchBuild.build();
        }
        return null;
    }

    private static IpMatch ipMatch(final Match sourceMatch) {
        IpMatchBuilder targetIpMatchBuild = new IpMatchBuilder();
        MatchField networkTos = sourceMatch.getField(MatchType.NW_TOS);
        if (networkTos != null && networkTos.getValue() != null) {
            Dscp dscp = new Dscp(
                    (short) (NetUtils.getUnsignedByte((Byte) networkTos
                            .getValue())));
            targetIpMatchBuild.setIpDscp(dscp);
        }

        MatchField protocol = sourceMatch.getField(MatchType.NW_PROTO);
        if (protocol != null && protocol.getValue() != null) {
            targetIpMatchBuild.setIpProtocol((short) ((byte) protocol
                    .getValue()));
        }
        if((networkTos != null && networkTos.getValue() != null) || (protocol != null && protocol.getValue() != null)) {
            return targetIpMatchBuild.build();
        }
        return null;
    }

    private static EthernetMatch ethernetMatch(final Match sourceMatch) {
        final EthernetMatchBuilder targetEthMatchBuild = new EthernetMatchBuilder();
        if(sourceMatch.getField(DL_SRC) != null && sourceMatch.getField(DL_SRC).getValue() != null) {
            EthernetSourceBuilder ethSourBuild = new EthernetSourceBuilder()
                    .setAddress(ethernetSourceAddress(sourceMatch));
            targetEthMatchBuild.setEthernetSource(ethSourBuild.build());
        }
        if(sourceMatch.getField(DL_DST) != null && sourceMatch.getField(DL_DST).getValue() != null) {
            EthernetDestinationBuilder ethDestBuild = new EthernetDestinationBuilder()
                    .setAddress(ethernetDestAddress(sourceMatch));
            targetEthMatchBuild.setEthernetDestination(ethDestBuild.build());
        }

        final MatchField dataLinkType = sourceMatch.getField(MatchType.DL_TYPE);
        if (dataLinkType != null && dataLinkType.getValue() != null) {
            EtherType etherType = new EtherType(new Long(
                    NetUtils.getUnsignedShort((Short) dataLinkType.getValue())));
            EthernetTypeBuilder ethType = new EthernetTypeBuilder()
                    .setType(etherType);
            targetEthMatchBuild.setEthernetType(ethType.build());
        }
        if((sourceMatch.getField(DL_SRC) != null && sourceMatch.getField(DL_SRC).getValue() != null) ||
                (sourceMatch.getField(DL_DST) != null && sourceMatch.getField(DL_DST).getValue() != null)||
                dataLinkType != null ) {
            return targetEthMatchBuild.build();
        }
        return null;
    }

    private static MacAddress ethernetSourceAddress(final Match sourceMatch) {
        final MatchField dataLinkSource = sourceMatch.getField(DL_SRC);
        if (dataLinkSource != null && dataLinkSource.getValue() != null) {
            return MDFlowMapping.toMacAddress((byte[])dataLinkSource.getValue());
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
                return setLayer3MatchAsArp(sourceMatch,
                        (Inet4Address) inetSourceAddress,
                        (Inet4Address) inetDestAddress);
            } else {
                return setLayer3MatchAsIpv4((Inet4Address) inetSourceAddress,
                        (Inet4Address) inetDestAddress);
            }
        } else if ((inetSourceAddress instanceof Inet6Address)
                || (inetDestAddress instanceof Inet6Address)) {
            return setLayer3MatchAsIpv6((Inet6Address) inetSourceAddress,
                    (Inet6Address) inetDestAddress);
        }

        return null;

    }

    private static Layer3Match setLayer3MatchAsArp(final Match sourceMatch,
            final Inet4Address inetSourceAddress,
            final Inet4Address inetDestAddress) {
        String inetSourceAddressStr = InetAddresses
                .toAddrString(inetSourceAddress);
        Ipv4Prefix ipv4SourcePrefix = new Ipv4Prefix(inetSourceAddressStr + "/32");

        String inetDestAddressValue = InetAddresses
                .toAddrString(inetDestAddress);
        Ipv4Prefix ipv4DestPrefix = new Ipv4Prefix(inetDestAddressValue + "/32");

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
            layer4MatchBuild.setIpv4Source(new Ipv4Prefix(inetSrcAddressString + "/32"));
        }
        if(inetDestAddress != null) {
            String inetDstAddressString = InetAddresses
                    .toAddrString(inetDestAddress);
            layer4MatchBuild
            .setIpv4Destination(new Ipv4Prefix(inetDstAddressString + "/32"));
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
            layer6MatchBuild.setIpv6Source(new Ipv6Prefix(inetSrcAddressString + "/128"));
        }
        if(inetDestAddress != null) {
            String inetDstAddressString = InetAddresses
                    .toAddrString(inetDestAddress);
            layer6MatchBuild
                    .setIpv6Destination(new Ipv6Prefix(inetDstAddressString + "/128"));
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

    /**
     * @param nwDscp NW-DSCP
     * @return shifted to NW-TOS (with empty ECN part)
     */
    public static int dscpToTos(int nwDscp) {
        return (short) (nwDscp << ENC_FIELD_BIT_SIZE);
    }

}
