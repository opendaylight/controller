
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.usermanager.internal.AuthorizationConfig;
import org.opendaylight.controller.usermanager.internal.ServerConfig;
import org.opendaylight.controller.usermanager.internal.UserConfig;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * The Interface which describes the methods exposed by User Manager.
 */
public interface IUserManager extends UserDetailsService {

    /**
     * Returns the list of roles associated to the passed user name
     *
     * @param userName
     * @return the role associated to the user name
     */
    public List<String> getUserRoles(String userName);

    /**
     * Authenticate user with AAA server and return authentication and authorization info
     *
     * @param username	the username
     * @param password	the password
     * @return			{@link org.opendaylight.controller.sal.authorization.AuthResultEnum authenticate result}
     */
    public AuthResultEnum authenticate(String username, String password);

    /**
     * Add/remove AAA server
     *
     * @param configObject	refer to {@link org.opendaylight.controller.usermanager.internal.ServerConfig ServerConfig}
     * @return				status code
     */
    public Status addAAAServer(ServerConfig configObject);

    /**
     * Remove AAA server
     *
     * @param configObject	refer to {@link org.opendaylight.controller.usermanager.internal.ServerConfig ServerConfig}
     * @return				status code
     */
    public Status removeAAAServer(ServerConfig configObject);

    /**
     * Add a local user
     *
     * @param configObject refer to {@link org.opendaylight.controller.usermanager.internal.UserConfig UserConfig}
     * @return				status code
     */
    public Status addLocalUser(UserConfig configObject);

    /**
     * Remove a local user
     *
     * @param configObject refer to {@link org.opendaylight.controller.usermanager.internal.UserConfig UserConfig}
     * @return				status code
     */
    public Status removeLocalUser(UserConfig configObject);
    
    /**
     * Remove a local user
     * 
     * @param userName the user name
     * @return the status of this action
     */
    public Status removeLocalUser(String userName);

    /**
     * Add the authorization information for a user that gets authenticated remotely
     *
     * @param AAAconf
     * @return
     */
    public Status addAuthInfo(AuthorizationConfig AAAconf);

    /**
     * Remove the authorization information for a user that gets authenticated remotely
     *
     * @param AAAconf
     * @return
     */
    public Status removeAuthInfo(AuthorizationConfig AAAconf);

    /**
     * Return the list of authorization resources
     * @return
     */
    public List<AuthorizationConfig> getAuthorizationList();

    /**
     * Returns a list of AAA Providers.
     * @return Set of provider names.
     */
    public Set<String> getAAAProviderNames();

    /**
     * Change the current password for a configured user
     *
     * @param user
     * @param curPasssword
     * @param newPassword
     * @return
     */
    public Status changeLocalUserPassword(String user, String curPassword,
            String newPassword);

    /**
     * Return a list of AAA servers currently configured
     *
     * @return list of {@link org.opendaylight.controller.usermanager.internal.ServerConfig ServerConfig}
     */
    public List<ServerConfig> getAAAServerList();

    /**
     * Return a list of local users
     *
     * @return list of {@link org.opendaylight.controller.usermanager.internal.UserConfig UserConfig}
     */
    public List<UserConfig> getLocalUserList();

    /**
     * Save the local users to local disk
     *
     * @return status code
     */
    public Status saveLocalUserList();

    /**
     * Save the AAA server configurations to local disk
     *
     * @return status code
     */
    public Status saveAAAServerList();

    /**
     * Save the Authorization configurations to local disk
     *
     * @return status code
     */
    public Status saveAuthorizationList();

    /**
     * Remove user profile when user logs out
     *
     * @param username	the user name
     */
    public void userLogout(String username);

    /**
     * Remove user profile when user times out
     *
     * @param username	the user name
     */
    public void userTimedOut(String username);

    /**
     * Get the list of users currently logged in
     *
     * @return the list of users along with their administrative roles
     */
    public Map<String, List<String>> getUserLoggedIn();

    /**
     * Get date and time user was successfully authenticated
     *
     * @param user
     * @return Date in String format
     */
    public String getAccessDate(String user);

    /**
     * Returns the user level for the passed user name
     * It check the roles assigned to this user and checks
     * against the well known Controller user roles to
     * determines the highest user level associated with
     * the user
     *
     * @param userName the user name
     * @return	the highest user level for this user
     */
    public UserLevel getUserLevel(String userName);

    // For internal use. Place holder to move securityContext storage.
    public SecurityContextRepository getSecurityContextRepo();

    // Session manager to implement session mgmt across web-apps
    public ISessionManager getSessionManager();
    
    
    public String getPassword(String username);

}
