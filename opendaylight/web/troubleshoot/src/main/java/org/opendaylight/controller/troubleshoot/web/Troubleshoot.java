/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.troubleshoot.web;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.SetDlDst;
import org.opendaylight.controller.sal.action.SetDlSrc;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetNwSrc;
import org.opendaylight.controller.sal.action.SetNwTos;
import org.opendaylight.controller.sal.action.SetTpDst;
import org.opendaylight.controller.sal.action.SetTpSrc;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.action.SetVlanPcp;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.TimeStamp;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.web.DaylightWebUtil;
import org.opendaylight.controller.web.IDaylightWeb;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class Troubleshoot implements IDaylightWeb {
    private static final UserLevel AUTH_LEVEL = UserLevel.CONTAINERUSER;
    private static final List<String> flowStatsColumnNames = Arrays.asList("Node", "In Port",
            "DL Src", "DL Dst", "DL Type", "DL Vlan", "NW Src", "NW Dst",
            "NW Proto", "TP Src", "TP Dst", "Actions", "Bytes", "Packets",
            "Time (s)", "Timeout (s)",
            "Priority");
    private static final List<String> portStatsColumnNames = Arrays.asList("Node Connector",
            "Rx Pkts", "Tx Pkts", "Rx Bytes", "Tx Bytes", "Rx Drops",
            "Tx Drops", "Rx Errs", "Tx Errs", "Rx Frame Errs",
            "Rx OverRun Errs", "Rx CRC Errs", "Collisions");
    private static final List<String> nodesColumnNames = Arrays.asList("Node", "Node ID", "Statistics");
    private static final List<String> nodeStatsColumnNames = Arrays.asList("Node", "Node ID", "Statistics");
    private final String WEB_NAME = "Troubleshoot";
    private final String WEB_ID = "troubleshoot";
    private final short WEB_ORDER = 4;


    public Troubleshoot() {
        ServiceHelper.registerGlobalService(IDaylightWeb.class, this, null);
    }

    @Override
    public String getWebName() {
        return WEB_NAME;
    }

    @Override
    public String getWebId() {
        return WEB_ID;
    }

    @Override
    public short getWebOrder() {
        return WEB_ORDER;
    }

    @Override
    public boolean isAuthorized(UserLevel userLevel) {
        return userLevel.ordinal() <= AUTH_LEVEL.ordinal();
    }

    @RequestMapping(value = "/existingNodes", method = RequestMethod.GET)
    @ResponseBody
    public TroubleshootingJsonBean getExistingNodes(HttpServletRequest request, @RequestParam(required = false) String container) {
        List<Map<String, String>> lines = new ArrayList<Map<String, String>>();
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        Privilege privilege = DaylightWebUtil.getContainerPrivilege(userName, containerName, this);

        if (privilege != Privilege.NONE) {
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                    .getInstance(ISwitchManager.class, containerName, this);
            Set<Node> nodeSet = (switchManager != null) ? switchManager.getNodes() : null;
            if (nodeSet != null) {
                for (Node node : nodeSet) {
                    Map<String, String> device = new HashMap<String, String>();
                    device.put("nodeName", getNodeDesc(node, switchManager));
                    device.put("nodeId", node.toString());
                    lines.add(device);
                }
            }
        }

        TroubleshootingJsonBean result = new TroubleshootingJsonBean();
        result.setColumnNames(nodesColumnNames);
        result.setNodeData(lines);
        return result;
    }

    @RequestMapping(value = "/uptime", method = RequestMethod.GET)
    @ResponseBody
    public TroubleshootingJsonBean getUptime(HttpServletRequest request, @RequestParam(required = false) String container) {
        List<Map<String, String>> lines = new ArrayList<Map<String, String>>();
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        Privilege privilege = DaylightWebUtil.getContainerPrivilege(userName, containerName, this);

        if (privilege != Privilege.NONE) {
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                    .getInstance(ISwitchManager.class, containerName, this);
            Set<Node> nodeSet = (switchManager != null) ? switchManager.getNodes() : null;
            if (nodeSet != null) {
                for (Node node : nodeSet) {
                    Map<String, String> device = new HashMap<String, String>();
                    device.put("nodeName", getNodeDesc(node, switchManager));
                    device.put("nodeId", node.toString());
                    TimeStamp timeStamp = (TimeStamp) switchManager.getNodeProp(
                            node, TimeStamp.TimeStampPropName);
                    Long time = (timeStamp == null) ? 0 : timeStamp.getValue();
                    String date = (time == 0) ? "" : (new Date(time)).toString();
                    device.put("connectedSince", date);
                    lines.add(device);
                }
            }
        }

        TroubleshootingJsonBean result = new TroubleshootingJsonBean();
        result.setColumnNames(nodeStatsColumnNames);
        result.setNodeData(lines);
        return result;
    }

    @RequestMapping(value = "/flowStats", method = RequestMethod.GET)
    @ResponseBody
    public TroubleshootingJsonBean getFlowStats(
            @RequestParam("nodeId") String nodeId,
            HttpServletRequest request, @RequestParam(required = false) String container) {
        List<Map<String, String>> cells = new ArrayList<Map<String, String>>();
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        Privilege privilege = DaylightWebUtil.getContainerPrivilege(userName, containerName, this);

        if (privilege != Privilege.NONE) {
            IStatisticsManager statisticsManager = (IStatisticsManager) ServiceHelper
                    .getInstance(IStatisticsManager.class, containerName, this);
            if (statisticsManager != null) {
                Node node = Node.fromString(nodeId);
                List<FlowOnNode> statistics = statisticsManager.getFlows(node);
                for (FlowOnNode stats : statistics) {
                    cells.add(this.convertFlowStatistics(node, stats, containerName));
                }
            }
        }

        TroubleshootingJsonBean result = new TroubleshootingJsonBean();
        result.setColumnNames(flowStatsColumnNames);
        result.setNodeData(cells);
        return result;
    }

    @RequestMapping(value = "/portStats", method = RequestMethod.GET)
    @ResponseBody
    public TroubleshootingJsonBean getPortStats(
            @RequestParam("nodeId") String nodeId,
            HttpServletRequest request, @RequestParam(required = false) String container) {
        List<Map<String, String>> cells = new ArrayList<Map<String, String>>();
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        Privilege privilege = DaylightWebUtil.getContainerPrivilege(userName, containerName, this);

        if (privilege != Privilege.NONE) {
            IStatisticsManager statisticsManager = (IStatisticsManager) ServiceHelper
                    .getInstance(IStatisticsManager.class, containerName, this);
            if (statisticsManager != null) {
                Node node = Node.fromString(nodeId);
                List<NodeConnectorStatistics> statistics = statisticsManager
                        .getNodeConnectorStatistics(node);
                for (NodeConnectorStatistics stats : statistics) {
                    cells.add(this.convertPortsStatistics(stats, containerName));
                }
            }
        }

        TroubleshootingJsonBean result = new TroubleshootingJsonBean();
        result.setColumnNames(portStatsColumnNames);
        result.setNodeData(cells);
        return result;
    }

    private Map<String, String> convertPortsStatistics(
            NodeConnectorStatistics ncStats, String containerName) {
        Map<String, String> row = new HashMap<String, String>();

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        NodeConnector nodeConnector = ncStats.getNodeConnector();
        Description description = (Description) switchManager.getNodeProp(nodeConnector.getNode(), Description.propertyName);
        String desc = (description == null) ? "" : description.getValue();
        String nodeName = desc.equalsIgnoreCase("none") ? nodeConnector.getNode().getNodeIDString() : desc;
        String nodeConnectorDisplayName = nodeConnector.getType() + "|" + nodeConnector.getID() + "@" + nodeName;
        row.put("nodeConnector",
                String.valueOf(nodeConnectorDisplayName));

        row.put("rxPkts", String.valueOf(ncStats.getReceivePacketCount()));
        row.put("txPkts", String.valueOf(ncStats.getTransmitPacketCount()));
        row.put("rxBytes", String.valueOf(ncStats.getReceiveByteCount()));
        row.put("txBytes", String.valueOf(ncStats.getTransmitByteCount()));
        row.put("rxDrops", String.valueOf(ncStats.getReceiveDropCount()));
        row.put("txDrops", String.valueOf(ncStats.getTransmitDropCount()));
        row.put("rxErrors", String.valueOf(ncStats.getReceiveErrorCount()));
        row.put("txErrors", String.valueOf(ncStats.getTransmitErrorCount()));
        row.put("rxFrameErrors",
                String.valueOf(ncStats.getReceiveFrameErrorCount()));
        row.put("rxOverRunErrors",
                String.valueOf(ncStats.getReceiveOverRunErrorCount()));
        row.put("rxCRCErrors",
                String.valueOf(ncStats.getReceiveCRCErrorCount()));
        row.put("collisions", String.valueOf(ncStats.getCollisionCount()));

        return row;
    }

    private Map<String, String> convertFlowStatistics(Node node,
            FlowOnNode flowOnNode,
            String containerName) {
        Map<String, String> row = new HashMap<String, String>();
        Flow flow = flowOnNode.getFlow();
        Match match = flow.getMatch();
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        String desc = getNodeDesc(node, switchManager);
        desc = (desc == null || desc.isEmpty() || desc.equalsIgnoreCase("none"))?
                        node.toString() : desc;
        row.put("nodeName", desc);
        if (match.isPresent(MatchType.IN_PORT)) {
            row.put(MatchType.IN_PORT.id(), ((NodeConnector) flow.getMatch()
                    .getField(MatchType.IN_PORT).getValue())
                    .getNodeConnectorIdAsString());
        } else {
            row.put(MatchType.IN_PORT.id(), "*");
        }
        if (match.isPresent(MatchType.DL_SRC)) {
            row.put(MatchType.DL_SRC.id(),
                    (HexEncode.bytesToHexString(((byte[]) flow.getMatch()
                            .getField(MatchType.DL_SRC).getValue()))));
        } else {
            row.put(MatchType.DL_SRC.id(), "*");
        }
        if (match.isPresent(MatchType.DL_DST)) {
            row.put(MatchType.DL_DST.id(),
                    (HexEncode.bytesToHexString(((byte[]) flow.getMatch()
                            .getField(MatchType.DL_DST).getValue()))));
        } else {
            row.put(MatchType.DL_DST.id(), "*");
        }
        if (match.isPresent(MatchType.DL_TYPE)) {
            row.put(MatchType.DL_TYPE.id(),
                    EtherTypes.getEtherTypeName(((Short) flow.getMatch()
                            .getField(MatchType.DL_TYPE).getValue())));
        } else {
            row.put(MatchType.DL_TYPE.id(), "*");
        }

        // Some physical switch has vlan as ffff to show "any" vlan
        if (match.isPresent(MatchType.DL_VLAN)) {
            if (((Short) flow.getMatch().getField(MatchType.DL_VLAN).getValue())
                    .shortValue() < 0) {
                row.put(MatchType.DL_VLAN.id(), "0");
            } else {
                row.put(MatchType.DL_VLAN.id(), ((Short) flow.getMatch()
                        .getField(MatchType.DL_VLAN).getValue()).toString());
            }
        } else {
            row.put(MatchType.DL_VLAN.id(), "*");
        }
        if (match.isPresent(MatchType.NW_SRC)) {
            row.put(MatchType.NW_SRC.id(), ((InetAddress) flow.getMatch()
                    .getField(MatchType.NW_SRC).getValue()).getHostAddress());
        } else {
            row.put(MatchType.NW_SRC.id(), "*");
        }
        if (match.isPresent(MatchType.NW_DST)) {
            row.put(MatchType.NW_DST.id(), ((InetAddress) flow.getMatch()
                    .getField(MatchType.NW_DST).getValue()).getHostAddress());
        } else {
            row.put(MatchType.NW_DST.id(), "*");
        }
        if (match.isPresent(MatchType.NW_PROTO)) {
            row.put(MatchType.NW_PROTO.id(),
                    IPProtocols.getProtocolName(((Byte) flow.getMatch()
                            .getField(MatchType.NW_PROTO).getValue())));
        } else {
            row.put(MatchType.NW_PROTO.id(), "*");
        }
        if (match.isPresent(MatchType.TP_SRC)) {
            Short tpSrc = (Short) (flow.getMatch().getField(MatchType.TP_SRC)
                    .getValue());
            row.put(MatchType.TP_SRC.id(),
                        String.valueOf(NetUtils.getUnsignedShort(tpSrc)));
        } else {
            row.put(MatchType.TP_SRC.id(), "*");
        }
        if (match.isPresent(MatchType.TP_DST)) {
            Short tpDst = (Short) (flow.getMatch().getField(MatchType.TP_DST)
                    .getValue());
            row.put(MatchType.TP_DST.id(),
                        String.valueOf(NetUtils.getUnsignedShort(tpDst)));
        } else {
            row.put(MatchType.TP_DST.id(), "*");
        }

        row.put("byteCount", ((Long) flowOnNode.getByteCount()).toString());
        row.put("packetCount", ((Long) flowOnNode.getPacketCount()).toString());

        StringBuffer actions = new StringBuffer();
        StringBuffer outPorts = new StringBuffer();
        for (Action action : flow.getActions()) {

            if (action instanceof Output) {
                Output ao = (Output) action;
                if (outPorts.length() > 0) {
                    outPorts.append(" ");
                }
                actions.append(action.getType().toString()).append(" = ").append(ao.getPort().getNodeConnectorIdAsString()).append("<br>");
            } else if (action instanceof SetVlanId) {
                SetVlanId av = (SetVlanId) action;
                String outVlanId = String.valueOf(av.getVlanId());
                actions.append(action.getType().toString()).append(" = ").append(outVlanId).append("<br>");
            } else if (action instanceof SetDlSrc) {
                SetDlSrc ads = (SetDlSrc) action;
                actions.append(action.getType().toString()).append(" = ").append(HexEncode.bytesToHexStringFormat(ads.getDlAddress())).append("<br>");
            } else if (action instanceof SetDlDst) {
                SetDlDst add = (SetDlDst) action;
                actions.append(action.getType().toString()).append(" = ").append(HexEncode.bytesToHexStringFormat(add.getDlAddress())).append("<br>");
            } else if (action instanceof SetNwSrc) {
                SetNwSrc ans = (SetNwSrc) action;
                actions.append(action.getType().toString()).append(" = ").append(ans.getAddressAsString()).append("<br>");
            } else if (action instanceof SetNwDst) {
                SetNwDst and = (SetNwDst) action;
                actions.append(action.getType().toString()).append(" = ").append(and.getAddressAsString()).append("<br>");
            } else if (action instanceof SetNwTos) {
                SetNwTos ant = (SetNwTos) action;
                actions.append(action.getType().toString()).append(" = ").append(ant.getNwTos()).append("<br>");
            } else if (action instanceof SetTpSrc) {
                SetTpSrc ads = (SetTpSrc) action;
                actions.append(action.getType().toString()).append(" = ").append(ads.getPort()).append("<br>");
            } else if (action instanceof SetTpDst) {
                SetTpDst atd = (SetTpDst) action;
                actions.append(action.getType().toString()).append(" = ").append(atd.getPort()).append("<br>");
            } else if (action instanceof SetVlanPcp) {
                SetVlanPcp avp = (SetVlanPcp) action;
                actions.append(action.getType().toString()).append(" = ").append(avp.getPcp()).append("<br>");
                // } else if (action instanceof SetDlSrc) {
                // SetDlSrc ads = (SetDlSrc) action;
            } else {
                actions.append(action.getType().toString()).append("<br>");
            }
        }
        row.put("actions", actions.toString());
        row.put("durationSeconds",
                ((Integer) flowOnNode.getDurationSeconds()).toString());
        row.put("idleTimeout", ((Short) flow.getIdleTimeout()).toString());
        row.put("priority", String.valueOf(flow.getPriority()));
        return row;
    }

    private String getNodeDesc(Node node, ISwitchManager switchManager) {
        if (switchManager == null) {
            return null;
        }
        Description desc = (Description) switchManager.getNodeProp(node, Description.propertyName);
        return (desc == null) ? "" : desc.getValue();
    }
}
