/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import java.util.ArrayList;
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
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroup;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroupRule;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroupRule_Direction;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroupRule_Ethertype;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroupRule_Protocol;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.utils.ServiceHelper;


/**
 * Neutron SecurityGroupRules Northbound REST APIs.<
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

@Path("/security-groups-rules")
public class NeutronSecurityGroupRulesNorthbound {

    // Returns a list of all SecurityGroupRules

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 501, condition = "Not Implemented")})
    public Response listSecurityGroupRules(
            // return fields
            @QueryParam("fields") List<String> fields,
            // note: openstack isn't clear about filtering on lists, so we aren't handling them
            @QueryParam("id") String queryID,
            @QueryParam("direction") NeutronSecurityGroupRule_Direction queryDirection,
            @QueryParam("ethertype") NeutronSecurityGroupRule_Ethertype queryEthertype,
            @QueryParam("port_range_max") Integer queryPortRangeMax,
            @QueryParam("port_range_min") Integer queryPortRangeMin,
            @QueryParam("protocol") NeutronSecurityGroupRule_Protocol queryProtocol,
            @QueryParam("remote_group_id") String queryRemoteGroupId,
            @QueryParam("remote_ip_prefix") String queryRemoteIpPrefix,
            @QueryParam("security_group_id") String querySecGroupID,
            @QueryParam("tenant_id") String queryTenantID,
            // pagination
            @QueryParam("limit") String limit,
            @QueryParam("marker") String marker,
            @QueryParam("page_reverse") String pageReverse
            // sorting not supported
    ) {
        INeutronSecurityGroupRuleCRUD sgRuleCRUD = getSecGroupRuleCRUD();

        List<NeutronSecurityGroupRule> ans = new ArrayList<>();
        for (NeutronSecurityGroupRule secGroupRule : sgRuleCRUD.getAll()) {
            if ((queryID == null || queryID.equals(secGroupRule.getID())) &&
                    (queryDirection == null || queryDirection.equals(secGroupRule.getDirection())) &&
                    (queryEthertype == null || queryEthertype.equals(secGroupRule.getEthertype())) &&
                    (queryPortRangeMax == null || queryPortRangeMax.equals(secGroupRule.getPortRangeMax())) &&
                    (queryPortRangeMin == null || queryPortRangeMin.equals(secGroupRule.getPortRangeMin())) &&
                    (queryProtocol == null || queryProtocol.equals(secGroupRule.getProtocol())) &&
                    (queryRemoteGroupId == null || queryRemoteGroupId.equals(secGroupRule.getRemoteGroupUUID())) &&
                    (queryRemoteIpPrefix == null || queryRemoteIpPrefix.equals(secGroupRule.getRemoteIpPrefix())) &&
                    (querySecGroupID == null || querySecGroupID.equals(secGroupRule.getSecGroupUUID())) &&
                    (queryTenantID == null || queryTenantID.equals(secGroupRule.getTenantUUID()))) {
                if (fields.size() > 0)
                    ans.add(secGroupRule.extractFields(fields));
                else
                    ans.add(secGroupRule);
            }
        }

        //TODO: apply pagination to results
        return Response.status(200).entity(
                new NeutronSecurityGroupRuleRequest(ans)).build();
    }

    // Returns a specific SecurityGroup

    @Path("{secGroupRuleUUID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented")})
    public Response showSecurityGroupRule(
            @PathParam("secGroupRuleUUID") String secGroupRuleUUID,
            // return fields
            @QueryParam("fields") List<String> fields) {

        INeutronSecurityGroupRuleCRUD sgRuleCRUD = getSecGroupRuleCRUD();

        Response.ResponseBuilder resp;
        if (sgRuleCRUD.exists(secGroupRuleUUID)) {
            NeutronSecurityGroupRule ans = sgRuleCRUD.get(secGroupRuleUUID);
            resp = Response.status(200).entity(new NeutronSecurityGroupRuleRequest(
                    fields.size() > 0 ? ans.extractFields(fields) : ans));
        } else {
            resp = Response.status(404);
        }
        return resp.build();
    }

    // Creates new SecurityGroup

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 201, condition = "Created"),
            @ResponseCode(code = 400, condition = "Bad Request"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response createSecurityGroupRule(final NeutronSecurityGroupRuleRequest input) {
        INeutronSecurityGroupCRUD sgCRUD = getSecGroupCRUD();
        INeutronSecurityGroupRuleCRUD sgRuleCRUD = getSecGroupRuleCRUD();

        if (!input.isSingleton()) {
            return Response.status(400).build();
        }

        NeutronSecurityGroupRule singleton = input.getSingleton();

        if (sgCRUD.exists(singleton.getID()) ||
             singleton.getDirection() == null ||
             singleton.getSecGroupUUID() == null ||
             (singleton.getRemoteGroupUUID() != null && singleton.getRemoteIpPrefix() != null)) {
            return Response.status(400).build();
        }

        if (singleton.getProtocol() == NeutronSecurityGroupRule_Protocol.ICMP &&
            !(validICMPType(singleton.getPortRangeMax()) && validICMPType(singleton.getPortRangeMin()))) {
            return Response.status(400).build();
        }

        NeutronSecurityGroup secGroup = sgCRUD.get(singleton.getSecGroupUUID());
        if (secGroup == null) {
            return Response.status(400).build();
        }

        singleton.initDefaults();

        INeutronSecurityGroupAware[] services = getSecurityGroupAwareServices();
        if (services != null) {
            for (INeutronSecurityGroupAware service: services) {
                int status = service.canAddSecurityGroupRule(secGroup, singleton);
                if (status < 200 || status > 299) {
                    return Response.status(status).build();
                }
            }
        }

        sgRuleCRUD.add(singleton);

        if (services != null) {
            for (INeutronSecurityGroupAware service : services) {
                service.neutronSecurityGroupRuleAdded(secGroup, singleton);
            }
        }

        return Response.status(201).entity(input).build();
    }

    // Updates a SecurityGroupRule

    @Path("{secGroupRuleUUID}")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 400, condition = "Bad Request"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response updateSecurityGroupRule(
            @PathParam("secGroupRuleUUID") String secGroupRuleUUID,
            NeutronSecurityGroupRuleRequest input
    ) {
        INeutronSecurityGroupRuleCRUD sgRuleCRUD = getSecGroupRuleCRUD();
        INeutronSecurityGroupCRUD sgCRUD = getSecGroupCRUD();

        if (!sgRuleCRUD.exists(secGroupRuleUUID))
            return Response.status(404).build();
        if (!input.isSingleton())
            return Response.status(400).build();

        NeutronSecurityGroupRule singleton = input.getSingleton();
        NeutronSecurityGroupRule original = sgRuleCRUD.get(secGroupRuleUUID);
        NeutronSecurityGroup secGroup = sgCRUD.get(original.getSecGroupUUID());

        if (singleton.getID() != null || singleton.getTenantUUID() != null ||
            singleton.getEthertype() != null || singleton.getPortRangeMax() != null ||
            singleton.getPortRangeMin() != null || singleton.getProtocol() != null ||
            singleton.getRemoteGroupUUID() != null || singleton.getRemoteIpPrefix() != null ||
            singleton.getSecGroupRuleUUID() != null)
            return Response.status(400).build();

        INeutronSecurityGroupAware[] services = getSecurityGroupAwareServices();
        if (services != null) {
            for (INeutronSecurityGroupAware service: services) {
                int status = service.canUpdateSecurityGroupRule(secGroup, singleton, original);
                if (status < 200 || status > 299) {
                    return Response.status(status).build();
                }
            }
        }

        sgRuleCRUD.update(secGroupRuleUUID, singleton);

        NeutronSecurityGroupRule updatedSecurityGroupRule = sgRuleCRUD.get(secGroupRuleUUID);
        if (services != null) {
            for (INeutronSecurityGroupAware service: services) {
                service.neutronSecurityGroupRuleUpdated(secGroup, updatedSecurityGroupRule);
            }
        }

        return Response.status(200).entity(
                new NeutronSecurityGroupRuleRequest(updatedSecurityGroupRule)).build();
    }

    // Deletes a SecurityGroupRule

    @Path("{secGroupRuleUUID}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 204, condition = "No Content"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response deleteSecurityGroup(
            @PathParam("secGroupRuleUUID") String secGroupRuleUUID) {

        INeutronSecurityGroupRuleCRUD sgRuleCRUD = getSecGroupRuleCRUD();
        INeutronSecurityGroupCRUD sgCRUD = getSecGroupCRUD();

        if (!sgRuleCRUD.exists(secGroupRuleUUID))
            return Response.status(404).build();

        NeutronSecurityGroupRule sgRule = sgRuleCRUD.get(secGroupRuleUUID);
        NeutronSecurityGroup secGroup = sgCRUD.get(sgRule.getSecGroupUUID());

        INeutronSecurityGroupAware[] services = getSecurityGroupAwareServices();
        if (services != null) {
            for (INeutronSecurityGroupAware service: services) {
                int status = service.canRemoveSecurityGroupRule(secGroup, sgRule);
                if (status < 200 || status > 299) {
                    return Response.status(status).build();
                }
            }
        }

        sgCRUD.remove(secGroupRuleUUID);

        if (services != null) {
            for (INeutronSecurityGroupAware service: services) {
                service.neutronSecurityGroupRuleRemoved(secGroup, sgRule);
            }
        }
        return Response.status(204).build();
    }

    private INeutronSecurityGroupRuleCRUD getSecGroupRuleCRUD() {
        INeutronSecurityGroupRuleCRUD sgRuleCRUD = NeutronCRUDInterfaces.getNeutronSecurityGroupRuleCRUD(this);
        if (sgRuleCRUD == null) {
            throw new ServiceUnavailableException("SecurityGroupRule CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        return sgRuleCRUD;
    }

    private INeutronSecurityGroupCRUD getSecGroupCRUD() {
        INeutronSecurityGroupCRUD sgInterface = NeutronCRUDInterfaces.getNeutronSecurityGroupCRUD(this);
        if (sgInterface == null) {
            throw new ServiceUnavailableException("SecurityGroup CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        return sgInterface;
    }

    private INeutronSecurityGroupAware[] getSecurityGroupAwareServices() {
        return (INeutronSecurityGroupAware[])ServiceHelper.getGlobalInstances(
                INeutronSecurityGroupAware.class, this, null);
    }

    private static boolean validICMPType(Integer type) {
        return type == null || type >= 0 && type <= 255;
    }
}