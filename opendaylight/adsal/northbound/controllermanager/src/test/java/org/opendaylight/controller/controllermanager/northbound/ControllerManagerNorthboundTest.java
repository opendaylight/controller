/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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
