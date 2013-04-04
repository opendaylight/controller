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
import java.util.Set;

import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.authorization.UserLevel;
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
    private final String WEB_NAME = "Troubleshoot";
    private final String WEB_ID = "troubleshoot";
    private final short WEB_ORDER = 4;
    private final String containerName = GlobalConstants.DEFAULT.toString();

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
    public TroubleshootingJsonBean getExistingNodes() {
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        List<HashMap<String, String>> lines = new ArrayList<HashMap<String, String>>();
        Set<Node> nodeSet = null;
        if (switchManager != null) {
            nodeSet = switchManager.getNodes();
        }
        if (nodeSet != null) {
            for (Node node : nodeSet) {
                HashMap<String, String> device = new HashMap<String, String>();
                device.put("nodeName", switchManager.getNodeDescription(node));
                device.put("nodeId", node.toString());
                lines.add(device);
            }
        }
        TroubleshootingJsonBean result = new TroubleshootingJsonBean();

        List<String> guiFieldNames = new ArrayList<String>();
        guiFieldNames.add("Node");
        guiFieldNames.add("Node ID");
        guiFieldNames.add("Statistics");

        result.setColumnNames(guiFieldNames);
        result.setNodeData(lines);
        return result;
    }

    @RequestMapping(value = "/uptime", method = RequestMethod.GET)
    @ResponseBody
    public TroubleshootingJsonBean getUptime() {
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        List<HashMap<String, String>> lines = new ArrayList<HashMap<String, String>>();
        Set<Node> nodeSet = null;
        if (switchManager != null) {
            nodeSet = switchManager.getNodes();
        }
        if (nodeSet != null) {
            for (Node node : nodeSet) {
                HashMap<String, String> device = new HashMap<String, String>();
                device.put("nodeName", switchManager.getNodeDescription(node));
                device.put("nodeId", node.toString());
                TimeStamp timeStamp = (TimeStamp) switchManager.getNodeProp(
                        node, TimeStamp.TimeStampPropName);
                Long time = (timeStamp == null) ? 0 : timeStamp.getValue();
                String date = (time == 0) ? "" : (new Date(time)).toString();
                device.put("connectedSince", date);
                lines.add(device);
            }
        }
        TroubleshootingJsonBean result = new TroubleshootingJsonBean();

        List<String> guiFieldNames = new ArrayList<String>();
        guiFieldNames.add("Node");
        guiFieldNames.add("Node ID");
        guiFieldNames.add("Connected");

        result.setColumnNames(guiFieldNames);
        result.setNodeData(lines);
        return result;
    }

    @RequestMapping(value = "/flowStats", method = RequestMethod.GET)
    @ResponseBody
    public TroubleshootingJsonBean getFlowStats(
            @RequestParam("nodeId") String nodeId) {
        Node node = Node.fromString(nodeId);
        List<HashMap<String, String>> cells = new ArrayList<HashMap<String, String>>();
        IStatisticsManager statisticsManager = (IStatisticsManager) ServiceHelper
                .getInstance(IStatisticsManager.class, containerName, this);

        List<FlowOnNode> statistics = statisticsManager.getFlows(node);
        for (FlowOnNode stats : statistics) {
            cells.add(this.convertFlowStatistics(node, stats));
        }
        List<String> columnNames = new ArrayList<String>();
        columnNames.addAll(Arrays.asList(new String[] { "Node", "In Port",
                "DL Src", "DL Dst", "DL Type", "DL Vlan", "NW Src", "NW Dst",
                "NW Proto", "TP Src", "TP Dst", "Actions", "Bytes", "Packets",
                "Time (s)", "Timeout (s)", "Out Port(s)", "Out Vlan",
                "Priority" }));
        TroubleshootingJsonBean result = new TroubleshootingJsonBean();
        result.setColumnNames(columnNames);
        result.setNodeData(cells);
        return result;
    }

    @RequestMapping(value = "/portStats", method = RequestMethod.GET)
    @ResponseBody
    public TroubleshootingJsonBean getPortStats(
            @RequestParam("nodeId") String nodeId) {
        Node node = Node.fromString(nodeId);
        List<HashMap<String, String>> cells = new ArrayList<HashMap<String, String>>();
        IStatisticsManager statisticsManager = (IStatisticsManager) ServiceHelper
                .getInstance(IStatisticsManager.class, containerName, this);
        List<NodeConnectorStatistics> statistics = statisticsManager
                .getNodeConnectorStatistics(node);
        for (NodeConnectorStatistics stats : statistics) {
            cells.add(this.convertPortsStatistics(stats));
        }
        TroubleshootingJsonBean result = new TroubleshootingJsonBean();
        List<String> columnNames = new ArrayList<String>();
        columnNames.addAll(Arrays.asList(new String[] { "Node Connector",
                "Rx Pkts", "Tx Pkts", "Rx Bytes", "Tx Bytes", "Rx Drops",
                "Tx Drops", "Rx Errs", "Tx Errs", "Rx Frame Errs",
                "Rx OverRun Errs", "Rx CRC Errs", "Collisions" }));
        result.setColumnNames(columnNames);
        result.setNodeData(cells);
        return result;
    }

    private HashMap<String, String> convertPortsStatistics(
            NodeConnectorStatistics ncStats) {
        HashMap<String, String> row = new HashMap<String, String>();

        row.put("nodeConnector",
                String.valueOf(ncStats.getNodeConnector().toString()));
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

    private HashMap<String, String> convertFlowStatistics(Node node,
            FlowOnNode flowOnNode) {
        HashMap<String, String> row = new HashMap<String, String>();
        Flow flow = flowOnNode.getFlow();
        Match match = flow.getMatch();
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        String desc = (switchManager == null)? 
        		"" : switchManager.getNodeDescription(node);
        desc = (desc.isEmpty() || desc.equalsIgnoreCase("none"))? 
        		node.toString(): desc;
        row.put("nodeName", desc);
        if (match.isPresent(MatchType.IN_PORT)) {
            row.put(MatchType.IN_PORT.id(), ((NodeConnector) flow.getMatch()
                    .getField(MatchType.IN_PORT).getValue()).getID().toString());
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
        String outVlanId = null;
        for (Action action : flow.getActions()) {
            actions.append(action.getType().toString() + "\n");
            if (action instanceof Output) {
                Output ao = (Output) action;
                if (outPorts.length() > 0) {
                    outPorts.append(" ");
                }
                outPorts.append(ao.getPort().getNodeConnectorIdAsString());
            } else if (action instanceof SetVlanId) {
                SetVlanId av = (SetVlanId) action;
                outVlanId = String.valueOf(av.getVlanId());
            }
        }
        if (outPorts.length() == 0) {
            outPorts.append("*");
        }
        if (outVlanId == null) {
            outVlanId = "*";
        }
        row.put("actions", actions.toString());
        row.put("outPorts", outPorts.toString());
        row.put("outVlanId", outVlanId);
        row.put("durationSeconds",
                ((Integer) flowOnNode.getDurationSeconds()).toString());
        row.put("idleTimeout", ((Short) flow.getIdleTimeout()).toString());
        row.put("priority", String.valueOf(flow.getPriority()));
        return row;
    }

}
