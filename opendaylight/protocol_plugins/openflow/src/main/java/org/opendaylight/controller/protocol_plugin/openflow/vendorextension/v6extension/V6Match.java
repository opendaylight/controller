
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openflow.protocol.OFMatch;
import org.openflow.util.U16;
import org.openflow.util.U8;

import java.net.Inet6Address;
import org.opendaylight.controller.sal.utils.HexEncode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class forms the vendor specific IPv6 Flow Match messages as well as
 * processes the vendor specific IPv6 Stats Reply message.
 * 
 * For message creation, it parses the user entered IPv6 match fields, creates 
 * a sub-message for each field which are later used to form the complete 
 * message.  
 * 
 * For message processing, it parses the incoming message and reads each field
 * of the message and stores in appropriate field of V6Match object.
 *  
 *
 */
public class V6Match extends OFMatch implements Cloneable {
    private static final Logger logger = LoggerFactory.getLogger(V6Match.class);
    private static final long serialVersionUID = 1L;
    protected InetAddress nwSrc;
    protected InetAddress nwDst;
    protected short inputPortMask;
    protected byte[] dataLayerSourceMask;
    protected byte[] dataLayerDestinationMask;
    protected short dataLayerVirtualLanMask;
    protected byte dataLayerVirtualLanPriorityCodePointMask;
    protected short dataLayerTypeMask;
    protected byte networkTypeOfServiceMask;
    protected byte networkProtocolMask;
    protected InetAddress networkSourceMask;
    protected InetAddress networkDestinationMask;
    protected short transportSourceMask;
    protected short transportDestinationMask;
    protected short srcIPv6SubnetMaskbits;
    protected short dstIPv6SubnetMaskbits;

    protected MatchFieldState inputPortState;
    protected MatchFieldState dlSourceState;
    protected MatchFieldState dlDestState;
    protected MatchFieldState dlVlanState;
    protected MatchFieldState ethTypeState;
    protected MatchFieldState nwTosState;
    protected MatchFieldState nwProtoState;
    protected MatchFieldState nwSrcState;
    protected MatchFieldState nwDstState;
    protected MatchFieldState tpSrcState;
    protected MatchFieldState tpDstState;
    protected short match_len = 0;
    protected short pad_size = 0;

    private static int IPV6_EXT_MIN_HDR_LEN = 36;

    private enum MatchFieldState {
        MATCH_ABSENT, MATCH_FIELD_ONLY, MATCH_FIELD_WITH_MASK
    }

    private enum OF_Match_Types {
        MATCH_OF_IN_PORT(0), MATCH_OF_ETH_DST(1), MATCH_OF_ETH_SRC(2), MATCH_OF_ETH_TYPE(
                3), MATCH_OF_VLAN_TCI(4), MATCH_OF_IP_TOS(5), MATCH_OF_IP_PROTO(
                6), MATCH_OF_IP_SRC(7), MATCH_OF_IP_DST(8), MATCH_OF_TCP_SRC(9), MATCH_OF_TCP_DST(
                10), MATCH_OF_UDP_SRC(11), MATCH_OF_UDP_DST(12), MATCH_OF_ICMTP_TYPE(
                13), MATCH_OF_ICMP_CODE(14), MATCH_OF_ARP_OP(15);

        private int value;

        private OF_Match_Types(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    private enum IPv6Extension_Match_Types {
        MATCH_IPV6EXT_TUN_ID(16), MATCH_IPV6EXT_ARP_SHA(17), MATCH_IPV6EXT_ARP_THA(
                18), MATCH_IPV6EXT_IPV6_SRC(19), MATCH_IPV6EXT_IPV6_DST(20);

        private int value;

        private IPv6Extension_Match_Types(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum Extension_Types {
        OF_10(0), IPV6EXT(1);

        protected int value;

        private Extension_Types(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public V6Match() {
        super();

        this.nwSrc = null;
        this.nwDst = null;

        this.inputPortMask = 0;
        this.dataLayerSourceMask = null;
        this.dataLayerDestinationMask = null;
        this.dataLayerTypeMask = 0;
        this.dataLayerVirtualLanMask = 0;
        this.dataLayerVirtualLanPriorityCodePointMask = 0;
        this.networkDestinationMask = null;
        this.networkSourceMask = null;
        this.networkTypeOfServiceMask = 0;
        this.networkProtocolMask = 0;
        this.transportSourceMask = 0;
        this.transportDestinationMask = 0;

        this.inputPortState = MatchFieldState.MATCH_ABSENT;
        this.dlSourceState = MatchFieldState.MATCH_ABSENT;
        this.dlDestState = MatchFieldState.MATCH_ABSENT;
        this.dlVlanState = MatchFieldState.MATCH_ABSENT;
        this.ethTypeState = MatchFieldState.MATCH_ABSENT;
        this.nwTosState = MatchFieldState.MATCH_ABSENT;
        this.nwProtoState = MatchFieldState.MATCH_ABSENT;
        this.nwSrcState = MatchFieldState.MATCH_ABSENT;
        this.nwDstState = MatchFieldState.MATCH_ABSENT;
        this.tpSrcState = MatchFieldState.MATCH_ABSENT;
        this.tpDstState = MatchFieldState.MATCH_ABSENT;

        this.match_len = 0;
        this.pad_size = 0;
    }

    public V6Match(OFMatch match) {
        super();
        this.match_len = 0;
        this.pad_size = 0;

        this.networkSourceMask = null;
        if (match.getNetworkSource() != 0) {
            InetAddress address = null;
            try {
                address = InetAddress.getByAddress(ByteBuffer.allocate(4)
                        .putInt(match.getNetworkSource()).array());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            this.setNetworkSource(address, null);
        } else {
            this.nwSrc = null;
            this.nwSrcState = MatchFieldState.MATCH_ABSENT;
        }

        this.networkDestinationMask = null;
        if (match.getNetworkDestination() != 0) {
            InetAddress address = null;
            try {
                address = InetAddress.getByAddress(ByteBuffer.allocate(4)
                        .putInt(match.getNetworkDestination()).array());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            this.setNetworkDestination(address, null);
        } else {
            this.nwDst = null;
            this.nwDstState = MatchFieldState.MATCH_ABSENT;
        }

        this.inputPortMask = 0;
        if (match.getInputPort() != 0) {
            this.setInputPort(match.getInputPort(), (short) 0);
        } else {
            this.inputPortMask = 0;
            this.inputPortState = MatchFieldState.MATCH_ABSENT;
        }

        this.dataLayerSourceMask = null;
        if (match.getDataLayerSource() != null) {
            this.setDataLayerSource(match.getDataLayerSource(), null);
        } else {
            this.dataLayerSource = null;
            this.dlSourceState = MatchFieldState.MATCH_ABSENT;
        }
        this.dataLayerDestinationMask = null;
        if (match.getDataLayerDestination() != null) {
            this.setDataLayerDestination(match.getDataLayerDestination(), null);
        } else {
            this.dataLayerDestination = null;
            this.dlDestState = MatchFieldState.MATCH_ABSENT;
        }

        this.dataLayerTypeMask = 0;
        if (match.getDataLayerType() != 0) {
            this.setDataLayerType(match.getDataLayerType(), (short) 0);
        } else {
            this.dataLayerType = 0;
            this.ethTypeState = MatchFieldState.MATCH_ABSENT;
        }

        this.dataLayerVirtualLanMask = 0;
        if (match.getDataLayerVirtualLan() != 0) {
            this.setDataLayerVirtualLan(match.getDataLayerVirtualLan(),
                    (short) 0);
        } else {
            this.dataLayerVirtualLan = 0;
            this.dlVlanState = MatchFieldState.MATCH_ABSENT;
        }

        this.dataLayerVirtualLanPriorityCodePointMask = 0;
        if (match.getDataLayerVirtualLanPriorityCodePoint() != 0) {
            this.setDataLayerVirtualLanPriorityCodePoint(match
                    .getDataLayerVirtualLanPriorityCodePoint(), (byte) 0);
        } else {
            this.dataLayerVirtualLanPriorityCodePoint = 0;
        }

        this.networkProtocolMask = 0;
        if (match.getNetworkProtocol() != 0) {
            this.setNetworkProtocol(this.networkProtocol = match
                    .getNetworkProtocol(), (byte) 0);
        } else {
            this.networkProtocol = 0;
            this.nwProtoState = MatchFieldState.MATCH_ABSENT;
        }

        this.networkTypeOfServiceMask = 0;
        if (match.getNetworkTypeOfService() != 0) {
            this.setNetworkTypeOfService(this.networkTypeOfService = match
                    .getNetworkTypeOfService(), (byte) 0);
        } else {
            this.networkTypeOfService = match.getNetworkTypeOfService();
            this.nwTosState = MatchFieldState.MATCH_ABSENT;
        }

        this.transportSourceMask = 0;
        if (match.getTransportSource() != 0) {
            this.setTransportSource(match.getTransportSource(), (short) 0);
        } else {
            this.transportSource = 0;
            this.tpSrcState = MatchFieldState.MATCH_ABSENT;
        }

        this.transportDestinationMask = 0;
        if (match.getTransportDestination() != 0) {
            this.setTransportDestination(match.getTransportDestination(),
                    (short) 0);
        } else {
            this.transportDestination = 0;
            this.tpDstState = MatchFieldState.MATCH_ABSENT;
        }

        this.setWildcards(match.getWildcards());
    }

    private enum IPProtocols {
        ICMP(1), TCP(6), UDP(17), ICMPV6(58);

        private int protocol;

        private IPProtocols(int value) {
            this.protocol = value;
        }

        private byte getValue() {
            return (byte) this.protocol;
        }
    }

    public short getIPv6MatchLen() {
        return match_len;
    }

    public int getIPv6ExtMinHdrLen() {
        return IPV6_EXT_MIN_HDR_LEN;
    }

    public short getPadSize() {
        return (short) (((match_len + 7) / 8) * 8 - match_len);
    }

    private int getIPv6ExtensionMatchHeader(Extension_Types extType, int field,
            int has_mask, int length) {
        return (((extType.getValue() & 0x0000ffff) << 16)
                | ((field & 0x0000007f) << 9) | ((has_mask & 0x00000001) << 8) | (length & 0x000000ff));
    }

    private byte[] getIPv6ExtensionPortMatchMsg(short port) {
        ByteBuffer ipv6ext_port_msg = ByteBuffer.allocate(6);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.OF_10,
                OF_Match_Types.MATCH_OF_IN_PORT.getValue(), 0, 2);
        ipv6ext_port_msg.putInt(nxm_header);
        ipv6ext_port_msg.putShort(port);
        return (ipv6ext_port_msg.array());
    }

    private byte[] getIPv6ExtensionDestMacMatchMsg(byte[] destMac) {
        ByteBuffer ipv6ext_destmac_msg = ByteBuffer.allocate(10);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.OF_10,
                OF_Match_Types.MATCH_OF_ETH_DST.getValue(), 0, 6);
        ipv6ext_destmac_msg.putInt(nxm_header);
        ipv6ext_destmac_msg.put(destMac);
        return (ipv6ext_destmac_msg.array());
    }

    private byte[] getIPv6ExtensionSrcMacMatchMsg(byte[] srcMac) {
        ByteBuffer ipv6ext_srcmac_msg = ByteBuffer.allocate(10);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.OF_10,
                OF_Match_Types.MATCH_OF_ETH_SRC.getValue(), 0, 6);
        ipv6ext_srcmac_msg.putInt(nxm_header);
        ipv6ext_srcmac_msg.put(srcMac);
        return (ipv6ext_srcmac_msg.array());
    }

    private byte[] getIPv6ExtensionEtherTypeMatchMsg(short EtherType) {
        ByteBuffer ipv6ext_etype_msg = ByteBuffer.allocate(6);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.OF_10,
                OF_Match_Types.MATCH_OF_ETH_TYPE.getValue(), 0, 2);
        ipv6ext_etype_msg.putInt(nxm_header);
        ipv6ext_etype_msg.putShort(EtherType);
        return (ipv6ext_etype_msg.array());
    }

    private byte[] getIPv6ExtensionVlanIDMatchMsg(short VLAN) {
        ByteBuffer ipv6ext_vlanid_msg = ByteBuffer.allocate(6);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.OF_10,
                OF_Match_Types.MATCH_OF_VLAN_TCI.getValue(), 0, 2);
        ipv6ext_vlanid_msg.putInt(nxm_header);
        ipv6ext_vlanid_msg.putShort(VLAN);
        return (ipv6ext_vlanid_msg.array());
    }

    private byte[] getIPv6ExtensionSrcIPv6MatchMsg(byte[] srcIpv6) {
        ByteBuffer ipv6ext_ipv6_msg = ByteBuffer.allocate(20);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.IPV6EXT,
                IPv6Extension_Match_Types.MATCH_IPV6EXT_IPV6_SRC.getValue(), 0,
                16);
        ipv6ext_ipv6_msg.putInt(nxm_header);
        ipv6ext_ipv6_msg.put(srcIpv6);
        return (ipv6ext_ipv6_msg.array());
    }

    private byte[] getIPv6ExtensionSrcIPv6MatchwithMaskMsg(byte[] srcIpv6,
            short masklen) {
        ByteBuffer ipv6ext_ipv6_msg = ByteBuffer.allocate(36);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.IPV6EXT,
                IPv6Extension_Match_Types.MATCH_IPV6EXT_IPV6_SRC.getValue(), 1,
                32);
        ipv6ext_ipv6_msg.putInt(nxm_header);
        ipv6ext_ipv6_msg.put(srcIpv6);
        byte[] ipv6_mask = getIPv6NetworkMaskinBytes(masklen);
        ipv6ext_ipv6_msg.put(ipv6_mask);
        return (ipv6ext_ipv6_msg.array());
    }

    private byte[] getIPv6ExtensionDstIPv6MatchMsg(byte[] dstIpv6) {
        ByteBuffer ipv6ext_ipv6_msg = ByteBuffer.allocate(20);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.IPV6EXT,
                IPv6Extension_Match_Types.MATCH_IPV6EXT_IPV6_DST.getValue(), 0,
                16);
        ipv6ext_ipv6_msg.putInt(nxm_header);
        ipv6ext_ipv6_msg.put(dstIpv6);
        return (ipv6ext_ipv6_msg.array());
    }

    private byte[] getIPv6ExtensionDstIPv6MatchwithMaskMsg(byte[] dstIpv6,
            short masklen) {
        ByteBuffer ipv6ext_ipv6_msg = ByteBuffer.allocate(36);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.IPV6EXT,
                IPv6Extension_Match_Types.MATCH_IPV6EXT_IPV6_DST.getValue(), 1,
                32);
        ipv6ext_ipv6_msg.putInt(nxm_header);
        ipv6ext_ipv6_msg.put(dstIpv6);
        byte[] ipv6_mask = getIPv6NetworkMaskinBytes(masklen);
        ipv6ext_ipv6_msg.put(ipv6_mask);
        return (ipv6ext_ipv6_msg.array());
    }

    private byte[] getIPv6ExtensionProtocolMatchMsg(byte protocol) {
        ByteBuffer ipv6ext_proto_msg = ByteBuffer.allocate(5);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.OF_10,
                OF_Match_Types.MATCH_OF_IP_PROTO.getValue(), 0, 1);
        if (protocol == 0) {
            return null;
        }
        ipv6ext_proto_msg.putInt(nxm_header);
        if (protocol == IPProtocols.ICMP.getValue()) {
            /*
             * The front end  passes the same protocol type values for IPv4
             * and IPv6 flows. For the Protocol types we allow in our GUI
             * (ICMP, TCP, UDP), ICMP is the only one which is different for
             * IPv6. It is 1 for v4 and 58 for v6 Therefore, we overwrite it
             * here.
             */
            protocol = IPProtocols.ICMPV6.getValue();
        }
        ipv6ext_proto_msg.put(protocol);
        return (ipv6ext_proto_msg.array());
    }

    private byte[] getIPv6ExtensionTOSMatchMsg(byte tos) {
        ByteBuffer ipv6ext_tos_msg = ByteBuffer.allocate(5);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.OF_10,
                OF_Match_Types.MATCH_OF_IP_TOS.getValue(), 0, 1);
        ipv6ext_tos_msg.putInt(nxm_header);
        ipv6ext_tos_msg.put(tos);
        return (ipv6ext_tos_msg.array());
    }

    private byte[] getIPv6ExtensionTCPSrcPortMatchMsg(short src_port) {
        ByteBuffer ipv6ext_tcp_srcport_msg = ByteBuffer.allocate(6);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.OF_10,
                OF_Match_Types.MATCH_OF_TCP_SRC.getValue(), 0, 2);
        ipv6ext_tcp_srcport_msg.putInt(nxm_header);
        ipv6ext_tcp_srcport_msg.putShort(src_port);
        return (ipv6ext_tcp_srcport_msg.array());
    }

    private byte[] getIPv6ExtensionTCPDstPortMatchMsg(short dst_port) {
        ByteBuffer ipv6ext_tcp_dstport_msg = ByteBuffer.allocate(6);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.OF_10,
                OF_Match_Types.MATCH_OF_TCP_DST.getValue(), 0, 2);
        ipv6ext_tcp_dstport_msg.putInt(nxm_header);
        ipv6ext_tcp_dstport_msg.putShort(dst_port);
        return (ipv6ext_tcp_dstport_msg.array());
    }

    private byte[] getIPv6ExtensionUDPSrcPortMatchMsg(short src_port) {
        ByteBuffer ipv6ext_udp_srcport_msg = ByteBuffer.allocate(6);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.OF_10,
                OF_Match_Types.MATCH_OF_UDP_SRC.getValue(), 0, 2);
        ipv6ext_udp_srcport_msg.putInt(nxm_header);
        ipv6ext_udp_srcport_msg.putShort(src_port);
        return (ipv6ext_udp_srcport_msg.array());
    }

    private byte[] getIPv6ExtensionUDPDstPortMatchMsg(short dst_port) {
        ByteBuffer ipv6ext_udp_dstport_msg = ByteBuffer.allocate(6);
        int nxm_header = getIPv6ExtensionMatchHeader(Extension_Types.OF_10,
                OF_Match_Types.MATCH_OF_UDP_DST.getValue(), 0, 2);
        ipv6ext_udp_dstport_msg.putInt(nxm_header);
        ipv6ext_udp_dstport_msg.putShort(dst_port);
        return (ipv6ext_udp_dstport_msg.array());
    }

    /**
     * Sets this (V6Match) object's member variables based on a comma-separated key=value pair similar to OFMatch's fromString.
     * 
     * @param match a key=value comma separated string.
     */
    @Override
    public void fromString(String match) throws IllegalArgumentException {
        if (match.equals("") || match.equalsIgnoreCase("any")
                || match.equalsIgnoreCase("all") || match.equals("[]"))
            match = "OFMatch[]";
        String[] tokens = match.split("[\\[,\\]]");
        String[] values;
        int initArg = 0;
        if (tokens[0].equals("OFMatch"))
            initArg = 1;
        this.wildcards = OFPFW_ALL;
        int i;
        for (i = initArg; i < tokens.length; i++) {
            values = tokens[i].split("=");
            if (values.length != 2)
                throw new IllegalArgumentException("Token " + tokens[i]
                        + " does not have form 'key=value' parsing " + match);
            values[0] = values[0].toLowerCase(); // try to make this case insens
            if (values[0].equals(STR_IN_PORT) || values[0].equals("input_port")) {
                this.inputPort = U16.t(Integer.valueOf(values[1]));
                inputPortState = MatchFieldState.MATCH_FIELD_ONLY;
                match_len += 6;
            } else if (values[0].equals(STR_DL_DST)
                    || values[0].equals("eth_dst")) {
                this.dataLayerDestination = HexEncode
                        .bytesFromHexString(values[1]);
                dlDestState = MatchFieldState.MATCH_FIELD_ONLY;
                match_len += 10;
            } else if (values[0].equals(STR_DL_SRC)
                    || values[0].equals("eth_src")) {
                this.dataLayerSource = HexEncode.bytesFromHexString(values[1]);
                dlSourceState = MatchFieldState.MATCH_FIELD_ONLY;
                match_len += 10;
                this.wildcards &= ~OFPFW_DL_SRC;
            } else if (values[0].equals(STR_DL_TYPE)
                    || values[0].equals("eth_type")) {
                if (values[1].startsWith("0x"))
                    this.dataLayerType = U16.t(Integer.valueOf(values[1]
                            .replaceFirst("0x", ""), 16));
                else
                    this.dataLayerType = U16.t(Integer.valueOf(values[1]));
                ethTypeState = MatchFieldState.MATCH_FIELD_ONLY;
                match_len += 6;
            } else if (values[0].equals(STR_DL_VLAN)) {
                this.dataLayerVirtualLan = U16.t(Integer.valueOf(values[1]));
                dlVlanState = MatchFieldState.MATCH_FIELD_ONLY;
                match_len += 6;
            } else if (values[0].equals(STR_DL_VLAN_PCP)) {
                this.dataLayerVirtualLanPriorityCodePoint = U8.t(Short
                        .valueOf(values[1]));
                this.wildcards &= ~OFPFW_DL_VLAN_PCP;
            } else if (values[0].equals(STR_NW_DST)
                    || values[0].equals("ip_dst")) {
                try {
                    if (values[1].contains("/")) {
                        String ipv6addr_wmask[] = values[1].split("/");
                        this.nwDst = InetAddress.getByName(ipv6addr_wmask[0]);
                        this.dstIPv6SubnetMaskbits = Short
                                .valueOf(ipv6addr_wmask[1]);
                        nwDstState = MatchFieldState.MATCH_FIELD_WITH_MASK;
                        match_len += 36;
                    } else {
                        this.nwDst = InetAddress.getByName(values[1]);
                        nwDstState = MatchFieldState.MATCH_FIELD_ONLY;
                        match_len += 20;
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            } else if (values[0].equals(STR_NW_SRC)
                    || values[0].equals("ip_src")) {
                try {
                    if (values[1].contains("/")) {
                        String ipv6addr_wmask[] = values[1].split("/");
                        this.nwSrc = InetAddress.getByName(ipv6addr_wmask[0]);
                        this.srcIPv6SubnetMaskbits = Short
                                .valueOf(ipv6addr_wmask[1]);
                        nwSrcState = MatchFieldState.MATCH_FIELD_WITH_MASK;
                        match_len += 36;
                    } else {
                        this.nwSrc = InetAddress.getByName(values[1]);
                        nwSrcState = MatchFieldState.MATCH_FIELD_ONLY;
                        match_len += 20;
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            } else if (values[0].equals(STR_NW_PROTO)) {
                this.networkProtocol = U8.t(Short.valueOf(values[1]));
                if (!(this.networkProtocol == 0)) {
                    /*
                     * if user selects proto 0, don't use it
                     */
                    nwProtoState = MatchFieldState.MATCH_FIELD_ONLY;
                    match_len += 5;
                }
            } else if (values[0].equals(STR_NW_TOS)) {
                this.networkTypeOfService = U8.t(Short.valueOf(values[1]));
                nwTosState = MatchFieldState.MATCH_FIELD_ONLY;
                match_len += 5;
            } else if (values[0].equals(STR_TP_DST)) {
                this.transportDestination = U16.t(Integer.valueOf(values[1]));
                tpDstState = MatchFieldState.MATCH_FIELD_ONLY;
                match_len += 6;
            } else if (values[0].equals(STR_TP_SRC)) {
                this.transportSource = U16.t(Integer.valueOf(values[1]));
                tpSrcState = MatchFieldState.MATCH_FIELD_ONLY;
                match_len += 6;
            } else
                throw new IllegalArgumentException("unknown token " + tokens[i]
                        + " parsing " + match);
        }

        /*
         * In a V6 extension message action list should be preceded by a padding of 0 to
         * 7 bytes based upon following formula.
         */

        pad_size = (short) (((match_len + 7) / 8) * 8 - match_len);

    }

    /**
     * Write this message's binary format to the specified ByteBuffer
     *
     * @param data
     */
    @Override
    public void writeTo(ByteBuffer data) {
        if (inputPortState == MatchFieldState.MATCH_FIELD_ONLY) {
            byte[] ipv6ext_ingress_port_msg = getIPv6ExtensionPortMatchMsg(this.inputPort);
            data.put(ipv6ext_ingress_port_msg);
        }
        if (ethTypeState == MatchFieldState.MATCH_FIELD_ONLY) {
            byte[] ipv6ext_ether_type_msg = getIPv6ExtensionEtherTypeMatchMsg(this.dataLayerType);
            data.put(ipv6ext_ether_type_msg);
        }
        if (dlDestState == MatchFieldState.MATCH_FIELD_ONLY) {
            byte[] ipv6ext_destmac_msg = getIPv6ExtensionDestMacMatchMsg(this.dataLayerDestination);
            data.put(ipv6ext_destmac_msg);
        }
        if (dlSourceState == MatchFieldState.MATCH_FIELD_ONLY) {
            byte[] ipv6ext_srcmac_msg = getIPv6ExtensionSrcMacMatchMsg(this.dataLayerSource);
            data.put(ipv6ext_srcmac_msg);
        }
        if (dlVlanState == MatchFieldState.MATCH_FIELD_ONLY) {
            byte[] ipv6ext_vlan_id_msg = getIPv6ExtensionVlanIDMatchMsg(this.dataLayerVirtualLan);
            data.put(ipv6ext_vlan_id_msg);
        }
        if (nwSrcState == MatchFieldState.MATCH_FIELD_ONLY) {
            byte[] ipv6ext_src_ipv6_msg = getIPv6ExtensionSrcIPv6MatchMsg(this.nwSrc
                    .getAddress());
            data.put(ipv6ext_src_ipv6_msg);
        } else if (nwSrcState == MatchFieldState.MATCH_FIELD_WITH_MASK) {
            byte[] ipv6ext_src_ipv6_with_mask_msg = getIPv6ExtensionSrcIPv6MatchwithMaskMsg(
                    this.nwSrc.getAddress(), this.srcIPv6SubnetMaskbits);
            data.put(ipv6ext_src_ipv6_with_mask_msg);
        }
        if (nwDstState == MatchFieldState.MATCH_FIELD_ONLY) {
            byte[] ipv6ext_dst_ipv6_msg = getIPv6ExtensionDstIPv6MatchMsg(this.nwDst
                    .getAddress());
            data.put(ipv6ext_dst_ipv6_msg);
        } else if (nwDstState == MatchFieldState.MATCH_FIELD_WITH_MASK) {
            byte[] ipv6ext_dst_ipv6_with_mask_msg = getIPv6ExtensionDstIPv6MatchwithMaskMsg(
                    this.nwDst.getAddress(), this.dstIPv6SubnetMaskbits);
            data.put(ipv6ext_dst_ipv6_with_mask_msg);
        }
        if (nwProtoState == MatchFieldState.MATCH_FIELD_ONLY) {
            byte[] ipv6ext_protocol_msg = getIPv6ExtensionProtocolMatchMsg(this.networkProtocol);
            if (ipv6ext_protocol_msg != null) {
                data.put(ipv6ext_protocol_msg);
            }
        }
        if (nwTosState == MatchFieldState.MATCH_FIELD_ONLY) {
            byte[] ipv6ext_tos_msg = getIPv6ExtensionTOSMatchMsg(this.networkTypeOfService);
            data.put(ipv6ext_tos_msg);
        }
        if (tpSrcState == MatchFieldState.MATCH_FIELD_ONLY) {
            byte[] ipv6ext_srcport_msg = null;
            if (this.networkProtocol == IPProtocols.TCP.getValue()) {
                ipv6ext_srcport_msg = getIPv6ExtensionTCPSrcPortMatchMsg(this.transportSource);
            } else if (this.networkProtocol == IPProtocols.UDP.getValue()) {
                ipv6ext_srcport_msg = getIPv6ExtensionUDPSrcPortMatchMsg(this.transportSource);
            }
            if (ipv6ext_srcport_msg != null) {
                data.put(ipv6ext_srcport_msg);
            }
        }
        if (tpDstState == MatchFieldState.MATCH_FIELD_ONLY) {
            byte[] ipv6ext_dstport_msg = null;
            if (this.networkProtocol == IPProtocols.TCP.getValue()) {
                ipv6ext_dstport_msg = getIPv6ExtensionTCPDstPortMatchMsg(this.transportDestination);
            } else if (this.networkProtocol == IPProtocols.UDP.getValue()) {
                ipv6ext_dstport_msg = getIPv6ExtensionUDPDstPortMatchMsg(this.transportDestination);
            }
            if (ipv6ext_dstport_msg != null) {
                data.put(ipv6ext_dstport_msg);
            }
        }
        logger.trace("{}", this.toString());
    }

    private void readInPort(ByteBuffer data, int nxmLen, boolean hasMask) {
        if ((nxmLen != 2) || (data.remaining() < 2) || (hasMask))
            /*
             * mask is not allowed for inport port
             */
            return;
        super.setInputPort(data.getShort());
        this.inputPortState = MatchFieldState.MATCH_FIELD_ONLY;
        this.wildcards ^= (1 << 0); // Sync with 0F 1.0 Match
        this.match_len += 6;
    }

    private void readDataLinkDestination(ByteBuffer data, int nxmLen,
            boolean hasMask) {
        if (hasMask) {
            if ((nxmLen != 2 * 6) || (data.remaining() < 2 * 6))
                return;
            else {
                byte[] bytes = new byte[6];
                data.get(bytes);
                super.setDataLayerDestination(bytes);
                this.dataLayerDestinationMask = new byte[6];
                data.get(this.dataLayerDestinationMask);
                this.dlDestState = MatchFieldState.MATCH_FIELD_WITH_MASK;
                this.match_len += 16;
            }
        } else {
            if ((nxmLen != 6) || (data.remaining() < 6))
                return;
            else {
                byte[] bytes = new byte[6];
                data.get(bytes);
                super.setDataLayerDestination(bytes);
                this.dlDestState = MatchFieldState.MATCH_FIELD_ONLY;
                this.match_len += 10;
            }
        }
        this.wildcards ^= (1 << 3); // Sync with 0F 1.0 Match
    }

    private void readDataLinkSource(ByteBuffer data, int nxmLen, boolean hasMask) {
        /*
         * mask is not allowed in data link source
         */
        if ((nxmLen != 6) || (data.remaining() < 6) || (hasMask))
            return;
        byte[] bytes = new byte[6];
        data.get(bytes);
        super.setDataLayerSource(bytes);
        this.dlSourceState = MatchFieldState.MATCH_FIELD_ONLY;
        this.match_len += 10;
        this.wildcards ^= (1 << 2); // Sync with 0F 1.0 Match
    }

    private void readEtherType(ByteBuffer data, int nxmLen, boolean hasMask) {
        /*
         * mask is not allowed in ethertype
         */
        if ((nxmLen != 2) || (data.remaining() < 2) || (hasMask))
            return;
        super.setDataLayerType(data.getShort());
        this.ethTypeState = MatchFieldState.MATCH_FIELD_ONLY;
        this.wildcards ^= (1 << 4); // Sync with 0F 1.0 Match
        this.match_len += 6;
    }

    private void readVlanTci(ByteBuffer data, int nxmLen, boolean hasMask) {
        short vlan_mask = 0xfff;
        if (hasMask) {
            if ((nxmLen != 2 * 2) || (data.remaining() < 2 * 2))
                return;
            else {
                short vlan = data.getShort();
                vlan &= vlan_mask;
                super.setDataLayerVirtualLan(vlan);
                this.dataLayerVirtualLanMask = data.getShort();
                this.dlVlanState = MatchFieldState.MATCH_FIELD_WITH_MASK;
                this.match_len += 8;
                this.wildcards ^= (1 << 20);
            }
        } else {
            if ((nxmLen != 2) || (data.remaining() < 2))
                return;
            else {
                short vlan = data.getShort();
                vlan &= vlan_mask;
                super.setDataLayerVirtualLan(vlan);
                this.dlVlanState = MatchFieldState.MATCH_FIELD_ONLY;
                this.match_len += 6;
            }
        }

        this.wildcards ^= (1 << 1); // Sync with 0F 1.0 Match
    }

    private void readIpTos(ByteBuffer data, int nxmLen, boolean hasMask) {
        /*
         * mask is not allowed in IP TOS
         */
        if ((nxmLen != 1) || (data.remaining() < 1) || (hasMask))
            return;
        super.setNetworkTypeOfService(data.get());
        this.nwTosState = MatchFieldState.MATCH_FIELD_ONLY;
        this.match_len += 5;
        this.wildcards ^= (1 << 21); // Sync with 0F 1.0 Match
    }

    private void readIpProto(ByteBuffer data, int nxmLen, boolean hasMask) {
        /*
         * mask is not allowed in IP protocol
         */
        if ((nxmLen != 1) || (data.remaining() < 1) || (hasMask))
            return;
        super.setNetworkProtocol(data.get());
        this.nwProtoState = MatchFieldState.MATCH_FIELD_ONLY;
        this.match_len += 5;
        this.wildcards ^= (1 << 5); // Sync with 0F 1.0 Match
    }

    private void readIpv4Src(ByteBuffer data, int nxmLen, boolean hasMask) {
        if (hasMask) {
            if ((nxmLen != 2 * 4) || (data.remaining() < 2 * 4))
                return;
            else {
                byte[] sbytes = new byte[4];
                data.get(sbytes);
                try {
                    this.nwSrc = InetAddress.getByAddress(sbytes);
                } catch (UnknownHostException e) {
                    return;
                }
                byte[] mbytes = new byte[4];
                data.get(mbytes);
                try {
                    this.networkSourceMask = InetAddress.getByAddress(mbytes);
                } catch (UnknownHostException e) {
                    return;
                }
                this.nwSrcState = MatchFieldState.MATCH_FIELD_WITH_MASK;
                this.match_len += 12;
                int prefixlen = getNetworkMaskPrefixLength(mbytes);
                this.wildcards ^= (((1 << 6) - 1) << 8); // Sync with 0F 1.0 Match
                this.wildcards |= ((32 - prefixlen) << 8); // Sync with 0F 1.0 Match

            }
        } else {
            if ((nxmLen != 4) || (data.remaining() < 4))
                return;
            else {
                byte[] sbytes = new byte[4];
                data.get(sbytes);
                try {
                    this.nwSrc = InetAddress.getByAddress(sbytes);
                } catch (UnknownHostException e) {
                    return;
                }
                this.nwSrcState = MatchFieldState.MATCH_FIELD_ONLY;
                this.match_len += 8;
                this.wildcards ^= (((1 << 6) - 1) << 8); // Sync with 0F 1.0 Match
            }
        }
    }

    private void readIpv4Dst(ByteBuffer data, int nxmLen, boolean hasMask) {
        if (hasMask) {
            if ((nxmLen != 2 * 4) || (data.remaining() < 2 * 4))
                return;
            else {
                byte[] dbytes = new byte[4];
                data.get(dbytes);
                try {
                    this.nwDst = InetAddress.getByAddress(dbytes);
                } catch (UnknownHostException e) {
                    return;
                }
                byte[] mbytes = new byte[4];
                data.get(mbytes);
                try {
                    this.networkDestinationMask = InetAddress
                            .getByAddress(mbytes);
                } catch (UnknownHostException e) {
                    return;
                }
                this.nwDstState = MatchFieldState.MATCH_FIELD_WITH_MASK;
                this.match_len += 12;
                int prefixlen = getNetworkMaskPrefixLength(mbytes);
                this.wildcards ^= (((1 << 6) - 1) << 14); // Sync with 0F 1.0 Match
                this.wildcards |= ((32 - prefixlen) << 14); // Sync with 0F 1.0 Match
            }
        } else {
            if ((nxmLen != 4) || (data.remaining() < 4))
                return;
            else {
                byte[] dbytes = new byte[4];
                data.get(dbytes);
                try {
                    this.nwDst = InetAddress.getByAddress(dbytes);
                } catch (UnknownHostException e) {
                    return;
                }
                this.nwDstState = MatchFieldState.MATCH_FIELD_ONLY;
                this.wildcards ^= (((1 << 6) - 1) << 14); // Sync with 0F 1.0 Match
                this.match_len += 8;
            }
        }
    }

    private void readTcpSrc(ByteBuffer data, int nxmLen, boolean hasMask) {
        /*
         * mask is not allowed in TCP SRC
         */
        if ((nxmLen != 2) || (data.remaining() < 2) || (hasMask))
            return;
        super.setTransportSource(data.getShort());
        this.tpSrcState = MatchFieldState.MATCH_FIELD_ONLY;
        this.match_len += 6;
        this.wildcards ^= (1 << 6); // Sync with 0F 1.0 Match
    }

    private void readTcpDst(ByteBuffer data, int nxmLen, boolean hasMask) {
        /*
         * mask is not allowed in TCP DST
         */
        if ((nxmLen != 2) || (data.remaining() < 2) || (hasMask))
            return;
        super.setTransportDestination(data.getShort());
        this.tpDstState = MatchFieldState.MATCH_FIELD_ONLY;
        this.match_len += 6;
        this.wildcards ^= (1 << 7); // Sync with 0F 1.0 Match
    }

    private void readUdpSrc(ByteBuffer data, int nxmLen, boolean hasMask) {
        /*
         * mask is not allowed in UDP SRC
         */
        if ((nxmLen != 2) || (data.remaining() < 2) || (hasMask))
            return;
        super.setTransportSource(data.getShort());
        this.tpSrcState = MatchFieldState.MATCH_FIELD_ONLY;
        this.match_len += 6;
        this.wildcards ^= (1 << 6); // Sync with 0F 1.0 Match
    }

    private void readUdpDst(ByteBuffer data, int nxmLen, boolean hasMask) {
        /*
         * mask is not allowed in UDP DST
         */
        if ((nxmLen != 2) || (data.remaining() < 2) || (hasMask))
            return;
        super.setTransportDestination(data.getShort());
        this.tpDstState = MatchFieldState.MATCH_FIELD_ONLY;
        this.match_len += 6;
        this.wildcards ^= (1 << 7); // Sync with 0F 1.0 Match
    }

    private void readIpv6Src(ByteBuffer data, int nxmLen, boolean hasMask) {
        if (hasMask) {
            if ((nxmLen != 2 * 16) || (data.remaining() < 2 * 16))
                return;
            else {
                byte[] sbytes = new byte[16];
                data.get(sbytes);
                try {
                    this.nwSrc = InetAddress.getByAddress(sbytes);
                } catch (UnknownHostException e) {
                    return;
                }
                byte[] mbytes = new byte[16];
                data.get(mbytes);
                try {
                    this.networkSourceMask = InetAddress.getByAddress(mbytes);
                } catch (UnknownHostException e) {
                    return;
                }
                this.nwSrcState = MatchFieldState.MATCH_FIELD_WITH_MASK;
                this.match_len += 36;
            }
        } else {
            if ((nxmLen != 16) || (data.remaining() < 16))
                return;
            else {
                byte[] sbytes = new byte[16];
                data.get(sbytes);
                try {
                    this.nwSrc = InetAddress.getByAddress(sbytes);
                } catch (UnknownHostException e) {
                    return;
                }
                this.nwSrcState = MatchFieldState.MATCH_FIELD_ONLY;
                this.match_len += 20;
            }
        }
    }

    private void readIpv6Dst(ByteBuffer data, int nxmLen, boolean hasMask) {
        if (hasMask) {
            if ((nxmLen != 2 * 16) || (data.remaining() < 2 * 16))
                return;
            else {
                byte[] dbytes = new byte[16];
                data.get(dbytes);
                try {
                    this.nwDst = InetAddress.getByAddress(dbytes);
                } catch (UnknownHostException e) {
                    return;
                }
                byte[] mbytes = new byte[16];
                data.get(mbytes);
                try {
                    this.networkDestinationMask = InetAddress
                            .getByAddress(mbytes);
                } catch (UnknownHostException e) {
                    return;
                }
                this.nwDstState = MatchFieldState.MATCH_FIELD_WITH_MASK;
                this.match_len += 36;
            }
        } else {
            if ((nxmLen != 16) || (data.remaining() < 16))
                return;
            else {
                byte[] dbytes = new byte[16];
                data.get(dbytes);
                try {
                    this.nwDst = InetAddress.getByAddress(dbytes);
                } catch (UnknownHostException e) {
                    return;
                }
                this.nwDstState = MatchFieldState.MATCH_FIELD_ONLY;
                this.match_len += 20;
            }
        }
    }

    @Override
    public String toString() {
        return "V6Match[" + ReflectionToStringBuilder.toString(this) + "]";
    }

    /**
     * Read the data corresponding to the match field (received from the wire)
     * Input: data: match field(s). Since match field is of variable length, the whole data that are passed in
     * are assumed to fem0tbd.be the match fields.
     * @param data
     */
    @Override
    public void readFrom(ByteBuffer data) {
        readFromInternal(data);
        postprocessWildCardInfo();
    }

    private void readFromInternal(ByteBuffer data) {
        this.match_len = 0;
        while (data.remaining() > 0) {
            if (data.remaining() < 4) {
                /*
                 * at least 4 bytes for each match header
                 */
                logger.error("Invalid Vendor Extension Header. Size {}", data
                        .remaining());
                return;
            }
            /*
             * read the 4 byte match header
             */
            int nxmVendor = data.getShort();
            int b = data.get();
            int nxmField = b >> 1;
            boolean hasMask = ((b & 0x01) == 1) ? true : false;
            int nxmLen = data.get();
            if (nxmVendor == Extension_Types.OF_10.getValue()) {
                if (nxmField == OF_Match_Types.MATCH_OF_IN_PORT.getValue()) {
                    readInPort(data, nxmLen, hasMask);
                } else if (nxmField == OF_Match_Types.MATCH_OF_ETH_DST
                        .getValue()) {
                    readDataLinkDestination(data, nxmLen, hasMask);
                } else if (nxmField == OF_Match_Types.MATCH_OF_ETH_SRC
                        .getValue()) {
                    readDataLinkSource(data, nxmLen, hasMask);
                } else if (nxmField == OF_Match_Types.MATCH_OF_ETH_TYPE
                        .getValue()) {
                    readEtherType(data, nxmLen, hasMask);
                } else if (nxmField == OF_Match_Types.MATCH_OF_VLAN_TCI
                        .getValue()) {
                    readVlanTci(data, nxmLen, hasMask);
                } else if (nxmField == OF_Match_Types.MATCH_OF_IP_TOS
                        .getValue()) {
                    readIpTos(data, nxmLen, hasMask);
                } else if (nxmField == OF_Match_Types.MATCH_OF_IP_PROTO
                        .getValue()) {
                    readIpProto(data, nxmLen, hasMask);
                } else if (nxmField == OF_Match_Types.MATCH_OF_IP_SRC
                        .getValue()) {
                    readIpv4Src(data, nxmLen, hasMask);
                } else if (nxmField == OF_Match_Types.MATCH_OF_IP_DST
                        .getValue()) {
                    readIpv4Dst(data, nxmLen, hasMask);
                } else if (nxmField == OF_Match_Types.MATCH_OF_TCP_SRC
                        .getValue()) {
                    readTcpSrc(data, nxmLen, hasMask);
                } else if (nxmField == OF_Match_Types.MATCH_OF_TCP_DST
                        .getValue()) {
                    readTcpDst(data, nxmLen, hasMask);
                } else if (nxmField == OF_Match_Types.MATCH_OF_UDP_SRC
                        .getValue()) {
                    readUdpSrc(data, nxmLen, hasMask);
                } else if (nxmField == OF_Match_Types.MATCH_OF_UDP_DST
                        .getValue()) {
                    readUdpDst(data, nxmLen, hasMask);
                } else {
                    // unexpected nxmField
                    return;
                }
            } else if (nxmVendor == Extension_Types.IPV6EXT.getValue()) {
                if (nxmField == IPv6Extension_Match_Types.MATCH_IPV6EXT_IPV6_SRC
                        .getValue()) {
                    readIpv6Src(data, nxmLen, hasMask);
                } else if (nxmField == IPv6Extension_Match_Types.MATCH_IPV6EXT_IPV6_DST
                        .getValue()) {
                    readIpv6Dst(data, nxmLen, hasMask);
                } else {
                    // unexpected nxmField
                    return;
                }
            } else {
                // invalid nxmVendor
                return;
            }
        }
    }

    private void postprocessWildCardInfo() {
        // Sync with 0F 1.0 Match
        if (super.getDataLayerType() == 0x800) {
            if (((this.wildcards >> 8) & 0x3f) == 0x3f) {
                //ipv4 src processing
                this.wildcards ^= (((1 << 5) - 1) << 8);
            }
            if (((this.wildcards >> 14) & 0x3f) == 0x3f) {
                //ipv4 dest processing
                this.wildcards ^= (((1 << 5) - 1) << 14);
            }
        } else {
            this.wildcards = 0;
        }
    }

    @Override
    public V6Match clone() {

        V6Match ret = (V6Match) super.clone();
        try {
            if (this.nwSrc != null) {
                ret.nwSrc = InetAddress.getByAddress(this.nwSrc.getAddress());
            }
            if (this.nwDst != null) {
                ret.nwDst = InetAddress.getByAddress(this.nwDst.getAddress());
            }
            return ret;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get nw_dst
     *
     * @return
     */

    public InetAddress getNetworkDest() {
        return this.nwDst;
    }

    /**
     * Get nw_src
     *
     * @return
     */

    public void setNetworkSrc(InetAddress address) {
        nwSrc = address;
    }

    /**
     * Set nw_dst
     *
     * @return
     */

    public void setNetworkDest(InetAddress address) {
        nwDst = address;
    }

    /**
     * Set nw_src
     *
     * @return
     */

    public InetAddress getNetworkSrc() {
        return this.nwSrc;
    }

    private int getNetworkMaskPrefixLength(byte[] netMask) {
        ByteBuffer nm = ByteBuffer.wrap(netMask);
        int trailingZeros = Integer.numberOfTrailingZeros(nm.getInt());
        return 32 - trailingZeros;
    }

    public short getInputPortMask() {
        return inputPortMask;
    }

    public void setInputPort(short port, short mask) {
        super.inputPort = port;
        this.inputPortState = MatchFieldState.MATCH_FIELD_ONLY;
        match_len += 6;
        // Looks like mask is not allowed for input port. Will discard it
    }

    public byte[] getDataLayerSourceMask() {
        return dataLayerSourceMask;
    }

    public void setDataLayerSource(byte[] mac, byte[] mask) {
        if (mac != null) {
            System.arraycopy(mac, 0, super.dataLayerSource, 0, mac.length);
        }
        if (mask == null) {
            this.dlSourceState = MatchFieldState.MATCH_FIELD_ONLY;
            this.match_len += 10;
        } else {
            if (this.dataLayerSourceMask == null) {
                this.dataLayerSourceMask = new byte[mask.length];
            }
            System.arraycopy(mask, 0, this.dataLayerSourceMask, 0, mask.length);
            this.dlSourceState = MatchFieldState.MATCH_FIELD_WITH_MASK;
            this.match_len += 16;
        }
    }

    public byte[] getDataLayerDestinationMask() {
        return dataLayerDestinationMask;
    }

    public void setDataLayerDestination(byte[] mac, byte[] mask) {
        if (mac != null) {
            System.arraycopy(mac, 0, super.dataLayerDestination, 0, mac.length);
        }
        if (mask == null) {
            this.dlDestState = MatchFieldState.MATCH_FIELD_ONLY;
            this.match_len += 10;
        } else {
            if (this.dataLayerDestinationMask == null) {
                this.dataLayerDestinationMask = new byte[mask.length];
            }
            System.arraycopy(mask, 0, this.dataLayerDestinationMask, 0,
                    mask.length);
            this.dlDestState = MatchFieldState.MATCH_FIELD_WITH_MASK;
            this.match_len += 16;
        }
    }

    public short getDataLayerVirtualLanMask() {
        return dataLayerVirtualLanMask;
    }

    public void setDataLayerVirtualLan(short vlan, short mask) {
        super.dataLayerVirtualLan = vlan;
        if (mask == 0) {
            this.dlVlanState = MatchFieldState.MATCH_FIELD_ONLY;
            this.match_len += 6;
        } else {
            this.dataLayerVirtualLanMask = mask;
            this.dlVlanState = MatchFieldState.MATCH_FIELD_WITH_MASK;
            this.match_len += 8;
        }
    }

    public void setDataLayerVirtualLanPriorityCodePoint(byte pcp, byte mask) {
        super.dataLayerVirtualLanPriorityCodePoint = pcp;
    }

    public void setDataLayerType(short ethType, short mask) {
        // mask not allowed
        super.dataLayerType = ethType;
        this.ethTypeState = MatchFieldState.MATCH_FIELD_ONLY;
        this.match_len += 6;
    }

    public void setNetworkTypeOfService(byte tos, byte mask) {
        // mask not allowed
        super.networkTypeOfService = tos;
        this.nwTosState = MatchFieldState.MATCH_FIELD_ONLY;
        match_len += 5;
    }

    public void setNetworkProtocol(byte ipProto, byte mask) {
        // mask not allowed
        super.networkProtocol = ipProto;
        this.nwProtoState = MatchFieldState.MATCH_FIELD_ONLY;
        this.match_len += 5;
    }

    public InetAddress getNetworkSourceMask() {
        return networkSourceMask;
    }

    public void setNetworkSource(InetAddress address, InetAddress mask) {
        this.nwSrc = address;
        if (mask == null) {
            this.nwSrcState = MatchFieldState.MATCH_FIELD_ONLY;
            this.match_len += (address instanceof Inet6Address) ? 20 : 8;
        } else {
            this.networkSourceMask = mask;
            this.nwSrcState = MatchFieldState.MATCH_FIELD_WITH_MASK;
            this.match_len += (address instanceof Inet6Address) ? 36 : 12;
        }
    }

    public InetAddress getNetworkDestinationMask() {
        return networkDestinationMask;
    }

    public void setNetworkDestination(InetAddress address, InetAddress mask) {
        this.nwDst = address;
        if (mask == null) {
            this.nwDstState = MatchFieldState.MATCH_FIELD_ONLY;
            this.match_len += (address instanceof Inet6Address) ? 20 : 8;
        } else {
            this.networkDestinationMask = mask;
            this.nwDstState = MatchFieldState.MATCH_FIELD_WITH_MASK;
            this.match_len += (address instanceof Inet6Address) ? 36 : 12;
        }
    }

    public void setTransportSource(short tpSrc, short mask) {
        // mask not allowed
        super.transportSource = tpSrc;
        this.tpSrcState = MatchFieldState.MATCH_FIELD_ONLY;
        this.match_len += 6;
    }

    public short getTransportDestinationMask() {
        return transportDestinationMask;
    }

    public void setTransportDestination(short tpDst, short mask) {
        // mask not allowed
        super.transportDestination = tpDst;
        this.tpDstState = MatchFieldState.MATCH_FIELD_ONLY;
        this.match_len += 6;
    }

    private byte[] getIPv6NetworkMaskinBytes(short num) {
        byte[] nbytes = new byte[16];
        int quot = num / 8;
        int bits = num % 8;
        int i;

        for (i = 0; i < quot; i++) {
            nbytes[i] = (byte) 0xff;
        }

        if (bits > 0) {
            nbytes[i] = (byte) 0xff;
            nbytes[i] <<= 8 - bits;
        }
        return nbytes;
    }
}
