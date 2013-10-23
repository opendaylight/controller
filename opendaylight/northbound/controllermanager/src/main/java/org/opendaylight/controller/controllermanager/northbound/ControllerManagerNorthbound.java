/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.controllermanager.northbound;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
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
 * The class provides Northbound REST APIs to access the controller properties.
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

    private ISwitchManager getIfSwitchManagerService(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper.getGlobalInstance(
                IContainerManager.class, this);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames) {
            if (cName.trim().equalsIgnoreCase(containerName.trim())) {
                found = true;
                break;
            }
        }

        if (found == false) {
            throw new ResourceNotFoundException(containerName + " " + RestMessages.NOCONTAINER.toString());
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName,
                this);

        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return switchManager;
    }

    /**
     * Retrieve a list of all controller properties in the network
     *
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @return A list of controller properties
     *         {@link org.opendaylight.controller.sal.core.Property}
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/controllermanager/default/property/
     *
     *
     * Response body in XML:
     *
     * <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
     * <controllerProperties>
     *     <properties>
     *             <macAddress>
     *                         <value>3e:04:ef:11:13:80</value>
     *             </macAddress>
     *    </properties>
     * </controllerProperties>
     *
     * Response body in JSON:
     *
     * {
     *   "controllerProperties":
     *   {
     *       "properties":
     *       {
     *          "macAddress": { "value": "3e:04:ef:11:13:80" }
     *       }
     *   }
     * }
     *
     */
    @Path("/{containerName}/property")
    @GET
    @TypeHint(ControllerProperties.class)
    @StatusCodes({@ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable")})
    public ControllerProperties getControllerProperties(@PathParam("containerName") String containerName)
    {
        if (!isValidContainer(containerName)) {
            throw new ResourceNotFoundException("Container " + containerName + " does not exist.");
        }

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Map<String, Property> propertyMap = switchManager.getControllerProperties();
        if(propertyMap == null){
            throw new ResourceNotFoundException(RestMessages.INVALIDDATA.toString());
        }
        Set<Property> properties = new HashSet<Property>(propertyMap.values());

        return new ControllerProperties(properties);
    }

    /**
     * Retrieve a property for the controller in the network
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
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/controllermanager/default/property/macAddress
     *
     * Response Body in XML:
     * <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
     * <macAddress>
     *     <value>3e:04:ef:11:13:80</value>
     * </macAddress>
     *
     * Response Body in JSON
     * {
     *   "macAddress": { "value": "3e:04:ef:11:13:80" }
     * }
     *
     */
    @Path("/{containerName}/property/{propertyName}")
    @GET
    @TypeHint(Property.class)
    @StatusCodes({@ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName or property is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable")})
    public Property getControllerProperty(@PathParam("containerName") String containerName, @PathParam("propertyName") String propertyName)
    {

        if (!isValidContainer(containerName)) {
            throw new ResourceNotFoundException("Container " + containerName + " does not exist.");
        }

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Property property = switchManager.getControllerProperty(propertyName);
        if(property==null){
            throw new ResourceNotFoundException(RestMessages.INVALIDDATA.toString());
        }

        return property;
    }

    /**
     * Add a controller property to the controller. This method overrides previously set property values if the property already exist.
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
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/controllermanager/default/property/description/defaultController
     */
    @Path("/{containerName}/property/{propertyName}/{propertyValue}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 201, condition = "Operation successful"),
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

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Property prop = switchManager.createProperty(propertyName, propertyValue);
        if (prop == null) {
            throw new ResourceNotFoundException("Property with name " + propertyName + " does not exist.");
        }

        Status status = switchManager.setControllerProperty(prop);

      if (status.isSuccess()) {
          NorthboundUtils.auditlog("Property " + propertyName, username, "updated", containerName);
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
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/controllermanager/default/property/description
     */
    @Path("/{containerName}/property/{propertyName}")
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

        ISwitchManager switchManager = getIfSwitchManagerService(containerName);
        if (switchManager == null) {
            throw new ServiceUnavailableException("Switch Manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Status status = switchManager.removeControllerProperty(propertyName);

        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Property " + propertyName, username, "removed", containerName);

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
            throw new InternalServerErrorException(RestMessages.INTERNALERROR.toString());
        }
        if (containerManager.getContainerNames().contains(containerName)) {
            return true;
        }
        return false;
    }

}
