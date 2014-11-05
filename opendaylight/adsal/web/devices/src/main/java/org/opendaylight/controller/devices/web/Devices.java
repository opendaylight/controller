/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.devices.web;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opendaylight.controller.connectionmanager.IConnectionManager;
import org.opendaylight.controller.forwarding.staticrouting.IForwardingStaticRouting;
import org.opendaylight.controller.forwarding.staticrouting.StaticRouteConfig;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Config;
import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.ForwardingMode;
import org.opendaylight.controller.sal.core.Name;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.Tier;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.sal.utils.TierHelper;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.SpanConfig;
import org.opendaylight.controller.switchmanager.SubnetConfig;
import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.controller.switchmanager.SwitchConfig;
import org.opendaylight.controller.web.DaylightWebUtil;
import org.opendaylight.controller.web.IDaylightWeb;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Controller
@RequestMapping("/")
public class Devices implements IDaylightWeb {
    private static final UserLevel AUTH_LEVEL = UserLevel.CONTAINERUSER;
    private static final String WEB_NAME = "Devices";
    private static final String WEB_ID = "devices";
    private static final short WEB_ORDER = 1;

    public Devices() {
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

    @RequestMapping(value = "/nodesLearnt", method = RequestMethod.GET)
    @ResponseBody
    public DevicesJsonBean getNodesLearnt(HttpServletRequest request, @RequestParam(required = false) String container) {
        Gson gson = new Gson();
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        Privilege privilege = DaylightWebUtil.getContainerPrivilege(userName, containerName, this);

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);
        List<Map<String, String>> nodeData = new ArrayList<Map<String, String>>();
        if (switchManager != null && privilege != Privilege.NONE) {
            for (Switch device : switchManager.getNetworkDevices()) {
                HashMap<String, String> nodeDatum = new HashMap<String, String>();
                Node node = device.getNode();
                Tier tier = (Tier) switchManager.getNodeProp(node, Tier.TierPropName);
                nodeDatum.put("containerName", containerName);
                Description description = (Description) switchManager.getNodeProp(node, Description.propertyName);
                String desc = (description == null) ? "" : description.getValue();
                nodeDatum.put("nodeName", desc);
                nodeDatum.put("nodeId", node.toString());
                int tierNumber = (tier == null) ? TierHelper.unknownTierNumber : tier.getValue();
                nodeDatum.put("tierName", TierHelper.getTierName(tierNumber) + " (Tier-" + tierNumber + ")");
                nodeDatum.put("tier", tierNumber + "");
                String modeStr = "0";
                ForwardingMode mode = null;
                if (!containerName.equals(GlobalConstants.DEFAULT.toString())) {
                    ISwitchManager switchManagerDefault = (ISwitchManager) ServiceHelper.getInstance(
                            ISwitchManager.class, GlobalConstants.DEFAULT.toString(), this);
                    mode = (ForwardingMode) switchManagerDefault.getNodeProp(node, ForwardingMode.name);
                } else {
                    mode = (ForwardingMode) switchManager.getNodeProp(node, ForwardingMode.name);
                }
                if (mode != null) {
                    modeStr = String.valueOf(mode.getValue());
                }
                nodeDatum.put("mode", modeStr);

                nodeDatum.put("json", gson.toJson(nodeDatum));
                nodeDatum.put("mac", HexEncode.bytesToHexStringFormat(device.getDataLayerAddress()));
                StringBuffer sb1 = new StringBuffer();
                Set<NodeConnector> nodeConnectorSet = device.getNodeConnectors();
                if (nodeConnectorSet != null && nodeConnectorSet.size() > 0) {
                    Map<Short, String> portList = new HashMap<Short, String>();
                    List<String> intfList = new ArrayList<String>();
                    for (NodeConnector nodeConnector : nodeConnectorSet) {
                        String nodeConnectorNumberToStr = nodeConnector.getID().toString();
                        Name ncName = ((Name) switchManager.getNodeConnectorProp(nodeConnector, Name.NamePropName));
                        Config portConfig = ((Config) switchManager.getNodeConnectorProp(nodeConnector,
                                Config.ConfigPropName));
                        State portState = ((State) switchManager.getNodeConnectorProp(nodeConnector,
                                State.StatePropName));
                        String nodeConnectorName = (ncName != null) ? ncName.getValue() : "";
                        nodeConnectorName += " (" + nodeConnector.getID() + ")";

                        if (portConfig != null) {
                            if (portConfig.getValue() == Config.ADMIN_UP) {
                                if (portState != null && portState.getValue() == State.EDGE_UP) {
                                    nodeConnectorName = "<span class='admin-up'>" + nodeConnectorName + "</span>";
                                } else if (portState == null || portState.getValue() == State.EDGE_DOWN) {
                                    nodeConnectorName = "<span class='edge-down'>" + nodeConnectorName + "</span>";
                                }
                            } else if (portConfig.getValue() == Config.ADMIN_DOWN) {
                                nodeConnectorName = "<span class='admin-down'>" + nodeConnectorName + "</span>";
                            }
                        }

                        Class<?> idClass = nodeConnector.getID().getClass();
                        if (idClass.equals(Short.class)) {
                            portList.put(Short.parseShort(nodeConnectorNumberToStr), nodeConnectorName);
                        } else {
                            intfList.add(nodeConnectorName);
                        }
                    }

                    if (portList.size() > 0) {
                        Map<Short, String> sortedPortList = new TreeMap<Short, String>(portList);

                        for (Entry<Short, String> e : sortedPortList.entrySet()) {
                            sb1.append(e.getValue());
                            sb1.append("<br>");
                        }
                    } else if (intfList.size() > 0) {
                        for (String intf : intfList) {
                            sb1.append(intf);
                            sb1.append("<br>");
                        }
                    }
                }
                nodeDatum.put("ports", sb1.toString());
                nodeData.add(nodeDatum);
            }
        }

        DevicesJsonBean result = new DevicesJsonBean();
        result.setNodeData(nodeData);
        result.setPrivilege(privilege);
        List<String> columnNames = new ArrayList<String>();
        columnNames.add("Node ID");
        columnNames.add("Node Name");
        columnNames.add("Tier");
        columnNames.add("Mac Address");
        columnNames.add("Ports");
        columnNames.add("Port Status");

        result.setColumnNames(columnNames);
        return result;
    }

    @RequestMapping(value = "/tiers", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getTiers() {
        return TierHelper.getTiers();
    }

    @RequestMapping(value = "/nodesLearnt/update", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean updateLearntNode(@RequestParam("nodeName") String nodeName,
            @RequestParam("nodeId") String nodeId, @RequestParam("tier") String tier,
            @RequestParam("operationMode") String operationMode, HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil.getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class,
                    containerName, this);
            Map<String, Property> nodeProperties = new HashMap<String, Property>();
            Property desc = new Description(nodeName);
            nodeProperties.put(desc.getName(), desc);
            Property nodeTier = new Tier(Integer.parseInt(tier));
            nodeProperties.put(nodeTier.getName(), nodeTier);
            if (containerName.equals(GlobalConstants.DEFAULT.toString())) {
                Property mode = new ForwardingMode(Integer.parseInt(operationMode));
                nodeProperties.put(mode.getName(), mode);
            }
            SwitchConfig cfg = new SwitchConfig(nodeId, nodeProperties);
            Status result = switchManager.updateNodeConfig(cfg);
            if (!result.isSuccess()) {
                resultBean.setStatus(false);
                resultBean.setMessage(result.getDescription());
            } else {
                resultBean.setStatus(true);
                resultBean.setMessage("Updated node information successfully");
                DaylightWebUtil.auditlog("Property", userName, "updated",
                        "of Node " + DaylightWebUtil.getNodeDesc(Node.fromString(nodeId), switchManager));
            }
        } catch (Exception e) {
            resultBean.setStatus(false);
            resultBean.setMessage("Error updating node information. " + e.getMessage());
        }
        return resultBean;
    }

    @RequestMapping(value = "/staticRoutes", method = RequestMethod.GET)
    @ResponseBody
    public DevicesJsonBean getStaticRoutes(HttpServletRequest request, @RequestParam(required = false) String container) {
        Gson gson = new Gson();
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        Privilege privilege = DaylightWebUtil.getContainerPrivilege(userName, containerName, this);

        IForwardingStaticRouting staticRouting = (IForwardingStaticRouting) ServiceHelper.getInstance(
                IForwardingStaticRouting.class, containerName, this);
        if (staticRouting == null) {
            return null;
        }
        List<Map<String, String>> staticRoutes = new ArrayList<Map<String, String>>();
        ConcurrentMap<String, StaticRouteConfig> routeConfigs = staticRouting.getStaticRouteConfigs();
        if (routeConfigs == null) {
            return null;
        }
        if (privilege != Privilege.NONE) {
            for (StaticRouteConfig conf : routeConfigs.values()) {
                Map<String, String> staticRoute = new HashMap<String, String>();
                staticRoute.put("name", conf.getName());
                staticRoute.put("staticRoute", conf.getStaticRoute());
                staticRoute.put("nextHopType", conf.getNextHopType());
                staticRoute.put("nextHop", conf.getNextHop());
                staticRoute.put("json", gson.toJson(conf));
                staticRoutes.add(staticRoute);
            }
        }
        DevicesJsonBean result = new DevicesJsonBean();
        result.setPrivilege(privilege);
        result.setColumnNames(StaticRouteConfig.getGuiFieldsNames());
        result.setNodeData(staticRoutes);
        return result;
    }

    @RequestMapping(value = "/staticRoute/add", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean addStaticRoute(@RequestParam("routeName") String routeName,
            @RequestParam("staticRoute") String staticRoute, @RequestParam("nextHop") String nextHop,
            HttpServletRequest request, @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil.getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return unauthorizedMessage();
        }

        StatusJsonBean result = new StatusJsonBean();
        try {
            IForwardingStaticRouting staticRouting = (IForwardingStaticRouting) ServiceHelper.getInstance(
                    IForwardingStaticRouting.class, containerName, this);
            StaticRouteConfig config = new StaticRouteConfig();
            config.setName(routeName);
            config.setStaticRoute(staticRoute);
            config.setNextHop(nextHop);
            Status addStaticRouteResult = staticRouting.addStaticRoute(config);
            if (addStaticRouteResult.isSuccess()) {
                result.setStatus(true);
                result.setMessage("Static Route saved successfully");
                DaylightWebUtil.auditlog("Static Route", userName, "added", routeName, containerName);
            } else {
                result.setStatus(false);
                result.setMessage(addStaticRouteResult.getDescription());
            }
        } catch (Exception e) {
            result.setStatus(false);
            result.setMessage("Error - " + e.getMessage());
        }
        return result;
    }

    @RequestMapping(value = "/staticRoute/delete", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean deleteStaticRoute(@RequestParam("routesToDelete") String routesToDelete,
            HttpServletRequest request, @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil.getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            IForwardingStaticRouting staticRouting = (IForwardingStaticRouting) ServiceHelper.getInstance(
                    IForwardingStaticRouting.class, containerName, this);
            String[] routes = routesToDelete.split(",");
            Status result;
            resultBean.setStatus(true);
            resultBean.setMessage("Successfully deleted selected static routes");
            for (String route : routes) {
                result = staticRouting.removeStaticRoute(route);
                if (!result.isSuccess()) {
                    resultBean.setStatus(false);
                    resultBean.setMessage(result.getDescription());
                    break;
                }
                DaylightWebUtil.auditlog("Static Route", userName, "removed", route, containerName);
            }
        } catch (Exception e) {
            resultBean.setStatus(false);
            resultBean.setMessage("Error occurred while deleting static routes. " + e.getMessage());
        }
        return resultBean;
    }

    @RequestMapping(value = "/subnets", method = RequestMethod.GET)
    @ResponseBody
    public DevicesJsonBean getSubnetGateways(HttpServletRequest request,
            @RequestParam(required = false) String container) {
        Gson gson = new Gson();
        List<Map<String, String>> subnets = new ArrayList<Map<String, String>>();
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        Privilege privilege = DaylightWebUtil.getContainerPrivilege(userName, containerName, this);

        if (privilege != Privilege.NONE) {
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class,
                    containerName, this);
            if (switchManager != null) {
                for (SubnetConfig conf : switchManager.getSubnetsConfigList()) {
                    Map<String, String> subnet = new HashMap<String, String>();
                    subnet.put("name", conf.getName());
                    subnet.put("subnet", conf.getSubnet());
                    List<SubnetGatewayPortBean> portsList = new ArrayList<SubnetGatewayPortBean>();
                    Iterator<NodeConnector> itor = conf.getNodeConnectors().iterator();
                    while (itor.hasNext()) {
                        SubnetGatewayPortBean bean = new SubnetGatewayPortBean();
                        NodeConnector nodeConnector = itor.next();
                        String nodeName = getNodeDesc(nodeConnector.getNode().toString(), containerName);
                        Name ncName = ((Name) switchManager.getNodeConnectorProp(nodeConnector, Name.NamePropName));
                        String nodeConnectorName = (ncName != null) ? ncName.getValue() : "";
                        bean.setNodeName(nodeName);
                        bean.setNodePortName(nodeConnectorName);
                        bean.setNodeId(nodeConnector.getNode().toString());
                        bean.setNodePortId(nodeConnector.toString());
                        portsList.add(bean);
                    }
                    subnet.put("nodePorts", gson.toJson(portsList));
                    subnets.add(subnet);
                }
            }
        }
        DevicesJsonBean result = new DevicesJsonBean();
        result.setPrivilege(privilege);
        result.setColumnNames(SubnetConfig.getGuiFieldsNames());
        result.setNodeData(subnets);
        return result;
    }

    @RequestMapping(value = "/subnetGateway/add", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean addSubnetGateways(@RequestParam("gatewayName") String gatewayName,
            @RequestParam("gatewayIPAddress") String gatewayIPAddress, HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil.getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class,
                    containerName, this);
            SubnetConfig cfgObject = new SubnetConfig(gatewayName, gatewayIPAddress, new ArrayList<String>());
            Status result = switchManager.addSubnet(cfgObject);
            if (result.isSuccess()) {
                resultBean.setStatus(true);
                resultBean.setMessage("Added gateway address successfully");
                DaylightWebUtil.auditlog("Subnet Gateway", userName, "added", gatewayName, containerName);
            } else {
                resultBean.setStatus(false);
                resultBean.setMessage(result.getDescription());
            }
        } catch (Exception e) {
            resultBean.setStatus(false);
            resultBean.setMessage(e.getMessage());
        }
        return resultBean;
    }

    @RequestMapping(value = "/subnetGateway/delete", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean deleteSubnetGateways(@RequestParam("gatewaysToDelete") String gatewaysToDelete,
            HttpServletRequest request, @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil.getContainerPrivilege(userName, container, this) != Privilege.WRITE) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class,
                    containerName, this);
            String[] subnets = gatewaysToDelete.split(",");
            resultBean.setStatus(true);
            resultBean.setMessage("Added gateway address successfully");
            for (String subnet : subnets) {
                Status result = switchManager.removeSubnet(subnet);
                if (!result.isSuccess()) {
                    resultBean.setStatus(false);
                    resultBean.setMessage(result.getDescription());
                    break;
                }
                DaylightWebUtil.auditlog("Subnet Gateway", userName, "removed", subnet, containerName);
            }
        } catch (Exception e) {
            resultBean.setStatus(false);
            resultBean.setMessage(e.getMessage());
        }
        return resultBean;
    }

    @RequestMapping(value = "/subnetGateway/ports/add", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean addSubnetGatewayPort(@RequestParam("portsName") String portsName,
            @RequestParam("ports") String ports, @RequestParam("nodeId") String nodeId, HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil.getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class,
                    containerName, this);
            List<String> toAdd = new ArrayList<String>();
            for (String port : ports.split(",")) {
                toAdd.add(port);
            }
            Status result = switchManager.addPortsToSubnet(portsName, toAdd);

            if (result.isSuccess()) {
                resultBean.setStatus(true);
                resultBean.setMessage("Added ports to subnet gateway address successfully");
                for (String port : toAdd) {
                    DaylightWebUtil.auditlog("Port", userName, "added",
                            DaylightWebUtil.getPortName(NodeConnector.fromString(port), switchManager)
                            + " to Subnet Gateway " + portsName, containerName);
                }
            } else {
                resultBean.setStatus(false);
                resultBean.setMessage(result.getDescription());
            }
        } catch (Exception e) {
            resultBean.setStatus(false);
            resultBean.setMessage(e.getMessage());
        }
        return resultBean;
    }

    @RequestMapping(value = "/subnetGateway/ports/delete", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean deleteSubnetGatewayPort(@RequestParam("gatewayName") String gatewayName,
            @RequestParam("nodePort") String nodePort, HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil.getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class,
                    containerName, this);
            List<String> toRemove = new ArrayList<String>();
            for (String port : nodePort.split(",")) {
                toRemove.add(port);
            }
            Status result = switchManager.removePortsFromSubnet(gatewayName, toRemove);

            if (result.isSuccess()) {
                resultBean.setStatus(true);
                resultBean.setMessage("Deleted port from subnet gateway address successfully");
                for (String port : toRemove) {
                    DaylightWebUtil.auditlog("Port", userName, "removed",
                            DaylightWebUtil.getPortName(NodeConnector.fromString(port), switchManager)
                            + " from Subnet Gateway " + gatewayName, containerName);
                }
            } else {
                resultBean.setStatus(false);
                resultBean.setMessage(result.getDescription());
            }
        } catch (Exception e) {
            resultBean.setStatus(false);
            resultBean.setMessage(e.getMessage());
        }
        return resultBean;
    }

    @RequestMapping(value = "/spanPorts", method = RequestMethod.GET)
    @ResponseBody
    public DevicesJsonBean getSpanPorts(HttpServletRequest request, @RequestParam(required = false) String container) {
        Gson gson = new Gson();
        List<Map<String, String>> spanConfigs = new ArrayList<Map<String, String>>();
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        Privilege privilege = DaylightWebUtil.getContainerPrivilege(userName, containerName, this);

        if (privilege != Privilege.NONE) {
            List<String> spanConfigs_json = new ArrayList<String>();
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class,
                    containerName, this);
            if (switchManager != null) {
                for (SpanConfig conf : switchManager.getSpanConfigList()) {
                    spanConfigs_json.add(gson.toJson(conf));
                }
            }
            ObjectMapper mapper = new ObjectMapper();

            for (String config_json : spanConfigs_json) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> config_data = mapper.readValue(config_json, HashMap.class);
                    Map<String, String> config = new HashMap<String, String>();
                    for (String name : config_data.keySet()) {
                        config.put(name, config_data.get(name));
                        // Add switch portName value (non-configuration field)
                        config.put("nodeName", getNodeDesc(config_data.get("nodeId"), containerName));
                        NodeConnector spanPortNodeConnector = NodeConnector.fromString(config_data.get("spanPort"));
                        Name ncName = ((Name) switchManager.getNodeConnectorProp(spanPortNodeConnector,
                                Name.NamePropName));
                        String spanPortName = (ncName != null) ? ncName.getValue() : "";
                        config.put("spanPortName", spanPortName);
                    }
                    config.put("json", config_json);
                    spanConfigs.add(config);
                } catch (Exception e) {
                    // TODO: Handle the exception.
                }
            }
        }

        DevicesJsonBean result = new DevicesJsonBean();
        result.setPrivilege(privilege);
        result.setColumnNames(SpanConfig.getGuiFieldsNames());
        result.setNodeData(spanConfigs);
        return result;
    }

    @RequestMapping(value = "/nodeports")
    @ResponseBody
    public String getNodePorts(HttpServletRequest request, @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil.getContainerPrivilege(userName, containerName, this) == Privilege.NONE) {
            return null;
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);
        if (switchManager == null) {
            return null;
        }
        List<NodeJsonBean> nodeJsonBeans = new ArrayList<NodeJsonBean>();

        for (Switch node : switchManager.getNetworkDevices()) {
            NodeJsonBean nodeJsonBean = new NodeJsonBean();
            List<PortJsonBean> port = new ArrayList<PortJsonBean>();
            Set<NodeConnector> nodeConnectorSet = node.getNodeConnectors();
            if (nodeConnectorSet != null) {
                for (NodeConnector nodeConnector : nodeConnectorSet) {
                    String nodeConnectorName = ((Name) switchManager.getNodeConnectorProp(nodeConnector,
                            Name.NamePropName)).getValue();
                    port.add(new PortJsonBean(nodeConnector.getID().toString(), nodeConnectorName, nodeConnector
                            .toString()));
                }
            }
            nodeJsonBean.setNodeId(node.getNode().toString());
            nodeJsonBean.setNodeName(getNodeDesc(node.getNode().toString(), containerName));
            nodeJsonBean.setNodePorts(port);
            nodeJsonBeans.add(nodeJsonBean);
        }

        return new Gson().toJson(nodeJsonBeans);
    }

    @RequestMapping(value = "/spanPorts/add", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean addSpanPort(@RequestParam("jsonData") String jsonData, HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil.getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            Gson gson = new Gson();
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class,
                    containerName, this);
            SpanConfig cfgObject = gson.fromJson(jsonData, SpanConfig.class);
            Status result = switchManager.addSpanConfig(cfgObject);
            if (result.isSuccess()) {
                resultBean.setStatus(true);
                resultBean.setMessage("SPAN Port added successfully");
                DaylightWebUtil.auditlog("SPAN Port", userName, "added",
                        DaylightWebUtil.getPortName(NodeConnector.fromString(cfgObject.getSpanPort()), switchManager),
                        containerName);
            } else {
                resultBean.setStatus(false);
                resultBean.setMessage(result.getDescription());
            }
        } catch (Exception e) {
            resultBean.setStatus(false);
            resultBean.setMessage("Error occurred while adding span port. " + e.getMessage());
        }
        return resultBean;
    }

    @RequestMapping(value = "/spanPorts/delete", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean deleteSpanPorts(@RequestParam("spanPortsToDelete") String spanPortsToDelete,
            HttpServletRequest request, @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil.getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            Gson gson = new Gson();
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class,
                    containerName, this);
            Type collectionType = new TypeToken<List<SpanPortJsonBean>>() {
            }.getType();
            List<SpanPortJsonBean> jsonBeanList = gson.fromJson(spanPortsToDelete, collectionType);
            for (SpanPortJsonBean jsonBean : jsonBeanList) {
                SpanConfig cfgObject = gson.fromJson(gson.toJson(jsonBean), SpanConfig.class);
                Status result = switchManager.removeSpanConfig(cfgObject);
                if (!result.isSuccess()) {
                    resultBean.setStatus(false);
                    resultBean.setMessage(result.getDescription());
                    break;
                }
                DaylightWebUtil.auditlog("SPAN Port", userName, "removed",
                        DaylightWebUtil.getPortName(NodeConnector.fromString(cfgObject.getSpanPort()), switchManager),
                        containerName);
            }
            resultBean.setStatus(true);
            resultBean.setMessage("SPAN Port(s) deleted successfully");
        } catch (Exception e) {
            resultBean.setStatus(false);
            resultBean.setMessage("Error occurred while deleting span port. " + e.getMessage());
        }
        return resultBean;
    }

    @RequestMapping(value = "/connect/nodes", method = RequestMethod.GET)
    @ResponseBody
    public List<NodeJsonBean> getNodes(HttpServletRequest request) {
        IConnectionManager connectionManager = (IConnectionManager) ServiceHelper.getGlobalInstance(
                IConnectionManager.class, this);
        if (connectionManager == null) {
            return null;
        }
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class,
                GlobalConstants.DEFAULT.toString(), this);
        if (switchManager == null) {
            return null;
        }

        Set<Node> nodes = connectionManager.getLocalNodes();
        List<NodeJsonBean> result = new LinkedList<NodeJsonBean>();
        if (nodes == null) {
            return result;
        }
        for (Node node : nodes) {
            Description descriptionProperty = (Description) switchManager.getNodeProp(node, "description");
            String description = node.toString();
            if (descriptionProperty != null) {
                description = descriptionProperty.getValue();
            }
            NodeJsonBean nodeBean = new NodeJsonBean();
            nodeBean.setNodeId(node.getNodeIDString());
            nodeBean.setNodeType(node.getType());
            if (description.equals("None")) {
                nodeBean.setNodeName(node.toString());
            } else {
                nodeBean.setNodeName(description);
            }
            result.add(nodeBean);
        }

        return result;
    }

    @RequestMapping(value = "/connect/{nodeId:.+}", method = RequestMethod.POST)
    @ResponseBody
    public Status addNode(HttpServletRequest request, @PathVariable("nodeId") String nodeId,
            @RequestParam(required = true) String ipAddress, @RequestParam(required = true) String port,
            @RequestParam(required = false) String nodeType) {
        IConnectionManager connectionManager = (IConnectionManager) ServiceHelper.getGlobalInstance(
                IConnectionManager.class, this);
        if (connectionManager == null) {
            return new Status(StatusCode.NOTFOUND, "Service not found");
        }

        if (!NetUtils.isIPv4AddressValid(ipAddress)) {
            return new Status(StatusCode.NOTACCEPTABLE, "Invalid IP Address: " + ipAddress);
        }

        try {
            Integer.parseInt(port);
        } catch (Exception e) {
            return new Status(StatusCode.NOTACCEPTABLE, "Invalid Layer 4 Port: " + port);
        }

        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        params.put(ConnectionConstants.ADDRESS, ipAddress);
        params.put(ConnectionConstants.PORT, port);

        Node node = null;
        if (nodeType != null) {
            node = connectionManager.connect(nodeType, nodeId, params);
        } else {
            node = connectionManager.connect(nodeId, params);
        }
        if (node == null) {
            return new Status(StatusCode.NOTFOUND, "Failed to connect to Node at " + ipAddress + ":" + port);
        }
        return new Status(StatusCode.SUCCESS);
    }

    @RequestMapping(value = "/disconnect/{nodeId:.+}", method = RequestMethod.POST)
    @ResponseBody
    public Status removeNode(HttpServletRequest request, @PathVariable("nodeId") String nodeId,
            @RequestParam(required = true) String nodeType) {
        IConnectionManager connectionManager = (IConnectionManager) ServiceHelper.getGlobalInstance(
                IConnectionManager.class, this);
        if (connectionManager == null) {
            return new Status(StatusCode.NOTFOUND, "Service not found");
        }

        try {
            Node node = new Node(nodeType, nodeId);
            return connectionManager.disconnect(node);
        } catch (Exception e) {
            return new Status(StatusCode.NOTFOUND, "Resource not found");
        }
    }

    private String getNodeDesc(String nodeId, String containerName) {
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);
        String description = "";
        if (switchManager != null) {
            Description desc = (Description) switchManager.getNodeProp(Node.fromString(nodeId),
                    Description.propertyName);
            if (desc != null) {
                description = desc.getValue();
            }
        }
        return (description.isEmpty() || description.equalsIgnoreCase("none")) ? nodeId : description;
    }

    private StatusJsonBean unauthorizedMessage() {
        StatusJsonBean message = new StatusJsonBean();
        message.setStatus(false);
        message.setMessage("Operation not authorized");
        return message;
    }

    @RequestMapping(value = "login")
    public String login(final HttpServletRequest request, final HttpServletResponse response) {
        /*
         * IUserManager userManager = (IUserManager) ServiceHelper
         * .getGlobalInstance(IUserManager.class, this); if (userManager ==
         * null) { return "User Manager is not available"; }
         *
         * String username = request.getUserPrincipal().getName();
         *
         *
         * model.addAttribute("username", username); model.addAttribute("role",
         * userManager.getUserLevel(username).toNumber());
         */
        return "forward:" + "/";
    }

}
