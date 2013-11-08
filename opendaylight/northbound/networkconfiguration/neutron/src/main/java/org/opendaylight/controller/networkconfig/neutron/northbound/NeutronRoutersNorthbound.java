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
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronRouterAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronRouterCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter_Interface;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.utils.ServiceHelper;


/**
 * Open DOVE Northbound REST APIs.<br>
 * This class provides REST APIs for managing the open DOVE
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

@Path("/routers")
public class NeutronRoutersNorthbound {

    private NeutronRouter extractFields(NeutronRouter o, List<String> fields) {
        return o.extractFields(fields);
    }

    /**
     * Returns a list of all Routers */

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackRouters.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response listRouters(
            // return fields
            @QueryParam("fields") List<String> fields,
            // note: openstack isn't clear about filtering on lists, so we aren't handling them
            @QueryParam("id") String queryID,
            @QueryParam("name") String queryName,
            @QueryParam("admin_state_up") String queryAdminStateUp,
            @QueryParam("status") String queryStatus,
            @QueryParam("tenant_id") String queryTenantID,
            @QueryParam("external_gateway_info") String queryExternalGatewayInfo,
            // pagination
            @QueryParam("limit") String limit,
            @QueryParam("marker") String marker,
            @QueryParam("page_reverse") String pageReverse
            // sorting not supported
            ) {
        INeutronRouterCRUD routerInterface = NeutronCRUDInterfaces.getINeutronRouterCRUD(this);
        if (routerInterface == null) {
            throw new ServiceUnavailableException("Router CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronRouter> allRouters = routerInterface.getAllRouters();
        List<NeutronRouter> ans = new ArrayList<NeutronRouter>();
        Iterator<NeutronRouter> i = allRouters.iterator();
        while (i.hasNext()) {
            NeutronRouter oSS = i.next();
            if ((queryID == null || queryID.equals(oSS.getID())) &&
                    (queryName == null || queryName.equals(oSS.getName())) &&
                    (queryAdminStateUp == null || queryAdminStateUp.equals(oSS.getAdminStateUp())) &&
                    (queryStatus == null || queryStatus.equals(oSS.getStatus())) &&
                    (queryExternalGatewayInfo == null || queryExternalGatewayInfo.equals(oSS.getExternalGatewayInfo())) &&
                    (queryTenantID == null || queryTenantID.equals(oSS.getTenantID()))) {
                if (fields.size() > 0)
                    ans.add(extractFields(oSS,fields));
                else
                    ans.add(oSS);
            }
        }
        //TODO: apply pagination to results
        return Response.status(200).entity(
                new NeutronRouterRequest(ans)).build();
    }

    /**
     * Returns a specific Router */

    @Path("{routerUUID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackRouters.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 403, condition = "Forbidden"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response showRouter(
            @PathParam("routerUUID") String routerUUID,
            // return fields
            @QueryParam("fields") List<String> fields) {
        INeutronRouterCRUD routerInterface = NeutronCRUDInterfaces.getINeutronRouterCRUD(this);
        if (routerInterface == null) {
            throw new ServiceUnavailableException("Router CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!routerInterface.routerExists(routerUUID))
            return Response.status(404).build();
        if (fields.size() > 0) {
            NeutronRouter ans = routerInterface.getRouter(routerUUID);
            return Response.status(200).entity(
                    new NeutronRouterRequest(extractFields(ans, fields))).build();
        } else
            return Response.status(200).entity(
                    new NeutronRouterRequest(routerInterface.getRouter(routerUUID))).build();
    }

    /**
     * Creates new Routers */

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackRouters.class)
    @StatusCodes({
            @ResponseCode(code = 201, condition = "Created"),
            @ResponseCode(code = 400, condition = "Bad Request"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response createRouters(final NeutronRouterRequest input) {
        INeutronRouterCRUD routerInterface = NeutronCRUDInterfaces.getINeutronRouterCRUD(this);
        if (routerInterface == null) {
            throw new ServiceUnavailableException("Router CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException("Network CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (input.isSingleton()) {
            NeutronRouter singleton = input.getSingleton();

            /*
             * verify that the router doesn't already exist (issue: is deeper inspection necessary?)
             * if there is external gateway information provided, verify that the specified network
             * exists and has been designated as "router:external"
             */
            if (routerInterface.routerExists(singleton.getID()))
                return Response.status(400).build();
            if (singleton.getExternalGatewayInfo() != null) {
                String externNetworkPtr = singleton.getExternalGatewayInfo().getNetworkID();
                if (!networkInterface.networkExists(externNetworkPtr))
                    return Response.status(400).build();
                NeutronNetwork externNetwork = networkInterface.getNetwork(externNetworkPtr);
                if (!externNetwork.isRouterExternal())
                    return Response.status(400).build();
            }
            Object[] instances = ServiceHelper.getGlobalInstances(INeutronRouterAware.class, this, null);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronRouterAware service = (INeutronRouterAware) instance;
                    int status = service.canCreateRouter(singleton);
                    if (status < 200 || status > 299)
                        return Response.status(status).build();
                }
            }

            /*
             * add router to the cache
             */
            routerInterface.addRouter(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronRouterAware service = (INeutronRouterAware) instance;
                    service.neutronRouterCreated(singleton);
                }
            }
        } else {

            /*
             * only singleton router creates supported
             */
            return Response.status(400).build();
        }
        return Response.status(201).entity(input).build();
    }

    /**
     * Updates a Router */

    @Path("{routerUUID}")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackRouters.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 400, condition = "Bad Request"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response updateRouter(
            @PathParam("routerUUID") String routerUUID,
            NeutronRouterRequest input
            ) {
        INeutronRouterCRUD routerInterface = NeutronCRUDInterfaces.getINeutronRouterCRUD(this);
        if (routerInterface == null) {
            throw new ServiceUnavailableException("Router CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException("Network CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * router has to exist and only a single delta can be supplied
         */
        if (!routerInterface.routerExists(routerUUID))
            return Response.status(404).build();
        if (!input.isSingleton())
            return Response.status(400).build();
        NeutronRouter singleton = input.getSingleton();
        NeutronRouter original = routerInterface.getRouter(routerUUID);

        /*
         * attribute changes blocked by Neutron
         */
        if (singleton.getID() != null || singleton.getTenantID() != null ||
                singleton.getStatus() != null)
            return Response.status(400).build();

        Object[] instances = ServiceHelper.getGlobalInstances(INeutronRouterAware.class, this, null);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronRouterAware service = (INeutronRouterAware) instance;
                int status = service.canUpdateRouter(singleton, original);
                if (status < 200 || status > 299)
                    return Response.status(status).build();
            }
        }
        /*
         * if the external gateway info is being changed, verify that the new network
         * exists and has been designated as an external network
         */
        if (singleton.getExternalGatewayInfo() != null) {
            String externNetworkPtr = singleton.getExternalGatewayInfo().getNetworkID();
            if (!networkInterface.networkExists(externNetworkPtr))
                return Response.status(400).build();
            NeutronNetwork externNetwork = networkInterface.getNetwork(externNetworkPtr);
            if (!externNetwork.isRouterExternal())
                return Response.status(400).build();
        }

        /*
         * update the router entry and return the modified object
         */
        routerInterface.updateRouter(routerUUID, singleton);
        NeutronRouter updatedRouter = routerInterface.getRouter(routerUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronRouterAware service = (INeutronRouterAware) instance;
                service.neutronRouterUpdated(updatedRouter);
            }
        }
        return Response.status(200).entity(
                new NeutronRouterRequest(routerInterface.getRouter(routerUUID))).build();

    }

    /**
     * Deletes a Router */

    @Path("{routerUUID}")
    @DELETE
    @StatusCodes({
            @ResponseCode(code = 204, condition = "No Content"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response deleteRouter(
            @PathParam("routerUUID") String routerUUID) {
        INeutronRouterCRUD routerInterface = NeutronCRUDInterfaces.getINeutronRouterCRUD(this);
        if (routerInterface == null) {
            throw new ServiceUnavailableException("Router CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * verify that the router exists and is not in use before removing it
         */
        if (!routerInterface.routerExists(routerUUID))
            return Response.status(404).build();
        if (routerInterface.routerInUse(routerUUID))
            return Response.status(409).build();
        NeutronRouter singleton = routerInterface.getRouter(routerUUID);
        Object[] instances = ServiceHelper.getGlobalInstances(INeutronRouterAware.class, this, null);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronRouterAware service = (INeutronRouterAware) instance;
                int status = service.canDeleteRouter(singleton);
                if (status < 200 || status > 299)
                    return Response.status(status).build();
            }
        }
        routerInterface.removeRouter(routerUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronRouterAware service = (INeutronRouterAware) instance;
                service.neutronRouterDeleted(singleton);
            }
        }
        return Response.status(204).build();
    }

    /**
     * Adds an interface to a router */

    @Path("{routerUUID}/add_router_interface")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackRouterInterfaces.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 400, condition = "Bad Request"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response addRouterInterface(
            @PathParam("routerUUID") String routerUUID,
            NeutronRouter_Interface input
            ) {
        INeutronRouterCRUD routerInterface = NeutronCRUDInterfaces.getINeutronRouterCRUD(this);
        if (routerInterface == null) {
            throw new ServiceUnavailableException("Router CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD(this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronSubnetCRUD subnetInterface = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        if (subnetInterface == null) {
            throw new ServiceUnavailableException("Subnet CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         *  While the Neutron specification says that the router has to exist and the input can only specify either a subnet id
         *  or a port id, but not both, this code assumes that the plugin has filled everything in for us and so both must be present
         */
        if (!routerInterface.routerExists(routerUUID))
            return Response.status(400).build();
        NeutronRouter target = routerInterface.getRouter(routerUUID);
        if (input.getSubnetUUID() == null ||
                    input.getPortUUID() == null)
                return Response.status(400).build();

        // check that the port is part of the subnet
        NeutronSubnet targetSubnet = subnetInterface.getSubnet(input.getSubnetUUID());
        if (targetSubnet == null)
            return Response.status(400).build();
        NeutronPort targetPort = portInterface.getPort(input.getPortUUID());
        if (targetPort == null)
            return Response.status(400).build();
        if (!targetSubnet.getPortsInSubnet().contains(targetPort))
            return Response.status(400).build();

        if (targetPort.getFixedIPs().size() != 1)
            return Response.status(400).build();
        if (targetPort.getDeviceID() != null ||
                targetPort.getDeviceOwner() != null)
            return Response.status(409).build();

        Object[] instances = ServiceHelper.getGlobalInstances(INeutronRouterAware.class, this, null);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronRouterAware service = (INeutronRouterAware) instance;
                service.canAttachInterface(target, input);
            }
        }

        //mark the port device id and device owner fields
        targetPort.setDeviceOwner("network:router_interface");
        targetPort.setDeviceID(routerUUID);

        target.addInterface(input.getPortUUID(), input);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronRouterAware service = (INeutronRouterAware) instance;
                service.neutronRouterInterfaceAttached(target, input);
            }
        }

        return Response.status(200).entity(input).build();
    }

    /**
     * Removes an interface to a router */

    @Path("{routerUUID}/remove_router_interface")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackRouterInterfaces.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 400, condition = "Bad Request"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 409, condition = "Conflict"),
            @ResponseCode(code = 501, condition = "Not Implemented") })
    public Response removeRouterInterface(
            @PathParam("routerUUID") String routerUUID,
            NeutronRouter_Interface input
            ) {
        INeutronRouterCRUD routerInterface = NeutronCRUDInterfaces.getINeutronRouterCRUD(this);
        if (routerInterface == null) {
            throw new ServiceUnavailableException("Router CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD(this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronSubnetCRUD subnetInterface = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        if (subnetInterface == null) {
            throw new ServiceUnavailableException("Subnet CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        // verify the router exists
        if (!routerInterface.routerExists(routerUUID))
            return Response.status(400).build();
        NeutronRouter target = routerInterface.getRouter(routerUUID);

        /*
         * remove by subnet id.  Collect information about the impacted router for the response and
         * remove the port corresponding to the gateway IP address of the subnet
         */
        if (input.getPortUUID() == null &&
                input.getSubnetUUID() != null) {
            NeutronPort port = portInterface.getGatewayPort(input.getSubnetUUID());
            if (port == null)
                return Response.status(404).build();
            input.setPortUUID(port.getID());
            input.setID(target.getID());
            input.setTenantID(target.getTenantID());

            Object[] instances = ServiceHelper.getGlobalInstances(INeutronRouterAware.class, this, null);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronRouterAware service = (INeutronRouterAware) instance;
                    service.canDetachInterface(target, input);
                }
            }

            // reset the port ownership
            port.setDeviceID(null);
            port.setDeviceOwner(null);

            target.removeInterface(input.getPortUUID());
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronRouterAware service = (INeutronRouterAware) instance;
                    service.neutronRouterInterfaceDetached(target, input);
                }
            }
            return Response.status(200).entity(input).build();
        }

        /*
         * remove by port id. collect information about the impacted router for the response
         * remove the interface and reset the port ownership
         */
        if (input.getPortUUID() != null &&
                input.getSubnetUUID() == null) {
            NeutronRouter_Interface targetInterface = target.getInterfaces().get(input.getPortUUID());
            input.setSubnetUUID(targetInterface.getSubnetUUID());
            input.setID(target.getID());
            input.setTenantID(target.getTenantID());
            NeutronPort port = portInterface.getPort(input.getPortUUID());
            port.setDeviceID(null);
            port.setDeviceOwner(null);
            target.removeInterface(input.getPortUUID());
            Object[] instances = ServiceHelper.getGlobalInstances(INeutronRouterAware.class, this, null);
            for (Object instance : instances) {
                INeutronRouterAware service = (INeutronRouterAware) instance;
                service.neutronRouterInterfaceDetached(target, input);
            }
            return Response.status(200).entity(input).build();
        }

        /*
         * remove by both port and subnet ID.  Verify that the first fixed IP of the port is a valid
         * IP address for the subnet, and then remove the interface, collecting information about the
         * impacted router for the response and reset port ownership
         */
        if (input.getPortUUID() != null &&
                input.getSubnetUUID() != null) {
            NeutronPort port = portInterface.getPort(input.getPortUUID());
            NeutronSubnet subnet = subnetInterface.getSubnet(input.getSubnetUUID());
            if (!subnet.isValidIP(port.getFixedIPs().get(0).getIpAddress()))
                return Response.status(409).build();
            input.setID(target.getID());
            input.setTenantID(target.getTenantID());
            Object[] instances = ServiceHelper.getGlobalInstances(INeutronRouterAware.class, this, null);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronRouterAware service = (INeutronRouterAware) instance;
                    service.canDetachInterface(target, input);
                }
            }
            port.setDeviceID(null);
            port.setDeviceOwner(null);
            target.removeInterface(input.getPortUUID());
            for (Object instance : instances) {
                INeutronRouterAware service = (INeutronRouterAware) instance;
                service.neutronRouterInterfaceDetached(target, input);
            }
            return Response.status(200).entity(input).build();
        }

        // have to specify either a port ID or a subnet ID
        return Response.status(400).build();
    }
}
