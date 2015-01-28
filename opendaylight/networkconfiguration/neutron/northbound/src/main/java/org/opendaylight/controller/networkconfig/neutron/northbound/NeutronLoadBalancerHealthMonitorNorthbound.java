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
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerHealthMonitorAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerHealthMonitorCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancer;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerHealthMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Neutron Northbound REST APIs for Load Balancer HealthMonitor.<br>
 * This class provides REST APIs for managing neutron LoadBalancerHealthMonitor
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
@Path("/healthmonitors")
public class NeutronLoadBalancerHealthMonitorNorthbound {
    private static final Logger logger = LoggerFactory.getLogger(NeutronLoadBalancer.class);

    private NeutronLoadBalancerHealthMonitor extractFields(NeutronLoadBalancerHealthMonitor o, List<String> fields) {
        return o.extractFields(fields);
    }

    /**
     * Returns a list of all LoadBalancerHealthMonitor */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 501, condition = "Not Implemented") })

    public Response listGroups(
            // return fields
            @QueryParam("fields") List<String> fields,
            // OpenStack LoadBalancerHealthMonitor attributes
            @QueryParam("id") String queryLoadBalancerHealthMonitorID,
            @QueryParam("tenant_id") String queryLoadBalancerHealthMonitorTenantID,
            // TODO "type" is being a property by the JSON parser.
            @QueryParam("healthmonitor_type") String queryLoadBalancerHealthMonitorType,
            @QueryParam("delay") Integer queryLoadBalancerHealthMonitorDelay,
            @QueryParam("timeout") Integer queryLoadBalancerHealthMonitorTimeout,
            @QueryParam("max_retries") Integer queryLoadBalancerHealthMonitorMaxRetries,
            @QueryParam("http_method") String queryLoadBalancerHealthMonitorHttpMethod,
            @QueryParam("url_path") String queryLoadBalancerHealthMonitorUrlPath,
            @QueryParam("expected_codes") String queryLoadBalancerHealthMonitorExpectedCodes,
            @QueryParam("admin_state_up") Boolean queryLoadBalancerHealthMonitorIsAdminStateUp,
            @QueryParam("status") String queryLoadBalancerHealthMonitorStatus,
            // pagination
            @QueryParam("limit") String limit,
            @QueryParam("marker") String marker,
            @QueryParam("page_reverse") String pageReverse
            // sorting not supported
    ) {
        INeutronLoadBalancerHealthMonitorCRUD loadBalancerHealthMonitorInterface = NeutronCRUDInterfaces
                .getINeutronLoadBalancerHealthMonitorCRUD(this);
        if (loadBalancerHealthMonitorInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerHealthMonitor CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronLoadBalancerHealthMonitor> allLoadBalancerHealthMonitors = loadBalancerHealthMonitorInterface.getAllNeutronLoadBalancerHealthMonitors();
        List<NeutronLoadBalancerHealthMonitor> ans = new ArrayList<NeutronLoadBalancerHealthMonitor>();
        Iterator<NeutronLoadBalancerHealthMonitor> i = allLoadBalancerHealthMonitors.iterator();
        while (i.hasNext()) {
            NeutronLoadBalancerHealthMonitor nsg = i.next();
            if ((queryLoadBalancerHealthMonitorID == null ||
                    queryLoadBalancerHealthMonitorID.equals(nsg.getLoadBalancerHealthMonitorID())) &&
                    (queryLoadBalancerHealthMonitorTenantID == null ||
                            queryLoadBalancerHealthMonitorTenantID.equals
                                    (nsg.getLoadBalancerHealthMonitorTenantID())) &&
                    (queryLoadBalancerHealthMonitorType == null ||
                            queryLoadBalancerHealthMonitorType.equals
                                    (nsg.getLoadBalancerHealthMonitorType())) &&
                    (queryLoadBalancerHealthMonitorDelay == null ||
                            queryLoadBalancerHealthMonitorDelay.equals
                                    (nsg.getLoadBalancerHealthMonitorDelay())) &&
                    (queryLoadBalancerHealthMonitorTimeout == null ||
                            queryLoadBalancerHealthMonitorTimeout.equals
                                    (nsg.getLoadBalancerHealthMonitorTimeout())) &&
                    (queryLoadBalancerHealthMonitorMaxRetries == null ||
                            queryLoadBalancerHealthMonitorMaxRetries.equals
                                    (nsg.getLoadBalancerHealthMonitorMaxRetries())) &&
                    (queryLoadBalancerHealthMonitorHttpMethod == null ||
                            queryLoadBalancerHealthMonitorHttpMethod.equals
                                    (nsg.getLoadBalancerHealthMonitorHttpMethod())) &&
                    (queryLoadBalancerHealthMonitorUrlPath == null ||
                            queryLoadBalancerHealthMonitorUrlPath.equals
                                    (nsg.getLoadBalancerHealthMonitorUrlPath())) &&
                    (queryLoadBalancerHealthMonitorExpectedCodes == null ||
                            queryLoadBalancerHealthMonitorExpectedCodes.equals
                                    (nsg.getLoadBalancerHealthMonitorExpectedCodes())) &&
                    (queryLoadBalancerHealthMonitorIsAdminStateUp == null ||
                            queryLoadBalancerHealthMonitorIsAdminStateUp.equals
                                    (nsg.getLoadBalancerHealthMonitorAdminStateIsUp())) &&
                    (queryLoadBalancerHealthMonitorStatus == null ||
                            queryLoadBalancerHealthMonitorStatus.equals
                                    (nsg.getLoadBalancerHealthMonitorStatus()))) {
                if (fields.size() > 0) {
                    ans.add(extractFields(nsg,fields));
                } else {
                    ans.add(nsg);
                }
            }
        }
        return Response.status(200).entity(
                new NeutronLoadBalancerHealthMonitorRequest(ans)).build();
    }

    /**
     * Returns a specific LoadBalancerHealthMonitor */

    @Path("{loadBalancerHealthMonitorID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response showLoadBalancerHealthMonitor(@PathParam("loadBalancerHealthMonitorID") String loadBalancerHealthMonitorID,
            // return fields
            @QueryParam("fields") List<String> fields) {
        INeutronLoadBalancerHealthMonitorCRUD loadBalancerHealthMonitorInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerHealthMonitorCRUD(this);
        if (loadBalancerHealthMonitorInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerHealthMonitor CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!loadBalancerHealthMonitorInterface.neutronLoadBalancerHealthMonitorExists(loadBalancerHealthMonitorID)) {
            throw new ResourceNotFoundException("LoadBalancerHealthMonitor UUID does not exist.");
        }
        if (fields.size() > 0) {
            NeutronLoadBalancerHealthMonitor ans = loadBalancerHealthMonitorInterface.getNeutronLoadBalancerHealthMonitor(loadBalancerHealthMonitorID);
            return Response.status(200).entity(
                    new NeutronLoadBalancerHealthMonitorRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(200).entity(new NeutronLoadBalancerHealthMonitorRequest(loadBalancerHealthMonitorInterface.getNeutronLoadBalancerHealthMonitor(loadBalancerHealthMonitorID))).build();
        }
    }

    /**
     * Creates new LoadBalancerHealthMonitor */

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
    public Response createLoadBalancerHealthMonitors(final NeutronLoadBalancerHealthMonitorRequest input) {
        INeutronLoadBalancerHealthMonitorCRUD loadBalancerHealthMonitorInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerHealthMonitorCRUD(this);
        if (loadBalancerHealthMonitorInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerHealthMonitor CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (input.isSingleton()) {
            NeutronLoadBalancerHealthMonitor singleton = input.getSingleton();

            /*
             *  Verify that the LoadBalancerHealthMonitor doesn't already exist.
             */
            if (loadBalancerHealthMonitorInterface.neutronLoadBalancerHealthMonitorExists(singleton.getLoadBalancerHealthMonitorID())) {
                throw new BadRequestException("LoadBalancerHealthMonitor UUID already exists");
            }
            loadBalancerHealthMonitorInterface.addNeutronLoadBalancerHealthMonitor(singleton);

            Object[] instances = NeutronUtil.getInstances(INeutronLoadBalancerHealthMonitorAware.class, this);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronLoadBalancerHealthMonitorAware service = (INeutronLoadBalancerHealthMonitorAware) instance;
                    int status = service.canCreateNeutronLoadBalancerHealthMonitor(singleton);
                    if (status < 200 || status > 299) {
                        return Response.status(status).build();
                    }
                }
            }
            loadBalancerHealthMonitorInterface.addNeutronLoadBalancerHealthMonitor(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronLoadBalancerHealthMonitorAware service = (INeutronLoadBalancerHealthMonitorAware) instance;
                    service.neutronLoadBalancerHealthMonitorCreated(singleton);
                }
            }
        } else {
            List<NeutronLoadBalancerHealthMonitor> bulk = input.getBulk();
            Iterator<NeutronLoadBalancerHealthMonitor> i = bulk.iterator();
            HashMap<String, NeutronLoadBalancerHealthMonitor> testMap = new HashMap<String, NeutronLoadBalancerHealthMonitor>();
            Object[] instances = NeutronUtil.getInstances(INeutronLoadBalancerHealthMonitorAware.class, this);
            while (i.hasNext()) {
                NeutronLoadBalancerHealthMonitor test = i.next();

                /*
                 *  Verify that the firewall policy doesn't already exist
                 */

                if (loadBalancerHealthMonitorInterface
                        .neutronLoadBalancerHealthMonitorExists(test.getLoadBalancerHealthMonitorID())) {
                    throw new BadRequestException("LoadBalancerHealthMonitor UUID already is already created");
                }
                if (testMap.containsKey(test.getLoadBalancerHealthMonitorID())) {
                    throw new BadRequestException("LoadBalancerHealthMonitor UUID already exists");
                }
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronLoadBalancerHealthMonitorAware service = (INeutronLoadBalancerHealthMonitorAware) instance;
                        int status = service.canCreateNeutronLoadBalancerHealthMonitor(test);
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
                NeutronLoadBalancerHealthMonitor test = i.next();
                loadBalancerHealthMonitorInterface.addNeutronLoadBalancerHealthMonitor(test);
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronLoadBalancerHealthMonitorAware service = (INeutronLoadBalancerHealthMonitorAware) instance;
                        service.neutronLoadBalancerHealthMonitorCreated(test);
                    }
                }
            }
        }
        return Response.status(201).entity(input).build();
    }

    /**
     * Updates a LoadBalancerHealthMonitor Policy
     */
    @Path("{loadBalancerHealthMonitorID}")
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
    public Response updateLoadBalancerHealthMonitor(
            @PathParam("loadBalancerHealthMonitorID") String loadBalancerHealthMonitorID,
            final NeutronLoadBalancerHealthMonitorRequest input) {
        INeutronLoadBalancerHealthMonitorCRUD loadBalancerHealthMonitorInterface = NeutronCRUDInterfaces
                .getINeutronLoadBalancerHealthMonitorCRUD(this);
        if (loadBalancerHealthMonitorInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerHealthMonitor CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the LoadBalancerHealthMonitor exists and there is only one delta provided
         */
        if (!loadBalancerHealthMonitorInterface.neutronLoadBalancerHealthMonitorExists(loadBalancerHealthMonitorID)) {
            throw new ResourceNotFoundException("LoadBalancerHealthMonitor UUID does not exist.");
        }
        if (!input.isSingleton()) {
            throw new BadRequestException("Only singleton edit supported");
        }
        NeutronLoadBalancerHealthMonitor delta = input.getSingleton();
        NeutronLoadBalancerHealthMonitor original = loadBalancerHealthMonitorInterface
                .getNeutronLoadBalancerHealthMonitor(loadBalancerHealthMonitorID);

        /*
         * updates restricted by Neutron
         */
        if (delta.getLoadBalancerHealthMonitorID() != null ||
                delta.getLoadBalancerHealthMonitorTenantID() != null ||
                delta.getLoadBalancerHealthMonitorType() != null ||
                delta.getLoadBalancerHealthMonitorDelay() != null ||
                delta.getLoadBalancerHealthMonitorTimeout() != null ||
                delta.getLoadBalancerHealthMonitorMaxRetries() != null ||
                delta.getLoadBalancerHealthMonitorHttpMethod() != null ||
                delta.getLoadBalancerHealthMonitorUrlPath() != null ||
                delta.getLoadBalancerHealthMonitorExpectedCodes() != null ||
                delta.getLoadBalancerHealthMonitorAdminStateIsUp() != null ||
                delta.getLoadBalancerHealthMonitorStatus() != null) {
            throw new BadRequestException("Attribute edit blocked by Neutron");
        }

        Object[] instances = NeutronUtil.getInstances(INeutronLoadBalancerHealthMonitorAware.class, this);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronLoadBalancerHealthMonitorAware service = (INeutronLoadBalancerHealthMonitorAware) instance;
                int status = service.canUpdateNeutronLoadBalancerHealthMonitor(delta, original);
                if (status < 200 || status > 299) {
                    return Response.status(status).build();
                }
            }
        }

        /*
         * update the object and return it
         */
        loadBalancerHealthMonitorInterface.updateNeutronLoadBalancerHealthMonitor(loadBalancerHealthMonitorID, delta);
        NeutronLoadBalancerHealthMonitor updatedLoadBalancerHealthMonitor = loadBalancerHealthMonitorInterface
                .getNeutronLoadBalancerHealthMonitor(loadBalancerHealthMonitorID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronLoadBalancerHealthMonitorAware service = (INeutronLoadBalancerHealthMonitorAware) instance;
                service.neutronLoadBalancerHealthMonitorUpdated(updatedLoadBalancerHealthMonitor);
            }
        }
        return Response.status(200).entity(new NeutronLoadBalancerHealthMonitorRequest
                (loadBalancerHealthMonitorInterface.getNeutronLoadBalancerHealthMonitor
                        (loadBalancerHealthMonitorID))).build();
    }



    /**
     * Deletes a LoadBalancerHealthMonitor
     * */
    @Path("{loadBalancerHealthMonitorID}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 204, condition = "No Content"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response deleteLoadBalancerHealthMonitor(
            @PathParam("loadBalancerHealthMonitorID") String loadBalancerHealthMonitorID) {
        INeutronLoadBalancerHealthMonitorCRUD loadBalancerHealthMonitorInterface = NeutronCRUDInterfaces.getINeutronLoadBalancerHealthMonitorCRUD(this);
        if (loadBalancerHealthMonitorInterface == null) {
            throw new ServiceUnavailableException("LoadBalancerHealthMonitor CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        /*
         * verify the LoadBalancerHealthMonitor exists and it isn't currently in use
         */
        if (!loadBalancerHealthMonitorInterface.neutronLoadBalancerHealthMonitorExists(loadBalancerHealthMonitorID)) {
            throw new ResourceNotFoundException("LoadBalancerHealthMonitor UUID does not exist.");
        }
        if (loadBalancerHealthMonitorInterface.neutronLoadBalancerHealthMonitorInUse(loadBalancerHealthMonitorID)) {
            return Response.status(409).build();
        }
        NeutronLoadBalancerHealthMonitor singleton = loadBalancerHealthMonitorInterface.getNeutronLoadBalancerHealthMonitor(loadBalancerHealthMonitorID);
        Object[] instances = NeutronUtil.getInstances(INeutronLoadBalancerHealthMonitorAware.class, this);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronLoadBalancerHealthMonitorAware service = (INeutronLoadBalancerHealthMonitorAware) instance;
                int status = service.canDeleteNeutronLoadBalancerHealthMonitor(singleton);
                if (status < 200 || status > 299) {
                    return Response.status(status).build();
                }
            }
        }
        loadBalancerHealthMonitorInterface.removeNeutronLoadBalancerHealthMonitor(loadBalancerHealthMonitorID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronLoadBalancerHealthMonitorAware service = (INeutronLoadBalancerHealthMonitorAware) instance;
                service.neutronLoadBalancerHealthMonitorDeleted(singleton);
            }
        }
        return Response.status(204).build();
    }
}
