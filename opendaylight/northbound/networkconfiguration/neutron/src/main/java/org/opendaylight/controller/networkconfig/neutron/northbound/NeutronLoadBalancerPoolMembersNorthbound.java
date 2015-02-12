/*
 * Copyright (C) 2014 SDN Hub, LLC.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Srini Seetharaman
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolMemberAware;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPool;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPoolMember;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.BadRequestException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.utils.ServiceHelper;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@Path("/pools/{loadBalancerPoolUUID}/members")
public class NeutronLoadBalancerPoolMembersNorthbound {
    private NeutronLoadBalancerPoolMember extractFields(NeutronLoadBalancerPoolMember o, List<String> fields) {
        return o.extractFields(fields);
    }
/**
 * Returns a list of all LoadBalancerPoolMembers in specified pool
 */
@GET
@Produces({MediaType.APPLICATION_JSON})
@StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "Unauthorized"),
        @ResponseCode(code = 501, condition = "Not Implemented")})

public Response listMembers(
        //Path param
        @PathParam("loadBalancerPoolUUID") String loadBalancerPoolUUID,

        // return fields
        @QueryParam("fields") List<String> fields,

        // OpenStack LoadBalancerPool attributes
        @QueryParam("id") String queryLoadBalancerPoolMemberID,
        @QueryParam("tenant_id") String queryLoadBalancerPoolMemberTenantID,
        @QueryParam("address") String queryLoadBalancerPoolMemberAddress,
        @QueryParam("protocol_port") String queryLoadBalancerPoolMemberProtoPort,
        @QueryParam("admin_state_up") String queryLoadBalancerPoolMemberAdminStateUp,
        @QueryParam("weight") String queryLoadBalancerPoolMemberWeight,
        @QueryParam("subnet_id") String queryLoadBalancerPoolMemberSubnetID,
        @QueryParam("status") String queryLoadBalancerPoolMemberStatus,

        // pagination
        @QueryParam("limit") String limit,
        @QueryParam("marker") String marker,
        @QueryParam("page_reverse") String pageReverse
        // sorting not supported
) {
    INeutronLoadBalancerPoolCRUD loadBalancerPoolInterface = NeutronCRUDInterfaces
            .getINeutronLoadBalancerPoolCRUD(this);
    if (loadBalancerPoolInterface == null) {
        throw new ServiceUnavailableException("LoadBalancerPool CRUD Interface "
                + RestMessages.SERVICEUNAVAILABLE.toString());
    }
    if (!loadBalancerPoolInterface.neutronLoadBalancerPoolExists(loadBalancerPoolUUID)) {
        throw new ResourceNotFoundException("loadBalancerPool UUID does not exist.");
    }
    List<NeutronLoadBalancerPoolMember> members =
                loadBalancerPoolInterface.getNeutronLoadBalancerPool(loadBalancerPoolUUID).getLoadBalancerPoolMembers();
    List<NeutronLoadBalancerPoolMember> ans = new ArrayList<NeutronLoadBalancerPoolMember>();
    Iterator<NeutronLoadBalancerPoolMember> i = members.iterator();
    while (i.hasNext()) {
        NeutronLoadBalancerPoolMember nsg = i.next();
        if ((queryLoadBalancerPoolMemberID == null ||
                queryLoadBalancerPoolMemberID.equals(nsg.getPoolMemberID())) &&
                loadBalancerPoolUUID.equals(nsg.getPoolID()) &&
                (queryLoadBalancerPoolMemberTenantID == null ||
                        queryLoadBalancerPoolMemberTenantID.equals(nsg.getPoolMemberTenantID())) &&
                (queryLoadBalancerPoolMemberAddress == null ||
                        queryLoadBalancerPoolMemberAddress.equals(nsg.getPoolMemberAddress())) &&
                (queryLoadBalancerPoolMemberAdminStateUp == null ||
                        queryLoadBalancerPoolMemberAdminStateUp.equals(nsg.getPoolMemberAdminStateIsUp())) &&
                (queryLoadBalancerPoolMemberWeight == null ||
                        queryLoadBalancerPoolMemberWeight.equals(nsg.getPoolMemberWeight())) &&
                (queryLoadBalancerPoolMemberSubnetID == null ||
                        queryLoadBalancerPoolMemberSubnetID.equals(nsg.getPoolMemberSubnetID())) &&
                (queryLoadBalancerPoolMemberStatus == null ||
                        queryLoadBalancerPoolMemberStatus.equals(nsg.getPoolMemberStatus()))) {
            if (fields.size() > 0) {
                ans.add(extractFields(nsg, fields));
            } else {
                ans.add(nsg);
            }
        }
    }
    return Response.status(200).entity(
            new NeutronLoadBalancerPoolMemberRequest(ans)).build();
}

/**
 * Returns a specific LoadBalancerPoolMember
 */

@Path("{loadBalancerPoolMemberUUID}")
@GET
@Produces({ MediaType.APPLICATION_JSON })
//@TypeHint(OpenStackLoadBalancerPoolMembers.class)
@StatusCodes({
    @ResponseCode(code = 200, condition = "Operation successful"),
    @ResponseCode(code = 401, condition = "Unauthorized"),
    @ResponseCode(code = 404, condition = "Not Found"),
    @ResponseCode(code = 501, condition = "Not Implemented") })
public Response showLoadBalancerPoolMember(
        @PathParam("loadBalancerPoolUUID") String loadBalancerPoolUUID,
        @PathParam("loadBalancerPoolMemberUUID") String loadBalancerPoolMemberUUID,
        // return fields
        @QueryParam("fields") List<String> fields ) {

    INeutronLoadBalancerPoolCRUD loadBalancerPoolInterface = NeutronCRUDInterfaces
            .getINeutronLoadBalancerPoolCRUD(this);
    if (loadBalancerPoolInterface == null) {
        throw new ServiceUnavailableException("LoadBalancerPool CRUD Interface "
                + RestMessages.SERVICEUNAVAILABLE.toString());
    }
    if (!loadBalancerPoolInterface.neutronLoadBalancerPoolExists(loadBalancerPoolUUID)) {
        throw new ResourceNotFoundException("loadBalancerPool UUID does not exist.");
    }
    List<NeutronLoadBalancerPoolMember> members =
                loadBalancerPoolInterface.getNeutronLoadBalancerPool(loadBalancerPoolUUID).getLoadBalancerPoolMembers();
    for (NeutronLoadBalancerPoolMember ans: members) {
        if (!ans.getPoolMemberID().equals(loadBalancerPoolMemberUUID))
            continue;

        if (fields.size() > 0) {
            return Response.status(200).entity(
                new NeutronLoadBalancerPoolMemberRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(200).entity(
                new NeutronLoadBalancerPoolMemberRequest(ans)).build();
        }
    }
    return Response.status(204).build();
}

/**
 * Adds a Member to an LBaaS Pool member
 */
@PUT
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
@StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "Unauthorized"),
        @ResponseCode(code = 404, condition = "Not Found"),
        @ResponseCode(code = 501, condition = "Not Implemented")})
public Response createLoadBalancerPoolMember(
        @PathParam("loadBalancerPoolUUID") String loadBalancerPoolUUID,
        final NeutronLoadBalancerPoolMemberRequest input) {

    INeutronLoadBalancerPoolCRUD loadBalancerPoolInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerPoolCRUD(this);
    if (loadBalancerPoolInterface == null) {
        throw new ServiceUnavailableException("LoadBalancerPool CRUD Interface "
                + RestMessages.SERVICEUNAVAILABLE.toString());
    }
    // Verify that the loadBalancerPool exists, for the member to be added to its cache
    if (!loadBalancerPoolInterface.neutronLoadBalancerPoolExists(loadBalancerPoolUUID)) {
        throw new ResourceNotFoundException("loadBalancerPool UUID does not exist.");
    }
    NeutronLoadBalancerPool singletonPool = loadBalancerPoolInterface.getNeutronLoadBalancerPool(loadBalancerPoolUUID);

    if (input.isSingleton()) {
        NeutronLoadBalancerPoolMember singleton = input.getSingleton();
        singleton.setPoolID(loadBalancerPoolUUID);
        String loadBalancerPoolMemberUUID = singleton.getPoolMemberID();

        /*
         *  Verify that the LoadBalancerPoolMember doesn't already exist.
         */
        List<NeutronLoadBalancerPoolMember> members = singletonPool.getLoadBalancerPoolMembers();
        for (NeutronLoadBalancerPoolMember member: members) {
            if (member.getPoolMemberID().equals(loadBalancerPoolMemberUUID))
                throw new BadRequestException("LoadBalancerPoolMember UUID already exists");
        }

        Object[] instances = ServiceHelper.getGlobalInstances(INeutronLoadBalancerPoolMemberAware.class, this, null);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronLoadBalancerPoolMemberAware service = (INeutronLoadBalancerPoolMemberAware) instance;
                    int status = service.canCreateNeutronLoadBalancerPoolMember(singleton);
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
        if (instances != null) {
            for (Object instance : instances) {
                INeutronLoadBalancerPoolMemberAware service = (INeutronLoadBalancerPoolMemberAware) instance;
                service.neutronLoadBalancerPoolMemberCreated(singleton);
            }
        }

        /**
         * Add the member from the neutron load balancer pool as well
         */
        singletonPool.addLoadBalancerPoolMember(singleton);

    } else {
        List<NeutronLoadBalancerPoolMember> bulk = input.getBulk();
        Iterator<NeutronLoadBalancerPoolMember> i = bulk.iterator();
        HashMap<String, NeutronLoadBalancerPoolMember> testMap = new HashMap<String, NeutronLoadBalancerPoolMember>();
        Object[] instances = ServiceHelper.getGlobalInstances(INeutronLoadBalancerPoolMemberAware.class, this, null);
        while (i.hasNext()) {
            NeutronLoadBalancerPoolMember test = i.next();
            String loadBalancerPoolMemberUUID = test.getPoolMemberID();

            /*
             *  Verify that the LoadBalancerPoolMember doesn't already exist.
             */
            List<NeutronLoadBalancerPoolMember> members = singletonPool.getLoadBalancerPoolMembers();
            for (NeutronLoadBalancerPoolMember member: members) {
                if (member.getPoolMemberID().equals(loadBalancerPoolMemberUUID))
                    throw new BadRequestException("LoadBalancerPoolMember UUID already exists");
            }

            if (testMap.containsKey(test.getPoolMemberID())) {
                throw new BadRequestException("Load Balancer PoolMember UUID already exists");
            }
            if (instances != null) {
                if (instances.length > 0) {
                    for (Object instance : instances) {
                        INeutronLoadBalancerPoolMemberAware service = (INeutronLoadBalancerPoolMemberAware) instance;
                        int status = service.canCreateNeutronLoadBalancerPoolMember(test);
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
        }
        /*
         * now, each element of the bulk request can be added to the cache
         */
        i = bulk.iterator();
        while (i.hasNext()) {
            NeutronLoadBalancerPoolMember test = i.next();
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronLoadBalancerPoolMemberAware service = (INeutronLoadBalancerPoolMemberAware) instance;
                    service.neutronLoadBalancerPoolMemberCreated(test);
                }
            }
            singletonPool.addLoadBalancerPoolMember(test);
        }
    }
    return Response.status(201).entity(input).build();
}

/**
 * Updates a LB member pool
 */

@Path("{loadBalancerPoolMemberUUID}")
@PUT
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 400, condition = "Bad Request"),
        @ResponseCode(code = 401, condition = "Unauthorized"),
        @ResponseCode(code = 403, condition = "Forbidden"),
        @ResponseCode(code = 404, condition = "Not Found"),
        @ResponseCode(code = 501, condition = "Not Implemented") })
public Response updateLoadBalancerPoolMember(
        @PathParam("loadBalancerPoolUUID") String loadBalancerPoolUUID,
        @PathParam("loadBalancerPoolMemberUUID") String loadBalancerPoolMemberUUID,
        final NeutronLoadBalancerPoolMemberRequest input) {

    //TODO: Implement update LB member pool
    return Response.status(501).entity(input).build();
}

/**
 * Deletes a LoadBalancerPoolMember
 */

@Path("{loadBalancerPoolMemberUUID}")
@DELETE
@StatusCodes({
    @ResponseCode(code = 204, condition = "No Content"),
    @ResponseCode(code = 401, condition = "Unauthorized"),
    @ResponseCode(code = 403, condition = "Forbidden"),
    @ResponseCode(code = 404, condition = "Not Found"),
    @ResponseCode(code = 501, condition = "Not Implemented") })
public Response deleteLoadBalancerPoolMember(
        @PathParam("loadBalancerPoolUUID") String loadBalancerPoolUUID,
        @PathParam("loadBalancerPoolMemberUUID") String loadBalancerPoolMemberUUID) {
    INeutronLoadBalancerPoolCRUD loadBalancerPoolInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerPoolCRUD(this);
    if (loadBalancerPoolInterface == null) {
        throw new ServiceUnavailableException("LoadBalancerPool CRUD Interface "
                + RestMessages.SERVICEUNAVAILABLE.toString());
    }

    // Verify that the loadBalancerPool exists, for the member to be removed from its cache
    if (!loadBalancerPoolInterface.neutronLoadBalancerPoolExists(loadBalancerPoolUUID)) {
        throw new ResourceNotFoundException("loadBalancerPool UUID does not exist.");
    }

    //Verify that the LB pool member exists
    NeutronLoadBalancerPoolMember singleton = null;
    List<NeutronLoadBalancerPoolMember> members =
            loadBalancerPoolInterface.getNeutronLoadBalancerPool(loadBalancerPoolUUID).getLoadBalancerPoolMembers();
    for (NeutronLoadBalancerPoolMember member: members) {
        if (member.getPoolMemberID().equals(loadBalancerPoolMemberUUID)) {
            singleton = member;
            break;
        }
    }
    if (singleton == null)
        throw new BadRequestException("LoadBalancerPoolMember UUID does not exist.");

    Object[] instances = ServiceHelper.getGlobalInstances(INeutronLoadBalancerPoolMemberAware.class, this, null);
    if (instances != null) {
        if (instances.length > 0) {
            for (Object instance : instances) {
                INeutronLoadBalancerPoolMemberAware service = (INeutronLoadBalancerPoolMemberAware) instance;
                int status = service.canDeleteNeutronLoadBalancerPoolMember(singleton);
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

    if (instances != null) {
        for (Object instance : instances) {
            INeutronLoadBalancerPoolMemberAware service = (INeutronLoadBalancerPoolMemberAware) instance;
            service.neutronLoadBalancerPoolMemberDeleted(singleton);
        }
    }

    /**
     * Remove the member from the neutron load balancer pool
     */
    NeutronLoadBalancerPool singletonPool = loadBalancerPoolInterface.getNeutronLoadBalancerPool(loadBalancerPoolUUID);
    singletonPool.removeLoadBalancerPoolMember(singleton);

    return Response.status(204).build();
}
}
