/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.controllermanager.northbound;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.BadRequestException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.switchmanager.ISwitchManager;

/**
 * The class provides Northbound REST APIs to manager the controller. Currently
 * it supports getting controller property(ies), setting a property, and
 * removing a property
 *
 */

@Path("/")
public class ControllerManagerNorthbound {

    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        if (context != null && context.getUserPrincipal() != null) {
            username = context.getUserPrincipal().getName();
        }
    }

    protected String getUserName() {
        return username;
    }

    private ISwitchManager getISwitchManagerService(String containerName) {
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);

        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return switchManager;
    }

    /**
     * Retrieve a property or all properties for the controller in the network
     *
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @param propertyName
     *            Name of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     *
     *            Example:
     *
     *            Request URL:
     *            http://localhost:8080/controller/nb/v2/controllermanager/default/properties/?propertyName=macAddress
     *
     *            Response Body in XML:
     *            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
     *            <controllerProperties>
     *                  <properties>
     *                           <macAddress>
     *                                   <value>3e:04:ef:11:13:80</value>
     *                          </macAddress>
     *                   </properties>
     *            </controllerProperties>
     *
     *            Response Body in JSON:
     *            { "controllerProperties":
     *                  {"properties":
     *                          { "macAddress":
     *                                  { "value": "3e:04:ef:11:13:80" }
     *                           }
     *                   }
     *            }
     *
     */
    @Path("/{containerName}/properties/")
    @GET
    @TypeHint(Property.class)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
            @ResponseCode(code = 404, condition = "The containerName or property is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public ControllerProperties getControllerProperties(@PathParam("containerName") String containerName,
            @QueryParam("propertyName") String propertyName) {

        if (!isValidContainer(containerName)) {
            throw new ResourceNotFoundException("Container " + containerName + " does not exist.");
        }

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }

        ISwitchManager switchManager = getISwitchManagerService(containerName);

        if (propertyName == null) {
            Map<String, Property> propertyMap = switchManager.getControllerProperties();
            Set<Property> properties = new HashSet<Property>(propertyMap.values());
            return new ControllerProperties(properties);
        }

        Set<Property> properties = new HashSet<Property>();
        Property property = switchManager.getControllerProperty(propertyName);
        if (property == null) {
            throw new ResourceNotFoundException("Unable to find property with name: " + propertyName);
        }
        properties.add(property);

        return new ControllerProperties(properties);

    }

    /**
     * Add a controller property to the controller. This method overrides
     * previously set property values if the property already exist.
     *
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @param propertyName
     *            Name of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @param propertyValue
     *            Value of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @return Response as dictated by the HTTP Response Status code
     *
     *         Example:
     *
     *         Request URL:
     *         http://localhost:8080/controller/nb/v2/controllermanager/default/properties/description/defaultController
     */
    @Path("/{containerName}/properties/{propertyName}/{propertyValue}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 201, condition = "Operation successful"),
            @ResponseCode(code = 400, condition = "Invalid property parameters"),
            @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
            @ResponseCode(code = 404, condition = "The containerName or property is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Response setControllerProperty(@Context UriInfo uriInfo, @PathParam("containerName") String containerName,
            @PathParam("propertyName") String propertyName, @PathParam("propertyValue") String propertyValue) {

        if (!isValidContainer(containerName)) {
            throw new ResourceNotFoundException("Container " + containerName + " does not exist.");
        }

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }

        ISwitchManager switchManager = getISwitchManagerService(containerName);

        Property prop = switchManager.createProperty(propertyName, propertyValue);
        if (prop == null) {
            throw new BadRequestException("Property with name " + propertyName + " cannot be created.");
        }

        Status status = switchManager.setControllerProperty(prop);

        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Controller Property", username, "updated", propertyName);
            return Response.created(uriInfo.getRequestUri()).build();
        }
        return NorthboundUtils.getResponse(status);
    }

    /**
     * Delete a property of the controller
     *
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @param propertyName
     *            Name of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @return Response as dictated by the HTTP Response Status code
     *
     *         Example:
     *
     *         Request URL:
     *         http://localhost:8080/controller/nb/v2/controllermanager/default/properties/description
     */
    @Path("/{containerName}/properties/{propertyName}")
    @DELETE
    @StatusCodes({ @ResponseCode(code = 204, condition = "Property removed successfully"),
            @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Response removeControllerProperty(@PathParam("containerName") String containerName,
            @PathParam("propertyName") String propertyName) {

        if (!isValidContainer(containerName)) {
            throw new ResourceNotFoundException("Container " + containerName + " does not exist.");
        }

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }

        ISwitchManager switchManager = getISwitchManagerService(containerName);

        Status status = switchManager.removeControllerProperty(propertyName);

        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Controller Property", username, "removed", propertyName);

            return Response.noContent().build();
        }
        return NorthboundUtils.getResponse(status);
    }

    private boolean isValidContainer(String containerName) {
        if (containerName.equals(GlobalConstants.DEFAULT.toString())) {
            return true;
        }
        IContainerManager containerManager = (IContainerManager) ServiceHelper.getGlobalInstance(
                IContainerManager.class, this);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (containerManager.getContainerNames().contains(containerName)) {
            return true;
        }
        return false;
    }

}
