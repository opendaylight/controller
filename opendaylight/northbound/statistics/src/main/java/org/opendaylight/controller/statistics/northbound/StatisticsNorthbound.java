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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.opendaylight.controller.switchmanager.ISwitchManager;

/**
 * Northbound APIs that returns various Statistics exposed by the Southbound
 * protocol plugins such as Openflow.
 *
 * <br>
 * <br>
 * Authentication scheme : <b>HTTP Basic</b><br>
 * Authentication realm : <b>opendaylight</b><br>
 * Transport : <b>HTTP and HTTPS</b><br>
 * <br>
 * HTTPS Authentication is disabled by default. Administrator can enable it in
 * tomcat-server.xml after adding a proper keystore / SSL certificate from a
 * trusted authority.<br>
 * More info :
 * http://tomcat.apache.org/tomcat-7.0-doc/ssl-howto.html#Configuration
 *
 */
@Path("/")
public class StatisticsNorthbound {

    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        username = context.getUserPrincipal().getName();
    }

    protected String getUserName() {
        return username;
    }

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
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @return List of FlowStatistics from all the Nodes
     *
     *         <pre>
     * Example:
     * Request URL: localhost:8080/controller/nb/v2/statistics/default/flow
     *
     * Response in JSON:
     * {
     *     "flowStatistics": [
     *         {
     *             "node": {
     *                 "@type": "OF",
     *                 "@id": "00:00:00:00:00:00:00:02"
     *             },
     *             "flowStatistic": [
     *                 {
     *                     "flow": {
     *                         "match": {
     *                             "matchField": [
     *                                 {
     *                                     "type": "DL_TYPE",
     *                                     "value": "2048"
     *                                 },
     *                                 {
     *                                     "mask": "255.255.255.255",
     *                                     "type": "NW_DST",
     *                                     "value": "1.1.1.1"
     *                                 }
     *                             ]
     *                         },
     *                         "actions": {
     *                             "@type": "output",
     *                             "port": {
     *                                 "@type": "OF",
     *                                 "@id": "3",
     *                                 "node": {
     *                                     "@type": "OF",
     *                                     "@id": "00:00:00:00:00:00:00:02"
     *                                 }
     *                             }
     *                         },
     *                         "priority": "1",
     *                         "idleTimeout": "0",
     *                         "hardTimeout": "0",
     *                         "id": "0"
     *                     },
     *                     "tableId": "0",
     *                     "durationSeconds": "1828",
     *                     "durationNanoseconds": "397000000",
     *                     "packetCount": "0",
     *                     "byteCount": "0"
     *                 }
     *             ]
     *         },
     *         {   flow statistics of another node
     *             ............
     *             ................
     *             ......................
     *         }
     *
     *     ]
     * }
     * Response in XML:
     * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot; standalone=&quot;yes&quot;?&gt;
     * &lt;list&gt;
     *     &lt;flowStatistics&gt;
     *         &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:02&quot;/&gt;
     *         &lt;flowStatistic&gt;
     *             &lt;flow&gt;
     *                 &lt;match&gt;
     *                     &lt;matchField&gt;
     *                         &lt;type&gt;DL_TYPE&lt;/type&gt;
     *                         &lt;value&gt;2048&lt;/value&gt;
     *                     &lt;/matchField&gt;
     *                     &lt;matchField&gt;
     *                         &lt;mask&gt;255.255.255.255&lt;/mask&gt;
     *                         &lt;type&gt;NW_DST&lt;/type&gt;
     *                         &lt;value&gt;1.1.1.2&lt;/value&gt;
     *                     &lt;/matchField&gt;
     *                 &lt;/match&gt;
     *                 &lt;actions
     *                     xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot; xsi:type=&quot;output&quot;&gt;
     *                     &lt;port type=&quot;OF&quot; id=&quot;3&quot;&gt;
     *                         &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:02&quot;/&gt;
     *                     &lt;/port&gt;
     *                 &lt;/actions&gt;
     *                 &lt;priority&gt;1&lt;/priority&gt;
     *                 &lt;idleTimeout&gt;0&lt;/idleTimeout&gt;
     *                 &lt;hardTimeout&gt;0&lt;/hardTimeout&gt;
     *                 &lt;id&gt;0&lt;/id&gt;
     *             &lt;/flow&gt;
     *             &lt;tableId&gt;0&lt;/tableId&gt;
     *             &lt;durationSeconds&gt;337&lt;/durationSeconds&gt;
     *             &lt;durationNanoseconds&gt;149000000&lt;/durationNanoseconds&gt;
     *             &lt;packetCount&gt;0&lt;/packetCount&gt;
     *             &lt;byteCount&gt;0&lt;/byteCount&gt;
     *         &lt;/flowStatistic&gt;
     *     &lt;/flowStatistics&gt;
     *     &lt;flowStatistics&gt;
     *          flow statistics for another node
     *          ..........
     *          ................
     *          .....................
     *     &lt;/flowStatistics&gt;
     * &lt;/list&gt;
     * </pre>
     */

    @Path("/{containerName}/flow")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(AllFlowStatistics.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public AllFlowStatistics getFlowStatistics(
            @PathParam("containerName") String containerName) {
        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
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
            FlowStatistics stat = new FlowStatistics(node, flowStats);
            statistics.add(stat);
        }
        return new AllFlowStatistics(statistics);
    }

    /**
     * Returns a list of Flow Statistics for a given Node.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @param nodeType
     *            Node Type as specifid in {@link org.opendaylight.controller.sal.core.Node} class
     * @param nodeId
     *            Node Identifier
     * @return List of Flow Statistics for a given Node. *
     *
     *         <pre>
     * Example:
     * Request URL:
     * http://host:8080/controller/nb/v2/statistics/default/flow/node/OF/00:00:00:00:00:00:00:01
     *
     * Response in JSON:
     * {
     *     "node": {
     *         "@type": "OF",
     *         "@id": "00:00:00:00:00:00:00:01"
     *     },
     *     "flowStatistic": [
     *         {
     *             "flow": {
     *                 "match": {
     *                     "matchField": [
     *                         {
     *                             "type": "DL_TYPE",
     *                             "value": "2048"
     *                         },
     *                         {
     *                             "mask": "255.255.255.255",
     *                             "type": "NW_DST",
     *                             "value": "1.1.1.2"
     *                         }
     *                     ]
     *                 },
     *                 "actions": [
     *                     {
     *                         "@type": "setDlDst",
     *                         "address": "52d28b0f76ec"
     *                     },
     *                     {
     *                         "@type": "output",
     *                         "port": {
     *                             "@type": "OF",
     *                             "@id": "5",
     *                             "node": {
     *                                 "@type": "OF",
     *                                 "@id": "00:00:00:00:00:00:00:01"
     *                             }
     *                         }
     *                     }
     *                 ],
     *                 "priority": "1",
     *                 "idleTimeout": "0",
     *                 "hardTimeout": "0",
     *                 "id": "0"
     *             },
     *             "tableId": "0",
     *             "durationSeconds": "2089",
     *             "durationNanoseconds": "538000000",
     *             "packetCount": "0",
     *             "byteCount": "0"
     *         }
     *     ]
     * }
     *
     *
     * Response in XML:
     * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot; standalone=&quot;yes&quot;?&gt;
     *     &lt;nodeFlowStatistics&gt;
     *         &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:02&quot;/&gt;
     *         &lt;flowStatistic&gt;
     *             &lt;flow&gt;
     *                 &lt;match&gt;
     *                     &lt;matchField&gt;
     *                         &lt;type&gt;DL_TYPE&lt;/type&gt;
     *                         &lt;value&gt;2048&lt;/value&gt;
     *                     &lt;/matchField&gt;
     *                     &lt;matchField&gt;
     *                         &lt;mask&gt;255.255.255.255&lt;/mask&gt;
     *                         &lt;type&gt;NW_DST&lt;/type&gt;
     *                         &lt;value&gt;1.1.1.2&lt;/value&gt;
     *                     &lt;/matchField&gt;
     *                 &lt;/match&gt;
     *                 &lt;actions
     *                     xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot; xsi:type=&quot;output&quot;&gt;
     *                     &lt;port type=&quot;OF&quot; id=&quot;3&quot;&gt;
     *                         &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:02&quot;/&gt;
     *                     &lt;/port&gt;
     *                 &lt;/actions&gt;
     *                 &lt;priority&gt;1&lt;/priority&gt;
     *                 &lt;idleTimeout&gt;0&lt;/idleTimeout&gt;
     *                 &lt;hardTimeout&gt;0&lt;/hardTimeout&gt;
     *                 &lt;id&gt;0&lt;/id&gt;
     *             &lt;/flow&gt;
     *             &lt;tableId&gt;0&lt;/tableId&gt;
     *             &lt;durationSeconds&gt;337&lt;/durationSeconds&gt;
     *             &lt;durationNanoseconds&gt;149000000&lt;/durationNanoseconds&gt;
     *             &lt;packetCount&gt;0&lt;/packetCount&gt;
     *             &lt;byteCount&gt;0&lt;/byteCount&gt;
     *         &lt;/flowStatistic&gt;
     *         &lt;flowStatistic&gt;
     *             &lt;flow&gt;
     *                 &lt;match&gt;
     *                     &lt;matchField&gt;
     *                         &lt;type&gt;DL_TYPE&lt;/type&gt;
     *                         &lt;value&gt;2048&lt;/value&gt;
     *                     &lt;/matchField&gt;
     *                     &lt;matchField&gt;
     *                         &lt;mask&gt;255.255.255.255&lt;/mask&gt;
     *                         &lt;type&gt;NW_DST&lt;/type&gt;
     *                         &lt;value&gt;1.1.1.1&lt;/value&gt;
     *                     &lt;/matchField&gt;
     *                 &lt;/match&gt;
     *                 &lt;actions
     *                     xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot; xsi:type=&quot;output&quot;&gt;
     *                     &lt;port type=&quot;OF&quot; id=&quot;3&quot;&gt;
     *                         &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:02&quot;/&gt;
     *                     &lt;/port&gt;
     *                 &lt;/actions&gt;
     *                 &lt;priority&gt;1&lt;/priority&gt;
     *                 &lt;idleTimeout&gt;0&lt;/idleTimeout&gt;
     *                 &lt;hardTimeout&gt;0&lt;/hardTimeout&gt;
     *                 &lt;id&gt;0&lt;/id&gt;
     *             &lt;/flow&gt;
     *             &lt;tableId&gt;0&lt;/tableId&gt;
     *             &lt;durationSeconds&gt;337&lt;/durationSeconds&gt;
     *             &lt;durationNanoseconds&gt;208000000&lt;/durationNanoseconds&gt;
     *             &lt;packetCount&gt;0&lt;/packetCount&gt;
     *             &lt;byteCount&gt;0&lt;/byteCount&gt;
     *         &lt;/flowStatistic&gt;
     *     &lt;/nodeFlowStatistics&gt;
     * </pre>
     */
    @Path("/{containerName}/flow/node/{nodeType}/{nodeId}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(FlowStatistics.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public FlowStatistics getFlowStatistics(
            @PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType,
            @PathParam("nodeId") String nodeId) {
        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
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
     * Returns a list of all the Port Statistics across all the NodeConnectors
     * on all the Nodes.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @return List of all the Port Statistics across all the NodeConnectors on
     *         all the Nodes.
     *
     *         <pre>
     * Example:
     *
     * Requset URL: http://host:8080/controller/nb/v2/statistics/default/port
     *
     * Response in JSON:
     * {
     *     "portStatistics": [
     *         {
     *             "node": {
     *                 "@type": "OF",
     *                 "@id": "00:00:00:00:00:00:00:02"
     *             },
     *             "portStatistic": [
     *                 {
     *                     "nodeConnector": {
     *                         "@type": "OF",
     *                         "@id": "3",
     *                         "node": {
     *                             "@type": "OF",
     *                             "@id": "00:00:00:00:00:00:00:02"
     *                         }
     *                     },
     *                     "receivePackets": "182",
     *                     "transmitPackets": "173",
     *                     "receiveBytes": "12740",
     *                     "transmitBytes": "12110",
     *                     "receiveDrops": "0",
     *                     "transmitDrops": "0",
     *                     "receiveErrors": "0",
     *                     "transmitErrors": "0",
     *                     "receiveFrameError": "0",
     *                     "receiveOverRunError": "0",
     *                     "receiveCrcError": "0",
     *                     "collisionCount": "0"
     *                 },
     *                 {
     *                     "nodeConnector": {
     *                         "@type": "OF",
     *                         "@id": "2",
     *                         "node": {
     *                             "@type": "OF",
     *                             "@id": "00:00:00:00:00:00:00:02"
     *                         }
     *                     },
     *                     "receivePackets": "174",
     *                     "transmitPackets": "181",
     *                     "receiveBytes": "12180",
     *                     "transmitBytes": "12670",
     *                     "receiveDrops": "0",
     *                     "transmitDrops": "0",
     *                     "receiveErrors": "0",
     *                     "transmitErrors": "0",
     *                     "receiveFrameError": "0",
     *                     "receiveOverRunError": "0",
     *                     "receiveCrcError": "0",
     *                     "collisionCount": "0"
     *                 },
     *
     *             ]
     *         },
     *         {
     *             "node": {
     *                 "@type": "OF",
     *                 "@id": "00:00:00:00:00:00:00:03"
     *             },
     *             "portStatistic": [
     *                  ..................
     *                  .......................
     *                  ..........................
     *             ]
     *         }
     *     ]
     * }
     *
     * Response in XML:
     * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot; standalone=&quot;yes&quot;?&gt;
     * &lt;list&gt;
     *     &lt;portStatistics&gt;
     *         &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:02&quot;/&gt;
     *         &lt;portStatistic&gt;
     *             &lt;nodeConnector type=&quot;OF&quot; id=&quot;3&quot;&gt;
     *                 &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:02&quot;/&gt;
     *             &lt;/nodeConnector&gt;
     *             &lt;receivePackets&gt;181&lt;/receivePackets&gt;
     *             &lt;transmitPackets&gt;172&lt;/transmitPackets&gt;
     *             &lt;receiveBytes&gt;12670&lt;/receiveBytes&gt;
     *             &lt;transmitBytes&gt;12040&lt;/transmitBytes&gt;
     *             &lt;receiveDrops&gt;0&lt;/receiveDrops&gt;
     *             &lt;transmitDrops&gt;0&lt;/transmitDrops&gt;
     *             &lt;receiveErrors&gt;0&lt;/receiveErrors&gt;
     *             &lt;transmitErrors&gt;0&lt;/transmitErrors&gt;
     *             &lt;receiveFrameError&gt;0&lt;/receiveFrameError&gt;
     *             &lt;receiveOverRunError&gt;0&lt;/receiveOverRunError&gt;
     *             &lt;receiveCrcError&gt;0&lt;/receiveCrcError&gt;
     *             &lt;collisionCount&gt;0&lt;/collisionCount&gt;
     *         &lt;/portStatistic&gt;
     *         &lt;portStatistic&gt;
     *             &lt;nodeConnector type=&quot;OF&quot; id=&quot;2&quot;&gt;
     *                 &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:02&quot;/&gt;
     *             &lt;/nodeConnector&gt;
     *             &lt;receivePackets&gt;173&lt;/receivePackets&gt;
     *             &lt;transmitPackets&gt;180&lt;/transmitPackets&gt;
     *             &lt;receiveBytes&gt;12110&lt;/receiveBytes&gt;
     *             &lt;transmitBytes&gt;12600&lt;/transmitBytes&gt;
     *             &lt;receiveDrops&gt;0&lt;/receiveDrops&gt;
     *             &lt;transmitDrops&gt;0&lt;/transmitDrops&gt;
     *             &lt;receiveErrors&gt;0&lt;/receiveErrors&gt;
     *             &lt;transmitErrors&gt;0&lt;/transmitErrors&gt;
     *             &lt;receiveFrameError&gt;0&lt;/receiveFrameError&gt;
     *             &lt;receiveOverRunError&gt;0&lt;/receiveOverRunError&gt;
     *             &lt;receiveCrcError&gt;0&lt;/receiveCrcError&gt;
     *             &lt;collisionCount&gt;0&lt;/collisionCount&gt;
     *         &lt;/portStatistic&gt;
     *
     *     &lt;/portStatistics&gt;
     * &lt;/list&gt;
     * </pre>
     */

    @Path("/{containerName}/port")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(AllPortStatistics.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public AllPortStatistics getPortStatistics(
            @PathParam("containerName") String containerName) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
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
     * Returns a list of all the Port Statistics across all the NodeConnectors
     * in a given Node.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @param nodeType
     *            Node Type as specifid in {@link org.opendaylight.controller.sal.core.Node} class
     * @param Node
     *            Identifier (e.g. MAC address)
     * @return Returns a list of all the Port Statistics across all the
     *         NodeConnectors in a given Node.
     *
     *         <pre>
     * Example:
     *
     * Request URL:
     * http://host:8080/controller/nb/v2/statistics/default/port/node/OF/00:00:00:00:00:00:00:01
     *
     * Response in JSON:
     * {
     *     "node": {
     *         "@type": "OF",
     *         "@id": "00:00:00:00:00:00:00:01"
     *     },
     *     "portStatistic": [
     *         {
     *             "nodeConnector": {
     *                 "@type": "OF",
     *                 "@id": "3",
     *                 "node": {
     *                     "@type": "OF",
     *                     "@id": "00:00:00:00:00:00:00:01"
     *                 }
     *             },
     *             "receivePackets": "171",
     *             "transmitPackets": "2451",
     *             "receiveBytes": "11970",
     *             "transmitBytes": "235186",
     *             "receiveDrops": "0",
     *             "transmitDrops": "0",
     *             "receiveErrors": "0",
     *             "transmitErrors": "0",
     *             "receiveFrameError": "0",
     *             "receiveOverRunError": "0",
     *             "receiveCrcError": "0",
     *             "collisionCount": "0"
     *         },
     *         {
     *             "nodeConnector": {
     *                 "@type": "OF",
     *                 "@id": "2",
     *                 "node": {
     *                     "@type": "OF",
     *                     "@id": "00:00:00:00:00:00:00:01"
     *                 }
     *             },
     *             "receivePackets": "179",
     *             "transmitPackets": "2443",
     *             "receiveBytes": "12530",
     *             "transmitBytes": "234626",
     *             "receiveDrops": "0",
     *             "transmitDrops": "0",
     *             "receiveErrors": "0",
     *             "transmitErrors": "0",
     *             "receiveFrameError": "0",
     *             "receiveOverRunError": "0",
     *             "receiveCrcError": "0",
     *             "collisionCount": "0"
     *         }
     *     ]
     * }
     *
     * Response in XML:
     * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot; standalone=&quot;yes&quot;?&gt;
     * &lt;nodePortStatistics&gt;
     *     &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:01&quot;/&gt;
     *     &lt;portStatistic&gt;
     *         &lt;nodeConnector type=&quot;OF&quot; id=&quot;2&quot;&gt;
     *             &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:01&quot;/&gt;
     *         &lt;/nodeConnector&gt;
     *         &lt;receivePackets&gt;180&lt;/receivePackets&gt;
     *         &lt;transmitPackets&gt;2594&lt;/transmitPackets&gt;
     *         &lt;receiveBytes&gt;12600&lt;/receiveBytes&gt;
     *         &lt;transmitBytes&gt;249396&lt;/transmitBytes&gt;
     *         &lt;receiveDrops&gt;0&lt;/receiveDrops&gt;
     *         &lt;transmitDrops&gt;0&lt;/transmitDrops&gt;
     *         &lt;receiveErrors&gt;0&lt;/receiveErrors&gt;
     *         &lt;transmitErrors&gt;0&lt;/transmitErrors&gt;
     *         &lt;receiveFrameError&gt;0&lt;/receiveFrameError&gt;
     *         &lt;receiveOverRunError&gt;0&lt;/receiveOverRunError&gt;
     *         &lt;receiveCrcError&gt;0&lt;/receiveCrcError&gt;
     *         &lt;collisionCount&gt;0&lt;/collisionCount&gt;
     *     &lt;/portStatistic&gt;
     *     &lt;portStatistic&gt;
     *         &lt;nodeConnector type=&quot;OF&quot; id=&quot;5&quot;&gt;
     *             &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:01&quot;/&gt;
     *         &lt;/nodeConnector&gt;
     *         &lt;receivePackets&gt;2542&lt;/receivePackets&gt;
     *         &lt;transmitPackets&gt;2719&lt;/transmitPackets&gt;
     *         &lt;receiveBytes&gt;243012&lt;/receiveBytes&gt;
     *         &lt;transmitBytes&gt;255374&lt;/transmitBytes&gt;
     *         &lt;receiveDrops&gt;0&lt;/receiveDrops&gt;
     *         &lt;transmitDrops&gt;0&lt;/transmitDrops&gt;
     *         &lt;receiveErrors&gt;0&lt;/receiveErrors&gt;
     *         &lt;transmitErrors&gt;0&lt;/transmitErrors&gt;
     *         &lt;receiveFrameError&gt;0&lt;/receiveFrameError&gt;
     *         &lt;receiveOverRunError&gt;0&lt;/receiveOverRunError&gt;
     *         &lt;receiveCrcError&gt;0&lt;/receiveCrcError&gt;
     *         &lt;collisionCount&gt;0&lt;/collisionCount&gt;
     *     &lt;/portStatistic&gt;
     * &lt;/nodePortStatistics&gt;
     * </pre>
     */
    @Path("/{containerName}/port/node/{nodeType}/{nodeId}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(PortStatistics.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public PortStatistics getPortStatistics(
            @PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType,
            @PathParam("nodeId") String nodeId) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
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
        return new PortStatistics(node,
                statisticsManager.getNodeConnectorStatistics(node));
    }

    /**
     * Returns a list of all the Table Statistics on all Nodes.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     *
     * @return Returns a list of all the Table Statistics in a given Node.
     *
     *         <pre>
     *
     *  Example:
     *
     *  Request URL:
     *  http://host:8080/controller/nb/v2/statistics/default/table
     *
     *  Response in JSON:
     *
     * {
     *     "tableStatistics": [
     *         {
     *             "node": {
     *                 "@type": "OF",
     *                 "@id": "00:00:00:00:00:00:00:02"
     *             },
     *             "tableStatistic": [
     *                 {
     *                     "nodeTable": {
     *                         "@id": "0",
     *                         "node": {
     *                             "@type": "OF",
     *                             "@id": "00:00:00:00:00:00:00:02"
     *                         }
     *                     },
     *                     "activeCount": "11",
     *                     "lookupCount": "816",
     *                     "matchedCount": "220"
     *                 },
     *                 {
     *                     ...another table
     *                     .....
     *                     ........
     *                 }
     *
     *             ]
     *         }
     *     ]
     * }
     *
     *
     *  Response in XML:
     *  &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot; standalone=&quot;yes&quot;?&gt;
     *  &lt;list&gt;
     *  &lt;tableStatistics&gt;
     *      &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:01&quot;/&gt;
     *      &lt;tableStatistic&gt;
     *          &lt;nodeTable id=&quot;0&quot;&gt;
     *              &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:01&quot;/&gt;
     *          &lt;/nodeTable&gt;
     *          &lt;activeCount&gt;12&lt;/activeCount&gt;
     *          &lt;lookupCount&gt;10935&lt;/lookupCount&gt;
     *          &lt;matchedCount&gt;10084&lt;/matchedCount&gt;
     *      &lt;/tableStatistic&gt;
     *      &lt;tableStatistic&gt;
     *          &lt;nodeTable id=&quot;1&quot;&gt;
     *              &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:01&quot;/&gt;
     *          &lt;/nodeTable&gt;
     *          &lt;activeCount&gt;0&lt;/activeCount&gt;
     *          &lt;lookupCount&gt;0&lt;/lookupCount&gt;
     *          &lt;matchedCount&gt;0&lt;/matchedCount&gt;
     *      &lt;/tableStatistic&gt;
     *      &lt;tableStatistic&gt;
     *          &lt;nodeTable id=&quot;2&quot;&gt;
     *              &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:01&quot;/&gt;
     *          &lt;/nodeTable&gt;
     *          &lt;activeCount&gt;0&lt;/activeCount&gt;
     *          &lt;lookupCount&gt;0&lt;/lookupCount&gt;
     *          &lt;matchedCount&gt;0&lt;/matchedCount&gt;
     *      &lt;/tableStatistic&gt;
     *  &lt;/tableStatistics&gt;
     *  &lt;tableStatistics&gt;
     *  ...
     *  ......
     *  ........
     *  &lt;/tableStatistics&gt;
     *  &lt;/list&gt;
     *
     * </pre>
     */
    @Path("/{containerName}/table")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(AllTableStatistics.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public AllTableStatistics getTableStatistics(
            @PathParam("containerName") String containerName) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        handleDefaultDisabled(containerName);

        IStatisticsManager statisticsManager = getStatisticsService(containerName);
        if (statisticsManager == null) {
            throw new ServiceUnavailableException("Statistics manager"
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        List<TableStatistics> statistics = new ArrayList<TableStatistics>();
        for (Node node : switchManager.getNodes()) {
            List<NodeTableStatistics> stat = statisticsManager
                    .getNodeTableStatistics(node);
            TableStatistics tableStat = new TableStatistics(node, stat);
            statistics.add(tableStat);
        }
        return new AllTableStatistics(statistics);
    }

    /**
     * Returns a list of all the Table Statistics on a specific node.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @param nodeType
     *            Node Type as specified in {@link org.opendaylight.controller.sal.core.Node} class (e.g. OF for Openflow)
     * @param Node
     *            Identifier (e.g. MAC address)
     * @return Returns a list of all the Table Statistics in a given Node.
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://host:8080/controller/nb/v2/statistics/default/table/node/OF/00:00:00:00:00:00:00:01
     *
     * Response in JSON:
     * {
     *     "node": {
     *         "@type": "OF",
     *         "@id": "00:00:00:00:00:00:00:01"
     *     },
     *     "tableStatistic": [
     *         {
     *             "nodeTable": {
     *                 "@id": "0",
     *                 "node": {
     *                     "@type": "OF",
     *                     "@id": "00:00:00:00:00:00:00:01"
     *                 }
     *             },
     *             "activeCount": "12",
     *             "lookupCount": "11382",
     *             "matchedCount": "10524"
     *         },
     *         {
     *             "nodeTable": {
     *                 "@id": "1",
     *                 "node": {
     *                     "@type": "OF",
     *                     "@id": "00:00:00:00:00:00:00:01"
     *                 }
     *             },
     *             "activeCount": "0",
     *             "lookupCount": "0",
     *             "matchedCount": "0"
     *         }
     *    ]
     * }
     *
     * Response in XML:
     * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot; standalone=&quot;yes&quot;?&gt;
     * &lt;nodeTableStatistics&gt;
     *     &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:01&quot;/&gt;
     *     &lt;tableStatistic&gt;
     *         &lt;nodeTable id=&quot;0&quot;&gt;
     *             &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:01&quot;/&gt;
     *         &lt;/nodeTable&gt;
     *         &lt;activeCount&gt;12&lt;/activeCount&gt;
     *         &lt;lookupCount&gt;10935&lt;/lookupCount&gt;
     *         &lt;matchedCount&gt;10084&lt;/matchedCount&gt;
     *     &lt;/tableStatistic&gt;
     *     &lt;tableStatistic&gt;
     *         &lt;nodeTable id=&quot;1&quot;&gt;
     *             &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:01&quot;/&gt;
     *         &lt;/nodeTable&gt;
     *         &lt;activeCount&gt;0&lt;/activeCount&gt;
     *         &lt;lookupCount&gt;0&lt;/lookupCount&gt;
     *         &lt;matchedCount&gt;0&lt;/matchedCount&gt;
     *     &lt;/tableStatistic&gt;
     *     &lt;tableStatistic&gt;
     *         &lt;nodeTable id=&quot;2&quot;&gt;
     *             &lt;node type=&quot;OF&quot; id=&quot;00:00:00:00:00:00:00:01&quot;/&gt;
     *         &lt;/nodeTable&gt;
     *         &lt;activeCount&gt;0&lt;/activeCount&gt;
     *         &lt;lookupCount&gt;0&lt;/lookupCount&gt;
     *         &lt;matchedCount&gt;0&lt;/matchedCount&gt;
     *     &lt;/tableStatistic&gt;
     * &lt;/nodeTableStatistics&gt;
     *
     *
     * </pre>
     */
    @Path("/{containerName}/table/node/{nodeType}/{nodeId}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(TableStatistics.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public TableStatistics getTableStatistics(
            @PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType,
            @PathParam("nodeId") String nodeId) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
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
        return new TableStatistics(node,
                statisticsManager.getNodeTableStatistics(node));
    }

    private void handleDefaultDisabled(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            throw new InternalServerErrorException(
                    RestMessages.INTERNALERROR.toString());
        }
        if (containerName.equals(GlobalConstants.DEFAULT.toString())
                && containerManager.hasNonDefaultContainer()) {
            throw new ResourceConflictException(
                    RestMessages.DEFAULTDISABLED.toString());
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
