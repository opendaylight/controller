/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
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
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroup;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroupRule;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroupRule_Direction;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroupRule_Ethertype;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.utils.ServiceHelper;

/**
 * Open DOVE Northbound REST APIs for Network.<br>
 * This class provides REST APIs for managing open DOVE internals related to Networks
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

@Path("/networks")
public class NeutronNetworksNorthbound {

    private NeutronNetwork extractFields(NeutronNetwork o, List<String> fields) {
        return o.extractFields(fields);
    }

    /**
     * Returns a list of all Networks */

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackNetworks.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "Unauthorized") })
    public Response listNetworks(
            // return fields
            @QueryParam("fields") List<String> fields,
            // note: openstack isn't clear about filtering on lists, so we aren't handling them
            @QueryParam("id") String queryID,
            @QueryParam("name") String queryName,
            @QueryParam("admin_state_up") String queryAdminStateUp,
            @QueryParam("status") String queryStatus,
            @QueryParam("shared") String queryShared,
            @QueryParam("tenant_id") String queryTenantID,
            @QueryParam("router_external") String queryRouterExternal,
            @QueryParam("provider_network_type") String queryProviderNetworkType,
            @QueryParam("provider_physical_network") String queryProviderPhysicalNetwork,
            @QueryParam("provider_segmentation_id") String queryProviderSegmentationID,
            // pagination
            @QueryParam("limit") String limit,
            @QueryParam("marker") String marker,
            @QueryParam("page_reverse") String pageReverse
            // sorting not supported
            ) {
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException("Network CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronNetwork> allNetworks = networkInterface.getAll();
        List<NeutronNetwork> ans = new ArrayList<NeutronNetwork>();
        Iterator<NeutronNetwork> i = allNetworks.iterator();
        while (i.hasNext()) {
            NeutronNetwork oSN = i.next();
            //match filters: TODO provider extension
            Boolean bAdminStateUp = null;
            Boolean bShared = null;
            Boolean bRouterExternal = null;
            if (queryAdminStateUp != null) {
                bAdminStateUp = Boolean.valueOf(queryAdminStateUp);
            }
            if (queryShared != null) {
                bShared = Boolean.valueOf(queryShared);
            }
            if (queryRouterExternal != null) {
                bRouterExternal = Boolean.valueOf(queryRouterExternal);
            }
            if ((queryID == null || queryID.equals(oSN.getID())) &&
                    (queryName == null || queryName.equals(oSN.getNetworkName())) &&
                    (bAdminStateUp == null || bAdminStateUp.booleanValue() == oSN.isAdminStateUp()) &&
                    (queryStatus == null || queryStatus.equals(oSN.getStatus())) &&
                    (bShared == null || bShared.booleanValue() == oSN.isShared()) &&
                    (bRouterExternal == null || bRouterExternal.booleanValue() == oSN.isRouterExternal()) &&
                    (queryTenantID == null || queryTenantID.equals(oSN.getTenantID()))) {
                if (fields.size() > 0) {
                    ans.add(extractFields(oSN,fields));
                } else {
                    ans.add(oSN);
                }
            }
        }
        //TODO: apply pagination to results
        return Response.status(200).entity(
                new NeutronNetworkRequest(ans)).build();
    }

    /**
     * Returns a specific Network */

    @Path("{netUUID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackNetworks.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "Unauthorized"),
        @ResponseCode(code = 404, condition = "Not Found") })
    public Response showNetwork(
            @PathParam("netUUID") String netUUID,
            // return fields
            @QueryParam("fields") List<String> fields
            ) {
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException("Network CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!networkInterface.exists(netUUID)) {
            return Response.status(404).build();
        }
        if (fields.size() > 0) {
            NeutronNetwork ans = networkInterface.get(netUUID);
            return Response.status(200).entity(
                    new NeutronNetworkRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(200).entity(
                    new NeutronNetworkRequest(networkInterface.get(netUUID))).build();
        }
    }

    /**
     * Creates new Networks */
    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    @TypeHint(NeutronNetwork.class)
    @StatusCodes({
        @ResponseCode(code = 201, condition = "Created"),
        @ResponseCode(code = 400, condition = "Bad Request"),
        @ResponseCode(code = 401, condition = "Unauthorized") })
    public Response createNetworks(final NeutronNetworkRequest input) {
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException("Network CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        INeutronSecurityGroupCRUD sgCRUD = NeutronCRUDInterfaces.getNeutronSecurityGroupCRUD(this);
        if (sgCRUD == null) {
            throw new ServiceUnavailableException("SecurityGroup CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        INeutronSecurityGroupRuleCRUD sgRuleCRUD = NeutronCRUDInterfaces.getNeutronSecurityGroupRuleCRUD(this);
        if (sgRuleCRUD == null) {
            throw new ServiceUnavailableException("SecurityGroupRule CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Object[] instances = ServiceHelper.getGlobalInstances(INeutronNetworkAware.class, this, null);
        INeutronSecurityGroupAware[] sgInstances = (INeutronSecurityGroupAware[])ServiceHelper
                                    .getGlobalInstances(INeutronSecurityGroupAware.class, this, null);

        if (input.isSingleton()) {
            NeutronNetwork singleton = input.getSingleton();

            /*
             * network ID can't already exist
             */
            if (networkInterface.exists(singleton.getID())) {
                return Response.status(400).build();
            }

            if (instances != null) {
                for (Object instance : instances) {
                    INeutronNetworkAware service = (INeutronNetworkAware) instance;
                    int status = service.canCreateNetwork(singleton);
                    if (!isOk(status)) {
                        return Response.status(status).build();
                    }
                }
            }

            int status = tryCreateDefaultGroup(singleton.getTenantID(), sgRuleCRUD,
                                               sgCRUD, sgInstances);
            if (!isOk(status)) {
                return Response.status(status).build();
            }

            // add network to cache
            singleton.initDefaults();
            networkInterface.add(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronNetworkAware service = (INeutronNetworkAware) instance;
                    service.neutronNetworkCreated(singleton);
                }
            }

        } else {
            List<NeutronNetwork> bulk = input.getBulk();
            Iterator<NeutronNetwork> i = bulk.iterator();
            HashMap<String, NeutronNetwork> testMap = new HashMap<String, NeutronNetwork>();
            while (i.hasNext()) {
                NeutronNetwork test = i.next();

                /*
                 * network ID can't already exist, nor can there be an entry for this UUID
                 * already in this bulk request
                 */
                if (networkInterface.exists(test.getID())) {
                    return Response.status(400).build();
                }
                if (testMap.containsKey(test.getID())) {
                    return Response.status(400).build();
                }
                if (instances != null) {
                    for (Object instance: instances) {
                        INeutronNetworkAware service = (INeutronNetworkAware) instance;
                        int status = service.canCreateNetwork(test);
                        if (!isOk(status)) {
                            return Response.status(status).build();
                        }
                    }
                }

                int status = tryCreateDefaultGroup(test.getTenantID(), sgRuleCRUD,
                                                   sgCRUD, sgInstances);
                if (!isOk(status)) {
                    return Response.status(status).build();
                }

                testMap.put(test.getID(),test);
            }

            // now that everything passed, add items to the cache
            i = bulk.iterator();
            while (i.hasNext()) {
                NeutronNetwork test = i.next();
                test.initDefaults();
                networkInterface.add(test);
                if (instances != null) {
                    for (Object instance: instances) {
                        INeutronNetworkAware service = (INeutronNetworkAware) instance;
                        service.neutronNetworkCreated(test);
                    }
                }
            }
        }
        return Response.status(201).entity(input).build();
    }

    /**
     * Updates a Network */
    @Path("{netUUID}")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackNetworks.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 400, condition = "Bad Request"),
        @ResponseCode(code = 403, condition = "Forbidden"),
        @ResponseCode(code = 404, condition = "Not Found"), })
    public Response updateNetwork(
            @PathParam("netUUID") String netUUID, final NeutronNetworkRequest input
            ) {
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException("Network CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * network has to exist and only a single delta is supported
         */
        if (!networkInterface.exists(netUUID)) {
            return Response.status(404).build();
        }
        if (!input.isSingleton()) {
            return Response.status(400).build();
        }
        NeutronNetwork delta = input.getSingleton();

        /*
         * transitions forbidden by Neutron
         */
        if (delta.getID() != null || delta.getTenantID() != null ||
                delta.getStatus() != null) {
            return Response.status(400).build();
        }

        Object[] instances = ServiceHelper.getGlobalInstances(INeutronNetworkAware.class, this, null);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronNetworkAware service = (INeutronNetworkAware) instance;
                NeutronNetwork original = networkInterface.get(netUUID);
                int status = service.canUpdateNetwork(delta, original);
                if (!isOk(status)) {
                    return Response.status(status).build();
                }
            }
        }

        // update network object and return the modified object
                networkInterface.update(netUUID, delta);
                NeutronNetwork updatedSingleton = networkInterface.get(netUUID);
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronNetworkAware service = (INeutronNetworkAware) instance;
                        service.neutronNetworkUpdated(updatedSingleton);
                    }
                }
                return Response.status(200).entity(
                        new NeutronNetworkRequest(networkInterface.get(netUUID))).build();
    }

    /**
     * Deletes a Network */

    @Path("{netUUID}")
    @DELETE
    @StatusCodes({
        @ResponseCode(code = 204, condition = "No Content"),
        @ResponseCode(code = 401, condition = "Unauthorized"),
        @ResponseCode(code = 404, condition = "Not Found"),
        @ResponseCode(code = 409, condition = "Network In Use") })
    public Response deleteNetwork(
            @PathParam("netUUID") String netUUID) {
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException("Network CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * network has to exist and not be in use before it can be removed
         */
        if (!networkInterface.exists(netUUID)) {
            return Response.status(404).build();
        }
        if (networkInterface.networkInUse(netUUID)) {
            return Response.status(409).build();
        }

        NeutronNetwork singleton = networkInterface.get(netUUID);
        Object[] instances = ServiceHelper.getGlobalInstances(INeutronNetworkAware.class, this, null);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronNetworkAware service = (INeutronNetworkAware) instance;
                int status = service.canDeleteNetwork(singleton);
                if (!isOk(status)) {
                    return Response.status(status).build();
                }
            }
        }
        networkInterface.remove(netUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronNetworkAware service = (INeutronNetworkAware) instance;
                service.neutronNetworkDeleted(singleton);
            }
        }
        return Response.status(204).build();
    }

    //TODO: Is it a good idea for the default SG to share the ID of the tenant?
    //      For now, it seems to be the most efficient way.
    private int tryCreateDefaultGroup(String tenantID,
                                      INeutronSecurityGroupRuleCRUD sgRuleCRUD,
                                      INeutronSecurityGroupCRUD sgCRUD,
                                      INeutronSecurityGroupAware[] services) {
        if (sgCRUD.exists(tenantID))  {
            return 200;
        }

        NeutronSecurityGroup secGroup = new NeutronSecurityGroup();
        secGroup.setSecGroupUUID(tenantID);
        secGroup.setTenantUUID(tenantID);
        secGroup.setName(NeutronSecurityGroup.DEFAULT_NAME);
        secGroup.setDescription(NeutronSecurityGroup.DEFAULT_NAME);
        secGroup.initDefaults();

        NeutronSecurityGroupRule tenantRuleIPv4 =
                getTenantIntercommunicationRule(secGroup, NeutronSecurityGroupRule_Ethertype.IPv4);
        NeutronSecurityGroupRule tenantRuleIPv6 =
                getTenantIntercommunicationRule(secGroup, NeutronSecurityGroupRule_Ethertype.IPv6);

        if (services != null) {
            for (INeutronSecurityGroupAware service: services) {
                int status = service.canCreateSecurityGroup(secGroup);
                if (!isOk(status)) return status;

                status = service.canAddSecurityGroupRule(secGroup, tenantRuleIPv4);
                if (!isOk(status)) return status;

                status = service.canAddSecurityGroupRule(secGroup, tenantRuleIPv6);
                if (!isOk(status)) return status;
            }
        }

        sgCRUD.add(secGroup);
        sgRuleCRUD.add(tenantRuleIPv4);
        sgRuleCRUD.add(tenantRuleIPv6);

        if (services != null) {
            for (INeutronSecurityGroupAware service: services) {
                service.neutronSecurityGroupCreated(secGroup);
                service.neutronSecurityGroupRuleAdded(secGroup, tenantRuleIPv4);
                service.neutronSecurityGroupRuleAdded(secGroup, tenantRuleIPv6);
            }
        }

        return 200;
    }

    private NeutronSecurityGroupRule getTenantIntercommunicationRule(
                                                NeutronSecurityGroup sg,
                                                NeutronSecurityGroupRule_Ethertype ethertype) {
        NeutronSecurityGroupRule rule = new NeutronSecurityGroupRule();
        rule.setEthertype(ethertype);
        rule.setDirection(NeutronSecurityGroupRule_Direction.INGRESS);
        rule.setTenantUUID(sg.getTenantUUID());
        rule.setSecGroupUUID(sg.getSecGroupUUID());
        rule.setRemoteGroupUUID(sg.getSecGroupUUID());
        rule.initDefaults();

        return rule;
    }

    private static boolean isOk(int status) {
        return status >= 200 && status <= 299;
    }
}
