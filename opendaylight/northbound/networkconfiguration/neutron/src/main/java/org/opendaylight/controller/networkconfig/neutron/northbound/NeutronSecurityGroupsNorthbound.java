/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroup;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.BadRequestException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Neutron Northbound REST APIs for Security Group.<br>
 * This class provides REST APIs for managing neutron Security Group
 * <p/>
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
@Path ("/security-groups")
public class NeutronSecurityGroupsNorthbound {
    static final Logger logger = LoggerFactory.getLogger(NeutronSecurityGroupsNorthbound.class);

    private NeutronSecurityGroup extractFields(NeutronSecurityGroup o, List<String> fields) {
        return o.extractFields(fields);
    }

    /**
     * Returns a list of all Security Groups
     */
    @GET
    @Produces ({MediaType.APPLICATION_JSON})
    @StatusCodes ({
            @ResponseCode (code = 200, condition = "Operation successful"),
            @ResponseCode (code = 401, condition = "Unauthorized"),
            @ResponseCode (code = 501, condition = "Not Implemented")})

    public Response listGroups(
            // return fields
            @QueryParam ("fields") List<String> fields,
            // OpenStack security group attributes
            @QueryParam ("id") String querySecurityGroupUUID,
            @QueryParam ("name") String querySecurityGroupName,
            @QueryParam ("description") String querySecurityDescription,
            @QueryParam ("tenant_id") String querySecurityTenantID,
            @QueryParam ("limit") String limit,
            @QueryParam ("marker") String marker,
            @QueryParam ("page_reverse") String pageReverse
    ) {
        INeutronSecurityGroupCRUD securityGroupInterface = NeutronCRUDInterfaces.getINeutronSecurityGroupCRUD(this);

        if (securityGroupInterface == null) {
            throw new ServiceUnavailableException("Security Group CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronSecurityGroup> allSecurityGroups = securityGroupInterface.getAllNeutronSecurityGroups();
        List<NeutronSecurityGroup> ans = new ArrayList<NeutronSecurityGroup>();
        Iterator<NeutronSecurityGroup> i = allSecurityGroups.iterator();
        while (i.hasNext()) {
            NeutronSecurityGroup nsg = i.next();
            if ((querySecurityGroupUUID == null ||
                    querySecurityGroupUUID.equals(nsg.getSecurityGroupUUID())) &&
                    (querySecurityGroupName == null ||
                            querySecurityGroupName.equals(nsg.getSecurityGroupName())) &&
                    (querySecurityDescription == null ||
                            querySecurityDescription.equals(nsg.getSecurityGroupDescription())) &&
                    (querySecurityTenantID == null ||
                            querySecurityTenantID.equals(nsg.getSecurityGroupTenantID()))) {
                if (fields.size() > 0) {
                    ans.add(extractFields(nsg, fields));
                } else {
                    ans.add(nsg);
                }
            }
        }
        return Response.status(200).entity(
                new NeutronSecurityGroupRequest(ans)).build();
    }

    /**
     * Returns a specific Security Group
     */

    @Path ("{securityGroupUUID}")
    @GET
    @Produces ({MediaType.APPLICATION_JSON})
    @StatusCodes ({
            @ResponseCode (code = 200, condition = "Operation successful"),
            @ResponseCode (code = 401, condition = "Unauthorized"),
            @ResponseCode (code = 404, condition = "Not Found"),
            @ResponseCode (code = 501, condition = "Not Implemented")})
    public Response showSecurityGroup(@PathParam ("securityGroupUUID") String securityGroupUUID,
                                      // return fields
                                      @QueryParam ("fields") List<String> fields) {
        INeutronSecurityGroupCRUD securityGroupInterface = NeutronCRUDInterfaces.getINeutronSecurityGroupCRUD(this);
        if (securityGroupInterface == null) {
            throw new ServiceUnavailableException("Security Group CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!securityGroupInterface.neutronSecurityGroupExists(securityGroupUUID)) {
            throw new ResourceNotFoundException("Security Group UUID does not exist.");
        }
        if (!fields.isEmpty()) {
            NeutronSecurityGroup ans = securityGroupInterface.getNeutronSecurityGroup(securityGroupUUID);
            return Response.status(200).entity(
                    new NeutronSecurityGroupRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(200).entity(new NeutronSecurityGroupRequest(securityGroupInterface.getNeutronSecurityGroup(securityGroupUUID))).build();
        }
    }

    /**
     * Creates new Security Group
     */

    @POST
    @Produces ({MediaType.APPLICATION_JSON})
    @Consumes ({MediaType.APPLICATION_JSON})
    @StatusCodes ({
            @ResponseCode (code = 201, condition = "Created"),
            @ResponseCode (code = 400, condition = "Bad Request"),
            @ResponseCode (code = 401, condition = "Unauthorized"),
            @ResponseCode (code = 403, condition = "Forbidden"),
            @ResponseCode (code = 404, condition = "Not Found"),
            @ResponseCode (code = 409, condition = "Conflict"),
            @ResponseCode (code = 501, condition = "Not Implemented")})
    public Response createSecurityGroups(final NeutronSecurityGroupRequest input) {
        INeutronSecurityGroupCRUD securityGroupInterface = NeutronCRUDInterfaces.getINeutronSecurityGroupCRUD(this);
        if (securityGroupInterface == null) {
            throw new ServiceUnavailableException("Security Group CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        if (input.isSingleton()) {
            NeutronSecurityGroup singleton = input.getSingleton();

            /*
             *  Verify that the Security Group doesn't already exist.
             */
            if (securityGroupInterface.neutronSecurityGroupExists(singleton.getSecurityGroupUUID())) {
                throw new BadRequestException("Security Group UUID already exists");
            }

            Object[] instances = ServiceHelper.getGlobalInstances(INeutronSecurityGroupAware.class, this, null);
            if (instances != null) {
		if (instances.length > 0) {
                    for (Object instance : instances) {
                        INeutronSecurityGroupAware service = (INeutronSecurityGroupAware) instance;
                        int status = service.canCreateNeutronSecurityGroup(singleton);
                        if (status < 200 || status > 299) {
                            return Response.status(status).build();
                        }
                    }
                } else {
                    throw new ServiceUnavailableException("No providers registered.  Please try again later");
                }
            } else {
                throw new ServiceUnavailableException("Couldn't get providers list.  Please try again later");
            }
            // Add to Neutron cache
            securityGroupInterface.addNeutronSecurityGroup(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronSecurityGroupAware service = (INeutronSecurityGroupAware) instance;
                    service.neutronSecurityGroupCreated(singleton);
                }
            }
        } else {
            List<NeutronSecurityGroup> bulk = input.getBulk();
            Iterator<NeutronSecurityGroup> i = bulk.iterator();
            HashMap<String, NeutronSecurityGroup> testMap = new HashMap<String, NeutronSecurityGroup>();
            Object[] instances = ServiceHelper.getGlobalInstances(INeutronSecurityGroupAware.class, this, null);
            while (i.hasNext()) {
                NeutronSecurityGroup test = i.next();

                /*
                 *  Verify that the security group doesn't already exist
                 */

                if (securityGroupInterface.neutronSecurityGroupExists(test.getSecurityGroupUUID())) {
                    throw new BadRequestException("Security Group UUID already is already created");
                }
                if (instances != null) {
                    if (instances.length > 0) {
                        for (Object instance : instances) {
                            INeutronSecurityGroupAware service = (INeutronSecurityGroupAware) instance;
                            int status = service.canCreateNeutronSecurityGroup(test);
                            if ((status < 200) || (status > 299)) return Response.status(status).build();
                        }
                    } else {
                        throw new BadRequestException("No providers registered.  Please try again later");
                    }
                } else {
   		throw new ServiceUnavailableException("Couldn't get providers list.  Please try again later");
                }
            }

            /*
             * now, each element of the bulk request can be added to the cache
             */
            i = bulk.iterator();
            while (i.hasNext()) {
                NeutronSecurityGroup test = i.next();
                securityGroupInterface.addNeutronSecurityGroup(test);
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronSecurityGroupAware service = (INeutronSecurityGroupAware) instance;
                        service.neutronSecurityGroupCreated(test);
                    }
                }
            }
        }
        return Response.status(201).entity(input).build();
    }

    /**
     * Updates a Security Group
     */

    @Path ("{securityGroupUUID}")
    @PUT
    @Produces ({MediaType.APPLICATION_JSON})
    @Consumes ({MediaType.APPLICATION_JSON})
    @StatusCodes ({
            @ResponseCode (code = 200, condition = "Operation successful"),
            @ResponseCode (code = 400, condition = "Bad Request"),
            @ResponseCode (code = 401, condition = "Unauthorized"),
            @ResponseCode (code = 403, condition = "Forbidden"),
            @ResponseCode (code = 404, condition = "Not Found"),
            @ResponseCode (code = 501, condition = "Not Implemented")})
    public Response updateSecurityGroup(
            @PathParam ("securityGroupUUID") String securityGroupUUID, final NeutronSecurityGroupRequest input) {
        INeutronSecurityGroupCRUD securityGroupInterface = NeutronCRUDInterfaces.getINeutronSecurityGroupCRUD(this);
        if (securityGroupInterface == null) {
            throw new ServiceUnavailableException("Security Group CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the Security Group exists and there is only one delta provided
         */
        if (!securityGroupInterface.neutronSecurityGroupExists(securityGroupUUID)) {
            throw new ResourceNotFoundException("Security Group UUID does not exist.");
        }
        if (!input.isSingleton()) {
            throw new BadRequestException("Only singleton edit supported");
        }
        NeutronSecurityGroup delta = input.getSingleton();
        NeutronSecurityGroup original = securityGroupInterface.getNeutronSecurityGroup(securityGroupUUID);

        if (delta.getSecurityGroupUUID() != null ||
                delta.getSecurityGroupTenantID() != null ||
                delta.getSecurityGroupName() != null ||
                delta.getSecurityGroupDescription() != null) {
            throw new BadRequestException("Attribute edit blocked by Neutron");
        }

        Object[] instances = ServiceHelper.getGlobalInstances(INeutronSecurityGroupAware.class, this, null);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronSecurityGroupAware service = (INeutronSecurityGroupAware) instance;
                    int status = service.canUpdateNeutronSecurityGroup(delta, original);
                    if (status < 200 || status > 299) {
                        return Response.status(status).build();
                    }
                }
            } else {
                throw new ServiceUnavailableException("No providers registered.  Please try again later");
            }
        } else {
            throw new ServiceUnavailableException("Couldn't get providers list.  Please try again later");
        }

        /*
         * update the object and return it
         */
        securityGroupInterface.updateNeutronSecurityGroup(securityGroupUUID, delta);
        NeutronSecurityGroup updatedSecurityGroup = securityGroupInterface.getNeutronSecurityGroup(securityGroupUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronSecurityGroupAware service = (INeutronSecurityGroupAware) instance;
                service.neutronSecurityGroupUpdated(updatedSecurityGroup);
            }
        }
        return Response.status(200).entity(new NeutronSecurityGroupRequest(securityGroupInterface.getNeutronSecurityGroup(securityGroupUUID))).build();
    }

    /**
     * Deletes a Security Group
     */

    @Path ("{securityGroupUUID}")
    @DELETE
    @StatusCodes ({
            @ResponseCode (code = 204, condition = "No Content"),
            @ResponseCode (code = 401, condition = "Unauthorized"),
            @ResponseCode (code = 404, condition = "Not Found"),
            @ResponseCode (code = 409, condition = "Conflict"),
            @ResponseCode (code = 501, condition = "Not Implemented")})
    public Response deleteSecurityGroup(
            @PathParam ("securityGroupUUID") String securityGroupUUID) {
        INeutronSecurityGroupCRUD securityGroupInterface = NeutronCRUDInterfaces.getINeutronSecurityGroupCRUD(this);
        if (securityGroupInterface == null) {
            throw new ServiceUnavailableException("Security Group CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the Security Group exists and it isn't currently in use
         */
        if (!securityGroupInterface.neutronSecurityGroupExists(securityGroupUUID)) {
            throw new ResourceNotFoundException("Security Group UUID does not exist.");
        }
        if (securityGroupInterface.neutronSecurityGroupInUse(securityGroupUUID)) {
            return Response.status(409).build();
        }
        NeutronSecurityGroup singleton = securityGroupInterface.getNeutronSecurityGroup(securityGroupUUID);
        Object[] instances = ServiceHelper.getGlobalInstances(INeutronSecurityGroupAware.class, this, null);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronSecurityGroupAware service = (INeutronSecurityGroupAware) instance;
                    int status = service.canDeleteNeutronSecurityGroup(singleton);
                    if ((status < 200) || (status > 299)) {
                        return Response.status(status).build();
                    }
                }
            } else {
                throw new ServiceUnavailableException("No providers registered.  Please try again later");
	    }
        } else {
            throw new ServiceUnavailableException("Couldn't get providers list.  Please try again later");
        }

        /*
         * remove it and return 204 status
         */
        securityGroupInterface.removeNeutronSecurityGroup(securityGroupUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronSecurityGroupAware service = (INeutronSecurityGroupAware) instance;
                service.neutronSecurityGroupDeleted(singleton);
            }
        }
        return Response.status(204).build();
    }
}