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
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityRuleAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Neutron Northbound REST APIs for Security Rule.<br>
 * This class provides REST APIs for managing neutron Security Rule
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

@Path ("/security-group-rules")
public class NeutronSecurityRulesNorthbound {
    static final Logger logger = LoggerFactory.getLogger(NeutronSecurityRulesNorthbound.class);

    private NeutronSecurityRule extractFields(NeutronSecurityRule o, List<String> fields) {
        return o.extractFields(fields);
    }

    /**
     * Returns a list of all Security Rules
     */
    @GET
    @Produces ({MediaType.APPLICATION_JSON})
    @StatusCodes ({
            @ResponseCode (code = 200, condition = "Operation successful"),
            @ResponseCode (code = 401, condition = "Unauthorized"),
            @ResponseCode (code = 501, condition = "Not Implemented")})
    public Response listRules(
            // return fields
            @QueryParam ("fields") List<String> fields,
            // OpenStack security rule attributes
            @QueryParam ("id") String querySecurityRuleUUID,
            @QueryParam ("direction") String querySecurityRuleDirection,
            @QueryParam ("protocol") String querySecurityRuleProtocol,
            @QueryParam ("port_range_min") Integer querySecurityRulePortMin,
            @QueryParam ("port_range_max") Integer querySecurityRulePortMax,
            @QueryParam ("ethertype") String querySecurityRuleEthertype,
            @QueryParam ("remote_ip_prefix") String querySecurityRuleIpPrefix,
            @QueryParam ("remote_group_id") String querySecurityRemoteGroupID,
            @QueryParam ("security_group_id") String querySecurityRuleGroupID,
            @QueryParam ("tenant_id") String querySecurityRuleTenantID,
            @QueryParam ("limit") String limit,
            @QueryParam ("marker") String marker,
            @QueryParam ("page_reverse") String pageReverse
    ) {
        INeutronSecurityRuleCRUD securityRuleInterface = NeutronCRUDInterfaces.getINeutronSecurityRuleCRUD(this);
        if (securityRuleInterface == null) {
            throw new ServiceUnavailableException("Security Rule CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronSecurityRule> allSecurityRules = securityRuleInterface.getAllNeutronSecurityRules();
        List<NeutronSecurityRule> ans = new ArrayList<NeutronSecurityRule>();
        Iterator<NeutronSecurityRule> i = allSecurityRules.iterator();
        while (i.hasNext()) {
            NeutronSecurityRule nsr = i.next();
            if ((querySecurityRuleUUID == null ||
                    querySecurityRuleUUID.equals(nsr.getSecurityRuleUUID())) &&
                    (querySecurityRuleDirection == null ||
                            querySecurityRuleDirection.equals(nsr.getSecurityRuleDirection())) &&
                    (querySecurityRuleProtocol == null ||
                            querySecurityRuleProtocol.equals(nsr.getSecurityRuleProtocol())) &&
                    (querySecurityRulePortMin == null ||
                            querySecurityRulePortMin.equals(nsr.getSecurityRulePortMin())) &&
                    (querySecurityRulePortMax == null ||
                            querySecurityRulePortMax.equals(nsr.getSecurityRulePortMax())) &&
                    (querySecurityRuleEthertype == null ||
                            querySecurityRuleEthertype.equals(nsr.getSecurityRuleEthertype())) &&
                    (querySecurityRuleIpPrefix == null ||
                            querySecurityRuleIpPrefix.equals(nsr.getSecurityRuleRemoteIpPrefix())) &&
                    (querySecurityRuleGroupID == null ||
                            querySecurityRuleGroupID.equals(nsr.getSecurityRuleGroupID())) &&
                    (querySecurityRemoteGroupID == null ||
                            querySecurityRemoteGroupID.equals(nsr.getSecurityRemoteGroupID())) &&
                    (querySecurityRuleTenantID == null ||
                            querySecurityRuleTenantID.equals(nsr.getSecurityRuleTenantID()))) {
                if (fields.size() > 0) {
                    ans.add(extractFields(nsr, fields));
                } else {
                    ans.add(nsr);
                }
            }
        }
        return Response.status(200).entity(
                new NeutronSecurityRuleRequest(ans)).build();
    }

    /**
     * Returns a specific Security Rule
     */

    @Path ("{securityRuleUUID}")
    @GET
    @Produces ({MediaType.APPLICATION_JSON})
    @StatusCodes ({
            @ResponseCode (code = 200, condition = "Operation successful"),
            @ResponseCode (code = 401, condition = "Unauthorized"),
            @ResponseCode (code = 404, condition = "Not Found"),
            @ResponseCode (code = 501, condition = "Not Implemented")})
    public Response showSecurityRule(@PathParam ("securityRuleUUID") String securityRuleUUID,
                                     // return fields
                                     @QueryParam ("fields") List<String> fields) {
        INeutronSecurityRuleCRUD securityRuleInterface = NeutronCRUDInterfaces.getINeutronSecurityRuleCRUD(this);
        if (securityRuleInterface == null) {
            throw new ServiceUnavailableException("Security Rule CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!securityRuleInterface.neutronSecurityRuleExists(securityRuleUUID)) {
            throw new ResourceNotFoundException("Security Rule UUID does not exist.");
        }
        if (!fields.isEmpty()) {
            NeutronSecurityRule ans = securityRuleInterface.getNeutronSecurityRule(securityRuleUUID);
            return Response.status(200).entity(
                    new NeutronSecurityRuleRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(200).entity(new NeutronSecurityRuleRequest(securityRuleInterface.getNeutronSecurityRule(securityRuleUUID))).build();
        }
    }

    /**
     * Creates new Security Rule
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
    public Response createSecurityRules(final NeutronSecurityRuleRequest input) {
        INeutronSecurityRuleCRUD securityRuleInterface = NeutronCRUDInterfaces.getINeutronSecurityRuleCRUD(this);
        if (securityRuleInterface == null) {
            throw new ServiceUnavailableException("Security Rule CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronSecurityGroupCRUD securityGroupInterface = NeutronCRUDInterfaces.getINeutronSecurityGroupCRUD(this);
        if (securityGroupInterface == null) {
            throw new ServiceUnavailableException("Security Group CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * Existing entry checks
        */

        if (input.isSingleton()) {
            NeutronSecurityRule singleton = input.getSingleton();

            if (securityRuleInterface.neutronSecurityRuleExists(singleton.getSecurityRuleUUID())) {
                throw new BadRequestException("Security Rule UUID already exists");
            }
            Object[] instances = NeutronUtil.getInstances(INeutronSecurityRuleAware.class, this);
            if (instances != null) {
                if (instances.length > 0) {
                    for (Object instance : instances) {
                        INeutronSecurityRuleAware service = (INeutronSecurityRuleAware) instance;
                        int status = service.canCreateNeutronSecurityRule(singleton);
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

            // add rule to cache
            singleton.initDefaults();
            securityRuleInterface.addNeutronSecurityRule(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronSecurityRuleAware service = (INeutronSecurityRuleAware) instance;
                    service.neutronSecurityRuleCreated(singleton);
                }
            }

            securityRuleInterface.addNeutronSecurityRule(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronSecurityRuleAware service = (INeutronSecurityRuleAware) instance;
                    service.neutronSecurityRuleCreated(singleton);
                }
            }
        } else {
            List<NeutronSecurityRule> bulk = input.getBulk();
            Iterator<NeutronSecurityRule> i = bulk.iterator();
            HashMap<String, NeutronSecurityRule> testMap = new HashMap<String, NeutronSecurityRule>();
            Object[] instances = NeutronUtil.getInstances(INeutronSecurityRuleAware.class, this);
            while (i.hasNext()) {
                NeutronSecurityRule test = i.next();

                /*
                 *  Verify that the security rule doesn't already exist
                 */

                if (securityRuleInterface.neutronSecurityRuleExists(test.getSecurityRuleUUID())) {
                    throw new BadRequestException("Security Rule UUID already exists");
                }
                if (testMap.containsKey(test.getSecurityRuleUUID())) {
                    throw new BadRequestException("Security Rule UUID already exists");
                }
                if (instances != null) {
                    if (instances.length > 0) {
                        for (Object instance : instances) {
                            INeutronSecurityRuleAware service = (INeutronSecurityRuleAware) instance;
                            int status = service.canCreateNeutronSecurityRule(test);
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
            }

            /*
             * now, each element of the bulk request can be added to the cache
             */
            i = bulk.iterator();
            while (i.hasNext()) {
                NeutronSecurityRule test = i.next();
                securityRuleInterface.addNeutronSecurityRule(test);
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronSecurityRuleAware service = (INeutronSecurityRuleAware) instance;
                        service.neutronSecurityRuleCreated(test);
                    }
                }
            }
        }
        return Response.status(201).entity(input).build();
    }

    /**
     * Updates a Security Rule
     */

    @Path ("{securityRuleUUID}")
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
    public Response updateSecurityRule(
            @PathParam ("securityRuleUUID") String securityRuleUUID, final NeutronSecurityRuleRequest input) {
        INeutronSecurityRuleCRUD securityRuleInterface = NeutronCRUDInterfaces.getINeutronSecurityRuleCRUD(this);
        if (securityRuleInterface == null) {
            throw new ServiceUnavailableException("Security Rule CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the Security Rule exists and there is only one delta provided
         */
        if (!securityRuleInterface.neutronSecurityRuleExists(securityRuleUUID)) {
            throw new ResourceNotFoundException("Security Rule UUID does not exist.");
        }
        if (!input.isSingleton()) {
            throw new BadRequestException("Only singleton edit supported");
        }
        NeutronSecurityRule delta = input.getSingleton();
        NeutronSecurityRule original = securityRuleInterface.getNeutronSecurityRule(securityRuleUUID);

        /*
         * updates restricted by Neutron
         *
         */
        if (delta.getSecurityRuleUUID() != null ||
                delta.getSecurityRuleDirection() != null ||
                delta.getSecurityRuleProtocol() != null ||
                delta.getSecurityRulePortMin() != null ||
                delta.getSecurityRulePortMax() != null ||
                delta.getSecurityRuleEthertype() != null ||
                delta.getSecurityRuleRemoteIpPrefix() != null ||
                delta.getSecurityRuleGroupID() != null ||
                delta.getSecurityRemoteGroupID() != null ||
                delta.getSecurityRuleTenantID() != null) {
            throw new BadRequestException("Attribute edit blocked by Neutron");
        }

        Object[] instances = NeutronUtil.getInstances(INeutronSecurityRuleAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronSecurityRuleAware service = (INeutronSecurityRuleAware) instance;
                    int status = service.canUpdateNeutronSecurityRule(delta, original);
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
        securityRuleInterface.updateNeutronSecurityRule(securityRuleUUID, delta);
        NeutronSecurityRule updatedSecurityRule = securityRuleInterface.getNeutronSecurityRule(securityRuleUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronSecurityRuleAware service = (INeutronSecurityRuleAware) instance;
                service.neutronSecurityRuleUpdated(updatedSecurityRule);
            }
        }
        return Response.status(200).entity(new NeutronSecurityRuleRequest(securityRuleInterface.getNeutronSecurityRule(securityRuleUUID))).build();
    }

    /**
     * Deletes a Security Rule
     */

    @Path ("{securityRuleUUID}")
    @DELETE
    @StatusCodes ({
            @ResponseCode (code = 204, condition = "No Content"),
            @ResponseCode (code = 401, condition = "Unauthorized"),
            @ResponseCode (code = 404, condition = "Not Found"),
            @ResponseCode (code = 409, condition = "Conflict"),
            @ResponseCode (code = 501, condition = "Not Implemented")})
    public Response deleteSecurityRule(
            @PathParam ("securityRuleUUID") String securityRuleUUID) {
        INeutronSecurityRuleCRUD securityRuleInterface = NeutronCRUDInterfaces.getINeutronSecurityRuleCRUD(this);
        if (securityRuleInterface == null) {
            throw new ServiceUnavailableException("Security Rule CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the Security Rule exists and it isn't currently in use
         */
        if (!securityRuleInterface.neutronSecurityRuleExists(securityRuleUUID)) {
            throw new ResourceNotFoundException("Security Rule UUID does not exist.");
        }
        if (securityRuleInterface.neutronSecurityRuleInUse(securityRuleUUID)) {
            return Response.status(409).build();
        }
        NeutronSecurityRule singleton = securityRuleInterface.getNeutronSecurityRule(securityRuleUUID);
        Object[] instances = NeutronUtil.getInstances(INeutronSecurityRuleAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronSecurityRuleAware service = (INeutronSecurityRuleAware) instance;
                    int status = service.canDeleteNeutronSecurityRule(singleton);
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
        securityRuleInterface.removeNeutronSecurityRule(securityRuleUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronSecurityRuleAware service = (INeutronSecurityRuleAware) instance;
                service.neutronSecurityRuleDeleted(singleton);
            }
        }
        return Response.status(204).build();
    }
}
