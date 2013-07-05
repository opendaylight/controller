/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager.northbound;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.JAXBElement;

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
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.SubnetConfig;

/**
 * The class provides Northbound REST APIs to access the nodes, node connectors
 * and their properties.
 *
 */

@Path("/")
public class SwitchNorthbound {

    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        username = context.getUserPrincipal().getName();
    }

    protected String getUserName() {
        return username;
    }

    private ISwitchManager getIfSwitchManagerService(String containerName) {
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
                break;
            }
        }

        if (found == false) {
            throw new ResourceNotFoundException(containerName + " "
                    + RestMessages.NOCONTAINER.toString());
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);

        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return switchManager;
    }

    /**
     *
     * Retrieve a list of all the nodes and their properties in the network
     *
     * @param containerName
     *            The container for which we want to retrieve the list
     * @return A list of Pair each pair represents a
     *         {@link org.opendaylight.controller.sal.core.Node} and Set of
     *         {@link org.opendaylight.controller.sal.core.Property} attached to
     *         it.
     */
    @Path("/{containerName}/nodes")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Nodes.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Nodes getNodes(@PathParam("containerName") String containerName) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        List<NodeProperties> res = new ArrayList<NodeProperties>();
        Set<Node> nodes = switchManager.getNodes();
        if (nodes == null) {
            return null;
        }

        for (Node node : nodes) {
            Map<String, Property> propMap = switchManager.getNodeProps(node);
            if (propMap == null) {
                continue;
            }
            Set<Property> props = new HashSet<Property>(propMap.values());

            NodeProperties nodeProps = new NodeProperties(node, props);
            res.add(nodeProps);
        }

        return new Nodes(res);
    }

    /**
     * Add a Name/Tier property to a node
     *
     * @param containerName
     *            Name of the Container
     * @param nodeType
     *            Type of the node being programmed
     * @param nodeId
     *            Node Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @param propName
     *            Name of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @param propValue
     *            Value of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @return Response as dictated by the HTTP Response Status code
     */

    @Path("/{containerName}/node/{nodeType}/{nodeId}/property/{propName}/{propValue}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Response.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response addNodeProperty(
            @PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType,
            @PathParam("nodeId") String nodeId,
            @PathParam("propName") String propName,
            @PathParam("propValue") String propValue) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
        handleDefaultDisabled(containerName);

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        handleNodeAvailability(containerName, nodeType, nodeId);
        Node node = Node.fromString(nodeType, nodeId);

        Property prop = switchManager.createProperty(propName, propValue);
        if (prop == null) {
            throw new ResourceNotFoundException(
                    RestMessages.INVALIDDATA.toString());
        }

        switchManager.setNodeProp(node, prop);
        return Response.status(Response.Status.CREATED).build();
    }

    /**
     * Delete a property of a node
     *
     * @param containerName
     *            Name of the Container
     * @param nodeType
     *            Type of the node being programmed
     * @param nodeId
     *            Node Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @param propertyName
     *            Name of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @return Response as dictated by the HTTP Response Status code
     */

    @Path("/{containerName}/node/{nodeType}/{nodeId}/property/{propertyName}")
    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response deleteNodeProperty(
            @PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType,
            @PathParam("nodeId") String nodeId,
            @PathParam("propertyName") String propertyName) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
        handleDefaultDisabled(containerName);

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        handleNodeAvailability(containerName, nodeType, nodeId);
        Node node = Node.fromString(nodeType, nodeId);
        Status ret = switchManager.removeNodeProp(node, propertyName);
        if (ret.isSuccess()) {
            return Response.ok().build();
        }
        throw new ResourceNotFoundException(ret.getDescription());
    }

    /**
     *
     * Retrieve a list of all the node connectors and their properties in a
     * given node
     *
     * @param containerName
     *            The container for which we want to retrieve the list
     * @param nodeType
     *            Type of the node being programmed
     * @param nodeId
     *            Node Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @return A List of Pair each pair represents a
     *         {@link org.opendaylight.controller.sal.core.NodeConnector} and
     *         its corresponding
     *         {@link org.opendaylight.controller.sal.core.Property} attached to
     *         it.
     */
    @Path("/{containerName}/node/{nodeType}/{nodeId}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(NodeConnectors.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public NodeConnectors getNodeConnectors(
            @PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType,
            @PathParam("nodeId") String nodeId) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        handleNodeAvailability(containerName, nodeType, nodeId);
        Node node = Node.fromString(nodeType, nodeId);
        List<NodeConnectorProperties> res = new ArrayList<NodeConnectorProperties>();
        Set<NodeConnector> ncs = switchManager.getNodeConnectors(node);
        if (ncs == null) {
            return null;
        }

        for (NodeConnector nc : ncs) {
            Map<String, Property> propMap = switchManager
                    .getNodeConnectorProps(nc);
            if (propMap == null) {
                continue;
            }
            Set<Property> props = new HashSet<Property>(propMap.values());
            NodeConnectorProperties ncProps = new NodeConnectorProperties(nc,
                    props);
            res.add(ncProps);
        }

        return new NodeConnectors(res);
    }

    /**
     * Add a Name/Bandwidth property to a node connector
     *
     * @param containerName
     *            Name of the Container
     * @param nodeType
     *            Type of the node being programmed
     * @param nodeId
     *            Node Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @param nodeConnectorType
     *            Type of the node connector being programmed
     * @param nodeConnectorId
     *            NodeConnector Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @param propName
     *            Name of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @param propValue
     *            Value of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @return Response as dictated by the HTTP Response Status code
     */

    @Path("/{containerName}/nodeconnector/{nodeType}/{nodeId}/{nodeConnectorType}/{nodeConnectorId}/property/{propName}/{propValue}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response addNodeConnectorProperty(
            @PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType,
            @PathParam("nodeId") String nodeId,
            @PathParam("nodeConnectorType") String nodeConnectorType,
            @PathParam("nodeConnectorId") String nodeConnectorId,
            @PathParam("propName") String propName,
            @PathParam("propValue") String propValue) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }

        handleDefaultDisabled(containerName);

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        handleNodeAvailability(containerName, nodeType, nodeId);
        Node node = Node.fromString(nodeType, nodeId);

        handleNodeConnectorAvailability(containerName, node, nodeConnectorType,
                nodeConnectorId);
        NodeConnector nc = NodeConnector
                .fromStringNoNode(nodeConnectorType, nodeConnectorId, node);

        Property prop = switchManager.createProperty(propName, propValue);
        if (prop == null) {
            throw new ResourceNotFoundException(
                    RestMessages.INVALIDDATA.toString());
        }

        Status ret = switchManager.addNodeConnectorProp(nc, prop);
        if (ret.isSuccess()) {
            return Response.status(Response.Status.CREATED).build();
        }
        throw new InternalServerErrorException(ret.getDescription());
    }

    /**
     * Delete a property of a node connector
     *
     * @param containerName
     *            Name of the Container
     * @param nodeType
     *            Type of the node being programmed
     * @param nodeId
     *            Node Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @param nodeConnectorType
     *            Type of the node connector being programmed
     * @param nodeConnectorId
     *            NodeConnector Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @param propertyName
     *            Name of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @return Response as dictated by the HTTP Response Status code
     */

    @Path("/{containerName}/nodeconnector/{nodeType}/{nodeId}/{nodeConnectorType}/{nodeConnectorId}/property/{propertyName}")
    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response deleteNodeConnectorProperty(
            @PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType,
            @PathParam("nodeId") String nodeId,
            @PathParam("nodeConnectorType") String nodeConnectorType,
            @PathParam("nodeConnectorId") String nodeConnectorId,
            @PathParam("propertyName") String propertyName) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }

        handleDefaultDisabled(containerName);

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        handleNodeAvailability(containerName, nodeType, nodeId);
        Node node = Node.fromString(nodeType, nodeId);

        handleNodeConnectorAvailability(containerName, node, nodeConnectorType,
                nodeConnectorId);
        NodeConnector nc = NodeConnector
                .fromStringNoNode(nodeConnectorType, nodeConnectorId, node);
        Status ret = switchManager.removeNodeConnectorProp(nc, propertyName);
        if (ret.isSuccess()) {
            return Response.ok().build();
        }
        throw new ResourceNotFoundException(ret.getDescription());
    }

    /**
     * Returns the set of {@link SubnetConfig}s currently installed in the
     * specified container.
     *
     * @param containerName
     *            Name of the Container
     * @return List of {@link SubnetConfig}s
     */
    @Path("/{containerName}/subnets")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Subnets.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Subnets getSubnets(@PathParam("containerName") String containerName) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName,
                                          Privilege.READ, this)) {
            throw new UnauthorizedException(
                "User is not authorized to perform this operation on container " + containerName);
        }

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException(
                "Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        List<SubnetConfig> ret = switchManager.getSubnetsConfigList();

        if(ret == null){
            ret = new ArrayList<SubnetConfig>();
        }

        return new Subnets(ret);
    }

    /**
     * Add a subnet.
     *
     * @param containerName
     *            Name of the Container
     * @param subnetConfigData
     *            the {@link SubnetConfig} structure in JSON passed as a POST
     *            parameter
     * @return If the operation was successful
     */
    @Path("/{containerName}/subnets")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Response addSubnet(@PathParam("containerName") String containerName,
                              @TypeHint(SubnetConfig.class) JAXBElement<SubnetConfig> subnetConfigData) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName,
                                          Privilege.WRITE, this)) {
            throw new UnauthorizedException(
                "User is not authorized to perform this operation on container " + containerName);
        }

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException(
                "Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        SubnetConfig subnetConf = subnetConfigData.getValue();
        if(subnetConf.getNodePorts() == null){
            subnetConf = new SubnetConfig(subnetConf.getName(), subnetConf.getSubnet(), new ArrayList<String>());
        }

        Status s = switchManager.addSubnet(subnetConf);

        if(s.isSuccess()){
            return Response.status(Response.Status.CREATED).build();
        }else{
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    /**
     * Modify a subnet. For now only changing the port list is allowed.
     *
     * @param containerName
     *            Name of the Container
     * @param name
     *            Name of the SubnetConfig to be modified
     * @param subnetConfigData
     *            the {@link SubnetConfig} structure in JSON passed as a POST
     *            parameter
     * @return If the operation is successful or not
     */
    @Path("/{containerName}/subnets/{name}")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Response modifySubnet(@PathParam("containerName") String containerName,
                                 @PathParam("name") String name,
                                 @TypeHint(SubnetConfig.class) JAXBElement<SubnetConfig> subnetConfigData) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName,
                                          Privilege.WRITE, this)) {
            throw new UnauthorizedException(
                "User is not authorized to perform this operation on container " + containerName);
        }

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException(
                "Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        SubnetConfig subnetConf = subnetConfigData.getValue();
        SubnetConfig existingConf = switchManager.getSubnetConfig(name);

        boolean successful = true;

        // make sure that the name matches an existing subnet and we're not
        // changing the name or subnet IP/mask
        if (existingConf == null
            || !existingConf.getName().equals(subnetConf.getName())
            || !existingConf.getSubnet().equals(subnetConf.getSubnet())) {

            successful = false;

        }else{
            // create a set for fast lookups
            Set<String> newPorts = new HashSet<String>(subnetConf.getNodePorts());

            // go through the current ports and (1) remove ports that aren't
            // there anymore and (2) remove ports that are still there from the
            // set of ports to add
            for(String s : existingConf.getNodePorts()){
                if(newPorts.contains(s)){
                    newPorts.remove(s);
                }else{
                    Status st = switchManager.removePortsFromSubnet(name, s);
                    successful = successful && st.isSuccess();
                }
            }

            // add any remaining ports
            for(String s : newPorts){
                Status st = switchManager.addPortsToSubnet(name, s);
                successful = successful && st.isSuccess();
            }
        }

        return Response.status(Response.Status.ACCEPTED).build();
    }

    /**
     * Remove a subnet.
     *
     * @param containerName
     *            Name of the Container
     * @return List of {@link SubnetConfig}s
     */
    @Path("/{containerName}/subnets/{name}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Response removeSubnet(@PathParam("containerName") String containerName,
                                 @PathParam("name") String name) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName,
                                          Privilege.WRITE, this)) {
            throw new UnauthorizedException(
                "User is not authorized to perform this operation on container " + containerName);
        }

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException(
                "Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Status s = switchManager.removeSubnet(name);

        if(s.isSuccess()){
            return Response.status(Response.Status.ACCEPTED).build();
        }else{
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    /*    *//**
     * Retrieve a list of Span ports that were configured previously.
     *
     * @param containerName
     *            Name of the Container
     * @return list of
     *         {@link org.opendaylight.controller.switchmanager.SpanConfig}
     *         resources
     */
    /*
     * @Path("/span-config/{containerName}")
     *
     * @GET
     *
     * @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
     *
     * @StatusCodes( {
     *
     * @ResponseCode(code = 200, condition = "Operation successful"),
     *
     * @ResponseCode(code = 404, condition = "The containerName is not found"),
     *
     * @ResponseCode(code = 503, condition =
     * "One or more of Controller Services are unavailable") }) public
     * List<SpanConfig> getSpanConfigList(@PathParam("containerName") String
     * containerName) { ISwitchManager switchManager = (ISwitchManager)
     * getIfSwitchManagerService(containerName); if (switchManager == null) {
     * throw new ServiceUnavailableException("Switch Manager " +
     * RestMessages.SERVICEUNAVAILABLE.toString()); }
     *
     * return switchManager.getSpanConfigList(); }
     *//**
     * Add a span configuration
     *
     * @param containerName
     *            Name of the Container
     * @param config
     *            {@link org.opendaylight.controller.switchmanager.SpanConfig}
     *            in JSON or XML format
     * @return Response as dictated by the HTTP Response Status code
     */
    /*
     * @Path("/span-config/{containerName}")
     *
     * @PUT
     *
     * @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
     *
     * @StatusCodes( {
     *
     * @ResponseCode(code = 200, condition = "Operation successful"),
     *
     * @ResponseCode(code = 404, condition = "The containerName is not found"),
     *
     * @ResponseCode(code = 503, condition =
     * "One or more of Controller Services are unavailable") }) public Response
     * addSpanConfig(@PathParam("containerName") String containerName,
     *
     * @TypeHint(SubnetConfig.class) JAXBElement<SpanConfig> config) {
     * ISwitchManager switchManager = (ISwitchManager)
     * getIfSwitchManagerService(containerName); if (switchManager == null) {
     * throw new ServiceUnavailableException("Switch Manager " +
     * RestMessages.SERVICEUNAVAILABLE.toString()); }
     *
     * String ret = switchManager.addSpanConfig(config.getValue()); if
     * (ret.equals(ReturnString.SUCCESS.toString())) { return
     * Response.status(Response.Status.CREATED).build(); } throw new
     * InternalServerErrorException(ret); }
     *//**
     * Delete a span configuration
     *
     * @param containerName
     *            Name of the Container
     * @param config
     *            {@link org.opendaylight.controller.switchmanager.SpanConfig}
     *            in JSON or XML format
     * @return Response as dictated by the HTTP Response Status code
     */
    /*
     * @Path("/span-config/{containerName}")
     *
     * @DELETE
     *
     * @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
     *
     * @StatusCodes( {
     *
     * @ResponseCode(code = 200, condition = "Operation successful"),
     *
     * @ResponseCode(code = 404, condition = "The containerName is not found"),
     *
     * @ResponseCode(code = 503, condition =
     * "One or more of Controller Services are unavailable") }) public Response
     * deleteSpanConfig(@PathParam("containerName") String containerName,
     *
     * @TypeHint(SubnetConfig.class) JAXBElement<SpanConfig> config) {
     * ISwitchManager switchManager = (ISwitchManager)
     * getIfSwitchManagerService(containerName); if (switchManager == null) {
     * throw new ServiceUnavailableException("Switch Manager " +
     * RestMessages.SERVICEUNAVAILABLE.toString()); }
     *
     * String ret = switchManager.removeSpanConfig(config.getValue()); if
     * (ret.equals(ReturnString.SUCCESS.toString())) { return
     * Response.ok().build(); } throw new ResourceNotFoundException(ret); }
     */

    /**
     * Save the current switch configurations
     *
     * @param containerName
     *            Name of the Container
     * @return Response as dictated by the HTTP Response Status code
     */
    @Path("/{containerName}/switch-config")
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Response saveSwitchConfig(
            @PathParam("containerName") String containerName) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Status ret = switchManager.saveSwitchConfig();
        if (ret.isSuccess()) {
            return Response.ok().build();
        }
        throw new InternalServerErrorException(ret.getDescription());
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

    private void handleNodeConnectorAvailability(String containerName,
            Node node, String nodeConnectorType, String nodeConnectorId) {

        NodeConnector nc = NodeConnector.fromStringNoNode(nodeConnectorType,
                nodeConnectorId, node);
        if (nc == null) {
            throw new ResourceNotFoundException(nc + " : "
                    + RestMessages.NORESOURCE.toString());
        }

        ISwitchManager sm = (ISwitchManager) ServiceHelper.getInstance(
                ISwitchManager.class, containerName, this);

        if (sm == null) {
            throw new ServiceUnavailableException("Switch Manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        if (!sm.getNodeConnectors(node).contains(nc)) {
            throw new ResourceNotFoundException(nc.toString() + " : "
                    + RestMessages.NORESOURCE.toString());
        }
    }

}
