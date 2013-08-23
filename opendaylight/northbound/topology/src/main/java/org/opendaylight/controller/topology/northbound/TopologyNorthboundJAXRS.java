/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.topology.northbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.TopologyUserLinkConfig;

/**
 * Topology Northbound REST API
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
 */

@Path("/")
public class TopologyNorthboundJAXRS {

    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        username = context.getUserPrincipal().getName();
    }

    protected String getUserName() {
        return username;
    }

    /**
     *
     * Retrieve the Topology
     *
     * @param containerName
     *            The container for which we want to retrieve the topology (Eg. 'default')
     *
     * @return A List of EdgeProps each EdgeProp represent an Edge of the grap
     *         with the corresponding properties attached to it.
     *
     * <pre>
     *
     * Example:
     *
     * RequestURL:
     * http://localhost:8080/controller/nb/v2/topology/default
     *
     * Response in XML:
     * &lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;&lt;topology&gt;&lt;edgeProperties&gt;&lt;edge&gt;&lt;tailNodeConnector id="2" type="OF"&gt;&lt;node id="00:00:00:00:00:00:00:02" type="OF"/&gt;&lt;/tailNodeConnector&gt;&lt;headNodeConnector id="2" type="OF"&gt;&lt;node id="00:00:00:00:00:00:00:51" type="OF"/&gt;&lt;/headNodeConnector&gt;&lt;/edge&gt;&lt;properties&gt;&lt;state&gt;&lt;value&gt;1&lt;/value&gt;&lt;/state&gt;&lt;config&gt;&lt;value&gt;1&lt;/value&gt;&lt;/config&gt;&lt;name&gt;&lt;value&gt;C1_2-L2_2&lt;/value&gt;&lt;/name&gt;&lt;timeStamp&gt;&lt;value&gt;1377279422032&lt;/value&gt;&lt;name&gt;creation&lt;/name&gt;&lt;/timeStamp&gt;&lt;/properties&gt;&lt;/edgeProperties&gt;&lt;edgeProperties&gt;&lt;edge&gt;&lt;tailNodeConnector id="2" type="OF"&gt;&lt;node id="00:00:00:00:00:00:00:51" type="OF"/&gt;&lt;/tailNodeConnector&gt;&lt;headNodeConnector id="2" type="OF"&gt;&lt;node id="00:00:00:00:00:00:00:02" type="OF"/&gt;&lt;/headNodeConnector&gt;&lt;/edge&gt;&lt;properties&gt;&lt;state&gt;&lt;value&gt;1&lt;/value&gt;&lt;/state&gt;&lt;name&gt;&lt;value&gt;L2_2-C1_2&lt;/value&gt;&lt;/name&gt;&lt;config&gt;&lt;value&gt;1&lt;/value&gt;&lt;/config&gt;&lt;timeStamp&gt;&lt;value&gt;1377279423564&lt;/value&gt;&lt;name&gt;creation&lt;/name&gt;&lt;/timeStamp&gt;&lt;/properties&gt;&lt;/edgeProperties&gt;&lt;/topology&gt;
     *
     * Response in JSON:
     * {"edgeProperties":[{"edge":{"tailNodeConnector":{"@id":"2","@type":"OF","node":{"@id":"00:00:00:00:00:00:00:02","@type":"OF"}},"headNodeConnector":{"@id":"2","@type":"OF","node":{"@id":"00:00:00:00:00:00:00:51","@type":"OF"}}},"properties":{"timeStamp":{"value":"1377278961017","name":"creation"}}},{"edge":{"tailNodeConnector":{"@id":"2","@type":"OF","node":{"@id":"00:00:00:00:00:00:00:51","@type":"OF"}},"headNodeConnector":{"@id":"2","@type":"OF","node":{"@id":"00:00:00:00:00:00:00:02","@type":"OF"}}},"properties":{"timeStamp":{"value":"1377278961018","name":"creation"}}}]}
     *
     * </pre>
     */
    @Path("/{containerName}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Topology.class)
    @StatusCodes({ @ResponseCode(code = 404, condition = "The Container Name was not found") })
    public Topology getTopology(@PathParam("containerName") String containerName) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
        ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
                .getInstance(ITopologyManager.class, containerName, this);
        if (topologyManager == null) {
            throw new ResourceNotFoundException(
                    RestMessages.NOCONTAINER.toString());
        }

        Map<Edge, Set<Property>> topo = topologyManager.getEdges();
        if (topo != null) {
            List<EdgeProperties> res = new ArrayList<EdgeProperties>();
            for (Map.Entry<Edge, Set<Property>> entry : topo.entrySet()) {
                EdgeProperties el = new EdgeProperties(entry.getKey(),
                        entry.getValue());
                res.add(el);
            }
            return new Topology(res);
        }

        return null;
    }

    /**
     * Retrieve the user configured links
     *
     * @param containerName
     *            The container for which we want to retrieve the user links (Eg. 'default')
     *
     * @return A List of user configured links
     *
     * <pre>
     *
     * Example:
     *
     * RequestURL:
     * http://localhost:8080/controller/nb/v2/topology/default/user-link
     *
     * Response in XML:
     * &lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;&lt;topologyUserLinks&gt;&lt;userLinks&gt;&lt;status&gt;Success&lt;/status&gt;&lt;name&gt;link1&lt;/name&gt;&lt;srcNodeConnector&gt;OF|2@OF|00:00:00:00:00:00:00:02&lt;/srcNodeConnector&gt;&lt;dstNodeConnector&gt;OF|2@OF|00:00:00:00:00:00:00:51&lt;/dstNodeConnector&gt;&lt;/userLinks&gt;&lt;/topologyUserLinks&gt;
     *
     * Response in JSON:
     * {"userLinks":{"status":"Success","name":"link1","srcNodeConnector":"OF|2@OF|00:00:00:00:00:00:00:02","dstNodeConnector":"OF|2@OF|00:00:00:00:00:00:00:51"}}
     *
     * </pre>
     */
    @Path("/{containerName}/user-link")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(TopologyUserLinks.class)
    @StatusCodes({ @ResponseCode(code = 404, condition = "The Container Name was not found") })
    public TopologyUserLinks getUserLinks(
            @PathParam("containerName") String containerName) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
        ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
                .getInstance(ITopologyManager.class, containerName, this);
        if (topologyManager == null) {
            throw new ResourceNotFoundException(
                    RestMessages.NOCONTAINER.toString());
        }

        ConcurrentMap<String, TopologyUserLinkConfig> userLinks = topologyManager
                .getUserLinks();
        if ((userLinks != null) && (userLinks.values() != null)) {
            List<TopologyUserLinkConfig> res = new ArrayList<TopologyUserLinkConfig>(
                    userLinks.values());
            return new TopologyUserLinks(res);
        }

        return null;
    }

    /**
     * Add an User Link
     *
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @param TopologyUserLinkConfig
     *            in JSON or XML format
     * @return Response as dictated by the HTTP Response Status code
     *
     * <pre>
     *
     * Example:
     *
     * RequestURL:
     * http://localhost:8080/controller/nb/v2/topology/default/user-link/config1-content
     *
     * Request in XML:
     * &lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;&lt;topologyUserLinks&gt;&lt;status&gt;Success&lt;/status&gt;&lt;name&gt;link1&lt;/name&gt;&lt;srcNodeConnector&gt;OF|2@OF|00:00:00:00:00:00:00:02&lt;/srcNodeConnector&gt;&lt;dstNodeConnector&gt;OF|2@OF|00:00:00:00:00:00:00:51&lt;/dstNodeConnector&gt;&lt;/topologyUserLinks&gt;
     *
     * Request in JSON:
     * {"status":"Success","name":"link1","srcNodeConnector":"OF|2@OF|00:00:00:00:00:00:00:02","dstNodeConnector":"OF|2@OF|00:00:00:00:00:00:00:51"}
     *
     * </pre>
     */
    @Path("/{containerName}/user-link")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 201, condition = "User Link added successfully"),
            @ResponseCode(code = 404, condition = "The Container Name was not found"),
            @ResponseCode(code = 409, condition = "Failed to add User Link due to Conflicting Name"),
            @ResponseCode(code = 500, condition = "Failed to add User Link. Failure Reason included in HTTP Error response"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response addUserLink(
            @PathParam(value = "containerName") String containerName,
            @TypeHint(TopologyUserLinkConfig.class) JAXBElement<TopologyUserLinkConfig> userLinkConfig) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
        ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
                .getInstance(ITopologyManager.class, containerName, this);
        if (topologyManager == null) {
            throw new ResourceNotFoundException(
                    RestMessages.NOCONTAINER.toString());
        }

        Status status = topologyManager.addUserLink(userLinkConfig.getValue());
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("User Link", username, "added", userLinkConfig.getValue().getName(), containerName);
            return Response.status(Response.Status.CREATED).build();
        }
        throw new InternalServerErrorException(status.getDescription());
    }

    /**
     * Delete an User Link
     *
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @param name
     *            Name of the Link Configuration (Eg. 'config1')
     * @return Response as dictated by the HTTP Response Status code
     *
     * <pre>
     *
     * Example:
     *
     * RequestURL:
     * http://localhost:8080/controller/nb/v2/topology/default/user-link/config1
     *
     * </pre>
     */
    @Path("/{containerName}/user-link/{name}")
    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or Link Configuration Name was not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response deleteUserLink(
            @PathParam("containerName") String containerName,
            @PathParam("name") String name) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
        ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
                .getInstance(ITopologyManager.class, containerName, this);
        if (topologyManager == null) {
            throw new ResourceNotFoundException(
                    RestMessages.NOCONTAINER.toString());
        }

        Status ret = topologyManager.deleteUserLink(name);
        if (ret.isSuccess()) {
            NorthboundUtils.auditlog("User Link", username, "removed", name, containerName);
            return Response.ok().build();
        }
        throw new ResourceNotFoundException(ret.getDescription());
    }
}
