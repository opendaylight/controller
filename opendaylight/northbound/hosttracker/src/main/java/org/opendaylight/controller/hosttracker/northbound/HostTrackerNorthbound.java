
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
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
import org.opendaylight.controller.northbound.commons.exception.UnsupportedMediaTypeException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.ISwitchManager;

/**
 * Host Tracker Northbound REST APIs.<br>
 * This class provides REST APIs to track host location in a network. Host Location is represented by Host node connector 
 * which is essentially a logical entity that represents a Switch/Port. A host is represented by it's IP-address 
 * and mac-address.
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
public class HostTrackerNorthbound {

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
     * Returns a list of all Hosts : both configured via PUT API and dynamically learnt on the network.
     *
     * @param containerName Name of the Container. The Container name for the base controller is "default".
     * @return List of Active Hosts.
     */
    @Path("/{containerName}")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Hosts.class)
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Hosts getActiveHosts(
            @PathParam("containerName") String containerName) {
        IfIptoHost hostTracker = getIfIpToHostService(containerName);
        if (hostTracker == null) {
            throw new ServiceUnavailableException("Host Tracker "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return new Hosts(hostTracker.getAllHosts());
    }

    /**
     * Returns a list of Hosts that are statically configured and are connected to a NodeConnector that is down.
     *
     * @param containerName Name of the Container. The Container name for the base controller is "default".
     * @return List of inactive Hosts.
     */
    @Path("/{containerName}/inactive")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Hosts.class)
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Hosts getInactiveHosts(
            @PathParam("containerName") String containerName) {
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
     * @param containerName Name of the Container. The Container name for the base controller is "default".
     * @param networkAddress IP Address being looked up
     * @return host that matches the IP Address
     */
    @Path("/{containerName}/{networkAddress}")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(HostNodeConnector.class)
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 415, condition = "Invalid IP Address passed in networkAddress parameter"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public HostNodeConnector getHostDetails(
            @PathParam("containerName") String containerName,
            @PathParam("networkAddress") String networkAddress) {
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
     * @param containerName Name of the Container. The Container name for the base controller is "default".
     * @param networkAddress Host IP Address
     * @param dataLayerAddress Host L2 data-layer address.
     * @param nodeType Node Type as specifid by Node class
     * @param nodeId Node Identifier as specifid by Node class
     * @param nodeConnectorType Port Type as specified by NodeConnector class
     * @param nodeConnectorId Port Identifier as specified by NodeConnector class
     * @param vlan Vlan number
     * @return Response as dictated by the HTTP Response Status code
     */

    @Path("/{containerName}/{networkAddress}")
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
            @ResponseCode(code = 201, condition = "Flow Config processed successfully"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
            @ResponseCode(code = 415, condition = "Invalid IP Address passed in networkAddress parameter"),
            @ResponseCode(code = 500, condition = "Failed to create Static Host entry. Failure Reason included in HTTP Error response"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response addHost(@PathParam("containerName") String containerName,
            @PathParam("networkAddress") String networkAddress,
            @QueryParam("dataLayerAddress") String dataLayerAddress,
            @QueryParam("nodeType") String nodeType,
            @QueryParam("nodeId") String nodeId,
            @QueryParam("nodeConnectorType") String nodeConnectorType,
            @QueryParam("nodeConnectorId") String nodeConnectorId,
            @DefaultValue("0") @QueryParam("vlan") String vlan) {

        handleDefaultDisabled(containerName);

        IfIptoHost hostTracker = getIfIpToHostService(containerName);
        if (hostTracker == null) {
            throw new ServiceUnavailableException("Host Tracker "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = handleNodeAvailability(containerName, nodeType, nodeId);
        if (node == null) {
            throw new InternalServerErrorException(RestMessages.NONODE.
                                                   toString());
        }

        try {
            InetAddress.getByName(networkAddress);
        } catch (UnknownHostException e) {
            throw new UnsupportedMediaTypeException(networkAddress + " "
                    + RestMessages.INVALIDADDRESS.toString());
        }
        NodeConnector nc = NodeConnector.fromStringNoNode(nodeConnectorType, nodeConnectorId,
                                                          node);
        if (nc == null) {
            throw new ResourceNotFoundException(nodeConnectorType+"|"+nodeConnectorId + " : "
                    + RestMessages.NONODE.toString());
        }
        Status status = hostTracker.addStaticHost(networkAddress,
                                               dataLayerAddress,
                                               nc, vlan);
        if (status.isSuccess()) {
            return Response.status(Response.Status.CREATED).build();
        } else if (status.getCode().equals(StatusCode.BADREQUEST)) {
            throw new UnsupportedMediaTypeException(status.getDescription());
        }
        throw new InternalServerErrorException(status.getDescription());
    }

    /**
     * Delete a Static Host configuration
     *
     * @param containerName Name of the Container. The Container name for the base controller is "default".
     * @param networkAddress   IP Address
     * @return Response as dictated by the HTTP Response code.
     */

    @Path("/{containerName}/{networkAddress}")
    @DELETE
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Flow Config deleted successfully"),
            @ResponseCode(code = 404, condition = "The Container Name or Node-id or Flow Name passed is not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
            @ResponseCode(code = 415, condition = "Invalid IP Address passed in networkAddress parameter"),
            @ResponseCode(code = 500, condition = "Failed to delete Flow config. Failure Reason included in HTTP Error response"),
            @ResponseCode(code = 503, condition = "One or more of Controller service is unavailable") })
    public Response deleteFlow(
            @PathParam(value = "containerName") String containerName,
            @PathParam(value = "networkAddress") String networkAddress) {

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
            return Response.ok().build();
        }
        throw new InternalServerErrorException(status.getDescription());

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
