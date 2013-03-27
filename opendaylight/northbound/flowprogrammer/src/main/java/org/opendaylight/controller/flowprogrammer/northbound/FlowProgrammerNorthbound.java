
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.flowprogrammer.northbound;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
import org.opendaylight.controller.northbound.commons.exception.NotAcceptableException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.switchmanager.ISwitchManager;

/**
 * Flow Configuration Northbound API
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
public class FlowProgrammerNorthbound {

    private IForwardingRulesManager getForwardingRulesManagerService(
            String containerName) {
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

        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper
                .getInstance(IForwardingRulesManager.class, containerName, this);

        if (frm == null) {
            throw new ServiceUnavailableException("Flow Programmer "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return frm;
    }

    private List<FlowConfig> getStaticFlowsInternal(String containerName,
            Node node) {
        IForwardingRulesManager frm = getForwardingRulesManagerService(containerName);

        if (frm == null) {
            throw new ServiceUnavailableException("Flow Programmer "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        List<FlowConfig> flows = new ArrayList<FlowConfig>();

        if (node == null) {
            for (FlowConfig flow : frm.getStaticFlows()) {
                flows.add(flow);
            }
        } else {
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

            for (FlowConfig flow : frm.getStaticFlows(node)) {
                flows.add(flow);
            }
        }
        return flows;
    }

    /**
     * Returns a list of Flows configured on the given container
     *
     * @param containerName Name of the Container. The Container name for the base controller is "default".
     * @return List of configured flows configured on a given container
     */
    @Path("/{containerName}")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(FlowConfigs.class)
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public FlowConfigs getStaticFlows(
            @PathParam("containerName") String containerName) {
        List<FlowConfig> flowConfigs = getStaticFlowsInternal(containerName, null);
        return new FlowConfigs(flowConfigs);
    }

    /**
     * Returns a list of Flows configured on a Node in a given container
     *
     * @param containerName Name of the Container. The Container name
     * for the base controller is "default".
     * @param nodeType Type of the node being programmed
     * @param nodeId Node Identifier
     * @return List of configured flows configured on a Node in a container
     */
    @Path("/{containerName}/{nodeType}/{nodeId}")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(FlowConfigs.class)
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName or nodeId is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public FlowConfigs getStaticFlows(
            @PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType,
            @PathParam("nodeId") String nodeId) {
        Node node = Node.fromString(nodeType, nodeId);
        if (node == null) {
            throw new ResourceNotFoundException(nodeId + " : "
                    + RestMessages.NONODE.toString());
        }
        List<FlowConfig> flows = getStaticFlowsInternal(containerName, node);
        return new FlowConfigs(flows);
    }

    /**
     * Returns the flow configuration matching a human-readable name and nodeId on a
     * given Container.
     *
     * @param containerName Name of the Container. The Container name
     * for the base controller is "default".
     * @param nodeType Type of the node being programmed
     * @param nodeId Node Identifier
     * @param name Human-readable name for the configured flow.
     * @return Flow configuration matching the name and nodeId on a Container
     */
    @Path("/{containerName}/{nodeType}/{nodeId}/{name}")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(FlowConfig.class)
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName or NodeId or Configuration name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public FlowConfig getStaticFlow(
            @PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType,
            @PathParam("nodeId") String nodeId,
            @PathParam("name") String name) {
        IForwardingRulesManager frm = getForwardingRulesManagerService(containerName);

        if (frm == null) {
            throw new ServiceUnavailableException("Flow Programmer "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = handleNodeAvailability(containerName, nodeType, nodeId);

        FlowConfig staticFlow = frm.getStaticFlow(name, node);
        if (staticFlow == null) {
            throw new ResourceNotFoundException(RestMessages.NOFLOW.toString());
        }

        return new FlowConfig(staticFlow);
    }

    /**
     * Add a flow configuration
     *
     * @param containerName Name of the Container. The Container name
     * for the base controller is "default".
     * @param nodeType Type of the node being programmed
     * @param nodeId Node Identifier
     * @param name Name of the Static Flow configuration
     * @param FlowConfig Flow Configuration in JSON or XML format
     * @return Response as dictated by the HTTP Response Status code
     */

    @Path("/{containerName}/{nodeType}/{nodeId}/{name}")
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
            @ResponseCode(code = 201, condition = "Flow Config processed successfully"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
            @ResponseCode(code = 409, condition = "Failed to create Static Flow entry due to Conflicting Name"),
            @ResponseCode(code = 500, condition = "Failed to create Static Flow entry. Failure Reason included in HTTP Error response"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response addFlow(
            @PathParam(value = "containerName") String containerName,
            @PathParam(value = "name") String name,
            @PathParam("nodeType") String nodeType,
            @PathParam(value = "nodeId") String nodeId,
            @TypeHint(FlowConfig.class) JAXBElement<FlowConfig> flowConfig) {

        handleDefaultDisabled(containerName);

        IForwardingRulesManager frm = getForwardingRulesManagerService(containerName);

        if (frm == null) {
            throw new ServiceUnavailableException("Flow Programmer "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = handleNodeAvailability(containerName, nodeType, nodeId);

        FlowConfig staticFlow = frm.getStaticFlow(name, node);
        if (staticFlow != null) {
            throw new ResourceConflictException(name + " already exists."
                    + RestMessages.RESOURCECONFLICT.toString());
        }

        Status status = frm.addStaticFlow(flowConfig.getValue(), false);
        if (status.isSuccess()) {
            return Response.status(Response.Status.CREATED).build();
        }
        throw new InternalServerErrorException(status.getDescription());
    }

    /**
     * Delete a Flow configuration
     *
     * DELETE /flows/{containerName}/{nodeType}/{nodeId}/{name}
     *
     * @param containerName Name of the Container. The Container name
     * for the base controller is "default".
     * @param nodeType Type of the node being programmed
     * @param nodeId Node Identifier
     * @param name Name of the Static Flow configuration
     * @return Response as dictated by the HTTP Response code
     */

    @Path("/{containerName}/{nodeType}/{nodeId}/{name}")
    @DELETE
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Flow Config deleted successfully"),
            @ResponseCode(code = 404, condition = "The Container Name or Node-id or Flow Name passed is not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
            @ResponseCode(code = 500, condition = "Failed to delete Flow config. Failure Reason included in HTTP Error response"),
            @ResponseCode(code = 503, condition = "One or more of Controller service is unavailable") })
    public Response deleteFlow(
            @PathParam(value = "containerName") String containerName,
            @PathParam(value = "name") String name,
            @PathParam("nodeType") String nodeType,
            @PathParam(value = "nodeId") String nodeId) {

        handleDefaultDisabled(containerName);

        IForwardingRulesManager frm = getForwardingRulesManagerService(containerName);

        if (frm == null) {
            throw new ServiceUnavailableException("Flow Programmer "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = handleNodeAvailability(containerName, nodeType, nodeId);

        FlowConfig staticFlow = frm.getStaticFlow(name, node);
        if (staticFlow == null) {
            throw new ResourceNotFoundException(name + " : "
                    + RestMessages.NOFLOW.toString());
        }

        Status status = frm.removeStaticFlow(name, node);
        if (status.isSuccess()) {
            return Response.ok().build();
        }
        throw new InternalServerErrorException(status.getDescription());
    }

    /**
     * Toggle a Flow configuration
     *
     * @param containerName Name of the Container. The Container name
     * for the base controller is "default".
     * @param nodeType Type of the node being programmed
     * @param nodeId Node Identifier
     * @param name Name of the Static Flow configuration
     * @return Response as dictated by the HTTP Response code
     */

    @Path("/{containerName}/{nodeType}/{nodeId}/{name}")
    @PUT
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Flow Config deleted successfully"),
            @ResponseCode(code = 404, condition = "The Container Name or Node-id or Flow Name passed is not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
            @ResponseCode(code = 500, condition = "Failed to delete Flow config. Failure Reason included in HTTP Error response"),
            @ResponseCode(code = 503, condition = "One or more of Controller service is unavailable") })
    public Response toggleFlow(
            @PathParam(value = "containerName") String containerName,
            @PathParam("nodeType") String nodeType,
            @PathParam(value = "nodeId") String nodeId,
            @PathParam(value = "name") String name) {

        handleDefaultDisabled(containerName);

        IForwardingRulesManager frm = getForwardingRulesManagerService(containerName);

        if (frm == null) {
            throw new ServiceUnavailableException("Flow Programmer "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = handleNodeAvailability(containerName, nodeType, nodeId);

        FlowConfig staticFlow = frm.getStaticFlow(name, node);
        if (staticFlow == null) {
            throw new ResourceNotFoundException(name + " : "
                    + RestMessages.NOFLOW.toString());
        }

        Status status = frm.toggleStaticFlowStatus(staticFlow);
        if (status.isSuccess()) {
            return Response.ok().build();
        }
        throw new InternalServerErrorException(status.getDescription());
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

    private void handleDefaultDisabled(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            throw new InternalServerErrorException(RestMessages.INTERNALERROR
                    .toString());
        }
        if (containerName.equals(GlobalConstants.DEFAULT.toString())
                && containerManager.hasNonDefaultContainer()) {
            throw new NotAcceptableException(RestMessages.DEFAULTDISABLED
                    .toString());
        }
    }

}
