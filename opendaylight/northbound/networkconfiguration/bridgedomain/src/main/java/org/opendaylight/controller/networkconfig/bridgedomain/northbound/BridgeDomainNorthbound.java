
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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.opendaylight.controller.connectionmanager.IConnectionManager;
import org.opendaylight.controller.northbound.commons.exception.NotAcceptableException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.IBridgeDomainConfigService;
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
        if (context != null && context.getUserPrincipal() != null) username = context.getUserPrincipal().getName();
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
     * Create a Bridge.
     * <pre>
     *
     * Example :
     *
     * Request :
     * http://localhost:8080/controller/nb/v2/networkconfig/bridgedomain/bridge/STUB/mgmt1/bridge1
     *
     *</pre>
     * @param nodeType Node Type of the node with the management session.
     * @param nodeId Node Identifier of the node with the management session.
     * @param bridgeName Name / Identifier for a bridge to be created.
     * @param bridgeConfigs Additional Bridge Configurations.
     *        It takes in complex structures under the ConfigConstants.CUSTOM key.
     *        The use-cases are documented under wiki.opendaylight.org project pages:
     *        https://wiki.opendaylight.org/view/OVSDB_Integration:Mininet_OVSDB_Tutorial
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
           @PathParam(value = "bridgeName") String name,
           Map<String, Object> bridgeConfigs) {

       IBridgeDomainConfigService configurationService = getConfigurationService();
       if (configurationService == null) {
           throw new ServiceUnavailableException("IBridgeDomainConfigService not available.");
       }

       Node node = Node.fromString(nodeType, nodeId);
       Status status = null;
       try {
           Map<ConfigConstants, Object> configs = this.buildConfig(bridgeConfigs);
           status = configurationService.createBridgeDomain(node, name, configs);
           if (status.getCode().equals(StatusCode.SUCCESS)) {
               return Response.status(Response.Status.CREATED).build();
           }
       } catch (Throwable t) {
           return Response.status(Response.Status.PRECONDITION_FAILED).build();
       }
       throw new ResourceNotFoundException(status.getDescription());
   }


   /**
    * Remove a Bridge.
    * <pre>
    *
    * Example :
    *
    * Request :
    * DELETE
    * http://localhost:8080/controller/nb/v2/networkconfig/bridgedomain/bridge/STUB/mgmt1/bridge1
    *
    *</pre>
    * @param nodeType Node Type of the node with the management session.
    * @param nodeId Node Identifier of the node with the management session.
    * @param bridgeName Name / Identifier for a bridge to be deleted.
    */

  @Path("/bridge/{nodeType}/{nodeId}/{bridgeName}")
  @DELETE
  @StatusCodes( { @ResponseCode(code = 200, condition = "Bridge deleted successfully"),
      @ResponseCode(code = 404, condition = "Could not delete Bridge"),
      @ResponseCode(code = 412, condition = "Failed to delete Bridge due to an exception"),
      @ResponseCode(code = 503, condition = "Bridge Domain Configuration Service not available")} )

  public Response deleteBridge(
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
          status = configurationService.deleteBridgeDomain(node, name);
          if (status.getCode().equals(StatusCode.SUCCESS)) {
              return Response.status(Response.Status.OK).build();
          }
      } catch (Throwable t) {
          return Response.status(Response.Status.PRECONDITION_FAILED).build();
      }
      throw new ResourceNotFoundException(status.getDescription());
  }

   /**
    * Add a Port to a Bridge
    * <pre>
    *
    * Example :
    *
    * Request :
    * http://localhost:8080/controller/nb/v2/networkconfig/bridgedomain/port/STUB/mgmt1/bridge1/port1
    *
    *</pre>
    * @param nodeType Node Type of the node with the management session.
    * @param nodeId Node Identifier of the node with the management session.
    * @param bridgeName Name / Identifier of the bridge to which a Port is being added.
    * @param portName Name / Identifier of a Port that is being added to a bridge.
    * @param portConfigs Additional Port Configurations.
    *        It takes in complex structures under the ConfigConstants.CUSTOM key.
    *        The use-cases are documented under wiki.opendaylight.org project pages :
    *        https://wiki.opendaylight.org/view/OVSDB_Integration:Mininet_OVSDB_Tutorial
    */

   @Path("/port/{nodeType}/{nodeId}/{bridgeName}/{portName}")
   @POST
   @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
   @StatusCodes( { @ResponseCode(code = 201, condition = "Port added successfully"),
       @ResponseCode(code = 404, condition = "Could not add Port to the Bridge"),
       @ResponseCode(code = 412, condition = "Failed to add Port due to an exception"),
       @ResponseCode(code = 503, condition = "Bridge Domain Configuration Service not available")} )

   public Response addPort(
           @PathParam(value = "nodeType") String nodeType,
           @PathParam(value = "nodeId") String nodeId,
           @PathParam(value = "bridgeName") String bridge,
           @PathParam(value = "portName") String port,
           Map<String, Object> portConfigs) {

       IBridgeDomainConfigService configurationService = getConfigurationService();
       if (configurationService == null) {
           throw new ServiceUnavailableException("IBridgeDomainConfigService not available.");
       }

       Node node = Node.fromString(nodeType, nodeId);
       Status status = null;
       try {
           Map<ConfigConstants, Object> configs = this.buildConfig(portConfigs);
           status = configurationService.addPort(node, bridge, port, configs);
           if (status.getCode().equals(StatusCode.SUCCESS)) {
               return Response.status(Response.Status.CREATED).build();
           }
       } catch (Throwable t) {
           return Response.status(Response.Status.PRECONDITION_FAILED).build();
       }
       throw new ResourceNotFoundException(status.getDescription());
   }

   /**
    * Remove a Port from a Bridge
    * <pre>
    *
    * Example :
    *
    * Request :
    * DELETE
    * http://localhost:8080/controller/nb/v2/networkconfig/bridgedomain/port/STUB/mgmt1/bridge1/port1
    *
    *</pre>
    * @param nodeType Node Type of the node with the management session.
    * @param nodeId Node Identifier of the node with the management session.
    * @param bridgeName Name / Identifier of the bridge to which a Port is being added.
    * @param portName Name / Identifier of a Port that is being deleted from a bridge.
    */

   @Path("/port/{nodeType}/{nodeId}/{bridgeName}/{portName}")
   @DELETE
   @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
   @StatusCodes( { @ResponseCode(code = 200, condition = "Port deleted successfully"),
       @ResponseCode(code = 404, condition = "Could not delete Port to the Bridge"),
       @ResponseCode(code = 412, condition = "Failed to delete Port due to an exception"),
       @ResponseCode(code = 503, condition = "Bridge Domain Configuration Service not available")} )

   public Response deletePort(
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
           status = configurationService.deletePort(node, bridge, port);
           if (status.getCode().equals(StatusCode.SUCCESS)) {
               return Response.status(Response.Status.OK).build();
           }
       } catch (Throwable t) {
           return Response.status(Response.Status.PRECONDITION_FAILED).build();
       }
       throw new ResourceNotFoundException(status.getDescription());
   }

   private Map<ConfigConstants, Object> buildConfig(Map<String, Object> rawConfigs) {
       if (rawConfigs == null) return null;
       Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
       for (String key : rawConfigs.keySet()) {
           ConfigConstants cc = ConfigConstants.valueOf(key.toUpperCase());
           configs.put(cc, rawConfigs.get(key));
       }
       return configs;
   }
/**
    * Add a Port,Vlan to a Bridge
    * <pre>
    *
    * Example :
    * Request :
    * http://localhost:8080/controller/nb/v2/networkconfig/bridgedomain/port/STUB/mgmt1/bridge1/port2/200
    *
    * </pre>
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
       configs.put(ConfigConstants.TYPE, ConfigConstants.VLAN.name());
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
