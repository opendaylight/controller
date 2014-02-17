/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConfigRegistry;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ConfigRegistryClientsTest {

    private TestingConfigRegistry testingRegistry;
    private ObjectName testingRegistryON;
    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    private ConfigRegistryClient jmxRegistryClient;

    @Before
    public void setUp() throws Exception {
        testingRegistry = new TestingConfigRegistry();
        testingRegistryON = ConfigRegistry.OBJECT_NAME;
        mbs.registerMBean(testingRegistry, testingRegistryON);
        jmxRegistryClient = new ConfigRegistryJMXClient(
                ManagementFactory.getPlatformMBeanServer());
    }

    @After
    public void cleanUp() throws Exception {
        if (testingRegistryON != null) {
            mbs.unregisterMBean(testingRegistryON);
        }
    }

    @Test
    public void testLookupRuntimeBeans() throws Exception {
        Set<ObjectName> jmxLookup = lookupRuntimeBeans(jmxRegistryClient);
        assertEquals(Sets.newHashSet(TestingConfigRegistry.run2, TestingConfigRegistry.run1, TestingConfigRegistry.run3), jmxLookup);
    }

    private Set<ObjectName> lookupRuntimeBeans(ConfigRegistryClient client)
            throws Exception {
        Set<ObjectName> beans = client.lookupRuntimeBeans();
        for (ObjectName on : beans) {
            assertEquals("RuntimeBean", on.getKeyProperty("type"));
        }
        assertEquals(3, beans.size());
        return beans;
    }

    @Test
    public void testLookupRuntimeBeansWithIfcNameAndInstanceName()
            throws InstanceNotFoundException {
        Set<ObjectName> jmxLookup = clientLookupRuntimeBeansWithModuleAndInstance(
                jmxRegistryClient, TestingConfigRegistry.moduleName1,
                TestingConfigRegistry.instName1);
        assertEquals(1, jmxLookup.size());
        assertEquals(Sets.newHashSet(TestingConfigRegistry.run2), jmxLookup);

        jmxLookup = clientLookupRuntimeBeansWithModuleAndInstance(
                jmxRegistryClient, TestingConfigRegistry.moduleName2,
                TestingConfigRegistry.instName2);
        assertEquals(1, jmxLookup.size());
        assertEquals(Sets.newHashSet(TestingConfigRegistry.run3), jmxLookup);

        jmxLookup = clientLookupRuntimeBeansWithModuleAndInstance(
                jmxRegistryClient, TestingConfigRegistry.moduleName1,
                TestingConfigRegistry.instName2);
        assertEquals(0, jmxLookup.size());
        assertEquals(Sets.newHashSet(), jmxLookup);
    }

    private Set<ObjectName> clientLookupRuntimeBeansWithModuleAndInstance(
            ConfigRegistryClient client, String moduleName, String instanceName) {
        Set<ObjectName> beans = client.lookupRuntimeBeans(moduleName, instanceName);
        if (beans.size() > 0) {
            assertEquals("RuntimeBean",
                    beans.iterator().next().getKeyProperty("type"));
        }
        return beans;
    }
}
