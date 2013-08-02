
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.bridgedomain.northbound;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.connectionmanager.IConnectionManager;
import org.opendaylight.controller.northbound.commons.exception.NotAcceptableException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.IBridgeDomainConfigService;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

/**
 * BridgeDomain Configuration Northbound APIs
 *
 * <br><br>
 * Authentication scheme : <b>HTTP Basic</b><br>
 * Authentication realm : <b>opendaylight</b><br>
 * Transport : <b>HTTP and HTTPS</b><br>
 * <br>
 * HTTPS Authentication is disabled by default. Administrator can enable it in tomcat-server.xml after adding
 * a proper keystore / SSL certificate from a trusted authority.<br>
 * More info : http://tomcat.apache.org/tomcat-7.0-doc/ssl-howto.html#Configuration
 */
@Path("/")
public class BridgeDomainNorthbound {
    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        username = context.getUserPrincipal().getName();
    }
    protected String getUserName() {
        return username;
    }

    private IBridgeDomainConfigService getConfigurationService() {
        return (IBridgeDomainConfigService) ServiceHelper
                .getGlobalInstance(IBridgeDomainConfigService.class, this);
    }

    private IConnectionManager getConnectionManager() {
        return (IConnectionManager) ServiceHelper
                .getGlobalInstance(IConnectionManager.class, this);
    }

    /**
     * If a Network Configuration Service needs a special Management Connection and if the
     * Node Type is unknown, use this REST api to connect to the management session.
     * <pre>
     * Example :
     * Request : PUT http://localhost:8080/controller/nb/v2/networkconfig/bridgedomain/connect/mgmt1/1.1.1.1/6634
     * Response : Node :
     *                  xml : &lt;node type="STUB" id="mgmt1"/&gt;
     *                  json: {"@type": "STUB","@id": "mgmt1"}
     *</pre>
     * @param nodeName User-Defined name of the node to connect with. This can be any alpha numeric value
     * @param ipAddress IP Address of the Node to connect with.
     * @param port Layer4 Port of the management session to connect with.
     * @return Node If the connection is successful, HTTP 404 otherwise.
     */

    @Path("/connect/{nodeName}/{ipAddress}/{port}/")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Node.class)
    @StatusCodes( {
        @ResponseCode(code = 201, condition = "Node connected successfully"),
        @ResponseCode(code = 404, condition = "Could not connect to the Node with the specified parameters"),
        @ResponseCode(code = 406, condition = "Invalid IP Address or Port parameter passed."),
        @ResponseCode(code = 503, condition = "Connection Manager Service not available")} )
    public Node connect(
            @PathParam(value = "nodeName") String nodeName,
            @PathParam(value = "ipAddress") String ipAddress,
            @PathParam(value = "port") String port) {

        IConnectionManager connectionManager = getConnectionManager();
        if (connectionManager == null) {
            throw new ServiceUnavailableException("IConnectionManager not available.");
        }

        if (!NetUtils.isIPv4AddressValid(ipAddress)) {
            throw new NotAcceptableException("Invalid ip address "+ipAddress);
        }

        try {
            Integer.parseInt(port);
        } catch (Exception e) {
            throw new NotAcceptableException("Invalid Layer4 Port "+port);
        }

        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        params.put(ConnectionConstants.ADDRESS, ipAddress);
        params.put(ConnectionConstants.PORT, port);

        Node node = null;
        try {
            node = connectionManager.connect(nodeName, params);
            if (node == null) {
                throw new ResourceNotFoundException("Failed to connect to Node at "+ipAddress+":"+port);
            }
            return node;
        } catch (Exception e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    /**
     * If a Network Configuration Service needs a special Management Connection, and if the
     * node Type is known, the user can choose to use this REST api to connect to the management session.
     * <pre>
     * Example :
     * Request : PUT http://localhost:8080/controller/nb/v2/networkconfig/bridgedomain/connect/STUB/mgmt1/1.1.1.1/6634
     * Response : Node :
     *                  xml : &lt;node type="STUB" id="mgmt1"/&gt;
     *                  json: {"@type": "STUB","@id": "mgmt1"}
     *</pre>
     * @param nodeName User-Defined name of the node to connect with. This can be any alpha numeric value
     * @param ipAddress IP Address of the Node to connect with.
     * @param port Layer4 Port of the management session to connect with.
     * @return Node If the connection is successful, HTTP 404 otherwise.
     */

    @Path("/connect/{nodeType}/{nodeId}/{ipAddress}/{port}/")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Node.class)
    @StatusCodes( {
        @ResponseCode(code = 201, condition = "Node connected successfully"),
        @ResponseCode(code = 404, condition = "Could not connect to the Node with the specified parameters"),
        @ResponseCode(code = 406, condition = "Invalid IP Address or Port parameter passed."),
        @ResponseCode(code = 503, condition = "Connection Manager Service not available")} )
    public Node connect(
            @PathParam(value = "nodeType") String nodeType,
            @PathParam(value = "nodeId") String nodeId,
            @PathParam(value = "ipAddress") String ipAddress,
            @PathParam(value = "port") String port) {

        IConnectionManager connectionManager = getConnectionManager();
        if (connectionManager == null) {
            throw new ServiceUnavailableException("IConnectionManager not available.");
        }

        if (!NetUtils.isIPv4AddressValid(ipAddress)) {
            throw new NotAcceptableException("Invalid ip address "+ipAddress);
        }

        try {
            Integer.parseInt(port);
        } catch (Exception e) {
            throw new NotAcceptableException("Invalid Layer4 Port "+port);
        }

        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        params.put(ConnectionConstants.ADDRESS, ipAddress);
        params.put(ConnectionConstants.PORT, port);

        Node node = null;
        try {
            node = connectionManager.connect(nodeType, nodeId, params);
            if (node == null) {
                throw new ResourceNotFoundException("Failed to connect to Node at "+ipAddress+":"+port);
            }
            return node;
        } catch (Exception e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    /**
     * Create a Bridge.
     * <pre>
     * Example :
     * Request : POST http://localhost:8080/controller/nb/v2/networkconfig/bridgedomain/bridge/STUB/mgmt1/bridge1
     *</pre>
     * @param nodeType Node Type of the node with the management session.
     * @param nodeId Node Identifier of the node with the management session.
     * @param bridgeName Name / Identifier for a bridge to be created.
     */

   @Path("/bridge/{nodeType}/{nodeId}/{bridgeName}")
   @POST
   @StatusCodes( { @ResponseCode(code = 201, condition = "Bridge created successfully"),
       @ResponseCode(code = 404, condition = "Could not create Bridge"),
       @ResponseCode(code = 412, condition = "Failed to create Bridge due to an exception"),
       @ResponseCode(code = 503, condition = "Bridge Domain Configuration Service not available")} )

   public Response createBridge(
           @PathParam(value = "nodeType") String nodeType,
           @PathParam(value = "nodeId") String nodeId,
           @PathParam(value = "bridgeName") String name) {

       IBridgeDomainConfigService configurationService = getConfigurationService();
       if (configurationService == null) {
           throw new ServiceUnavailableException("IBridgeDomainConfigService not available.");
       }

       Node node = Node.fromString(nodeType, nodeId);
       Status status = null;
       try {
           status = configurationService.createBridgeDomain(node, name, null);
           if (status.getCode().equals(StatusCode.SUCCESS)) {
               return Response.status(Response.Status.CREATED).build();
           }
       } catch (Throwable t) {
           return Response.status(Response.Status.PRECONDITION_FAILED).build();
       }
       throw new ResourceNotFoundException(status.getDescription());
   }

   /**
    * Add a Port to a Bridge
    * <pre>
    * Example :
    * Request : POST http://localhost:8080/controller/nb/v2/networkconfig/bridgedomain/port/STUB/mgmt1/bridge1/port1
    *</pre>
    * @param nodeType Node Type of the node with the management session.
    * @param nodeId Node Identifier of the node with the management session.
    * @param bridgeName Name / Identifier of the bridge to which a Port is being added.
    * @param portName Name / Identifier of a Port that is being added to a bridge.
    */

   @Path("/port/{nodeType}/{nodeId}/{bridgeName}/{portName}")
   @POST
   @StatusCodes( { @ResponseCode(code = 201, condition = "Port added successfully"),
       @ResponseCode(code = 404, condition = "Could not add Port to the Bridge"),
       @ResponseCode(code = 412, condition = "Failed to add Port due to an exception"),
       @ResponseCode(code = 503, condition = "Bridge Domain Configuration Service not available")} )

   public Response addPort(
           @PathParam(value = "nodeType") String nodeType,
           @PathParam(value = "nodeId") String nodeId,
           @PathParam(value = "bridgeName") String bridge,
           @PathParam(value = "portName") String port) {

       IBridgeDomainConfigService configurationService = getConfigurationService();
       if (configurationService == null) {
           throw new ServiceUnavailableException("IBridgeDomainConfigService not available.");
       }

       Node node = Node.fromString(nodeType, nodeId);
       Status status = null;
       try {
           status = configurationService.addPort(node, bridge, port, null);
           if (status.getCode().equals(StatusCode.SUCCESS)) {
               return Response.status(Response.Status.CREATED).build();
           }
       } catch (Throwable t) {
           return Response.status(Response.Status.PRECONDITION_FAILED).build();
       }
       throw new ResourceNotFoundException(status.getDescription());
   }

   /**
    * Add a Port,Vlan to a Bridge
    * <pre>
    * Example :
    * Request : POST http://localhost:8080/controller/nb/v2/networkconfig/bridgedomain/port/STUB/mgmt1/bridge1/port2/200
    *</pre>
    * @param nodeType Node Type of the node with the management session.
    * @param nodeId Node Identifier of the node with the management session.
    * @param bridgeName Name / Identifier of the bridge to which a Port is being added.
    * @param portName Name / Identifier of a Port that is being added to a bridge.
    * @param vlan Vlan Id.
    */

   @Path("/port/{nodeType}/{nodeId}/{bridgeName}/{portName}/{vlan}")
   @POST
   @StatusCodes( { @ResponseCode(code = 201, condition = "Created Port with Vlan tag successfully"),
       @ResponseCode(code = 404, condition = "Could not add Port,Vlan to the Bridge"),
       @ResponseCode(code = 406, condition = "Invalid Vlan parameter passed."),
       @ResponseCode(code = 412, condition = "Failed to add Port,Vlan due to an exception"),
       @ResponseCode(code = 503, condition = "Bridge Domain Configuration Service not available")} )

   public Response addPort(
           @PathParam(value = "nodeType") String nodeType,
           @PathParam(value = "nodeId") String nodeId,
           @PathParam(value = "bridgeName") String bridge,
           @PathParam(value = "portName") String port,
           @PathParam(value = "vlan") String vlan) {

       IBridgeDomainConfigService configurationService = getConfigurationService();
       if (configurationService == null) {
           throw new ServiceUnavailableException("IBridgeDomainConfigService not available.");
       }
       try {
           Integer.parseInt(vlan);
       } catch (Exception e) {
           throw new NotAcceptableException("Incorrect Vlan :"+vlan);
       }

       Node node = Node.fromString(nodeType, nodeId);
       Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
       configs.put(ConfigConstants.TYPE, ConfigConstants.VLAN);
       configs.put(ConfigConstants.VLAN, vlan);

       Status status = null;
       try {
       status = configurationService.addPort(node, bridge, port, configs);
       if (status.getCode().equals(StatusCode.SUCCESS)) {
           return Response.status(Response.Status.CREATED).build();
       }
       } catch (Exception e) {
           return Response.status(Response.Status.PRECONDITION_FAILED).build();
       }
       throw new ResourceNotFoundException(status.getDescription());
   }
}
