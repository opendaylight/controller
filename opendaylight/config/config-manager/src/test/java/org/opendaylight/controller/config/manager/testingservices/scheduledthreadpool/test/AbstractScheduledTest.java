/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool.test;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPModuleFactory;
import org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool.TestingScheduledThreadPoolImpl;
import org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool
        .TestingScheduledThreadPoolModuleFactory;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolModuleFactory;

public abstract class AbstractScheduledTest extends AbstractConfigTest {
    protected static final String scheduled1 = "scheduled1";

    @Before
    public final void setUp() {
        assertEquals(0,
                TestingScheduledThreadPoolImpl.getNumberOfCloseMethodCalls());
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(
                new TestingScheduledThreadPoolModuleFactory(),
                new TestingFixedThreadPoolModuleFactory(),
                new TestingParallelAPSPModuleFactory()));
    }

    @After
    public final void cleanUp() throws Exception {
        destroyAllConfigBeans();
        TestingScheduledThreadPoolImpl.cleanUp();
        assertEquals(0,
                TestingScheduledThreadPoolImpl.getNumberOfCloseMethodCalls());
    }
}
