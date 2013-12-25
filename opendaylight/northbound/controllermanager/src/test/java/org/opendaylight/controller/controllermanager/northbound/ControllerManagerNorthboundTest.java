package org.opendaylight.controller.controllermanager.northbound;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Property;

public class ControllerManagerNorthboundTest {

    @Test
    public void testControllerProperties() {
        ControllerProperties controllerProperties = new ControllerProperties(null);
        Assert.assertTrue(controllerProperties.getProperties() == null);

        Set<Property> properties = new HashSet<Property>();
        controllerProperties.setProperties(properties);
        Assert.assertTrue(controllerProperties.getProperties().equals(properties));
    }

}
