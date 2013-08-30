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
import javax.xml.bind.JAXBElement;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.BadRequestException;
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.SubnetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class SubnetsNorthbound {
    protected static final Logger logger = LoggerFactory.getLogger(SubnetsNorthbound.class);

    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        if (context != null && context.getUserPrincipal() != null) username = context.getUserPrincipal().getName();
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
     *         <pre>
     * Example:
     *
     * Request URL: http://localhost:8080/controller/nb/v2/subnet/default/subnet/all
     *
     * Response in XML:
     * &lt;subnetConfig&gt;
     *    &lt;name&gt;subnet1&lt;/name&gt;
     *    &lt;subnet&gt;30.0.0.1/24&lt;/subnet&gt;
     *    &lt;nodePorts&gt;1/1&gt;/nodePorts&gt;
     *    &lt;nodePorts&gt;1/2&gt;/nodePorts&gt;
     *    &lt;nodePorts&gt;1/3&gt;/nodePorts&gt;
     * &lt;/subnetConfig&gt;
     * &lt;subnetConfig&gt;
     *    &lt;name&gt;subnet2&lt;/name&gt;
     *    &lt;subnet&gt;20.0.0.1/24&lt;/subnet&gt;
     *    &lt;nodePorts&gt;2/1&gt;/nodePorts&gt;
     *    &lt;nodePorts&gt;2/2&gt;/nodePorts&gt;
     *    &lt;nodePorts&gt;2/3&gt;/nodePorts&gt;
     * &lt;/subnetConfig&gt;
     *
     * Response in JSON:
     * {
     *  "name":"subnet1",
     *  "subnet":"30.0.0.1/24",
     *  "nodePorts":["1/1","1/2","1/3"]
     * }
     * {
     *  "name":"subnet2",
     *  "subnet":"20.0.0.1/24",
     *  "nodePorts":["2/1","2/2","2/3"]
     * }
     * </pre>
     */
    @Path("/{containerName}/subnet/all")
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
         ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName, this);
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
     * Request URL: http://localhost:8080/controller/nb/v2/subnet/default/subnet/subnet1
     *
     * Response in XML:
     * &lt;subnetConfig&gt;
     *    &lt;name&gt;subnet1&lt;/name&gt;
     *    &lt;subnet&gt;30.0.0.1/24&lt;/subnet&gt;
     *    &lt;nodePorts&gt;1/1&gt;/nodePorts&gt;
     *    &lt;nodePorts&gt;1/2&gt;/nodePorts&gt;
     *    &lt;nodePorts&gt;1/3&gt;/nodePorts&gt;
     * &lt;/subnetConfig&gt;
     *
     * Response in JSON:
     * {
     *  "name":"subnet1",
     *  "subnet":"30.0.0.1/24",
     *  "nodePorts":["1/1","1/2","1/3"]
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
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("SwitchManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        SubnetConfig res = switchManager.getSubnetConfig(subnetName);
        if (res == null) {
            throw new ResourceNotFoundException(RestMessages.NOSUBNET.toString());
        } else {
            return res;
        }
    }

    /**
     * Add a subnet to a container
     *
     * @param containerName
     *            name of the container to which subnet needs to be added
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
     * Request URL: http://localhost:8080/controller/nb/v2/subnet/default/subnet/subnet1
     *
     * Request XML:
     *  &lt;subnetConfig&gt;
     *      &lt;name&gt;subnet1&lt;/name&gt;
     *      &lt;subnet&gt;30.0.0.1/24&lt;/subnet&gt;
     * &lt;/subnetConfig&gt;
     *
     * Request in JSON:
     * {
     *  "name":"subnet1",
     *  "subnet":"30.0.0.1/24"
     * }
     * </pre>
     */

    @Path("/{containerName}/subnet/{subnetName}")
    @POST
    @StatusCodes({ @ResponseCode(code = 201, condition = "Subnet created successfully"),
            @ResponseCode(code = 400, condition = "Invalid data passed"),
            @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
            @ResponseCode(code = 409, condition = "Subnet name in url conflicts with name in request body"),
            @ResponseCode(code = 404, condition = "Container name passed was not found or subnet config is null"),
            @ResponseCode(code = 500, condition = "Internal Server Error: Addition of subnet failed"),
            @ResponseCode(code = 503, condition = "Service unavailable") })
    public Response addSubnet(@PathParam("containerName") String containerName,
            @PathParam("subnetName") String subnetName,
            @TypeHint(SubnetConfig.class) JAXBElement<SubnetConfig> subnetConfigData) {

        handleContainerDoesNotExist(containerName);

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        SubnetConfig cfgObject = subnetConfigData.getValue();
        handleNameMismatch(cfgObject.getName(), subnetName);

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("SwitchManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        Set<String> ports = cfgObject.getNodePorts();
        SubnetConfig subnetCfg = null;
        if (ports == null) {
            subnetCfg = new SubnetConfig(cfgObject.getName(), cfgObject.getSubnet(), new HashSet<String>(0));
        } else {
            subnetCfg = cfgObject;
        }

        Status status = switchManager.addSubnet(subnetCfg);

        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Subnet Gateway", username, "added", subnetName, containerName);
            return Response.status(Response.Status.CREATED).build();
        }
        return NorthboundUtils.getResponse(status);
    }

    /**
     * Delete a subnet from a container
     *
     * @param containerName
     *            name of the container from which subnet needs to be removed
     * @param subnetName
     *            name of new subnet to be deleted
     * @return Response as dictated by the HTTP Response Status code
     *
     *         <pre>
     * Example:
     *            Request URL: http://localhost:8080/controller/nb/v2/subnet/default/subnet/subnet1
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

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName, this);
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
     * Modify a subnet. For now only changing the port list is allowed.
     *
     * @param containerName
     *            Name of the Container
     * @param subnetName
     *            Name of the SubnetConfig to be modified
     * @param subnetConfigData
     *            the {@link SubnetConfig} structure in request body
     *            parameter
     * @return If the operation is successful or not
     *
     *         <pre>
     * Example:
     *
     * Request URL: http://localhost:8080/controller/nb/v2/subnet/default/subnet/subnet1/node-ports
     *
     *  Request in XML:
     *  &lt;subnetConfig&gt;
     *      &lt;name&gt;subnet1&lt;/name&gt;
     *      &lt;subnet&gt;30.0.0.1/24&lt;/subnet&gt;
     *      &lt;nodePorts&gt;1/1&lt;/nodePorts&gt;
     *      &lt;nodePorts&gt;1/2&lt;/nodePorts&gt;
     *      &lt;nodePorts&gt;1/3&lt;/nodePorts&gt;
     * &lt;/subnetConfig&gt;
     *
     * Request in JSON:
     * {
     *  "name":"subnet1",
     *  "subnet":"30.0.0.1/24",
     *  "nodePorts":["1/1","1/2","1/3"]
     * }
     * </pre>
     */
    @Path("/{containerName}/subnet/{subnetName}/node-ports")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Ports replaced successfully"),
            @ResponseCode(code = 400, condition = "Invalid request to change subnet name or invalid node ports passed"),
            @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
            @ResponseCode(code = 409, condition = "Subnet name in url conflicts with name in request body"),
            @ResponseCode(code = 404, condition = "The containerName or subnetName is not found"),
            @ResponseCode(code = 500, condition = "Internal server error: Modify ports failed"),
            @ResponseCode(code = 503, condition = "Service unavailable") })
    public Response modifySubnet(@PathParam("containerName") String containerName,
            @PathParam("subnetName") String subnetName,
            @TypeHint(SubnetConfig.class) JAXBElement<SubnetConfig> subnetConfigData) {

        handleContainerDoesNotExist(containerName);

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        handleNameMismatch(subnetConfigData.getValue().getName(), subnetName);

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("SwitchManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        SubnetConfig subnetConf = subnetConfigData.getValue();
        SubnetConfig existingConf = switchManager.getSubnetConfig(subnetName);

        boolean successful = true;

        // make sure that the name matches an existing subnet and we're not
        // changing the name or subnet IP/mask
        if (existingConf == null) {
            // don't have a subnet by that name
            return Response.status(Response.Status.NOT_FOUND).build();

        } else if (!existingConf.getName().equals(subnetConf.getName())
                || !existingConf.getSubnet().equals(subnetConf.getSubnet())) {
            // can't change the name of a subnet
            Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            // create a set for fast lookups
            Set<String> newPorts = new HashSet<String>(subnetConf.getNodePorts());

            // go through the current ports and (1) remove ports that aren't
            // there anymore and (2) remove ports that are still there from the
            // set of ports to add
            for (String s : existingConf.getNodePorts()) {
                if (newPorts.contains(s)) {
                    newPorts.remove(s);
                } else {
                    Status st = switchManager.removePortsFromSubnet(subnetName, s);
                    successful = successful && st.isSuccess();
                }
            }

            // add any remaining ports
            for (String s : newPorts) {
                Status st = switchManager.addPortsToSubnet(subnetName, s);
                successful = successful && st.isSuccess();
                if (successful) {
                    NorthboundUtils.auditlog("Subnet Gateway", username, "added", s + " to " + subnetName,
                            containerName);
                }
            }
        }

        if (successful) {
            return Response.status(Response.Status.OK).build();
        }
        throw new InternalServerErrorException(RestMessages.INTERNALERROR.toString());
    }

    /**
     * Add ports to a subnet in the container
     *
     * @param containerName
     *            name of the container that has the subnet to which node ports
     *            need to be added
     * @param subnetName
     *            name of subnet to which node ports need to be added
     * @param SubnetConfig
     *            the {@link SubnetConfig} structure in request body
     * @return Response as dictated by the HTTP Response Status code
     *
     *         <pre>
     * Example:
     *            Request URL: http://localhost:8080/controller/nb/v2/subnet/default/subnet/subnet1/node-ports
     *
     * Request XML:
     *  &lt;subnetConfig&gt;
     *      &lt;name&gt;subnet1&lt;/name&gt;
     *      &lt;subnet&gt;30.0.0.1/24&lt;/subnet&gt;
     *      &lt;nodePorts&gt;1/1&lt;/nodePorts&gt;
     *      &lt;nodePorts&gt;1/2&lt;/nodePorts&gt;
     *      &lt;nodePorts&gt;1/3&lt;/nodePorts&gt;
     * &lt;/subnetConfig&gt;
     *
     * Request in JSON:
     * {
     *  "name":"subnet1",
     *  "subnet":"30.0.0.1/24",
     *  "nodePorts":["1/1","1/2","1/3"]
     * }
     *
     * </pre>
     */
    @Path("/{containerName}/subnet/{subnetName}/node-ports")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Added node ports to subnet successfully"),
            @ResponseCode(code = 400, condition = "Invalid request to change subnet name or invalid node ports passed"),
            @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
            @ResponseCode(code = 404, condition = "The containerName or subnet is not found"),
            @ResponseCode(code = 409, condition = "Subnet name in url conflicts with name in request body"),
            @ResponseCode(code = 500, condition = "Internal server error : Port add failed"),
            @ResponseCode(code = 503, condition = "Service unavailable") })
    public Response addNodePorts(@PathParam("containerName") String containerName,
            @PathParam("subnetName") String subnetName,
            @TypeHint(SubnetConfig.class) JAXBElement<SubnetConfig> subnetConfigData) {

        handleContainerDoesNotExist(containerName);

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        handleNameMismatch(subnetConfigData.getValue().getName(), subnetName);

        SubnetConfig subnetConf = subnetConfigData.getValue();

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("SwitchManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        SubnetConfig existingConf = switchManager.getSubnetConfig(subnetName);

        // make sure that the name matches an existing subnet and we're not
        // changing the name or subnet IP/mask
        if (existingConf == null) {
            // don't have a subnet by that name
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (!existingConf.getName().equals(subnetConf.getName())
                || !existingConf.getSubnet().equals(subnetConf.getSubnet())) {
            // can't change the name of a subnet
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Status st;
        boolean successful = true;
        Set<String> ports = subnetConf.getNodePorts();

        if (ports == null || ports.isEmpty()) {
            throw new BadRequestException(RestMessages.INVALIDDATA.toString());
        }

        // add new ports only
        if (existingConf.getNodePorts() != null) {
            ports.removeAll(existingConf.getNodePorts());
        }
        for (String port : ports) {
            st = switchManager.addPortsToSubnet(subnetName, port);
            successful = successful && st.isSuccess();
            if (successful) {
                NorthboundUtils.auditlog("Subnet Gateway", username, "added", st + " to " + subnetName, containerName);
            }
        }
        if (successful) {
            return Response.status(Response.Status.OK).build();
        }
        throw new InternalServerErrorException(RestMessages.INTERNALERROR.toString());
    }

    /**
     * Delete ports from a subnet in the container
     *
     * @param containerName
     *            name of the container that has the subnet from which node
     *            ports need to be deleted
     * @param subnetName
     *            name of subnet from which node ports need to be deleted
     * @param subnetConfigData
     *            SubnetConfig object to be deleted
     * @return Response as dictated by the HTTP Response Status code
     *
     *         <pre>
     * Example:
     *            Request URL: http://localhost:8080/controller/nb/v2/subnet/default/subnet/subnet1/node-ports
     *
     * Request XML:
     *  &lt;subnetConfig&gt;
     *      &lt;name&gt;subnet3&lt;/name&gt;
     *      &lt;subnet&gt;30.0.0.1/24&lt;/subnet&gt;
     *      &lt;nodePorts&gt;1/1,1/2,1/3&lt;/nodePorts&gt;
     * &lt;/subnetConfig&gt;
     *
     * Request in JSON:
     * { "name" : "subnet1",
     *   "subnet" : "30.0.0.1/24",
     *    nodePorts : ["1/1,1/2,1/3"]}
     *
     * </pre>
     */
    @Path("/{containerName}/subnet/{subnetName}/node-ports")
    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
            @ResponseCode(code = 204, condition = "No content"),
            @ResponseCode(code = 400, condition = "Invalid request to change subnet name or invalid node ports passed"),
            @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
            @ResponseCode(code = 404, condition = "The containerName or subnet is not found"),
            @ResponseCode(code = 409, condition = "Subnet name in url conflicts with name in request body"),
            @ResponseCode(code = 500, condition = "Internal server error : Delete node ports failed"),
            @ResponseCode(code = 503, condition = "Service unavailable") })
    public Response deleteNodePorts(@PathParam("containerName") String containerName,
            @PathParam("subnetName") String subnetName,
            @TypeHint(SubnetConfig.class) JAXBElement<SubnetConfig> subnetConfigData) {

        handleContainerDoesNotExist(containerName);

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        handleNameMismatch(subnetConfigData.getValue().getName(), subnetName);

        SubnetConfig subnetConf = subnetConfigData.getValue();

        if (subnetConf.getNodePorts() == null || subnetConf.getNodePorts().isEmpty()) {
            throw new BadRequestException(RestMessages.INVALIDDATA.toString() + " : invalid node ports");
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);
        if (switchManager == null) {
            throw new ServiceUnavailableException("SwitchManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        SubnetConfig existingConf = switchManager.getSubnetConfig(subnetName);

        // make sure that the name matches an existing subnet and we're not
        // changing the name or subnet IP/mask
        if (existingConf == null) {
            // don't have a subnet by that name
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (!existingConf.getName().equals(subnetConf.getName())
                || !existingConf.getSubnet().equals(subnetConf.getSubnet())) {
            // can't change the name of a subnet
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Status st;
        boolean successful = true;
        Set<String> ports = subnetConf.getNodePorts();

        // delete existing ports
        ports.retainAll(existingConf.getNodePorts());
        for (String port : ports) {
            st = switchManager.removePortsFromSubnet(subnetName, port);
            successful = successful && st.isSuccess();
            if (successful) {
                NorthboundUtils.auditlog("Subnet Gateway", username, "removed", st + " from " + subnetName,
                        containerName);
            }
        }
        if (successful) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        throw new InternalServerErrorException(RestMessages.INTERNALERROR.toString());
    }
}
