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
