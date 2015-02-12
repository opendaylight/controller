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
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallPolicyCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallRuleAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronFirewallRule;

/**
 * Neutron Northbound REST APIs for Firewall Rule.<br>
 * This class provides REST APIs for managing neutron Firewall Rule
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
 */

@Path("fw/firewalls_rules")
public class NeutronFirewallRulesNorthbound {

    private NeutronFirewallRule extractFields(NeutronFirewallRule o, List<String> fields) {
        return o.extractFields(fields);
    }

    /**
     * Returns a list of all Firewall Rules
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 501, condition = "Not Implemented")})
    public Response listRules(
            // return fields
            @QueryParam("fields") List<String> fields,
            // OpenStack firewall rule attributes
            @QueryParam("id") String queryFirewallRuleUUID,
            @QueryParam("tenant_id") String queryFirewallRuleTenantID,
            @QueryParam("name") String queryFirewallRuleName,
            @QueryParam("description") String queryFirewallRuleDescription,
            @QueryParam("admin_state_up") Boolean queryFirewallRuleAdminStateIsUp,
            @QueryParam("status") String queryFirewallRuleStatus,
            @QueryParam("shared") Boolean queryFirewallRuleIsShared,
            @QueryParam("firewall_policy_id") String queryFirewallRulePolicyID,
            @QueryParam("protocol") String queryFirewallRuleProtocol,
            @QueryParam("ip_version") Integer queryFirewallRuleIpVer,
            @QueryParam("source_ip_address") String queryFirewallRuleSrcIpAddr,
            @QueryParam("destination_ip_address") String queryFirewallRuleDstIpAddr,
            @QueryParam("source_port") Integer queryFirewallRuleSrcPort,
            @QueryParam("destination_port") Integer queryFirewallRuleDstPort,
            @QueryParam("position") Integer queryFirewallRulePosition,
            @QueryParam("action") String queryFirewallRuleAction,
            @QueryParam("enabled") Boolean queryFirewallRuleIsEnabled,
            // pagination
            @QueryParam("limit") String limit,
            @QueryParam("marker") String marker,
            @QueryParam("page_reverse") String pageReverse
            // sorting not supported
    ) {
        INeutronFirewallRuleCRUD firewallRuleInterface = NeutronCRUDInterfaces.getINeutronFirewallRuleCRUD(this);
        if (firewallRuleInterface == null) {
            throw new ServiceUnavailableException("Firewall Rule CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronFirewallRule> allFirewallRules = firewallRuleInterface.getAllNeutronFirewallRules();
        List<NeutronFirewallRule> ans = new ArrayList<NeutronFirewallRule>();
        Iterator<NeutronFirewallRule> i = allFirewallRules.iterator();
        while (i.hasNext()) {
            NeutronFirewallRule nsr = i.next();
            if ((queryFirewallRuleUUID == null ||
                    queryFirewallRuleUUID.equals(nsr.getFirewallRuleUUID())) &&
                    (queryFirewallRuleTenantID == null ||
                            queryFirewallRuleTenantID.equals(nsr.getFirewallRuleTenantID())) &&
                    (queryFirewallRuleName == null ||
                            queryFirewallRuleName.equals(nsr.getFirewallRuleName())) &&
                    (queryFirewallRuleDescription == null ||
                            queryFirewallRuleDescription.equals(nsr.getFirewallRuleDescription())) &&
                    (queryFirewallRuleAdminStateIsUp == null ||
                            queryFirewallRuleAdminStateIsUp.equals(nsr.getFirewallRuleAdminStateIsUp())) &&
                    (queryFirewallRuleStatus == null ||
                            queryFirewallRuleStatus.equals(nsr.getFirewallRuleStatus())) &&
                    (queryFirewallRuleIsShared == null ||
                            queryFirewallRuleIsShared.equals(nsr.getFirewallRuleIsShared())) &&
                    (queryFirewallRulePolicyID == null ||
                            queryFirewallRulePolicyID.equals(nsr.getFirewallRulePolicyID())) &&
                    (queryFirewallRuleProtocol == null ||
                            queryFirewallRuleProtocol.equals(nsr.getFirewallRuleProtocol())) &&
                    (queryFirewallRuleIpVer == null ||
                            queryFirewallRuleIpVer.equals(nsr.getFirewallRuleIpVer())) &&
                    (queryFirewallRuleSrcIpAddr == null ||
                            queryFirewallRuleSrcIpAddr.equals(nsr.getFirewallRuleSrcIpAddr())) &&
                    (queryFirewallRuleDstIpAddr == null ||
                            queryFirewallRuleDstIpAddr.equals(nsr.getFirewallRuleDstIpAddr())) &&
                    (queryFirewallRuleSrcPort == null ||
                            queryFirewallRuleSrcPort.equals(nsr.getFirewallRuleSrcPort())) &&
                    (queryFirewallRuleDstPort == null ||
                            queryFirewallRuleDstPort.equals(nsr.getFirewallRuleDstPort())) &&
                    (queryFirewallRulePosition == null ||
                            queryFirewallRulePosition.equals(nsr.getFirewallRulePosition())) &&
                    (queryFirewallRuleAction == null ||
                            queryFirewallRuleAction.equals(nsr.getFirewallRuleAction())) &&
                    (queryFirewallRuleIsEnabled == null ||
                            queryFirewallRuleIsEnabled.equals(nsr.getFirewallRuleIsEnabled()))) {
                if (fields.size() > 0) {
                    ans.add(extractFields(nsr, fields));
                } else {
                    ans.add(nsr);
                }
            }
        }
        //TODO: apply pagination to results
        return Response.status(200).entity(
                new NeutronFirewallRuleRequest(ans)).build();
    }

    /**
     * Returns a specific Firewall Rule
     */

    @Path("{firewallRuleUUID}")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented")})
    public Response showFirewallRule(@PathParam("firewallRuleUUID") String firewallRuleUUID,
            // return fields
            @QueryParam("fields") List<String> fields) {
        INeutronFirewallRuleCRUD firewallRuleInterface = NeutronCRUDInterfaces.getINeutronFirewallRuleCRUD(this);
        if (firewallRuleInterface == null) {
            throw new ServiceUnavailableException("Firewall Rule CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!firewallRuleInterface.neutronFirewallRuleExists(firewallRuleUUID)) {
            throw new ResourceNotFoundException("Firewall Rule UUID does not exist.");
        }
        if (fields.size() > 0) {
            NeutronFirewallRule ans = firewallRuleInterface.getNeutronFirewallRule(firewallRuleUUID);
            return Response.status(200).entity(
                    new NeutronFirewallRuleRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(200)
                    .entity(new NeutronFirewallRuleRequest(
                            firewallRuleInterface.getNeutronFirewallRule(firewallRuleUUID)))
                    .build();
        }
    }

    /**
     * Creates new Firewall Rule
     */

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @StatusCodes({
            @ResponseCode(code = 201, condition = "Created"),
            @ResponseCode(code = 400, condition = "Bad Request"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 403, condition = "Forbidden"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented")})
    public Response createFirewallRules(final NeutronFirewallRuleRequest input) {
        INeutronFirewallRuleCRUD firewallRuleInterface = NeutronCRUDInterfaces.getINeutronFirewallRuleCRUD(this);
        if (firewallRuleInterface == null) {
            throw new ServiceUnavailableException("Firewall Rule CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronFirewallPolicyCRUD firewallPolicyInterface = NeutronCRUDInterfaces.getINeutronFirewallPolicyCRUD(this);
        if (firewallPolicyInterface == null) {
            throw new ServiceUnavailableException("Firewall Policy CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        if (input.isSingleton()) {
            NeutronFirewallRule singleton = input.getSingleton();
            if (firewallRuleInterface.neutronFirewallRuleExists(singleton.getFirewallRuleUUID())) {
                throw new BadRequestException("Firewall Rule UUID already exists");
            }
            firewallRuleInterface.addNeutronFirewallRule(singleton);
            Object[] instances = NeutronUtil.getInstances(INeutronFirewallRuleAware.class, this);
            if (instances != null) {
                if (instances.length > 0) {
                    for (Object instance : instances) {
                        INeutronFirewallRuleAware service = (INeutronFirewallRuleAware) instance;
                        int status = service.canCreateNeutronFirewallRule(singleton);
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
            // add rule to cache
            singleton.initDefaults();
            firewallRuleInterface.addNeutronFirewallRule(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronFirewallRuleAware service = (INeutronFirewallRuleAware) instance;
                    service.neutronFirewallRuleCreated(singleton);
                }
            }
        } else {
            List<NeutronFirewallRule> bulk = input.getBulk();
            Iterator<NeutronFirewallRule> i = bulk.iterator();
            HashMap<String, NeutronFirewallRule> testMap = new HashMap<String, NeutronFirewallRule>();
            Object[] instances = NeutronUtil.getInstances(INeutronFirewallRuleAware.class, this);
            while (i.hasNext()) {
                NeutronFirewallRule test = i.next();

                /*
                 *  Verify that the Firewall rule doesn't already exist
                 */

                if (firewallRuleInterface.neutronFirewallRuleExists(test.getFirewallRuleUUID())) {
                    throw new BadRequestException("Firewall Rule UUID already exists");
                }
                if (testMap.containsKey(test.getFirewallRuleUUID())) {
                    throw new BadRequestException("Firewall Rule UUID already exists");
                }
                if (instances != null) {
                    if (instances.length > 0) {
                        for (Object instance : instances) {
                            INeutronFirewallRuleAware service = (INeutronFirewallRuleAware) instance;
                            int status = service.canCreateNeutronFirewallRule(test);
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
                NeutronFirewallRule test = i.next();
                firewallRuleInterface.addNeutronFirewallRule(test);
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronFirewallRuleAware service = (INeutronFirewallRuleAware) instance;
                        service.neutronFirewallRuleCreated(test);
                    }
                }
            }
        }
        return Response.status(201).entity(input).build();
    }

    /**
     * Updates a Firewall Rule
     */
    @Path("{firewallRuleUUID}")
    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 400, condition = "Bad Request"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 403, condition = "Forbidden"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented")})
    public Response updateFirewallRule(
            @PathParam("firewallRuleUUID") String firewallRuleUUID, final NeutronFirewallRuleRequest input) {
        INeutronFirewallRuleCRUD firewallRuleInterface = NeutronCRUDInterfaces.getINeutronFirewallRuleCRUD(this);
        if (firewallRuleInterface == null) {
            throw new ServiceUnavailableException("Firewall Rule CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        /*
         * verify the Firewall Rule exists
         */
        if (!firewallRuleInterface.neutronFirewallRuleExists(firewallRuleUUID)) {
            throw new ResourceNotFoundException("Firewall Rule UUID does not exist.");
        }
        if (!input.isSingleton()) {
            throw new BadRequestException("Only singleton edit supported");
        }
        NeutronFirewallRule delta = input.getSingleton();
        NeutronFirewallRule original = firewallRuleInterface.getNeutronFirewallRule(firewallRuleUUID);

        /*
         * updates restricted by Neutron
         *
         */
        if (delta.getFirewallRuleUUID() != null ||
                delta.getFirewallRuleTenantID() != null ||
                delta.getFirewallRuleName() != null ||
                delta.getFirewallRuleDescription() != null ||
                delta.getFirewallRuleAdminStateIsUp() != null ||
                delta.getFirewallRuleStatus() != null ||
                delta.getFirewallRuleIsShared() != null ||
                delta.getFirewallRulePolicyID() != null ||
                delta.getFirewallRuleProtocol() != null ||
                delta.getFirewallRuleIpVer() != null ||
                delta.getFirewallRuleSrcIpAddr() != null ||
                delta.getFirewallRuleDstIpAddr() != null ||
                delta.getFirewallRuleSrcPort() != null ||
                delta.getFirewallRuleDstPort() != null ||
                delta.getFirewallRulePosition() != null ||
                delta.getFirewallRuleAction() != null ||
                delta.getFirewallRuleIsEnabled() != null) {
            throw new BadRequestException("Attribute edit blocked by Neutron");
        }

        Object[] instances = NeutronUtil.getInstances(INeutronFirewallRuleAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronFirewallRuleAware service = (INeutronFirewallRuleAware) instance;
                    int status = service.canUpdateNeutronFirewallRule(delta, original);
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
        firewallRuleInterface.updateNeutronFirewallRule(firewallRuleUUID, delta);
        NeutronFirewallRule updatedFirewallRule = firewallRuleInterface.getNeutronFirewallRule(firewallRuleUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronFirewallRuleAware service = (INeutronFirewallRuleAware) instance;
                service.neutronFirewallRuleUpdated(updatedFirewallRule);
            }
        }
        return Response.status(200)
                .entity(new NeutronFirewallRuleRequest(firewallRuleInterface.getNeutronFirewallRule(firewallRuleUUID)))
                .build();
    }

    /**
     * Deletes a Firewall Rule
     */

    @Path("{firewallRuleUUID}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 204, condition = "No Content"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented")})
    public Response deleteFirewallRule(
            @PathParam("firewallRuleUUID") String firewallRuleUUID) {
        INeutronFirewallRuleCRUD firewallRuleInterface = NeutronCRUDInterfaces.getINeutronFirewallRuleCRUD(this);
        if (firewallRuleInterface == null) {
            throw new ServiceUnavailableException("Firewall Rule CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the Firewall Rule exists and it isn't currently in use
         */
        if (!firewallRuleInterface.neutronFirewallRuleExists(firewallRuleUUID)) {
            throw new ResourceNotFoundException("Firewall Rule UUID does not exist.");
        }
        if (firewallRuleInterface.neutronFirewallRuleInUse(firewallRuleUUID)) {
            return Response.status(409).build();
        }
        NeutronFirewallRule singleton = firewallRuleInterface.getNeutronFirewallRule(firewallRuleUUID);
        Object[] instances = NeutronUtil.getInstances(INeutronFirewallRuleAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronFirewallRuleAware service = (INeutronFirewallRuleAware) instance;
                    int status = service.canDeleteNeutronFirewallRule(singleton);
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
        firewallRuleInterface.removeNeutronFirewallRule(firewallRuleUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronFirewallRuleAware service = (INeutronFirewallRuleAware) instance;
                service.neutronFirewallRuleDeleted(singleton);
            }
        }
        return Response.status(204).build();
    }
}
