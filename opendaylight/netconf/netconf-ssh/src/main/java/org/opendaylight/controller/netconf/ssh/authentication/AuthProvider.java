/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.authentication;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.UserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthProvider implements AuthProviderInterface {

    private static IUserManager um;
    private static final String DEFAULT_USER = "netconf";
    private static final String DEFAULT_PASSWORD = "netconf";
    private static InputStream privateKeyFileInputStream;

    private static final Logger logger =  LoggerFactory.getLogger(AuthProvider.class);

    public AuthProvider(IUserManager ium,InputStream privateKeyFileInputStream) throws Exception {

        this.um = ium;
        if (this.um  == null){
            throw new Exception("No usermanager service available.");
        }

        this.privateKeyFileInputStream = privateKeyFileInputStream;

        List<String> roles = new ArrayList<String>(1);
        roles.add(UserLevel.SYSTEMADMIN.toString());
        this.um.addLocalUser(new UserConfig(DEFAULT_USER, DEFAULT_PASSWORD, roles));
    }
    @Override
    public boolean authenticated(String username, String password)  throws Exception {
        if (this.um  == null){
            throw new Exception("No usermanager service available.");
        }
        AuthResultEnum authResult = this.um.authenticate(username,password);
        if (authResult.equals(AuthResultEnum.AUTH_ACCEPT) || authResult.equals(AuthResultEnum.AUTH_ACCEPT_LOC)){
            return true;
        }
        return false;
    }

    @Override
    public char[] getPEMAsCharArray() throws Exception {
        char [] PEM  = IOUtils.toCharArray(privateKeyFileInputStream);
        privateKeyFileInputStream.close();
        return PEM;
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
