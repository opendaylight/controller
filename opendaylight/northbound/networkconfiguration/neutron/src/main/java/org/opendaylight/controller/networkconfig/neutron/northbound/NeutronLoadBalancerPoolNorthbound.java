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
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolCRUD;
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
import javax.ws.rs.POST;
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

/**
 * Neutron Northbound REST APIs for LoadBalancerPool Policies.<br>
 * This class provides REST APIs for managing neutron LoadBalancerPool Policies
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

/**
 * For now, the LB pool member data is maintained with the INeutronLoadBalancerPoolCRUD,
 * and not duplicated within the INeutronLoadBalancerPoolMemberCRUD's cache.
 */

@Path("/pools")
public class NeutronLoadBalancerPoolNorthbound {

    private NeutronLoadBalancerPool extractFields(NeutronLoadBalancerPool o, List<String> fields) {
        return o.extractFields(fields);
    }

    /**
     * Returns a list of all LoadBalancerPool
     * */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 501, condition = "Not Implemented") })

    public Response listGroups(
            // return fields
            @QueryParam("fields") List<String> fields,
            // OpenStack LoadBalancerPool attributes
            @QueryParam("id") String queryLoadBalancerPoolID,
            @QueryParam("tenant_id") String queryLoadBalancerPoolTenantID,
            @QueryParam("name") String queryLoadBalancerPoolName,
            @QueryParam("description") String queryLoadBalancerDescription,
            @QueryParam("protocol") String queryLoadBalancerProtocol,
            @QueryParam("lb_algorithm") String queryLoadBalancerPoolLbAlgorithm,
            @QueryParam("healthmonitor_id") String queryLoadBalancerPoolHealthMonitorID,
            @QueryParam("admin_state_up") String queryLoadBalancerIsAdminStateUp,
            @QueryParam("status") String queryLoadBalancerPoolStatus,
            @QueryParam("members") List<NeutronLoadBalancerPoolMember> queryLoadBalancerPoolMembers,
            // pagination
            @QueryParam("limit") String limit,
            @QueryParam("marker") String marker,
            @QueryParam("page_reverse") String pageReverse
            // sorting not supported
    ) {
        INeutronLoadBalancerPoolCRUD loadBalancerPoolInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerPoolCRUD(this);
        if (loadBalancerPoolInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerPool CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronLoadBalancerPool> allLoadBalancerPools = loadBalancerPoolInterface.getAllNeutronLoadBalancerPools();
        List<NeutronLoadBalancerPool> ans = new ArrayList<NeutronLoadBalancerPool>();
        Iterator<NeutronLoadBalancerPool> i = allLoadBalancerPools.iterator();
        while (i.hasNext()) {
            NeutronLoadBalancerPool nsg = i.next();
            if ((queryLoadBalancerPoolID == null ||
                    queryLoadBalancerPoolID.equals(nsg.getLoadBalancerPoolID())) &&
                    (queryLoadBalancerPoolTenantID == null ||
                            queryLoadBalancerPoolTenantID.equals(nsg.getLoadBalancerPoolTenantID())) &&
                    (queryLoadBalancerPoolName == null ||
                            queryLoadBalancerPoolName.equals(nsg.getLoadBalancerPoolName())) &&
                    (queryLoadBalancerDescription == null ||
                            queryLoadBalancerDescription.equals(nsg.getLoadBalancerPoolDescription())) &&
                    (queryLoadBalancerPoolLbAlgorithm == null ||
                            queryLoadBalancerPoolLbAlgorithm.equals(nsg.getLoadBalancerPoolLbAlgorithm())) &&
                    (queryLoadBalancerPoolHealthMonitorID == null ||
                            queryLoadBalancerPoolHealthMonitorID.equals(nsg.getNeutronLoadBalancerPoolHealthMonitorID())) &&
                    (queryLoadBalancerIsAdminStateUp == null ||
                            queryLoadBalancerIsAdminStateUp.equals(nsg.getLoadBalancerPoolAdminIsStateIsUp())) &&
                    (queryLoadBalancerPoolStatus == null ||
                            queryLoadBalancerPoolStatus.equals(nsg.getLoadBalancerPoolStatus())) &&
                    (queryLoadBalancerPoolMembers.size() == 0 ||
                            queryLoadBalancerPoolMembers.equals(nsg.getLoadBalancerPoolMembers()))) {
                if (fields.size() > 0) {
                    ans.add(extractFields(nsg,fields));
                } else {
                    ans.add(nsg);
                }
            }
        }
        return Response.status(200).entity(
                new NeutronLoadBalancerPoolRequest(ans)).build();
    }

    /**
     * Returns a specific LoadBalancerPool */

    @Path("{loadBalancerPoolID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response showLoadBalancerPool(@PathParam("loadBalancerPoolID") String loadBalancerPoolID,
            // return fields
            @QueryParam("fields") List<String> fields) {
        INeutronLoadBalancerPoolCRUD loadBalancerPoolInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerPoolCRUD(this);
        if (loadBalancerPoolInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerPool CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!loadBalancerPoolInterface.neutronLoadBalancerPoolExists(loadBalancerPoolID)) {
            throw new ResourceNotFoundException("LoadBalancerPool UUID does not exist.");
        }
        if (fields.size() > 0) {
            NeutronLoadBalancerPool ans = loadBalancerPoolInterface.getNeutronLoadBalancerPool(loadBalancerPoolID);
            return Response.status(200).entity(
                    new NeutronLoadBalancerPoolRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(200).entity(new NeutronLoadBalancerPoolRequest(loadBalancerPoolInterface.getNeutronLoadBalancerPool(loadBalancerPoolID))).build();
        }
    }

    /**
     * Creates new LoadBalancerPool */

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 201, condition = "Created"),
            @ResponseCode(code = 400, condition = "Bad Request"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 403, condition = "Forbidden"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response createLoadBalancerPools(final NeutronLoadBalancerPoolRequest input) {
        INeutronLoadBalancerPoolCRUD loadBalancerPoolInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerPoolCRUD(this);
        if (loadBalancerPoolInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerPool CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (input.isSingleton()) {
            NeutronLoadBalancerPool singleton = input.getSingleton();

            /*
             *  Verify that the LoadBalancerPool doesn't already exist.
             */
            if (loadBalancerPoolInterface.neutronLoadBalancerPoolExists(singleton.getLoadBalancerPoolID())) {
                throw new BadRequestException("LoadBalancerPool UUID already exists");
            }
            Object[] instances = ServiceHelper.getGlobalInstances(INeutronLoadBalancerPoolAware.class, this, null);
            if (instances != null) {
                if (instances.length > 0) {
                    for (Object instance : instances) {
                        INeutronLoadBalancerPoolAware service = (INeutronLoadBalancerPoolAware) instance;
                        int status = service.canCreateNeutronLoadBalancerPool(singleton);
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
            loadBalancerPoolInterface.addNeutronLoadBalancerPool(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronLoadBalancerPoolAware service = (INeutronLoadBalancerPoolAware) instance;
                    service.neutronLoadBalancerPoolCreated(singleton);
                }
            }
        } else {
            List<NeutronLoadBalancerPool> bulk = input.getBulk();
            Iterator<NeutronLoadBalancerPool> i = bulk.iterator();
            HashMap<String, NeutronLoadBalancerPool> testMap = new HashMap<String, NeutronLoadBalancerPool>();
            Object[] instances = ServiceHelper.getGlobalInstances(INeutronLoadBalancerPoolAware.class, this, null);
            while (i.hasNext()) {
                NeutronLoadBalancerPool test = i.next();

                /*
                 *  Verify that the loadBalancerPool doesn't already exist
                 */

                if (loadBalancerPoolInterface.neutronLoadBalancerPoolExists(test.getLoadBalancerPoolID())) {
                    throw new BadRequestException("Load Balancer Pool UUID already is already created");
                }
                if (testMap.containsKey(test.getLoadBalancerPoolID())) {
                    throw new BadRequestException("Load Balancer Pool UUID already exists");
                }
                if (instances != null) {
                    if (instances.length > 0) {
                        for (Object instance : instances) {
                            INeutronLoadBalancerPoolAware service = (INeutronLoadBalancerPoolAware) instance;
                            int status = service.canCreateNeutronLoadBalancerPool(test);
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
                NeutronLoadBalancerPool test = i.next();
                loadBalancerPoolInterface.addNeutronLoadBalancerPool(test);
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronLoadBalancerPoolAware service = (INeutronLoadBalancerPoolAware) instance;
                        service.neutronLoadBalancerPoolCreated(test);
                    }
                }
            }
        }
        return Response.status(201).entity(input).build();
    }

    /**
     * Updates a LoadBalancerPool Policy
     */
    @Path("{loadBalancerPoolID}")
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
    public Response updateLoadBalancerPool(
            @PathParam("loadBalancerPoolID") String loadBalancerPoolID, final NeutronLoadBalancerPoolRequest input) {
        INeutronLoadBalancerPoolCRUD loadBalancerPoolInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerPoolCRUD(this);
        if (loadBalancerPoolInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerPool CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the LoadBalancerPool exists and there is only one delta provided
         */
        if (!loadBalancerPoolInterface.neutronLoadBalancerPoolExists(loadBalancerPoolID)) {
            throw new ResourceNotFoundException("LoadBalancerPool UUID does not exist.");
        }
        if (!input.isSingleton()) {
            throw new BadRequestException("Only singleton edit supported");
        }
        NeutronLoadBalancerPool delta = input.getSingleton();
        NeutronLoadBalancerPool original = loadBalancerPoolInterface.getNeutronLoadBalancerPool(loadBalancerPoolID);

        /*
         * updates restricted by Neutron
         */
        if (delta.getLoadBalancerPoolID() != null ||
                delta.getLoadBalancerPoolTenantID() != null ||
                delta.getLoadBalancerPoolName() != null ||
                delta.getLoadBalancerPoolDescription() != null ||
                delta.getLoadBalancerPoolProtocol() != null ||
                delta.getLoadBalancerPoolLbAlgorithm() != null ||
                delta.getNeutronLoadBalancerPoolHealthMonitorID() != null ||
                delta.getLoadBalancerPoolAdminIsStateIsUp() != null ||
                delta.getLoadBalancerPoolStatus() != null ||
                delta.getLoadBalancerPoolMembers() != null) {
            throw new BadRequestException("Attribute edit blocked by Neutron");
        }

        Object[] instances = ServiceHelper.getGlobalInstances(INeutronLoadBalancerPoolAware.class, this, null);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronLoadBalancerPoolAware service = (INeutronLoadBalancerPoolAware) instance;
                    int status = service.canUpdateNeutronLoadBalancerPool(delta, original);
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
        loadBalancerPoolInterface.updateNeutronLoadBalancerPool(loadBalancerPoolID, delta);
        NeutronLoadBalancerPool updatedLoadBalancerPool = loadBalancerPoolInterface.getNeutronLoadBalancerPool(loadBalancerPoolID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronLoadBalancerPoolAware service = (INeutronLoadBalancerPoolAware) instance;
                service.neutronLoadBalancerPoolUpdated(updatedLoadBalancerPool);
            }
        }
        return Response.status(200).entity(new NeutronLoadBalancerPoolRequest(loadBalancerPoolInterface.getNeutronLoadBalancerPool(loadBalancerPoolID))).build();
    }

    /**
     * Deletes a LoadBalancerPool
     */

    @Path("{loadBalancerPoolUUID}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 204, condition = "No Content"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response deleteLoadBalancerPool(
            @PathParam("loadBalancerPoolUUID") String loadBalancerPoolUUID) {
        INeutronLoadBalancerPoolCRUD loadBalancerPoolInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerPoolCRUD(this);
        if (loadBalancerPoolInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerPool CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the LoadBalancerPool exists and it isn't currently in use
         */
        if (!loadBalancerPoolInterface.neutronLoadBalancerPoolExists(loadBalancerPoolUUID)) {
            throw new ResourceNotFoundException("LoadBalancerPool UUID does not exist.");
        }
        if (loadBalancerPoolInterface.neutronLoadBalancerPoolInUse(loadBalancerPoolUUID)) {
            return Response.status(409).build();
        }
        NeutronLoadBalancerPool singleton = loadBalancerPoolInterface.getNeutronLoadBalancerPool(loadBalancerPoolUUID);
        Object[] instances = ServiceHelper.getGlobalInstances(INeutronLoadBalancerPoolAware.class, this, null);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronLoadBalancerPoolAware service = (INeutronLoadBalancerPoolAware) instance;
                    int status = service.canDeleteNeutronLoadBalancerPool(singleton);
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
         * remove it and return 204 status
         */
        loadBalancerPoolInterface.removeNeutronLoadBalancerPool(loadBalancerPoolUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronLoadBalancerPoolAware service = (INeutronLoadBalancerPoolAware) instance;
                service.neutronLoadBalancerPoolDeleted(singleton);
            }
        }
        return Response.status(204).build();
    }
}
