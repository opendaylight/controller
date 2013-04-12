/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.devices.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.web.IDaylightWeb;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.opendaylight.controller.forwarding.staticrouting.IForwardingStaticRouting;
import org.opendaylight.controller.forwarding.staticrouting.StaticRouteConfig;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.core.Name;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Tier;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.TierHelper;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.SpanConfig;
import org.opendaylight.controller.switchmanager.SubnetConfig;
import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.controller.switchmanager.SwitchConfig;

import com.google.gson.Gson;

@Controller
@RequestMapping("/")
public class Devices implements IDaylightWeb {
    private static final UserLevel AUTH_LEVEL = UserLevel.CONTAINERUSER;
    private final String WEB_NAME = "Devices";
    private final String WEB_ID = "devices";
    private final short WEB_ORDER = 1;
    private final String containerName = GlobalConstants.DEFAULT.toString();

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
    public DevicesJsonBean getNodesLearnt() {
        Gson gson = new Gson();
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        List<Map<String, String>> nodeData = new ArrayList<Map<String, String>>();
        for (Switch device : switchManager.getNetworkDevices()) {
            HashMap<String, String> nodeDatum = new HashMap<String, String>();
            Node node = device.getNode();
            Tier tier = (Tier) switchManager.getNodeProp(node,
                    Tier.TierPropName);

            nodeDatum.put("containerName", containerName);
            nodeDatum.put("nodeName", switchManager.getNodeDescription(node));
            nodeDatum.put("nodeId", node.toString());
            int tierNumber = (tier == null) ? TierHelper.unknownTierNumber
                    : tier.getValue();
            nodeDatum.put("tierName", TierHelper.getTierName(tierNumber)
                    + " (Tier-" + tierNumber + ")");
            nodeDatum.put("tier", tierNumber + "");
            SwitchConfig sc = switchManager.getSwitchConfig(device.getNode()
                    .toString());
            String modeStr = (sc != null) ? sc.getMode() : "0";
            nodeDatum.put("mode", modeStr);

            nodeDatum.put("json", gson.toJson(nodeDatum));
            nodeDatum.put("mac",
                    HexEncode.bytesToHexString(device.getDataLayerAddress()));
            StringBuffer sb1 = new StringBuffer();
            Set<NodeConnector> nodeConnectorSet = device.getNodeConnectors();
            String nodeConnectorName;
            String nodeConnectorNumberToStr;
            if (nodeConnectorSet != null && nodeConnectorSet.size() > 0) {
                Map<Short, String> portList = new HashMap<Short, String>();
                for (NodeConnector nodeConnector : nodeConnectorSet) {
                    nodeConnectorNumberToStr = nodeConnector.getID().toString();
                    Name ncName = ((Name) switchManager.getNodeConnectorProp(
                            nodeConnector, Name.NamePropName));
                    nodeConnectorName = (ncName != null) ? ncName.getValue()
                            : "";
                    portList.put(Short.parseShort(nodeConnectorNumberToStr),
                            nodeConnectorName);
                }
                Map<Short, String> sortedPortList = new TreeMap<Short, String>(
                        portList);
                for (Entry<Short, String> e : sortedPortList.entrySet()) {
                    sb1.append(e.getValue() + "(" + e.getKey() + ")");
                    sb1.append("<br>");
                }
            }
            nodeDatum.put("ports", sb1.toString());
            nodeData.add(nodeDatum);
        }
        DevicesJsonBean result = new DevicesJsonBean();
        result.setNodeData(nodeData);
        List<String> columnNames = new ArrayList<String>();
        columnNames.add("Node ID");
        columnNames.add("Node Name");
        columnNames.add("Tier");
        columnNames.add("Mac Address");
        columnNames.add("Ports");

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
    public StatusJsonBean updateLearntNode(
            @RequestParam("nodeName") String nodeName,
            @RequestParam("nodeId") String nodeId,
            @RequestParam("tier") String tier,
            @RequestParam("operationMode") String operationMode,
            HttpServletRequest request) {
        if (!authorize(UserLevel.NETWORKADMIN, request)) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                    .getInstance(ISwitchManager.class, containerName, this);
            SwitchConfig cfg = new SwitchConfig(nodeId, nodeName, tier,
                    operationMode);
            switchManager.updateSwitchConfig(cfg);
            resultBean.setStatus(true);
            resultBean.setMessage("Updated node information successfully");
        } catch (Exception e) {
            resultBean.setStatus(false);
            resultBean.setMessage("Error updating node information. "
                    + e.getMessage());
        }
        return resultBean;
    }

    @RequestMapping(value = "/staticRoutes", method = RequestMethod.GET)
    @ResponseBody
    public DevicesJsonBean getStaticRoutes() {
        Gson gson = new Gson();
        IForwardingStaticRouting staticRouting = (IForwardingStaticRouting) ServiceHelper
                .getInstance(IForwardingStaticRouting.class, containerName,
                        this);
        List<Map<String, String>> staticRoutes = new ArrayList<Map<String, String>>();
        ConcurrentMap<String, StaticRouteConfig> routeConfigs = staticRouting
                .getStaticRouteConfigs();
        if (routeConfigs == null) {
            return null;
        }
        for (StaticRouteConfig conf : routeConfigs.values()) {
            Map<String, String> staticRoute = new HashMap<String, String>();
            staticRoute.put("name", conf.getName());
            staticRoute.put("staticRoute", conf.getStaticRoute());
            staticRoute.put("nextHopType", conf.getNextHopType());
            staticRoute.put("nextHop", conf.getNextHop());
            staticRoute.put("json", gson.toJson(conf));
            staticRoutes.add(staticRoute);
        }
        DevicesJsonBean result = new DevicesJsonBean();
        result.setColumnNames(StaticRouteConfig.getGuiFieldsNames());
        result.setNodeData(staticRoutes);
        return result;
    }

    @RequestMapping(value = "/staticRoute/add", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean addStaticRoute(
            @RequestParam("routeName") String routeName,
            @RequestParam("staticRoute") String staticRoute,
            @RequestParam("nextHop") String nextHop, HttpServletRequest request) {
        if (!authorize(UserLevel.NETWORKADMIN, request)) {
            return unauthorizedMessage();
        }

        StatusJsonBean result = new StatusJsonBean();
        try {
            IForwardingStaticRouting staticRouting = (IForwardingStaticRouting) ServiceHelper
                    .getInstance(IForwardingStaticRouting.class, containerName,
                            this);
            StaticRouteConfig config = new StaticRouteConfig();
            config.setName(routeName);
            config.setStaticRoute(staticRoute);
            config.setNextHop(nextHop);
            Status addStaticRouteResult = staticRouting.addStaticRoute(config);
            if (addStaticRouteResult.isSuccess()) {
                result.setStatus(true);
                result.setMessage("Static Route saved successfully");
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
    public StatusJsonBean deleteStaticRoute(
            @RequestParam("routesToDelete") String routesToDelete,
            HttpServletRequest request) {
        if (!authorize(UserLevel.NETWORKADMIN, request)) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            IForwardingStaticRouting staticRouting = (IForwardingStaticRouting) ServiceHelper
                    .getInstance(IForwardingStaticRouting.class, containerName,
                            this);
            String[] routes = routesToDelete.split(",");
            Status result;
            resultBean.setStatus(true);
            resultBean
                    .setMessage("Successfully deleted selected static routes");
            for (String route : routes) {
                result = staticRouting.removeStaticRoute(route);
                if (!result.isSuccess()) {
                    resultBean.setStatus(false);
                    resultBean.setMessage(result.getDescription());
                    break;
                }
            }
        } catch (Exception e) {
            resultBean.setStatus(false);
            resultBean
                    .setMessage("Error occurred while deleting static routes. "
                            + e.getMessage());
        }
        return resultBean;
    }

    @RequestMapping(value = "/subnets", method = RequestMethod.GET)
    @ResponseBody
    public DevicesJsonBean getSubnetGateways() {
        Gson gson = new Gson();
        List<Map<String, String>> subnets = new ArrayList<Map<String, String>>();
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        for (SubnetConfig conf : switchManager.getSubnetsConfigList()) {
            Map<String, String> subnet = new HashMap<String, String>();
            subnet.put("name", conf.getName());
            subnet.put("subnet", conf.getSubnet());
            subnet.put("json", gson.toJson(conf));
            subnets.add(subnet);
        }
        DevicesJsonBean result = new DevicesJsonBean();
        result.setColumnNames(SubnetConfig.getGuiFieldsNames());
        result.setNodeData(subnets);
        return result;
    }

    @RequestMapping(value = "/subnetGateway/add", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean addSubnetGateways(
            @RequestParam("gatewayName") String gatewayName,
            @RequestParam("gatewayIPAddress") String gatewayIPAddress,
            HttpServletRequest request) {
        if (!authorize(UserLevel.NETWORKADMIN, request)) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                    .getInstance(ISwitchManager.class, containerName, this);
            SubnetConfig cfgObject = new SubnetConfig(gatewayName,
                    gatewayIPAddress, new ArrayList<String>());
            Status result = switchManager.addSubnet(cfgObject);
            if (result.isSuccess()) {
                resultBean.setStatus(true);
                resultBean.setMessage("Added gateway address successfully");
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
    public StatusJsonBean deleteSubnetGateways(
            @RequestParam("gatewaysToDelete") String gatewaysToDelete,
            HttpServletRequest request) {
        if (!authorize(UserLevel.NETWORKADMIN, request)) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                    .getInstance(ISwitchManager.class, containerName, this);
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
            }
        } catch (Exception e) {
            resultBean.setStatus(false);
            resultBean.setMessage(e.getMessage());
        }
        return resultBean;
    }

    @RequestMapping(value = "/subnetGateway/ports/add", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean addSubnetGatewayPort(
            @RequestParam("portsName") String portsName,
            @RequestParam("ports") String ports,
            @RequestParam("nodeId") String nodeId, HttpServletRequest request) {
        if (!authorize(UserLevel.NETWORKADMIN, request)) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                    .getInstance(ISwitchManager.class, containerName, this);
            Status result = switchManager.addPortsToSubnet(portsName, nodeId
                    + "/" + ports);

            if (result.isSuccess()) {
                resultBean.setStatus(true);
                resultBean
                        .setMessage("Added ports to subnet gateway address successfully");
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
    public StatusJsonBean deleteSubnetGatewayPort(
            @RequestParam("gatewayName") String gatewayName,
            @RequestParam("nodePort") String nodePort,
            HttpServletRequest request) {
        if (!authorize(UserLevel.NETWORKADMIN, request)) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                    .getInstance(ISwitchManager.class, containerName, this);
            Status result = switchManager.removePortsFromSubnet(gatewayName,
                    nodePort);

            if (result.isSuccess()) {
                resultBean.setStatus(true);
                resultBean
                        .setMessage("Deleted port from subnet gateway address successfully");
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
    public DevicesJsonBean getSpanPorts() {
        Gson gson = new Gson();
        List<String> spanConfigs_json = new ArrayList<String>();
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        for (SpanConfig conf : switchManager.getSpanConfigList()) {
            spanConfigs_json.add(gson.toJson(conf));
        }
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, String>> spanConfigs = new ArrayList<Map<String, String>>();
        for (String config_json : spanConfigs_json) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> config_data = mapper.readValue(config_json,
                        HashMap.class);
                Map<String, String> config = new HashMap<String, String>();
                for (String name : config_data.keySet()) {
                    config.put(name, config_data.get(name));
                    // Add switch name value (non-configuration field)
                    config.put("nodeName",
                            getNodeDesc(config_data.get("nodeId")));
                }
                config.put("json", config_json);
                spanConfigs.add(config);
            } catch (Exception e) {
                // TODO: Handle the exception.
            }
        }
        DevicesJsonBean result = new DevicesJsonBean();
        result.setColumnNames(SpanConfig.getGuiFieldsNames());
        result.setNodeData(spanConfigs);
        return result;
    }

    @RequestMapping(value = "/nodeports")
    @ResponseBody
    public Map<String, Object> getNodePorts() {
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null)
            return null;

        Map<String, Object> nodes = new HashMap<String, Object>();
        Map<Short, String> port;

        for (Switch node : switchManager.getNetworkDevices()) {
            port = new HashMap<Short, String>(); // new port
            Set<NodeConnector> nodeConnectorSet = node.getNodeConnectors();

            if (nodeConnectorSet != null)
                for (NodeConnector nodeConnector : nodeConnectorSet) {
                    String nodeConnectorName = ((Name) switchManager
                            .getNodeConnectorProp(nodeConnector,
                                    Name.NamePropName)).getValue();
                    port.put((Short) nodeConnector.getID(), nodeConnectorName
                            + "(" + nodeConnector.getID() + ")");
                }

            nodes.put(node.getNode().toString(), port);
        }

        return nodes;
    }

    @RequestMapping(value = "/spanPorts/add", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean addSpanPort(
            @RequestParam("jsonData") String jsonData,
            HttpServletRequest request) {
        if (!authorize(UserLevel.NETWORKADMIN, request)) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            Gson gson = new Gson();
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                    .getInstance(ISwitchManager.class, containerName, this);
            SpanConfig cfgObject = gson.fromJson(jsonData, SpanConfig.class);
            Status result = switchManager.addSpanConfig(cfgObject);
            if (result.isSuccess()) {
                resultBean.setStatus(true);
                resultBean.setMessage("SPAN Port added successfully");
            } else {
                resultBean.setStatus(false);
                resultBean.setMessage(result.getDescription());
            }
        } catch (Exception e) {
            resultBean.setStatus(false);
            resultBean.setMessage("Error occurred while adding span port. "
                    + e.getMessage());
        }
        return resultBean;
    }

    @RequestMapping(value = "/spanPorts/delete", method = RequestMethod.GET)
    @ResponseBody
    public StatusJsonBean deleteSpanPorts(
            @RequestParam("spanPortsToDelete") String spanPortsToDelete,
            HttpServletRequest request) {
        if (!authorize(UserLevel.NETWORKADMIN, request)) {
            return unauthorizedMessage();
        }

        StatusJsonBean resultBean = new StatusJsonBean();
        try {
            Gson gson = new Gson();
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                    .getInstance(ISwitchManager.class, containerName, this);
            String[] spans = spanPortsToDelete.split("###");
            resultBean.setStatus(true);
            resultBean.setMessage("SPAN Port(s) deleted successfully");
            for (String span : spans) {
                if (!span.isEmpty()) {
                    SpanConfig cfgObject = gson
                            .fromJson(span, SpanConfig.class);
                    Status result = switchManager.removeSpanConfig(cfgObject);
                    if (!result.isSuccess()) {
                        resultBean.setStatus(false);
                        resultBean.setMessage(result.getDescription());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            resultBean.setStatus(false);
            resultBean.setMessage("Error occurred while deleting span port. "
                    + e.getMessage());
        }
        return resultBean;
    }

    private String getNodeDesc(String nodeId) {
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        String description = "";
        if (switchManager != null) {
            description = switchManager.getNodeDescription(Node
                    .fromString(nodeId));
        }
        return (description.isEmpty() || description.equalsIgnoreCase("none")) ? nodeId
                : description;
    }

    /**
     * Is the operation permitted for the given level
     * 
     * @param level
     */
    private boolean authorize(UserLevel level, HttpServletRequest request) {
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            return false;
        }

        String username = request.getUserPrincipal().getName();
        UserLevel userLevel = userManager.getUserLevel(username);
        if (userLevel.toNumber() <= level.toNumber()) {
            return true;
        }
        return false;
    }

    private StatusJsonBean unauthorizedMessage() {
        StatusJsonBean message = new StatusJsonBean();
        message.setStatus(false);
        message.setMessage("Operation not authorized");
        return message;
    }

    @RequestMapping(value = "login")
    public String login(final HttpServletRequest request,
            final HttpServletResponse response) {
        // response.setHeader("X-Page-Location", "/login");
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
