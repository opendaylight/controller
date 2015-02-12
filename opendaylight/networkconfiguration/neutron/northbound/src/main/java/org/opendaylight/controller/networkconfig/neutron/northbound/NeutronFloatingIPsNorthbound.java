/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import java.util.ArrayList;
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
import org.opendaylight.controller.networkconfig.neutron.INeutronFloatingIPAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronFloatingIPCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronFloatingIP;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.opendaylight.controller.networkconfig.neutron.Neutron_IPs;

/**
 * Neutron Northbound REST APIs.<br>
 * This class provides REST APIs for managing Neutron Floating IPs
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

@Path("/floatingips")
public class NeutronFloatingIPsNorthbound {

    private NeutronFloatingIP extractFields(NeutronFloatingIP o, List<String> fields) {
        return o.extractFields(fields);
    }

    /**
     * Returns a list of all FloatingIPs */

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response listFloatingIPs(
            // return fields
            @QueryParam("fields") List<String> fields,
            // note: openstack isn't clear about filtering on lists, so we aren't handling them
            @QueryParam("id") String queryID,
            @QueryParam("floating_network_id") String queryFloatingNetworkId,
            @QueryParam("port_id") String queryPortId,
            @QueryParam("fixed_ip_address") String queryFixedIPAddress,
            @QueryParam("floating_ip_address") String queryFloatingIPAddress,
            @QueryParam("tenant_id") String queryTenantID,
            // pagination
            @QueryParam("limit") String limit,
            @QueryParam("marker") String marker,
            @QueryParam("page_reverse") String pageReverse
            // sorting not supported
            ) {
        INeutronFloatingIPCRUD floatingIPInterface = NeutronCRUDInterfaces.getINeutronFloatingIPCRUD(this);
        if (floatingIPInterface == null) {
            throw new ServiceUnavailableException("Floating IP CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronFloatingIP> allFloatingIPs = floatingIPInterface.getAllFloatingIPs();
        List<NeutronFloatingIP> ans = new ArrayList<NeutronFloatingIP>();
        Iterator<NeutronFloatingIP> i = allFloatingIPs.iterator();
        while (i.hasNext()) {
            NeutronFloatingIP oSS = i.next();
            //match filters: TODO provider extension and router extension
            if ((queryID == null || queryID.equals(oSS.getID())) &&
                    (queryFloatingNetworkId == null || queryFloatingNetworkId.equals(oSS.getFloatingNetworkUUID())) &&
                    (queryPortId == null || queryPortId.equals(oSS.getPortUUID())) &&
                    (queryFixedIPAddress == null || queryFixedIPAddress.equals(oSS.getFixedIPAddress())) &&
                    (queryFloatingIPAddress == null || queryFloatingIPAddress.equals(oSS.getFloatingIPAddress())) &&
                    (queryTenantID == null || queryTenantID.equals(oSS.getTenantUUID()))) {
                if (fields.size() > 0)
                    ans.add(extractFields(oSS,fields));
                else
                    ans.add(oSS);
            }
        }
        //TODO: apply pagination to results
        return Response.status(200).entity(
                new NeutronFloatingIPRequest(ans)).build();
    }

    /**
     * Returns a specific FloatingIP */

    @Path("{floatingipUUID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response showFloatingIP(
            @PathParam("floatingipUUID") String floatingipUUID,
            // return fields
            @QueryParam("fields") List<String> fields ) {
        INeutronFloatingIPCRUD floatingIPInterface = NeutronCRUDInterfaces.getINeutronFloatingIPCRUD(this);
        if (floatingIPInterface == null) {
            throw new ServiceUnavailableException("Floating IP CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!floatingIPInterface.floatingIPExists(floatingipUUID))
            throw new ResourceNotFoundException("Floating IP UUID doesn't exist.");
        if (fields.size() > 0) {
            NeutronFloatingIP ans = floatingIPInterface.getFloatingIP(floatingipUUID);
            return Response.status(200).entity(
                    new NeutronFloatingIPRequest(extractFields(ans, fields))).build();
        } else
            return Response.status(200).entity(
                    new NeutronFloatingIPRequest(floatingIPInterface.getFloatingIP(floatingipUUID))).build();

    }

    /**
     * Creates new FloatingIPs */

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    @StatusCodes({
        @ResponseCode(code = 201, condition = "Created"),
        @ResponseCode(code = 400, condition = "Bad Request"),
        @ResponseCode(code = 401, condition = "Unauthorized"),
        @ResponseCode(code = 409, condition = "Conflict"),
        @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response createFloatingIPs(final NeutronFloatingIPRequest input) {
        INeutronFloatingIPCRUD floatingIPInterface = NeutronCRUDInterfaces.getINeutronFloatingIPCRUD(this);
        if (floatingIPInterface == null) {
            throw new ServiceUnavailableException("Floating IP CRUD Interface "
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
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD( this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (input.isSingleton()) {
            NeutronFloatingIP singleton = input.getSingleton();
            // check existence of id in cache and return badrequest if exists
            if (floatingIPInterface.floatingIPExists(singleton.getID()))
                throw new BadRequestException("Floating IP UUID already exists.");
            // check if the external network is specified, exists, and is an external network
            String externalNetworkUUID = singleton.getFloatingNetworkUUID();
            if (externalNetworkUUID == null)
                throw new BadRequestException("external network UUID doesn't exist.");
            if (!networkInterface.networkExists(externalNetworkUUID))
                throw new BadRequestException("external network UUID doesn't exist.");
            NeutronNetwork externNetwork = networkInterface.getNetwork(externalNetworkUUID);
            if (!externNetwork.isRouterExternal())
                throw new BadRequestException("external network isn't marked router:external");
            // if floating IP is specified, make sure it can come from the network
            String floatingIP = singleton.getFloatingIPAddress();
            if (floatingIP != null) {
                if (externNetwork.getSubnets().size() != 1)
                    throw new BadRequestException("external network doesn't have a subnet");
                NeutronSubnet externSubnet = subnetInterface.getSubnet(externNetwork.getSubnets().get(0));
                if (!externSubnet.isValidIP(floatingIP))
                    throw new BadRequestException("external IP isn't valid for the specified subnet.");
                if (externSubnet.isIPInUse(floatingIP))
                    throw new ResourceConflictException("floating IP is in use.");
            }
            // if port_id is specified, then check that the port exists and has at least one IP
            String port_id = singleton.getPortUUID();
            if (port_id != null) {
                String fixedIP = null;        // used for the fixedIP calculation
                if (!portInterface.portExists(port_id))
                    throw new ResourceNotFoundException("Port UUID doesn't exist.");
                NeutronPort port = portInterface.getPort(port_id);
                if (port.getFixedIPs().size() < 1)
                    throw new BadRequestException("port UUID doesn't have an IP address.");
                // if there is more than one fixed IP then check for fixed_ip_address
                // and that it is in the list of port addresses
                if (port.getFixedIPs().size() > 1) {
                    fixedIP = singleton.getFixedIPAddress();
                    if (fixedIP == null)
                        throw new BadRequestException("fixed IP address doesn't exist.");
                    Iterator<Neutron_IPs> i = port.getFixedIPs().iterator();
                    boolean validFixedIP = false;
                    while (i.hasNext() && !validFixedIP) {
                        Neutron_IPs ip = i.next();
                        if (ip.getIpAddress().equals(fixedIP))
                            validFixedIP = true;
                    }
                    if (!validFixedIP)
                        throw new BadRequestException("can't find a valid fixed IP address");
                } else {
                    fixedIP = port.getFixedIPs().get(0).getIpAddress();
                    if (singleton.getFixedIPAddress() != null && !fixedIP.equalsIgnoreCase(singleton.getFixedIPAddress()))
                        throw new BadRequestException("mismatched fixed IP address in request");
                }
                //lastly check that this fixed IP address isn't already used
                if (port.isBoundToFloatingIP(fixedIP))
                    throw new ResourceConflictException("fixed IP is in use.");
                singleton.setFixedIPAddress(fixedIP);
            }
            Object[] instances = NeutronUtil.getInstances(INeutronFloatingIPAware.class, this);
            if (instances != null) {
                if (instances.length > 0) {
                    for (Object instance : instances) {
                        INeutronFloatingIPAware service = (INeutronFloatingIPAware) instance;
                        int status = service.canCreateFloatingIP(singleton);
                        if (status < 200 || status > 299)
                            return Response.status(status).build();
                    }
                } else {
                    throw new ServiceUnavailableException("No providers registered.  Please try again later");
                }
            } else {
                throw new ServiceUnavailableException("Couldn't get providers list.  Please try again later");
            }
            floatingIPInterface.addFloatingIP(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronFloatingIPAware service = (INeutronFloatingIPAware) instance;
                    service.neutronFloatingIPCreated(singleton);
                }
            }
        } else {
            throw new BadRequestException("only singleton requests allowed.");
        }
        return Response.status(201).entity(input).build();
    }

    /**
     * Updates a FloatingIP */

    @Path("{floatingipUUID}")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 400, condition = "Bad Request"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response updateFloatingIP(
            @PathParam("floatingipUUID") String floatingipUUID,
            NeutronFloatingIPRequest input
            ) {
        INeutronFloatingIPCRUD floatingIPInterface = NeutronCRUDInterfaces.getINeutronFloatingIPCRUD(this);
        if (floatingIPInterface == null) {
            throw new ServiceUnavailableException("Floating IP CRUD Interface "
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
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD( this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!floatingIPInterface.floatingIPExists(floatingipUUID))
            throw new ResourceNotFoundException("Floating IP UUID doesn't exist.");

        NeutronFloatingIP sourceFloatingIP = floatingIPInterface.getFloatingIP(floatingipUUID);
        if (!input.isSingleton())
            throw new BadRequestException("only singleton requests allowed.");
        NeutronFloatingIP singleton = input.getSingleton();
        if (singleton.getID() == null)
            throw new BadRequestException("singleton UUID doesn't exist.");

        NeutronNetwork externNetwork = networkInterface.getNetwork(
                sourceFloatingIP.getFloatingNetworkUUID());

        // if floating IP is specified, make sure it can come from the network
        String floatingIP = singleton.getFloatingIPAddress();
        if (floatingIP != null) {
            if (externNetwork.getSubnets().size() != 1)
                throw new BadRequestException("external network doesn't have a subnet.");
            NeutronSubnet externSubnet = subnetInterface.getSubnet(externNetwork.getSubnets().get(0));
            if (!externSubnet.isValidIP(floatingIP))
                throw new BadRequestException("floating IP not valid for external subnet");
            if (externSubnet.isIPInUse(floatingIP))
                throw new ResourceConflictException("floating IP is in use.");
        }

        // if port_id is specified, then check that the port exists and has at least one IP
        String port_id = singleton.getPortUUID();
        if (port_id != null) {
            String fixedIP = null;        // used for the fixedIP calculation
            if (!portInterface.portExists(port_id))
                throw new ResourceNotFoundException("Port UUID doesn't exist.");
            NeutronPort port = portInterface.getPort(port_id);
            if (port.getFixedIPs().size() < 1)
                throw new BadRequestException("port ID doesn't have a fixed IP address.");
            // if there is more than one fixed IP then check for fixed_ip_address
            // and that it is in the list of port addresses
            if (port.getFixedIPs().size() > 1) {
                fixedIP = singleton.getFixedIPAddress();
                if (fixedIP == null)
                    throw new BadRequestException("request doesn't have a fixed IP address");
                Iterator<Neutron_IPs> i = port.getFixedIPs().iterator();
                boolean validFixedIP = false;
                while (i.hasNext() && !validFixedIP) {
                    Neutron_IPs ip = i.next();
                    if (ip.getIpAddress().equals(fixedIP))
                        validFixedIP = true;
                }
                if (!validFixedIP)
                    throw new BadRequestException("couldn't find a valid fixed IP address");
            } else {
                fixedIP = port.getFixedIPs().get(0).getIpAddress();
                if (singleton.getFixedIPAddress() != null &&
                        !fixedIP.equalsIgnoreCase(singleton.getFixedIPAddress()))
                    throw new BadRequestException("mismatch in fixed IP addresses");
            }
            //lastly check that this fixed IP address isn't already used
            if (port.isBoundToFloatingIP(fixedIP))
                throw new ResourceConflictException("fixed IP is in use.");
            singleton.setFixedIPAddress(fixedIP);
        }
        NeutronFloatingIP target = floatingIPInterface.getFloatingIP(floatingipUUID);
        Object[] instances = NeutronUtil.getInstances(INeutronFloatingIPAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronFloatingIPAware service = (INeutronFloatingIPAware) instance;
                    int status = service.canUpdateFloatingIP(singleton, target);
                    if (status < 200 || status > 299)
                        return Response.status(status).build();
                }
            } else {
                throw new ServiceUnavailableException("No providers registered.  Please try again later");
            }
        } else {
            throw new ServiceUnavailableException("Couldn't get providers list.  Please try again later");
        }
        floatingIPInterface.updateFloatingIP(floatingipUUID, singleton);
        target = floatingIPInterface.getFloatingIP(floatingipUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronFloatingIPAware service = (INeutronFloatingIPAware) instance;
                service.neutronFloatingIPUpdated(target);
            }
        }
        return Response.status(200).entity(
                new NeutronFloatingIPRequest(target)).build();

    }

    /**
     * Deletes a FloatingIP */

    @Path("{floatingipUUID}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 204, condition = "No Content"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response deleteFloatingIP(
            @PathParam("floatingipUUID") String floatingipUUID) {
        INeutronFloatingIPCRUD floatingIPInterface = NeutronCRUDInterfaces.getINeutronFloatingIPCRUD(this);
        if (floatingIPInterface == null) {
            throw new ServiceUnavailableException("Floating IP CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!floatingIPInterface.floatingIPExists(floatingipUUID))
            throw new ResourceNotFoundException("Floating IP UUID doesn't exist.");
        // TODO: need to undo port association if it exists
        NeutronFloatingIP singleton = floatingIPInterface.getFloatingIP(floatingipUUID);
        Object[] instances = NeutronUtil.getInstances(INeutronFloatingIPAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronFloatingIPAware service = (INeutronFloatingIPAware) instance;
                    int status = service.canDeleteFloatingIP(singleton);
                    if (status < 200 || status > 299)
                        return Response.status(status).build();
                }
            } else {
                throw new ServiceUnavailableException("No providers registered.  Please try again later");
            }
        } else {
            throw new ServiceUnavailableException("Couldn't get providers list.  Please try again later");
        }
        floatingIPInterface.removeFloatingIP(floatingipUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronFloatingIPAware service = (INeutronFloatingIPAware) instance;
                service.neutronFloatingIPDeleted(singleton);
            }
        }
        return Response.status(204).build();
    }
}
