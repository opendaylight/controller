/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subnets.northbound;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.SubnetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class SubnetsNorthboundJAXRS {
    protected static final Logger logger = LoggerFactory.getLogger(SubnetsNorthboundJAXRS.class);

    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        username = context.getUserPrincipal().getName();
    }

    protected String getUserName() {
        return username;
    }

    /**
     * List all the subnets in a given container
     *
     * @param containerName
     *            container in which we want to query the subnets
     *
     * @return a List of SubnetConfig
     */
    @Path("/{containerName}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 404, condition = "The containerName passed was not found") })
    @TypeHint(SubnetConfigs.class)
    public SubnetConfigs listSubnets(@PathParam("containerName") String containerName) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        ISwitchManager switchManager = null;
        switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            throw new ResourceNotFoundException(RestMessages.NOCONTAINER.toString());
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
     * @return a SubnetConfig
     */
    @Path("/{containerName}/{subnetName}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
        @ResponseCode(code = 404, condition = "The containerName passed was not found"),
        @ResponseCode(code = 404, condition = "Subnet does not exist") })
        @TypeHint(SubnetConfig.class)
    public SubnetConfig listSubnet(
        @PathParam("containerName") String containerName,
        @PathParam("subnetName") String subnetName) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        ISwitchManager switchManager = null;
        switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            throw new ResourceNotFoundException(RestMessages.NOCONTAINER.toString());
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
     *            container in which we want to add/update the subnet
     * @param subnetName
     *            that has to be added
     * @param subnet
     *            pair default gateway IP/mask that identify the subnet being
     *            added modified
     *
     */
    @Path("/{containerName}/{subnetName}")
    @POST
    @StatusCodes({
        @ResponseCode(code = 404, condition = "Invalid Data passed"),
        @ResponseCode(code = 201, condition = "Subnet added"),
        @ResponseCode(code = 500, condition = "Addition of subnet failed") })
    public Response addSubnet(
        @PathParam("containerName") String containerName,
        @PathParam("subnetName") String subnetName, @QueryParam("subnet") String subnet) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        if (subnetName == null) {
            throw new ResourceNotFoundException(RestMessages.INVALIDDATA.toString());
        }
        if (subnet == null) {
            throw new ResourceNotFoundException(RestMessages.INVALIDDATA.toString());
        }
        ISwitchManager switchManager = null;
        switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            throw new ResourceNotFoundException(RestMessages.NOCONTAINER.toString());
        }

        SubnetConfig cfgObject = new SubnetConfig(subnetName, subnet, new HashSet<String>(0));
        Status status = switchManager.addSubnet(cfgObject);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Subnet Gateway", username, "added", subnetName, containerName);
            return Response.status(Response.Status.CREATED).build();
        }
        throw new InternalServerErrorException(status.getDescription());
    }

    /**
     * Delete a subnet from a container
     *
     * @param containerName
     *            container in which we want to delete the subnet by name
     * @param subnetName
     *            of the subnet to be remove.
     *
     */
    @Path("/{containerName}/{subnetName}")
    @DELETE
    @StatusCodes({
        @ResponseCode(code = 404, condition = "The containerName passed was not found"),
        @ResponseCode(code = 500, condition = "Removal of subnet failed") })
    public Response removeSubnet(
        @PathParam("containerName") String containerName,
        @PathParam("subnetName") String subnetName) {
        if (subnetName == null) {
            throw new ResourceNotFoundException(RestMessages.INVALIDDATA.toString());
        }

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }

        ISwitchManager switchManager = null;
        switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            throw new ResourceNotFoundException(RestMessages.NOCONTAINER.toString());
        }
        Status status = switchManager.removeSubnet(subnetName);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Subnet Gateway", username, "removed", subnetName, containerName);
            return Response.status(Response.Status.OK).build();
        }
        throw new InternalServerErrorException(status.getDescription());
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
    @Path("/{containerName}/{subnetName}/modify")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
        @ResponseCode(code = 202, condition = "Operation successful"),
        @ResponseCode(code = 400, condition = "Invalid request, i.e., requested changing the subnet name"),
        @ResponseCode(code = 404, condition = "The containerName or subnetName is not found"),
        @ResponseCode(code = 500, condition = "Internal server error") })
    public Response modifySubnet(@PathParam("containerName") String containerName,
        @PathParam("subnetName") String name,
        @TypeHint(SubnetConfig.class) JAXBElement<SubnetConfig> subnetConfigData) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);
        if (switchManager == null) {
            throw new ResourceNotFoundException(RestMessages.NOCONTAINER.toString());
        }

        SubnetConfig subnetConf = subnetConfigData.getValue();
        SubnetConfig existingConf = switchManager.getSubnetConfig(name);

        boolean successful = true;

        // make sure that the name matches an existing subnet and we're not
        // changing the name or subnet IP/mask
        if (existingConf == null) {
            // don't have a subnet by that name
            return Response.status(Response.Status.NOT_FOUND).build();

        } else if (!existingConf.getName().equals(subnetConf.getName())
                || !existingConf.getSubnet().equals(subnetConf.getSubnet())) {
            // can't change the name of a subnet
            return Response.status(Response.Status.BAD_REQUEST).build();

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
                    Status st = switchManager.removePortsFromSubnet(name, s);
                    successful = successful && st.isSuccess();
                }
            }

            // add any remaining ports
            for (String s : newPorts) {
                Status st = switchManager.addPortsToSubnet(name, s);
                successful = successful && st.isSuccess();
                if(successful){
                    NorthboundUtils.auditlog("Subnet Gateway", username, "added", s +" to "+name, containerName);
                }
            }
        }

        if (successful) {
            return Response.status(Response.Status.ACCEPTED).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     *
     * Add ports to a subnet
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
    @Path("/{containerName}/{subnetName}/add")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
        @ResponseCode(code = 202, condition = "Operation successful"),
        @ResponseCode(code = 400, condition = "Invalid request"),
        @ResponseCode(code = 404, condition = "The containerName or subnetName is not found"),
        @ResponseCode(code = 500, condition = "Internal server error") })
    public Response addNodePorts(
            @PathParam("containerName") String containerName,
            @PathParam("subnetName") String name,
            @TypeHint(SubnetConfig.class) JAXBElement<SubnetConfig> subnetConfigData) {

        SubnetConfig subnetConf = subnetConfigData.getValue();
        return addOrDeletePorts(containerName, name, subnetConf, "add");
    }

    /**
     *
     * Delete ports from a subnet
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
    @Path("/{containerName}/{subnetName}/delete")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
        @ResponseCode(code = 202, condition = "Operation successful"),
        @ResponseCode(code = 400, condition = "Invalid request"),
        @ResponseCode(code = 404, condition = "The containerName or subnetName is not found"),
        @ResponseCode(code = 500, condition = "Internal server error") })
    public Response deleteNodePorts(
            @PathParam("containerName") String containerName,
            @PathParam("subnetName") String name,
            @TypeHint(SubnetConfig.class) JAXBElement<SubnetConfig> subnetConfigData) {

        SubnetConfig subnetConf = subnetConfigData.getValue();
        return addOrDeletePorts(containerName, name, subnetConf, "delete");
    }

    /**
    *
    * Add/Delete ports to/from a subnet
    *
    * @param containerName
    *            Name of the Container
    * @param name
    *            Name of the SubnetConfig to be modified
    * @param subnetConfig
    *            the {@link SubnetConfig} structure
    * @param action
    *            add or delete
    * @return If the operation is successful or not
    */
    private Response addOrDeletePorts(String containerName, String name, SubnetConfig subnetConf, String action) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);
        if (switchManager == null) {
            throw new ResourceNotFoundException(RestMessages.NOCONTAINER.toString());
        }

        SubnetConfig existingConf = switchManager.getSubnetConfig(name);

        // make sure that the name matches an existing subnet and we're not
        // changing the name or subnet IP/mask
        if (existingConf == null) {
            // don't have a subnet by that name
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (!existingConf.getName().equals(subnetConf.getName())
                || !existingConf.getSubnet().equals(subnetConf.getSubnet())) {
            // can't change the name of a subnet
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            Status st;
            boolean successful = true;
            Set<String> ports = subnetConf.getNodePorts();

            if (action.equals("add")) {
                // add new ports
                ports.removeAll(existingConf.getNodePorts());
                for (String port : ports) {
                    st = switchManager.addPortsToSubnet(name, port);
                    successful = successful && st.isSuccess();
                    if(successful){
                        NorthboundUtils.auditlog("Subnet Gateway", username, "added",  st +" to "+name, containerName);
                    }
                }
            } else if (action.equals("delete")) {
                // delete existing ports
                ports.retainAll(existingConf.getNodePorts());
                for (String port : ports) {
                    st = switchManager.removePortsFromSubnet(name, port);
                    successful = successful && st.isSuccess();
                    if(successful){
                        NorthboundUtils.auditlog("Subnet Gateway", username, "removed",  st +" from "+name, containerName);
                    }
                }
            } else {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            if (successful) {
                return Response.status(Response.Status.ACCEPTED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
}
