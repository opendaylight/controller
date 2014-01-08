/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.authentication;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.UserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthProvider implements AuthProviderInterface {

    private static String hostkey = null;
    private static IUserManager um;
    private static final String DEFAULT_USER = "netconf";
    private static final String DEFAULT_PASSWORD = "netconf";

    private static final Logger logger =  LoggerFactory.getLogger(AuthProvider.class);

    public AuthProvider(IUserManager ium,String privateKey) throws Exception {

        this.um = ium;
        this.hostkey = privateKey;
        if (this.um  == null){
            throw new Exception("No usermanager service available.");
        }

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
    public char[] getPEMAsCharArray() {
        if (hostkey!=null){
            char[] PEM = hostkey.toCharArray();
            logger.info("char array size :"+PEM.length);
            return PEM;
        }
/*
        InputStream is = getClass().getResourceAsStream("/RSA.pk");
        try {
            return IOUtils.toCharArray(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
        return null;
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
