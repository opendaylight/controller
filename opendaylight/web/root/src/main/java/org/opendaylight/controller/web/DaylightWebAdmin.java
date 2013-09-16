/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.web;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.connectionmanager.IConnectionManager;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.UserConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;

@Controller
@RequestMapping("/admin")
public class DaylightWebAdmin {
    Gson gson = new Gson();

    /**
     * Returns list of clustered controllers. Highlights "this" controller and
     * if controller is coordinator
     * @return List<ClusterBean>
     */
    @RequestMapping("/cluster")
    @ResponseBody
    public String getClusteredControllers() {
        IClusterGlobalServices clusterServices = (IClusterGlobalServices) ServiceHelper.getGlobalInstance(
                IClusterGlobalServices.class, this);
        if (clusterServices == null) {
            return null;
        }
        IConnectionManager connectionManager = (IConnectionManager) ServiceHelper.getGlobalInstance(
                IConnectionManager.class, this);
        if (connectionManager == null) {
            return null;
        }

        List<ClusterNodeBean> clusterNodes = new ArrayList<ClusterNodeBean>();

        List<InetAddress> controllers = clusterServices.getClusteredControllers();
        for (InetAddress controller : controllers) {
            ClusterNodeBean.Builder clusterBeanBuilder = new ClusterNodeBean.Builder(controller);

            //get number of connected nodes
            Set<Node> connectedNodes = connectionManager.getNodes(controller);
            int numNodes = connectedNodes == null ? 0 : connectedNodes.size();
            clusterBeanBuilder.nodesConnected(numNodes);

            //determine if this is the executing controller
            if (controller.equals(clusterServices.getMyAddress())) {
                clusterBeanBuilder.highlightMe();
            }

            //determine whether this is coordinator
            if (clusterServices.getCoordinatorAddress().equals(controller)) {
                clusterBeanBuilder.iAmCoordinator();
            }
            clusterNodes.add(clusterBeanBuilder.build());
        }

        return gson.toJson(clusterNodes);
    }

    /**
     * Return nodes connected to controller {controller}
     * @param controller
     *            - byte[] of the address of the controller
     * @return List<NodeBean>
     */
    @RequestMapping("/cluster/controller/{controller}")
    @ResponseBody
    public String getNodesConnectedToController(@PathVariable("controller") String controller) {
        IClusterGlobalServices clusterServices = (IClusterGlobalServices) ServiceHelper.getGlobalInstance(
                IClusterGlobalServices.class, this);
        if (clusterServices == null) {
            return null;
        }
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

        byte[] address = gson.fromJson(controller, byte[].class);
        InetAddress controllerAddress = null;
        try {
            controllerAddress = InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            return null;
        }

        List<NodeBean> result = new ArrayList<NodeBean>();

        Set<Node> nodes = connectionManager.getNodes(controllerAddress);
        if (nodes == null) {
            return gson.toJson(result);
        }
        for (Node node : nodes) {
            Description description = (Description) switchManager.getNodeProp(node, Description.propertyName);
            NodeBean nodeBean;
            if (description == null || description.getValue().equals("None")) {
                nodeBean = new NodeBean(node);
            } else {
                nodeBean = new NodeBean(node, description.getValue());
            }
            result.add(nodeBean);
        }

        return gson.toJson(result);
    }

    @RequestMapping("/users")
    @ResponseBody
    public List<UserConfig> getUsers() {
        IUserManager userManager = (IUserManager) ServiceHelper.getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            return null;
        }

        List<UserConfig> userConfList = userManager.getLocalUserList();

        return userConfList;
    }

    /*
     * Password in clear text, moving to HTTP/SSL soon
     */
    @RequestMapping(value = "/users", method = RequestMethod.POST)
    @ResponseBody
    public String saveLocalUserConfig(@RequestParam(required = true) String json,
            @RequestParam(required = true) String action, HttpServletRequest request) {

        IUserManager userManager = (IUserManager) ServiceHelper.getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            return "Internal Error";
        }

        if (!authorize(userManager, UserLevel.NETWORKADMIN, request)) {
            return "Operation not permitted";
        }

        Gson gson = new Gson();
        UserConfig plainConfig = gson.fromJson(json, UserConfig.class);
        // Recreate using the proper constructor which will hash the password
        UserConfig config = new UserConfig(plainConfig.getUser(), plainConfig.getPassword(), plainConfig.getRoles());

        Status result = (action.equals("add")) ? userManager.addLocalUser(config) : userManager.removeLocalUser(config);
        if (result.isSuccess()) {
            String userAction = (action.equals("add")) ? "added" : "removed";
            DaylightWebUtil.auditlog("User", request.getUserPrincipal().getName(), userAction, config.getUser());
            return "Success";
        }
        return result.getDescription();
    }

    @RequestMapping(value = "/users/{username}", method = RequestMethod.POST)
    @ResponseBody
    public String removeLocalUser(@PathVariable("username") String userName, HttpServletRequest request) {

        String username = request.getUserPrincipal().getName();
        if (username.equals(userName)) {
            return "Invalid Request: User cannot delete itself";
        }

        IUserManager userManager = (IUserManager) ServiceHelper.getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            return "Internal Error";
        }

        if (!authorize(userManager, UserLevel.NETWORKADMIN, request)) {
            return "Operation not permitted";
        }

        Status result = userManager.removeLocalUser(userName);
        if (result.isSuccess()) {
            DaylightWebUtil.auditlog("User", request.getUserPrincipal().getName(), "removed", userName);
            return "Success";
        }
        return result.getDescription();
    }

    @RequestMapping(value = "/users/password/{username}", method = RequestMethod.POST)
    @ResponseBody
    public Status changePassword(@PathVariable("username") String username, HttpServletRequest request,
            @RequestParam("currentPassword") String currentPassword, @RequestParam("newPassword") String newPassword) {
        IUserManager userManager = (IUserManager) ServiceHelper.getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            return new Status(StatusCode.GONE, "User Manager not found");
        }

        if (!authorize(userManager, UserLevel.NETWORKADMIN, request)) {
            return new Status(StatusCode.FORBIDDEN, "Operation not permitted");
        }

        if (newPassword.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Empty passwords not allowed");
        }

        Status status = userManager.changeLocalUserPassword(username, currentPassword, newPassword);
        if (status.isSuccess()) {
            DaylightWebUtil.auditlog("User", request.getUserPrincipal().getName(), "changed password for", username);
        }
        return status;
    }

    /**
     * Is the operation permitted for the given level
     * @param level
     */
    private boolean authorize(IUserManager userManager, UserLevel level, HttpServletRequest request) {
        String username = request.getUserPrincipal().getName();
        UserLevel userLevel = userManager.getUserLevel(username);
        return userLevel.toNumber() <= level.toNumber();
    }
}
