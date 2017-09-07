/*
 * Copyright (c) 2013, 2017 Cisco Systems, Inc. and others.  All rights reserved.
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

public class ConfigTransactionControllerImplTest extends AbstractLockedPlatformMBeanServerTest {

    private BaseJMXRegistrator baseJMXRegistrator;

    private ConfigTransactionControllerImpl testedTxController;
    private MBeanServer transactionsMBeanServer;

    private static final String TRANSACTION_NAME123 = "testTX1";
    private static final String TRANSACTION_NAME4 = "testTX2";

    private static final String MODULE_NAME124 = "module124";
    private static final String MODULE_NAME3 = "module3";

    private static final String INSTANCE_NAME134 = "instA";
    private static final String INSTANCE_NAME2 = "instB";

    private static final ObjectName NAME1 =
            ObjectNameUtil.createTransactionModuleON(TRANSACTION_NAME123, MODULE_NAME124, INSTANCE_NAME134);
    private static final ObjectName NAME2 =
            ObjectNameUtil.createTransactionModuleON(TRANSACTION_NAME123,
            MODULE_NAME124, INSTANCE_NAME2);
    private static final ObjectName NAME3 =
            ObjectNameUtil.createTransactionModuleON(TRANSACTION_NAME123, MODULE_NAME3, INSTANCE_NAME134);
    private static final ObjectName NAME4 =
            ObjectNameUtil.createTransactionModuleON(TRANSACTION_NAME4, MODULE_NAME124, INSTANCE_NAME134);

    @Before
    public void setUp() throws Exception {
        baseJMXRegistrator = new BaseJMXRegistrator(ManagementFactory.getPlatformMBeanServer());
        transactionsMBeanServer = MBeanServerFactory.createMBeanServer();
        Map<String, Map.Entry<ModuleFactory, BundleContext>> currentlyRegisteredFactories = new HashMap<>();

        ConfigTransactionLookupRegistry txLookupRegistry = new ConfigTransactionLookupRegistry(
                new TransactionIdentifier(TRANSACTION_NAME123),
            () -> baseJMXRegistrator.createTransactionJMXRegistrator(TRANSACTION_NAME123),
            currentlyRegisteredFactories);

        SearchableServiceReferenceWritableRegistry writableRegistry = ServiceReferenceRegistryImpl
                .createSRWritableRegistry(ServiceReferenceRegistryImpl.createInitialSRLookupRegistry(),
                        txLookupRegistry, currentlyRegisteredFactories);

        testedTxController = new ConfigTransactionControllerImpl(txLookupRegistry, 1, null, 1,
                currentlyRegisteredFactories, transactionsMBeanServer, ManagementFactory.getPlatformMBeanServer(),
                false, writableRegistry);
        TransactionModuleJMXRegistrator transactionModuleJMXRegistrator123 = testedTxController
                .getTxModuleJMXRegistrator();
        transactionModuleJMXRegistrator123.registerMBean(new TestingRuntimeBean(), NAME1);
        transactionModuleJMXRegistrator123.registerMBean(new TestingRuntimeBean(), NAME2);
        transactionModuleJMXRegistrator123.registerMBean(new TestingRuntimeBean(), NAME3);
        TransactionJMXRegistrator jmxRegistrator4 = baseJMXRegistrator
                .createTransactionJMXRegistrator(TRANSACTION_NAME4);
        jmxRegistrator4.createTransactionModuleJMXRegistrator().registerMBean(new TestingRuntimeBean(), NAME4);
    }

    @After
    public void cleanUp() {
        baseJMXRegistrator.close();
        MBeanServerFactory.releaseMBeanServer(transactionsMBeanServer);
    }

    /**
     * Tests if lookup method returns all beans with defined transaction name.
     */
    @Test
    public void testLookupConfigBeans() {
        Set<ObjectName> beans = testedTxController.lookupConfigBeans();
        assertEquals(Sets.newHashSet(NAME1, NAME2, NAME3), beans);
    }

    @Test
    public void testLookupConfigBeansWithModuleName() {
        Set<ObjectName> beans = testedTxController.lookupConfigBeans(MODULE_NAME124);
        assertEquals(Sets.newHashSet(NAME1, NAME2), beans);
    }

    @Test
    public void lookupConfigBeansWithModuleNameAndImplName() throws Exception {
        Set<ObjectName> beans = testedTxController.lookupConfigBeans(MODULE_NAME124, INSTANCE_NAME134);
        assertEquals(Sets.newHashSet(NAME1), beans);
    }
}
