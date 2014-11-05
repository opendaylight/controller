/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subnets.northbound;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.usermanager.AuthResponse;
import org.opendaylight.controller.usermanager.UserConfig;

public class UserManagerNorthboundTest {

    @Test
    public void testUserConfigs() {
       List<String> roles = new ArrayList<String>();
       roles.add("Network-Admin");

       UserConfig userConfig = new UserConfig("test","testPass",roles);

        Assert.assertNotNull(userConfig);
        Assert.assertNotNull(userConfig.getUser());
        Assert.assertNotNull(userConfig.getPassword());
        Assert.assertTrue(userConfig.getRoles().equals(roles));


        AuthResponse authResponse = userConfig.authenticate("testPass");
        Assert.assertNotNull(authResponse);

        Assert.assertEquals(AuthResultEnum.AUTH_ACCEPT_LOC,authResponse.getStatus());
    }

}
