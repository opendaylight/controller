
/*
 * Copyright (c) 2013 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.connectionmanager.northbound;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.connectionmanager.IConnectionManager;
import org.opendaylight.controller.northbound.commons.exception.NotAcceptableException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;

/**
 * Connection Manager Northbound APIs
 */
@Path("/")
public class ConnectionManagerNorthbound {
    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        if (context != null && context.getUserPrincipal() != null) username = context.getUserPrincipal().getName();
    }
    protected String getUserName() {
        return username;
    }

    private IConnectionManager getConnectionManager() {
        return (IConnectionManager) ServiceHelper
                .getGlobalInstance(IConnectionManager.class, this);
    }

    /**
     *
     * Retrieve a list of all the nodes connected to a given controller in the cluster.
     *
     * @param controllerAddress Optional parameter to retrieve the nodes connected to another
     *        controller in the cluster
     * @return A list of Nodes {@link org.opendaylight.controller.sal.core.Node}
     *
     * <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/connectionmanager/nodes?controller=1.1.1.1
     *
     * Response body in XML:
     *  &lt;list&gt;
     *       &lt;node&gt;
     *           &lt;id&gt;00:00:00:00:00:00:00:52&lt;/id&gt;
     *           &lt;type&gt;OF&lt;/type&gt;
     *       &lt;/node&gt;
     *       &lt;node&gt;
     *           &lt;id&gt;00:00:00:00:00:00:00:3e&lt;/id&gt;
     *           &lt;type&gt;OF&lt;/type&gt;
     *       &lt;/node&gt;
     *   &lt;/list&gt;
     *
     *  Response body in JSON:
     *  {
     *       "node": [
     *           {
     *               "type": "OF",
     *               "id": "00:00:00:00:00:00:00:52"
     *           },
     *           {
     *               "type": "OF",
     *               "id": "00:00:00:00:00:00:00:3e"
     *           }
     *       ]
     *   }
     * </pre>
     */
    @Path("/nodes")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Nodes.class)
    @StatusCodes( {
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 406, condition = "Invalid Controller IP Address passed."),
        @ResponseCode(code = 503, condition = "Connection Manager Service not available")})

    public Nodes getNodes(@DefaultValue("") @QueryParam("controller") String controllerAddress) {
        if (!NorthboundUtils.isAuthorized(getUserName(), "default", Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container");
        }

        IConnectionManager connectionManager = getConnectionManager();
        if (connectionManager == null) {
            throw new ServiceUnavailableException("IConnectionManager not available.");
        }

        if ((controllerAddress != null) && (controllerAddress.trim().length() > 0) &&
            !NetUtils.isIPv4AddressValid(controllerAddress)) {
            throw new NotAcceptableException("Invalid ip address "+controllerAddress);
        }
        Set<Node> nodeSet = null;

        if (controllerAddress != null) {
            try {
                nodeSet = connectionManager.getNodes(InetAddress.getByName(controllerAddress));
            } catch (UnknownHostException e) {
                throw new NotAcceptableException("Invalid ip address "+controllerAddress);
            }
        } else {
            nodeSet = connectionManager.getLocalNodes();
        }
        return new Nodes(nodeSet);
    }

    /**
     * If a Network Configuration Service needs a Management Connection and if the
     * Node Type is unknown, use this REST api to connect to the management session.
     * <pre>
     *
     * Example :
     *
     * Request :
     * PUT http://localhost:8080/controller/nb/v2/connectionmanager/node/mgmt1/address/1.1.1.1/port/6634
     *
     * Response :
     * Node :
     * xml :
     * &lt;node&gt;
     *    &lt;id&gt;mgmt1&lt;/id&gt;
     *    &lt;type&gt;STUB&lt;/type&gt;
     * &lt;/node&gt;
     *
     * json:
     * {"id": "mgmt1","type": "STUB"}
     *
     *</pre>
     * @param nodeId User-Defined name of the node to connect with. This can be any alpha numeric value
     * @param ipAddress IP Address of the Node to connect with.
     * @param port Layer4 Port of the management session to connect with.
     * @return Node If the connection is successful, HTTP 404 otherwise.
     */

    @Path("/node/{nodeId}/address/{ipAddress}/port/{port}/")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Node.class)
    @StatusCodes( {
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "Could not connect to the Node with the specified parameters"),
        @ResponseCode(code = 406, condition = "Invalid IP Address or Port parameter passed."),
        @ResponseCode(code = 503, condition = "Connection Manager Service not available")} )
    public Node connect(
            @PathParam(value = "nodeId") String nodeId,
            @PathParam(value = "ipAddress") String ipAddress,
            @PathParam(value = "port") String port) {

        if (!NorthboundUtils.isAuthorized(getUserName(), "default", Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container");
        }

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
            node = connectionManager.connect(nodeId, params);
            if (node == null) {
                throw new ResourceNotFoundException("Failed to connect to Node at "+ipAddress+":"+port);
            }
            return node;
        } catch (Exception e) {
            throw new ResourceNotFoundException("Failed to connect to Node with Exception "+e.getMessage());
        }
    }

    /**
     * If a Network Configuration Service needs a Management Connection, and if the
     * node Type is known, the user can choose to use this REST api to connect to the management session.
     * <pre>
     *
     * Example :
     *
     * Request :
     * PUT http://localhost:8080/controller/nb/v2/connectionmanager/node/STUB/mgmt1/address/1.1.1.1/port/6634
     *
     * Response : Node :
     * xml :
     * &lt;node&gt;
     *    &lt;id&gt;mgmt1&lt;/id&gt;
     *    &lt;type&gt;STUB&lt;/type&gt;
     * &lt;/node&gt;
     *
     * json:
     * {"id": "mgmt1","type": "STUB"}
     *
     *</pre>
     * @param nodeType Type of the Node the connection is made for.
     * @param nodeId User-Defined name of the node to connect with. This can be any alpha numeric value
     * @param ipAddress IP Address of the Node to connect with.
     * @param port Layer4 Port of the management session to connect with.
     * @return Node If the connection is successful, HTTP 404 otherwise.
     */

    @Path("/node/{nodeType}/{nodeId}/address/{ipAddress}/port/{port}/")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Node.class)
    @StatusCodes( {
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "Could not connect to the Node with the specified parameters"),
        @ResponseCode(code = 406, condition = "Invalid IP Address or Port parameter passed."),
        @ResponseCode(code = 503, condition = "Connection Manager Service not available")} )
    public Node connect(
            @PathParam(value = "nodeType") String nodeType,
            @PathParam(value = "nodeId") String nodeId,
            @PathParam(value = "ipAddress") String ipAddress,
            @PathParam(value = "port") String port) {

        if (!NorthboundUtils.isAuthorized(getUserName(), "default", Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container");
        }

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
     * Disconnect an existing Connection.
     * <pre>
     *
     * Example :
     *
     * Request :
     * DELETE http://localhost:8080/controller/nb/v2/connectionmanager/node/STUB/mgmt1
     *
     *</pre>
     * @param nodeType Type of the Node
     * @param nodeId Connection's NodeId.
     */

    @Path("/node/{nodeType}/{nodeId}/")
    @DELETE
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Response.class)
    @StatusCodes( {
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 200, condition = "Node disconnected successfully"),
        @ResponseCode(code = 404, condition = "Could not find a connection with the specified Node identifier"),
        @ResponseCode(code = 503, condition = "Connection Manager Service not available")} )
    public Response disconnect(
            @PathParam(value = "nodeType") String nodeType,
            @PathParam(value = "nodeId") String nodeId) {

        if (!NorthboundUtils.isAuthorized(getUserName(), "default", Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container");
        }
        IConnectionManager connectionManager = getConnectionManager();
        if (connectionManager == null) {
            throw new ServiceUnavailableException("IConnectionManager not available.");
        }

        try {
            Node node = new Node(nodeType, nodeId);
            Status status = connectionManager.disconnect(node);
            if (status.isSuccess()) {
                return Response.ok().build();
            }
            return NorthboundUtils.getResponse(status);
        } catch (Exception e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }
}
