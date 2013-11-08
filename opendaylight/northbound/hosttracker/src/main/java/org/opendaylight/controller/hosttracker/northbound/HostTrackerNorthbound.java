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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.BadRequestException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
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
 * HTTPS Authentication is disabled by default.
 *
 */

@Path("/")
public class HostTrackerNorthbound {

    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        if (context != null && context.getUserPrincipal() != null) username = context.getUserPrincipal().getName();
    }

    protected String getUserName() {
        return username;
    }

    private IfIptoHost getIfIpToHostService(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper.getGlobalInstance(
                IContainerManager.class, this);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames) {
            if (cName.trim().equalsIgnoreCase(containerName.trim())) {
                found = true;
                break;
            }
        }

        if (!found) {
            throw new ResourceNotFoundException(containerName + " " + RestMessages.NOCONTAINER.toString());
        }

        IfIptoHost hostTracker = (IfIptoHost) ServiceHelper.getInstance(IfIptoHost.class, containerName, this);
        if (hostTracker == null) {
            throw new ServiceUnavailableException("Host Tracker " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return hostTracker;
    }

    private Hosts convertHosts(Set<HostNodeConnector> hostNodeConnectors) {
        if(hostNodeConnectors == null) {
            return null;
        }
        Set<HostConfig> hosts = new HashSet<HostConfig>();
        for(HostNodeConnector hnc : hostNodeConnectors) {
            hosts.add(HostConfig.convert(hnc));
        }
        return new Hosts(hosts);
    }

    /**
     * Returns a list of all Hosts : both configured via PUT API and dynamically
     * learnt on the network.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @return List of Active Hosts.
     * <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/hosttracker/default/hosts/active
     *
     * Response body in XML
     *
     * &lt;list&gt;
     * &#x20;&lt;hostConfig&gt;
     * &#x20;&#x20;&lt;dataLayerAddress&gt;00:00:00:00:01:01&lt;/dataLayerAddress&gt;
     * &#x20;&#x20;&lt;networkAddress&gt;1.1.1.1&lt;/networkAddress&gt;
     * &#x20;&#x20;&lt;nodeType&gt;OF&lt;/nodeType&gt;
     * &#x20;&#x20;&lt;nodeId&gt;00:00:00:00:00:00:00:01&lt;/nodeId&gt;
     * &#x20;&#x20;&lt;nodeConnectorType&gt;OF&lt;/nodeConnectorType&gt;
     * &#x20;&#x20;&lt;nodeConnectorId&gt;9&lt;/nodeConnectorId&gt;
     * &#x20;&#x20;&lt;vlan&gt;0&lt;/vlan&gt;
     * &#x20;&#x20;&lt;staticHost&gt;false&lt;/staticHost&gt;
     * &#x20;&lt;/hostConfig&gt;
     * &#x20;&lt;hostConfig&gt;
     * &#x20;&#x20;&lt;dataLayerAddress&gt;00:00:00:00:02:02&lt;/dataLayerAddress&gt;
     * &#x20;&#x20;&lt;networkAddress&gt;2.2.2.2&lt;/networkAddress&gt;
     * &#x20;&#x20;&lt;nodeType&gt;OF&lt;/nodeType&gt;
     * &#x20;&#x20;&lt;nodeId&gt;00:00:00:00:00:00:00:02&lt;/nodeId&gt;
     * &#x20;&#x20;&lt;nodeConnectorType&gt;OF&lt;/nodeConnectorType&gt;
     * &#x20;&#x20;&lt;nodeConnectorId&gt;5&lt;/nodeConnectorId&gt;
     * &#x20;&#x20;&lt;vlan&gt;0&lt;/vlan&gt;
     * &#x20;&#x20;&lt;staticHost&gt;false&lt;/staticHost&gt;
     * &#x20;&lt;/hostConfig&gt;
     * &lt;/list&gt;
     *
     * Response body in JSON:
     *
     * {
     * &#x20;"hostConfig":[
     * &#x20;&#x20;{
     * &#x20;&#x20;&#x20;"dataLayerAddress":"00:00:00:00:01:01",
     * &#x20;&#x20;&#x20;"nodeType":"OF",
     * &#x20;&#x20;&#x20;"nodeId":"00:00:00:00:00:00:00:01",
     * &#x20;&#x20;&#x20;"nodeConnectorType":"OF",
     * &#x20;&#x20;&#x20;"nodeConnectorId":"9",
     * &#x20;&#x20;&#x20;"vlan":"0",
     * &#x20;&#x20;&#x20;"staticHost":"false",
     * &#x20;&#x20;&#x20;"networkAddress":"1.1.1.1"
     * &#x20;&#x20;},
     * &#x20;&#x20;{
     * &#x20;&#x20;&#x20;"dataLayerAddress":"00:00:00:00:02:02",
     * &#x20;&#x20;&#x20;"nodeType":"OF",
     * &#x20;&#x20;&#x20;"nodeId":"00:00:00:00:00:00:00:02",
     * &#x20;&#x20;&#x20;"nodeConnectorType":"OF",
     * &#x20;&#x20;&#x20;"nodeConnectorId":"5",
     * &#x20;&#x20;&#x20;"vlan":"0",
     * &#x20;&#x20;&#x20;"staticHost":"false",
     * &#x20;&#x20;&#x20;"networkAddress":"2.2.2.2"
     * &#x20;&#x20;}
     * &#x20;]
     * }
     * </pre>
     */
    @Path("/{containerName}/hosts/active")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Hosts.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Hosts getActiveHosts(@PathParam("containerName") String containerName) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        IfIptoHost hostTracker = getIfIpToHostService(containerName);
        return convertHosts(hostTracker.getAllHosts());
    }

    /**
     * Returns a list of Hosts that are statically configured and are connected
     * to a NodeConnector that is down.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @return List of inactive Hosts.
     * <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/hosttracker/default/hosts/inactive
     *
     * Response body in XML
     *
     * &lt;list&gt;
     * &#x20;&lt;hostConfig&gt;
     * &#x20;&#x20;&lt;dataLayerAddress&gt;00:00:00:00:01:01&lt;/dataLayerAddress&gt;
     * &#x20;&#x20;&lt;networkAddress&gt;1.1.1.1&lt;/networkAddress&gt;
     * &#x20;&#x20;&lt;nodeType&gt;OF&lt;/nodeType&gt;
     * &#x20;&#x20;&lt;nodeId&gt;00:00:00:00:00:00:00:01&lt;/nodeId&gt;
     * &#x20;&#x20;&lt;nodeConnectorType&gt;OF&lt;/nodeConnectorType&gt;
     * &#x20;&#x20;&lt;nodeConnectorId&gt;9&lt;/nodeConnectorId&gt;
     * &#x20;&#x20;&lt;vlan&gt;0&lt;/vlan&gt;
     * &#x20;&#x20;&lt;staticHost&gt;false&lt;/staticHost&gt;
     * &#x20;&lt;/hostConfig&gt;
     * &#x20;&lt;hostConfig&gt;
     * &#x20;&#x20;&lt;dataLayerAddress&gt;00:00:00:00:02:02&lt;/dataLayerAddress&gt;
     * &#x20;&#x20;&lt;networkAddress&gt;2.2.2.2&lt;/networkAddress&gt;
     * &#x20;&#x20;&lt;nodeType&gt;OF&lt;/nodeType&gt;
     * &#x20;&#x20;&lt;nodeId&gt;00:00:00:00:00:00:00:02&lt;/nodeId&gt;
     * &#x20;&#x20;&lt;nodeConnectorType&gt;OF&lt;/nodeConnectorType&gt;
     * &#x20;&#x20;&lt;nodeConnectorId&gt;5&lt;/nodeConnectorId&gt;
     * &#x20;&#x20;&lt;vlan&gt;0&lt;/vlan&gt;
     * &#x20;&#x20;&lt;staticHost&gt;false&lt;/staticHost&gt;
     * &#x20;&lt;/hostConfig&gt;
     * &lt;/list&gt;
     *
     * Response body in JSON:
     *
     * {
     * &#x20;"hostConfig":[
     * &#x20;&#x20;{
     * &#x20;&#x20;&#x20;"dataLayerAddress":"00:00:00:00:01:01",
     * &#x20;&#x20;&#x20;"nodeType":"OF",
     * &#x20;&#x20;&#x20;"nodeId":"00:00:00:00:00:00:00:01",
     * &#x20;&#x20;&#x20;"nodeConnectorType":"OF",
     * &#x20;&#x20;&#x20;"nodeConnectorId":"9",
     * &#x20;&#x20;&#x20;"vlan":"0",
     * &#x20;&#x20;&#x20;"staticHost":"false",
     * &#x20;&#x20;&#x20;"networkAddress":"1.1.1.1"
     * &#x20;&#x20;},
     * &#x20;&#x20;{
     * &#x20;&#x20;&#x20;"dataLayerAddress":"00:00:00:00:02:02",
     * &#x20;&#x20;&#x20;"nodeType":"OF",
     * &#x20;&#x20;&#x20;"nodeId":"00:00:00:00:00:00:00:02",
     * &#x20;&#x20;&#x20;"nodeConnectorType":"OF",
     * &#x20;&#x20;&#x20;"nodeConnectorId":"5",
     * &#x20;&#x20;&#x20;"vlan":"0",
     * &#x20;&#x20;&#x20;"staticHost":"false",
     * &#x20;&#x20;&#x20;"networkAddress":"2.2.2.2"
     * &#x20;&#x20;}
     * &#x20;]
     * }
     * </pre>
     */
    @Path("/{containerName}/hosts/inactive")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Hosts.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Hosts getInactiveHosts(
            @PathParam("containerName") String containerName) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        IfIptoHost hostTracker = getIfIpToHostService(containerName);
        return convertHosts(hostTracker.getInactiveStaticHosts());
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
     * <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/hosttracker/default/address/1.1.1.1
     *
     * Response body in XML
     *
     * &lt;hostConfig&gt;
     * &#x20;&lt;dataLayerAddress&gt;00:00:00:00:01:01&lt;/dataLayerAddress&gt;
     * &#x20;&lt;networkAddress&gt;1.1.1.1&lt;/networkAddress&gt;
     * &#x20;&lt;nodeType&gt;OF&lt;/nodeType&gt;
     * &#x20;&lt;nodeId&gt;00:00:00:00:00:00:00:01&lt;/nodeId&gt;
     * &#x20;&lt;nodeConnectorType&gt;OF&lt;/nodeConnectorType&gt;
     * &#x20;&lt;nodeConnectorId&gt;9&lt;/nodeConnectorId&gt;
     * &#x20;&lt;vlan&gt;0&lt;/vlan&gt;
     * &#x20;&lt;staticHost&gt;false&lt;/staticHost&gt;
     * &lt;/hostConfig&gt;
     *
     * Response body in JSON:
     *
     * {
     * &#x20;"dataLayerAddress":"00:00:00:00:01:01",
     * &#x20;"nodeType":"OF",
     * &#x20;"nodeId":"00:00:00:00:00:00:00:01",
     * &#x20;"nodeConnectorType":"OF",
     * &#x20;"nodeConnectorId":"9",
     * &#x20;"vlan":"0",
     * &#x20;"staticHost":"false",
     * &#x20;"networkAddress":"1.1.1.1"
     * }
     * </pre>
     */
    @Path("/{containerName}/address/{networkAddress}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(HostConfig.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 400, condition = "Invalid IP specified in networkAddress parameter"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public HostConfig getHostDetails(
            @PathParam("containerName") String containerName,
            @PathParam("networkAddress") String networkAddress) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        IfIptoHost hostTracker = getIfIpToHostService(containerName);

        InetAddress ip;
        try {
            ip = InetAddress.getByName(networkAddress);
        } catch (UnknownHostException e) {
            throw new BadRequestException(RestMessages.INVALIDADDRESS.toString() + " " + networkAddress);
        }
        for (HostNodeConnector host : hostTracker.getAllHosts()) {
            if (host.getNetworkAddress().equals(ip)) {
                return HostConfig.convert(host);
            }
        }
        throw new ResourceNotFoundException(RestMessages.NOHOST.toString());
    }

    /**
     * Add a Static Host configuration. If a host by the given address already
     * exists, this method will respond with a non-successful status response.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @param networkAddress
     *            Host IP Address
     * @param hostConfig
     *            Host Config Details
     * @return Response as dictated by the HTTP Response Status code
     *
     * <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/hosttracker/default/address/1.1.1.1
     *
     * Request body in XML
     *
     * &lt;hostConfig&gt;
     * &#x20;&lt;dataLayerAddress&gt;00:00:00:00:01:01&lt;/dataLayerAddress&gt;
     * &#x20;&lt;networkAddress&gt;1.1.1.1&lt;/networkAddress&gt;
     * &#x20;&lt;nodeType&gt;OF&lt;/nodeType&gt;
     * &#x20;&lt;nodeId&gt;00:00:00:00:00:00:00:01&lt;/nodeId&gt;
     * &#x20;&lt;nodeConnectorType&gt;OF&lt;/nodeConnectorType&gt;
     * &#x20;&lt;nodeConnectorId&gt;9&lt;/nodeConnectorId&gt;
     * &#x20;&lt;vlan&gt;1&lt;/vlan&gt;
     * &#x20;&lt;staticHost&gt;true&lt;/staticHost&gt;
     * &lt;/hostConfig&gt;
     *
     * Request body in JSON:
     *
     * {
     * &#x20;"dataLayerAddress":"00:00:00:00:01:01",
     * &#x20;"nodeType":"OF",
     * &#x20;"nodeId":"00:00:00:00:00:00:00:01",
     * &#x20;"nodeConnectorType":"OF",
     * &#x20;"nodeConnectorId":"9",
     * &#x20;"vlan":"1",
     * &#x20;"staticHost":"true",
     * &#x20;"networkAddress":"1.1.1.1"
     * }
     * </pre>
     */

    @Path("/{containerName}/address/{networkAddress}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 201, condition = "Static host created successfully"),
            @ResponseCode(code = 400, condition = "Invalid parameters specified, see response body for details"),
            @ResponseCode(code = 404, condition = "The container or resource is not found"),
            @ResponseCode(code = 409, condition = "Resource conflict, see response body for details"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response addHost(@Context UriInfo uriInfo, @PathParam("containerName") String containerName,
            @PathParam("networkAddress") String networkAddress,
            @TypeHint(HostConfig.class) HostConfig hostConfig) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User is not authorized to perform this operation on container " + containerName)
                    .build();
        }
        handleDefaultDisabled(containerName);

        IfIptoHost hostTracker = getIfIpToHostService(containerName);

        HostConfig hc = hostConfig;
        if (!networkAddress.equals(hc.getNetworkAddress())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Resource name in config object doesn't match URI")
                    .build();
        }
        if (!hc.isStaticHost()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Can only add static host.")
                    .build();
        }
        Node node = handleNodeAvailability(containerName, hc.getNodeType(), hc.getNodeId());
        NodeConnector nc = NodeConnector.fromStringNoNode(hc.getNodeConnectorType(), hc.getNodeConnectorId(), node);

        Status status = hostTracker.addStaticHost(networkAddress, hc.getDataLayerAddress(), nc, hc.getVlan());
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Static Host", username, "added", networkAddress, containerName);
            return Response.created(uriInfo.getRequestUri()).build();
        }

        return NorthboundUtils.getResponse(status);
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
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/hosttracker/default/address/1.1.1.1
     *
     */

    @Path("/{containerName}/address/{networkAddress}")
    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 204, condition = "Static host deleted successfully"),
            @ResponseCode(code = 404, condition = "The container or a specified resource was not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
            @ResponseCode(code = 503, condition = "One or more of Controller service is unavailable") })
    public Response deleteHost(
            @PathParam(value = "containerName") String containerName,
            @PathParam(value = "networkAddress") String networkAddress) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User is not authorized to perform this operation on container " + containerName)
                    .build();
        }
        handleDefaultDisabled(containerName);
        IfIptoHost hostTracker = getIfIpToHostService(containerName);

        Status status = hostTracker.removeStaticHost(networkAddress);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Static Host", username, "removed", networkAddress, containerName);
            return Response.noContent().build();
        }
        return NorthboundUtils.getResponse(status);

    }

    private void handleDefaultDisabled(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            throw new ServiceUnavailableException(
                    RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (containerName.equals(GlobalConstants.DEFAULT.toString())
                && containerManager.hasNonDefaultContainer()) {
            throw new ResourceConflictException(
                    RestMessages.DEFAULTDISABLED.toString());
        }
    }

    private Node handleNodeAvailability(String containerName, String nodeType, String nodeId) {

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
