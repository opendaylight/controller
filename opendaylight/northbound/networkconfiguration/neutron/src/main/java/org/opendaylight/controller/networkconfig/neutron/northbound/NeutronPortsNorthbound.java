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
import org.opendaylight.controller.networkconfig.neutron.INeutronPortAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.opendaylight.controller.networkconfig.neutron.Neutron_IPs;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.BadRequestException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.utils.ServiceHelper;

/**
 * Neutron Northbound REST APIs.<br>
 * This class provides REST APIs for managing neutron port objects
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

@Path("/ports")
public class NeutronPortsNorthbound {

    final String mac_regex="^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";

    private NeutronPort extractFields(NeutronPort o, List<String> fields) {
        return o.extractFields(fields);
    }

    @Context
    UriInfo uriInfo;

    /**
     * Returns a list of all Ports */

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackPorts.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "Unauthorized"),
        @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response listPorts(
            // return fields
            @QueryParam("fields") List<String> fields,
            // note: openstack isn't clear about filtering on lists, so we aren't handling them
            @QueryParam("id") String queryID,
            @QueryParam("network_id") String queryNetworkID,
            @QueryParam("name") String queryName,
            @QueryParam("admin_state_up") String queryAdminStateUp,
            @QueryParam("status") String queryStatus,
            @QueryParam("mac_address") String queryMACAddress,
            @QueryParam("device_id") String queryDeviceID,
            @QueryParam("device_owner") String queryDeviceOwner,
            @QueryParam("tenant_id") String queryTenantID,
            // linkTitle
            @QueryParam("limit") Integer limit,
            @QueryParam("marker") String marker,
            @DefaultValue("false") @QueryParam("page_reverse") Boolean pageReverse
            // sorting not supported
            ) {
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD(this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronPort> allPorts = portInterface.getAllPorts();
        List<NeutronPort> ans = new ArrayList<NeutronPort>();
        Iterator<NeutronPort> i = allPorts.iterator();
        while (i.hasNext()) {
            NeutronPort oSS = i.next();
            if ((queryID == null || queryID.equals(oSS.getID())) &&
                    (queryNetworkID == null || queryNetworkID.equals(oSS.getNetworkUUID())) &&
                    (queryName == null || queryName.equals(oSS.getName())) &&
                    (queryAdminStateUp == null || queryAdminStateUp.equals(oSS.getAdminStateUp())) &&
                    (queryStatus == null || queryStatus.equals(oSS.getStatus())) &&
                    (queryMACAddress == null || queryMACAddress.equals(oSS.getMacAddress())) &&
                    (queryDeviceID == null || queryDeviceID.equals(oSS.getDeviceID())) &&
                    (queryDeviceOwner == null || queryDeviceOwner.equals(oSS.getDeviceOwner())) &&
                    (queryTenantID == null || queryTenantID.equals(oSS.getTenantID()))) {
                if (fields.size() > 0) {
                    ans.add(extractFields(oSS,fields));
                } else {
                    ans.add(oSS);
                }
            }
        }

        if (limit != null && ans.size() > 1) {
            // Return a paginated request
            NeutronPortRequest request = (NeutronPortRequest) PaginatedRequestFactory.createRequest(limit,
                    marker, pageReverse, uriInfo, ans, NeutronPort.class);
            return Response.status(200).entity(request).build();
        }

        return Response.status(200).entity(
                new NeutronPortRequest(ans)).build();
    }

    /**
     * Returns a specific Port */

    @Path("{portUUID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackPorts.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "Unauthorized"),
        @ResponseCode(code = 404, condition = "Not Found"),
        @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response showPort(
            @PathParam("portUUID") String portUUID,
            // return fields
            @QueryParam("fields") List<String> fields ) {
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD(this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!portInterface.portExists(portUUID)) {
            throw new ResourceNotFoundException("port UUID does not exist.");
        }
        if (fields.size() > 0) {
            NeutronPort ans = portInterface.getPort(portUUID);
            return Response.status(200).entity(
                    new NeutronPortRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(200).entity(
                    new NeutronPortRequest(portInterface.getPort(portUUID))).build();
        }
    }

    /**
     * Creates new Ports */

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackPorts.class)
    @StatusCodes({
        @ResponseCode(code = 201, condition = "Created"),
        @ResponseCode(code = 400, condition = "Bad Request"),
        @ResponseCode(code = 401, condition = "Unauthorized"),
        @ResponseCode(code = 403, condition = "Forbidden"),
        @ResponseCode(code = 404, condition = "Not Found"),
        @ResponseCode(code = 409, condition = "Conflict"),
        @ResponseCode(code = 501, condition = "Not Implemented"),
        @ResponseCode(code = 503, condition = "MAC generation failure") })
    public Response createPorts(final NeutronPortRequest input) {
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD(this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException("Network CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronSubnetCRUD subnetInterface = NeutronCRUDInterfaces.getINeutronSubnetCRUD( this);
        if (subnetInterface == null) {
            throw new ServiceUnavailableException("Subnet CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (input.isSingleton()) {
            NeutronPort singleton = input.getSingleton();

            /*
             * the port must be part of an existing network, must not already exist,
             * have a valid MAC and the MAC not be in use
             */
            if (singleton.getNetworkUUID() == null) {
                throw new BadRequestException("network UUID musy be specified");
            }
            if (portInterface.portExists(singleton.getID())) {
                throw new BadRequestException("port UUID already exists");
            }
            if (!networkInterface.networkExists(singleton.getNetworkUUID())) {
                throw new ResourceNotFoundException("network UUID does not exist.");
            }
            if (singleton.getMacAddress() == null ||
                    !singleton.getMacAddress().matches(mac_regex)) {
                throw new BadRequestException("MAC address not properly formatted");
            }
            if (portInterface.macInUse(singleton.getMacAddress())) {
                throw new ResourceConflictException("MAC Address is in use.");
            }
            /*
             * if fixed IPs are specified, each one has to have an existing subnet ID
             * that is in the same scoping network as the port.  In addition, if an IP
             * address is specified it has to be a valid address for the subnet and not
             * already in use
             */
            List<Neutron_IPs> fixedIPs = singleton.getFixedIPs();
            if (fixedIPs != null && fixedIPs.size() > 0) {
                Iterator<Neutron_IPs> fixedIPIterator = fixedIPs.iterator();
                while (fixedIPIterator.hasNext()) {
                    Neutron_IPs ip = fixedIPIterator.next();
                    if (ip.getSubnetUUID() == null) {
                        throw new BadRequestException("subnet UUID not specified");
                    }
                    if (!subnetInterface.subnetExists(ip.getSubnetUUID())) {
                        throw new BadRequestException("subnet UUID must exists");
                    }
                    NeutronSubnet subnet = subnetInterface.getSubnet(ip.getSubnetUUID());
                    if (!singleton.getNetworkUUID().equalsIgnoreCase(subnet.getNetworkUUID())) {
                        throw new BadRequestException("network UUID must match that of subnet");
                    }
                    if (ip.getIpAddress() != null) {
                        if (!subnet.isValidIP(ip.getIpAddress())) {
                            throw new BadRequestException("IP address is not valid");
                        }
                        if (subnet.isIPInUse(ip.getIpAddress())) {
                            throw new ResourceConflictException("IP address is in use.");
                        }
                    }
                }
            }

            Object[] instances = ServiceHelper.getGlobalInstances(INeutronPortAware.class, this, null);
            if (instances != null) {
                if (instances.length > 0) {
                    for (Object instance : instances) {
                        INeutronPortAware service = (INeutronPortAware) instance;
                        int status = service.canCreatePort(singleton);
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


            // add the port to the cache
            portInterface.addPort(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronPortAware service = (INeutronPortAware) instance;
                    service.neutronPortCreated(singleton);
                }
            }
        } else {
            List<NeutronPort> bulk = input.getBulk();
            Iterator<NeutronPort> i = bulk.iterator();
            HashMap<String, NeutronPort> testMap = new HashMap<String, NeutronPort>();
            Object[] instances = ServiceHelper.getGlobalInstances(INeutronPortAware.class, this, null);
            while (i.hasNext()) {
                NeutronPort test = i.next();

                /*
                 * the port must be part of an existing network, must not already exist,
                 * have a valid MAC and the MAC not be in use.  Further the bulk request
                 * can't already contain a new port with the same UUID
                 */
                if (portInterface.portExists(test.getID())) {
                    throw new BadRequestException("port UUID already exists");
                }
                if (testMap.containsKey(test.getID())) {
                    throw new BadRequestException("port UUID already exists");
                }
                for (NeutronPort check : testMap.values()) {
                    if (test.getMacAddress().equalsIgnoreCase(check.getMacAddress())) {
                        throw new ResourceConflictException("MAC address already allocated");
                    }
                    for (Neutron_IPs test_fixedIP : test.getFixedIPs()) {
                        for (Neutron_IPs check_fixedIP : check.getFixedIPs()) {
                            if (test_fixedIP.getIpAddress().equals(check_fixedIP.getIpAddress())) {
                                throw new ResourceConflictException("IP address already allocated");
                            }
                        }
                    }
                }
                testMap.put(test.getID(), test);
                if (!networkInterface.networkExists(test.getNetworkUUID())) {
                    throw new ResourceNotFoundException("network UUID does not exist.");
                }
                if (!test.getMacAddress().matches(mac_regex)) {
                    throw new BadRequestException("MAC address not properly formatted");
                }
                if (portInterface.macInUse(test.getMacAddress())) {
                    throw new ResourceConflictException("MAC address in use");
                }

                /*
                 * if fixed IPs are specified, each one has to have an existing subnet ID
                 * that is in the same scoping network as the port.  In addition, if an IP
                 * address is specified it has to be a valid address for the subnet and not
                 * already in use (or be the gateway IP address of the subnet)
                 */
                List<Neutron_IPs> fixedIPs = test.getFixedIPs();
                if (fixedIPs != null && fixedIPs.size() > 0) {
                    Iterator<Neutron_IPs> fixedIPIterator = fixedIPs.iterator();
                    while (fixedIPIterator.hasNext()) {
                        Neutron_IPs ip = fixedIPIterator.next();
                        if (ip.getSubnetUUID() == null) {
                            throw new BadRequestException("subnet UUID must be specified");
                        }
                        if (!subnetInterface.subnetExists(ip.getSubnetUUID())) {
                            throw new BadRequestException("subnet UUID doesn't exists");
                        }
                        NeutronSubnet subnet = subnetInterface.getSubnet(ip.getSubnetUUID());
                        if (!test.getNetworkUUID().equalsIgnoreCase(subnet.getNetworkUUID())) {
                            throw new BadRequestException("network UUID must match that of subnet");
                        }
                        if (ip.getIpAddress() != null) {
                            if (!subnet.isValidIP(ip.getIpAddress())) {
                                throw new BadRequestException("ip address not valid");
                            }
                            //TODO: need to add consideration for a fixed IP being assigned the same address as a allocated IP in the
                            //same bulk create
                            if (subnet.isIPInUse(ip.getIpAddress())) {
                                throw new ResourceConflictException("IP address in use");
                            }
                        }
                    }
                }
                if (instances != null) {
                    if (instances.length > 0) {
                        for (Object instance : instances) {
                            INeutronPortAware service = (INeutronPortAware) instance;
                            int status = service.canCreatePort(test);
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

            //once everything has passed, then we can add to the cache
            i = bulk.iterator();
            while (i.hasNext()) {
                NeutronPort test = i.next();
                portInterface.addPort(test);
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronPortAware service = (INeutronPortAware) instance;
                        service.neutronPortCreated(test);
                    }
                }
            }
        }
        return Response.status(201).entity(input).build();
    }

    /**
     * Updates a Port */

    @Path("{portUUID}")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackPorts.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 400, condition = "Bad Request"),
        @ResponseCode(code = 401, condition = "Unauthorized"),
        @ResponseCode(code = 403, condition = "Forbidden"),
        @ResponseCode(code = 404, condition = "Not Found"),
        @ResponseCode(code = 409, condition = "Conflict"),
        @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response updatePort(
            @PathParam("portUUID") String portUUID,
            NeutronPortRequest input
            ) {
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD(this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronSubnetCRUD subnetInterface = NeutronCRUDInterfaces.getINeutronSubnetCRUD( this);
        if (subnetInterface == null) {
            throw new ServiceUnavailableException("Subnet CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        // port has to exist and only a single delta is supported
        if (!portInterface.portExists(portUUID)) {
            throw new ResourceNotFoundException("port UUID does not exist.");
        }
        NeutronPort target = portInterface.getPort(portUUID);
        if (!input.isSingleton()) {
            throw new BadRequestException("only singleton edit suported");
        }
        NeutronPort singleton = input.getSingleton();
        NeutronPort original = portInterface.getPort(portUUID);

        // deltas restricted by Neutron
        if (singleton.getID() != null || singleton.getTenantID() != null ||
                singleton.getStatus() != null) {
            throw new BadRequestException("attribute change blocked by Neutron");
        }

        Object[] instances = ServiceHelper.getGlobalInstances(INeutronPortAware.class, this, null);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronPortAware service = (INeutronPortAware) instance;
                    int status = service.canUpdatePort(singleton, original);
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

        // Verify the new fixed ips are valid
        List<Neutron_IPs> fixedIPs = singleton.getFixedIPs();
        if (fixedIPs != null && fixedIPs.size() > 0) {
            Iterator<Neutron_IPs> fixedIPIterator = fixedIPs.iterator();
            while (fixedIPIterator.hasNext()) {
                Neutron_IPs ip = fixedIPIterator.next();
                if (ip.getSubnetUUID() == null) {
                    throw new BadRequestException("subnet UUID must be specified");
                }
                if (!subnetInterface.subnetExists(ip.getSubnetUUID())) {
                    throw new BadRequestException("subnet UUID doesn't exist.");
                }
                NeutronSubnet subnet = subnetInterface.getSubnet(ip.getSubnetUUID());
                if (!target.getNetworkUUID().equalsIgnoreCase(subnet.getNetworkUUID())) {
                    throw new BadRequestException("network UUID must match that of subnet");
                }
                if (ip.getIpAddress() != null) {
                    if (!subnet.isValidIP(ip.getIpAddress())) {
                        throw new BadRequestException("invalid IP address");
                    }
                    if (subnet.isIPInUse(ip.getIpAddress())) {
                        throw new ResourceConflictException("IP address in use");
                    }
                }
            }
        }

        //        TODO: Support change of security groups
        // update the port and return the modified object
        portInterface.updatePort(portUUID, singleton);
        NeutronPort updatedPort = portInterface.getPort(portUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronPortAware service = (INeutronPortAware) instance;
                service.neutronPortUpdated(updatedPort);
            }
        }
        return Response.status(200).entity(
                new NeutronPortRequest(updatedPort)).build();

    }

    /**
     * Deletes a Port */

    @Path("{portUUID}")
    @DELETE
    @StatusCodes({
        @ResponseCode(code = 204, condition = "No Content"),
        @ResponseCode(code = 401, condition = "Unauthorized"),
        @ResponseCode(code = 403, condition = "Forbidden"),
        @ResponseCode(code = 404, condition = "Not Found"),
        @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response deletePort(
            @PathParam("portUUID") String portUUID) {
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD(this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        // port has to exist and not be owned by anyone.  then it can be removed from the cache
        if (!portInterface.portExists(portUUID)) {
            throw new ResourceNotFoundException("port UUID does not exist.");
        }
        NeutronPort port = portInterface.getPort(portUUID);
        if (port.getDeviceID() != null ||
                port.getDeviceOwner() != null) {
            Response.status(403).build();
        }
        NeutronPort singleton = portInterface.getPort(portUUID);
        Object[] instances = ServiceHelper.getGlobalInstances(INeutronPortAware.class, this, null);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronPortAware service = (INeutronPortAware) instance;
                    int status = service.canDeletePort(singleton);
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
        portInterface.removePort(portUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronPortAware service = (INeutronPortAware) instance;
                service.neutronPortDeleted(singleton);
            }
        }
        return Response.status(204).build();
    }
}
