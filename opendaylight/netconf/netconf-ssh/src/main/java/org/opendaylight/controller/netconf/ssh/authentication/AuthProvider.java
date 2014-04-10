/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.authentication;

import java.io.IOException;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.usermanager.IUserManager;
import static com.google.common.base.Preconditions.checkNotNull;

public class AuthProvider implements AuthProviderInterface {

    private IUserManager um;
    private final String pem;

    public AuthProvider(IUserManager ium, String pemCertificate) throws IllegalArgumentException, IOException {
        checkNotNull(pemCertificate, "Parameter 'pemCertificate' is null");
        checkNotNull(ium, "No user manager service available.");
        this.um = ium;
        pem = pemCertificate;
    }

    @Override
    public boolean authenticated(String username, String password) {
        AuthResultEnum authResult = this.um.authenticate(username, password);
        return authResult.equals(AuthResultEnum.AUTH_ACCEPT) || authResult.equals(AuthResultEnum.AUTH_ACCEPT_LOC);
    }

    @Override
    public char[] getPEMAsCharArray() {
        return pem.toCharArray();
    }

    @Override
    public void removeUserManagerService() {
        this.um = null;
    }

    @Override
    public void addUserManagerService(IUserManager userManagerService) {
        this.um = userManagerService;
    }
}
