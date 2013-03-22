
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwarding.staticrouting.northbound;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.forwarding.staticrouting.IForwardingStaticRouting;
import org.opendaylight.controller.forwarding.staticrouting.StaticRouteConfig;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
import org.opendaylight.controller.northbound.commons.exception.NotAcceptableException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;

/**
 * Static Routing Northbound APIs
 *
 * <br><br>
 * Authentication scheme : <b>HTTP Basic</b><br>
 * Authentication realm : <b>opendaylight</b><br>
 * Transport : <b>HTTP and HTTPS</b><br>
 * <br>
 * HTTPS Authentication is disabled by default. Administrator can enable it in tomcat-server.xml after adding 
 * a proper keystore / SSL certificate from a trusted authority.<br>
 * More info : http://tomcat.apache.org/tomcat-7.0-doc/ssl-howto.html#Configuration
 */
@Path("/")
public class StaticRoutingNorthbound {

    private List<StaticRoute> getStaticRoutesInternal(String containerName) {

        IForwardingStaticRouting staticRouting = (IForwardingStaticRouting) ServiceHelper
                .getInstance(IForwardingStaticRouting.class, containerName,
                        this);

        if (staticRouting == null) {
            throw new ResourceNotFoundException(RestMessages.NOCONTAINER
                    .toString());
        }

        List<StaticRoute> routes = new ArrayList<StaticRoute>();

        for (StaticRouteConfig conf : staticRouting.getStaticRouteConfigs()
                .values()) {
            StaticRoute route = new StaticRoute(conf.getName(), conf
                    .getStaticRoute(), conf.getNextHop());
            routes.add(route);
        }
        return routes;
    }

    /**
     * Returns a list of static routes present on the given container
     *
     * @param containerName Name of the Container. The Container name for the base controller is "default".
     * @return List of configured static routes on the given container
     */
    @Path("/{containerName}")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(StaticRoutes.class)
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName passed was not found") })
    public StaticRoutes getStaticRoutes(
            @PathParam("containerName") String containerName) {
        return new StaticRoutes(getStaticRoutesInternal(containerName));
    }

    /**
     * Returns the static route for the provided configuration name on a given container
     *
     * @param containerName Name of the Container. The Container name for the base controller is "default".
     * @param name Name of the Static Route configuration
     * @return Static route configured with the supplied Name.
     */
    @Path("/{containerName}/{name}")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(StaticRoute.class)
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or Static Route Configuration name passed was not found") })
    public StaticRoute getStaticRoute(
            @PathParam("containerName") String containerName,
            @PathParam("name") String name) {
        List<StaticRoute> routes = this.getStaticRoutesInternal(containerName);
        for (StaticRoute route : routes) {
            if (route.getName().equalsIgnoreCase(name)) {
                return route;
            }
        }

        throw new ResourceNotFoundException(RestMessages.NOSTATICROUTE
                .toString());
    }

    /**
     *
     * Add a new Static Route
     *
     * @param containerName Name of the Container. The Container name for the base controller is "default".
     * @param name Name of the Static Route configuration
     * @return Response as dictated by the HTTP Response code
     */
    @Path("/{containerName}/{name}")
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
            @ResponseCode(code = 201, condition = "Created Static Route successfully"),
            @ResponseCode(code = 404, condition = "The Container Name passed is not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
            @ResponseCode(code = 409, condition = "Failed to create Static Route entry due to Conflicting Name or Prefix."), })
    public Response addStaticRoute(
            @PathParam(value = "containerName") String containerName,
            @PathParam(value = "name") String name,
            @TypeHint(StaticRoute.class) JAXBElement<StaticRoute> staticRouteData) {

        handleDefaultDisabled(containerName);

        IForwardingStaticRouting staticRouting = (IForwardingStaticRouting) ServiceHelper
                .getInstance(IForwardingStaticRouting.class, containerName,
                        this);

        if (staticRouting == null) {
            throw new ResourceNotFoundException(RestMessages.NOCONTAINER
                    .toString());
        }

        StaticRoute sRoute = staticRouteData.getValue();
        StaticRouteConfig cfgObject = new StaticRouteConfig(sRoute.getName(),
                sRoute.getPrefix(), sRoute.getNextHop());
        Status response = staticRouting.addStaticRoute(cfgObject);
        if (response.isSuccess()) {
            return Response.status(Response.Status.CREATED).build();
        }
        throw new ResourceConflictException(response.getDescription());
    }

    /**
     *
     * Delete a Static Route
     *
     * @param containerName Name of the Container. The Container name for the base controller is "default".
     * @param name Name of the Static Route configuration to be removed
     *
     * @return Response as dictated by the HTTP Response code
     */

    @Path("/{containerName}/{name}")
    @DELETE
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "Container Name or Configuration Name not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active") })
    public Response removeStaticRoute(
            @PathParam(value = "containerName") String containerName,
            @PathParam(value = "name") String name) {

        handleDefaultDisabled(containerName);

        IForwardingStaticRouting staticRouting = (IForwardingStaticRouting) ServiceHelper
                .getInstance(IForwardingStaticRouting.class, containerName,
                        this);

        if (staticRouting == null) {
            throw new ResourceNotFoundException(RestMessages.NOCONTAINER
                    .toString());
        }

        Status status = staticRouting.removeStaticRoute(name);
        if (status.isSuccess()) {
            return Response.ok().build();
        }
        throw new ResourceNotFoundException(status.getDescription());
    }

    private void handleDefaultDisabled(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            throw new InternalServerErrorException(RestMessages.INTERNALERROR
                    .toString());
        }
        if (containerName.equals(GlobalConstants.DEFAULT.toString())
                && containerManager.hasNonDefaultContainer()) {
            throw new NotAcceptableException(RestMessages.DEFAULTDISABLED
                    .toString());
        }
    }
}
