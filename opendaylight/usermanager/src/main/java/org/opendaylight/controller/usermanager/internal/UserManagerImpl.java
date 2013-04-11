/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.IConfigurationAware;
import org.opendaylight.controller.containermanager.IContainerAuthorization;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.authorization.IResourceAuthorization;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.ObjectReader;
import org.opendaylight.controller.sal.utils.ObjectWriter;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.usermanager.AuthResponse;
import org.opendaylight.controller.usermanager.IAAAProvider;
import org.opendaylight.controller.usermanager.ISessionManager;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.security.SessionManager;
import org.opendaylight.controller.usermanager.security.UserSecurityContextRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * The internal implementation of the User Manager.
 */
public class UserManagerImpl implements IUserManager, IObjectReader,
        IConfigurationAware, ICacheUpdateAware<Long, String>, CommandProvider,
        AuthenticationProvider {
    private static final Logger logger = LoggerFactory
            .getLogger(UserManagerImpl.class);
    private static final String defaultAdmin = "admin";
    private static final String defaultAdminPassword = "admin";
    private static final String defaultAdminRole = UserLevel.NETWORKADMIN
            .toString();
    private static final String ROOT = GlobalConstants.STARTUPHOME.toString();
    private static final String SAVE = "save";
    private static final String usersFileName = ROOT + "users.conf";
    private static final String serversFileName = ROOT + "servers.conf";
    private static final String authFileName = ROOT + "authorization.conf";
    private ConcurrentMap<String, UserConfig> localUserConfigList;
    private ConcurrentMap<String, ServerConfig> remoteServerConfigList;
    private ConcurrentMap<String, AuthorizationConfig> authorizationConfList; // local authorization info for remotely authenticated users
    private ConcurrentMap<String, AuthenticatedUser> activeUsers;
    private ConcurrentMap<String, IAAAProvider> authProviders;
    private ConcurrentMap<Long, String> localUserListSaveConfigEvent,
            remoteServerSaveConfigEvent, authorizationSaveConfigEvent;
    private IClusterGlobalServices clusterGlobalService = null;
    private SecurityContextRepository securityContextRepo = new UserSecurityContextRepository();
    private IContainerAuthorization containerAuthorizationClient;
    private Set<IResourceAuthorization> applicationAuthorizationClients;
    private ISessionManager sessionMgr = new SessionManager();

    public boolean addAAAProvider(IAAAProvider provider) {
        if (provider == null
        		|| provider.getName() == null
                || provider.getName().trim().isEmpty()) {
            return false;
        }
        if (authProviders.get(provider.getName()) != null) {
            return false;
        }

        authProviders.put(provider.getName(), provider);
        return true;
    }

    public void removeAAAProvider(IAAAProvider provider) {
        authProviders.remove(provider.getName());
    }

    public IAAAProvider getAAAProvider(String name) {
        return authProviders.get(name);
    }

    public Set<String> getAAAProviderNames() {
        return authProviders.keySet();
    }

    @SuppressWarnings("deprecation")
    private void allocateCaches() {
        this.applicationAuthorizationClients = Collections
                .synchronizedSet(new HashSet<IResourceAuthorization>());
        if (clusterGlobalService == null) {
            logger
                    .error("un-initialized clusterGlobalService, can't create cache");
            return;
        }

        try {
            clusterGlobalService.createCache("usermanager.localUserConfigList",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterGlobalService.createCache(
                    "usermanager.remoteServerConfigList", EnumSet
                            .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterGlobalService.createCache(
                    "usermanager.authorizationConfList", EnumSet
                            .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterGlobalService.createCache("usermanager.activeUsers", EnumSet
                    .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterGlobalService.createCache(
                    "usermanager.localUserSaveConfigEvent", EnumSet
                            .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterGlobalService.createCache(
                    "usermanager.remoteServerSaveConfigEvent", EnumSet
                            .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterGlobalService.createCache(
                    "usermanager.authorizationSaveConfigEvent", EnumSet
                            .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger.error("\nCache configuration invalid - check cache mode");
        } catch (CacheExistException ce) {
            logger
                    .error("\nCache already exits - destroy and recreate if needed");
        }
    }

    @SuppressWarnings( { "unchecked", "deprecation" })
    private void retrieveCaches() {
        if (clusterGlobalService == null) {
            logger.error("un-initialized clusterService, can't retrieve cache");
            return;
        }

        activeUsers = (ConcurrentMap<String, AuthenticatedUser>) clusterGlobalService
                .getCache("usermanager.activeUsers");
        if (activeUsers == null) {
            logger.error("\nFailed to get cache for activeUsers");
        }

        localUserConfigList = (ConcurrentMap<String, UserConfig>) clusterGlobalService
                .getCache("usermanager.localUserConfigList");
        if (localUserConfigList == null) {
            logger.error("\nFailed to get cache for localUserConfigList");
        }

        remoteServerConfigList = (ConcurrentMap<String, ServerConfig>) clusterGlobalService
                .getCache("usermanager.remoteServerConfigList");
        if (remoteServerConfigList == null) {
            logger.error("\nFailed to get cache for remoteServerConfigList");
        }

        authorizationConfList = (ConcurrentMap<String, AuthorizationConfig>) clusterGlobalService
                .getCache("usermanager.authorizationConfList");
        if (authorizationConfList == null) {
            logger.error("\nFailed to get cache for authorizationConfList");
        }

        localUserListSaveConfigEvent = (ConcurrentMap<Long, String>) clusterGlobalService
                .getCache("usermanager.localUserSaveConfigEvent");
        if (localUserListSaveConfigEvent == null) {
            logger.error("\nFailed to get cache for localUserSaveConfigEvent");
        }

        remoteServerSaveConfigEvent = (ConcurrentMap<Long, String>) clusterGlobalService
                .getCache("usermanager.remoteServerSaveConfigEvent");
        if (remoteServerSaveConfigEvent == null) {
            logger
                    .error("\nFailed to get cache for remoteServerSaveConfigEvent");
        }

        authorizationSaveConfigEvent = (ConcurrentMap<Long, String>) clusterGlobalService
                .getCache("usermanager.authorizationSaveConfigEvent");
        if (authorizationSaveConfigEvent == null) {
            logger
                    .error("\nFailed to get cache for authorizationSaveConfigEvent");
        }
    }

    private void loadConfigurations() {
    	// To encode and decode user and server configuration objects
    	loadSecurityKeys();
    	
        /*
         * Do not load local startup file if we already got the
         * configurations synced from another cluster node
         */
        if (localUserConfigList.isEmpty()) {
            loadUserConfig();
        }
        if (remoteServerConfigList.isEmpty()) {
            loadServerConfig();
        }
        if (authorizationConfList.isEmpty()) {
            loadAuthConfig();
        }
    }

    private void loadSecurityKeys() {
		
	}

	private void checkDefaultNetworkAdmin() {
        // If startup config is not there, it's old or it was deleted, 
		// need to add Default Admin
        if (!localUserConfigList.containsKey(defaultAdmin)) {
        	localUserConfigList.put(defaultAdmin,
        					new UserConfig(defaultAdmin,
        							defaultAdminPassword,
            						defaultAdminRole));
        }
    }

    @Override
    public AuthResultEnum authenticate(String userName, String password) {
        IAAAProvider aaaClient;
        AuthResponse rcResponse = null;
        AuthenticatedUser result;
        String[] adminRoles = null;
        boolean remotelyAuthenticated = false;
        boolean authorizationInfoIsPresent = false;
        boolean authorized = false;

        /*
         * Attempt remote authentication first if server is configured
         */
        for (ServerConfig aaaServer : remoteServerConfigList.values()) {
            String protocol = aaaServer.getProtocol();
            aaaClient = this.getAAAProvider(protocol);
            if (aaaClient != null) {
                rcResponse = aaaClient.authService(userName, password,
                        aaaServer.getAddress(), aaaServer.getSecret());
                if (rcResponse.getStatus() == AuthResultEnum.AUTH_ACCEPT) {
                    logger
                            .info(
                                    "Remote Authentication Succeeded for User: \"{}\", by Server: {}",
                                    userName, aaaServer.getAddress());
                    remotelyAuthenticated = true;
                    break;
                } else if (rcResponse.getStatus() == AuthResultEnum.AUTH_REJECT) {
                    logger.info(
                            "Remote Authentication Rejected User: \"{}\", from Server: {}, Reason: "
                                    + rcResponse.getStatus().toString(),
                            userName, aaaServer.getAddress());
                } else {
                    logger.info(
                            "Remote Authentication Failed for User: \"{}\", from Server: {}, Reason: "
                                    + rcResponse.getStatus().toString(),
                            userName, aaaServer.getAddress());
                }
            }
        }

        if (!remotelyAuthenticated) {
            UserConfig localUser = this.localUserConfigList.get(userName);
            if (localUser == null) {
                logger.info(
                        "Local Authentication Failed for User:\"{}\", Reason: "
                                + "user not found in Local Database", userName);
                return (AuthResultEnum.AUTH_INVALID_LOC_USER);
            }
            rcResponse = localUser.authenticate(password);
            if (rcResponse.getStatus() != AuthResultEnum.AUTH_ACCEPT_LOC) {
                logger.info("Local Authentication Failed for User: \"{}\", Reason: {}",
                                userName, rcResponse.getStatus().toString());
                
                return (rcResponse.getStatus());
            }
            logger.info("Local Authentication Succeeded for User: \"{}\"",
                    userName);
        }

        /*
         * Authentication succeeded
         */
        result = new AuthenticatedUser(userName);

        /*
         * Extract attributes from response
         * All the information we are interested in is in the first Cisco VSA (vendor specific attribute).
         * Just process the first VSA and return
         */
        String attributes = (rcResponse.getData() != null && !rcResponse
                .getData().isEmpty()) ? rcResponse.getData().get(0) : null;

        /*
         * Check if the authorization information is present
         */
        authorizationInfoIsPresent = checkAuthorizationInfo(attributes);

        /*
         * The AAA server was only used to perform the authentication
         * Look for locally stored authorization info for this user
         * If found, add the data to the rcResponse
         */
        if (remotelyAuthenticated && !authorizationInfoIsPresent) {
            logger
                    .info(
                            "No Remote Authorization Info provided by Server for User: \"{}\"",
                            userName);
            logger.info(
                    "Looking for Local Authorization Info for User: \"{}\"",
                    userName);

            AuthorizationConfig resource = authorizationConfList.get(userName);
            if (resource != null) {
                logger.info("Found Local Authorization Info for User: \"{}\"",
                        userName);
                attributes = resource.getRolesData();

            }
            authorizationInfoIsPresent = checkAuthorizationInfo(attributes);
        }

        /*
         * Common response parsing for local & remote authenticated user
         * Looking for authorized resources, detecting attributes' validity
         */
        if (authorizationInfoIsPresent) {
        	// Identifying the administrative role
            adminRoles = attributes.split(" ");
            result.setRoleList(adminRoles);
            authorized = true;
        } else {
            logger.info("Not able to find Authorization Info for User: \"{}\"",
                    userName);
        }

        /*
         * Add profile for authenticated user
         */
        putUserInActiveList(userName, result);
        if (authorized) {
            logger.info("User \"{}\" authorized for the following role(s): "
                    + result.getUserRoles(), userName);
        } else {
            logger.info("User \"{}\" Not Authorized for any role ", userName);
        }

        return rcResponse.getStatus();
    }

    // Check in the attributes string whether or not authorization information is present
    private boolean checkAuthorizationInfo(String attributes) {
        return (attributes != null && !attributes.isEmpty());
    }

    private void putUserInActiveList(String user, AuthenticatedUser result) {
        activeUsers.put(user, result);
    }

    private void removeUserFromActiveList(String user) {
        if (!activeUsers.containsKey(user)) {
            // as cookie persists in cache, we can get logout for unexisting active users
            return;
        }
        activeUsers.remove(user);
    }

    public Status saveLocalUserList() {
        // Publish the save config event to the cluster nodes
        localUserListSaveConfigEvent.put(new Date().getTime(), SAVE);
        return saveLocalUserListInternal();
    }

    private Status saveLocalUserListInternal() {
        ObjectWriter objWriter = new ObjectWriter();
        return objWriter.write(new ConcurrentHashMap<String, UserConfig>(
                localUserConfigList), usersFileName);
    }

    public Status saveAAAServerList() {
        // Publish the save config event to the cluster nodes
        remoteServerSaveConfigEvent.put(new Date().getTime(), SAVE);
        return saveAAAServerListInternal();
    }

    private Status saveAAAServerListInternal() {
        ObjectWriter objWriter = new ObjectWriter();
        return objWriter.write(new ConcurrentHashMap<String, ServerConfig>(
                remoteServerConfigList), serversFileName);
    }

    public Status saveAuthorizationList() {
        // Publish the save config event to the cluster nodes
        authorizationSaveConfigEvent.put(new Date().getTime(), SAVE);
        return saveAuthorizationListInternal();
    }

    private Status saveAuthorizationListInternal() {
        ObjectWriter objWriter = new ObjectWriter();
        return objWriter.write(
                new ConcurrentHashMap<String, AuthorizationConfig>(
                        authorizationConfList), authFileName);
    }

    @Override
    public Object readObject(ObjectInputStream ois)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        // Perform the class deserialization locally, from inside the package where the class is defined
        return ois.readObject();
    }

    @SuppressWarnings("unchecked")
    private void loadUserConfig() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<String, UserConfig> confList = (ConcurrentMap<String, UserConfig>) objReader
                .read(this, usersFileName);

        if (confList == null) {
            return;
        }

        for (UserConfig conf : confList.values()) {
            addLocalUser(conf);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadServerConfig() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<String, ServerConfig> confList = (ConcurrentMap<String, ServerConfig>) objReader
                .read(this, serversFileName);

        if (confList == null) {
            return;
        }

        for (ServerConfig conf : confList.values()) {
            addAAAServer(conf);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadAuthConfig() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<String, AuthorizationConfig> confList = (ConcurrentMap<String, AuthorizationConfig>) objReader
                .read(this, authFileName);

        if (confList == null) {
            return;
        }

        for (AuthorizationConfig conf : confList.values()) {
            addAuthInfo(conf);
        }
    }

    /*
     * Interaction with GUI START
     */
    public Status addRemoveLocalUser(UserConfig AAAconf, boolean delete) {
        // Validation check
        if (!AAAconf.isValid()) {
        	String msg = "Invalid Local User configuration";
            logger.warn(msg);
            return new Status(StatusCode.BADREQUEST, msg);
        }

        // Update Config database
        if (delete) {
        	if (AAAconf.getUser().equals(UserManagerImpl.defaultAdmin)) {
        		String msg = "Invalid Request: Default Network Admin  User " +
        				"cannot be deleted";
        		logger.debug(msg);
        		return new Status(StatusCode.NOTALLOWED, msg);
        	}
            localUserConfigList.remove(AAAconf.getUser());
        } else {
        	if (AAAconf.getUser().equals(UserManagerImpl.defaultAdmin)) {
        		String msg = "Invalid Request: Default Network Admin  User " +
        				"cannot be added";
        		logger.debug(msg);
        		return new Status(StatusCode.NOTALLOWED, msg);
        	}
            localUserConfigList.put(AAAconf.getUser(), AAAconf);
        }

        return new Status(StatusCode.SUCCESS, null);
    }

    private Status addRemoveAAAServer(ServerConfig AAAconf, boolean delete) {
        // Validation check
        if (!AAAconf.isValid()) {
        	String msg = "Invalid Server configuration";
            logger.warn(msg);
            return new Status(StatusCode.BADREQUEST, msg);
        }

        // Update configuration database
        if (delete) {
            remoteServerConfigList.remove(AAAconf.getAddress());
        } else {
            remoteServerConfigList.put(AAAconf.getAddress(), AAAconf);
        }

        return new Status(StatusCode.SUCCESS, null);
    }

    private Status addRemoveAuthInfo(AuthorizationConfig AAAconf,
            boolean delete) {
        if (!AAAconf.isValid()) {
        	String msg = "Invalid Authorization configuration";
            logger.warn(msg);
            return new Status(StatusCode.BADREQUEST, msg);
        }

        // Update configuration database
        if (delete) {
            authorizationConfList.remove(AAAconf.getUser());
        } else {
            authorizationConfList.put(AAAconf.getUser(), AAAconf);
        }

        return new Status(StatusCode.SUCCESS, null);
    }

    @Override
    public Status addLocalUser(UserConfig AAAconf) {
        return addRemoveLocalUser(AAAconf, false);
    }

    @Override
    public Status removeLocalUser(UserConfig AAAconf) {
        return addRemoveLocalUser(AAAconf, true);
    }

    @Override
    public Status removeLocalUser(String userName) {
    	if (userName == null || userName.trim().isEmpty()) {
    		return new Status(StatusCode.BADREQUEST, "Invalid user name");
    	}
    	if (!localUserConfigList.containsKey(userName)) {
    		return new Status(StatusCode.NOTFOUND, "User does not exist");
    	}    	
        return addRemoveLocalUser(localUserConfigList.get(userName), true);
    }
    @Override
    public Status addAAAServer(ServerConfig AAAconf) {
        return addRemoveAAAServer(AAAconf, false);
    }

    @Override
    public Status removeAAAServer(ServerConfig AAAconf) {
        return addRemoveAAAServer(AAAconf, true);
    }

    @Override
    public Status addAuthInfo(AuthorizationConfig AAAconf) {
        return addRemoveAuthInfo(AAAconf, false);
    }

    @Override
    public Status removeAuthInfo(AuthorizationConfig AAAconf) {
        return addRemoveAuthInfo(AAAconf, true);
    }

    @Override
    public List<UserConfig> getLocalUserList() {
        return new ArrayList<UserConfig>(localUserConfigList.values());
    }

    @Override
    public List<ServerConfig> getAAAServerList() {
        return new ArrayList<ServerConfig>(remoteServerConfigList.values());
    }

    @Override
    public List<AuthorizationConfig> getAuthorizationList() {
        return new ArrayList<AuthorizationConfig>(authorizationConfList
                .values());
    }

    @Override
    public Status changeLocalUserPassword(String user, String curPassword,
            String newPassword) {
        UserConfig targetConfigEntry = null;

        // update configuration entry
        targetConfigEntry = localUserConfigList.get(user);
        if (targetConfigEntry == null) {
        	return new Status(StatusCode.NOTFOUND, "User not found");
        }
        if (false == targetConfigEntry.update(curPassword, newPassword, null)) {
        	return new Status(StatusCode.BADREQUEST, "Current password is incorrect");
        }
        localUserConfigList.put(user, targetConfigEntry); // trigger cluster update

        logger.info("Password changed for User \"{}\"", user);

        return new Status(StatusCode.SUCCESS, null);
    }

    @Override
    public void userLogout(String userName) {
        // TODO: if user was authenticated through AAA server, send Acct-Status-Type=stop message to server with logout as reason
        removeUserFromActiveList(userName);
        logger.info("User \"{}\" logged out", userName);
    }

    /*
     * This function will get called by http session mgr when session times out
     */
    @Override
    public void userTimedOut(String userName) {
        // TODO: if user was authenticated through AAA server, send Acct-Status-Type=stop message to server with timeout as reason
        removeUserFromActiveList(userName);
        logger.info("User \"{}\" timed out", userName);
    }

    @Override
    public String getAccessDate(String user) {
        return this.activeUsers.get(user).getAccessDate();
    }

    @Override
    public synchronized Map<String, List<String>> getUserLoggedIn() {
        Map<String, List<String>> loggedInList = new HashMap<String, List<String>>();
        for (Map.Entry<String, AuthenticatedUser> user : activeUsers.entrySet()) {
            String userNameShow = user.getKey();
            loggedInList.put(userNameShow, user.getValue().getUserRoles());
        }
        return loggedInList;
    }

    /*
     * Interaction with GUI END
     */

    /*
     * Cluster notifications
     */

    @Override
    public void entryCreated(Long key, String cacheName, boolean originLocal) {
        // don't react on this event
    }

    @Override
    public void entryUpdated(Long key, String new_value, String cacheName,
            boolean originLocal) {
        if (cacheName.equals("localUserSaveConfigEvent")) {
            this.saveLocalUserListInternal();
        } else if (cacheName.equals("remoteServerSaveConfigEvent")) {
            this.saveAAAServerListInternal();
        } else if (cacheName.equals("authorizationSaveConfigEvent")) {
            this.saveAuthorizationListInternal();
        }
    }

    @Override
    public void entryDeleted(Long key, String cacheName, boolean originLocal) {
        // don't react on this event
    }

    public void _umAddUser(CommandInterpreter ci) {
        String userName = ci.nextArgument();
        String password = ci.nextArgument();
        String role = ci.nextArgument();

        if (userName == null || userName.trim().isEmpty() || password == null
                || password.trim().isEmpty() || role == null
                || role.trim().isEmpty()) {
            ci.println("Invalid Arguments");
            ci.println("umAddUser <user_name> <password> <user_role>");
            return;
        }
        this.addLocalUser(new UserConfig(userName, password, role));
    }

    public void _umRemUser(CommandInterpreter ci) {
        String userName = ci.nextArgument();
        String password = ci.nextArgument();
        String role = ci.nextArgument();

        if (userName == null || userName.trim().isEmpty() || password == null
                || password.trim().isEmpty() || role == null
                || role.trim().isEmpty()) {
            ci.println("Invalid Arguments");
            ci.println("umRemUser <user_name> <password> <user_role>");
            return;
        }
        this.removeLocalUser(new UserConfig(userName, password, role));
    }

    public void _umGetUsers(CommandInterpreter ci) {
        for (UserConfig conf : this.getLocalUserList()) {
            ci.println(conf.getUser() + " " + conf.getRole());
        }
    }
    
    public void _addAAAServer (CommandInterpreter ci) {
        String server = ci.nextArgument();
        String secret = ci.nextArgument();
        String protocol = ci.nextArgument();
        
        if (server == null || secret == null || protocol == null) {
        	ci.println("Usage : addAAAServer <server> <secret> <protocol>");
        	return;
        }
        ServerConfig s = new ServerConfig(server, secret, protocol);
        addAAAServer(s);
    }
    
    public void _removeAAAServer (CommandInterpreter ci) {
        String server = ci.nextArgument();
        String secret = ci.nextArgument();
        String protocol = ci.nextArgument();
        
        if (server == null || secret == null || protocol == null) {
        	ci.println("Usage : addAAAServer <server> <secret> <protocol>");
        	return;
        }
        ServerConfig s = new ServerConfig(server, secret, protocol);
        removeAAAServer(s);
    }

    public void _printAAAServers (CommandInterpreter ci) {
        for (ServerConfig aaaServer : remoteServerConfigList.values()) {
            String protocol = aaaServer.getProtocol();
            ci.println(aaaServer.getAddress() + "-" + aaaServer.getProtocol());
        }
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        return help.toString();
    }

    void setClusterGlobalService(IClusterGlobalServices s) {
        logger.debug("Cluster Service Global set");
        this.clusterGlobalService = s;
    }

    void unsetClusterGlobalService(IClusterGlobalServices s) {
        if (this.clusterGlobalService == s) {
            logger.debug("Cluster Service Global removed!");
            this.clusterGlobalService = null;
        }
    }

    void unsetContainerAuthClient(IContainerAuthorization s) {
        if (this.containerAuthorizationClient == s) {
            this.containerAuthorizationClient = null;
        }
    }

    void setContainerAuthClient(IContainerAuthorization s) {
        this.containerAuthorizationClient = s;
    }

    void setAppAuthClient(IResourceAuthorization s) {
        this.applicationAuthorizationClients.add(s);
    }

    void unsetAppAuthClient(IResourceAuthorization s) {
        this.applicationAuthorizationClients.remove(s);
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        authProviders = new ConcurrentHashMap<String, IAAAProvider>();
        // Instantiate cluster synced variables
        allocateCaches();
        retrieveCaches();

        // Read startup configuration and populate databases
        loadConfigurations();

        // Make sure default Network Admin account is there
        checkDefaultNetworkAdmin();
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
    }

    @Override
    public List<String> getUserRoles(String userName) {
        if (userName == null) {
            return new ArrayList<String>(0);
        }
        AuthenticatedUser locatedUser = activeUsers.get(userName);
        return (locatedUser == null) ? new ArrayList<String>(0) : locatedUser
                .getUserRoles();
    }

    @Override
    public UserLevel getUserLevel(String username) {
        // Returns the controller well-know user level for the passed user
    	String roleName = null;

    	// First check in active users then in local configured users
        if (activeUsers.containsKey(username)) {
        	roleName = activeUsers.get(username).getUserRoles().get(0);
        } else if (localUserConfigList.containsKey(username)) {
        	roleName = localUserConfigList.get(username).getRole();
        }
        
        if (roleName == null) {
        	return UserLevel.NOUSER;
        }
        
        // For now only one role per user is allowed
        if (roleName.equals(UserLevel.SYSTEMADMIN.toString())) {
            return UserLevel.SYSTEMADMIN;
        }
        if (roleName.equals(UserLevel.NETWORKADMIN.toString())) {
            return UserLevel.NETWORKADMIN;
        }
        if (roleName.equals(UserLevel.NETWORKOPERATOR.toString())) {
            return UserLevel.NETWORKOPERATOR;
        }
        if (this.containerAuthorizationClient != null
                && this.containerAuthorizationClient
                        .isApplicationRole(roleName)) {
            return UserLevel.CONTAINERUSER;
        }
        for (IResourceAuthorization client : this.applicationAuthorizationClients) {
            if (client.isApplicationRole(roleName)) {
                return UserLevel.APPUSER;
            }
        }
        return UserLevel.NOUSER;
    }

    @Override
    public Status saveConfiguration() {
        boolean success = true;
        Status ret = saveLocalUserList();
        if (!ret.isSuccess()) {
            success = false;
        }
        ret = saveAAAServerList();
        if (!ret.isSuccess()) {
            success = false;
        }
        ret = saveAuthorizationList();
        if (!ret.isSuccess()) {
            success = false;
        }

        if (success) {
            return new Status(StatusCode.SUCCESS, null);
        }

        return new Status(StatusCode.INTERNALERROR,
        		"Failed to save user configurations");
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        AuthenticatedUser user = activeUsers.get(username);

        if (user != null) {
            boolean enabled = true;
            boolean accountNonExpired = true;
            boolean credentialsNonExpired = true;
            boolean accountNonLocked = true;

            return new User(username, localUserConfigList.get(username)
                    .getPassword(), enabled, accountNonExpired,
                    credentialsNonExpired, accountNonLocked, user
                            .getGrantedAuthorities(getUserLevel(username)));
        } else
            throw new UsernameNotFoundException("User not found " + username);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class
                .isAssignableFrom(authentication);

    }

    @Override
    public SecurityContextRepository getSecurityContextRepo() {
        return securityContextRepo;
    }

    public void setSecurityContextRepo(
            SecurityContextRepository securityContextRepo) {
        this.securityContextRepo = securityContextRepo;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        if (StringUtils.isBlank((String) authentication.getCredentials())
                || StringUtils.isBlank((String) authentication.getPrincipal())) {
            throw new BadCredentialsException(
                    "Username or credentials did not match");
        }

        AuthResultEnum result = authenticate((String) authentication
                .getPrincipal(), (String) authentication.getCredentials());
        if (result.equals(AuthResultEnum.AUTHOR_PASS)
                || result.equals(AuthResultEnum.AUTH_ACCEPT_LOC)
                || result.equals(AuthResultEnum.AUTH_ACCEPT)) {

            AuthenticatedUser user = activeUsers.get(authentication
                    .getPrincipal().toString());

            if (user == null) {
                throw new AuthenticationServiceException(
                        "Authentication Failure");
            }

            authentication = new UsernamePasswordAuthenticationToken(
                    authentication.getPrincipal(), authentication
                            .getCredentials(), user
                            .getGrantedAuthorities(getUserLevel(authentication
                                    .getName())));
            return authentication;

        } else
            throw new BadCredentialsException(
                    "Username or credentials did not match");

    }

    //following are setters for use in unit testing
    void setLocalUserConfigList(ConcurrentMap<String, UserConfig> ucl) {
    	if (ucl != null) { this.localUserConfigList = ucl; }
    }
    void setRemoteServerConfigList (ConcurrentMap<String, ServerConfig> scl) {
    	if (scl != null) { this.remoteServerConfigList = scl; }
    }
    void setAuthorizationConfList (ConcurrentMap<String, AuthorizationConfig> acl) {
    	if (acl != null) { this.authorizationConfList = acl; }
    }
    void setActiveUsers (ConcurrentMap<String, AuthenticatedUser> au) {
        if (au != null) { this.activeUsers = au; }
    }
    void setAuthProviders(ConcurrentMap<String, IAAAProvider> ap ) {
        if (ap != null){ 
            this.authProviders = ap;
        }
    }
    
    @Override
    public ISessionManager getSessionManager() {
        return this.sessionMgr;
    }
    
    public void setSessionMgr(ISessionManager sessionMgr) {
        this.sessionMgr = sessionMgr;
    }
    
    public String getPassword(String username) {
        return localUserConfigList.get(username).getPassword();
    }
}
