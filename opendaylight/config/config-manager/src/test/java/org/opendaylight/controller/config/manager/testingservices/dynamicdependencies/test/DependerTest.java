/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.dynamicdependencies.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.testingservices.dynamicdependencies.ThreadPoolDependerModule.DependerInstance;
import org.opendaylight.controller.config.manager.testingservices.dynamicdependencies.ThreadPoolDependerModuleFactory;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPImpl;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.test.AbstractParallelAPSPTest;
import org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool.TestingScheduledThreadPoolImpl;
import org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool.TestingScheduledThreadPoolModuleFactory;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPool;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolModuleFactory;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingThreadPoolIfc;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

import javax.management.ObjectName;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DependerTest extends AbstractConfigTest {


    @Before
    public void setUp() {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(
                new TestingFixedThreadPoolModuleFactory(),
                new TestingScheduledThreadPoolModuleFactory(),
                new ThreadPoolDependerModuleFactory()));
    }

    @After
    public void tearDown() {
        TestingFixedThreadPool.cleanUp();
        TestingScheduledThreadPoolImpl.cleanUp();
    }


    @Test
    public void failOnValidationIfNoThreadPoolsAreFound() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        ObjectName depender = transaction.createModule(ThreadPoolDependerModuleFactory.NAME, "depender");
        try {
            transaction.commit();
            fail();
        }catch(ValidationException e) {
        }
    }

    @Test
    public void test() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        ObjectName tp1ON = AbstractParallelAPSPTest.createFixedTP(transaction,
                TestingParallelAPSPImpl.MINIMAL_NUMBER_OF_THREADS, TestingFixedThreadPoolModuleFactory.NAME, "bar");
        ObjectName scheduled1 = transaction.createModule(TestingScheduledThreadPoolModuleFactory.NAME,
                "scheduled1");

        ObjectName depender = transaction.createModule(ThreadPoolDependerModuleFactory.NAME, "depender");

        transaction.commit();


        // validate that both TPs are in depender
        DependerInstance dependerInstance = (DependerInstance) getInstanceFromCurrentConfig(ModuleIdentifier.from(depender));
        Map<ModuleIdentifier,TestingThreadPoolIfc> threadPools = dependerInstance.getThreadPools();
        assertEquals(2, threadPools.size());
        assertTrue(threadPools.containsKey(ModuleIdentifier.from(tp1ON)));
        assertTrue(threadPools.containsKey(ModuleIdentifier.from(scheduled1)));

    }

}
