/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConfigRegistry;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.util.jolokia.ConfigRegistryJolokiaClient;

public class ConfigRegistryClientsTest {

    private String jolokiaURL;

    private TestingConfigRegistry testingRegistry;
    private ObjectName testingRegistryON;
    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    private ConfigRegistryClient jmxRegistryClient, jolokiaRegistryClient;

    @Before
    public void setUp() throws Exception {
        jolokiaURL = JolokiaHelper.startTestingJolokia();
        testingRegistry = new TestingConfigRegistry();
        testingRegistryON = ConfigRegistry.OBJECT_NAME;
        mbs.registerMBean(testingRegistry, testingRegistryON);
        jmxRegistryClient = new ConfigRegistryJMXClient(
                ManagementFactory.getPlatformMBeanServer());
        jolokiaRegistryClient = new ConfigRegistryJolokiaClient(jolokiaURL);
    }

    @After
    public void cleanUp() throws Exception {
        JolokiaHelper.stopJolokia();
        if (testingRegistryON != null) {
            mbs.unregisterMBean(testingRegistryON);
        }
    }

    @Test
    public void testLookupRuntimeBeans() throws Exception {
        Set<ObjectName> jmxLookup = lookupRuntimeBeans(jmxRegistryClient);
        Set<ObjectName> jolokiaLookup = lookupRuntimeBeans(jolokiaRegistryClient);
        assertEquals(jmxLookup, jolokiaLookup);
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
        Set<ObjectName> jolokiaLookup = clientLookupRuntimeBeansWithModuleAndInstance(
                jolokiaRegistryClient, TestingConfigRegistry.moduleName1,
                TestingConfigRegistry.instName1);
        assertEquals(jmxLookup, jolokiaLookup);

        jmxLookup = clientLookupRuntimeBeansWithModuleAndInstance(
                jmxRegistryClient, TestingConfigRegistry.moduleName2,
                TestingConfigRegistry.instName2);
        assertEquals(1, jmxLookup.size());
        jolokiaLookup = clientLookupRuntimeBeansWithModuleAndInstance(
                jolokiaRegistryClient, TestingConfigRegistry.moduleName2,
                TestingConfigRegistry.instName2);
        assertEquals(jmxLookup, jolokiaLookup);

        jmxLookup = clientLookupRuntimeBeansWithModuleAndInstance(
                jmxRegistryClient, TestingConfigRegistry.moduleName1,
                TestingConfigRegistry.instName2);
        assertEquals(0, jmxLookup.size());
        jolokiaLookup = clientLookupRuntimeBeansWithModuleAndInstance(
                jolokiaRegistryClient, TestingConfigRegistry.moduleName1,
                TestingConfigRegistry.instName2);
        assertEquals(jmxLookup, jolokiaLookup);
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

    @Test
    public void testValidationExceptionDeserialization() {
        try {
            jolokiaRegistryClient.commitConfig(null);
            fail();
        } catch (ValidationException e) {
            String moduleName = "moduleName", instanceName = "instanceName";
            assertThat(e.getFailedValidations().containsKey(moduleName),
                    is(true));
            assertThat(e.getFailedValidations().size(), is(1));
            assertThat(e.getFailedValidations().get(moduleName).size(), is(1));
            assertThat(
                    e.getFailedValidations().get(moduleName)
                            .containsKey(instanceName), is(true));
            assertThat(
                    e.getFailedValidations().get(moduleName).get(instanceName)
                            .getMessage(), is("message"));
            assertThat(
                    e.getFailedValidations().get(moduleName).get(instanceName)
                            .getTrace(),
                    containsString("org.opendaylight.controller.config.util.TestingConfigRegistry.commitConfig"));
        }
    }

}
