
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwarding.staticrouting;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;

/**
 * This class defines a static route object.
 */
public class StaticRoute {
    /**
     * This Enum defines the possible types for the next hop address.
     */
    public enum NextHopType {
        IPADDRESS("nexthop-ip"), SWITCHPORT("nexthop-interface");
        private NextHopType(String name) {
            this.name = name;
        }

        private String name;

        public String toString() {
            return name;
        }

        public static NextHopType fromString(String str) {
            if (str == null)
                return IPADDRESS;
            if (str.equals(IPADDRESS.toString()))
                return IPADDRESS;
            if (str.equals(SWITCHPORT.toString()))
                return SWITCHPORT;
            return IPADDRESS;
        }
    }

    InetAddress networkAddress;
    InetAddress mask;
    NextHopType type;
    InetAddress nextHopAddress;
    Node node;
    NodeConnector port;
    HostNodeConnector host;

    /**
     * Create a static route object with no specific information.
     */
    public StaticRoute() {

    }

    /**
     * Create a static route object from the StaticRouteConfig.
     * @param: config: StaticRouteConfig
     */
    public StaticRoute(StaticRouteConfig config) {
        networkAddress = config.getStaticRouteIP();
        mask = StaticRoute.getV4AddressMaskFromDecimal(config
                .getStaticRouteMask());
        type = NextHopType.fromString(config.getNextHopType());
        nextHopAddress = config.getNextHopIP();
        Map<Long, Short> switchPort = config.getNextHopSwitchPorts();
        if ((switchPort != null) && (switchPort.size() == 1)) {
            node = NodeCreator.createOFNode((Long) switchPort.keySet()
                    .toArray()[0]);
            port = NodeConnectorCreator.createOFNodeConnector(
                    (Short) switchPort.values().toArray()[0], node);
        }
    }

    /**
     * Get the IP address portion of the sub-network of the static route.
     * @return InetAddress: the IP address portion of the sub-network of the static route
     */
    public InetAddress getNetworkAddress() {
        return networkAddress;
    }

    /**
     * Set the IP address portion of the sub-network of the static route.
     * @param networkAddress The IP address (InetAddress) to be set
     */
    public void setNetworkAddress(InetAddress networkAddress) {
        this.networkAddress = networkAddress;
    }

    /**
     * Get the mask of the sub-network of the static route.
     * @return mask: the mask  (InetAddress) of the sub-network of the static route
     */
    public InetAddress getMask() {
        return mask;
    }

    /**
     * Set the sub-network's mask of the static route.
     * @param mask The mask (InetAddress) to be set
     */
    public void setMask(InetAddress mask) {
        this.mask = mask;
    }

    /**
     * Get the NextHopeType of the static route.
     * @return type: NextHopeType
     */
    public NextHopType getType() {
        return type;
    }

    /**
     * Set the nextHopType.
     * @param type The NextHopType to be set
     */
    public void setType(NextHopType type) {
        this.type = type;
    }

    /**
     * Get the next hop IP address.
     * @return: nextHopAddress (InetAddress)
     */
    public InetAddress getNextHopAddress() {
        return nextHopAddress;
    }

    /**
     * Set the next hop IP address.
     * @param nextHopAddress The IP address (InetAddress) to be set
     */
    public void setNextHopAddress(InetAddress nextHopAddress) {
        this.nextHopAddress = nextHopAddress;
    }

    /**
     * Get the Node associated with the static route.
     * @return: Node
     */
    public Node getNode() {
        return node;
    }

    /**
     * Set the node associated to the static route.
     * @param node: The node to be set
     */
    public void setNode(Node node) {
        this.node = node;
    }

    /**
     * Set the port associated to the static route.
     * @param port The port (NodeConnector) to be set
     */
    public void setPort(NodeConnector port) {
        this.port = port;
    }

    /**
     * Get the port associated to the static route.
     * @return port: The port (NodeConnector)
     */
    public NodeConnector getPort() {
        return port;
    }

    /**
     * Get the Host associated to static route.
     * @return host:  The host (HostNodeConnector)
     */
    public HostNodeConnector getHost() {
        return host;
    }

    /**
     * Set the host associated to the static route.
     * @param host: (HostNodeConnector) to be set
     */
    public void setHost(HostNodeConnector host) {
        this.host = host;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((mask == null) ? 0 : mask.hashCode());
        result = prime * result
                + ((networkAddress == null) ? 0 : networkAddress.hashCode());
        result = prime * result
                + ((nextHopAddress == null) ? 0 : nextHopAddress.hashCode());
        result = prime * result + ((port == null) ? 0 : port.hashCode());
        result = prime * result + ((node == null) ? 0 : node.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "StaticRoute [networkAddress=" + networkAddress + ", mask="
                + mask + ", type=" + type.toString() + ", nextHopAddress="
                + nextHopAddress + ", swid=" + node + ", port=" + port
                + ", host=" + host + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StaticRoute other = (StaticRoute) obj;
        if (!networkAddress.equals(other.networkAddress))
            return false;
        if (!mask.equals(other.mask))
            return false;
        return true;
    }

    private static InetAddress getV4AddressMaskFromDecimal(int mask) {
        int netmask = 0;
        for (int i = 0; i < mask; i++) {
            netmask |= (1 << 31 - i);
        }

        try {
            return InetAddress.getByAddress(BitBufferHelper
                    .toByteArray(netmask));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void applyV4MaskOnByteBuffer(ByteBuffer bb, ByteBuffer bbMask) {
        for (int i = 0; i < bb.array().length; i++) {
            bb.put(i, (byte) (bb.get(i) & bbMask.get(i)));
        }
    }

    /**
     * Compute and return the IP address  with longest prefix match from the static route based on the
     *  destNetworkAddress. Currently it only take IPv4 address format (Inet4Address)
     * @param destNetworkAddress: the IP address to be based on
     * @return: InetAddress: the IPv4 address with the longest prefix matching the static route.
     * If the destNetworkkAddress is not IPv4 format, it will return null.
     */
    public InetAddress longestPrefixMatch(InetAddress destNetworkAddress) {
        if (destNetworkAddress instanceof Inet4Address) {
            ByteBuffer bbdest = ByteBuffer
                    .wrap(destNetworkAddress.getAddress());
            ByteBuffer bbself = ByteBuffer.wrap(networkAddress.getAddress());

            ByteBuffer bbMask = ByteBuffer.wrap(mask.getAddress());

            applyV4MaskOnByteBuffer(bbdest, bbMask);
            applyV4MaskOnByteBuffer(bbself, bbMask);

            if (bbdest.equals(bbself)) {
                try {
                    return InetAddress.getByAddress(bbself.array());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Compare the static route with another static route. It only handles(for now) IPv4Address.
     * @param s: the other StaticRoute
     * @return: 0 if they are the same
     */
    public int compareTo(StaticRoute s) {
        if (s == null)
            return 1;
        if ((networkAddress instanceof Inet6Address)
                || (s.getNetworkAddress() instanceof Inet6Address)) {
            // HANDLE IPv6 Later
            return 1;
        }

        ByteBuffer bbchallenger = ByteBuffer.wrap(s.getNetworkAddress()
                .getAddress());
        ByteBuffer bbself = ByteBuffer.wrap(networkAddress.getAddress());
        ByteBuffer bbChallengerMask = ByteBuffer.wrap(s.getMask().getAddress());
        ByteBuffer bbSelfMask = ByteBuffer.wrap(getMask().getAddress());

        applyV4MaskOnByteBuffer(bbchallenger, bbChallengerMask);
        applyV4MaskOnByteBuffer(bbself, bbSelfMask);
        return bbself.compareTo(bbchallenger);
    }
}