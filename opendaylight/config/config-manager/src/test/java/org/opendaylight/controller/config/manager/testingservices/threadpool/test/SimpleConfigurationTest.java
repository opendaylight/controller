/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.threadpool.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.ValidationException.ExceptionMessageWithStackTrace;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPool;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolConfigMXBean;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

/**
 * Tests basic functionality of configuration registry:
 * <ol>
 * <li>Creation of instance</li>
 * <li>Destruction of instance</li>
 * <li>Reconfiguration of live object</li>
 * <li>Reconfiguration that triggers new object creation</li>
 * <li>Replacement of running instance with different one with same name</li>
 * </ol>
 * Only one bean is being configured - {@link TestingThreadPoolIfc} which has no
 * dependencies.
 */
public class SimpleConfigurationTest extends AbstractConfigTest {
    private static final int numberOfThreads = 5;
    private final int numberOfThreads2 = 10;
    private static final String fixed1 = "fixed1";
    private static final List<ObjectName> emptyONs = Collections
            .<ObjectName> emptyList();
    private static final ObjectName platformFixed1ON = ObjectNameUtil
            .createReadOnlyModuleON(TestingFixedThreadPoolModuleFactory.NAME, fixed1);
    private static final List<ObjectName> fixed1List = Arrays
            .asList(platformFixed1ON);

    @Before
    public void setUp() {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext,
                new TestingFixedThreadPoolModuleFactory()));
    }

    @After
    public void tearDown() {
        TestingFixedThreadPool.cleanUp();
    }

    private ObjectName firstCommit() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();

        ObjectName fixed1names = createFixedThreadPool(transaction);

        // commit
        assertEquals(1, configRegistryClient.getOpenConfigs().size());
        CommitStatus commitStatus = transaction.commit();
        assertEquals(0, configRegistryClient.getOpenConfigs().size());
        CommitStatus expected = new CommitStatus(Arrays.asList(ObjectNameUtil
                .withoutTransactionName(fixed1names)), emptyONs, emptyONs);
        assertEquals(expected, commitStatus);

        assertEquals(1, TestingFixedThreadPool.allExecutors.size());
        assertFalse(TestingFixedThreadPool.allExecutors.get(0).isShutdown());
        return fixed1names;
    }

    static ObjectName createFixedThreadPool(
            ConfigTransactionJMXClient transaction)
            throws InstanceAlreadyExistsException, InstanceNotFoundException {
        transaction.assertVersion(0, 1);

        ObjectName fixed1names = transaction.createModule(
                TestingFixedThreadPoolModuleFactory.NAME, fixed1);
        TestingFixedThreadPoolConfigMXBean fixedConfigProxy = transaction
                .newMXBeanProxy(fixed1names, TestingFixedThreadPoolConfigMXBean.class);
        fixedConfigProxy.setThreadCount(numberOfThreads);

        ObjectName retrievedNames = transaction.lookupConfigBean(
                TestingFixedThreadPoolModuleFactory.NAME, fixed1);
        assertEquals(fixed1names, retrievedNames);
        return fixed1names;
    }

    @Test
    public void testCreateAndDestroyBeanInSameTransaction() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        ObjectName fixed1names = createFixedThreadPool(transaction);
        transaction.destroyModule(fixed1names);
        CommitStatus commitStatus = transaction.commit();
        assertStatus(commitStatus, 0, 0, 0);

    }

    @Test
    public void testValidationUsingJMXClient() throws Exception {
        ConfigTransactionClient transaction = configRegistryClient
                .createTransaction();
        testValidation(transaction);
    }


    private void testValidation(ConfigTransactionClient transaction)
            throws InstanceAlreadyExistsException, ReflectionException,
            InstanceNotFoundException, MBeanException, ConflictingVersionException {
        ObjectName fixed1names = transaction.createModule(
                TestingFixedThreadPoolModuleFactory.NAME, fixed1);
        // call validate on config bean
        try {
            platformMBeanServer.invoke(fixed1names, "validate", new Object[0],
                    new String[0]);
            fail();
        } catch (MBeanException e) {
            Exception targetException = e.getTargetException();
            assertNotNull(targetException);
            assertEquals(ValidationException.class, targetException.getClass());
        }

        // validate config bean
        try {
            transaction.validateBean(fixed1names);
            fail();
        } catch (ValidationException e) {
            for (Map.Entry<String, Map<String, ExceptionMessageWithStackTrace>> exception : e
                    .getFailedValidations().entrySet()) {
                for (Map.Entry<String, ExceptionMessageWithStackTrace> entry : exception
                        .getValue().entrySet()) {
                    assertEquals(
                            "Parameter 'threadCount' must be greater than 0",
                            entry.getValue().getMessage());
                }
            }
        }
        // validate transaction
        try {
            transaction.validateConfig();
            fail();
        } catch (ValidationException e) {
            for (Map.Entry<String, Map<String, ExceptionMessageWithStackTrace>> exception : e
                    .getFailedValidations().entrySet()) {
                for (Map.Entry<String, ExceptionMessageWithStackTrace> entry : exception
                        .getValue().entrySet()) {
                    assertEquals(
                            "Parameter 'threadCount' must be greater than 0",
                            entry.getValue().getMessage());
                }
            }
        }
        try {
            transaction.commit();
        } catch (ValidationException e) {
            for (Map.Entry<String, Map<String, ExceptionMessageWithStackTrace>> exception : e
                    .getFailedValidations().entrySet()) {
                for (Map.Entry<String, ExceptionMessageWithStackTrace> entry : exception
                        .getValue().entrySet()) {
                    assertEquals(
                            "Parameter 'threadCount' must be greater than 0",
                            entry.getValue().getMessage());
                }
            }
        }
    }

    @Test
    public void test_createThreadPool_changeNumberOfThreads() throws Exception {
        firstCommit();
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        TestingFixedThreadPoolConfigMXBean fixedConfigProxy = startReconfiguringFixed1ThreadPool(transaction);
        assertEquals(numberOfThreads, fixedConfigProxy.getThreadCount());
        fixedConfigProxy.setThreadCount(numberOfThreads2);
        CommitStatus commitStatus = transaction.commit();
        checkThreadPools(1, numberOfThreads2);
        CommitStatus expected = new CommitStatus(emptyONs, fixed1List, emptyONs);
        assertEquals(expected, commitStatus);
    }

    @Test
    public void test_createFixedThreadPool_destroyIt() throws Exception {
        // 1, start transaction, create new fixed thread pool
        ObjectName fixed1name = firstCommit();

        // 2, check that configuration was copied to platform
        ObjectName on = ObjectNameUtil.withoutTransactionName(fixed1name);
        platformMBeanServer.getMBeanInfo(on);
        assertEquals(numberOfThreads, platformMBeanServer.getAttribute(on, "ThreadCount"));

        // 3, shutdown fixed1 in new transaction
        assertFalse(TestingFixedThreadPool.allExecutors.get(0).isShutdown());
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();

        // check versions
        transaction.assertVersion(1, 2);

        // test that it was copied to new transaction
        ObjectName retrievedName = transaction.lookupConfigBean(
                TestingFixedThreadPoolModuleFactory.NAME, fixed1);
        assertNotNull(retrievedName);

        // check that number of threads was copied from dynamic

        TestingFixedThreadPoolConfigMXBean fixedConfigProxy = transaction
                .newMXBeanProxy(retrievedName, TestingFixedThreadPoolConfigMXBean.class);
        assertEquals(numberOfThreads, fixedConfigProxy.getThreadCount());

        // destroy
        transaction.destroyModule(ObjectNameUtil.createTransactionModuleON(
                transaction.getTransactionName(),
                TestingFixedThreadPoolModuleFactory.NAME, fixed1));
        transaction.commit();

        // 4, check
        assertEquals(2, configRegistryClient.getVersion());
        assertEquals(0, TestingFixedThreadPool.allExecutors.size());

        // dynamic config should be removed from platform
        try {
            platformMBeanServer.getMBeanInfo(on);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof InstanceNotFoundException);
        }
    }

    @Test
    public void testReplaceFixed1() throws Exception {
        // 1, start transaction, create new fixed thread pool
        firstCommit();
        // destroy and recreate with different # of threads
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();

        transaction.destroyModule(ObjectNameUtil.createTransactionModuleON(
                transaction.getTransactionName(),
                TestingFixedThreadPoolModuleFactory.NAME, fixed1));

        ObjectName fixed1name = transaction.createModule(
                TestingFixedThreadPoolModuleFactory.NAME, fixed1);
        TestingFixedThreadPoolConfigMXBean fixedConfigProxy = transaction
                .newMXBeanProxy(fixed1name, TestingFixedThreadPoolConfigMXBean.class);
        fixedConfigProxy.setThreadCount(numberOfThreads2);
        // commit
        transaction.commit();
        // check that first threadpool is closed
        checkThreadPools(1, numberOfThreads2);
    }

    private void checkThreadPools(int expectedTotalNumberOfExecutors,
            int expectedNumberOfThreadsInLastExecutor) {
        assertEquals(expectedTotalNumberOfExecutors,
                TestingFixedThreadPool.allExecutors.size());
        for (int i = 0; i < expectedTotalNumberOfExecutors - 1; i++) {
            assertTrue(TestingFixedThreadPool.allExecutors.get(i).isShutdown());
        }
        ThreadPoolExecutor lastExecutor = TestingFixedThreadPool.allExecutors
                .get(expectedTotalNumberOfExecutors - 1);
        assertFalse(lastExecutor.isShutdown());
        assertEquals(expectedNumberOfThreadsInLastExecutor,
                lastExecutor.getMaximumPoolSize());
    }

    @Test
    public void testTriggerRecreatingInstance() throws Exception {
        // 1, start transaction, create new fixed thread pool
        firstCommit();
        // switch boolean to create new instance
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        TestingFixedThreadPoolConfigMXBean fixedConfigProxy = startReconfiguringFixed1ThreadPool(transaction);

        fixedConfigProxy.setTriggerNewInstanceCreation(true);
        // commit
        CommitStatus commitStatus = transaction.commit();
        // check that new threadpool is created and old one is closed
        checkThreadPools(1, numberOfThreads);
        CommitStatus expected = new CommitStatus(emptyONs, emptyONs, fixed1List);
        assertEquals(expected, commitStatus);
    }

    // return MBeanProxy for 'fixed1' and current transaction
    private TestingFixedThreadPoolConfigMXBean startReconfiguringFixed1ThreadPool(
            ConfigTransactionJMXClient transaction)
            throws InstanceNotFoundException {
        ObjectName fixed1name = transaction.lookupConfigBean(
                TestingFixedThreadPoolModuleFactory.NAME, fixed1);

        TestingFixedThreadPoolConfigMXBean fixedConfigProxy = transaction
                .newMXBeanProxy(fixed1name, TestingFixedThreadPoolConfigMXBean.class);
        return fixedConfigProxy;
    }

    @Test
    public void testAbort() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        assertEquals(1, configRegistryClient.getOpenConfigs().size());

        transaction.abortConfig();
        assertEquals(0, configRegistryClient.getOpenConfigs().size());
        try {
            platformMBeanServer.getMBeanInfo(transaction.getObjectName());
            fail();
        }catch(InstanceNotFoundException e){
            assertEquals("org.opendaylight.controller:TransactionName=ConfigTransaction-0-1,type=ConfigTransaction", e.getMessage());
        }
    }

    @Test
    public void testOptimisticLock_ConfigTransactionClient() throws Exception {
        ConfigTransactionJMXClient transaction1 = configRegistryClient
                .createTransaction();
        ConfigTransactionJMXClient transaction2 = configRegistryClient
                .createTransaction();
        transaction2.assertVersion(0, 2);
        transaction2.commit();
        try {
            transaction1.commit();
            fail();
        } catch (ConflictingVersionException e) {
            assertEquals(
                    "Optimistic lock failed. Expected parent version 2, was 0",
                    e.getMessage());
        }
    }

    @Test
    public void testOptimisticLock_ConfigRegistry() throws Exception {
        ConfigTransactionJMXClient transaction1 = configRegistryClient
                .createTransaction();
        ConfigTransactionJMXClient transaction2 = configRegistryClient
                .createTransaction();
        transaction2.assertVersion(0, 2);
        transaction2.commit();
        try {
            configRegistryClient.commitConfig(transaction1.getObjectName());
            fail();
        } catch (ConflictingVersionException e) {
            assertEquals(
                    "Optimistic lock failed. Expected parent version 2, was 0",
                    e.getMessage());
        }
    }


    @Test
    public void testQNames() {
        Set<String> availableModuleFactoryQNames = configRegistryClient.getAvailableModuleFactoryQNames();
        String expected = "(namespace?revision=2012-12-12)name";

        assertEquals(Sets.newHashSet(expected), availableModuleFactoryQNames);
    }

}
