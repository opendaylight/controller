/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager.northbound;

import java.util.ArrayList;
import java.util.HashMap;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.query.QueryContext;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.SwitchConfig;

/**
 * The class provides Northbound REST APIs to access the nodes, node connectors
 * and their properties.
 *
 */

@Path("/")
public class SwitchNorthbound {

    private String username;
    private QueryContext queryContext;

    @Context
    public void setQueryContext(ContextResolver<QueryContext> queryCtxResolver) {
      if (queryCtxResolver != null) {
        queryContext = queryCtxResolver.getContext(QueryContext.class);
      }
    }

    @Context
    public void setSecurityContext(SecurityContext context) {
        if (context != null && context.getUserPrincipal() != null) {
            username = context.getUserPrincipal().getName();
        }
    }

    protected String getUserName() {
        return username;
    }

    private ISwitchManager getIfSwitchManagerService(String containerName) {
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

        if (found == false) {
            throw new ResourceNotFoundException(containerName + " " + RestMessages.NOCONTAINER.toString());
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);

        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return switchManager;
    }

    /**
     *
     * Retrieve a list of all the nodes and their properties in the network
     *
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @return A list of Pair each pair represents a
     *         {@link org.opendaylight.controller.sal.core.Node} and Set of
     *         {@link org.opendaylight.controller.sal.core.Property} attached to
     *         it.
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/switchmanager/default/nodes
     *
     * Response body in XML:
     * &lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
     * &lt;list&gt;
     *     &#x20;&#x20;&#x20;&lt;nodeProperties&gt;
     *         &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;node&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;id&gt;00:00:00:00:00:00:00:02&lt;/id&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;type&gt;OF&lt;/type&gt;
     *         &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/node&gt;
     *         &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;properties&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;tables&gt;
     *                 &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;value&gt;-1&lt;/value&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/tables&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;description&gt;
     *                 &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;value&gt;Switch2&lt;/value&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/description&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;actions&gt;
     *                 &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;value&gt;4095&lt;/value&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/actions&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;macAddress&gt;
     *                 &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;value&gt;00:00:00:00:00:02&lt;/value&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/macAddress&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;capabilities&gt;
     *                 &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;value&gt;199&lt;/value&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/capabilities&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;timeStamp&gt;
     *                 &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;value&gt;1377291227877&lt;/value&gt;
     *                 &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;name&gt;connectedSince&lt;/name&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/timeStamp&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;buffers&gt;
     *                 &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;value&gt;256&lt;/value&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/buffers&gt;
     *         &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/properties&gt;
     *     &#x20;&#x20;&#x20;&lt;/nodeProperties&gt;
     * &lt;/list&gt;
     *
     * Response body in JSON:
     * {
     *    "nodeProperties":[
     *       {
     *          "node":{
     *             "id":"00:00:00:00:00:00:00:02",
     *             "type":"OF"
     *          },
     *          "properties":{
     *             "tables":{
     *                "value":"-1"
     *             },
     *             "description":{
     *                "value":"None"
     *             },
     *             "actions":{
     *                "value":"4095"
     *             },
     *             "macAddress":{
     *                "value":"00:00:00:00:00:02"
     *             },
     *             "capabilities":{
     *                "value":"199"
     *             },
     *             "timeStamp":{
     *                "value":"1377291227877",
     *                "name":"connectedSince"
     *             },
     *             "buffers":{
     *                "value":"256"
     *             }
     *          }
     *       }
     *    ]
     * }
     *
     * </pre>
     */
    @Path("/{containerName}/nodes")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Nodes.class)
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable"),
        @ResponseCode(code = 400, condition = "Incorrect query syntex") })
    public Nodes getNodes(@PathParam("containerName") String containerName, @QueryParam("_q") String queryString) {

        if (!isValidContainer(containerName)) {
            throw new ResourceNotFoundException("Container " + containerName + " does not exist.");
        }

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        List<NodeProperties> res = new ArrayList<NodeProperties>();
        Set<Node> nodes = switchManager.getNodes();
        if (nodes == null) {
            return new Nodes(res);
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
        Nodes result = new Nodes(res);
        if (queryString != null) {
            queryContext.createQuery(queryString, Nodes.class)
                .filter(result, NodeProperties.class);
        }
        return result;
    }

    /**
     * Add a Description, Tier and Forwarding mode property to a node. This
     * method returns a non-successful response if a node by that name already
     * exists.
     *
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @param nodeType
     *            Type of the node being programmed (Eg. 'OF')
     * @param nodeId
     *            Node Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.Node} (Eg.
     *            '00:00:00:00:00:00:00:03')
     * @param propertyName
     *            Name of the Property. Properties that can be configured are:
     *            description, forwarding(only for default container) and tier
     * @param propertyValue
     *            Value of the Property. Description can be any string (Eg.
     *            'Node1'), valid values for tier are non negative numbers, and
     *            valid values for forwarding are 0 for reactive and 1 for
     *            proactive forwarding.
     * @return Response as dictated by the HTTP Response Status code
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/switchmanager/default/node/OF/00:00:00:00:00:00:00:03/property/description/Switch3
     *
     * </pre>
     */

    @Path("/{containerName}/node/{nodeType}/{nodeId}/property/{propertyName}/{propertyValue}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Response.class)
    @StatusCodes({
        @ResponseCode(code = 201, condition = "Operation successful"),
        @ResponseCode(code = 400, condition = "The nodeId or configuration is invalid"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The Container Name or node or configuration name is not found"),
        @ResponseCode(code = 406, condition = "The property cannot be configured in non-default container"),
        @ResponseCode(code = 409, condition = "Unable to update configuration due to cluster conflict or conflicting description property"),
        @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response addNodeProperty(@Context UriInfo uriInfo, @PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType, @PathParam("nodeId") String nodeId,
            @PathParam("propertyName") String propertyName, @PathParam("propertyValue") String propertyValue) {

        if (!isValidContainer(containerName)) {
            throw new ResourceNotFoundException("Container " + containerName + " does not exist.");
        }
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        handleNodeAvailability(containerName, nodeType, nodeId);
        Node node = Node.fromString(nodeType, nodeId);
        Property prop = switchManager.createProperty(propertyName, propertyValue);
        if (prop == null) {
            throw new ResourceNotFoundException("Property with name " + propertyName + " does not exist.");
        }
        SwitchConfig switchConfig = switchManager.getSwitchConfig(node.toString());
        Map<String, Property> nodeProperties = (switchConfig == null) ? new HashMap<String, Property>()
                : new HashMap<String, Property>(switchConfig.getNodeProperties());
        nodeProperties.put(prop.getName(), prop);
        SwitchConfig newSwitchConfig = new SwitchConfig(node.toString(), nodeProperties);
        Status status = switchManager.updateNodeConfig(newSwitchConfig);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Property " + propertyName, username, "updated",
                    "of Node " + NorthboundUtils.getNodeDesc(node, switchManager), containerName);

            return Response.created(uriInfo.getRequestUri()).build();
        }
        return NorthboundUtils.getResponse(status);
    }

    /**
     * Delete a property of a node
     *
     * @param containerName
     *            Name of the Container (Eg. 'SliceRed')
     * @param nodeType
     *            Type of the node being programmed (Eg. 'OF')
     * @param nodeId
     *            Node Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.Node} (Eg.
     *            '00:00:00:00:00:03:01:02')
     * @param propertyName
     *            Name of the Property. Properties that can be deleted are
     *            description, forwarding(only in default container) and tier.
     * @return Response as dictated by the HTTP Response Status code
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/switchmanager/default/node/OF/00:00:00:00:00:00:00:03/property/forwarding
     *
     * </pre>
     */

    @Path("/{containerName}/node/{nodeType}/{nodeId}/property/{propertyName}")
    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 204, condition = "Property removed successfully"),
        @ResponseCode(code = 400, condition = "The nodeId or configuration is invalid"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
        @ResponseCode(code = 409, condition = "Unable to delete property due to cluster conflict"),
        @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response deleteNodeProperty(@PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType, @PathParam("nodeId") String nodeId,
            @PathParam("propertyName") String propertyName) {

        if (!isValidContainer(containerName)) {
            throw new ResourceNotFoundException("Container " + containerName + " does not exist.");
        }
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        handleNodeAvailability(containerName, nodeType, nodeId);
        Node node = Node.fromString(nodeType, nodeId);

        SwitchConfig switchConfig = switchManager.getSwitchConfig(node.toString());
        Status status;
        if (switchConfig == null) {
            status = new Status(StatusCode.NOTFOUND, "Switch Configuration does not exist");
        } else {
            Map<String, Property> nodeProperties = new HashMap<String, Property>(switchConfig.getNodeProperties());
            if (!nodeProperties.containsKey(propertyName.toLowerCase())) {
                String msg = "Property " + propertyName + " does not exist or not configured for switch " + nodeId;
                status = new Status(StatusCode.NOTFOUND, msg);
            } else {
                nodeProperties.remove(propertyName.toLowerCase());
                SwitchConfig newSwitchConfig = new SwitchConfig(node.toString(), nodeProperties);
                status = switchManager.updateNodeConfig(newSwitchConfig);
                if (status.isSuccess()) {
                    NorthboundUtils.auditlog("Property " + propertyName, username, "removed", "of Node "
                            + NorthboundUtils.getNodeDesc(node, switchManager), containerName);
                    return Response.noContent().build();
                }
            }
        }
        return NorthboundUtils.getResponse(status);
    }

    /**
     * Get a property of a node
     *
     * @param containerName
     *            Name of the Container (Eg. 'SliceRed')
     * @param nodeType
     *            Type of the node being programmed (Eg. 'OF')
     * @param nodeId
     *            Node Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.Node} (Eg.
     *            '00:00:00:00:00:03:01:02')
     * @param propertyName
     *            Name of the Property. Properties that can be deleted are
     *            description, forwarding(only in default container) and tier.
     * @return Property value of the property
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/switchmanager/default/node/OF/00:00:00:00:00:00:00:01/property/description
     *
     * Response body in XML
     * &lt;description&gt;
     *     &#x20;&#x20;&lt;value&gt;switch1&lt;/value&gt;
     * &lt;/description&gt;
     *
     * Response body in JSON
     * {
     *     &#x20;&#x20;"value": "switch1"
     * }
     * </pre>
     */

    @Path("/{containerName}/node/{nodeType}/{nodeId}/property/{propertyName}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(String.class)
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Property getNodeProperty(@PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType, @PathParam("nodeId") String nodeId,
            @PathParam("propertyName") String propertyName) {

        if (!isValidContainer(containerName)) {
            throw new ResourceNotFoundException("Container " + containerName + " does not exist.");
        }
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        handleNodeAvailability(containerName, nodeType, nodeId);
        Node node = Node.fromString(nodeType, nodeId);
        if (node == null) {
            throw new ResourceNotFoundException(nodeId + " : " + RestMessages.NONODE.toString());
        }
        SwitchConfig switchConfig = switchManager.getSwitchConfig(node.toString());
        if (switchConfig == null) {
            throw new ResourceNotFoundException(nodeId + " : " + "Config Not Found" );
        } else {
            Map<String, Property> nodeProperties = new HashMap<String, Property>(switchConfig.getNodeProperties());
            if (!nodeProperties.containsKey(propertyName.toLowerCase())) {
                String msg = "Property " + propertyName + " does not exist or not "
                        + "configured for switch " + nodeId;
                throw new ResourceNotFoundException(msg);
            } else {
                return nodeProperties.get(propertyName.toLowerCase());
            }
        }
    }

    /**
     *
     * Retrieve a list of all the nodeconnectors and their properties in a given
     * node
     *
     * @param containerName
     *            The container for which we want to retrieve the list (Eg.
     *            'default')
     * @param nodeType
     *            Type of the node being programmed (Eg. 'OF')
     * @param nodeId
     *            Node Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.Node} (Eg.
     *            '00:00:00:00:00:00:00:03')
     * @return A List of Pair each pair represents a
     *         {@link org.opendaylight.controller.sal.core.NodeConnector} and
     *         its corresponding
     *         {@link org.opendaylight.controller.sal.core.Property} attached to
     *         it.
     *
     * <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/switchmanager/default/node/OF/00:00:00:00:00:00:00:01
     *
     * Response body in XML:
     * &lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
     * &lt;list&gt;
     *     &#x20;&#x20;&#x20;&lt;nodeConnectorProperties&gt;
     *         &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;nodeconnector&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;node&gt;
     *                 &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;id&gt;00:00:00:00:00:00:00:01&lt;/id&gt;
     *                 &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;type&gt;OF&lt;/type&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/node&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;id&gt;2&lt;/id&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;type&gt;OF&lt;/type&gt;
     *         &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/nodeconnector&gt;
     *         &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;properties&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;state&gt;
     *                 &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;value&gt;1&lt;/value&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/state&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;config&gt;
     *                 &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;value&gt;1&lt;/value&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/config&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;name&gt;
     *                 &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;value&gt;L1_2-C2_1&lt;/value&gt;
     *             &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/name&gt;
     *         &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/properties&gt;
     *     &#x20;&#x20;&#x20;&lt;/nodeConnectorProperties&gt;
     * &lt;/list&gt;
     *
     * Response body in JSON:
     * {
     *    "nodeConnectorProperties":[
     *       {
     *          "nodeconnector":{
     *             "node":{
     *                "id":"00:00:00:00:00:00:00:01",
     *                "type":"OF"
     *             },
     *             "id":"2",
     *             "type":"OF"
     *          },
     *          "properties":{
     *             "state":{
     *                "value":"1"
     *             },
     *             "config":{
     *                "value":"1"
     *             },
     *             "name":{
     *                "value":"L1_2-C2_1"
     *             }
     *          }
     *       }
     *    ]
     * }
     *
     * </pre>
     */
    @Path("/{containerName}/node/{nodeType}/{nodeId}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(NodeConnectors.class)
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable"),
        @ResponseCode(code = 400, condition = "Incorrect query syntex") })
    public NodeConnectors getNodeConnectors(@PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType, @PathParam("nodeId") String nodeId,
            @QueryParam("_q") String queryString) {

        if (!isValidContainer(containerName)) {
            throw new ResourceNotFoundException("Container " + containerName + " does not exist.");
        }
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        handleNodeAvailability(containerName, nodeType, nodeId);
        Node node = Node.fromString(nodeType, nodeId);
        List<NodeConnectorProperties> res = new ArrayList<NodeConnectorProperties>();
        Set<NodeConnector> ncs = switchManager.getNodeConnectors(node);
        if (ncs == null) {
            return null;
        }

        for (NodeConnector nc : ncs) {
            Map<String, Property> propMap = switchManager.getNodeConnectorProps(nc);
            if (propMap == null) {
                continue;
            }
            Set<Property> props = new HashSet<Property>(propMap.values());
            NodeConnectorProperties ncProps = new NodeConnectorProperties(nc, props);
            res.add(ncProps);
        }
        NodeConnectors result = new NodeConnectors(res);
        if (queryString != null) {
            queryContext.createQuery(queryString, NodeConnectors.class)
                .filter(result, NodeConnectorProperties.class);
        }
        return result;
    }

    /**
     * Add node-connector property to a node connector. This method returns a
     * non-successful response if a node connector by the given name already
     * exists.
     *
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @param nodeType
     *            Type of the node being programmed (Eg. 'OF')
     * @param nodeId
     *            Node Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.Node} (Eg.
     *            '00:00:00:00:00:00:00:03')
     * @param nodeConnectorType
     *            Type of the node connector being programmed (Eg. 'OF')
     * @param nodeConnectorId
     *            NodeConnector Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.NodeConnector}.
     *            (Eg. '2'). If nodeConnecterId contains forward slash(/),
     *            replace forward slash with underscore(_) in the URL. (Eg. for
     *            Ethernet1/2, use Ethernet1_2)
     * @param propertyName
     *            Name of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes Property that can be configured is bandwidth
     * @param propertyValue
     *            Value of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @return Response as dictated by the HTTP Response Status code
     *
     * <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/switchmanager/default/nodeconnector/OF/00:00:00:00:00:00:00:01/OF/2/property/bandwidth/1
     *
     * </pre>
     */

    @Path("/{containerName}/nodeconnector/{nodeType}/{nodeId}/{nodeConnectorType}/{nodeConnectorId}/property/{propertyName}/{propertyValue}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 201, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
        @ResponseCode(code = 409, condition = "Unable to add property due to cluster conflict"),
        @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response addNodeConnectorProperty(@Context UriInfo uriInfo,
            @PathParam("containerName") String containerName, @PathParam("nodeType") String nodeType,
            @PathParam("nodeId") String nodeId, @PathParam("nodeConnectorType") String nodeConnectorType,
            @PathParam("nodeConnectorId") String nodeConnectorId, @PathParam("propertyName") String propertyName,
            @PathParam("propertyValue") String propertyValue) {

        if (!isValidContainer(containerName)) {
            throw new ResourceNotFoundException("Container " + containerName + " does not exist.");
        }
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        handleNodeAvailability(containerName, nodeType, nodeId);
        Node node = Node.fromString(nodeType, nodeId);

        if (nodeConnectorId.contains("_")) {
            nodeConnectorId = nodeConnectorId.replace("_", "/");
        }

        handleNodeConnectorAvailability(containerName, node, nodeConnectorType, nodeConnectorId);
        NodeConnector nc = NodeConnector.fromStringNoNode(nodeConnectorType, nodeConnectorId, node);

        Property prop = switchManager.createProperty(propertyName, propertyValue);
        if (prop == null) {
            throw new ResourceNotFoundException(RestMessages.INVALIDDATA.toString());
        }

        Status ret = switchManager.addNodeConnectorProp(nc, prop);
        if (ret.isSuccess()) {
            NorthboundUtils.auditlog("Property " + propertyName, username, "updated", "of Node Connector "
                    + NorthboundUtils.getPortName(nc, switchManager), containerName);
            return Response.created(uriInfo.getRequestUri()).build();
        }
        throw new InternalServerErrorException(ret.getDescription());
    }

    /**
     * Delete a property of a node connector
     *
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @param nodeType
     *            Type of the node being programmed (Eg. 'OF')
     * @param nodeId
     *            Node Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.Node} (Eg.
     *            '00:00:00:00:00:00:00:01')
     * @param nodeConnectorType
     *            Type of the node connector being programmed (Eg. 'OF')
     * @param nodeConnectorId
     *            NodeConnector Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.NodeConnector}
     *            (Eg. '1'). If nodeConnecterId contains forward slash(/),
     *            replace forward slash with underscore(_) in the URL. (Eg. for
     *            Ethernet1/2, use Ethernet1_2)
     * @param propertyName
     *            Name of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes. Property that can be deleted is bandwidth
     * @return Response as dictated by the HTTP Response Status code
     *
     * <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/switchmanager/default/nodeconnector/OF/00:00:00:00:00:00:00:01/OF/2/property/bandwidth
     *
     * </pre>
     */

    @Path("/{containerName}/nodeconnector/{nodeType}/{nodeId}/{nodeConnectorType}/{nodeConnectorId}/property/{propertyName}")
    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 204, condition = "Property removed successfully"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response deleteNodeConnectorProperty(@PathParam("containerName") String containerName,
            @PathParam("nodeType") String nodeType, @PathParam("nodeId") String nodeId,
            @PathParam("nodeConnectorType") String nodeConnectorType,
            @PathParam("nodeConnectorId") String nodeConnectorId, @PathParam("propertyName") String propertyName) {

        if (!isValidContainer(containerName)) {
            throw new ResourceNotFoundException("Container " + containerName + " does not exist.");
        }
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        handleNodeAvailability(containerName, nodeType, nodeId);
        Node node = Node.fromString(nodeType, nodeId);

        if (nodeConnectorId.contains("_")) {
            nodeConnectorId = nodeConnectorId.replace("_", "/");
        }

        handleNodeConnectorAvailability(containerName, node, nodeConnectorType, nodeConnectorId);
        NodeConnector nc = NodeConnector.fromStringNoNode(nodeConnectorType, nodeConnectorId, node);
        Status ret = switchManager.removeNodeConnectorProp(nc, propertyName);
        if (ret.isSuccess()) {
            NorthboundUtils.auditlog("Property " + propertyName, username, "removed", "of Node Connector "
                    + NorthboundUtils.getPortName(nc, switchManager), containerName);
            return Response.noContent().build();
        }
        throw new ResourceNotFoundException(ret.getDescription());
    }

    /**
     * Save the current switch configurations
     *
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @return Response as dictated by the HTTP Response Status code
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/switchmanager/default/save
     *
     * </pre>
     */
    @Path("/{containerName}/save")
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 500, condition = "Failed to save switch configuration. Failure Reason included in HTTP Error response"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Response saveSwitchConfig(@PathParam("containerName") String containerName) {

        if (!isValidContainer(containerName)) {
            throw new ResourceNotFoundException("Container " + containerName + " does not exist.");
        }
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Status ret = switchManager.saveSwitchConfig();
        if (ret.isSuccess()) {
            return Response.ok().build();
        }
        throw new InternalServerErrorException(ret.getDescription());
    }

    private Node handleNodeAvailability(String containerName, String nodeType, String nodeId) {

        Node node = Node.fromString(nodeType, nodeId);
        if (node == null) {
            throw new ResourceNotFoundException(nodeId + " : " + RestMessages.NONODE.toString());
        }

        ISwitchManager sm = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName, this);

        if (sm == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        if (!sm.getNodes().contains(node)) {
            throw new ResourceNotFoundException(node.toString() + " : " + RestMessages.NONODE.toString());
        }
        return node;
    }

    private void handleNodeConnectorAvailability(String containerName, Node node, String nodeConnectorType,
            String nodeConnectorId) {

        NodeConnector nc = NodeConnector.fromStringNoNode(nodeConnectorType, nodeConnectorId, node);
        if (nc == null) {
            throw new ResourceNotFoundException(nc + " : " + RestMessages.NORESOURCE.toString());
        }

        ISwitchManager sm = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName, this);

        if (sm == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        if (!sm.getNodeConnectors(node).contains(nc)) {
            throw new ResourceNotFoundException(nc.toString() + " : " + RestMessages.NORESOURCE.toString());
        }
    }

    private boolean isValidContainer(String containerName) {
        if (containerName.equals(GlobalConstants.DEFAULT.toString())) {
            return true;
        }
        IContainerManager containerManager = (IContainerManager) ServiceHelper.getGlobalInstance(
                IContainerManager.class, this);
        if (containerManager == null) {
            throw new InternalServerErrorException(RestMessages.INTERNALERROR.toString());
        }
        if (containerManager.getContainerNames().contains(containerName)) {
            return true;
        }
        return false;
    }

}
