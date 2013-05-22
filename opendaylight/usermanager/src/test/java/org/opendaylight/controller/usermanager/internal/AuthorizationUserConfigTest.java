/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.usermanager.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.usermanager.AuthResponse;

/*
 * This test case includes tests for UserConfig and the extending class AuthorizationConfig
 */
public class AuthorizationUserConfigTest {

    @Test
    public void AuthorizationConfigTest() {
        AuthorizationConfig authConfig;
        List<String> roles = new ArrayList<String>();

        // test isValid
        roles.add(UserLevel.SYSTEMADMIN.toString());
        authConfig = new AuthorizationConfig(null, roles);
        assertFalse(authConfig.validate().isSuccess());
        authConfig = new AuthorizationConfig("admin", new ArrayList<String>());
        assertFalse(authConfig.validate().isSuccess());
        authConfig = new AuthorizationConfig("admin", roles);
        assertTrue(authConfig.validate().isSuccess());
    }

    @Test
    public void UserConfigTest() {
        UserConfig userConfig;
        List<String> roles = new ArrayList<String>();

        roles.add(UserLevel.SYSTEMADMIN.toString());
        userConfig = new UserConfig(null, "cisco", roles);
        assertFalse(userConfig.validate().isSuccess());

        roles.clear();
        roles.add("cisco");
        userConfig = new UserConfig("uname", "", roles);
        assertFalse(userConfig.validate().isSuccess());

        roles.clear();
        roles.add(UserLevel.NETWORKOPERATOR.toString());
        userConfig = new UserConfig("uname", "ciscocisco", roles);
        assertTrue(userConfig.validate().isSuccess());

        // currentPassword mismatch
        assertFalse(userConfig.update("Cisco", "cisco123", roles).isSuccess());

        // Role change only
        roles.clear();
        roles.add(UserLevel.NETWORKADMIN.toString());
        assertTrue(userConfig.update("ciscocisco", null, roles).isSuccess());
        
        // Role change and same new password
        roles.clear();
        roles.add(UserLevel.NETWORKOPERATOR.toString());
        assertTrue(userConfig.update("ciscocisco", "ciscocisco", roles)
                .isSuccess());
        
        // New Password = null, No change in password
        assertTrue(userConfig.getPassword().equals("ciscocisco"));

        // Password changed successfully, no change in user role
        assertTrue(userConfig.update("ciscocisco", "cisco123", roles)
                .isSuccess());
        assertTrue(userConfig.getPassword().equals("cisco123"));
        assertTrue(userConfig.getRoles().get(0).equals(
                UserLevel.NETWORKOPERATOR.toString()));

        // Password not changed, role changed successfully
        roles.clear();
        roles.add(UserLevel.SYSTEMADMIN.toString());
        assertTrue(userConfig.update("cisco123", "cisco123", roles)
                .isSuccess());
        assertTrue(userConfig.getPassword().equals("cisco123"));
        assertTrue(userConfig.getRoles().get(0)
                .equals(UserLevel.SYSTEMADMIN.toString()));

        // Password and role changed successfully
        assertTrue(userConfig.update("cisco123", "ciscocisco", roles)
                .isSuccess());
        assertTrue(userConfig.getPassword().equals("ciscocisco"));
        assertTrue(userConfig.getRoles().get(0)
                .equals(UserLevel.SYSTEMADMIN.toString()));

        String username = userConfig.getUser();
        assertTrue(username.equals("uname"));

        // test authenticate
        AuthResponse authresp = userConfig.authenticate("ciscocisco");
        assertTrue(authresp.getStatus().equals(AuthResultEnum.AUTH_ACCEPT_LOC));
        authresp = userConfig.authenticate("wrongPassword");
        assertTrue(authresp.getStatus().equals(AuthResultEnum.AUTH_REJECT_LOC));

        // test equals()
        roles.clear();
        roles.add(UserLevel.NETWORKOPERATOR.toString());
        userConfig = new UserConfig("uname", "ciscocisco", roles);
        assertEquals(userConfig, userConfig);
        UserConfig userConfig2 = new UserConfig("uname", "ciscocisco", roles);
        assertEquals(userConfig, userConfig2);
    }
}
