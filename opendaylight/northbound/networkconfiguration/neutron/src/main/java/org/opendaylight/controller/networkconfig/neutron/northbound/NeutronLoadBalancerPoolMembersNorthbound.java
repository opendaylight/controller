/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolMemberAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolMemberCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPoolMember;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.BadRequestException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.utils.ServiceHelper;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


@Path("/pools/{loadBalancerPoolID}/members")
public class NeutronLoadBalancerPoolMembersNorthbound {

    private NeutronLoadBalancerPoolMember extractFields(NeutronLoadBalancerPoolMember o, List<String> fields) {
        return o.extractFields(fields);
    }
/**
 * Returns a list of all LoadBalancerPool
 */
@GET
@Produces({MediaType.APPLICATION_JSON})
@StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "Unauthorized"),
        @ResponseCode(code = 501, condition = "Not Implemented")})

public Response listMembers(
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
    INeutronLoadBalancerPoolMemberCRUD loadBalancerPoolMemberInterface = NeutronCRUDInterfaces
            .getINeutronLoadBalancerPoolMemberCRUD(this);
    if (loadBalancerPoolMemberInterface == null) {
        throw new ServiceUnavailableException("LoadBalancerPool CRUD Interface "
                + RestMessages.SERVICEUNAVAILABLE.toString());
    }
    List<NeutronLoadBalancerPoolMember> allLoadBalancerPoolMembers = loadBalancerPoolMemberInterface
            .getAllNeutronLoadBalancerPoolMembers();
    List<NeutronLoadBalancerPoolMember> ans = new ArrayList<NeutronLoadBalancerPoolMember>();
    Iterator<NeutronLoadBalancerPoolMember> i = allLoadBalancerPoolMembers.iterator();
    while (i.hasNext()) {
        NeutronLoadBalancerPoolMember nsg = i.next();
        if ((queryLoadBalancerPoolMemberID == null ||
                queryLoadBalancerPoolMemberID.equals(nsg.getPoolMemberID())) &&
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
            new INeutronLoadBalancerPoolMemberRequest(ans)).build();
}

/**
 * Adds a Member to an LBaaS Pool member
 */
@Path("/pools/{loadBalancerPoolID}/members")
@PUT
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
@StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "Unauthorized"),
        @ResponseCode(code = 404, condition = "Not Found"),
        @ResponseCode(code = 501, condition = "Not Implemented")})
public Response createLoadBalancerPoolMember(  INeutronLoadBalancerPoolMemberRequest input) {

    INeutronLoadBalancerPoolMemberCRUD loadBalancerPoolMemberInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerPoolMemberCRUD(
            this);
    if (loadBalancerPoolMemberInterface == null) {
        throw new ServiceUnavailableException("LoadBalancerPoolMember CRUD Interface "
                + RestMessages.SERVICEUNAVAILABLE.toString());
    }
    if (input.isSingleton()) {
        NeutronLoadBalancerPoolMember singleton = input.getSingleton();

        /*
         *  Verify that the LoadBalancerPoolMember doesn't already exist.
         */
        if (loadBalancerPoolMemberInterface.neutronLoadBalancerPoolMemberExists(
                singleton.getPoolMemberID())) {
            throw new BadRequestException("LoadBalancerPoolMember UUID already exists");
        }
        loadBalancerPoolMemberInterface.addNeutronLoadBalancerPoolMember(singleton);

        Object[] instances = ServiceHelper.getGlobalInstances(INeutronLoadBalancerPoolMemberAware.class, this, null);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronLoadBalancerPoolMemberAware service = (INeutronLoadBalancerPoolMemberAware) instance;
                int status = service.canCreateNeutronLoadBalancerPoolMember(singleton);
                if (status < 200 || status > 299) {
                    return Response.status(status).build();
                }
            }
        }
        loadBalancerPoolMemberInterface.addNeutronLoadBalancerPoolMember(singleton);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronLoadBalancerPoolMemberAware service = (INeutronLoadBalancerPoolMemberAware) instance;
                service.neutronLoadBalancerPoolMemberCreated(singleton);
            }
        }
    } else {
        List<NeutronLoadBalancerPoolMember> bulk = input.getBulk();
        Iterator<NeutronLoadBalancerPoolMember> i = bulk.iterator();
        HashMap<String, NeutronLoadBalancerPoolMember> testMap = new HashMap<String, NeutronLoadBalancerPoolMember>();
        Object[] instances = ServiceHelper.getGlobalInstances(INeutronLoadBalancerPoolMemberAware.class, this, null);
        while (i.hasNext()) {
            NeutronLoadBalancerPoolMember test = i.next();

            /*
             *  Verify that the firewall doesn't already exist
             */

            if (loadBalancerPoolMemberInterface.neutronLoadBalancerPoolMemberExists(
                    test.getPoolMemberID())) {
                throw new BadRequestException("Load Balancer PoolMember UUID already is already created");
            }
            if (testMap.containsKey(test.getPoolMemberID())) {
                throw new BadRequestException("Load Balancer PoolMember UUID already exists");
            }
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronLoadBalancerPoolMemberAware service = (INeutronLoadBalancerPoolMemberAware) instance;
                    int status = service.canCreateNeutronLoadBalancerPoolMember(test);
                    if (status < 200 || status > 299) {
                        return Response.status(status).build();
                    }
                }
            }
        }
        /*
         * now, each element of the bulk request can be added to the cache
         */
        i = bulk.iterator();
        while (i.hasNext()) {
            NeutronLoadBalancerPoolMember test = i.next();
            loadBalancerPoolMemberInterface.addNeutronLoadBalancerPoolMember(test);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronLoadBalancerPoolMemberAware service = (INeutronLoadBalancerPoolMemberAware) instance;
                    service.neutronLoadBalancerPoolMemberCreated(test);
                }
            }
        }
    }
    return Response.status(201).entity(input).build();
}
}
