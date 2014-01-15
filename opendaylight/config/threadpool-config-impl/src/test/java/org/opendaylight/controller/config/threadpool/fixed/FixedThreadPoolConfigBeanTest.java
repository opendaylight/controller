/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.threadpool.fixed;

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

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

public class FixedThreadPoolConfigBeanTest extends AbstractConfigTest {

    private FixedThreadPoolModuleFactory factory;
    private final String nameInstance = "fixedInstance";

    @Before
    public void setUp() {
        factory = new FixedThreadPoolModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(factory,
                new NamingThreadFactoryModuleFactory()));
    }

    @Test
    public void testCreateBean() throws InstanceAlreadyExistsException, ValidationException,
            ConflictingVersionException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createFixed(transaction, nameInstance, 2);

        transaction.validateConfig();
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 2, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createFixed(transaction, nameInstance, 4);

        transaction.validateConfig();
        transaction.commit();

        assertBeanCount(1, factory.getImplementationName());

        transaction = configRegistryClient.createTransaction();
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 0, 0, 2);
    }

    @Test
    public void testNegative() throws ConflictingVersionException, ValidationException, InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createFixed(transaction, nameInstance, 5);
        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        try {
            createFixed(transaction, nameInstance, 0);
            fail();
        } catch (InstanceAlreadyExistsException e) {
            assertThat(
                    e.getMessage(),
                    containsString("There is an instance registered with name ModuleIdentifier{factoryName='threadpool-fixed', instanceName='fixedInstance'}"));
        }
    }

    @Test
    public void testDestroy() throws InstanceAlreadyExistsException, ValidationException, ConflictingVersionException,
            InstanceNotFoundException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createFixed(transaction, nameInstance, 1);

        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        transaction.destroyConfigBean(factory.getImplementationName(), nameInstance);
        CommitStatus status = transaction.commit();

        assertBeanCount(0, factory.getImplementationName());
        assertStatus(status, 0, 0, 1);
    }

    @Test
    public void testValidationException() throws InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createFixed(transaction, nameInstance, -1);
        try {
            transaction.validateConfig();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getMessage(), containsString("MaxThreadCount must be greater than zero"));
        }
    }

    private ObjectName createFixed(ConfigTransactionJMXClient transaction, String name, int numberOfThreads)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(factory.getImplementationName(), name);
        FixedThreadPoolModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, FixedThreadPoolModuleMXBean.class);
        mxBean.setMaxThreadCount(numberOfThreads);

        ObjectName threadFactoryON = transaction.createModule(NamingThreadFactoryModuleFactory.NAME, "naming");
        NamingThreadFactoryModuleMXBean namingThreadFactoryModuleMXBean = transaction.newMXBeanProxy(threadFactoryON,
                NamingThreadFactoryModuleMXBean.class);
        namingThreadFactoryModuleMXBean.setNamePrefix("prefix");

        mxBean.setThreadFactory(threadFactoryON);

        return nameCreated;
    }

}
