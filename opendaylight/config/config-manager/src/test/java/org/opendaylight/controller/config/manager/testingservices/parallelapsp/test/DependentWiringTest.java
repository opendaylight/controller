/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.parallelapsp.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import javax.management.ObjectName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.ValidationException.ExceptionMessageWithStackTrace;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPConfigMXBean;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPImpl;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPModuleFactory;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.TestingThreadPoolServiceInterface;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPool;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolConfigMXBean;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

public class DependentWiringTest extends AbstractParallelAPSPTest {
    private final String fixed1 = "fixed1";
    private final String apsp1 = "apsp-parallel";

    @Before
    public void setUp() {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext,
                new TestingFixedThreadPoolModuleFactory(),
                new TestingParallelAPSPModuleFactory()));
    }

    @After
    public void tearDown() {
        TestingFixedThreadPool.cleanUp();
    }

    @Override
    protected String getThreadPoolImplementationName() {
        return TestingFixedThreadPoolModuleFactory.NAME;
    }

    @Test
    public void testDependencies() throws Exception {
        ObjectName apspON;
        {
            ConfigTransactionJMXClient transaction = configRegistryClient
                    .createTransaction();
            // create fixed1
            ObjectName threadPoolTransactionON = createFixed1(transaction,
                    TestingParallelAPSPImpl.MINIMAL_NUMBER_OF_THREADS);
            // create apsp-parallel
            ObjectName apspNameTransactionON = createParallelAPSP(transaction,
                    threadPoolTransactionON);
            TestingParallelAPSPConfigMXBean parallelAPSPConfigProxy = transaction
                    .newMXBeanProxy(apspNameTransactionON, TestingParallelAPSPConfigMXBean.class);
            parallelAPSPConfigProxy.setSomeParam("");// trigger validation
                                                     // failure
            try {
                transaction.validateConfig();
                fail();
            } catch (ValidationException e) {
                for (Map.Entry<String, Map<String, ExceptionMessageWithStackTrace>> exception : e
                        .getFailedValidations().entrySet()) {
                    for (Map.Entry<String, ExceptionMessageWithStackTrace> entry : exception
                            .getValue().entrySet()) {
                        assertThat(
                                entry.getValue().getMessage(),
                                containsString("Parameter 'SomeParam' is blank"));
                    }
                }
            }

            // try committing (validation fails)
            try {
                transaction.commit();
                fail();
            } catch (ValidationException e) {
                for (Map.Entry<String, Map<String, ExceptionMessageWithStackTrace>> exception : e
                        .getFailedValidations().entrySet()) {
                    for (Map.Entry<String, ExceptionMessageWithStackTrace> entry : exception
                            .getValue().entrySet()) {
                        String err = entry.getValue().getMessage();
                        assertTrue("Unexpected error message: " + err,
                                err.contains("Parameter 'SomeParam' is blank"));
                    }
                }
            }

            parallelAPSPConfigProxy.setSomeParam("abc");// fix validation
                                                        // failure
            transaction.commit();
            apspON = ObjectNameUtil
                    .withoutTransactionName(apspNameTransactionON);
        }

        // test reported apsp number of threads
        TestingParallelAPSPConfigMXBean parallelAPSPRuntimeProxy = configRegistryClient
                .newMXBeanProxy(apspON, TestingParallelAPSPConfigMXBean.class);
        assertEquals(
                (Integer) TestingParallelAPSPImpl.MINIMAL_NUMBER_OF_THREADS,
                parallelAPSPRuntimeProxy.getMaxNumberOfThreads());

        // next transaction - recreate new thread pool
        int newNumberOfThreads = TestingParallelAPSPImpl.MINIMAL_NUMBER_OF_THREADS * 2;
        {
            // start new transaction
            ConfigTransactionJMXClient transaction = configRegistryClient
                    .createTransaction();
            ObjectName threadPoolNames_newTx = transaction.lookupConfigBean(
                    getThreadPoolImplementationName(), fixed1);
            TestingFixedThreadPoolConfigMXBean fixedConfigTransactionProxy = transaction
                    .newMXBeanProxy(threadPoolNames_newTx, TestingFixedThreadPoolConfigMXBean.class);
            fixedConfigTransactionProxy.setThreadCount(newNumberOfThreads);

            transaction.commit();
        }
        // new reference should be copied to apsp-parallel
        assertEquals((Integer) newNumberOfThreads,
                parallelAPSPRuntimeProxy.getMaxNumberOfThreads());

    }

    @Test
    public void testUsingServiceReferences() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        ObjectName threadPoolON = createFixed1(transaction, 10);
        transaction.lookupConfigBean(getThreadPoolImplementationName(), fixed1);
        String refName = "ref";
        ObjectName serviceReferenceON = transaction.saveServiceReference(TestingThreadPoolServiceInterface.QNAME, refName,
                threadPoolON);
        createParallelAPSP(transaction, serviceReferenceON);
        transaction.commit();

    }
}
