
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.containermanager.northbound;

import java.util.ArrayList;
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
import org.opendaylight.controller.containermanager.ContainerConfig;
import org.opendaylight.controller.containermanager.ContainerFlowConfig;
import org.opendaylight.controller.containermanager.IContainerAuthorization;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.BadRequestException;
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ResourceForbiddenException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.usermanager.IUserManager;

/**
 * Container Manager Northbound API
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
public class ContainerManagerNorthbound {
    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        if (context != null && context.getUserPrincipal() != null) {
            username = context.getUserPrincipal().getName();
        }
    }

    protected String getUserName() {
        return username;
    }

    private IContainerManager getContainerManager() {
        IContainerManager containerMgr = (IContainerManager) ServiceHelper.getGlobalInstance(IContainerManager.class, this);
        if (containerMgr == null) {
            throw new InternalServerErrorException(RestMessages.INTERNALERROR.toString());
        }
        return containerMgr;
    }

    private void handleNameMismatch(String name, String nameinURL) {
        if (name == null || nameinURL == null) {
            throw new BadRequestException(RestMessages.INVALIDJSON.toString());
        }

        if (name.equalsIgnoreCase(nameinURL)) {
            return;
        }
        throw new BadRequestException(RestMessages.INVALIDJSON.toString());
    }



    /**
     * Get all the containers configured in the system
     *
     * @return a List of all {@link org.opendaylight.controller.containermanager.ContainerConfig}
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/containermanager/containers
     *
     * Response body in XML:
     * &lt;containerConfig-list&gt;
     *    &#x20;&#x20;&#x20;&lt;containerConfig&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;container&gt;black&lt;/container&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;staticVlan&gt;10&lt;/staticVlan&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;nodeConnectors&gt;OF|1@OF|00:00:00:00:00:00:00:01&lt;/nodeConnectors&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;nodeConnectors&gt;OF|23@OF|00:00:00:00:00:00:20:21&lt;/nodeConnectors&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;flowSpecs&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;name&gt;tcp&lt;/name&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;protocol&gt;TCP&lt;/protocol&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/flowSpecs&gt;
     *    &#x20;&#x20;&#x20;&#x20;&lt;/containerConfig&gt;
     *    &#x20;&#x20;&#x20;&#x20;&lt;containerConfig&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;container&gt;red&lt;/container&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;staticVlan&gt;20&lt;/staticVlan&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;nodeConnectors&gt;OF|1@OF|00:00:00:00:00:00:00:01&lt;/nodeConnectors&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;nodeConnectors&gt;OF|23@OF|00:00:00:00:00:00:20:21&lt;/nodeConnectors&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;flowSpecs&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;name&gt;udp&lt;/name&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;protocol&gt;UDP&lt;/protocol&gt;
     *    &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;/flowSpecs&gt;
     *  &#x20;&#x20;&#x20;&#x20;&lt;/containerConfig&gt;
     * &lt;/containerConfig-list&gt;
     *
     * Response body in JSON:
     * { "containerConfig" : [
     *     { "container" : "black",
     *       "nodeConnectors" : [
     *          "OF|1@OF|00:00:00:00:00:00:00:01", "OF|23@OF|00:00:00:00:00:00:20:21"
     *       ],
     *       "staticVlan" : "10",
     *       "flowSpecs : [
     *          { "name": "udp",
     *            "protocol": "UDP" }
     *       ]
     *     },
     *     { "container" : "red",
     *       "nodeConnectors" : [
     *          "OF|1@OF|00:00:00:00:00:00:00:01",
     *          "OF|23@OF|00:00:00:00:00:00:20:21"
     *       ],
     *       "staticVlan" : "20",
     *       "flowSpecs": [
     *          { "name": "tcp",
     *            "protocol": "TCP"
     *          }
     *       ]
     *     }
     *   ]
     * }
     * </pre>
     */
    @Path("/containers")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(ContainerConfigs.class)
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "User is not authorized to perform this operation"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public ContainerConfigs viewAllContainers() {

        handleNetworkAuthorization(getUserName());

        IContainerManager containerManager = getContainerManager();

        return new ContainerConfigs(containerManager.getContainerConfigList());
    }

    /**
     * Get the container configuration for container name requested
     *
     * @param container
     *            name of the Container (eg. blue)
     * @return a List of {@link org.opendaylight.controller.containermanager.ContainerConfig}
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/containermanager/container/blue
     *
     * Response body in XML:
     * &lt;containerConfig&gt;
     *  &#x20;&#x20;&#x20;&#x20;&lt;container&gt;blue&lt;/container&gt;
     *  &#x20;&#x20;&#x20;&#x20;&lt;staticVlan&gt;10&lt;/staticVlan&gt;
     *  &#x20;&#x20;&#x20;&#x20;&lt;nodeConnectors&gt;OF|1@OF|00:00:00:00:00:00:00:01&lt;/nodeConnectors&gt;
     *  &#x20;&#x20;&#x20;&#x20;&lt;nodeConnectors&gt;OF|23@OF|00:00:00:00:00:00:20:21&lt;/nodeConnectors&gt;
     * &lt;/containerConfig&gt;
     *
     * Response body in JSON:
     * {
     *    "containerConfig": [
     *       {
     *        "container": "yellow",
     *        "staticVlan": "10",
     *        "nodeConnectors": [
     *           "OF|1@OF|00:00:00:00:00:00:00:01",
     *           "OF|2@OF|00:00:00:00:00:00:00:02"
     *        ],
     *        "flowSpecs": []
     *       }
     *    ]
     * }
     * </pre>
     */
    @Path("/container/{container}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(ContainerConfig.class)
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "User is not authorized to perform this operation"),
            @ResponseCode(code = 403, condition = "Operation forbidden on default"),
            @ResponseCode(code = 404, condition = "The container is not found") })
    public ContainerConfigs viewContainer(@PathParam(value = "container") String container) {

        handleContainerAuthorization(container, getUserName());
        handleForbiddenOnDefault(container);

        handleContainerNotExists(container);

        IContainerManager containerManager = getContainerManager();
        List<ContainerConfig> containerConfigs = new ArrayList<ContainerConfig>();
        containerConfigs.add(containerManager.getContainerConfig(container));
        return new ContainerConfigs(containerConfigs);
    }

    /**
     * Create a container
     *
     * @param uriInfo
     * @param container
     *            name of the Container (eg. yellow)
     * @param containerConfig
     *            details of the container as specified by:
     *            {@link org.opendaylight.controller.containermanager.ContainerConfig}
     * @return Response as dictated by the HTTP Response Status code
     *
     * <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/containermanager/container/yellow
     *
     * Request body in XML:
     * &lt;containerConfig&gt;
     *   &#x20;&#x20;&#x20;&#x20;&lt;container&gt;yellow&lt;/container&gt;
     *   &#x20;&#x20;&#x20;&#x20;&lt;staticVlan&gt;10&lt;/staticVlan&gt;
     *   &#x20;&#x20;&#x20;&#x20;&lt;nodeConnectors&gt;&lt;/nodeConnectors&gt;
     * &lt;/containerConfig&gt;
     *
     * Request body in JSON:
     * {
     *    "container" : "yellow",
     *    "nodeConnectors" : [
     *       "OF|1@OF|00:00:00:00:00:00:00:01",
     *       "OF|23@OF|00:00:00:00:00:00:20:21"
     *    ],
     *    "staticVlan" : "10"
     * }
     *
     * </pre>
     */
    @Path("/container/{container}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 201, condition = "Container created successfully"),
            @ResponseCode(code = 400, condition = "Invalid Container configuration."),
            @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
            @ResponseCode(code = 403, condition = "Operation forbidden on default"),
            @ResponseCode(code = 404, condition = "Container Name is not found"),
            @ResponseCode(code = 409, condition = "Failed to create Container due to Conflicting Name"),
            @ResponseCode(code = 500, condition = "Failure Reason included in HTTP Error response") })
    public Response createContainer(@Context UriInfo uriInfo,
            @PathParam(value = "container") String container,
            @TypeHint(ContainerConfig.class) ContainerConfig containerConfig) {

        handleAdminAuthorization(getUserName());
        handleContainerExists(container);

        handleNameMismatch(containerConfig.getContainerName(), container);
        handleForbiddenOnDefault(container);

        IContainerManager containerManager = getContainerManager();
        Status status = containerManager.addContainer(containerConfig);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Container", username, "added", container);
            return Response.created(uriInfo.getRequestUri()).build();
        }
        return NorthboundUtils.getResponse(status);
    }

    /**
     * Delete a container
     *
     * @param container
     *            name of the Container (eg. green)
     * @return Response as dictated by the HTTP Response code
     *
     * <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/containermanager/container/green
     *
     * </pre>
     */
    @Path("/container/{container}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 204, condition = "Container deleted successfully"),
            @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
            @ResponseCode(code = 403, condition = "Operation forbidden on default"),
            @ResponseCode(code = 404, condition = "The container is not found") })
    public Response removeContainer(@PathParam(value = "container") String container) {

        handleAdminAuthorization(getUserName());
        handleForbiddenOnDefault(container);
        handleContainerNotExists(container);
        IContainerManager containerManager = getContainerManager();
        Status status = containerManager.removeContainer(container);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Container", username, "removed", container);
            return Response.noContent().build();
        }
        return NorthboundUtils.getResponse(status);
    }

    /**
     * Get flowspec within a given container
     *
     * @param container
     *            name of the Container (eg. green)
     * @param name
     *            name of the flowspec (eg. ssh)
     * @return flowspec detail as specified by:
     *         {@link org.opendaylight.controller.containermanager.ContainerFlowConfig}
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/containermanager/container/green/flowspec/ssh
     *
     * Response body in XML:
     * &lt;flow-spec-config&gt;
     *  &#x20;&#x20;&#x20;&#x20;&lt;name&gt;ssh&lt;/name&gt;
     *  &#x20;&#x20;&#x20;&#x20;&lt;dlVlan&gt;52&lt;/dlVlan&gt;
     *  &#x20;&#x20;&#x20;&#x20;&lt;nwSrc&gt;10.0.0.101&lt;/nwSrc&gt;
     *  &#x20;&#x20;&#x20;&#x20;&lt;nwDst&gt;10.0.0.102&lt;/nwDst&gt;
     *  &#x20;&#x20;&#x20;&#x20;&lt;protocol&gt;IPv4&lt;/protocol&gt;
     *  &#x20;&#x20;&#x20;&#x20;&lt;tpSrc&gt;80&lt;/tpSrc&gt;
     *  &#x20;&#x20;&#x20;&#x20;&lt;tpDst&gt;100&lt;/tpDst&gt;
     * &lt;/flow-spec-config&gt;
     *
     * Response body in JSON:
     * {
     *    "protocol" : "IPv4",
     *    "dlVlan" : "52",
     *    "nwDst" : "10.0.0.102",
     *    "name" : "ssh",
     *    "nwSrc" : "10.0.0.101",
     *    "tpSrc" : "80",
     *    "tpDst" : "100"
     * }
     *
     * </pre>
     */
    @Path("/container/{container}/flowspec/{flowspec}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(ContainerFlowConfig.class)
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
            @ResponseCode(code = 404, condition = "The container is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public ContainerFlowConfig viewContainerFlowSpec(@PathParam(value = "container") String container,
            @PathParam(value = "flowspec") String flowspec) {

        handleContainerAuthorization(container, getUserName());
        handleForbiddenOnDefault(container);

        handleContainerNotExists(container);
        IContainerManager containerManager = getContainerManager();
        List<ContainerFlowConfig> flowSpecs = containerManager.getContainerFlows(container);

        for (ContainerFlowConfig containerFlowConfig : flowSpecs) {
            if (containerFlowConfig.equalsByName(flowspec)) {
                return containerFlowConfig;
            }
        }
        throw new ResourceNotFoundException("Flow Spec not found");
    }

    /**
     * Get all the flowspec in a given container
     *
     * @param container
     *            name of the Container (eg. red)
     * @return list of all flowspec configured for a container. Flowspec as
     *         specified by:
     *         {@link org.opendaylight.controller.containermanager.ContainerFlowConfig}
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/containermanager/container/red/flowspec
     *
     * Response body in XML:
     * &lt;flow-spec-configs&gt;
     *   &#x20;&#x20;&#x20;&#x20;&lt;flow-spec-config&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;name&gt;ssh&lt;/name&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;dlVlan&gt;52&lt;/dlVlan&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;nwSrc&gt;10.0.0.101&lt;/nwSrc&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;nwDst&gt;10.0.0.102&lt;/nwDst&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;protocol&gt;IPv4&lt;/protocol&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;tpSrc&gt;23&lt;/tpSrc&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;tpDst&gt;100&lt;/tpDst&gt;
     *   &#x20;&#x20;&#x20;&#x20;&lt;/flow-spec-config&gt;
     *   &#x20;&#x20;&#x20;&#x20;&lt;flow-spec-config&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;name&gt;http2&lt;/name&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;dlVlan&gt;123&lt;/dlVlan&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;nwSrc&gt;10.0.0.201&lt;/nwSrc&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;nwDst&gt;10.0.0.202&lt;/nwDst&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;protocol&gt;&lt;/protocol&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;tpSrc&gt;80&lt;/tpSrc&gt;
     *     &#x20;&#x20;&#x20;&#x20;&#x20;&#x20;&lt;tpDst&gt;100&lt;/tpDst&gt;
     *   &#x20;&#x20;&#x20;&#x20;&lt;/flow-spec-config&gt;
     * &lt;/flow-spec-configs&gt;
     *
      * Response body in JSON:
     * {
     *   "flow-spec-config": [
     *     {
     *       "name": "http",
     *       "dlVlan" : "52",
     *       "nwSrc": "10.0.0.201",
     *       "nwDst": "10.0.0.202",
     *       "protocol": "",
     *       "tpSrc": "80",
     *       "tpDst": "100"
     *     },
     *     {
     *       "name": "ssh",
     *       "dlVlan" : "123",
     *       "nwSrc": "10.0.0.101",
     *       "nwDst": "10.0.0.102",
     *       "protocol": "IPv4",
     *       "tpSrc": "23",
     *       "tpDst": "100"
     *     }
     *   ]
     * }
     *
     * </pre>
     */
    @Path("/container/{container}/flowspecs")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(FlowSpecConfigs.class)
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The container is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public FlowSpecConfigs viewContainerFlowSpecs(@PathParam(value = "container") String container) {

        handleContainerAuthorization(container, getUserName());
        handleForbiddenOnDefault(container);

        handleContainerNotExists(container);

        IContainerManager containerManager = getContainerManager();

        return new FlowSpecConfigs(containerManager.getContainerFlows(container));
    }

    /**
     * Add flowspec to a container
     *
     * @param container
     *            name of the container (eg. purple)
     * @param name
     *            name of the flowspec (eg. http)
     * @param flowspec
     *            configuration as specified by:
     *            {@link org.opendaylight.controller.containermanager.ContainerFlowConfig}
     *
     * @return Response as dictated by the HTTP Response code
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/containermanager/container/purple/flowspec/http
     *
     * Request body in XML:
     *   &lt;flow-spec-config&gt;
     *     &#x20;&#x20;&#x20;&#x20;&lt;name&gt;http&lt;/name&gt;
     *     &#x20;&#x20;&#x20;&#x20;&lt;dlVlan&gt;25&lt;/dlVlan&gt;
     *     &#x20;&#x20;&#x20;&#x20;&lt;nwSrc&gt;10.0.0.101&lt;/nwSrc&gt;
     *     &#x20;&#x20;&#x20;&#x20;&lt;nwDst&gt;10.0.0.102&lt;/nwDst&gt;
     *     &#x20;&#x20;&#x20;&#x20;&lt;protocol&gt;&lt;/protocol&gt;
     *     &#x20;&#x20;&#x20;&#x20;&lt;tpSrc&gt;80&lt;/tpSrc&gt;
     *     &#x20;&#x20;&#x20;&#x20;&lt;tpDst&gt;100&lt;/tpDst&gt;
     *   &lt;/flow-spec-config&gt;
     *
     * Request body in JSON:
     * {
     *    "name" : "http",
     *    "dlVlan" : "25",
     *    "nwSrc" : "10.0.0.101",
     *    "nwDst" : "10.0.0.102",
     *    "protocol" : "",
     *    "tpSrc" : "80",
     *    "tpDst" : "100"
     * }
     *
     * </pre>
     */
    @Path("/container/{container}/flowspec/{flowspec}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 201, condition = "FlowSpec created successfully"),
            @ResponseCode(code = 400, condition = "Invalid flowspec configuration"),
            @ResponseCode(code = 404, condition = "The container is not found"),
            @ResponseCode(code = 409, condition = "Container Entry already exists"),
            @ResponseCode(code = 500, condition = "Failed to create Flow specifications. Failure Reason included in HTTP Error response") })
    public Response createFlowSpec(@Context UriInfo uriInfo,
            @PathParam(value = "container") String container,
            @PathParam(value = "flowspec") String flowspec,
            @TypeHint(ContainerFlowConfig.class) ContainerFlowConfig containerFlowConfig) {

        handleAdminAuthorization(getUserName());
        handleForbiddenOnDefault(container);

        handleContainerNotExists(container);
        handleNameMismatch(containerFlowConfig.getName(), flowspec);

        IContainerManager containerManager = getContainerManager();
        List<ContainerFlowConfig> list = new ArrayList<ContainerFlowConfig>();
        list.add(containerFlowConfig);
        Status status = containerManager.addContainerFlows(container, list);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Flow Spec", username, "added", containerFlowConfig.getName());
            return Response.created(uriInfo.getRequestUri()).build();
        }
        return NorthboundUtils.getResponse(status);
    }

    /**
     * Remove flowspec from a container
     *
     * @param name
     *            name of the flowspec (eg. telnet)
     * @param container
     *            name of the Container (eg. black)
     * @return Response as dictated by the HTTP Response code
     *
     * <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/containermanager/container/black/flowspec/telnet
     *
     * </pre>
     */
    @Path("/container/{container}/flowspec/{flowspec}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 204, condition = "Flow Spec deleted successfully"),
            @ResponseCode(code = 400, condition = "Invalid flowspec configuration"),
            @ResponseCode(code = 404, condition = "Container or Container Entry not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
            @ResponseCode(code = 500, condition = "Failed to delete Flowspec. Failure Reason included in HTTP Error response"),
            @ResponseCode(code = 503, condition = "One or more of Controller service is unavailable") })
    public Response removeFlowSpec(@PathParam(value = "container") String container,
            @PathParam(value = "flowspec") String flowspec) {

        handleAdminAuthorization(getUserName());
        handleForbiddenOnDefault(container);

        handleContainerNotExists(container);

        IContainerManager containerManager = getContainerManager();
        Set<String> set = new HashSet<String>();
        set.add(flowspec);
        Status status = containerManager.removeContainerFlows(container, set);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Flow Spec", username, "added", flowspec);
            return Response.noContent().build();
        }
        return NorthboundUtils.getResponse(status);
    }

    /**
     * Add node connectors to a container
     *
     * @param container
     *            name of the container (eg. green)
     * @param list
     *            The list of strings each representing a node connector in the form "<Port Type>|<Port id>@<Node Type>|<Node id>", as "OF|1@OF|00:00:00:ab:00:00:00:01"
     * @return response as dictated by the HTTP Status code
     *
     * <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/containermanager/container/green/nodeconnector
     *
     * Request body in XML:
     * &lt;nodeConnectors&gt;
     *     &lt;nodeConnectors&gt;OF|1@OF|00:00:00:00:00:00:00:01&lt;/nodeConnectors&gt;
     *     &lt;nodeConnectors&gt;OF|2@OF|00:00:00:00:00:00:00:01&lt;/nodeConnectors&gt;
     *     &lt;nodeConnectors&gt;OF|3@OF|00:00:00:00:00:00:00:22&lt;/nodeConnectors&gt;
     *     &lt;nodeConnectors&gt;OF|4@OF|00:00:00:00:00:00:00:22&lt;/nodeConnectors&gt;
     * &lt;/nodeConnectors&gt;
     *
     * Request body in JSON:
     * {
     *    "nodeConnectors" : [
     *       "OF|1@OF|00:00:00:00:00:00:00:01",
     *       "OF|2@OF|00:00:00:00:00:00:00:01",
     *       "OF|3@OF|00:00:00:00:00:00:00:22",
     *       "OF|4@OF|00:00:00:00:00:00:00:22"
     *    ]
     * }
     *
     * </pre>
     */
    @Path("/container/{container}/nodeconnector/")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Response.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "NodeConnectors added successfully"),
            @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
            @ResponseCode(code = 403, condition = "Operation forbidden on default"),
            @ResponseCode(code = 404, condition = "The Container is not found"),
            @ResponseCode(code = 409, condition = "Container Entry already exists"),
            @ResponseCode(code = 500, condition = "Failed to create nodeconnectors. Failure Reason included in HTTP Error response") })
    public Response addNodeConnectors(@PathParam(value = "container") String container,
            @TypeHint(StringList.class) StringList list) {

        handleAdminAuthorization(getUserName());
        handleForbiddenOnDefault(container);
        handleContainerNotExists(container);

        IContainerManager containerManager = getContainerManager();
        Status status = containerManager.addContainerEntry(container, list.getList());
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Node ", username, "added", " Ports:" + list.getList());
        }
        return NorthboundUtils.getResponse(status);
    }

    /**
     * Remove node connectors from a container
     *
     * @param container
     *            name of the container (eg. red)
     * @param list
     *            The list of strings each representing a node connector in the form "<Port Type>|<Port id>@<Node Type>|<Node id>", as "OF|1@OF|00:00:00:ab:00:00:00:01"
     * @return response as dictated by the HTTP Status code
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/containermanager/container/red/nodeconnector
     *
     * Request body in XML:
     * &lt;nodeConnectors&gt;
     *     &lt;nodeConnectors&gt;OF|1@OF|00:00:00:00:00:00:00:01&lt;/nodeConnectors&gt;
     *     &lt;nodeConnectors&gt;OF|2@OF|00:00:00:00:00:00:00:01&lt;/nodeConnectors&gt;
     *     &lt;nodeConnectors&gt;OF|3@OF|00:00:00:00:00:00:00:22&lt;/nodeConnectors&gt;
     *     &lt;nodeConnectors&gt;OF|4@OF|00:00:00:00:00:00:00:22&lt;/nodeConnectors&gt;
     * &lt;/nodeConnectors&gt;
     *
     * Request body in JSON:
     * {
     *    "nodeConnectors" : [
     *       "OF|1@OF|00:00:00:00:00:00:00:01",
     *       "OF|2@OF|00:00:00:00:00:00:00:01",
     *       "OF|3@OF|00:00:00:00:00:00:00:22",
     *       "OF|4@OF|00:00:00:00:00:00:00:22"
     *       ]
     * }
     *
     * </pre>
     */
    @Path("/container/{container}/nodeconnector/")
    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 204, condition = "Container Entry deleted successfully"),
            @ResponseCode(code = 400, condition = "Invalid Container Entry configuration"),
            @ResponseCode(code = 404, condition = "The Container is not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
            @ResponseCode(code = 500, condition = "Failed to delete node connector. Failure Reason included in HTTP Error response") })
    public Response removeNodeConnectors(@PathParam(value = "container") String container,
            @TypeHint(StringList.class) StringList portList) {

        handleAdminAuthorization(getUserName());
        handleForbiddenOnDefault(container);
        handleContainerNotExists(container);

        IContainerManager containerManager = getContainerManager();
        Status status = containerManager.removeContainerEntry(container, portList.getList());
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Node", username, "removed", " Ports:" + portList.getList());
            return Response.noContent().build();
        }
        return NorthboundUtils.getResponse(status);
    }

    /*
     * Check If the function is not allowed on default container, Throw a
     * ResourceForbiddenException exception if forbidden
     */
    private void handleForbiddenOnDefault(String container) {
        if (container.equalsIgnoreCase(GlobalConstants.DEFAULT.toString())) {
            throw new ResourceForbiddenException(RestMessages.NODEFAULT.toString() + ": " + container);
        }
    }

    /*
     * Check if container exists, Throw a ResourceNotFoundException exception if it
     * does not exist
     */
    private void handleContainerNotExists(String container) {
        IContainerManager containerManager = getContainerManager();
        if (!containerManager.doesContainerExist(container)) {
            throw new ResourceNotFoundException(RestMessages.NOCONTAINER.toString() + ": " + container);
        }
    }

    private void handleContainerExists(String container) {
        IContainerManager containerManager = getContainerManager();
        if (containerManager.doesContainerExist(container)) {
            throw new ResourceConflictException(RestMessages.RESOURCECONFLICT.toString() + ": " + container);
        }
    }

    private void handleAdminAuthorization(String userName) {
        IUserManager usrMgr = (IUserManager) ServiceHelper.getGlobalInstance(IUserManager.class, this);

        UserLevel level = usrMgr.getUserLevel(userName);
        if (level.ordinal() <= UserLevel.NETWORKADMIN.ordinal()) {
            return;
        }

        throw new UnauthorizedException("User is not authorized to perform this operation");
    }

    private void handleNetworkAuthorization(String userName) {
        IUserManager usrMgr = (IUserManager) ServiceHelper.getGlobalInstance(IUserManager.class, this);

        UserLevel level = usrMgr.getUserLevel(userName);
        if (level.ordinal() <= UserLevel.NETWORKOPERATOR.ordinal()) {
            return;
        }
        throw new UnauthorizedException("User is not authorized to perform this operation");
    }

    private void handleContainerAuthorization(String container, String userName) {
        IContainerAuthorization auth = (IContainerAuthorization) ServiceHelper.getGlobalInstance(
                IContainerAuthorization.class, this);

        UserLevel level = auth.getUserLevel(userName);
        if (level.ordinal() <= UserLevel.NETWORKOPERATOR.ordinal()) {
            return;
        }

        Privilege current = (auth == null) ? Privilege.NONE : auth.getResourcePrivilege(userName, container);

        if (current.ordinal() > Privilege.NONE.ordinal()) {
            return;
        }
        throw new UnauthorizedException("User is not authorized to perform this operation");
    }

}
