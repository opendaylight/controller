/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerListenerAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerListenerCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerListener;

/**
 * Neutron Northbound REST APIs for LoadBalancerListener Policies.<br>
 * This class provides REST APIs for managing neutron LoadBalancerListener Policies
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
@Path("/listeners")
public class NeutronLoadBalancerListenerNorthbound {

    private NeutronLoadBalancerListener extractFields(NeutronLoadBalancerListener o, List<String> fields) {
        return o.extractFields(fields);
    }

    /**
     * Returns a list of all LoadBalancerListener */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 501, condition = "Not Implemented") })

    public Response listGroups(
            // return fields
            @QueryParam("fields") List<String> fields,
            // OpenStack LoadBalancerListener attributes
            @QueryParam("id") String queryLoadBalancerListenerID,
            @QueryParam("default_pool_id") String queryLoadBalancerListenerDefaultPoolID,
            @QueryParam("tenant_id") String queryLoadBalancerListenerTenantID,
            @QueryParam("name") String queryLoadBalancerListenerName,
            @QueryParam("description") String queryLoadBalancerListenerDescription,
            @QueryParam("shared") String queryLoadBalancerListenerIsShared,
            @QueryParam("protocol") String queryLoadBalancerListenerProtocol,
            @QueryParam("protocol_port") String queryLoadBalancerListenerProtocolPort,
            @QueryParam("load_balancer_id") String queryLoadBalancerListenerLoadBalancerID,
            @QueryParam("admin_state_up") String queryLoadBalancerListenerAdminIsUp,
            @QueryParam("status") String queryLoadBalancerListenerStatus,
            // pagination
            @QueryParam("limit") String limit,
            @QueryParam("marker") String marker,
            @QueryParam("page_reverse") String pageReverse
            // sorting not supported
    ) {
        INeutronLoadBalancerListenerCRUD loadBalancerListenerInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerListenerCRUD(this);
        //        INeutronLoadBalancerListenerRuleCRUD firewallRuleInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerListenerRuleCRUD(this);

        if (loadBalancerListenerInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerListener CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronLoadBalancerListener> allLoadBalancerListeners = loadBalancerListenerInterface.getAllNeutronLoadBalancerListeners();
        //        List<NeutronLoadBalancerListenerRule> allLoadBalancerListenerRules = firewallRuleInterface.getAllNeutronLoadBalancerListenerRules();
        List<NeutronLoadBalancerListener> ans = new ArrayList<NeutronLoadBalancerListener>();
        //        List<NeutronLoadBalancerListenerRule> rules = new ArrayList<NeutronLoadBalancerListenerRule>();
        Iterator<NeutronLoadBalancerListener> i = allLoadBalancerListeners.iterator();
        while (i.hasNext()) {
            NeutronLoadBalancerListener nsg = i.next();
            if ((queryLoadBalancerListenerID == null ||
                    queryLoadBalancerListenerID.equals(nsg.getLoadBalancerListenerID())) &&
                    (queryLoadBalancerListenerDefaultPoolID == null ||
                            queryLoadBalancerListenerDefaultPoolID.equals(nsg.getNeutronLoadBalancerListenerDefaultPoolID())) &&
                    (queryLoadBalancerListenerTenantID == null ||
                            queryLoadBalancerListenerTenantID.equals(nsg.getLoadBalancerListenerTenantID())) &&
                    (queryLoadBalancerListenerName == null ||
                            queryLoadBalancerListenerName.equals(nsg.getLoadBalancerListenerName())) &&
                    (queryLoadBalancerListenerDescription == null ||
                            queryLoadBalancerListenerDescription.equals(nsg.getLoadBalancerListenerDescription())) &&
                    (queryLoadBalancerListenerIsShared == null ||
                            queryLoadBalancerListenerIsShared.equals(nsg.getLoadBalancerListenerIsShared())) &&
                    (queryLoadBalancerListenerProtocol == null ||
                            queryLoadBalancerListenerProtocol.equals(nsg.getNeutronLoadBalancerListenerProtocol())) &&
                    (queryLoadBalancerListenerProtocolPort == null ||
                            queryLoadBalancerListenerProtocolPort.equals(nsg.getNeutronLoadBalancerListenerProtocolPort())) &&
                    (queryLoadBalancerListenerLoadBalancerID == null ||
                            queryLoadBalancerListenerLoadBalancerID.equals(nsg.getNeutronLoadBalancerListenerLoadBalancerID())) &&
                    (queryLoadBalancerListenerAdminIsUp == null ||
                            queryLoadBalancerListenerAdminIsUp.equals(nsg.getLoadBalancerListenerAdminStateIsUp())) &&
                    (queryLoadBalancerListenerStatus == null ||
                            queryLoadBalancerListenerStatus.equals(nsg.getLoadBalancerListenerStatus()))) {
                if (fields.size() > 0) {
                    ans.add(extractFields(nsg,fields));
                } else {
                    ans.add(nsg);
                }
            }
        }
        return Response.status(200).entity(
                new NeutronLoadBalancerListenerRequest(ans)).build();
    }

    /**
     * Returns a specific LoadBalancerListener */

    @Path("{loadBalancerListenerID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response showLoadBalancerListener(@PathParam("loadBalancerListenerID") String loadBalancerListenerID,
            // return fields
            @QueryParam("fields") List<String> fields) {
        INeutronLoadBalancerListenerCRUD loadBalancerListenerInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerListenerCRUD(this);
        if (loadBalancerListenerInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerListener CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!loadBalancerListenerInterface.neutronLoadBalancerListenerExists(loadBalancerListenerID)) {
            throw new ResourceNotFoundException("LoadBalancerListener UUID does not exist.");
        }
        if (fields.size() > 0) {
            NeutronLoadBalancerListener ans = loadBalancerListenerInterface.getNeutronLoadBalancerListener(loadBalancerListenerID);
            return Response.status(200).entity(
                    new NeutronLoadBalancerListenerRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(200).entity(new NeutronLoadBalancerListenerRequest(loadBalancerListenerInterface.getNeutronLoadBalancerListener(loadBalancerListenerID))).build();
        }
    }

    /**
     * Creates new LoadBalancerListener */

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
    public Response createLoadBalancerListeners(final NeutronLoadBalancerListenerRequest input) {
        INeutronLoadBalancerListenerCRUD loadBalancerListenerInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerListenerCRUD(this);
        if (loadBalancerListenerInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerListener CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (input.isSingleton()) {
            NeutronLoadBalancerListener singleton = input.getSingleton();

            /*
             *  Verify that the LoadBalancerListener doesn't already exist.
             */
            if (loadBalancerListenerInterface.neutronLoadBalancerListenerExists(singleton.getLoadBalancerListenerID())) {
                throw new BadRequestException("LoadBalancerListener UUID already exists");
            }
            loadBalancerListenerInterface.addNeutronLoadBalancerListener(singleton);

            Object[] instances = NeutronUtil.getInstances(INeutronLoadBalancerListenerAware.class, this);
            if (instances != null) {
                if (instances.length > 0) {
                    for (Object instance : instances) {
                        INeutronLoadBalancerListenerAware service = (INeutronLoadBalancerListenerAware) instance;
                        int status = service.canCreateNeutronLoadBalancerListener(singleton);
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
            loadBalancerListenerInterface.addNeutronLoadBalancerListener(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronLoadBalancerListenerAware service = (INeutronLoadBalancerListenerAware) instance;
                    service.neutronLoadBalancerListenerCreated(singleton);
                }
            }
        } else {
            List<NeutronLoadBalancerListener> bulk = input.getBulk();
            Iterator<NeutronLoadBalancerListener> i = bulk.iterator();
            HashMap<String, NeutronLoadBalancerListener> testMap = new HashMap<String, NeutronLoadBalancerListener>();
            Object[] instances = NeutronUtil.getInstances(INeutronLoadBalancerListenerAware.class, this);
            while (i.hasNext()) {
                NeutronLoadBalancerListener test = i.next();

                /*
                 *  Verify that the firewall policy doesn't already exist
                 */

                if (loadBalancerListenerInterface.neutronLoadBalancerListenerExists(test.getLoadBalancerListenerID())) {
                    throw new BadRequestException("LoadBalancerListener UUID already is already created");
                }
                if (testMap.containsKey(test.getLoadBalancerListenerID())) {
                    throw new BadRequestException("LoadBalancerListener UUID already exists");
                }
                if (instances != null) {
                    if (instances.length > 0) {
                        for (Object instance : instances) {
                            INeutronLoadBalancerListenerAware service = (INeutronLoadBalancerListenerAware) instance;
                            int status = service.canCreateNeutronLoadBalancerListener(test);
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
                NeutronLoadBalancerListener test = i.next();
                loadBalancerListenerInterface.addNeutronLoadBalancerListener(test);
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronLoadBalancerListenerAware service = (INeutronLoadBalancerListenerAware) instance;
                        service.neutronLoadBalancerListenerCreated(test);
                    }
                }
            }
        }
        return Response.status(201).entity(input).build();
    }

    /**
     * Updates a LoadBalancerListener Policy
     */
    @Path("{loadBalancerListenerID}")
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
    public Response updateLoadBalancerListener(
            @PathParam("loadBalancerListenerID") String loadBalancerListenerID, final NeutronLoadBalancerListenerRequest input) {
        INeutronLoadBalancerListenerCRUD loadBalancerListenerInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerListenerCRUD(this);
        if (loadBalancerListenerInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerListener CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the LoadBalancerListener exists and there is only one delta provided
         */
        if (!loadBalancerListenerInterface.neutronLoadBalancerListenerExists(loadBalancerListenerID)) {
            throw new ResourceNotFoundException("LoadBalancerListener UUID does not exist.");
        }
        if (!input.isSingleton()) {
            throw new BadRequestException("Only singleton edit supported");
        }
        NeutronLoadBalancerListener delta = input.getSingleton();
        NeutronLoadBalancerListener original = loadBalancerListenerInterface.getNeutronLoadBalancerListener(loadBalancerListenerID);

        /*
         * updates restricted by Neutron
         */
        if (delta.getLoadBalancerListenerID() != null ||
                delta.getNeutronLoadBalancerListenerDefaultPoolID() != null ||
                delta.getLoadBalancerListenerTenantID() != null ||
                delta.getLoadBalancerListenerName() != null ||
                delta.getLoadBalancerListenerDescription() != null ||
                delta.getLoadBalancerListenerIsShared() != null ||
                delta.getNeutronLoadBalancerListenerProtocol() != null ||
                delta.getNeutronLoadBalancerListenerProtocolPort() != null ||
                delta.getNeutronLoadBalancerListenerLoadBalancerID() != null ||
                delta.getLoadBalancerListenerAdminStateIsUp() != null ||
                delta.getLoadBalancerListenerStatus() != null) {
            throw new BadRequestException("Attribute edit blocked by Neutron");
        }

        Object[] instances = NeutronUtil.getInstances(INeutronLoadBalancerListenerAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronLoadBalancerListenerAware service = (INeutronLoadBalancerListenerAware) instance;
                    int status = service.canUpdateNeutronLoadBalancerListener(delta, original);
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
        loadBalancerListenerInterface.updateNeutronLoadBalancerListener(loadBalancerListenerID, delta);
        NeutronLoadBalancerListener updatedLoadBalancerListener = loadBalancerListenerInterface.getNeutronLoadBalancerListener(loadBalancerListenerID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronLoadBalancerListenerAware service = (INeutronLoadBalancerListenerAware) instance;
                service.neutronLoadBalancerListenerUpdated(updatedLoadBalancerListener);
            }
        }
        return Response.status(200).entity(new NeutronLoadBalancerListenerRequest(loadBalancerListenerInterface.getNeutronLoadBalancerListener(loadBalancerListenerID))).build();
    }

    /**
     * Deletes a LoadBalancerListener */

    @Path("{loadBalancerListenerID}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 204, condition = "No Content"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response deleteLoadBalancerListener(
            @PathParam("loadBalancerListenerID") String loadBalancerListenerID) {
        INeutronLoadBalancerListenerCRUD loadBalancerListenerInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerListenerCRUD(this);
        if (loadBalancerListenerInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerListener CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the LoadBalancerListener exists and it isn't currently in use
         */
        if (!loadBalancerListenerInterface.neutronLoadBalancerListenerExists(loadBalancerListenerID)) {
            throw new ResourceNotFoundException("LoadBalancerListener UUID does not exist.");
        }
        if (loadBalancerListenerInterface.neutronLoadBalancerListenerInUse(loadBalancerListenerID)) {
            return Response.status(409).build();
        }
        NeutronLoadBalancerListener singleton = loadBalancerListenerInterface.getNeutronLoadBalancerListener(loadBalancerListenerID);
        Object[] instances = NeutronUtil.getInstances(INeutronLoadBalancerListenerAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronLoadBalancerListenerAware service = (INeutronLoadBalancerListenerAware) instance;
                    int status = service.canDeleteNeutronLoadBalancerListener(singleton);
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

        loadBalancerListenerInterface.removeNeutronLoadBalancerListener(loadBalancerListenerID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronLoadBalancerListenerAware service = (INeutronLoadBalancerListenerAware) instance;
                service.neutronLoadBalancerListenerDeleted(singleton);
            }
        }
        return Response.status(204).build();
    }
}
