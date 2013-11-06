/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
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
import org.opendaylight.controller.usermanager.AuthenticatedUser;
import org.opendaylight.controller.usermanager.AuthorizationConfig;
import org.opendaylight.controller.usermanager.IAAAProvider;
import org.opendaylight.controller.usermanager.ISessionManager;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.ServerConfig;
import org.opendaylight.controller.usermanager.UserConfig;
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
public class UserManager implements IUserManager, IObjectReader,
        IConfigurationAware, CommandProvider, AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);
    private static final String DEFAULT_ADMIN = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    private static final String DEFAULT_ADMIN_ROLE = UserLevel.NETWORKADMIN.toString();
    private static final String ROOT = GlobalConstants.STARTUPHOME.toString();
    private static final String USERS_FILE_NAME = ROOT + "users.conf";
    private static final String SERVERS_FILE_NAME = ROOT + "servers.conf";
    private static final String AUTH_FILE_NAME = ROOT + "authorization.conf";
    private static final String RECOVERY_FILE = ROOT + "NETWORK_ADMIN_PASSWORD_RECOVERY";
    private ConcurrentMap<String, UserConfig> localUserConfigList;
    private ConcurrentMap<String, ServerConfig> remoteServerConfigList;
    // local authorization info for remotely authenticated users
    private ConcurrentMap<String, AuthorizationConfig> authorizationConfList;
    private ConcurrentMap<String, AuthenticatedUser> activeUsers;
    private ConcurrentMap<String, IAAAProvider> authProviders;
    private IClusterGlobalServices clusterGlobalService = null;
    private SecurityContextRepository securityContextRepo = new UserSecurityContextRepository();
    private IContainerAuthorization containerAuthorizationClient;
    private Set<IResourceAuthorization> applicationAuthorizationClients;
    private ISessionManager sessionMgr = new SessionManager();
    protected enum Command {
        ADD("add", "added"),
        MODIFY("modify", "modified"),
        REMOVE("remove", "removed");
        private String action;
        private String postAction;
        private Command(String action, String postAction) {
            this.action = action;
            this.postAction = postAction;
        }

        public String getAction() {
            return action;
        }

        public String getPostAction() {
            return postAction;
        }
    }

    public boolean addAAAProvider(IAAAProvider provider) {
        if (provider == null || provider.getName() == null
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

    @Override
    public Set<String> getAAAProviderNames() {
        return authProviders.keySet();
    }

    private void allocateCaches() {
        this.applicationAuthorizationClients = Collections.synchronizedSet(new HashSet<IResourceAuthorization>());
        if (clusterGlobalService == null) {
            logger.error("un-initialized clusterGlobalService, can't create cache");
            return;
        }

        try {
            clusterGlobalService.createCache("usermanager.localUserConfigList",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterGlobalService.createCache(
                    "usermanager.remoteServerConfigList",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterGlobalService.createCache(
                    "usermanager.authorizationConfList",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterGlobalService.createCache("usermanager.activeUsers",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger.error("Cache configuration invalid - check cache mode");
        } catch (CacheExistException ce) {
            logger.debug("Skipping cache creation as already present");
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void retrieveCaches() {
        if (clusterGlobalService == null) {
            logger.error("un-initialized clusterService, can't retrieve cache");
            return;
        }

        activeUsers = (ConcurrentMap<String, AuthenticatedUser>) clusterGlobalService
                .getCache("usermanager.activeUsers");
        if (activeUsers == null) {
            logger.error("Failed to get cache for activeUsers");
        }

        localUserConfigList = (ConcurrentMap<String, UserConfig>) clusterGlobalService
                .getCache("usermanager.localUserConfigList");
        if (localUserConfigList == null) {
            logger.error("Failed to get cache for localUserConfigList");
        }

        remoteServerConfigList = (ConcurrentMap<String, ServerConfig>) clusterGlobalService
                .getCache("usermanager.remoteServerConfigList");
        if (remoteServerConfigList == null) {
            logger.error("Failed to get cache for remoteServerConfigList");
        }

        authorizationConfList = (ConcurrentMap<String, AuthorizationConfig>) clusterGlobalService
                .getCache("usermanager.authorizationConfList");
        if (authorizationConfList == null) {
            logger.error("Failed to get cache for authorizationConfList");
        }
    }

    private void loadConfigurations() {
        // To encode and decode user and server configuration objects
        loadSecurityKeys();

        /*
         * Do not load local startup file if we already got the configurations
         * synced from another cluster node
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
        /*
         * If startup config is not there, it's old or it was deleted or if a
         * password recovery was run, need to add Default Network Admin User
         */
        if (!localUserConfigList.containsKey(DEFAULT_ADMIN)) {
            List<String> roles = new ArrayList<String>(1);
            roles.add(DEFAULT_ADMIN_ROLE);
            // Need to skip the strong password check for the default admin
            UserConfig defaultAdmin = UserConfig.getUncheckedUserConfig(UserManager.DEFAULT_ADMIN,
                    UserManager.DEFAULT_ADMIN_PASSWORD, roles);
            localUserConfigList.put(UserManager.DEFAULT_ADMIN, defaultAdmin);
        }
    }

    private void checkPasswordRecovery() {
        final String fileDescription = "Default Network Administrator password recovery file";
        try {
            FileInputStream fis = new FileInputStream(UserManager.RECOVERY_FILE);
            /*
             * Recovery file detected, remove current default network
             * administrator entry from local users configuration list.
             * Warn user and delete recovery file.
             */
            this.localUserConfigList.remove(UserManager.DEFAULT_ADMIN);
            logger.info("Default Network Administrator password has been reset to factory default.");
            logger.info("Please change the default Network Administrator password as soon as possible");
            File filePointer = new File(UserManager.RECOVERY_FILE);
            boolean status = filePointer.delete();
            if (!status) {
                logger.warn("Failed to delete {}", fileDescription);
            } else {
                logger.trace("{} deleted", fileDescription);
            }
            fis.close();
        } catch (FileNotFoundException fnf) {
            logger.trace("{} not present", fileDescription);
        } catch (IOException e) {
            logger.warn("Failed to close file stream for {}", fileDescription);
        }
    }

    @Override
    public AuthResultEnum authenticate(String userName, String password) {
        IAAAProvider aaaClient;
        AuthResponse rcResponse = null;
        AuthenticatedUser result;
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
                    logger.info(
                            "Remote Authentication Succeeded for User: \"{}\", by Server: {}",
                            userName, aaaServer.getAddress());
                    remotelyAuthenticated = true;
                    break;
                } else if (rcResponse.getStatus() == AuthResultEnum.AUTH_REJECT) {
                    logger.info(
                            "Remote Authentication Rejected User: \"{}\", from Server: {}, Reason:{}",
                            new Object[] { userName, aaaServer.getAddress(),
                                    rcResponse.getStatus().toString() });
                } else {
                    logger.info(
                            "Remote Authentication Failed for User: \"{}\", from Server: {}, Reason:{}",
                            new Object[] { userName, aaaServer.getAddress(),
                                    rcResponse.getStatus().toString() });
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
                logger.info(
                        "Local Authentication Failed for User: \"{}\", Reason: {}",
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
         * Extract attributes from response All the information we are
         * interested in is in the first Cisco VSA (vendor specific attribute).
         * Just process the first VSA and return
         */
        String attributes = (rcResponse.getData() != null && !rcResponse
                .getData().isEmpty()) ? rcResponse.getData().get(0) : null;

        /*
         * Check if the authorization information is present
         */
        authorizationInfoIsPresent = checkAuthorizationInfo(attributes);

        /*
         * The AAA server was only used to perform the authentication Look for
         * locally stored authorization info for this user If found, add the
         * data to the rcResponse
         */
        if (remotelyAuthenticated && !authorizationInfoIsPresent) {
            logger.info(
                    "No Remote Authorization Info provided by Server for User: \"{}\"",
                    userName);
            logger.info(
                    "Looking for Local Authorization Info for User: \"{}\"",
                    userName);

            AuthorizationConfig resource = authorizationConfList.get(userName);
            if (resource != null) {
                logger.info("Found Local Authorization Info for User: \"{}\"",
                        userName);
                attributes = resource.getRolesString();

            }
            authorizationInfoIsPresent = checkAuthorizationInfo(attributes);
        }

        /*
         * Common response parsing for local & remote authenticated user Looking
         * for authorized resources, detecting attributes' validity
         */
        if (authorizationInfoIsPresent) {
            // Identifying the administrative role
            result.setRoleList(attributes.split(" "));
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
            logger.info("User \"{}\" authorized for the following role(s): {}",
                    userName, result.getUserRoles());
        } else {
            logger.info("User \"{}\" Not Authorized for any role ", userName);
        }

        return rcResponse.getStatus();
    }

    // Check in the attributes string whether or not authorization information
    // is present
    private boolean checkAuthorizationInfo(String attributes) {
        return (attributes != null && !attributes.isEmpty());
    }

    private void putUserInActiveList(String user, AuthenticatedUser result) {
        activeUsers.put(user, result);
    }

    private void removeUserFromActiveList(String user) {
        if (!activeUsers.containsKey(user)) {
            // as cookie persists in cache, we can get logout for unexisting
            // active users
            return;
        }
        activeUsers.remove(user);
    }

    @Override
    public Status saveLocalUserList() {
        return saveLocalUserListInternal();
    }

    private Status saveLocalUserListInternal() {
        ObjectWriter objWriter = new ObjectWriter();
        return objWriter.write(new ConcurrentHashMap<String, UserConfig>(
                localUserConfigList), USERS_FILE_NAME);
    }

    @Override
    public Status saveAAAServerList() {
        return saveAAAServerListInternal();
    }

    private Status saveAAAServerListInternal() {
        ObjectWriter objWriter = new ObjectWriter();
        return objWriter.write(new ConcurrentHashMap<String, ServerConfig>(
                remoteServerConfigList), SERVERS_FILE_NAME);
    }

    @Override
    public Status saveAuthorizationList() {
        return saveAuthorizationListInternal();
    }

    private Status saveAuthorizationListInternal() {
        ObjectWriter objWriter = new ObjectWriter();
        return objWriter.write(
                new ConcurrentHashMap<String, AuthorizationConfig>(
                        authorizationConfList), AUTH_FILE_NAME);
    }

    @Override
    public Object readObject(ObjectInputStream ois)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        // Perform the class deserialization locally, from inside the package
        // where the class is defined
        return ois.readObject();
    }

    @SuppressWarnings("unchecked")
    private void loadUserConfig() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<String, UserConfig> confList = (ConcurrentMap<String, UserConfig>) objReader
                .read(this, USERS_FILE_NAME);

        if (confList == null) {
            return;
        }

        for (UserConfig conf : confList.values()) {
            addRemoveLocalUserInternal(conf, false);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadServerConfig() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<String, ServerConfig> confList = (ConcurrentMap<String, ServerConfig>) objReader
                .read(this, SERVERS_FILE_NAME);

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
                .read(this, AUTH_FILE_NAME);

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
    private Status changeLocalUser(UserConfig AAAconf, Command command) {
        // UserConfig Validation check
        Status validCheck = AAAconf.validate();
        if (!validCheck.isSuccess()) {
            return validCheck;
        }

        String user = AAAconf.getUser();

        // Check default admin user
        if (user.equals(UserManager.DEFAULT_ADMIN)) {
            String msg = String.format("Invalid Request: Default Network Admin  User cannot be %s", command.getPostAction());
            logger.debug(msg);
            return new Status(StatusCode.NOTALLOWED, msg);
        }

        // Check user presence/conflict
        UserConfig currentAAAconf = localUserConfigList.get(user);
        StatusCode statusCode = null;
        String reason = null;
        switch (command) {
        case ADD:
            if (currentAAAconf != null) {
                reason = "already present";
                statusCode = StatusCode.CONFLICT;
            }
            break;
        case MODIFY:
        case REMOVE:
            if (currentAAAconf == null) {
                reason = "not found";
                statusCode = StatusCode.NOTFOUND;
            }
            break;
        default:
            break;

        }
        if (statusCode != null) {
            String action = String.format("Failed to %s user %s: ", command.getAction(), user);
            String msg = String.format("User %s %s in configuration database", user, reason);
            logger.debug(action + msg);
            return new Status(statusCode, msg);
        }

        switch (command) {
        case ADD:
            return addRemoveLocalUserInternal(AAAconf, false);
        case MODIFY:
            addRemoveLocalUserInternal(currentAAAconf, true);
            return addRemoveLocalUserInternal(AAAconf, false);
        case REMOVE:
            return addRemoveLocalUserInternal(AAAconf, true);
        default:
            return new Status(StatusCode.INTERNALERROR, "Unknown action");
        }
    }

    private Status addRemoveLocalUserInternal(UserConfig AAAconf, boolean delete) {
        // Update Config database
        if (delete) {
            localUserConfigList.remove(AAAconf.getUser());
            /*
             * A user account has been removed form local database, we assume
             * admin does not want this user to stay connected, in case he has
             * an open session. So we clean the active list as well.
             */
            removeUserFromActiveList(AAAconf.getUser());
        } else {
            localUserConfigList.put(AAAconf.getUser(), AAAconf);
        }

        return new Status(StatusCode.SUCCESS);
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

        return new Status(StatusCode.SUCCESS);
    }

    private Status addRemoveAuthInfo(AuthorizationConfig AAAconf, boolean delete) {
        Status configCheck = AAAconf.validate();
        if (!configCheck.isSuccess()) {
            String msg = "Invalid Authorization configuration: "
                    + configCheck.getDescription();
            logger.warn(msg);
            return new Status(StatusCode.BADREQUEST, msg);
        }

        // Update configuration database
        if (delete) {
            authorizationConfList.remove(AAAconf.getUser());
        } else {
            authorizationConfList.put(AAAconf.getUser(), AAAconf);
        }

        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status addLocalUser(UserConfig AAAconf) {
        return changeLocalUser(AAAconf, Command.ADD);
    }

    @Override
    public Status modifyLocalUser(UserConfig AAAconf) {
        return changeLocalUser(AAAconf, Command.MODIFY);
    }

    @Override
    public Status removeLocalUser(UserConfig AAAconf) {
        return changeLocalUser(AAAconf, Command.REMOVE);
    }

    @Override
    public Status removeLocalUser(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Invalid user name");
        }

        if (!localUserConfigList.containsKey(userName)) {
            return new Status(StatusCode.NOTFOUND, "User does not exist");
        }

        return changeLocalUser(localUserConfigList.get(userName), Command.REMOVE);
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
        return new ArrayList<AuthorizationConfig>(
                authorizationConfList.values());
    }

    @Override
    public Status changeLocalUserPassword(String user, String curPassword, String newPassword) {
        UserConfig targetConfigEntry = null;

        // update configuration entry
        targetConfigEntry = localUserConfigList.get(user);
        if (targetConfigEntry == null) {
            return new Status(StatusCode.NOTFOUND, "User not found");
        }
        Status status = targetConfigEntry.update(curPassword, newPassword, null);
        if (!status.isSuccess()) {
            return status;
        }
        // Trigger cluster update
        localUserConfigList.put(user, targetConfigEntry);

        logger.info("Password changed for User \"{}\"", user);

        return status;
    }

    @Override
    public void userLogout(String userName) {
        // TODO: if user was authenticated through AAA server, send
        // Acct-Status-Type=stop message to server with logout as reason
        removeUserFromActiveList(userName);
        logger.info("User \"{}\" logged out", userName);
    }

    /*
     * This function will get called by http session mgr when session times out
     */
    @Override
    public void userTimedOut(String userName) {
        // TODO: if user was authenticated through AAA server, send
        // Acct-Status-Type=stop message to server with timeout as reason
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

    public void _umAddUser(CommandInterpreter ci) {
        String userName = ci.nextArgument();
        String password = ci.nextArgument();
        String role = ci.nextArgument();

        List<String> roles = new ArrayList<String>();
        while (role != null) {
            if (!role.trim().isEmpty()) {
                roles.add(role);
            }
            role = ci.nextArgument();
        }

        if (userName == null || userName.trim().isEmpty() || password == null || password.trim().isEmpty()
                || roles.isEmpty()) {
            ci.println("Invalid Arguments");
            ci.println("umAddUser <user_name> <password> <user_role>");
            return;
        }
        ci.print(this.addLocalUser(new UserConfig(userName, password, roles)));
    }

    public void _umRemUser(CommandInterpreter ci) {
        String userName = ci.nextArgument();

        if (userName == null || userName.trim().isEmpty()) {
            ci.println("Invalid Arguments");
            ci.println("umRemUser <user_name>");
            return;
        }
        UserConfig target = localUserConfigList.get(userName);
        if (target == null) {
            ci.println("User not found");
            return;
        }
        ci.println(this.removeLocalUser(target));
    }

    public void _umGetUsers(CommandInterpreter ci) {
        for (UserConfig conf : this.getLocalUserList()) {
            ci.println(conf.getUser() + " " + conf.getRoles());
        }
    }

    public void _addAAAServer(CommandInterpreter ci) {
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

    public void _removeAAAServer(CommandInterpreter ci) {
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

    public void _printAAAServers(CommandInterpreter ci) {
        for (ServerConfig aaaServer : remoteServerConfigList.values()) {
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
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
        authProviders = new ConcurrentHashMap<String, IAAAProvider>();
        // Instantiate cluster synced variables
        allocateCaches();
        retrieveCaches();

        // Read startup configuration and populate databases
        loadConfigurations();

        // Check if a password recovery was triggered for default network admin user
        checkPasswordRecovery();

        // Make sure default Network Admin account is there
        checkDefaultNetworkAdmin();

        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this, null);
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
    }

    @Override
    public List<String> getUserRoles(String userName) {
        List<String> roles = null;
        if (userName != null) {
            /*
             * First look in active users then in local configured users,
             * finally in local authorized users
             */
            if (activeUsers.containsKey(userName)) {
                roles = activeUsers.get(userName).getUserRoles();
            } else if (localUserConfigList.containsKey(userName)) {
                roles = localUserConfigList.get(userName).getRoles();
            } else if (authorizationConfList.containsKey(userName)) {
                roles = authorizationConfList.get(userName).getRoles();
            }
        }
        return (roles == null) ? new ArrayList<String>(0) : roles;
    }

    @Override
    public UserLevel getUserLevel(String username) {
        // Returns the highest controller user level for the passed user
        List<String> rolesNames = getUserRoles(username);

        if (rolesNames.isEmpty()) {
            return UserLevel.NOUSER;
        }

        // Check against the well known controller roles first
        if (rolesNames.contains(UserLevel.SYSTEMADMIN.toString())) {
            return UserLevel.SYSTEMADMIN;
        }
        if (rolesNames.contains(UserLevel.NETWORKADMIN.toString())) {
            return UserLevel.NETWORKADMIN;
        }
        if (rolesNames.contains(UserLevel.NETWORKOPERATOR.toString())) {
            return UserLevel.NETWORKOPERATOR;
        }
        // Check if container user now
        if (containerAuthorizationClient != null) {
            for (String roleName : rolesNames) {
                if (containerAuthorizationClient.isApplicationRole(roleName)) {
                    return UserLevel.CONTAINERUSER;
                }
            }
        }
        // Finally check if application user
        if (applicationAuthorizationClients != null) {
            for (String roleName : rolesNames) {
                for (IResourceAuthorization client : this.applicationAuthorizationClients) {
                    if (client.isApplicationRole(roleName)) {
                        return UserLevel.APPUSER;
                    }
                }
            }
        }
        return UserLevel.NOUSER;
    }


    @Override
    public List<UserLevel> getUserLevels(String username) {
        // Returns the controller user levels for the passed user
        List<String> rolesNames =  getUserRoles(username);
        List<UserLevel> levels = new ArrayList<UserLevel>();

        if (rolesNames.isEmpty()) {
            return levels;
        }

        // Check against the well known controller roles first
        if (rolesNames.contains(UserLevel.SYSTEMADMIN.toString())) {
            levels.add(UserLevel.SYSTEMADMIN);
        }
        if (rolesNames.contains(UserLevel.NETWORKADMIN.toString())) {
            levels.add(UserLevel.NETWORKADMIN);
        }
        if (rolesNames.contains(UserLevel.NETWORKOPERATOR.toString())) {
            levels.add(UserLevel.NETWORKOPERATOR);
        }
        // Check if container user now
        if (containerAuthorizationClient != null) {
            for (String roleName : rolesNames) {
                if (containerAuthorizationClient.isApplicationRole(roleName)) {
                    levels.add(UserLevel.CONTAINERUSER);
                    break;
                }
            }
        }
        // Finally check if application user
        if (applicationAuthorizationClients != null) {
            for (String roleName : rolesNames) {
                for (IResourceAuthorization client : this.applicationAuthorizationClients) {
                    if (client.isApplicationRole(roleName)) {
                        levels.add(UserLevel.APPUSER);
                        break;
                    }
                }
            }
        }
        return levels;
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
            return new Status(StatusCode.SUCCESS);
        }

        return new Status(StatusCode.INTERNALERROR, "Failed to save user configurations");
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
                    credentialsNonExpired, accountNonLocked,
                    user.getGrantedAuthorities(getUserLevel(username)));
        } else {
            throw new UsernameNotFoundException("User not found " + username);
        }
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

        AuthResultEnum result = authenticate(
                (String) authentication.getPrincipal(),
                (String) authentication.getCredentials());
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
                    authentication.getPrincipal(),
                    authentication.getCredentials(),
                    user.getGrantedAuthorities(getUserLevel(authentication
                            .getName())));
            return authentication;

        } else {
            throw new BadCredentialsException(
                    "Username or credentials did not match");
        }

    }

    // Following are setters for use in unit testing
    void setLocalUserConfigList(ConcurrentMap<String, UserConfig> ucl) {
        if (ucl != null) {
            this.localUserConfigList = ucl;
        }
    }

    void setRemoteServerConfigList(ConcurrentMap<String, ServerConfig> scl) {
        if (scl != null) {
            this.remoteServerConfigList = scl;
        }
    }

    void setAuthorizationConfList(ConcurrentMap<String, AuthorizationConfig> acl) {
        if (acl != null) {
            this.authorizationConfList = acl;
        }
    }

    void setActiveUsers(ConcurrentMap<String, AuthenticatedUser> au) {
        if (au != null) {
            this.activeUsers = au;
        }
    }

    void setAuthProviders(ConcurrentMap<String, IAAAProvider> ap) {
        if (ap != null) {
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

    @Override
    public String getPassword(String username) {
        return localUserConfigList.get(username).getPassword();
    }

    @Override
    public boolean isRoleInUse(String role) {
        if (role == null || role.isEmpty()) {
            return false;
        }
        // Check against controller roles
        if (role.equals(UserLevel.SYSTEMADMIN.toString())
                || role.equals(UserLevel.NETWORKADMIN.toString())
                || role.equals(UserLevel.NETWORKOPERATOR.toString())) {
            return true;
        }
        // Check if container roles
        if (containerAuthorizationClient != null) {
            if (containerAuthorizationClient.isApplicationRole(role)) {
                return true;
            }
        }
        // Finally if application role
        if (applicationAuthorizationClients != null) {
            for (IResourceAuthorization client : this.applicationAuthorizationClients) {
                if (client.isApplicationRole(role)) {
                    return true;
                }
            }
        }
        return false;
    }
}
