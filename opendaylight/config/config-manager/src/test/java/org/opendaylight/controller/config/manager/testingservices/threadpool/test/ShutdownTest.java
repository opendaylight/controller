/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.testingservices.threadpool.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.ModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPool;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolModuleFactory;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.osgi.framework.BundleContext;

public class ShutdownTest extends AbstractConfigTest {
    private final TestingFixedThreadPoolModuleFactory testingFixedThreadPoolModuleFactory = new TestingFixedThreadPoolModuleFactory();

    @Mock
    ModuleFactoriesResolver mockedResolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Map<String, Entry<ModuleFactory, BundleContext>> allFactories = ImmutableMap.of(
                testingFixedThreadPoolModuleFactory.getImplementationName(),
                Maps.<ModuleFactory, BundleContext>immutableEntry(testingFixedThreadPoolModuleFactory, mockedContext));
        doReturn(allFactories).when(mockedResolver).getAllFactories();
        super.initConfigTransactionManagerImpl(mockedResolver);
    }


    @Test
    public void testCreateAndDestroyBeanInSameTransaction() throws Exception {
        {
            ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
            SimpleConfigurationTest.createFixedThreadPool(transaction);
            transaction.commit();
        }
        assertEquals(1, TestingFixedThreadPool.allExecutors.size());
        doReturn(Collections.emptyMap()).when(mockedResolver).getAllFactories();
        {
            ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
            transaction.commit();
        }
        assertEquals(1, TestingFixedThreadPool.allExecutors.size());
    }
}
