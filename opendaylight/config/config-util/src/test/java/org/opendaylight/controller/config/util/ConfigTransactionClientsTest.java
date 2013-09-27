/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import static org.junit.Assert.assertEquals;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.ConfigTransactionControllerMXBean;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.util.jolokia.ConfigTransactionJolokiaClient;

public class ConfigTransactionClientsTest {

    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    private String jolokiaURL;
    private ConfigTransactionControllerMXBean transactionController;
    private ObjectName transactionControllerON;
    private ConfigTransactionClient jmxTransactionClient,
            jolokiaTransactionClient;

    @Before
    public void setUp() throws Exception {
        jolokiaURL = JolokiaHelper.startTestingJolokia();
        transactionController = new TestingConfigTransactionController();
        transactionControllerON = new ObjectName(ObjectNameUtil.ON_DOMAIN + ":"
                + ObjectNameUtil.TYPE_KEY + "=TransactionController");
        mbs.registerMBean(transactionController, transactionControllerON);
        jmxTransactionClient = new ConfigTransactionJMXClient(null,
                transactionControllerON,
                ManagementFactory.getPlatformMBeanServer());
        jolokiaTransactionClient = new ConfigTransactionJolokiaClient(
                jolokiaURL, transactionControllerON, null);
    }

    @After
    public void cleanUp() throws Exception {
        JolokiaHelper.stopJolokia();
        if (transactionControllerON != null) {
            mbs.unregisterMBean(transactionControllerON);
        }
    }

    @Test
    public void testLookupConfigBeans() throws Exception {
        Set<ObjectName> jmxLookup = testClientLookupConfigBeans(jmxTransactionClient);
        Set<ObjectName> jolokiaLookup = testClientLookupConfigBeans(jolokiaTransactionClient);
        assertEquals(jmxLookup, jolokiaLookup);
    }

    private Set<ObjectName> testClientLookupConfigBeans(
            ConfigTransactionClient client) {
        Set<ObjectName> beans = client.lookupConfigBeans();
        for (ObjectName on : beans) {
            assertEquals("Module", on.getKeyProperty("type"));
        }
        assertEquals(3, beans.size());
        return beans;
    }

}
