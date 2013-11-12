/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.usermanager.northbound;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
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
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.BadRequestException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.UserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides REST APIs to manage users.
 * This API will only be availalbe via HTTPS.
 * <br>
 * <br>
 * Authentication scheme : <b>HTTP Basic</b><br>
 * Authentication realm : <b>opendaylight</b><br>
 * Transport : <b> HTTPS </b><br>
 * <br>
 * HTTPS Authentication is disabled by default so to
 * use UserManager APIs turn on HTTPS on Web Server
 */

@Path("/")
public class UserManagerNorthbound {

    protected static final Logger logger = LoggerFactory.getLogger(UserManagerNorthbound.class);

    private String username;
    @Context
    UriInfo uriInfo;

    @Context
    public void setSecurityContext(SecurityContext context) {
        if (context != null && context.getUserPrincipal() != null) {
            username = context.getUserPrincipal().getName();
        }
    }

    protected String getUserName() {
        return username;
    }

    private void handleNameMismatch(String name, String nameinURL) {
        if (name == null || nameinURL == null) {
            throw new BadRequestException(RestMessages.INVALIDDATA.toString() + " : Name is null");
        }

        if (name.equals(nameinURL)) {
            return;
        }
        throw new ResourceConflictException(RestMessages.INVALIDDATA.toString()
                + " : Name in URL does not match the name in request body");
    }

    /**
     * Add a user
     *
     * @param userConfigData
     *            the {@link UserConfig} user config structure in request body
     *
     * @return Response as dictated by the HTTP Response Status code
     *
     *         <pre>
     * Example:
     *
     * Request URL:
     * https://localhost/controller/nb/v2/usermanager/users
     *
     * Request body in XML:
     *  &lt;userConfig&gt;
     *      &lt;user&gt;testuser&lt;/user&gt;
     *      &lt;roles&gt;Network-Admin&lt;/roles&gt;
     *      &lt;password&gt;pass!23&lt;/password&gt;
     *  &lt;/userConfig&gt;
     *
     * Request body in JSON:
     * {
     *  "user":"testuser",
     *  "password":"pass!23",
     *  "roles":[
     *       "Network-Admin"
     *       ]
     * }
     * </pre>
     */

    @Path("/users")
    @POST
    @StatusCodes({ @ResponseCode(code = 201, condition = "User created successfully"),
        @ResponseCode(code = 400, condition = "Invalid data passed"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 409, condition = "User name in url conflicts with name in request body"),
        @ResponseCode(code = 404, condition = "User config is null"),
        @ResponseCode(code = 500, condition = "Internal Server Error: Addition of user failed"),
        @ResponseCode(code = 503, condition = "Service unavailable") })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response addLocalUser(@TypeHint(UserConfig.class) UserConfig userConfigData) {

        if (!isAdminUser()) {
            throw new UnauthorizedException("User is not authorized to perform user management operations ");
        }

        // Reconstructing the object so password can be hashed in userConfig
        UserConfig userCfgObject = new UserConfig(userConfigData.getUser(),userConfigData.getPassword(),
                 userConfigData.getRoles());

        IUserManager userManager = (IUserManager) ServiceHelper.getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            throw new ServiceUnavailableException("UserManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        Status status = userManager.addLocalUser(userCfgObject);
        if (status.isSuccess()) {

            NorthboundUtils.auditlog("User", username, "added", userCfgObject.getUser());
            URI uri = uriInfo.getAbsolutePathBuilder().path("/"+userCfgObject.getUser()).build();
            return Response.created(uri).build();
        }
        return NorthboundUtils.getResponse(status);
    }

    /**
     * Delete a user
     *
     * @param userName
     *            name of user to be deleted
     * @return Response as dictated by the HTTP Response Status code
     *
     * <pre>
     * Example:
     *
     * Request URL:
     * https://localhost/controller/nb/v2/usermanager/users/testuser
     *
     * </pre>
     */
    @Path("/users/{userName}")
    @DELETE
    @StatusCodes({ @ResponseCode(code = 204, condition = "User Deleted Successfully"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The userName passed was not found"),
        @ResponseCode(code = 500, condition = "Internal Server Error : Removal of user failed"),
        @ResponseCode(code = 503, condition = "Service unavailable") })
    public Response removeLocalUser(@PathParam("userName") String userToBeRemoved) {

        if (!isAdminUser()) {
            throw new UnauthorizedException("User is not authorized to perform user management operations ");
        }

        IUserManager userManager = (IUserManager) ServiceHelper.getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            throw new ServiceUnavailableException("UserManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        Status status = userManager.removeLocalUser(userToBeRemoved);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("User", username, "removed", userToBeRemoved);
            return Response.noContent().build();
        }
        return NorthboundUtils.getResponse(status);
    }

    private boolean isAdminUser(){
        // get UserManager's instance
        IUserManager auth = (IUserManager) ServiceHelper.getGlobalInstance(IUserManager.class, this);
        // check if logged in user has privileges of NETWORK_ADMIN or SYSTEM_ADMIN, if so return true
        return auth.getUserLevel(getUserName()).ordinal() <= UserLevel.NETWORKADMIN.ordinal();
    }

}
