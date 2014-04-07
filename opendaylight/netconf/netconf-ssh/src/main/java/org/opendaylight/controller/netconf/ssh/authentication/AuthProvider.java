/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.authentication;

import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.UserConfig;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class AuthProvider implements AuthProviderInterface {

    private static IUserManager um; //FIXME static mutable state, no locks
    private static final String DEFAULT_USER = "netconf";
    private static final String DEFAULT_PASSWORD = "netconf";
    private final String pem;

    public AuthProvider(IUserManager ium, String pemCertificate) throws Exception {
        checkNotNull(pemCertificate, "Parameter 'pemCertificate' is null");
        AuthProvider.um = ium;
        if (AuthProvider.um == null) {
            throw new Exception("No usermanager service available.");
        }

        List<String> roles = new ArrayList<String>(1);
        roles.add(UserLevel.SYSTEMADMIN.toString());
        AuthProvider.um.addLocalUser(new UserConfig(DEFAULT_USER, DEFAULT_PASSWORD, roles)); //FIXME hardcoded auth
        pem = pemCertificate;
    }

    @Override
    public boolean authenticated(String username, String password) throws Exception {
        if (AuthProvider.um == null) {
            throw new Exception("No usermanager service available.");
        }
        AuthResultEnum authResult = AuthProvider.um.authenticate(username, password);
        return authResult.equals(AuthResultEnum.AUTH_ACCEPT) || authResult.equals(AuthResultEnum.AUTH_ACCEPT_LOC);
    }

    @Override
    public char[] getPEMAsCharArray() {
        return pem.toCharArray();
    }

    @Override
    public void removeUserManagerService() {
        AuthProvider.um = null;
    }

    @Override
    public void addUserManagerService(IUserManager userManagerService) {
        AuthProvider.um = userManagerService;
    }
}
