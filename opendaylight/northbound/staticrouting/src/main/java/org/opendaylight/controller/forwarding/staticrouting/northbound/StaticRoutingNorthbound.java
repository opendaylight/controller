
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
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
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;

/**
 * <p>Static Routing Northbound API allows for the management of the static
 * routes.</p>
 * </br>
 * An example request/response for retrieving the static routes may look like this: </br>
 * <pre>
 * GET http://localhost:8080/controller/nb/v2/staticroute/default HTTP/1.1
 * Accept: application/json
 *
 * HTTP/1.1 200 OK
 * Content-Type: application/json
 *
 * {"staticRoute":{"name":"route-1","prefix":"10.10.1.0/24","nextHop":"1.1.1.1"}}
 *
 * </pre>
 *
 * <br><br>
 * Authentication scheme : <b>HTTP Basic</b><br>
 * Authentication realm : <b>opendaylight</b><br>
 * Transport : <b>HTTP and HTTPS</b><br>
 * <br>
 */
@Path("/")
public class StaticRoutingNorthbound {

    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        username = context.getUserPrincipal().getName();
    }
    protected String getUserName() {
        return username;
    }



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
     * Get a list of static routes present on the given container.
     *
     * @param containerName Name of the Container. The Container name for the base controller is "default".
     * @return List of configured static routes on the given container
     *
     * <pre>
     * Example:
     *
     * Request URL:
     * GET http://localhost:8080/controller/nb/v2/staticroute/default
     *
     * Response in XML:
     *  &lt;list&gt;
     *   &lt;staticRoute&gt;
     *     &lt;name&gt;route-1&lt;/name&gt;
     *     &lt;prefix&gt;10.10.1.0/24&lt;/prefix&gt;
     *     &lt;nextHop&gt;1.1.1.1&lt;/nextHop&gt;
     *   &lt;/staticRoute&gt;
     *  &lt;/list&gt;
     *
     * Response in JSON:
     * {"staticRoute":{"name":"route-1","prefix":"10.10.1.0/24","nextHop":"1.1.1.1"}}
     *
     * </pre>
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

        if(!NorthboundUtils.isAuthorized(getUserName(), containerName,
                Privilege.WRITE, this)){
            throw new
                UnauthorizedException("User is not authorized to perform this operation on container "
                            + containerName);
        }
        return new StaticRoutes(getStaticRoutesInternal(containerName));
    }

    /**
     * Returns the static route for the provided configuration name on a given container
     *
     * @param containerName Name of the Container. The Container name for the base controller is "default".
     * @param route Name of the Static Route configuration
     * @return Static route configured with the supplied Name.
     *
     * <pre>
     * Example:
     *
     * Request URL:
     * GET http://localhost:8080/controller/nb/v2/staticroute/default/route/route-1
     *
     * Response in XML:
     *
     *   &lt;staticRoute&gt;
     *     &lt;name&gt;route-1&lt;/name&gt;
     *     &lt;prefix&gt;10.10.1.0/24&lt;/prefix&gt;
     *     &lt;nextHop&gt;1.1.1.1&lt;/nextHop&gt;
     *   &lt;/staticRoute&gt;
     *
     * Response in JSON:
     * {"name":"route-1","prefix":"10.10.1.0/24","nextHop":"1.1.1.1"}
     *
     * </pre>
     */
    @Path("/{containerName}/route/{route}")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(StaticRoute.class)
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or Static Route Configuration name passed was not found") })
    public StaticRoute getStaticRoute(
            @PathParam("containerName") String containerName,
            @PathParam("route") String route) {

        if(!NorthboundUtils.isAuthorized(getUserName(), containerName,
                Privilege.WRITE, this)){
            throw new
                UnauthorizedException("User is not authorized to perform this operation on container "
                            + containerName);
        }
        List<StaticRoute> routes = this.getStaticRoutesInternal(containerName);
        for (StaticRoute r : routes) {
            if (r.getName().equalsIgnoreCase(route)) {
                return r;
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
     * @param route Name of the Static Route configuration
     * @return Response as dictated by the HTTP Response code
     *
     * <pre>
     * Example:
     *
     * Request URL:
     * POST http://localhost:8080/controller/nb/v2/staticroute/default/route/route-1
     *
     * Request payload in JSON:
     * {"name":"route-1","prefix":"10.10.1.0/24","nextHop":"1.1.1.1"}
     *
     * </pre>
     */
    @Path("/{containerName}/route/{route}")
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
            @ResponseCode(code = 201, condition = "Created Static Route successfully"),
            @ResponseCode(code = 404, condition = "The Container Name passed is not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
            @ResponseCode(code = 409, condition = "Failed to create Static Route entry due to Conflicting Name or Prefix."), })
    public Response addStaticRoute(
            @Context UriInfo uriInfo,
            @PathParam(value = "containerName") String containerName,
            @PathParam(value = "route") String route,
            @TypeHint(StaticRoute.class) JAXBElement<StaticRoute> staticRouteData) {


        if(!NorthboundUtils.isAuthorized(getUserName(), containerName,
                Privilege.WRITE, this)){
            throw new
                UnauthorizedException("User is not authorized to perform this operation on container "
                            + containerName);
        }
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
            NorthboundUtils.auditlog("Static Route", username, "added", route, containerName);
            return Response.created(uriInfo.getRequestUri()).build();
        }
        throw new ResourceConflictException(response.getDescription());
    }

    /**
     *
     * Delete a Static Route
     *
     * @param containerName Name of the Container. The Container name for the base controller is "default".
     * @param route Name of the Static Route configuration to be removed
     *
     * @return Response as dictated by the HTTP Response code
     *
     * <pre>
     * Example:
     *
     * Request URL:
     * DELETE http://localhost:8080/controller/nb/v2/staticroute/default/route/route-1
     *
     * </pre>
     */
    @Path("/{containerName}/route/{route}")
    @DELETE
    @StatusCodes( {
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "Container Name or Configuration Name not found"),
            @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active") })
    public Response removeStaticRoute(
            @PathParam(value = "containerName") String containerName,
            @PathParam(value = "route") String route) {

        if(!NorthboundUtils.isAuthorized(getUserName(), containerName,
                Privilege.WRITE, this)){
            throw new
                UnauthorizedException("User is not authorized to perform this operation on container "
                            + containerName);
        }
        handleDefaultDisabled(containerName);

        IForwardingStaticRouting staticRouting = (IForwardingStaticRouting) ServiceHelper
                .getInstance(IForwardingStaticRouting.class, containerName,
                        this);

        if (staticRouting == null) {
            throw new ResourceNotFoundException(RestMessages.NOCONTAINER
                    .toString());
        }

        Status status = staticRouting.removeStaticRoute(route);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Static Route", username, "removed", route, containerName);
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
