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
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.utils.ServiceHelper;


/**
 * Neutron SecurityGroups Northbound REST APIs.<
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

@Path("/security-groups")
public class NeutronSecurityGroupsNorthbound {

    // Returns a list of all SecurityGroups

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 501, condition = "Not Implemented")})
    public Response listSecurityGroups(
            // return fields
            @QueryParam("fields") List<String> fields,
            // note: openstack isn't clear about filtering on lists, so we aren't handling them
            @QueryParam("id") String queryID,
            @QueryParam("name") String queryName,
            @QueryParam("description") String queryDescription,
            @QueryParam("tenant_id") String queryTenantID,
            // pagination
            @QueryParam("limit") String limit,
            @QueryParam("marker") String marker,
            @QueryParam("page_reverse") String pageReverse
            // sorting not supported
    ) {
        INeutronSecurityGroupCRUD sgCRUD = getSecGroupCRUD();

        List<NeutronSecurityGroup> ans = new ArrayList<>();
        for (NeutronSecurityGroup secGroup : sgCRUD.getAll()) {
            if ((queryID == null || queryID.equals(secGroup.getID())) &&
                (queryName == null || queryName.equals(secGroup.getName())) &&
                (queryDescription == null || queryDescription.equals(secGroup.getDescription())) &&
                (queryTenantID == null || queryTenantID.equals(secGroup.getTenantUUID()))) {
                if (fields.size() > 0)
                    ans.add(secGroup.extractFields(fields));
                else
                    ans.add(secGroup);
            }
        }

        //TODO: apply pagination to results
        return Response.status(200).entity(
                new NeutronSecurityGroupRequest(ans)).build();
    }

    // Returns a specific SecurityGroup

    @Path("{secGroupUUID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented")})
    public Response showSecurityGroup(
            @PathParam("secGroupUUID") String secGroupUUID,
            // return fields
            @QueryParam("fields") List<String> fields) {
        INeutronSecurityGroupCRUD sgCRUD = getSecGroupCRUD();

        Response.ResponseBuilder resp;
        if (sgCRUD.exists(secGroupUUID)) {
            NeutronSecurityGroup ans = sgCRUD.get(secGroupUUID);
            resp = Response.status(200).entity(new NeutronSecurityGroupRequest(
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
    public Response createSecurityGroup(final NeutronSecurityGroupRequest input) {
        INeutronSecurityGroupCRUD sgCRUD = getSecGroupCRUD();

        INeutronSecurityGroupRuleCRUD sgRuleCRUD = NeutronCRUDInterfaces.getNeutronSecurityGroupRuleCRUD(this);
        if (sgRuleCRUD == null) {
            throw new ServiceUnavailableException("SecurityGroupRule CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        if (!input.isSingleton()) {
            return Response.status(400).build();
        }

        NeutronSecurityGroup singleton = input.getSingleton();

        if (sgCRUD.exists(singleton.getID()) ||
             (singleton.getDescription() == null && singleton.getName() == null) ||
             singleton.getRules() != null) {
            return Response.status(400).build();
        }

        singleton.initDefaults();

        NeutronSecurityGroupRule defaultIPv4 =
                createDefaultRule(NeutronSecurityGroupRule_Ethertype.IPv4);
        NeutronSecurityGroupRule defaultIPv6 =
                createDefaultRule(NeutronSecurityGroupRule_Ethertype.IPv6);

        INeutronSecurityGroupAware[] services = getSecurityGroupAwareServices();
        if (services != null) {
            for (INeutronSecurityGroupAware service: services) {
                int status = service.canCreateSecurityGroup(singleton);
                if (!isOk(status)) {
                    return Response.status(status).build();
                }

                status = service.canAddSecurityGroupRule(singleton, defaultIPv4);
                if (!isOk(status)) {
                    return Response.status(status).build();
                }

                status = service.canAddSecurityGroupRule(singleton, defaultIPv6);
                if (!isOk(status)) {
                    return Response.status(status).build();
                }
            }
        }

        sgCRUD.add(singleton);
        sgRuleCRUD.add(defaultIPv4);
        sgRuleCRUD.add(defaultIPv6);

        if (services != null) {
            for (INeutronSecurityGroupAware service : services) {
                service.neutronSecurityGroupCreated(singleton);
                service.neutronSecurityGroupRuleAdded(singleton, defaultIPv4);
                service.neutronSecurityGroupRuleAdded(singleton, defaultIPv6);
            }
        }

        return Response.status(201).entity(input).build();
    }

    // Updates a SecurityGroup

    @Path("{secGroupUUID}")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 400, condition = "Bad Request"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response updateSecurityGroup(
            @PathParam("secGroupUUID") String secGroupUUID,
            NeutronSecurityGroupRequest input
    ) {
        INeutronSecurityGroupCRUD sgCRUD = getSecGroupCRUD();

        if (!sgCRUD.exists(secGroupUUID))
            return Response.status(404).build();
        if (!input.isSingleton())
            return Response.status(400).build();

        NeutronSecurityGroup singleton = input.getSingleton();
        NeutronSecurityGroup original = sgCRUD.get(secGroupUUID);

        if (singleton.getID() != null || singleton.getTenantUUID() != null ||
                singleton.getRules() != null)
            return Response.status(400).build();

        INeutronSecurityGroupAware[] services = getSecurityGroupAwareServices();
        if (services != null) {
            for (INeutronSecurityGroupAware service: services) {
                int status = service.canUpdateSecurityGroup(singleton, original);
                if (!isOk(status)) {
                    return Response.status(status).build();
                }
            }
        }

        sgCRUD.update(secGroupUUID, singleton);

        NeutronSecurityGroup updatedSecurityGroup = sgCRUD.get(secGroupUUID);
        if (services != null) {
            for (INeutronSecurityGroupAware service: services) {
                service.neutronSecurityGroupUpdated(updatedSecurityGroup);
            }
        }

        return Response.status(200).entity(
                new NeutronSecurityGroupRequest(updatedSecurityGroup)).build();
    }

    // Deletes a SecurityGroup

    @Path("{secGroupUUID}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 204, condition = "No Content"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response deleteSecurityGroup(
            @PathParam("secGroupUUID") String secGroupUUID) {

        INeutronSecurityGroupCRUD sgCRUD = getSecGroupCRUD();

        if (!sgCRUD.exists(secGroupUUID))
            return Response.status(404).build();
        if (sgCRUD.securityGroupInUse(secGroupUUID))
            return Response.status(409).build();

        NeutronSecurityGroup secGroup = sgCRUD.get(secGroupUUID);

        INeutronSecurityGroupAware[] services = getSecurityGroupAwareServices();
        if (services != null) {
            for (INeutronSecurityGroupAware service: services) {
                int status = service.canDeleteSecurityGroup(secGroup);
                if (!isOk(status)) {
                    return Response.status(status).build();
                }
            }
        }

        sgCRUD.remove(secGroupUUID);

        if (services != null) {
            for (INeutronSecurityGroupAware service: services) {
                service.neutronSecurityGroupDeleted(secGroup);
            }
        }
        return Response.status(204).build();
    }

    private INeutronSecurityGroupCRUD getSecGroupCRUD() {
        INeutronSecurityGroupCRUD sgCRUD = NeutronCRUDInterfaces.getNeutronSecurityGroupCRUD(this);
        if (sgCRUD == null) {
            throw new ServiceUnavailableException("SecurityGroup CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        return sgCRUD;
    }

    private INeutronSecurityGroupAware[] getSecurityGroupAwareServices() {
        return (INeutronSecurityGroupAware[])ServiceHelper.getGlobalInstances(
                INeutronSecurityGroupAware.class, this, null);
    }

    private static NeutronSecurityGroupRule createDefaultRule(NeutronSecurityGroupRule_Ethertype type) {
        NeutronSecurityGroupRule ans = new NeutronSecurityGroupRule();
        ans.initDefaults();
        ans.setEthertype(type);
        ans.setDirection(NeutronSecurityGroupRule_Direction.EGRESS);
        return ans;
    }

    private static boolean isOk(int status) {
        return status >= 200 && status <= 299;
    }
}