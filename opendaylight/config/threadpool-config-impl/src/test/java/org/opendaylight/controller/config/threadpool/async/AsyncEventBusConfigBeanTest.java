/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.threadpool.async;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.ClassBasedModuleFactory;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.threadpool.scheduled.TestingScheduledThreadPoolModule;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.threadpool.impl.AsyncEventBusModuleFactory;
import org.opendaylight.controller.config.yang.threadpool.impl.AsyncEventBusModuleMXBean;

public class AsyncEventBusConfigBeanTest extends AbstractConfigTest {

    private AsyncEventBusModuleFactory factory;
    private final String instanceName = "async1";
    private final String poolImplName = "fixed1";

    @Before
    public void setUp() {

        ClassBasedModuleFactory scheduledThreadPoolConfigFactory = createClassBasedCBF(
                TestingScheduledThreadPoolModule.class, poolImplName);

        factory = new AsyncEventBusModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext,factory,
                scheduledThreadPoolConfigFactory));
    }

    @Test
    public void testCreateBean() throws InstanceAlreadyExistsException, ValidationException,
            ConflictingVersionException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createAsynced(transaction, instanceName, transaction.createModule(poolImplName, "pool-test"));
        transaction.validateConfig();
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 2, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException {

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createAsynced(transaction, instanceName, transaction.createModule(poolImplName, "pool-test"));

        transaction.commit();

        assertBeanCount(1, factory.getImplementationName());

        transaction = configRegistryClient.createTransaction();
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 0, 0, 2);

    }

    @Test
    public void testInstanceAlreadyExistsException() throws ConflictingVersionException, ValidationException,
            InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        ObjectName poolCB = transaction.createModule(poolImplName, "pool-test");
        createAsynced(transaction, instanceName, poolCB);
        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        try {
            createAsynced(transaction, instanceName, poolCB);
            fail();
        } catch (InstanceAlreadyExistsException e) {
            assertThat(
                    e.getMessage(),
                    containsString("There is an instance registered with name ModuleIdentifier{factoryName='async-eventbus', instanceName='async1'}"));
        }
    }

    private ObjectName createAsynced(ConfigTransactionJMXClient transaction, String instanceName, ObjectName threadPool)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(factory.getImplementationName(), instanceName);
        AsyncEventBusModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated, AsyncEventBusModuleMXBean.class);
        mxBean.setThreadpool(threadPool);
        return nameCreated;
    }

}
