package org.opendaylight.controller.config.api.jmx;


import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.constants.ConfigRegistryConstants;

public class ConfigRegistryConstantsTest {

    @Test(expected = IllegalArgumentException.class)
    public void testCreateON() throws Exception {
        ConfigRegistryConstants.createON("test.<:", "asd", "asd");
    }
}
