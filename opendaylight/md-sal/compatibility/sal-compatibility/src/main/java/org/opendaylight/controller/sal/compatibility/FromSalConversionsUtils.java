/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.VlanIdAttributes.VlanIdOrVlanPresent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.VlanIdNone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.VlanPcpAttributes.VlanPcpOrPcpPresent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ArpSourceHardwareAddressCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ArpSourceHardwareAddressCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ArpSourceTransportAddressCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ArpSourceTransportAddressCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ArpTargetHardwareAddressCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ArpTargetHardwareAddressCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ArpTargetTransportAddressCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ArpTargetTransportAddressCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.EthernetDestinationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.EthernetSourceCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.EthernetTypeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.InPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.IpDscpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.IpProtocolCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv4DestinationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv4DestinationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv4SourceCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv4SourceCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv6DestinationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv6DestinationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv6SourceCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv6SourceCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.TcpDestinationPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.TcpDestinationPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.TcpSourcePortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.TcpSourcePortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.UdpDestinationPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.UdpDestinationPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.UdpSourcePortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.UdpSourcePortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.VlanIdCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.VlanPcpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.arp.source.hardware.address._case.ArpSourceHardwareAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.arp.source.hardware.address._case.ArpSourceHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.arp.source.transport.address._case.ArpSourceTransportAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.arp.source.transport.address._case.ArpSourceTransportAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.arp.target.hardware.address._case.ArpTargetHardwareAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.arp.target.hardware.address._case.ArpTargetHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.arp.target.transport.address._case.ArpTargetTransportAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.arp.target.transport.address._case.ArpTargetTransportAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ethernet.destination._case.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ethernet.source._case.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ethernet.type._case.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.in.port._case.InPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ip.dscp._case.IpDscpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ip.protocol._case.IpProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv4.destination._case.Ipv4Destination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv4.destination._case.Ipv4DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv4.source._case.Ipv4Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv4.source._case.Ipv4SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv6.destination._case.Ipv6Destination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv6.destination._case.Ipv6DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv6.source._case.Ipv6Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv6.source._case.Ipv6SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.tcp.destination.port._case.TcpDestinationPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.tcp.destination.port._case.TcpDestinationPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.tcp.source.port._case.TcpSourcePort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.tcp.source.port._case.TcpSourcePortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.udp.destination.port._case.UdpDestinationPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.udp.destination.port._case.UdpDestinationPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.udp.source.port._case.UdpSourcePort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.udp.source.port._case.UdpSourcePortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.vlan.id._case.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.vlan.pcp._case.VlanPcpBuilder;

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

            addMatch(matches,ethernetSource(sourceMatch));
            addMatch(matches,ethernetDestination(sourceMatch));
            addMatch(matches,ethernetType(sourceMatch));
            addMatch(matches,vlanIdMatch(sourceMatch));
            addMatch(matches,vlanPcpMatch(sourceMatch));
            addMatch(matches,ipProto(sourceMatch));
            addMatch(matches,ipDscp(sourceMatch));
            addMatch(matches,ipV4SourceMatch(sourceMatch));
            addMatch(matches,ipV4DestinationMatch(sourceMatch));
            addMatch(matches,ipV6SourceMatch(sourceMatch));
            addMatch(matches,ipV6DestinationMatch(sourceMatch));
            addMatch(matches,arpSourceTransportAddressMatch(sourceMatch));
            addMatch(matches,arpTargetTransportAddressMatch(sourceMatch));
            addMatch(matches,arpSourceHardwareAddressMatch(sourceMatch));
            addMatch(matches,arpTargetHardwareAddressMatch(sourceMatch));
            addMatch(matches,tcpSourcePortMatch(sourceMatch));
            addMatch(matches,tcpDestinationPortMatch(sourceMatch));
            addMatch(matches,udpSourcePortMatch(sourceMatch));
            addMatch(matches,udpDestinationPortMatch(sourceMatch));
            addMatch(matches,inPortMatch(sourceMatch));
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

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder tcpSourcePortMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;
        Integer sourcePort = transportPort(sourceMatch, MatchType.TP_SRC);
        if(sourcePort != null
                && sourceMatch.getField(MatchType.NW_PROTO) != null
                && sourceMatch.getField(MatchType.NW_PROTO).getValue() != null
                && (short) ((byte)sourceMatch.getField(MatchType.NW_PROTO).getValue()) == TCP) {
            TcpSourcePort port = new TcpSourcePortBuilder()
                    .setTcpSourcePort(new PortNumber(sourcePort))
                    .build();
            TcpSourcePortCase portCase = new TcpSourcePortCaseBuilder().setTcpSourcePort(port).build();
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(portCase);
        }
        return mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder tcpDestinationPortMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;
        Integer destinationPort = transportPort(sourceMatch, MatchType.TP_DST);
        if(destinationPort != null
                && sourceMatch.getField(MatchType.NW_PROTO) != null
                && sourceMatch.getField(MatchType.NW_PROTO).getValue() != null
                && (short) ((byte)sourceMatch.getField(MatchType.NW_PROTO).getValue()) == TCP) {
            TcpDestinationPort port = new TcpDestinationPortBuilder()
                    .setTcpDestinationPort(new PortNumber(destinationPort))
                    .build();
            TcpDestinationPortCase portCase = new TcpDestinationPortCaseBuilder().setTcpDestinationPort(port).build();
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(portCase);
        }
        return mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder udpSourcePortMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;
        Integer sourcePort = transportPort(sourceMatch, MatchType.TP_SRC);
        if(sourcePort != null
                && sourceMatch.getField(MatchType.NW_PROTO) != null
                && sourceMatch.getField(MatchType.NW_PROTO).getValue() != null
                && (short) ((byte)sourceMatch.getField(MatchType.NW_PROTO).getValue()) == UDP) {
            UdpSourcePort port = new UdpSourcePortBuilder()
                    .setUdpSourcePort(new PortNumber(sourcePort))
                    .build();
            UdpSourcePortCase portCase = new UdpSourcePortCaseBuilder().setUdpSourcePort(port).build();
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(portCase);
        }
        return mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder udpDestinationPortMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;
        Integer destinationPort = transportPort(sourceMatch, MatchType.TP_DST);
        if(destinationPort != null
                && sourceMatch.getField(MatchType.NW_PROTO) != null
                && sourceMatch.getField(MatchType.NW_PROTO).getValue() != null
                && (short) ((byte)sourceMatch.getField(MatchType.NW_PROTO).getValue()) == UDP) {
            UdpDestinationPort port = new UdpDestinationPortBuilder()
                    .setUdpDestinationPort(new PortNumber(destinationPort))
                    .build();
            UdpDestinationPortCase portCase = new UdpDestinationPortCaseBuilder().setUdpDestinationPort(port).build();
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(portCase);
        }
        return mb;
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

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder vlanIdMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;
        if(sourceMatch != null && sourceMatch.getField(MatchType.DL_VLAN) != null) {
            short vid = (short)sourceMatch.getField(MatchType.DL_VLAN).getValue();
            VlanIdOrVlanPresent vlanId = null;
            if(vid != MatchType.DL_VLAN_NONE) {
                vlanId = new VlanIdOrVlanPresent(new VlanId(NetUtils
                        .getUnsignedShort(vid)));
            } else {
                vlanId = new VlanIdOrVlanPresent(new VlanIdNone());
            }
            VlanIdBuilder vlanIdb = new VlanIdBuilder().setVlanIdOrVlanPresent(vlanId);
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(new VlanIdCaseBuilder().setVlanId(vlanIdb.build()).build());
        }
        return mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder vlanPcpMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;
        if(sourceMatch != null && sourceMatch.getField(MatchType.DL_VLAN_PR) != null) {
            short pcp = (short)((byte)sourceMatch.getField(MatchType.DL_VLAN_PR).getValue());
            VlanPcpBuilder pcpb = new VlanPcpBuilder().setVlanPcpOrPcpPresent(new VlanPcpOrPcpPresent(new VlanPcp(pcp)));
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(new VlanPcpCaseBuilder().setVlanPcp(pcpb.build()).build());
        }
        return mb;
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

    private static boolean matchFieldIsIpv4(MatchField mf) {
        boolean verdict = false;
        verdict = (verdict || mf != null); // is mf null
        verdict = (verdict && mf.getValue() instanceof Inet4Address); // Do we have an Inet4Address
        return verdict;
    }

    private static boolean matchFieldIsIpv6(MatchField mf) {
        boolean verdict = false;
        verdict = (verdict || mf != null); // is mf null
        verdict = (verdict && mf.getValue() instanceof Inet6Address); // Do we have an Inet6Address
        return verdict;
    }

    private static boolean matchFieldIsArp(MatchField mf) {
        boolean verdict = false;
        verdict = (verdict || mf != null);
        verdict = (verdict && mf.getValue() instanceof Short);
        verdict = (verdict && mf.getType() ==  MatchType.DL_TYPE);
        verdict = (verdict && ((Short)mf.getValue()).equals(ETHERNET_ARP));
        return verdict;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder ipV4SourceMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;

        MatchField netSource = sourceMatch.getField(MatchType.NW_SRC);
        MatchField dataLinkType = sourceMatch.getField(DL_TYPE);
        if (matchFieldIsIpv4(netSource) && !matchFieldIsArp(dataLinkType)) {
            Inet4Address inetSourceAddress = (Inet4Address) (netSource.getValue());
            Ipv4Prefix ipv4 = new Ipv4Prefix(InetAddresses.toAddrString(inetSourceAddress));
            Ipv4Source ipv4src = new Ipv4SourceBuilder().setIpv4Source(ipv4).build();
            Ipv4SourceCase ipv4SrcCase = new Ipv4SourceCaseBuilder().setIpv4Source(ipv4src).build();
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(ipv4SrcCase);
        }
        return mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder ipV4DestinationMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;

        MatchField netSource = sourceMatch.getField(MatchType.NW_DST);
        MatchField dataLinkType = sourceMatch.getField(DL_TYPE);
        if (matchFieldIsIpv4(netSource) && !matchFieldIsArp(dataLinkType)) {
            Inet4Address inetSourceAddress = (Inet4Address) (netSource.getValue());
            Ipv4Prefix ipv4 = new Ipv4Prefix(InetAddresses.toAddrString(inetSourceAddress));
            Ipv4Destination ipv4dst = new Ipv4DestinationBuilder().setIpv4Destination(ipv4).build();
            Ipv4DestinationCase ipv4SrcCase = new Ipv4DestinationCaseBuilder().setIpv4Destination((ipv4dst)).build();
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(ipv4SrcCase);
        }
        return mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder ipV6SourceMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;

        MatchField netSource = sourceMatch.getField(MatchType.NW_SRC);
        MatchField dataLinkType = sourceMatch.getField(DL_TYPE);
        if (matchFieldIsIpv6(netSource) && !matchFieldIsArp(dataLinkType)) {
            Inet6Address inetSourceAddress = (Inet6Address) (netSource.getValue());
            Ipv6Prefix ipv6 = new Ipv6Prefix(InetAddresses.toAddrString(inetSourceAddress));
            Ipv6Source ipv6src = new Ipv6SourceBuilder().setIpv6Source(ipv6).build();
            Ipv6SourceCase ipv6SrcCase = new Ipv6SourceCaseBuilder().setIpv6Source(ipv6src).build();
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(ipv6SrcCase);
        }
        return mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder ipV6DestinationMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;

        MatchField netSource = sourceMatch.getField(MatchType.NW_DST);
        MatchField dataLinkType = sourceMatch.getField(DL_TYPE);
        if (matchFieldIsIpv6(netSource) && !matchFieldIsArp(dataLinkType)) {
            Inet6Address inetSourceAddress = (Inet6Address) (netSource.getValue());
            Ipv6Prefix ipv6 = new Ipv6Prefix(InetAddresses.toAddrString(inetSourceAddress));
            Ipv6Destination ipv6dst = new Ipv6DestinationBuilder().setIpv6Destination(ipv6).build();
            Ipv6DestinationCase ipv6case = new Ipv6DestinationCaseBuilder().setIpv6Destination((ipv6dst)).build();
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(ipv6case);
        }
        return mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder arpSourceTransportAddressMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;

        MatchField netSource = sourceMatch.getField(MatchType.NW_SRC);
        MatchField dataLinkType = sourceMatch.getField(DL_TYPE);
        if (matchFieldIsArp(dataLinkType) && matchFieldIsIpv4(netSource)) {
            Inet4Address inetSourceAddress = (Inet4Address) (netSource.getValue());
            Ipv4Prefix aprIPv4 = new Ipv4Prefix(InetAddresses.toAddrString(inetSourceAddress));
            ArpSourceTransportAddress arpSrc = new ArpSourceTransportAddressBuilder().setArpSourceTransportAddress(aprIPv4).build();
            ArpSourceTransportAddressCase arpSrcCase = new ArpSourceTransportAddressCaseBuilder().setArpSourceTransportAddress((arpSrc)).build();
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(arpSrcCase);
        }
        return mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder arpTargetTransportAddressMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;

        MatchField netSource = sourceMatch.getField(MatchType.NW_DST);
        MatchField dataLinkType = sourceMatch.getField(DL_TYPE);
        if (matchFieldIsArp(dataLinkType) && matchFieldIsIpv4(netSource)) {
            Inet4Address inetSourceAddress = (Inet4Address) (netSource.getValue());
            Ipv4Prefix aprIPv4 = new Ipv4Prefix(InetAddresses.toAddrString(inetSourceAddress));
            ArpTargetTransportAddress arpDst = new ArpTargetTransportAddressBuilder().setArpTargetTransportAddress(aprIPv4).build();
            ArpTargetTransportAddressCase arpDstCase = new ArpTargetTransportAddressCaseBuilder().setArpTargetTransportAddress((arpDst)).build();
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(arpDstCase);
        }
        return mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder arpSourceHardwareAddressMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;

        MatchField netSource = sourceMatch.getField(MatchType.NW_SRC);
        MatchField dataLinkType = sourceMatch.getField(DL_TYPE);
        if (matchFieldIsArp(dataLinkType) && matchFieldIsIpv4(netSource)) {
            ArpSourceHardwareAddress arpHwSrc = new ArpSourceHardwareAddressBuilder().setAddress(ethernetSourceAddress(sourceMatch)).build();
            ArpSourceHardwareAddressCase arpHwSrcBuilder = new ArpSourceHardwareAddressCaseBuilder().setArpSourceHardwareAddress(arpHwSrc).build();
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(arpHwSrcBuilder);
        }
        return mb;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder arpTargetHardwareAddressMatch(final Match sourceMatch) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder mb = null;

        MatchField netSource = sourceMatch.getField(MatchType.NW_DST);
        MatchField dataLinkType = sourceMatch.getField(DL_TYPE);
        if (matchFieldIsArp(dataLinkType) && matchFieldIsIpv4(netSource)) {
            ArpTargetHardwareAddress arpHwDst = new ArpTargetHardwareAddressBuilder().setAddress(ethernetSourceAddress(sourceMatch)).build();
            ArpTargetHardwareAddressCase arpHwSrcBuilder = new ArpTargetHardwareAddressCaseBuilder().setArpTargetHardwareAddress(arpHwDst).build();
            mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder();
            mb.setMatch(arpHwSrcBuilder);
        }
        return mb;
    }


    private static MacAddress ethernetDestAddress(final Match sourceMatch) {
        final MatchField dataLinkDest = sourceMatch.getField(DL_DST);
        if (dataLinkDest != null && dataLinkDest.getValue() != null) {
            return MDFlowMapping.toMacAddress((byte[]) dataLinkDest.getValue());
        }
        return null;
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
