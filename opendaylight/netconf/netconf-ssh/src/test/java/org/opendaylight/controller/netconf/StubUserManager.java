/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.usermanager.AuthorizationConfig;
import org.opendaylight.controller.usermanager.ISessionManager;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.ServerConfig;
import org.opendaylight.controller.usermanager.UserConfig;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.SecurityContextRepository;

public class StubUserManager implements IUserManager{


    private static String user;
    private static String password;

    public StubUserManager(String user, String password){
        StubUserManager.user = user;
        StubUserManager.password = password;
    }
    @Override
    public List<String> getUserRoles(String userName) {
        return null;
    }

    @Override
    public AuthResultEnum authenticate(String username, String password) {
        if (StubUserManager.user.equals(username) && StubUserManager.password.equals(password)){
            return AuthResultEnum.AUTH_ACCEPT_LOC;
        }
        return AuthResultEnum.AUTH_REJECT_LOC;
    }

    @Override
    public Status addAAAServer(ServerConfig configObject) {
        return null;
    }

    @Override
    public Status removeAAAServer(ServerConfig configObject) {
        return null;
    }

    @Override
    public Status addLocalUser(UserConfig configObject) {
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status modifyLocalUser(UserConfig configObject) {
        return null;
    }

    @Override
    public Status removeLocalUser(UserConfig configObject) {
        return null;
    }

    @Override
    public Status removeLocalUser(String userName) {
        return null;
    }

    @Override
    public Status addAuthInfo(AuthorizationConfig AAAconf) {
        return null;
    }

    @Override
    public Status removeAuthInfo(AuthorizationConfig AAAconf) {
        return null;
    }

    @Override
    public List<AuthorizationConfig> getAuthorizationList() {
        return null;
    }

    @Override
    public Set<String> getAAAProviderNames() {
        return null;
    }

    @Override
    public Status changeLocalUserPassword(String user, String curPassword, String newPassword) {
        return null;
    }

    @Override
    public List<ServerConfig> getAAAServerList() {
        return null;
    }

    @Override
    public List<UserConfig> getLocalUserList() {
        return null;
    }

    @Override
    public Status saveLocalUserList() {
        return null;
    }

    @Override
    public Status saveAAAServerList() {
        return null;
    }

    @Override
    public Status saveAuthorizationList() {
        return null;
    }

    @Override
    public void userLogout(String username) {

    }

    @Override
    public void userTimedOut(String username) {

    }

    @Override
    public Map<String, List<String>> getUserLoggedIn() {
        return null;
    }

    @Override
    public String getAccessDate(String user) {
        return null;
    }

    @Override
    public UserLevel getUserLevel(String userName) {
        return null;
    }

    @Override
    public List<UserLevel> getUserLevels(String userName) {
        return null;
    }

    @Override
    public SecurityContextRepository getSecurityContextRepo() {
        return null;
    }

    @Override
    public ISessionManager getSessionManager() {
        return null;
    }

    @Override
    public boolean isRoleInUse(String role) {
        return false;
    }

    @Override
    public String getPassword(String username) {
        return null;
    }

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        return null;
    }

}
