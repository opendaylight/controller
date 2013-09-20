/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subnets.northbound;

import java.util.HashSet;
import java.util.List;
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
import javax.ws.rs.core.UriInfo;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.BadRequestException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.SubnetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides REST APIs to manage subnets.
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
public class SubnetsNorthbound {
    protected static final Logger logger = LoggerFactory.getLogger(SubnetsNorthbound.class);

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

    private void handleContainerDoesNotExist(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper.getGlobalInstance(
                IContainerManager.class, this);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container " + RestMessages.NOCONTAINER.toString());
        }

        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames) {
            if (cName.trim().equalsIgnoreCase(containerName.trim())) {
                return;
            }
        }

        throw new ResourceNotFoundException(containerName + " " + RestMessages.NOCONTAINER.toString());
    }

    private void handleNameMismatch(String name, String nameinURL) {
        if (name == null || nameinURL == null) {
            throw new BadRequestException(RestMessages.INVALIDDATA.toString() + " : Name is null");
        }

        if (name.equals(nameinURL)) {
            return;
        }
        throw new ResourceConflictException(RestMessages.INVALIDDATA.toString()
                + " : Name in URL does not match the name in request body");
    }

    /**
     * List all the subnets in a given container
     *
     * @param containerName
     *            container in which we want to query the subnets
     *
     * @return a List of SubnetConfig
     *
     * <pre>
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/subnetservice/default/subnets
     *
     * Response body in XML:
     * &lt;list&gt;
     * &lt;subnetConfig&gt;
     *    &lt;name&gt;marketingdepartment&lt;/name&gt;
     *    &lt;subnet&gt;30.31.54.254/24&lt;/subnet&gt;
     * &lt;/subnetConfig&gt;
     * &lt;subnetConfig&gt;
     *    &lt;name&gt;salesdepartment&lt;/name&gt;
     *    &lt;subnet&gt;20.18.1.254/16&lt;/subnet&gt;
     *    &lt;nodeConnectors&gt;OF|11@OF|00:00:00:aa:bb:cc:dd:ee&lt;/nodeConnectors&gt;
     *    &lt;nodeConnectors&gt;OF|13@OF|00:00:00:aa:bb:cc:dd:ee&lt;/nodeConnectors&gt;
     * &lt;/subnetConfig&gt;
     * &lt;/list&gt;
     * Response body in JSON:
     * {
     *   "subnetConfig": [
     *     {
     *       "name": "marketingdepartment",
     *       "subnet": "30.31.54.254/24",
     *       "nodeConnectors": [
     *           "OF|04@OF|00:00:00:00:00:00:00:04",
     *           "OF|07@OF|00:00:00:00:00:00:00:07"
     *       ]
     *     },
     *     {
     *       "name":"salesdepartment",
     *       "subnet":"20.18.1.254/16",
     *       "nodeConnectors": [
     *            "OF|11@OF|00:00:00:aa:bb:cc:dd:ee",
     *            "OF|13@OF|00:00:00:aa:bb:cc:dd:ee"
     *        ]
     *      }
     *   ]
     * }
     *
     * </pre>
     */
    @Path("/{containerName}/subnets")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName passed was not found"),
        @ResponseCode(code = 503, condition = "Service unavailable") })
    @TypeHint(SubnetConfigs.class)
    public SubnetConfigs listSubnets(@PathParam("containerName") String containerName) {

        handleContainerDoesNotExist(containerName);
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("SwitchManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        return new SubnetConfigs(switchManager.getSubnetsConfigList());
    }

    /**
     * List the configuration of a subnet in a given container
     *
     * @param containerName
     *            container in which we want to query the subnet
     * @param subnetName
     *            of the subnet being queried
     *
     * @return SubnetConfig
     *
     *         <pre>
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/subnetservice/default/subnet/marketingdepartment
     *
     * Response body in XML:
     * &lt;subnetConfig&gt;
     *    &lt;name&gt;marketingdepartment&lt;/name&gt;
     *    &lt;subnet&gt;30.0.0.1/24&lt;/subnet&gt;
     *    &lt;nodeConnectors&gt;OF|1@OF|00:00:00:00:00:00:00:01&lt;/nodePorts&gt;
     *    &lt;nodeConnectors&gt;OF|3@OF|00:00:00:00:00:00:00:03&lt;/nodePorts&gt;
     * &lt;/subnetConfig&gt;
     *
     * Response body in JSON:
     * {
     *  "name":"marketingdepartment",
     *  "subnet":"30.0.0.1/24",
     *  "nodeConnectors":[
     *       "OF|1@OF|00:00:00:00:00:00:00:01",
     *       "OF|3@OF|00:00:00:00:00:00:00:03"
     *   ]
     * }
     * </pre>
     */
    @Path("/{containerName}/subnet/{subnetName}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName or subnetName passed was not found"),
        @ResponseCode(code = 503, condition = "Service unavailable") })
    @TypeHint(SubnetConfig.class)
    public SubnetConfig listSubnet(@PathParam("containerName") String containerName,
            @PathParam("subnetName") String subnetName) {

        handleContainerDoesNotExist(containerName);

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("SwitchManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        SubnetConfig res = switchManager.getSubnetConfig(subnetName);
        if (res == null) {
            throw new ResourceNotFoundException(RestMessages.NOSUBNET.toString());
        }
        return res;
    }

    /**
     * Add a subnet into the specified container context, node connectors are
     * optional
     *
     * @param containerName
     *            name of the container context in which the subnet needs to be
     *            added
     * @param subnetName
     *            name of new subnet to be added
     * @param subnetConfigData
     *            the {@link SubnetConfig} structure in request body
     *
     * @return Response as dictated by the HTTP Response Status code
     *
     *         <pre>
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/subnetservice/default/subnet/salesdepartment
     *
     * Request body in XML:
     *  &lt;subnetConfig&gt;
     *      &lt;name&gt;salesdepartment&lt;/name&gt;
     *      &lt;subnet&gt;172.173.174.254/24&lt;/subnet&gt;
     *      &lt;nodeConnectors&gt;OF|22@OF|00:00:11:22:33:44:55:66&lt;/nodeConnectors&gt;
     *      &lt;nodeConnectors&gt;OF|39@OF|00:00:ab:cd:33:44:55:66&lt;/nodeConnectors&gt;
     *  &lt;/subnetConfig&gt;
     *
     * Request body in JSON:
     * {
     *  "name":"salesdepartment",
     *  "subnet":"172.173.174.254/24",
     *  "nodeConnectors":[
     *       "OF|22@OF|00:00:11:22:33:44:55:66",
     *       "OF|39@OF|00:00:ab:cd:33:44:55:66"
     *       ]
     * }
     * </pre>
     */

    @Path("/{containerName}/subnet/{subnetName}")
    @PUT
    @StatusCodes({ @ResponseCode(code = 201, condition = "Subnet created successfully"),
        @ResponseCode(code = 400, condition = "Invalid data passed"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 409, condition = "Subnet name in url conflicts with name in request body"),
        @ResponseCode(code = 404, condition = "Container name passed was not found or subnet config is null"),
        @ResponseCode(code = 500, condition = "Internal Server Error: Addition of subnet failed"),
        @ResponseCode(code = 503, condition = "Service unavailable") })
    public Response addSubnet(@PathParam("containerName") String containerName,
            @PathParam("subnetName") String subnetName, @TypeHint(SubnetConfig.class) SubnetConfig subnetConfigData) {

        handleContainerDoesNotExist(containerName);

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        SubnetConfig cfgObject = subnetConfigData;
        handleNameMismatch(cfgObject.getName(), subnetName);

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("SwitchManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        Status status = switchManager.addSubnet(cfgObject);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Subnet Gateway", username, "added", subnetName, containerName);
            if (subnetConfigData.getNodeConnectors() != null) {
                for (NodeConnector port : subnetConfigData.getNodeConnectors()) {
                    NorthboundUtils.auditlog("Port", getUserName(), "added",
                            NorthboundUtils.getPortName(port, switchManager) + " to Subnet Gateway " + subnetName,
                            containerName);
                }
            }
            return Response.status(Response.Status.CREATED).build();
        }
        return NorthboundUtils.getResponse(status);
    }

    /**
     * Delete a subnet from the specified container context
     *
     * @param containerName
     *            name of the container in which subnet needs to be removed
     * @param subnetName
     *            name of new subnet to be deleted
     * @return Response as dictated by the HTTP Response Status code
     *
     * <pre>
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/subnetservice/default/subnet/engdepartment
     *
     * </pre>
     */
    @Path("/{containerName}/subnet/{subnetName}")
    @DELETE
    @StatusCodes({ @ResponseCode(code = 204, condition = "No Content"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName passed was not found"),
        @ResponseCode(code = 500, condition = "Internal Server Error : Removal of subnet failed"),
        @ResponseCode(code = 503, condition = "Service unavailable") })
    public Response removeSubnet(@PathParam("containerName") String containerName,
            @PathParam("subnetName") String subnetName) {

        handleContainerDoesNotExist(containerName);

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("SwitchManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        Status status = switchManager.removeSubnet(subnetName);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Subnet Gateway", username, "removed", subnetName, containerName);
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        return NorthboundUtils.getResponse(status);
    }

    /**
     * Modify a subnet. Replace the existing subnet with the new specified one.
     * For now only port list modification is allowed. If the respective subnet
     * configuration does not exist this call is equivalent to a subnet
     * creation.
     *
     * @param containerName
     *            Name of the Container context
     * @param subnetName
     *            Name of the subnet to be modified
     * @param subnetConfigData
     *            the {@link SubnetConfig} structure in request body parameter
     * @return Response as dictated by the HTTP Response Status code
     *
     *         <pre>
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/subnetservice/default/subnet/salesdepartment
     *
     *  Request body in XML:
     *  &lt;subnetConfig&gt;
     *      &lt;name&gt;salesdepartment&lt;/name&gt;
     *      &lt;subnet&gt;172.173.174.254/24&lt;/subnet&gt;
     *      &lt;nodeConnectors&gt;OF|22@OF|00:00:11:22:33:44:55:66&lt;/nodeConnectors&gt;
     *      &lt;nodeConnectors&gt;OF|39@OF|00:00:ab:cd:33:44:55:66&lt;/nodeConnectors&gt;
     *  &lt;/subnetConfig&gt;
     *
     * Request body in JSON:
     * {
     *  "name":"salesdepartment",
     *  "subnet":"172.173.174.254/24",
     *  "nodeConnectors":[
     *      "OF|22@OF|00:00:11:22:33:44:55:66",
     *      "OF|39@OF|00:00:ab:cd:33:44:55:66"
     *  ]
     * }
     * </pre>
     */
    @Path("/{containerName}/subnet/{subnetName}")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 200, condition = "Configuration replaced successfully"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 409, condition = "Subnet name in url conflicts with name in request body"),
        @ResponseCode(code = 404, condition = "The containerName or subnetName is not found"),
        @ResponseCode(code = 500, condition = "Internal server error: Modify subnet failed"),
        @ResponseCode(code = 503, condition = "Service unavailable") })
    public Response modifySubnet(@Context UriInfo uriInfo, @PathParam("containerName") String containerName,
            @PathParam("subnetName") String subnetName, @TypeHint(SubnetConfig.class) SubnetConfig subnetConfigData) {

        handleContainerDoesNotExist(containerName);

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        handleNameMismatch(subnetConfigData.getName(), subnetName);

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("SwitchManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        // Need to check this until Status does not return a CREATED status code
        SubnetConfig existingConf = switchManager.getSubnetConfig(subnetName);

        Status status = switchManager.modifySubnet(subnetConfigData);

        if (status.isSuccess()) {
            if (existingConf == null) {
                NorthboundUtils.auditlog("Subnet Gateway", username, "added", subnetName, containerName);
                if (subnetConfigData.getNodeConnectors() != null) {
                    for (NodeConnector port : subnetConfigData.getNodeConnectors()) {
                        NorthboundUtils.auditlog("Port", getUserName(), "added",
                                NorthboundUtils.getPortName(port, switchManager) + " to Subnet Gateway" + subnetName,
                                containerName);
                    }
                }
                return Response.created(uriInfo.getRequestUri()).build();
            } else {
                Set<NodeConnector> existingNCList = existingConf.getNodeConnectors();

                if (existingNCList == null) {
                    existingNCList = new HashSet<NodeConnector>(0);
                }
                if (subnetConfigData.getNodeConnectors() != null) {
                    for (NodeConnector port : subnetConfigData.getNodeConnectors()) {
                        if (!existingNCList.contains(port)) {
                            NorthboundUtils.auditlog("Port", getUserName(), "added",
                                    NorthboundUtils.getPortName(port, switchManager) + " to Subnet Gateway "
                                            + subnetName, containerName);
                        }
                    }
                }
                for (NodeConnector port : existingNCList) {
                    if (!subnetConfigData.getNodeConnectors().contains(port)) {
                        NorthboundUtils
                                .auditlog("Port", getUserName(), "removed",
                                        NorthboundUtils.getPortName(port, switchManager) + " from Subnet Gateway "
                                                + subnetName, containerName);
                    }
                }
            }
        }
        return NorthboundUtils.getResponse(status);
    }
}
