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
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallPolicyAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallPolicyCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronFirewallPolicy;

/**
 * Neutron Northbound REST APIs for Firewall Policies.<br>
 * This class provides REST APIs for managing neutron Firewall Policies
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
@Path("/fw/firewalls_policies")
public class NeutronFirewallPolicyNorthbound {

    private NeutronFirewallPolicy extractFields(NeutronFirewallPolicy o, List<String> fields) {
        return o.extractFields(fields);
    }

    /**
     * Returns a list of all Firewall Policies */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 501, condition = "Not Implemented") })

    public Response listGroups(
            // return fields
            @QueryParam("fields") List<String> fields,
            // OpenStack Firewall Policy attributes
            @QueryParam("id") String queryFirewallPolicyUUID,
            @QueryParam("tenant_id") String queryFirewallPolicyTenantID,
            @QueryParam("name") String queryFirewallPolicyName,
            @QueryParam("description") String querySecurityPolicyDescription,
            @QueryParam("shared") String querySecurityPolicyIsShared,
            @QueryParam("firewall_rules") List<String> querySecurityPolicyFirewallRules,
            @QueryParam("audited") Boolean querySecurityPolicyIsAudited,
            // pagination
            @QueryParam("limit") String limit,
            @QueryParam("marker") String marker,
            @QueryParam("page_reverse") String pageReverse
            // sorting not supported
    ) {
        INeutronFirewallPolicyCRUD firewallPolicyInterface = NeutronCRUDInterfaces.getINeutronFirewallPolicyCRUD(this);

        if (firewallPolicyInterface == null) {
            throw new ServiceUnavailableException("Firewall Policy CRUD Interface "
                + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronFirewallPolicy> allFirewallPolicies = firewallPolicyInterface.getAllNeutronFirewallPolicies();
        List<NeutronFirewallPolicy> ans = new ArrayList<NeutronFirewallPolicy>();
        Iterator<NeutronFirewallPolicy> i = allFirewallPolicies.iterator();
        while (i.hasNext()) {
            NeutronFirewallPolicy nsg = i.next();
            if ((queryFirewallPolicyUUID == null ||
                queryFirewallPolicyUUID.equals(nsg.getFirewallPolicyUUID())) &&
                (queryFirewallPolicyTenantID == null ||
                    queryFirewallPolicyTenantID.equals(nsg.getFirewallPolicyTenantID())) &&
                (queryFirewallPolicyName == null ||
                    queryFirewallPolicyName.equals(nsg.getFirewallPolicyName())) &&
                (querySecurityPolicyDescription == null ||
                    querySecurityPolicyDescription.equals(nsg.getFirewallPolicyDescription())) &&
                (querySecurityPolicyIsShared == null ||
                    querySecurityPolicyIsShared.equals(nsg.getFirewallPolicyIsShared())) &&
                (querySecurityPolicyFirewallRules.size() == 0 ||
                    querySecurityPolicyFirewallRules.equals(nsg.getFirewallPolicyRules())) &&
                (querySecurityPolicyIsAudited == null ||
                    querySecurityPolicyIsAudited.equals(nsg.getFirewallPolicyIsAudited()))) {
                if (fields.size() > 0) {
                    ans.add(extractFields(nsg,fields));
                } else {
                    ans.add(nsg);
                }
            }
        } // ans.add((NeutronFirewallPolicy) rules);
        //TODO: apply pagination to results
        return Response.status(200).entity(
                new NeutronFirewallPolicyRequest(ans)).build();
    }

    /**
     * Returns a specific Firewall Policy */

    @Path("{firewallPolicyUUID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response showFirewallPolicy(@PathParam("firewallPolicyUUID") String firewallPolicyUUID,
                                      // return fields
                                      @QueryParam("fields") List<String> fields) {
        INeutronFirewallPolicyCRUD firewallPolicyInterface = NeutronCRUDInterfaces.getINeutronFirewallPolicyCRUD(this);
        if (firewallPolicyInterface == null) {
            throw new ServiceUnavailableException("Firewall Policy CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!firewallPolicyInterface.neutronFirewallPolicyExists(firewallPolicyUUID)) {
            throw new ResourceNotFoundException("Firewall Policy UUID does not exist.");
        }
        if (fields.size() > 0) {
            NeutronFirewallPolicy ans = firewallPolicyInterface.getNeutronFirewallPolicy(firewallPolicyUUID);
            return Response.status(200).entity(
                    new NeutronFirewallPolicyRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(200).entity(new NeutronFirewallPolicyRequest(firewallPolicyInterface.getNeutronFirewallPolicy(firewallPolicyUUID))).build();
        }
    }

    /**
     * Creates new Firewall Policy
     * */
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
    public Response createFirewallPolicies(final NeutronFirewallPolicyRequest input) {
        INeutronFirewallPolicyCRUD firewallPolicyInterface = NeutronCRUDInterfaces.getINeutronFirewallPolicyCRUD(this);
        if (firewallPolicyInterface == null) {
            throw new ServiceUnavailableException("Firewall Policy CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (input.isSingleton()) {
            NeutronFirewallPolicy singleton = input.getSingleton();

            /*
             *  Verify that the Firewall Policy doesn't already exist.
             */
            if (firewallPolicyInterface.neutronFirewallPolicyExists(singleton.getFirewallPolicyUUID())) {
                throw new BadRequestException("Firewall Policy UUID already exists");
            }
            firewallPolicyInterface.addNeutronFirewallPolicy(singleton);

            Object[] instances = NeutronUtil.getInstances(INeutronFirewallPolicyAware.class, this);
            if (instances != null) {
                if (instances.length > 0) {
                    for (Object instance : instances) {
                        INeutronFirewallPolicyAware service = (INeutronFirewallPolicyAware) instance;
                        int status = service.canCreateNeutronFirewallPolicy(singleton);
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
            firewallPolicyInterface.addNeutronFirewallPolicy(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronFirewallPolicyAware service = (INeutronFirewallPolicyAware) instance;
                    service.neutronFirewallPolicyCreated(singleton);
                }
            }
        } else {
            List<NeutronFirewallPolicy> bulk = input.getBulk();
            Iterator<NeutronFirewallPolicy> i = bulk.iterator();
            HashMap<String, NeutronFirewallPolicy> testMap = new HashMap<String, NeutronFirewallPolicy>();
            Object[] instances = NeutronUtil.getInstances(INeutronFirewallPolicyAware.class, this);
            while (i.hasNext()) {
                NeutronFirewallPolicy test = i.next();

                /*
                 *  Verify that the firewall policy doesn't already exist
                 */

                if (firewallPolicyInterface.neutronFirewallPolicyExists(test.getFirewallPolicyUUID())) {
                    throw new BadRequestException("Firewall Policy UUID already is already created");
                }
                if (testMap.containsKey(test.getFirewallPolicyUUID())) {
                    throw new BadRequestException("Firewall Policy UUID already exists");
                }
                if (instances != null) {
                    if (instances.length > 0) {
                        for (Object instance : instances) {
                            INeutronFirewallPolicyAware service = (INeutronFirewallPolicyAware) instance;
                            int status = service.canCreateNeutronFirewallPolicy(test);
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
                NeutronFirewallPolicy test = i.next();
                firewallPolicyInterface.addNeutronFirewallPolicy(test);
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronFirewallPolicyAware service = (INeutronFirewallPolicyAware) instance;
                        service.neutronFirewallPolicyCreated(test);
                    }
                }
            }
        }
        return Response.status(201).entity(input).build();
    }

    /**
     * Updates a Firewall Policy
     */
    @Path("{firewallPolicyUUID}")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackSubnets.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 400, condition = "Bad Request"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 403, condition = "Forbidden"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response updateFirewallPolicy(
            @PathParam("firewallPolicyUUID") String firewallPolicyUUID, final NeutronFirewallPolicyRequest input) {
        INeutronFirewallPolicyCRUD firewallPolicyInterface = NeutronCRUDInterfaces.getINeutronFirewallPolicyCRUD(this);
        if (firewallPolicyInterface == null) {
            throw new ServiceUnavailableException("Firewall Policy CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the Firewall Policy exists and there is only one delta provided
         */
        if (!firewallPolicyInterface.neutronFirewallPolicyExists(firewallPolicyUUID)) {
            throw new ResourceNotFoundException("Firewall Policy UUID does not exist.");
        }
        if (!input.isSingleton()) {
            throw new BadRequestException("Only singleton edit supported");
        }
        NeutronFirewallPolicy delta = input.getSingleton();
        NeutronFirewallPolicy original = firewallPolicyInterface.getNeutronFirewallPolicy(firewallPolicyUUID);

        /*
         * updates restricted by Neutron
         */
        if (delta.getFirewallPolicyUUID() != null ||
                delta.getFirewallPolicyTenantID() != null ||
                delta.getFirewallPolicyName() != null ||
                delta.getFirewallPolicyDescription() != null ||
                delta.getFirewallPolicyIsShared() != null ||
                delta.getFirewallPolicyRules().size() > 0 ||
                delta.getFirewallPolicyIsAudited() != null) {
            throw new BadRequestException("Attribute edit blocked by Neutron");
        }

        Object[] instances = NeutronUtil.getInstances(INeutronFirewallPolicyAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronFirewallPolicyAware service = (INeutronFirewallPolicyAware) instance;
                    int status = service.canUpdateNeutronFirewallPolicy(delta, original);
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
        firewallPolicyInterface.updateNeutronFirewallPolicy(firewallPolicyUUID, delta);
        NeutronFirewallPolicy updatedFirewallPolicy = firewallPolicyInterface.getNeutronFirewallPolicy(firewallPolicyUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronFirewallPolicyAware service = (INeutronFirewallPolicyAware) instance;
                service.neutronFirewallPolicyUpdated(updatedFirewallPolicy);
            }
        }
        return Response.status(200).entity(new NeutronFirewallPolicyRequest(firewallPolicyInterface.getNeutronFirewallPolicy(firewallPolicyUUID))).build();
    }

    /**
     * Deletes a Firewall Policy */

    @Path("{firewallPolicyUUID}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 204, condition = "No Content"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response deleteFirewallPolicy(
            @PathParam("firewallPolicyUUID") String firewallPolicyUUID) {
        INeutronFirewallPolicyCRUD firewallPolicyInterface = NeutronCRUDInterfaces.getINeutronFirewallPolicyCRUD(this);
        if (firewallPolicyInterface == null) {
            throw new ServiceUnavailableException("Firewall Policy CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the Firewall Policy exists and it isn't currently in use
         */
        if (!firewallPolicyInterface.neutronFirewallPolicyExists(firewallPolicyUUID)) {
            throw new ResourceNotFoundException("Firewall Policy UUID does not exist.");
        }
        if (firewallPolicyInterface.neutronFirewallPolicyInUse(firewallPolicyUUID)) {
            return Response.status(409).build();
        }
        NeutronFirewallPolicy singleton = firewallPolicyInterface.getNeutronFirewallPolicy(firewallPolicyUUID);
        Object[] instances = NeutronUtil.getInstances(INeutronFirewallPolicyAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronFirewallPolicyAware service = (INeutronFirewallPolicyAware) instance;
                    int status = service.canDeleteNeutronFirewallPolicy(singleton);
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

        firewallPolicyInterface.removeNeutronFirewallPolicy(firewallPolicyUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronFirewallPolicyAware service = (INeutronFirewallPolicyAware) instance;
                service.neutronFirewallPolicyDeleted(singleton);
            }
        }
        return Response.status(204).build();
    }
}
