/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.statistics.northbound;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.containermanager.IContainerManager;

import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.opendaylight.controller.switchmanager.ISwitchManager;

/**
 * Northbound APIs that returns various Statistics exposed by the Southbound plugins such as Openflow.
 * 
 * <br><br>
 * Authentication scheme : <b>HTTP Basic</b><br>
 * Authentication realm : <b>opendaylight</b><br>
 * Transport : <b>HTTP and HTTPS</b><br>
 * <br>
 * HTTPS Authentication is disabled by default. Administrator can enable it in tomcat-server.xml after adding 
 * a proper keystore / SSL certificate from a trusted authority.<br>
 * More info : http://tomcat.apache.org/tomcat-7.0-doc/ssl-howto.html#Configuration
 *
 */
@Path("/")
public class StatisticsNorthbound {

    private IStatisticsManager getStatisticsService(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames) {
            if (cName.trim().equalsIgnoreCase(containerName.trim())) {
                found = true;
            }
        }

        if (found == false) {
            throw new ResourceNotFoundException(containerName + " "
                    + RestMessages.NOCONTAINER.toString());
        }

        IStatisticsManager statsManager = (IStatisticsManager) ServiceHelper
                .getInstance(IStatisticsManager.class, containerName, this);

        if (statsManager == null) {
            throw new ServiceUnavailableException("Statistics "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return statsManager;
    }

    /**
     * Returns a list of all Flow Statistics from all the Nodes.
     *
     * @param containerName Name of the Container. The Container name for the base controller is "default". 
     * @return List of FlowStatistics from all the Nodes
     */

    @Path("/{containerName}/flowstats")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(AllFlowStatistics.class)
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public AllFlowStatistics getFlowStatistics(
            @PathParam("containerName") String containerName) {
        IStatisticsManager statisticsManager = getStatisticsService(containerName);
        if (statisticsManager == null) {
            throw new ServiceUnavailableException("Statistics "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        List<FlowStatistics> statistics = new ArrayList<FlowStatistics>();
        for (Node node : switchManager.getNodes()) {
            List<FlowOnNode> flowStats = new ArrayList<FlowOnNode>();

            List<FlowOnNode> flows = statisticsManager.getFlows(node);
            for (FlowOnNode flowOnSwitch : flows) {
                flowStats.add(flowOnSwitch);
            }
            FlowStatistics stat = new FlowStatistics(node,
                                                     flowStats);
            statistics.add(stat);
        }
        return new AllFlowStatistics(statistics);
    }

    /**
     * Returns a list of Flow Statistics for a given Node.
     *
     * @param containerName Name of the Container. The Container name
     * for the base controller is "default". 
     * @param nodeType Node Type as specifid by Node class
     * @param nodeId Node Identifier
     * @return List of Flow Statistics for a given Node.
     */
    @Path("/{containerName}/flowstats/{nodeType}/{nodeId}")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(FlowStatistics.class)
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public FlowStatistics getFlowStatistics(
            @PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType,
            @PathParam("nodeId") String nodeId) {

        handleDefaultDisabled(containerName);

        IStatisticsManager statisticsManager = getStatisticsService(containerName);
        if (statisticsManager == null) {
            throw new ServiceUnavailableException("Statistics "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = handleNodeAvailability(containerName, nodeType, nodeId);
        return new FlowStatistics(node, statisticsManager.getFlows(node));
    }

    /**
     * Returns a list of all the Port Statistics across all the NodeConnectors on all the Nodes.
     *
     * @param containerName Name of the Container. The Container name for the base controller is "default". 
     * @return List of all the Port Statistics across all the NodeConnectors on all the Nodes.
     */

    @Path("/{containerName}/portstats")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(AllPortStatistics.class)
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public AllPortStatistics getPortStatistics(
            @PathParam("containerName") String containerName) {

        IStatisticsManager statisticsManager = getStatisticsService(containerName);
        if (statisticsManager == null) {
            throw new ServiceUnavailableException("Statistics "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        List<PortStatistics> statistics = new ArrayList<PortStatistics>();
        for (Node node : switchManager.getNodes()) {
            List<NodeConnectorStatistics> stat = statisticsManager
                    .getNodeConnectorStatistics(node);
            PortStatistics portStat = new PortStatistics(node, stat);
            statistics.add(portStat);
        }
        return new AllPortStatistics(statistics);
    }

    /**
     * Returns a list of all the Port Statistics across all the NodeConnectors in a given Node.
     *
     * @param containerName Name of the Container. The Container name
     * for the base controller is "default". 
     * @param nodeType Node Type as specifid by Node class
     * @param Node Identifier
     * @return Returns a list of all the Port Statistics across all the NodeConnectors in a given Node.
     */
    @Path("/{containerName}/portstats/{nodeType}/{nodeId}")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(PortStatistics.class)
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public PortStatistics getPortStatistics(
            @PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType,
            @PathParam("nodeId") String nodeId) {

        handleDefaultDisabled(containerName);

        IStatisticsManager statisticsManager = getStatisticsService(containerName);
        if (statisticsManager == null) {
            throw new ServiceUnavailableException("Statistics "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = handleNodeAvailability(containerName,
                                           nodeType, nodeId);
        return new PortStatistics(node, statisticsManager
                .getNodeConnectorStatistics(node));
    }

    private void handleDefaultDisabled(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            throw new InternalServerErrorException(RestMessages.INTERNALERROR
                    .toString());
        }
        if (containerName.equals(GlobalConstants.DEFAULT.toString())
                && containerManager.hasNonDefaultContainer()) {
            throw new ResourceConflictException(RestMessages.DEFAULTDISABLED
                    .toString());
        }
    }

    private Node handleNodeAvailability(String containerName, String nodeType,
                                        String nodeId) {

        Node node = Node.fromString(nodeType, nodeId);
        if (node == null) {
            throw new ResourceNotFoundException(nodeId + " : "
                    + RestMessages.NONODE.toString());
        }

        ISwitchManager sm = (ISwitchManager) ServiceHelper.getInstance(
                ISwitchManager.class, containerName, this);

        if (sm == null) {
            throw new ServiceUnavailableException("Switch Manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        if (!sm.getNodes().contains(node)) {
            throw new ResourceNotFoundException(node.toString() + " : "
                    + RestMessages.NONODE.toString());
        }
        return node;
    }

}
