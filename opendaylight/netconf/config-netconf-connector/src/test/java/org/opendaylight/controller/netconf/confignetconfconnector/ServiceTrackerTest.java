/*
* Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package org.opendaylight.controller.netconf.confignetconfconnector;

import org.junit.Test;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.Services;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.Services.ServiceInstance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServiceTrackerTest {

    @Test
    public void test() {
        Services.ServiceInstance serviceInstance = new ServiceInstance("module", "serviceInstance");

        String string = serviceInstance.toString();

        Services.ServiceInstance serviceInstance2 = Services.ServiceInstance.fromString(string);

        assertEquals(serviceInstance, serviceInstance2);
    }

    @Test
    public void testOneInstanceMultipleServices() {
        Services services = new Services();
        services.addServiceEntry("nm", "s1", "module", "instance");
        assertEquals(1, services.getMappedServices().size());

        services.addServiceEntry("nm2", "s2", "module", "instance");
        assertEquals(2, services.getMappedServices().size());
    }

    @Test
    public void testMultipleInstancesOneName() throws Exception {
        Services services = new Services();
        services.addServiceEntry("nm", "s1", "module", "instance");
        assertEquals(1, services.getMappedServices().size());

        services.addServiceEntry("nm", "s1", "module2", "instance");
        assertEquals(1, services.getMappedServices().size());
        assertEquals(2, services.getMappedServices().get("nm").get("s1").size());
        assertTrue(services.getMappedServices().get("nm").get("s1").containsKey("ref_instance"));
        assertTrue(services.getMappedServices().get("nm").get("s1").containsKey("ref_instance_1"));
    }

    @Test
    public void testMultipleInstancesOneName2() throws Exception {
        Services services = new Services();
        services.addServiceEntry("nm", "s1", "module", "instance_1");

        services.addServiceEntry("nm2", "s2", "module2", "instance");
        services.addServiceEntry("nm2", "s2", "module3", "instance");
        services.addServiceEntry("nm", "s1", "module3", "instance");

        assertEquals(2, services.getMappedServices().get("nm").get("s1").size());
        assertEquals(2, services.getMappedServices().get("nm2").get("s2").size());
        assertTrue(services.getMappedServices().get("nm").get("s1").containsKey("ref_instance_2"));
        assertTrue(services.getMappedServices().get("nm").get("s1").containsKey("ref_instance_1"));
        assertTrue(services.getMappedServices().get("nm2").get("s2").containsKey("ref_instance"));
        assertTrue(services.getMappedServices().get("nm2").get("s2").containsKey("ref_instance_2"));
    }

}
