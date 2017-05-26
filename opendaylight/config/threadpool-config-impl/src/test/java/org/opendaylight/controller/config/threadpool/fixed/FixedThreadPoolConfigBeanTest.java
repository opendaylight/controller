/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.threadpool.fixed;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.threadpool.impl.NamingThreadFactoryModuleFactory;
import org.opendaylight.controller.config.yang.threadpool.impl.NamingThreadFactoryModuleMXBean;
import org.opendaylight.controller.config.yang.threadpool.impl.fixed.FixedThreadPoolModuleFactory;
import org.opendaylight.controller.config.yang.threadpool.impl.fixed.FixedThreadPoolModuleMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixedThreadPoolConfigBeanTest extends AbstractConfigTest {
    private static final Logger LOG = LoggerFactory.getLogger(FixedThreadPoolConfigBeanTest.class);

    private FixedThreadPoolModuleFactory factory;
    private final String nameInstance = "fixedInstance";
    private ObjectName threadFactoryON;

    @Before
    public void setUp() {
        factory = new FixedThreadPoolModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, factory,
                new NamingThreadFactoryModuleFactory()));
    }

    @Test
    public void testCreateBean() throws InstanceAlreadyExistsException, ValidationException,
            ConflictingVersionException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createFixed(transaction, nameInstance, 2, nameInstance);

        transaction.validateConfig();
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 2, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createFixed(transaction, nameInstance, 4, nameInstance);

        transaction.validateConfig();
        transaction.commit();

        assertBeanCount(1, factory.getImplementationName());

        transaction = configRegistryClient.createTransaction();
        NamingThreadFactoryModuleMXBean namingThreadFactoryModuleMXBean = transaction.newMXBeanProxy(threadFactoryON, NamingThreadFactoryModuleMXBean.class);
        namingThreadFactoryModuleMXBean.setNamePrefix("newPrefix");
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 0, 2, 0);
    }

    @Test
    public void testNegative() throws ConflictingVersionException, ValidationException, InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createFixed(transaction, nameInstance, 5, nameInstance);
        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        try {
            createFixed(transaction, nameInstance, 0, nameInstance);
            fail();
        } catch (InstanceAlreadyExistsException e) {
            assertThat(
                    e.getMessage(),
                    containsString("There is an instance registered with name ModuleIdentifier{factoryName='threadpool-fixed', instanceName='fixedInstance'}"));
        }
    }

    private int countThreadsByPrefix(String prefix) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        int result = 0;
        List<String> names = new ArrayList<>();
        for (ThreadInfo threadInfo : threadMXBean.dumpAllThreads(false, false)) {
            names.add(threadInfo.getThreadName());
            if (threadInfo.getThreadName().startsWith(prefix)) {
                result++;
            }
        }
        LOG.info("Current threads {}", names);
        return result;
    }

    @Test
    public void testDestroy() throws InstanceAlreadyExistsException, ValidationException, ConflictingVersionException,
            InstanceNotFoundException, InterruptedException {

        String prefix = org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(10);

        int numberOfThreads = 100;
        int threadCount1 = countThreadsByPrefix(prefix);
        assertEquals(0, threadCount1);
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createFixed(transaction, nameInstance, numberOfThreads, prefix);
        transaction.commit();
        int threadCount2 = countThreadsByPrefix(prefix);
        assertEquals(numberOfThreads, threadCount2);

        transaction = configRegistryClient.createTransaction();
        transaction.destroyModule(factory.getImplementationName(), nameInstance);
        CommitStatus status = transaction.commit();

        assertBeanCount(0, factory.getImplementationName());
        assertStatus(status, 0, 0, 1);

        for (int i = 0; i < 60; i++) {
            if (countThreadsByPrefix(prefix) == 0) {
                return;
            }
            Thread.sleep(1000);
        }
        assertEquals(0, countThreadsByPrefix(prefix));
    }

    @Test
    public void testValidationException() throws InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createFixed(transaction, nameInstance, -1, nameInstance);
        try {
            transaction.validateConfig();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getMessage(), containsString("MaxThreadCount must be greater than zero"));
        }
    }

    private ObjectName createFixed(ConfigTransactionJMXClient transaction, String name, int numberOfThreads, String prefix)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(factory.getImplementationName(), name);
        FixedThreadPoolModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, FixedThreadPoolModuleMXBean.class);
        mxBean.setMaxThreadCount(numberOfThreads);

        threadFactoryON = transaction.createModule(NamingThreadFactoryModuleFactory.NAME, "naming");
        NamingThreadFactoryModuleMXBean namingThreadFactoryModuleMXBean = transaction.newMXBeanProxy(threadFactoryON,
                NamingThreadFactoryModuleMXBean.class);
        namingThreadFactoryModuleMXBean.setNamePrefix(prefix);

        mxBean.setThreadFactory(threadFactoryON);

        return nameCreated;
    }

}
