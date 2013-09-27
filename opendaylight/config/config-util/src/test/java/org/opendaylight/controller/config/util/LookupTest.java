/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConfigRegistry;
import org.opendaylight.controller.config.api.LookupRegistry;
import org.opendaylight.controller.config.api.jmx.ConfigTransactionControllerMXBean;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.util.jolokia.ConfigRegistryJolokiaClient;
import org.opendaylight.controller.config.util.jolokia.ConfigTransactionJolokiaClient;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class LookupTest {

    private String jolokiaURL;
    private TestingConfigRegistry testingRegistry;
    private ObjectName testingRegistryON;
    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    private ConfigRegistryClient jmxRegistryClient, jolokiaRegistryClient;
    private ConfigTransactionControllerMXBean testingTransactionController;
    private ObjectName testingTransactionControllerON;
    private ConfigTransactionClient jmxTransactionClient,
            jolokiaTransactionClient;

    Map<LookupRegistry, ? extends Set<? extends LookupRegistry>> lookupProvidersToClients;

    @Before
    public void setUp() throws Exception {
        jolokiaURL = JolokiaHelper.startTestingJolokia();
        testingRegistry = new TestingConfigRegistry();
        testingRegistryON = ConfigRegistry.OBJECT_NAME;
        mbs.registerMBean(testingRegistry, testingRegistryON);
        jmxRegistryClient = new ConfigRegistryJMXClient(
                ManagementFactory.getPlatformMBeanServer());
        jolokiaRegistryClient = new ConfigRegistryJolokiaClient(jolokiaURL);

        testingTransactionController = new TestingConfigTransactionController();
        testingTransactionControllerON = new ObjectName(
                ObjectNameUtil.ON_DOMAIN + ":" + ObjectNameUtil.TYPE_KEY
                        + "=TransactionController");
        mbs.registerMBean(testingTransactionController,
                testingTransactionControllerON);

        jmxTransactionClient = new ConfigTransactionJMXClient(null,
                testingTransactionControllerON,
                ManagementFactory.getPlatformMBeanServer());
        jolokiaTransactionClient = new ConfigTransactionJolokiaClient(
                jolokiaURL, testingTransactionControllerON, null);
        lookupProvidersToClients = ImmutableMap
                .of(testingRegistry, Sets.newHashSet(jmxRegistryClient, jolokiaRegistryClient),
                        testingTransactionController, Sets.newHashSet(jmxTransactionClient, jolokiaTransactionClient));
    }

    @After
    public void cleanUp() throws Exception {
        JolokiaHelper.stopJolokia();
        mbs.unregisterMBean(testingRegistryON);
        mbs.unregisterMBean(testingTransactionControllerON);
    }

    @Test
    public void testLookupConfigBeans() throws Exception {
        Method method = LookupRegistry.class.getMethod("lookupConfigBeans");
        Object[] args = new Object[0];
        test(method, args);
    }

    @Test
    public void testLookupConfigBeans1() throws Exception {
        Method method = LookupRegistry.class.getMethod("lookupConfigBeans",
                String.class);
        Object[] args = new Object[] { TestingConfigRegistry.moduleName1 };
        test(method, args);
    }

    @Test
    public void testLookupConfigBeans2() throws Exception {
        Method method = LookupRegistry.class.getMethod("lookupConfigBeans",
                String.class, String.class);
        Object[] args = new Object[] { TestingConfigRegistry.moduleName1,
                TestingConfigRegistry.instName1 };
        test(method, args);
    }

    @Test
    public void testLookupConfigBean() throws Exception {
        Method method = LookupRegistry.class.getMethod("lookupConfigBean",
                String.class, String.class);
        Object[] args = new Object[] { TestingConfigRegistry.moduleName1,
                TestingConfigRegistry.instName1 };
        test(method, args);
    }

    private void test(Method method, Object[] args) throws Exception {
        for (Entry<LookupRegistry, ? extends Set<? extends LookupRegistry>> entry : lookupProvidersToClients
                .entrySet()) {
            Object expected = method.invoke(entry.getKey(), args);
            for (LookupRegistry client : entry.getValue()) {
                Object actual = method.invoke(client, args);
                assertEquals("Error while comparing " + entry.getKey()
                        + " with client " + client, expected, actual);
            }
        }
    }

    @Test
    public void testException() {
        for (Entry<LookupRegistry, ? extends Set<? extends LookupRegistry>> entry : lookupProvidersToClients
                .entrySet()) {
            for (LookupRegistry client : entry.getValue()) {
                try {
                    client.lookupConfigBean(
                            InstanceNotFoundException.class.getSimpleName(), "");
                    fail(client.toString());
                } catch (InstanceNotFoundException e) {

                }
            }
        }
    }
}
