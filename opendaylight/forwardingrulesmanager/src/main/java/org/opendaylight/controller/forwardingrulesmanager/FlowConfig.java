
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager;

import java.io.Serializable;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.ActionType;
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.HwPath;
import org.opendaylight.controller.sal.action.Loopback;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.action.SetDlDst;
import org.opendaylight.controller.sal.action.SetDlSrc;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetNwSrc;
import org.opendaylight.controller.sal.action.SetNwTos;
import org.opendaylight.controller.sal.action.SetTpDst;
import org.opendaylight.controller.sal.action.SetTpSrc;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.action.SetVlanPcp;
import org.opendaylight.controller.sal.action.SwPath;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Switch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration Java Object which represents a flow configuration information
 * for Forwarding Rules Manager.
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class FlowConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(FlowConfig.class);
    private static final String staticFlowsGroup = "**StaticFlows";
    private boolean dynamic;
    private String status;

    /*
     * The order of the object data defined below is used directly in the UI built using JSP.
     * Hence try to keep the order in a more logical way.
     */
    @XmlElement
    private String installInHw;
    @XmlElement
    private String name;
    @XmlElement
    private Node node;
    @XmlElement
    private String ingressPort;
    private String portGroup;
    @XmlElement
    private String priority;
    @XmlElement
    private String etherType;
    @XmlElement
    private String vlanId;
    @XmlElement
    private String vlanPriority;
    @XmlElement
    private String dlSrc;
    @XmlElement
    private String dlDst;
    @XmlElement
    private String nwSrc;
    @XmlElement
    private String nwDst;
    @XmlElement
    private String protocol;
    @XmlElement
    private String tosBits;
    @XmlElement
    private String tpSrc;
    @XmlElement
    private String tpDst;
    @XmlElement
    private String cookie;
    @XmlElement
    private String idleTimeout;
    @XmlElement
    private String hardTimeout;
    @XmlElement
    private List<String> actions;

    private enum EtherIPType {
        ANY, V4, V6;
    };

    private enum SetNextHopType {
        CISCO_EXTENSION("Cisco NextHop Extension"), RESOLVE_L2RW(
                "Resolve L2 Rewrite");

        private SetNextHopType(String name) {
            this.name = name;
        }

        private String name;

        public String toString() {
            return name;
        }

        public boolean equals(String type) {
            if (type.trim().equalsIgnoreCase(name))
                return true;
            return false;
        }
    }

    public FlowConfig() {
    }

    public FlowConfig(String installInHw, String name, Node node,
            String priority, String cookie, String ingressPort,
            String portGroup, String vlanId, String vlanPriority,
            String etherType, String srcMac, String dstMac, String protocol,
            String tosBits, String srcIP, String dstIP, String tpSrc,
            String tpDst, String idleTimeout, String hardTimeout,
            List<String> actions) {
        super();
        this.installInHw = installInHw;
        this.name = name;
        this.node = node;
        this.priority = priority;
        this.cookie = cookie;
        this.ingressPort = ingressPort;
        this.portGroup = portGroup;
        this.vlanId = vlanId;
        this.vlanPriority = vlanPriority;
        this.etherType = etherType;
        this.dlSrc = srcMac;
        this.dlDst = dstMac;
        this.protocol = protocol;
        this.tosBits = tosBits;
        this.nwSrc = srcIP;
        this.nwDst = dstIP;
        this.tpSrc = tpSrc;
        this.tpDst = tpDst;
        this.idleTimeout = idleTimeout;
        this.hardTimeout = hardTimeout;
        this.actions = actions;
        this.status = StatusCode.SUCCESS.toString();
    }

    public FlowConfig(FlowConfig from) {
        this.installInHw = from.installInHw;
        this.name = from.name;
        this.node = from.node;
        this.priority = from.priority;
        this.cookie = from.cookie;
        this.ingressPort = from.ingressPort;
        this.portGroup = from.portGroup;
        this.vlanId = from.vlanId;
        this.vlanPriority = from.vlanPriority;
        this.etherType = from.etherType;
        this.dlSrc = from.dlSrc;
        this.dlDst = from.dlDst;
        this.protocol = from.protocol;
        this.tosBits = from.tosBits;
        this.nwSrc = from.nwSrc;
        this.nwDst = from.nwDst;
        this.tpSrc = from.tpSrc;
        this.tpDst = from.tpDst;
        this.idleTimeout = from.idleTimeout;
        this.hardTimeout = from.hardTimeout;
        this.actions = new ArrayList<String>(from.actions);
    }

    public boolean installInHw() {
        if (installInHw == null) {
            // backward compatibility
            installInHw = "true";
        }
        return installInHw.equals("true");
    }

    public void setInstallInHw(boolean inHw) {
        installInHw = inHw ? "true" : "false";
    }
    
    public String getInstallInHw() {
    	return installInHw;
    }

    public boolean isInternalFlow() {
        // Controller generated static flows have name starting with "**"
    	return (this.name != null && this.name.startsWith("**"));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            return;
        }
        this.name = name;
    }

    public Node getNode() {
        return this.node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getIngressPort() {
        return ingressPort;
    }

    public void setIngressPort(String ingressPort) {
        this.ingressPort = ingressPort;
    }

    public String getPortGroup() {
        return portGroup;
    }

    @Override
    public String toString() {
        return "FlowConfig [dynamic=" + dynamic + ", status=" + status
                + ", installInHw=" + installInHw + ", name=" + name
                + ", switchId=" + node + ", ingressPort=" + ingressPort
                + ", portGroup=" + portGroup + ", etherType=" + etherType
                + ", priority=" + priority + ", vlanId=" + vlanId
                + ", vlanPriority=" + vlanPriority + ", dlSrc=" + dlSrc
                + ", dlDst=" + dlDst + ", nwSrc=" + nwSrc + ", nwDst=" + nwDst
                + ", protocol=" + protocol + ", tosBits=" + tosBits
                + ", tpSrc=" + tpSrc + ", tpDst=" + tpDst + ", cookie="
                + cookie + ", idleTimeout=" + idleTimeout + ", hardTimeout="
                + hardTimeout + ", actions=" + actions + "]";
    }

    public void setPortGroup(String portGroup) {
        this.portGroup = portGroup;
    }

    public String getVlanId() {
        return vlanId;
    }

    public void setVlanId(String vlanId) {
        this.vlanId = vlanId;
    }

    public String getVlanPriority() {
        return vlanPriority;
    }

    public void setVlanPriority(String vlanPriority) {
        this.vlanPriority = vlanPriority;
    }

    public String getEtherType() {
        return etherType;
    }

    public void setEtherType(String etherType) {
        this.etherType = etherType;
    }

    public String getSrcMac() {
        return dlSrc;
    }

    public void setSrcMac(String srcMac) {
        this.dlSrc = srcMac;
    }

    public String getDstMac() {
        return dlDst;
    }

    public void setDstMac(String dstMac) {
        this.dlDst = dstMac;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getTosBits() {
        return tosBits;
    }

    public void setTosBits(String tos_bits) {
        this.tosBits = tos_bits;
    }

    public String getSrcIp() {
        return nwSrc;
    }

    public void setSrcIp(String src_ip) {
        this.nwSrc = src_ip;
    }

    public String getDstIp() {
        return nwDst;
    }

    public void setDstIp(String dst_ip) {
        this.nwDst = dst_ip;
    }

    public String getSrcPort() {
        return tpSrc;
    }

    public void setSrcPort(String src_port) {
        this.tpSrc = src_port;
    }

    public String getDstPort() {
        return tpDst;
    }

    public void setDstPort(String dst_port) {
        this.tpDst = dst_port;
    }

    public String getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(String idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public String getHardTimeout() {
        return hardTimeout;
    }

    public void setHardTimeout(String hardTimeout) {
        this.hardTimeout = hardTimeout;
    }

    public boolean isIPv6() {
        if (NetUtils.isIPv6AddressValid(this.getSrcIp())
                || NetUtils.isIPv6AddressValid(this.getDstIp())) {
            return true;
        }
        return false;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public boolean isPortGroupEnabled() {
        return (portGroup != null);
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isStatusSuccessful() {
        return status.equals(StatusCode.SUCCESS.toString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((actions == null) ? 0 : actions.hashCode());
        result = prime * result + ((cookie == null) ? 0 : cookie.hashCode());
        result = prime * result + ((dlDst == null) ? 0 : dlDst.hashCode());
        result = prime * result + ((dlSrc == null) ? 0 : dlSrc.hashCode());
        result = prime * result + (dynamic ? 1231 : 1237);
        result = prime * result
                + ((etherType == null) ? 0 : etherType.hashCode());
        result = prime * result
                + ((ingressPort == null) ? 0 : ingressPort.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((nwDst == null) ? 0 : nwDst.hashCode());
        result = prime * result + ((nwSrc == null) ? 0 : nwSrc.hashCode());
        result = prime * result
                + ((portGroup == null) ? 0 : portGroup.hashCode());
        result = prime * result
                + ((priority == null) ? 0 : priority.hashCode());
        result = prime * result
                + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + ((node == null) ? 0 : node.hashCode());
        result = prime * result + ((tosBits == null) ? 0 : tosBits.hashCode());
        result = prime * result + ((tpDst == null) ? 0 : tpDst.hashCode());
        result = prime * result + ((tpSrc == null) ? 0 : tpSrc.hashCode());
        result = prime * result + ((vlanId == null) ? 0 : vlanId.hashCode());
        result = prime * result
                + ((vlanPriority == null) ? 0 : vlanPriority.hashCode());
        result = prime * result
                + ((idleTimeout == null) ? 0 : idleTimeout.hashCode());
        result = prime * result
                + ((hardTimeout == null) ? 0 : hardTimeout.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FlowConfig other = (FlowConfig) obj;
        if (actions == null) {
            if (other.actions != null)
                return false;
        } else if (!actions.equals(other.actions))
            return false;
        if (cookie == null) {
            if (other.cookie != null)
                return false;
        } else if (!cookie.equals(other.cookie))
            return false;
        if (dlDst == null) {
            if (other.dlDst != null)
                return false;
        } else if (!dlDst.equals(other.dlDst))
            return false;
        if (dlSrc == null) {
            if (other.dlSrc != null)
                return false;
        } else if (!dlSrc.equals(other.dlSrc))
            return false;
        if (dynamic != other.dynamic)
            return false;
        if (etherType == null) {
            if (other.etherType != null)
                return false;
        } else if (!etherType.equals(other.etherType))
            return false;
        if (ingressPort == null) {
            if (other.ingressPort != null)
                return false;
        } else if (!ingressPort.equals(other.ingressPort))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (nwDst == null) {
            if (other.nwDst != null)
                return false;
        } else if (!nwDst.equals(other.nwDst))
            return false;
        if (nwSrc == null) {
            if (other.nwSrc != null)
                return false;
        } else if (!nwSrc.equals(other.nwSrc))
            return false;
        if (portGroup == null) {
            if (other.portGroup != null)
                return false;
        } else if (!portGroup.equals(other.portGroup))
            return false;
        if (priority == null) {
            if (other.priority != null)
                return false;
        } else if (!priority.equals(other.priority))
            return false;
        if (protocol == null) {
            if (other.protocol != null)
                return false;
        } else if (!protocol.equals(other.protocol))
            return false;
        if (node == null) {
            if (other.node != null)
                return false;
        } else if (!node.equals(other.node))
            return false;
        if (tosBits == null) {
            if (other.tosBits != null)
                return false;
        } else if (!tosBits.equals(other.tosBits))
            return false;
        if (tpDst == null) {
            if (other.tpDst != null)
                return false;
        } else if (!tpDst.equals(other.tpDst))
            return false;
        if (tpSrc == null) {
            if (other.tpSrc != null)
                return false;
        } else if (!tpSrc.equals(other.tpSrc))
            return false;
        if (vlanId == null) {
            if (other.vlanId != null)
                return false;
        } else if (!vlanId.equals(other.vlanId))
            return false;
        if (vlanPriority == null) {
            if (other.vlanPriority != null)
                return false;
        } else if (!vlanPriority.equals(other.vlanPriority))
            return false;
        if (idleTimeout == null) {
            if (other.idleTimeout != null)
                return false;
        } else if (!idleTimeout.equals(other.idleTimeout))
            return false;
        if (hardTimeout == null) {
            if (other.hardTimeout != null)
                return false;
        } else if (!hardTimeout.equals(other.hardTimeout))
            return false;
        return true;
    }

    public InetAddress getNextHopAddressForL2RWAction() {
        if (actions != null) {
            Matcher sstr;
            for (String actiongrp : actions) {
                sstr = Pattern.compile("SET_NEXT_HOP=(.*)").matcher(actiongrp);
                if (sstr.matches()) {
                    SetNextHopType setNHType = SetNextHopType.CISCO_EXTENSION;
                    String nextHopInfo = sstr.group(1);
                    String values[] = nextHopInfo.split("//");
                    String address = values[0].trim();
                    String type = null;
                    if (values.length > 1) {
                        type = values[1].trim();
                    }

                    if (type != null) {
                        for (SetNextHopType nh : SetNextHopType.values()) {
                            if (nh.equals(type))
                                setNHType = nh;
                        }
                    }

                    log.debug("Get Nexthop address = " + address + " Type = "
                            + setNHType.toString());
                    if (setNHType == SetNextHopType.RESOLVE_L2RW) {
                        try {
                            return InetAddress.getByName(address);
                        } catch (Exception e) {
                            log
                                    .debug("Exception during nextHopAddress resolution : "
                                            + e.getMessage());
                        }
                    }
                }
            }
        }
        return null;
    }

    public String getNextHopL2RWUsageError() {
        return "Could not resolve NextHop IP Address for the selected Switch.<br>"
                + "Please Check the following configurations.<br>"
                + "1. Is the NextHop IP address directly connected to the Selected Switch<br>"
                + "2. If appropriate Subnet Configurations are done in the Switch Manager<br>"
                + "3. If the Nexthop IP-Address is Correct";
    }

    public boolean isL2AddressValid(String mac) {
        if (mac == null) {
            return false;
        }

        Pattern macPattern = Pattern
                .compile("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}");
        Matcher mm = macPattern.matcher(mac);
        if (!mm.matches()) {
            log
                    .debug(
                            "Ethernet address {} is not valid. Example: 00:05:b9:7c:81:5f",
                            mac);
            return false;
        }
        return true;
    }

    public boolean isPortValid(Switch sw, Short port) {
        if (port < 1) {
            log.debug("port {} is not valid", port);
            return false;
        }

        if (sw == null) {
            log
                    .debug("switch info is not available. Skip checking if port is part of a switch or not.");
            return true;
        }

        Set<NodeConnector> nodeConnectorSet = sw.getNodeConnectors();
        for (NodeConnector nodeConnector : nodeConnectorSet) {
            if (((Short) nodeConnector.getID()).equals(port)) {
                return true;
            }
        }
        log.debug("port {} is not a valid port of node {}", port, sw.getNode());
        return false;
    }

    public boolean isVlanIdValid(String vlanId) {
        int vlan = Integer.decode(vlanId);
        return ((vlan >= 0) && (vlan < 4096));
    }

    public boolean isVlanPriorityValid(String vlanPriority) {
        int pri = Integer.decode(vlanPriority);
        return ((pri >= 0) && (pri < 8));
    }

    public boolean isTOSBitsValid(String tosBits) {
        int tos = Integer.decode(tosBits);
        return ((tos >= 0) && (tos < 64));
    }

    public boolean isTpPortValid(String tpPort) {
        int port = Integer.decode(tpPort);
        return ((port > 0) && (port <= 0xffff));
    }

    public boolean isTimeoutValid(String timeout) {
        int to = Integer.decode(timeout);
        return ((to >= 0) && (to <= 0xffff));
    }

    private boolean conflictWithContainerFlow(IContainer container,
            StringBuffer resultStr) {
        // Return true if it's default container
        if (container.getName().equals(GlobalConstants.DEFAULT.toString())) {
            return false;
        }

        // No container flow = no conflict
        List<ContainerFlow> cFlowList = container.getContainerFlows();
        if (((cFlowList == null)) || cFlowList.isEmpty()) {
            return false;
        }

        // Check against each container's flow
        Flow flow = this.getFlow();

        // Configuration is rejected if it conflicts with _all_ the container flows
        for (ContainerFlow cFlow : cFlowList) {
            if (cFlow.allowsFlow(flow)) {
                log
                        .trace("Config is congruent with at least one container flow");
                return false;
            }
        }
        String msg = "Flow Config conflicts with all existing container flows";
        resultStr.append(msg);
        log.trace(msg);

        return true;
    }

    public boolean isValid(IContainer container, StringBuffer resultStr) {
        EtherIPType etype = EtherIPType.ANY;
        EtherIPType ipsrctype = EtherIPType.ANY;
        EtherIPType ipdsttype = EtherIPType.ANY;

        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container.getName();
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);

        Switch sw = null;
        try {
            if (name == null) {
                resultStr.append(String.format("Name is null"));
                return false;
            }
            if (node == null) {
                resultStr.append(String.format("Node is null"));
                return false;
            }
            if (switchManager != null) {
                for (Switch device : switchManager.getNetworkDevices()) {
                    if (device.getNode().equals(node)) {
                        sw = device;
                        break;
                    }
                }
                if (sw == null) {
                    resultStr
                            .append(String.format("Node %s not found", node));
                    return false;
                }
            } else {
                log.debug("switchmanager is not set yet");
            }

            if (priority != null) {
                if (Integer.decode(priority) < 0
                        || (Integer.decode(priority) > 65535)) {
                    resultStr.append(String.format(
                            "priority %s is not in the range 0 - 65535",
                            priority));
                    return false;
                }
            }

            // make sure it's a valid number
            if (cookie != null)
                Long.decode(cookie);

            if (ingressPort != null) {
                Short port = Short.decode(ingressPort);
                if (isPortValid(sw, port) == false) {
                    resultStr
                            .append(String
                                    .format(
                                            "Ingress port %d is not valid for the Switch",
                                            port));
                    if ((container != null)
                            && !container.getName().equals(
                                    GlobalConstants.DEFAULT.toString())) {
                        resultStr
                                .append(" in Container " + container.getName());
                    }
                    return false;
                }
            }

            if ((vlanId != null) && !isVlanIdValid(vlanId)) {
                resultStr.append(String.format(
                        "Vlan ID %s is not in the range 0 - 4095", vlanId));
                return false;
            }

            if ((vlanPriority != null) && !isVlanPriorityValid(vlanPriority)) {
                resultStr.append(String.format(
                        "Vlan priority %s is not in the range 0 - 7",
                        vlanPriority));
                return false;
            }

            if (etherType != null) {
                int type = Integer.decode(etherType);
                if ((type < 0) || (type > 0xffff)) {
                    resultStr.append(String.format(
                            "Ethernet type %s is not valid", etherType));
                    return false;
                } else {
                    if (type == 0x800)
                        etype = EtherIPType.V4;
                    else if (type == 0x86dd)
                        etype = EtherIPType.V6;
                }
            }

            if ((tosBits != null) && !isTOSBitsValid(tosBits)) {
                resultStr.append(String.format(
                        "IP ToS bits %s is not in the range 0 - 63", tosBits));
                return false;
            }

            if ((tpSrc != null) && !isTpPortValid(tpSrc)) {
                resultStr.append(String.format(
                        "Transport source port %s is not valid", tpSrc));
                return false;
            }
            if ((tpDst != null) && !isTpPortValid(tpDst)) {
                resultStr.append(String.format(
                        "Transport destination port %s is not valid", tpDst));
                return false;
            }

            if ((dlSrc != null) && !isL2AddressValid(dlSrc)) {
                resultStr
                        .append(String
                                .format(
                                        "Ethernet source address %s is not valid. Example: 00:05:b9:7c:81:5f",
                                        dlSrc));
                return false;
            }

            if ((dlDst != null) && !isL2AddressValid(dlDst)) {
                resultStr
                        .append(String
                                .format(
                                        "Ethernet destination address %s is not valid. Example: 00:05:b9:7c:81:5f",
                                        dlDst));
                return false;
            }

            if (nwSrc != null) {
                if (NetUtils.isIPv4AddressValid(nwSrc)) {
                    ipsrctype = EtherIPType.V4;
                } else if (NetUtils.isIPv6AddressValid(nwSrc)) {
                    ipsrctype = EtherIPType.V6;
                } else {
                    resultStr.append(String.format(
                            "IP source address %s is not valid", nwSrc));
                    return false;
                }
            }

            if (nwDst != null) {
                if (NetUtils.isIPv4AddressValid(nwDst)) {
                    ipdsttype = EtherIPType.V4;
                } else if (NetUtils.isIPv6AddressValid(nwDst)) {
                    ipdsttype = EtherIPType.V6;
                } else {
                    resultStr.append(String.format(
                            "IP destination address %s is not valid", nwDst));
                    return false;
                }
            }

            if (etype != EtherIPType.ANY) {
                if ((ipsrctype != EtherIPType.ANY) && (ipsrctype != etype)) {
                    resultStr.append(String
                            .format("Type mismatch between Ethernet & Src IP"));
                    return false;
                }
                if ((ipdsttype != EtherIPType.ANY) && (ipdsttype != etype)) {
                    resultStr.append(String
                            .format("Type mismatch between Ethernet & Dst IP"));
                    return false;
                }
            }
            if (ipsrctype != ipdsttype) {
                if (!((ipsrctype == EtherIPType.ANY) || (ipdsttype == EtherIPType.ANY))) {
                    resultStr
                            .append(String.format("IP Src Dest Type mismatch"));
                    return false;
                }
            }

            if ((idleTimeout != null) && !isTimeoutValid(idleTimeout)) {
                resultStr.append(String.format(
                        "Idle Timeout value %s is not valid", idleTimeout));
                return false;
            }

            if ((hardTimeout != null) && !isTimeoutValid(hardTimeout)) {
                resultStr.append(String.format(
                        "Hard Timeout value %s is not valid", hardTimeout));
                return false;
            }

            Matcher sstr;
            if (actions != null && !actions.isEmpty()) {
                for (String actiongrp : actions) {
                    // check output ports
                    sstr = Pattern.compile("OUTPUT=(.*)").matcher(actiongrp);
                    if (sstr.matches()) {
                        for (String t : sstr.group(1).split(",")) {
                            Matcher n = Pattern.compile("(?:(\\d+))")
                                    .matcher(t);
                            if (n.matches()) {
                                if (n.group(1) != null) {
                                    Short port = Short.parseShort(n.group(1));
                                    if (isPortValid(sw, port) == false) {
                                        resultStr
                                                .append(String
                                                        .format(
                                                                "Output port %d is not valid for this switch",
                                                                port));
                                        if ((container != null)
                                                && !container.getName().equals(
                                                        GlobalConstants.DEFAULT
                                                                .toString())) {
                                            resultStr.append(" in Container "
                                                    + container.getName());
                                        }
                                        return false;
                                    }
                                }
                            }
                        }
                        continue;
                    }
                    // Check src IP
                    sstr = Pattern.compile(ActionType.FLOOD.toString())
                            .matcher(actiongrp);
                    if (sstr.matches()) {
                        if (container != null) {
                            resultStr.append(String.format(
                                    "flood is not allowed in container %s",
                                    container.getName()));
                            return false;
                        }
                        continue;
                    }
                    // Check src IP
                    sstr = Pattern.compile(
                            ActionType.SET_NW_SRC.toString() + "=(.*)")
                            .matcher(actiongrp);
                    if (sstr.matches()) {
                        if (!NetUtils.isIPv4AddressValid(sstr.group(1))) {
                            resultStr.append(String.format(
                                    "IP source address %s is not valid", sstr
                                            .group(1)));
                            return false;
                        }
                        continue;
                    }
                    // Check dst IP
                    sstr = Pattern.compile(
                            ActionType.SET_NW_DST.toString() + "=(.*)")
                            .matcher(actiongrp);
                    if (sstr.matches()) {
                        if (!NetUtils.isIPv4AddressValid(sstr.group(1))) {
                            resultStr.append(String.format(
                                    "IP destination address %s is not valid",
                                    sstr.group(1)));
                            return false;
                        }
                        continue;
                    }

                    sstr = Pattern.compile(
                            ActionType.SET_VLAN_ID.toString() + "=(.*)")
                            .matcher(actiongrp);
                    if (sstr.matches()) {
                        if ((sstr.group(1) != null)
                                && !isVlanIdValid(sstr.group(1))) {
                            resultStr.append(String.format(
                                    "Vlan ID %s is not in the range 0 - 4095",
                                    sstr.group(1)));
                            return false;
                        }
                        continue;
                    }

                    sstr = Pattern.compile(
                            ActionType.SET_VLAN_PCP.toString() + "=(.*)")
                            .matcher(actiongrp);
                    if (sstr.matches()) {
                        if ((sstr.group(1) != null)
                                && !isVlanPriorityValid(sstr.group(1))) {
                            resultStr
                                    .append(String
                                            .format(
                                                    "Vlan priority %s is not in the range 0 - 7",
                                                    sstr.group(1)));
                            return false;
                        }
                        continue;
                    }

                    sstr = Pattern.compile(
                            ActionType.SET_DL_SRC.toString() + "=(.*)")
                            .matcher(actiongrp);
                    if (sstr.matches()) {
                        if ((sstr.group(1) != null)
                                && !isL2AddressValid(sstr.group(1))) {
                            resultStr
                                    .append(String
                                            .format(
                                                    "Ethernet source address %s is not valid. Example: 00:05:b9:7c:81:5f",
                                                    sstr.group(1)));
                            return false;
                        }
                        continue;
                    }

                    sstr = Pattern.compile(
                            ActionType.SET_DL_DST.toString() + "=(.*)")
                            .matcher(actiongrp);
                    if (sstr.matches()) {
                        if ((sstr.group(1) != null)
                                && !isL2AddressValid(sstr.group(1))) {
                            resultStr
                                    .append(String
                                            .format(
                                                    "Ethernet destination address %s is not valid. Example: 00:05:b9:7c:81:5f",
                                                    sstr.group(1)));
                            return false;
                        }
                        continue;
                    }

                    sstr = Pattern.compile(
                            ActionType.SET_NW_TOS.toString() + "=(.*)")
                            .matcher(actiongrp);
                    if (sstr.matches()) {
                        if ((sstr.group(1) != null)
                                && !isTOSBitsValid(sstr.group(1))) {
                            resultStr
                                    .append(String
                                            .format(
                                                    "IP ToS bits %s is not in the range 0 - 63",
                                                    sstr.group(1)));
                            return false;
                        }
                        continue;
                    }

                    sstr = Pattern.compile(
                            ActionType.SET_TP_SRC.toString() + "=(.*)")
                            .matcher(actiongrp);
                    if (sstr.matches()) {
                        if ((sstr.group(1) != null)
                                && !isTpPortValid(sstr.group(1))) {
                            resultStr.append(String.format(
                                    "Transport source port %s is not valid",
                                    sstr.group(1)));
                            return false;
                        }
                        continue;
                    }

                    sstr = Pattern.compile(
                            ActionType.SET_TP_DST.toString() + "=(.*)")
                            .matcher(actiongrp);
                    if (sstr.matches()) {
                        if ((sstr.group(1) != null)
                                && !isTpPortValid(sstr.group(1))) {
                            resultStr
                                    .append(String
                                            .format(
                                                    "Transport destination port %s is not valid",
                                                    sstr.group(1)));
                            return false;
                        }
                        continue;
                    }
                    sstr = Pattern.compile(
                            ActionType.SET_NEXT_HOP.toString() + "=(.*)")
                            .matcher(actiongrp);
                    if (sstr.matches()) {
                        String nextHopInfo = sstr.group(1);
                        String values[] = nextHopInfo.split("//");
                        String address = values[0].trim();

                        if ((address == null) || !isOutputNextHopValid(address)) {
                            resultStr.append(String.format(
                                    "next hop %s is not valid", sstr.group(1)));
                            return false;
                        }
                        continue;
                    }
                }
            }
            // Check against the container flow
            if ((container != null)
                    && conflictWithContainerFlow(container, resultStr)) {
                return false;
            }
        } catch (NumberFormatException e) {
            resultStr.append(String.format("Invalid number format %s", e
                    .getMessage()));
            return false;
        }

        return true;
    }

    public FlowEntry getFlowEntry() {
        return new FlowEntry(FlowConfig.staticFlowsGroup, this.name, this
                .getFlow(), this.getNode());
    }

    public Flow getFlow() {
        Match match = new Match();

        if (this.ingressPort != null) {
            match.setField(MatchType.IN_PORT, NodeConnectorCreator
                    .createOFNodeConnector(Short.parseShort(ingressPort),
                            getNode()));
        }
        if (this.dlSrc != null) {
            match.setField(MatchType.DL_SRC, HexEncode
                    .bytesFromHexString(this.dlSrc));
        }
        if (this.dlDst != null) {
            match.setField(MatchType.DL_DST, HexEncode
                    .bytesFromHexString(this.dlDst));
        }
        if (this.etherType != null) {
            match.setField(MatchType.DL_TYPE, Integer.decode(etherType)
                    .shortValue());
        }
        if (this.vlanId != null) {
            match.setField(MatchType.DL_VLAN, Short.parseShort(this.vlanId));
        }
        if (this.vlanPriority != null) {
            match.setField(MatchType.DL_VLAN_PR, Byte
                    .parseByte(this.vlanPriority));
        }
        if (this.nwSrc != null) {
            String parts[] = this.nwSrc.split("/");
            InetAddress ip = NetUtils.parseInetAddress(parts[0]);
            InetAddress mask = null;
            if (parts.length > 1) {
                int maskLen = Integer.parseInt(parts[1]);
                mask = NetUtils.getInetNetworkMask(maskLen,
                        ip instanceof Inet6Address);
            }
            match.setField(MatchType.NW_SRC, ip, mask);
        }
        if (this.nwDst != null) {
            String parts[] = this.nwDst.split("/");
            InetAddress ip = NetUtils.parseInetAddress(parts[0]);
            InetAddress mask = null;
            if (parts.length > 1) {
                int maskLen = Integer.parseInt(parts[1]);
                mask = NetUtils.getInetNetworkMask(maskLen,
                        ip instanceof Inet6Address);
            }
            match.setField(MatchType.NW_DST, ip, mask);
        }
        if (this.protocol != null) {
            match.setField(MatchType.NW_PROTO, IPProtocols
                    .getProtocolNumberByte(this.protocol));
        }
        if (this.tosBits != null) {
            match.setField(MatchType.NW_TOS, Byte.parseByte(this.tosBits));
        }
        if (this.tpSrc != null) {
            match.setField(MatchType.TP_SRC, Integer.valueOf(this.tpSrc)
                    .shortValue());
        }
        if (this.tpDst != null) {
            match.setField(MatchType.TP_DST, Integer.valueOf(this.tpDst)
                    .shortValue());
        }

        Flow flow = new Flow(match, getActionList());
        if (this.cookie != null) {
            flow.setId(Long.parseLong(cookie));
        }
        if (this.hardTimeout != null) {
            flow.setHardTimeout(Short.parseShort(this.hardTimeout));
        }
        if (this.idleTimeout != null) {
            flow.setIdleTimeout(Short.parseShort(idleTimeout));
        }
        if (this.priority != null) {
            flow.setPriority(Integer.decode(this.priority).shortValue());
        }
        return flow;
    }

    public boolean isOutputNextHopValid(String onh) {
        if (onh == null) {
            return false;
        }
        /*
         * For now, only takes IPv4 or IPv6 address
         */
        return (NetUtils.isIPv4AddressValid(onh) || NetUtils
                .isIPv6AddressValid(onh));
    }

    public boolean isByNameAndNodeIdEqual(FlowConfig that) {
        return (this.name.equals(that.name) && this.node.equals(that.node));
    }

    public boolean isByNameAndNodeIdEqual(String name, Node node) {
        return (this.name.equals(name) && this.node.equals(node));
    }

    public boolean onNode(Node node) {
        return this.node.equals(node);
    }

    public static List<String> getSupportedNextHopTypes() {
        List<String> s = new ArrayList<String>();
        for (SetNextHopType nh : SetNextHopType.values()) {
            s.add(nh.toString());
        }
        return s;
    }

    public void toggleStatus() {
        installInHw = (installInHw == null) ? "true" : (installInHw
                .equals("true")) ? "false" : "true";
    }

    /*
     * Parses the actions string and return the List of SAL Action
     * No syntax check run, as this function will be called when the
     * config validation check has already been performed
     */
    private List<Action> getActionList() {
        List<Action> actionList = new ArrayList<Action>();

        if (actions != null) {
            Matcher sstr;
            for (String actiongrp : actions) {
                sstr = Pattern.compile(ActionType.OUTPUT + "=(.*)").matcher(
                        actiongrp);
                if (sstr.matches()) {
                    for (String t : sstr.group(1).split(",")) {
                        Matcher n = Pattern.compile("(?:(\\d+))").matcher(t);
                        if (n.matches()) {
                            if (n.group(1) != null) {
                                short ofPort = Short.parseShort(n.group(1));
                                actionList.add(new Output(NodeConnectorCreator
                                        .createOFNodeConnector(ofPort, this
                                                .getNode())));
                            }
                        }
                    }
                    continue;
                }

                sstr = Pattern.compile(ActionType.DROP.toString()).matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList.add(new Drop());
                    continue;
                }

                sstr = Pattern.compile(ActionType.LOOPBACK.toString()).matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList.add(new Loopback());
                    continue;
                }

                sstr = Pattern.compile(ActionType.FLOOD.toString()).matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList.add(new Flood());
                    continue;
                }

                sstr = Pattern.compile(ActionType.SW_PATH.toString()).matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList.add(new SwPath());
                    continue;
                }

                sstr = Pattern.compile(ActionType.HW_PATH.toString()).matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList.add(new HwPath());
                    continue;
                }

                sstr = Pattern.compile(ActionType.CONTROLLER.toString())
                        .matcher(actiongrp);
                if (sstr.matches()) {
                    actionList.add(new Controller());
                    continue;
                }

                sstr = Pattern.compile(
                        ActionType.SET_VLAN_ID.toString() + "=(.*)").matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList.add(new SetVlanId(Short
                            .parseShort(sstr.group(1))));
                    continue;
                }

                sstr = Pattern.compile(
                        ActionType.SET_VLAN_PCP.toString() + "=(.*)").matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList
                            .add(new SetVlanPcp(Byte.parseByte(sstr.group(1))));
                    continue;
                }

                sstr = Pattern.compile(ActionType.POP_VLAN.toString()).matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList.add(new PopVlan());
                    continue;
                }

                sstr = Pattern.compile(
                        ActionType.SET_DL_SRC.toString() + "=(.*)").matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList.add(new SetDlSrc(HexEncode
                            .bytesFromHexString(sstr.group(1))));
                    continue;
                }

                sstr = Pattern.compile(
                        ActionType.SET_DL_DST.toString() + "=(.*)").matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList.add(new SetDlDst(HexEncode
                            .bytesFromHexString(sstr.group(1))));
                    continue;
                }
                sstr = Pattern.compile(
                        ActionType.SET_NW_SRC.toString() + "=(.*)").matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList.add(new SetNwSrc(NetUtils.parseInetAddress(sstr
                            .group(1))));
                    continue;
                }
                sstr = Pattern.compile(
                        ActionType.SET_NW_DST.toString() + "=(.*)").matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList.add(new SetNwDst(NetUtils.parseInetAddress(sstr
                            .group(1))));
                    continue;
                }

                sstr = Pattern.compile(
                        ActionType.SET_NW_TOS.toString() + "=(.*)").matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList.add(new SetNwTos(Byte.parseByte(sstr.group(1))));
                    continue;
                }

                sstr = Pattern.compile(
                        ActionType.SET_TP_SRC.toString() + "=(.*)").matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList
                            .add(new SetTpSrc(Integer.valueOf(sstr.group(1))));
                    continue;
                }

                sstr = Pattern.compile(
                        ActionType.SET_TP_DST.toString() + "=(.*)").matcher(
                        actiongrp);
                if (sstr.matches()) {
                    actionList
                            .add(new SetTpDst(Integer.valueOf(sstr.group(1))));
                    continue;
                }

                sstr = Pattern.compile(
                        ActionType.SET_NEXT_HOP.toString() + "=(.*)").matcher(
                        actiongrp);
                if (sstr.matches()) {
                    log.warn("We do not handle next hop action yet....");
                    continue;
                }
            }
        }
        return actionList;
    }

}
