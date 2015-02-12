/*
 * Copyright IBM Corporation and others, 2013.  All rights reserved.
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;

/**
 * Neutron Northbound REST APIs for Subnets.<br>
 * This class provides REST APIs for managing neutron Subnets
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

@Path("/subnets")
public class NeutronSubnetsNorthbound {

    private NeutronSubnet extractFields(NeutronSubnet o, List<String> fields) {
        return o.extractFields(fields);
    }

    @Context
    UriInfo uriInfo;

    /**
     * Returns a list of all Subnets */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackSubnets.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response listSubnets(
            // return fields
            @QueryParam("fields") List<String> fields,
            // note: openstack isn't clear about filtering on lists, so we aren't handling them
            @QueryParam("id") String queryID,
            @QueryParam("network_id") String queryNetworkID,
            @QueryParam("name") String queryName,
            @QueryParam("ip_version") String queryIPVersion,
            @QueryParam("cidr") String queryCIDR,
            @QueryParam("gateway_ip") String queryGatewayIP,
            @QueryParam("enable_dhcp") String queryEnableDHCP,
            @QueryParam("tenant_id") String queryTenantID,
            @QueryParam("ipv6_address_mode") String queryIpV6AddressMode,
            @QueryParam("ipv6_ra_mode") String queryIpV6RaMode,
            // linkTitle
            @QueryParam("limit") Integer limit,
            @QueryParam("marker") String marker,
            @DefaultValue("false") @QueryParam("page_reverse") Boolean pageReverse
            // sorting not supported
            ) {
        INeutronSubnetCRUD subnetInterface = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        if (subnetInterface == null) {
            throw new ServiceUnavailableException("Subnet CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronSubnet> allNetworks = subnetInterface.getAllSubnets();
        List<NeutronSubnet> ans = new ArrayList<NeutronSubnet>();
        Iterator<NeutronSubnet> i = allNetworks.iterator();
        while (i.hasNext()) {
            NeutronSubnet oSS = i.next();
            if ((queryID == null || queryID.equals(oSS.getID())) &&
                    (queryNetworkID == null || queryNetworkID.equals(oSS.getNetworkUUID())) &&
                    (queryName == null || queryName.equals(oSS.getName())) &&
                    (queryIPVersion == null || queryIPVersion.equals(oSS.getIpVersion())) &&
                    (queryCIDR == null || queryCIDR.equals(oSS.getCidr())) &&
                    (queryGatewayIP == null || queryGatewayIP.equals(oSS.getGatewayIP())) &&
                    (queryEnableDHCP == null || queryEnableDHCP.equals(oSS.getEnableDHCP())) &&
                    (queryTenantID == null || queryTenantID.equals(oSS.getTenantID())) &&
                    (queryIpV6AddressMode == null || queryIpV6AddressMode.equals(oSS.getIpV6AddressMode())) &&
                    (queryIpV6RaMode == null || queryIpV6RaMode.equals(oSS.getIpV6RaMode()))){
                if (fields.size() > 0) {
                    ans.add(extractFields(oSS,fields));
                } else {
                    ans.add(oSS);
                }
            }
        }

        if (limit != null && ans.size() > 1) {
            // Return a paginated request
            NeutronSubnetRequest request = (NeutronSubnetRequest) PaginatedRequestFactory.createRequest(limit,
                    marker, pageReverse, uriInfo, ans, NeutronSubnet.class);
            return Response.status(200).entity(request).build();
        }

        return Response.status(200).entity(
                new NeutronSubnetRequest(ans)).build();
    }

    /**
     * Returns a specific Subnet */

    @Path("{subnetUUID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackSubnets.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response showSubnet(
            @PathParam("subnetUUID") String subnetUUID,
            // return fields
            @QueryParam("fields") List<String> fields) {
        INeutronSubnetCRUD subnetInterface = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        if (subnetInterface == null) {
            throw new ServiceUnavailableException("Subnet CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!subnetInterface.subnetExists(subnetUUID)) {
            throw new ResourceNotFoundException("subnet UUID does not exist.");
        }
        if (fields.size() > 0) {
            NeutronSubnet ans = subnetInterface.getSubnet(subnetUUID);
            return Response.status(200).entity(
                    new NeutronSubnetRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(200).entity(
                    new NeutronSubnetRequest(subnetInterface.getSubnet(subnetUUID))).build();
        }
    }

    /**
     * Creates new Subnets */

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackSubnets.class)
    @StatusCodes({
            @ResponseCode(code = 201, condition = "Created"),
            @ResponseCode(code = 400, condition = "Bad Request"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 403, condition = "Forbidden"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response createSubnets(final NeutronSubnetRequest input) {
        INeutronSubnetCRUD subnetInterface = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        if (subnetInterface == null) {
            throw new ServiceUnavailableException("Subnet CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException("Network CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (input.isSingleton()) {
            NeutronSubnet singleton = input.getSingleton();

            /*
             *  Verify that the subnet doesn't already exist (Issue: is a deeper check necessary?)
             *  the specified network exists, the subnet has a valid network address,
             *  and that the gateway IP doesn't overlap with the allocation pools
             *  *then* add the subnet to the cache
             */
            if (subnetInterface.subnetExists(singleton.getID())) {
                throw new BadRequestException("subnet UUID already exists");
            }
            if (!networkInterface.networkExists(singleton.getNetworkUUID())) {
                throw new ResourceNotFoundException("network UUID does not exist.");
            }
            if (!singleton.isValidCIDR()) {
                throw new BadRequestException("invaild CIDR");
            }
            if (!singleton.initDefaults()) {
                throw new InternalServerErrorException("subnet object could not be initialized properly");
            }
            if (singleton.gatewayIP_Pool_overlap()) {
                throw new ResourceConflictException("IP pool overlaps with gateway");
            }
            Object[] instances = NeutronUtil.getInstances(INeutronSubnetAware.class, this);
            if (instances != null) {
                if (instances.length > 0) {
                    for (Object instance : instances) {
                        INeutronSubnetAware service = (INeutronSubnetAware) instance;
                        int status = service.canCreateSubnet(singleton);
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
            subnetInterface.addSubnet(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronSubnetAware service = (INeutronSubnetAware) instance;
                    service.neutronSubnetCreated(singleton);
                }
            }
        } else {
            List<NeutronSubnet> bulk = input.getBulk();
            Iterator<NeutronSubnet> i = bulk.iterator();
            HashMap<String, NeutronSubnet> testMap = new HashMap<String, NeutronSubnet>();
            Object[] instances = NeutronUtil.getInstances(INeutronSubnetAware.class, this);
            while (i.hasNext()) {
                NeutronSubnet test = i.next();

                /*
                 *  Verify that the subnet doesn't already exist (Issue: is a deeper check necessary?)
                 *  the specified network exists, the subnet has a valid network address,
                 *  and that the gateway IP doesn't overlap with the allocation pools,
                 *  and that the bulk request doesn't already contain a subnet with this id
                 */

                if (!test.initDefaults()) {
                    throw new InternalServerErrorException("subnet object could not be initialized properly");
                }
                if (subnetInterface.subnetExists(test.getID())) {
                    throw new BadRequestException("subnet UUID already exists");
                }
                if (testMap.containsKey(test.getID())) {
                    throw new BadRequestException("subnet UUID already exists");
                }
                testMap.put(test.getID(), test);
                if (!networkInterface.networkExists(test.getNetworkUUID())) {
                    throw new ResourceNotFoundException("network UUID does not exist.");
                }
                if (!test.isValidCIDR()) {
                    throw new BadRequestException("Invalid CIDR");
                }
                if (test.gatewayIP_Pool_overlap()) {
                    throw new ResourceConflictException("IP pool overlaps with gateway");
                }
                if (instances != null) {
                    if (instances.length > 0) {
                        for (Object instance : instances) {
                            INeutronSubnetAware service = (INeutronSubnetAware) instance;
                            int status = service.canCreateSubnet(test);
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
                NeutronSubnet test = i.next();
                subnetInterface.addSubnet(test);
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronSubnetAware service = (INeutronSubnetAware) instance;
                        service.neutronSubnetCreated(test);
                    }
                }
            }
        }
        return Response.status(201).entity(input).build();
    }

    /**
     * Updates a Subnet */

    @Path("{subnetUUID}")
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
    public Response updateSubnet(
            @PathParam("subnetUUID") String subnetUUID, final NeutronSubnetRequest input
            ) {
        INeutronSubnetCRUD subnetInterface = NeutronCRUDInterfaces.getINeutronSubnetCRUD( this);
        if (subnetInterface == null) {
            throw new ServiceUnavailableException("Subnet CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the subnet exists and there is only one delta provided
         */
        if (!subnetInterface.subnetExists(subnetUUID)) {
            throw new ResourceNotFoundException("subnet UUID does not exist.");
        }
        if (!input.isSingleton()) {
            throw new BadRequestException("Only singleton edit supported");
        }
        NeutronSubnet delta = input.getSingleton();
        NeutronSubnet original = subnetInterface.getSubnet(subnetUUID);

        /*
         * updates restricted by Neutron
         */
        if (delta.getID() != null || delta.getTenantID() != null ||
                delta.getIpVersion() != null || delta.getCidr() != null ||
                delta.getAllocationPools() != null) {
            throw new BadRequestException("Attribute edit blocked by Neutron");
        }

        Object[] instances = NeutronUtil.getInstances(INeutronSubnetAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronSubnetAware service = (INeutronSubnetAware) instance;
                    int status = service.canUpdateSubnet(delta, original);
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
        subnetInterface.updateSubnet(subnetUUID, delta);
        NeutronSubnet updatedSubnet = subnetInterface.getSubnet(subnetUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronSubnetAware service = (INeutronSubnetAware) instance;
                service.neutronSubnetUpdated(updatedSubnet);
            }
        }
        return Response.status(200).entity(
                new NeutronSubnetRequest(subnetInterface.getSubnet(subnetUUID))).build();
    }

    /**
     * Deletes a Subnet */

    @Path("{subnetUUID}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 204, condition = "No Content"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response deleteSubnet(
            @PathParam("subnetUUID") String subnetUUID) {
        INeutronSubnetCRUD subnetInterface = NeutronCRUDInterfaces.getINeutronSubnetCRUD( this);
        if (subnetInterface == null) {
            throw new ServiceUnavailableException("Network CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify the subnet exists and it isn't currently in use
         */
        if (!subnetInterface.subnetExists(subnetUUID)) {
            throw new ResourceNotFoundException("subnet UUID does not exist.");
        }
        if (subnetInterface.subnetInUse(subnetUUID)) {
            return Response.status(409).build();
        }
        NeutronSubnet singleton = subnetInterface.getSubnet(subnetUUID);
        Object[] instances = NeutronUtil.getInstances(INeutronSubnetAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronSubnetAware service = (INeutronSubnetAware) instance;
                    int status = service.canDeleteSubnet(singleton);
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
        subnetInterface.removeSubnet(subnetUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronSubnetAware service = (INeutronSubnetAware) instance;
                service.neutronSubnetDeleted(singleton);
            }
        }
        return Response.status(204).build();
    }
}
