/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Sets;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.jmx.BaseJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionModuleJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.runtimembean.TestingRuntimeBean;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;

public class ConfigTransactionControllerImplTest extends
        AbstractLockedPlatformMBeanServerTest {

    private BaseJMXRegistrator baseJMXRegistrator;

    private ConfigTransactionControllerImpl testedTxController;
    private MBeanServer transactionsMBeanServer;

    private static final String transactionName123 = "testTX1";
    private static final String transactionName4 = "testTX2";

    private static final String moduleName124 = "module124";
    private static final String moduleName3 = "module3";

    private static final String instanceName134 = "instA";
    private static final String instanceName2 = "instB";

    private static final ObjectName name1 = ObjectNameUtil
            .createTransactionModuleON(transactionName123, moduleName124, instanceName134);
    private static final ObjectName name2 = ObjectNameUtil
            .createTransactionModuleON(transactionName123, moduleName124, instanceName2);
    private static final ObjectName name3 = ObjectNameUtil
            .createTransactionModuleON(transactionName123, moduleName3, instanceName134);
    private static final ObjectName name4 = ObjectNameUtil
            .createTransactionModuleON(transactionName4, moduleName124, instanceName134);

    @Before
    public void setUp() throws Exception {
        baseJMXRegistrator = new BaseJMXRegistrator(
                ManagementFactory.getPlatformMBeanServer());
        transactionsMBeanServer = MBeanServerFactory.createMBeanServer();
        Map<String, Map.Entry<ModuleFactory, BundleContext>> currentlyRegisteredFactories = new HashMap<>();

        ConfigTransactionLookupRegistry txLookupRegistry = new ConfigTransactionLookupRegistry(new TransactionIdentifier(transactionName123), new TransactionJMXRegistratorFactory() {
            @Override
            public TransactionJMXRegistrator create() {
                return baseJMXRegistrator.createTransactionJMXRegistrator(transactionName123);
            }
        }, currentlyRegisteredFactories);

        SearchableServiceReferenceWritableRegistry writableRegistry = ServiceReferenceRegistryImpl.createSRWritableRegistry(
                ServiceReferenceRegistryImpl.createInitialSRLookupRegistry(), txLookupRegistry, currentlyRegisteredFactories);


        testedTxController = new ConfigTransactionControllerImpl(
                txLookupRegistry, 1, null, 1,
                currentlyRegisteredFactories, transactionsMBeanServer,
                ManagementFactory.getPlatformMBeanServer(), false, writableRegistry);
        TransactionModuleJMXRegistrator transactionModuleJMXRegistrator123 = testedTxController
                .getTxModuleJMXRegistrator();
        transactionModuleJMXRegistrator123.registerMBean(
                new TestingRuntimeBean(), name1);
        transactionModuleJMXRegistrator123.registerMBean(
                new TestingRuntimeBean(), name2);
        transactionModuleJMXRegistrator123.registerMBean(
                new TestingRuntimeBean(), name3);
        TransactionJMXRegistrator jmxRegistrator4 = baseJMXRegistrator
                .createTransactionJMXRegistrator(transactionName4);
        jmxRegistrator4.createTransactionModuleJMXRegistrator().registerMBean(
                new TestingRuntimeBean(), name4);
    }

    @After
    public void cleanUp() {
        baseJMXRegistrator.close();
        MBeanServerFactory.releaseMBeanServer(transactionsMBeanServer);
    }

    /**
     * Tests if lookup method returns all beans with defined transaction name
     *
     * @throws Exception
     */
    @Test
    public void testLookupConfigBeans() {
        Set<ObjectName> beans = testedTxController.lookupConfigBeans();
        assertEquals(Sets.newHashSet(name1, name2, name3), beans);
    }

    @Test
    public void testLookupConfigBeansWithModuleName() {
        Set<ObjectName> beans = testedTxController
                .lookupConfigBeans(moduleName124);
        assertEquals(Sets.newHashSet(name1, name2), beans);
    }

    @Test
    public void lookupConfigBeansWithModuleNameAndImplName() throws Exception {
        Set<ObjectName> beans = testedTxController.lookupConfigBeans(
                moduleName124, instanceName134);
        assertEquals(Sets.newHashSet(name1), beans);
    }
}