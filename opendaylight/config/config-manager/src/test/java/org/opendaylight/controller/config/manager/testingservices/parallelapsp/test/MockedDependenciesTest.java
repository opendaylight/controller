/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.parallelapsp.test;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executor;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.impl.ClassBasedModuleFactory;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPImpl;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPModuleFactory;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.TestingThreadPoolServiceInterface;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingThreadPoolConfigMXBean;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingThreadPoolIfc;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

public class MockedDependenciesTest extends AbstractParallelAPSPTest {
    private final String threadPoolImplementationName = "mockedthreadpool";

    @Before
    public void setUp() {

        ClassBasedModuleFactory mockedThreadPoolConfigFactory = new ClassBasedModuleFactory(
                threadPoolImplementationName, MockedThreadPoolModule.class);

        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext,
                new TestingParallelAPSPModuleFactory(),
                mockedThreadPoolConfigFactory));
    }

    public static interface MockedTestingThreadPoolConfigMXBean extends
            TestingThreadPoolConfigMXBean {
        public void setThreadCount(int threadCount);
    }

    public static class MockedThreadPoolModule implements Module,
            MockedTestingThreadPoolConfigMXBean,
            TestingThreadPoolServiceInterface {

        private final ModuleIdentifier moduleIdentifier;

        int threadCount;

        public MockedThreadPoolModule(
                DynamicMBeanWithInstance dynamicMBeanWithInstance, ModuleIdentifier moduleIdentifier) {
            // no reconfiguration / reuse is supported
            this.moduleIdentifier = moduleIdentifier;
        }

        @Override
        public int getThreadCount() {
            return threadCount;
        }

        @Override
        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }

        @Override
        public void validate() {

        }

        @Override
        public boolean canReuse(Module oldModule) {
            return false;
        }

        @Override
        public Closeable getInstance() {
            return new MockedThreadPool(threadCount);
        }

        @Override
        public ModuleIdentifier getIdentifier() {
            return moduleIdentifier;
        }
    }

    public static class MockedThreadPool implements TestingThreadPoolIfc,
            Closeable {
        private final int threadCount;

        public MockedThreadPool(int threadCount) {
            this.threadCount = threadCount;
        }

        @Override
        public Executor getExecutor() {
            return null;
        }

        @Override
        public int getMaxNumberOfThreads() {
            return threadCount;
        }

        @Override
        public void close() throws IOException {

        }
    }

    @Override
    protected String getThreadPoolImplementationName() {
        return threadPoolImplementationName;
    }

    @Test
    public void testDependencies() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        // create fixed1
        ObjectName threadPoolTransactionON = createFixed1(transaction,
                TestingParallelAPSPImpl.MINIMAL_NUMBER_OF_THREADS);
        // create apsp-parallel
        createParallelAPSP(transaction, threadPoolTransactionON);

        transaction.commit();
    }

}
