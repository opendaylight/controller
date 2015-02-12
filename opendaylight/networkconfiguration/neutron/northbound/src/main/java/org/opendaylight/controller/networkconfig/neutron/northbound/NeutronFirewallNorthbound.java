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
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronFirewall;

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
 * Neutron Northbound REST APIs for Firewall.<br>
 * This class provides REST APIs for managing neutron Firewall
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
@Path("/fw/firewalls")
public class NeutronFirewallNorthbound {

    private NeutronFirewall extractFields(NeutronFirewall o, List<String> fields) {
        return o.extractFields(fields);
    }

    /**
     * Returns a list of all Firewalls */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 501, condition = "Not Implemented") })

    public Response listGroups(
            // return fields
            @QueryParam("fields") List<String> fields,
            // OpenStack firewall attributes
            @QueryParam("id") String queryFirewallUUID,
            @QueryParam("tenant_id") String queryFirewallTenantID,
            @QueryParam("name") String queryFirewallName,
            @QueryParam("description") String queryFirewallDescription,
            @QueryParam("shared") Boolean queryFirewallAdminStateIsUp,
            @QueryParam("status") String queryFirewallStatus,
            @QueryParam("shared") Boolean queryFirewallIsShared,
            @QueryParam("firewall_policy_id") String queryFirewallPolicyID,
            // pagination
            @QueryParam("limit") String limit,
            @QueryParam("marker") String marker,
            @QueryParam("page_reverse") String pageReverse
            // sorting not supported
    ) {
        INeutronFirewallCRUD firewallInterface = NeutronCRUDInterfaces.getINeutronFirewallCRUD(this);
        INeutronFirewallRuleCRUD firewallRuleInterface = NeutronCRUDInterfaces.getINeutronFirewallRuleCRUD(this);

        if (firewallInterface == null) {
            throw new ServiceUnavailableException("Firewall CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronFirewall> allFirewalls = firewallInterface.getAllNeutronFirewalls();
        List<NeutronFirewall> ans = new ArrayList<NeutronFirewall>();
        Iterator<NeutronFirewall> i = allFirewalls.iterator();
        while (i.hasNext()) {
            NeutronFirewall nsg = i.next();
            if ((queryFirewallUUID == null ||
                queryFirewallUUID.equals(nsg.getFirewallUUID())) &&
                (queryFirewallTenantID == null ||
                    queryFirewallTenantID.equals(nsg.getFirewallTenantID())) &&
                (queryFirewallName == null ||
                    queryFirewallName.equals(nsg.getFirewallName())) &&
                (queryFirewallDescription == null ||
                    queryFirewallDescription.equals(nsg.getFirewallDescription())) &&
                (queryFirewallAdminStateIsUp == null ||
                    queryFirewallAdminStateIsUp.equals(nsg.getFirewallAdminStateIsUp())) &&
                (queryFirewallStatus == null ||
                    queryFirewallStatus.equals(nsg.getFirewallStatus())) &&
                (queryFirewallIsShared == null ||
                    queryFirewallIsShared.equals(nsg.getFirewallIsShared())) &&
                (queryFirewallPolicyID == null ||
                    queryFirewallPolicyID.equals(nsg.getFirewallPolicyID()))) {
                if (fields.size() > 0) {
                    ans.add(extractFields(nsg,fields));
                } else {
                    ans.add(nsg);
                }
            }
        }
        //TODO: apply pagination to results
        return Response.status(200).entity(
                new NeutronFirewallRequest(ans)).build();
    }

    /**
     * Returns a specific Firewall */

    @Path("{firewallUUID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response showFirewall(@PathParam("firewallUUID") String firewallUUID,
                                      // return fields
                                      @QueryParam("fields") List<String> fields) {
        INeutronFirewallCRUD firewallInterface = NeutronCRUDInterfaces.getINeutronFirewallCRUD(this);
        if (firewallInterface == null) {
            throw new ServiceUnavailableException("Firewall CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!firewallInterface.neutronFirewallExists(firewallUUID)) {
            throw new ResourceNotFoundException("Firewall UUID does not exist.");
        }
        if (fields.size() > 0) {
            NeutronFirewall ans = firewallInterface.getNeutronFirewall(firewallUUID);
            return Response.status(200).entity(
                    new NeutronFirewallRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(200).entity(new NeutronFirewallRequest(firewallInterface.getNeutronFirewall(firewallUUID))).build();
        }
    }

    /**
     * Creates new Firewall */

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
    public Response createFirewalls(final NeutronFirewallRequest input) {
        INeutronFirewallCRUD firewallInterface = NeutronCRUDInterfaces.getINeutronFirewallCRUD(this);
        if (firewallInterface == null) {
            throw new ServiceUnavailableException("Firewall CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (input.isSingleton()) {
            NeutronFirewall singleton = input.getSingleton();

            /*
             *  Verify that the Firewall doesn't already exist.
             */
            if (firewallInterface.neutronFirewallExists(singleton.getFirewallUUID())) {
                throw new BadRequestException("Firewall UUID already exists");
            }
            firewallInterface.addNeutronFirewall(singleton);
            Object[] instances = NeutronUtil.getInstances(INeutronFirewallAware.class, this);
            if (instances != null) {
                if (instances.length > 0) {
                    for (Object instance : instances) {
                        INeutronFirewallAware service = (INeutronFirewallAware) instance;
                        int status = service.canCreateNeutronFirewall(singleton);
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
            firewallInterface.addNeutronFirewall(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronFirewallAware service = (INeutronFirewallAware) instance;
                    service.neutronFirewallCreated(singleton);
                }
            }
        } else {
            List<NeutronFirewall> bulk = input.getBulk();
            Iterator<NeutronFirewall> i = bulk.iterator();
            HashMap<String, NeutronFirewall> testMap = new HashMap<String, NeutronFirewall>();
            Object[] instances = NeutronUtil.getInstances(INeutronFirewallAware.class, this);
            while (i.hasNext()) {
                NeutronFirewall test = i.next();

                /*
                 *  Verify that the secruity group doesn't already exist
                 */
                if (firewallInterface.neutronFirewallExists(test.getFirewallUUID())) {
                    throw new BadRequestException("Firewall UUID already is already created");
                }
                if (testMap.containsKey(test.getFirewallUUID())) {
                    throw new BadRequestException("Firewall UUID already exists");
                }
                if (instances != null) {
                    if (instances.length > 0) {
                        for (Object instance : instances) {
                            INeutronFirewallAware service = (INeutronFirewallAware) instance;
                            int status = service.canCreateNeutronFirewall(test);
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
                NeutronFirewall test = i.next();
                firewallInterface.addNeutronFirewall(test);
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronFirewallAware service = (INeutronFirewallAware) instance;
                        service.neutronFirewallCreated(test);
                    }
                }
            }
        }
        return Response.status(201).entity(input).build();
    }

    /**
     * Updates a Firewall */

    @Path("{firewallUUID}")
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
    public Response updateFirewall(
            @PathParam("firewallUUID") String firewallUUID, final NeutronFirewallRequest input) {
        INeutronFirewallCRUD firewallInterface = NeutronCRUDInterfaces.getINeutronFirewallCRUD(this);
        if (firewallInterface == null) {
            throw new ServiceUnavailableException("Firewall CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the Firewall exists and there is only one delta provided
         */
        if (!firewallInterface.neutronFirewallExists(firewallUUID)) {
            throw new ResourceNotFoundException("Firewall UUID does not exist.");
        }
        if (!input.isSingleton()) {
            throw new BadRequestException("Only singleton edit supported");
        }
        NeutronFirewall delta = input.getSingleton();
        NeutronFirewall original = firewallInterface.getNeutronFirewall(firewallUUID);

        /*
         * updates restricted by Neutron
         */
        if (delta.getFirewallUUID() != null ||
                delta.getFirewallTenantID() != null ||
                delta.getFirewallName() != null ||
                delta.getFirewallDescription() != null ||
                delta.getFirewallAdminStateIsUp() != null ||
                delta.getFirewallStatus() != null ||
                delta.getFirewallIsShared() != null ||
                delta.getFirewallPolicyID() != null) {
            throw new BadRequestException("Attribute edit blocked by Neutron");
        }

        Object[] instances = NeutronUtil.getInstances(INeutronFirewallAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronFirewallAware service = (INeutronFirewallAware) instance;
                    int status = service.canUpdateNeutronFirewall(delta, original);
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
        firewallInterface.updateNeutronFirewall(firewallUUID, delta);
        NeutronFirewall updatedFirewall = firewallInterface.getNeutronFirewall(firewallUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronFirewallAware service = (INeutronFirewallAware) instance;
                service.neutronFirewallUpdated(updatedFirewall);
            }
        }
        return Response.status(200).entity(new NeutronFirewallRequest(firewallInterface.getNeutronFirewall(firewallUUID))).build();
    }

    /**
     * Deletes a Firewall */

    @Path("{firewallUUID}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 204, condition = "No Content"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response deleteFirewall(
            @PathParam("firewallUUID") String firewallUUID) {
        INeutronFirewallCRUD firewallInterface = NeutronCRUDInterfaces.getINeutronFirewallCRUD(this);
        if (firewallInterface == null) {
            throw new ServiceUnavailableException("Firewall CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the Firewall exists and it isn't currently in use
         */
        if (!firewallInterface.neutronFirewallExists(firewallUUID)) {
            throw new ResourceNotFoundException("Firewall UUID does not exist.");
        }
        if (firewallInterface.neutronFirewallInUse(firewallUUID)) {
            return Response.status(409).build();
        }
        NeutronFirewall singleton = firewallInterface.getNeutronFirewall(firewallUUID);
        Object[] instances = NeutronUtil.getInstances(INeutronFirewallAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronFirewallAware service = (INeutronFirewallAware) instance;
                    int status = service.canDeleteNeutronFirewall(singleton);
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
        firewallInterface.removeNeutronFirewall(firewallUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronFirewallAware service = (INeutronFirewallAware) instance;
                service.neutronFirewallDeleted(singleton);
            }
        }
        return Response.status(204).build();
    }
}
