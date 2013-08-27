/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker.northbound;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.JAXBElement;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.exception.UnsupportedMediaTypeException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.ISwitchManager;

/**
 * Host Tracker Northbound REST APIs.<br>
 * This class provides REST APIs to track host location in a network. Host
 * Location is represented by Host node connector which is essentially a logical
 * entity that represents a Switch/Port. A host is represented by it's
 * IP-address and mac-address.
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
public class HostTrackerNorthbound {

    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        username = context.getUserPrincipal().getName();
    }

    protected String getUserName() {
        return username;
    }

    private IfIptoHost getIfIpToHostService(String containerName) {
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

        IfIptoHost hostTracker = (IfIptoHost) ServiceHelper.getInstance(
                IfIptoHost.class, containerName, this);

        if (hostTracker == null) {
            throw new ServiceUnavailableException("Host Tracker "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return hostTracker;
    }

    /**
     * Returns a list of all Hosts : both configured via PUT API and dynamically
     * learnt on the network.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @return List of Active Hosts.
     *         <pre>
     *
     * Example:
     *
     * RequestURL:
     *
     * http://localhost:8080//controller/nb/v2/host/default
     *
     * Response in XML
     *
     * &lt;hosts&gt;
     * &#x20;&lt;host&gt;
     * &#x20;&#x20;&lt;dataLayerAddress xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ethernetAddress"&gt;
     * &#x20;&#x20;&lt;macAddress&gt;00:00:00:00:01:01&lt;/macAddress&gt;&lt;/dataLayerAddress&gt;
     * &#x20;&#x20;&lt;networkAddress&gt;1.1.1.1&lt;/networkAddress&gt;
     * &#x20;&#x20;&lt;nodeConnector id="9" type="OF"&gt;&lt;node id="00:00:00:00:00:00:00:01" type="OF"/&gt;&lt;/nodeConnector&gt;
     * &#x20;&#x20;&lt;vlan&gt;0&lt;/vlan&gt;
     * &#x20;&#x20;&lt;staticHost&gt;false&lt;/staticHost&gt;
     * &#x20;&#x20;&lt;/host&gt;
     * &#x20;&#x20;&lt;host&gt;
     * &#x20;&#x20;&lt;dataLayerAddress xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ethernetAddress"&gt;
     * &#x20;&#x20;&lt;macAddress&gt;00:00:00:00:02:02&lt;/macAddress&gt;&lt;/dataLayerAddress&gt;
     * &#x20;&#x20;&lt;networkAddress&gt;2.2.2.1&lt;/networkAddress&gt;
     * &#x20;&#x20;&lt;nodeConnector id="5" type="OF"&gt;&lt;node id="00:00:00:00:00:00:00:02" type="OF"/&gt;&lt;/nodeConnector&gt;
     * &#x20;&#x20;&lt;vlan&gt;0&lt;/vlan&gt;
     * &#x20;&#x20;&lt;staticHost&gt;false&lt;/staticHost&gt;
     * &#x20;&#x20;&lt;/host&gt;
     * &lt;/hosts&gt;
     *
     * Response in JSON:
     *
     * {"host":[{"dataLayerAddress":{"@type":"ethernetAddress","macAddress":"00:00:00:00:01:01"},
     * "networkAddress":"1.1.1.1","nodeConnector":{"@id":"9","@type":"OF",
     * "node":{"@id":"00:00:00:00:00:00:00:01","@type":"OF"}},"vlan":"0","staticHost":"false"},
     * {"dataLayerAddress":{"@type":"ethernetAddress","macAddress":"00:00:00:00:02:02"},
     * "networkAddress":"2.2.2.1","nodeConnector":{"@id":"5","@type":"OF","node":{"@id":"00:00:00:00:00:00:00:02","@type":"OF"}},
     * "vlan":"0","staticHost":"false"}]}
     * </pre>
     */
    @Path("/{containerName}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Hosts.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Hosts getActiveHosts(@PathParam("containerName") String containerName) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
        IfIptoHost hostTracker = getIfIpToHostService(containerName);
        if (hostTracker == null) {
            throw new ServiceUnavailableException("Host Tracker "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return new Hosts(hostTracker.getAllHosts());
    }

    /**
     * Returns a list of Hosts that are statically configured and are connected
     * to a NodeConnector that is down.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @return List of inactive Hosts.
     *     <pre>
     *
     * Example:
     *
     * RequestURL:
     *
     * http://localhost:8080//controller/nb/v2/host/default/inactive
     *
     * Response in XML
     *
     * &lt;hosts&gt;
     * &#x20;&lt;host&gt;
     * &#x20;&#x20;&lt;dataLayerAddress xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ethernetAddress"&gt;
     * &#x20;&#x20;&lt;macAddress&gt;00:00:00:00:01:01&lt;/macAddress&gt;&lt;/dataLayerAddress&gt;
     * &#x20;&#x20;&lt;networkAddress&gt;1.1.1.1&lt;/networkAddress&gt;
     * &#x20;&#x20;&lt;nodeConnector id="9" type="OF"&gt;&lt;node id="00:00:00:00:00:00:00:01" type="OF"/&gt;&lt;/nodeConnector&gt;
     * &#x20;&#x20;&lt;vlan&gt;0&lt;/vlan&gt;
     * &#x20;&#x20;&lt;staticHost&gt;true&lt;/staticHost&gt;
     * &#x20;&#x20;&lt;/host&gt;
     * &#x20;&#x20;&lt;host&gt;
     * &#x20;&#x20;&lt;dataLayerAddress xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ethernetAddress"&gt;
     * &#x20;&#x20;&lt;macAddress&gt;00:00:00:00:02:02&lt;/macAddress&gt;&lt;/dataLayerAddress&gt;
     * &#x20;&#x20;&lt;networkAddress&gt;2.2.2.1&lt;/networkAddress&gt;
     * &#x20;&#x20;&lt;nodeConnector id="5" type="OF"&gt;&lt;node id="00:00:00:00:00:00:00:02" type="OF"/&gt;&lt;/nodeConnector&gt;
     * &#x20;&#x20;&lt;vlan&gt;0&lt;/vlan&gt;
     * &#x20;&#x20;&lt;staticHost&gt;true&lt;/staticHost&gt;
     * &#x20;&#x20;&lt;/host&gt;
     * &lt;/hosts&gt;
     *
     * Response in JSON:
     *
     * {"host":[{"dataLayerAddress":{"@type":"ethernetAddress","macAddress":"00:00:00:00:01:01"},
     * "networkAddress":"3.3.3.1","nodeConnector":{"@id":"9","@type":"OF",
     * "node":{"@id":"00:00:00:00:00:00:03:03","@type":"OF"}},"vlan":"0","staticHost":"true"},
     * {"dataLayerAddress":{"@type":"ethernetAddress","macAddress":"00:00:00:00:04:04"},
     * "networkAddress":"4.4.4.1","nodeConnector":{"@id":"5","@type":"OF","node":{"@id":"00:00:00:00:00:00:00:02","@type":"OF"}},
     * "vlan":"0","staticHost":"true"}]}
     * </pre>
     */
    @Path("/{containerName}/inactive")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Hosts.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Hosts getInactiveHosts(
            @PathParam("containerName") String containerName) {
        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
        IfIptoHost hostTracker = getIfIpToHostService(containerName);
        if (hostTracker == null) {
            throw new ServiceUnavailableException("Host Tracker "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return new Hosts(hostTracker.getInactiveStaticHosts());
    }

    /**
     * Returns a host that matches the IP Address value passed as parameter.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @param networkAddress
     *            IP Address being looked up
     * @return host that matches the IP Address
     *
     *  <pre>
     *
     *  Example:
     *
     *  RequestURL:
     *
     *  http://localhost:8080/controller/nb/v2/host/default/1.1.1.1
     *
     *  Response in XML
     *
     *  &lt;host&gt;
     *  &#x20;&#x20;&lt;dataLayerAddress xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ethernetAddress"&gt;
     *  &#x20;&#x20;&lt;macAddress&gt;00:00:00:00:01:01&lt;/macAddress&gt;&lt;/dataLayerAddress&gt;
     *  &#x20;&#x20;&lt;networkAddress&gt;1.1.1.1&lt;/networkAddress&gt;
     *  &#x20;&#x20;&lt;nodeConnector id="9" type="OF"&gt;&lt;node id="00:00:00:00:00:00:00:01" type="OF"/&gt;&lt;/nodeConnector&gt;
     *  &#x20;&#x20;&lt;vlan&gt;0&lt;/vlan&gt;
     *  &#x20;&#x20;&lt;staticHost&gt;false&lt;/staticHost&gt;
     *  &lt;/host&gt;
     *
     * Response in JSON
     *
     *  {"dataLayerAddress":{"@type":"ethernetAddress","macAddress":"00:00:00:00:01:04"},
     *  "networkAddress":"3.3.3.4",
     *  "nodeConnector":{"@id":"9","@type":"OF","node":{"@id":"00:00:00:00:00:00:00:01","@type":"OF"}},
     *  "vlan":"0",
     * "staticHost":"true"}
     *
     */
    @Path("/{containerName}/{networkAddress}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(HostNodeConnector.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 415, condition = "Invalid IP Address passed in networkAddress parameter"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public HostNodeConnector getHostDetails(
            @PathParam("containerName") String containerName,
            @PathParam("networkAddress") String networkAddress) {
        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
        IfIptoHost hostTracker = getIfIpToHostService(containerName);
        if (hostTracker == null) {
            throw new ServiceUnavailableException("Host Tracker "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        InetAddress ip;
        try {
            ip = InetAddress.getByName(networkAddress);
        } catch (UnknownHostException e) {
            throw new UnsupportedMediaTypeException(networkAddress + " "
                    + RestMessages.INVALIDADDRESS.toString());
        }
        for (HostNodeConnector host : hostTracker.getAllHosts()) {
            if (host.getNetworkAddress().equals(ip)) {
                return host;
            }
        }
        throw new ResourceNotFoundException(RestMessages.NOHOST.toString());
    }

    /**
     * Add a Static Host configuration
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @param networkAddress
     *            Host IP Address
     * @param dataLayerAddress
     *            Host L2 data-layer address.
     * @param nodeType
     *            Node Type as specifid by Node class
     * @param nodeId
     *            Node Identifier as specifid by Node class
     * @param nodeConnectorType
     *            Port Type as specified by NodeConnector class
     * @param nodeConnectorId
     *            Port Identifier as specified by NodeConnector class
     * @param vlan
     *            Vlan number
     * @return Response as dictated by the HTTP Response Status code
     *
     * Example:
     *
     *  RequestURL:
     *
     *  http://localhost:8080/controller/nb/v2/host/default/3.3.3.6
     *
     *  Request in XML
     *
     *  &lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
     *  &lt;hostConfig&gt;
     *  &#x20;&#x20;&lt;dataLayerAddress&gt;00:00:00:00:01:06&lt;/dataLayerAddress&gt;
     *  &#x20;&#x20;&lt;nodeType&gt;OF&lt;/nodeType&gt;
     *  &#x20;&#x20;&lt;nodeId&gt;00:00:00:00:00:00:00:01&lt;/nodeId&gt;
     *  &#x20;&#x20;&lt;nodeConnectorType&gt;OF&lt;/nodeConnectorType&gt;
     *  &#x20;&#x20;&lt;nodeConnectorId&gt;9&lt;/nodeConnectorId&gt;
     *  &#x20;&#x20;&lt;vlan&gt;0&lt;/vlan&gt;
     *  &lt;/hostConfig&gt;
     */

    @Path("/{containerName}/{networkAddress}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 201, condition = "Static host created successfully"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
            @ResponseCode(code = 415, condition = "Invalid IP Address passed in networkAddress parameter"),
            @ResponseCode(code = 500, condition = "Failed to create Static Host entry. Failure Reason included in HTTP Error response"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response addHost(@PathParam("containerName") String containerName,
            @PathParam("networkAddress") String networkAddress,
            @TypeHint(HostConfig.class) JAXBElement<HostConfig> hostConfig) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
        handleDefaultDisabled(containerName);

        IfIptoHost hostTracker = getIfIpToHostService(containerName);
        if (hostTracker == null) {
            throw new ServiceUnavailableException("Host Tracker "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        HostConfig hc = hostConfig.getValue();
        Node node = handleNodeAvailability(containerName, hc.getNodeType(), hc.getNodeId());
        if (node == null) {
            throw new InternalServerErrorException(
                    RestMessages.NONODE.toString());
        }

        try {
            InetAddress.getByName(networkAddress);
        } catch (UnknownHostException e) {
            throw new UnsupportedMediaTypeException(networkAddress + " "
                    + RestMessages.INVALIDADDRESS.toString());
        }
        NodeConnector nc = NodeConnector.fromStringNoNode(hc.getNodeConnectorType(), hc.getNodeConnectorId(), node);
        if (nc == null) {
            throw new ResourceNotFoundException(hc.getNodeConnectorType() + "|"
                    + hc.getNodeConnectorId() + " : " + RestMessages.NONODE.toString());
        }
        Status status = hostTracker.addStaticHost(networkAddress,
                hc.getDataLayerAddress(), nc, hc.getVlan());
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Static Host", username, "added", networkAddress, containerName);
            return Response.status(Response.Status.CREATED).build();
        } else if (status.getCode().equals(StatusCode.BADREQUEST)) {
            throw new UnsupportedMediaTypeException(status.getDescription());
        }
        throw new InternalServerErrorException(status.getDescription());
    }

    /**
     * Delete a Static Host configuration
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @param networkAddress
     *            IP Address
     * @return Response as dictated by the HTTP Response code.
     */

    @Path("/{containerName}/{networkAddress}")
    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Flow Config deleted successfully"),
            @ResponseCode(code = 404, condition = "The Container Name or Node-id or Flow Name passed is not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
            @ResponseCode(code = 415, condition = "Invalid IP Address passed in networkAddress parameter"),
            @ResponseCode(code = 500, condition = "Failed to delete Flow config. Failure Reason included in HTTP Error response"),
            @ResponseCode(code = 503, condition = "One or more of Controller service is unavailable") })
    public Response deleteFlow(
            @PathParam(value = "containerName") String containerName,
            @PathParam(value = "networkAddress") String networkAddress) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
        handleDefaultDisabled(containerName);
        IfIptoHost hostTracker = getIfIpToHostService(containerName);
        if (hostTracker == null) {
            throw new ServiceUnavailableException("Host Tracker "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        try {
            InetAddress.getByName(networkAddress);
        } catch (UnknownHostException e) {
            throw new UnsupportedMediaTypeException(networkAddress + " "
                    + RestMessages.INVALIDADDRESS.toString());
        }

        Status status = hostTracker.removeStaticHost(networkAddress);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Static Host", username, "removed", networkAddress, containerName);
            return Response.ok().build();
        }
        throw new InternalServerErrorException(status.getDescription());

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
