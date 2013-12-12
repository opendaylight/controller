/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.authentication;

import ch.ethz.ssh2.signature.RSAPrivateKey;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.IOUtils;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.UserConfig;
import org.opendaylight.controller.usermanager.internal.UserManager;

public class AuthProvider implements AuthProviderInterface {

    private static RSAPrivateKey hostkey = null;
    private static UserManager um;
    private static final String DEAFULT_USER = "netconf";
    private static final String DEAFULT_PASSWORD = "netconf";


    public AuthProvider(IUserManager ium) throws Exception {

        IUserManager userManager = ium;

        if (userManager == null){
            userManager = (IUserManager) ServiceHelper
                    .getGlobalInstance(IUserManager.class, this);
        }

        if (userManager instanceof UserManager) {
            this.um = (UserManager) userManager;
        } else {
            throw new Exception("User manager service can't be resolved.");
        }
        um.setLocalUserConfigList(new ConcurrentHashMap<String, UserConfig>() {
            static final long serialVersionUID = 2L;

            {
                List<String> roles = new ArrayList<String>(1);
                roles.add(UserLevel.SYSTEMADMIN.toString());
                put(DEAFULT_USER, new UserConfig(DEAFULT_USER, DEAFULT_PASSWORD, roles));
            }
        });
    }
    @Override
    public boolean authenticated(String username, String password) {
        AuthResultEnum authResult = this.um.authenticate(username,password);
        if (authResult.equals(AuthResultEnum.AUTH_ACCEPT) || authResult.equals(AuthResultEnum.AUTH_ACCEPT_LOC)){
            return true;
        }
        return false;
    }

    @Override
    public char[] getPEMAsCharArray() {

        InputStream is = getClass().getResourceAsStream("/RSA.pk");
        try {
            return IOUtils.toCharArray(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
