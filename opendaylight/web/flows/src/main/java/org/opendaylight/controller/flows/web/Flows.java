/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.flows.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.Name;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.ISwitchManager;
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

import com.google.gson.Gson;

@Controller
@RequestMapping("/")
public class Flows implements IDaylightWeb {
    private static final UserLevel AUTH_LEVEL = UserLevel.CONTAINERUSER;
    private static final String WEB_NAME = "Flows";

    private static final String WEB_ID = "flows";
    private static final short WEB_ORDER = 2;

    public Flows() {
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

    @RequestMapping(value = "/main")
    @ResponseBody
    public Map<String, Object> getFlows(HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        Privilege privilege = DaylightWebUtil.getContainerPrivilege(userName,
                containerName, this);
        if (privilege == Privilege.NONE) {
            return null;
        }

        // fetch frm
        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper
                .getInstance(IForwardingRulesManager.class, containerName, this);
        if (frm == null) {
            return null;
        }

        // fetch sm
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            return null;
        }

        // get static flow list
        List<FlowConfig> staticFlowList = frm.getStaticFlows();
        Set<Map<String, Object>> flowSet = new HashSet<Map<String, Object>>();
        for (FlowConfig flowConfig : staticFlowList) {
            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put("flow", flowConfig);
            entry.put("name", flowConfig.getName());
            Node node = flowConfig.getNode();
            entry.put("node", getNodeDesc(node, switchManager));
            entry.put("nodeId", node.toString());
            flowSet.add(entry);
        }

        Map<String, Object> output = new HashMap<String, Object>(2);
        output.put("flows", flowSet);
        output.put("privilege", privilege);
        return output;
    }

    @RequestMapping(value = "/node-ports")
    @ResponseBody
    public Map<String, Object> getNodePorts(HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil
                .getContainerPrivilege(userName, containerName, this) == Privilege.NONE) {
            return null;
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            return null;
        }

        Map<String, Object> nodes = new HashMap<String, Object>();
        Map<Short, String> port;

        for (Switch node : switchManager.getNetworkDevices()) {
            port = new HashMap<Short, String>(); // new port
            Set<NodeConnector> nodeConnectorSet = node.getNodeConnectors();

            if (nodeConnectorSet != null) {
                for (NodeConnector nodeConnector : nodeConnectorSet) {
                    String nodeConnectorName = ((Name) switchManager
                            .getNodeConnectorProp(nodeConnector,
                                    Name.NamePropName)).getValue();
                    port.put((Short) nodeConnector.getID(), nodeConnectorName
                            + "(" + nodeConnector.getNodeConnectorIDString()
                            + ")");
                }
            }

            // add ports
            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put("ports", port);

            // add name
            entry.put("name", getNodeDesc(node.getNode(), switchManager));

            // add to the node
            nodes.put(node.getNode().toString(), entry);
        }

        return nodes;
    }

    @RequestMapping(value = "/node-flows")
    @ResponseBody
    public Map<String, Object> getNodeFlows(HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil
                .getContainerPrivilege(userName, containerName, this) == Privilege.NONE) {
            return null;
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            return null;
        }
        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper
                .getInstance(IForwardingRulesManager.class, containerName, this);
        if (frm == null) {
            return null;
        }

        Map<String, Object> nodes = new HashMap<String, Object>();

        for (Switch sw : switchManager.getNetworkDevices()) {
            Node node = sw.getNode();

            List<FlowConfig> flows = frm.getStaticFlows(node);

            String nodeDesc = node.toString();
            SwitchConfig config = switchManager
                    .getSwitchConfig(node.toString());
            if ((config != null)
                    && (config.getProperty(Description.propertyName) != null)) {
                nodeDesc = ((Description) config
                        .getProperty(Description.propertyName)).getValue();
            }

            nodes.put(nodeDesc, flows.size());
        }

        return nodes;
    }

    @RequestMapping(value = "/flow", method = RequestMethod.POST)
    @ResponseBody
    public String actionFlow(@RequestParam(required = true) String action,
            @RequestParam(required = false) String body,
            @RequestParam(required = true) String nodeId,
            HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil
                .getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return "Operation not authorized";
        }

        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper
                .getInstance(IForwardingRulesManager.class, containerName, this);
        if (frm == null) {
            return null;
        }

        Gson gson = new Gson();
        FlowConfig flow = gson.fromJson(body, FlowConfig.class);
        Node node = Node.fromString(nodeId);
        flow.setNode(node);
        Status result = new Status(StatusCode.BADREQUEST, "Invalid request");
        if (action.equals("add")) {
            result = frm.addStaticFlow(flow);
            DaylightWebUtil.auditlog("Flow", userName, "added", flow.getName(),
                    containerName);
        }

        return (result.isSuccess()) ? StatusCode.SUCCESS.toString() : result
                .getDescription();
    }

    @RequestMapping(value = "/flow/{nodeId}/{name:.*}", method = RequestMethod.POST)
    @ResponseBody
    public String removeFlow(@PathVariable("nodeId") String nodeId,
            @PathVariable("name") String name,
            @RequestParam(required = true) String action,
            HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil
                .getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return "Operation not authorized";
        }

        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper
                .getInstance(IForwardingRulesManager.class, containerName, this);
        if (frm == null) {
            return null;
        }

        Status result = null;
        Node node = Node.fromString(nodeId);
        if (node == null) {
            return null;
        }
        if (action.equals("remove")) {
            result = frm.removeStaticFlow(name, node);
            if (result.isSuccess()) {
                DaylightWebUtil.auditlog("Flow", userName, "removed", name,
                        containerName);
            }
        } else if (action.equals("toggle")) {
            result = frm.toggleStaticFlowStatus(name, node);
            if (result.isSuccess()) {
                DaylightWebUtil.auditlog("Flow", userName, "toggled", name,
                        containerName);
            }
        } else {
            result = new Status(StatusCode.BADREQUEST, "Unknown action");
        }

        return (result.isSuccess()) ? StatusCode.SUCCESS.toString() : result
                .getDescription();
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/flow/deleteFlows", method = RequestMethod.POST)
    @ResponseBody
    public String removeSelectedFlows(
            @RequestParam(required = false) String body,
            HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil
                .getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return "Operation not authorized";
        }

        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper
                .getInstance(IForwardingRulesManager.class, containerName, this);
        if (frm == null) {
            return "Forwarding Rules Manager is not available";
        }

        Gson gson = new Gson();
        List<Map<String, String>> flowList = new ArrayList<Map<String, String>>();
        flowList = gson.fromJson(body, flowList.getClass());
        Status result = new Status(StatusCode.BADREQUEST, "Invalid request");
        String status = "";
        for (Map<String, String> flowEntry : flowList) {
            Node node = Node.fromString(flowEntry.get("node"));
            result = frm.removeStaticFlow(flowEntry.get("name"), node);
            if (result.isSuccess()) {
                DaylightWebUtil.auditlog("Flow", userName, "removed",
                        flowEntry.get("name"), containerName);
            } else {
                status = flowEntry.get("name") + ", " + status;
            }
        }
        if (!status.equals("")) {
            return "Could not remove "
                    + status.substring(0, status.length() - 2) + " Flow(s)";
        } else {
            return "Success";
        }
    }

    private String getNodeDesc(Node node, ISwitchManager switchManager) {
        Description desc = (Description) switchManager.getNodeProp(node,
                Description.propertyName);
        String description = (desc == null) ? "" : desc.getValue();
        return (description.isEmpty() || description.equalsIgnoreCase("none")) ? node
                .toString() : description;
    }

}
