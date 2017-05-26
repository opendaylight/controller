/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.threadpool.flexible;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
import org.opendaylight.controller.config.yang.threadpool.impl.flexible.FlexibleThreadPoolModuleFactory;
import org.opendaylight.controller.config.yang.threadpool.impl.flexible.FlexibleThreadPoolModuleMXBean;

public class FlexibleThreadPoolConfigBeanTest extends AbstractConfigTest {

    private FlexibleThreadPoolModuleFactory flexibleFactory;
    private final String instanceName = "flexible1";
    private final String threadFactoryName = "threadFactoryName";

    @Before
    public void setUp() {

        flexibleFactory = new FlexibleThreadPoolModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext,flexibleFactory,
                new NamingThreadFactoryModuleFactory()));
    }

    @Test
    public void testCreateBean() throws InstanceAlreadyExistsException, ValidationException,
            ConflictingVersionException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createFlexible(transaction, instanceName, threadFactoryName, 1, 20, 20);
        transaction.validateConfig();
        CommitStatus status = transaction.commit();

        assertBeanCount(1, flexibleFactory.getImplementationName());
        assertStatus(status, 2, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException {

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createFlexible(transaction, instanceName, threadFactoryName, 1, 20, 10);

        transaction.commit();

        assertBeanCount(1, flexibleFactory.getImplementationName());

        transaction = configRegistryClient.createTransaction();
        CommitStatus status = transaction.commit();

        assertBeanCount(1, flexibleFactory.getImplementationName());
        assertStatus(status, 0, 0, 2);

    }

    @Test
    public void testInstanceAlreadyExistsException() throws ConflictingVersionException, ValidationException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        try {
            createFlexible(transaction, instanceName, threadFactoryName, 1, 1, 2);
            transaction.commit();
        } catch (InstanceAlreadyExistsException e1) {
            fail();
        }

        transaction = configRegistryClient.createTransaction();
        try {
            createFlexible(transaction, instanceName, "threadFactoryName1", 2, 2, 2);
            fail();
        } catch (InstanceAlreadyExistsException e) {
            assertThat(
                    e.getMessage(),
                    containsString("There is an instance registered with name ModuleIdentifier{factoryName='threadpool-flexible', instanceName='flexible1'}"));
        }
    }

    @Test
    public void testValidationException() throws InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createFlexible(transaction, instanceName, threadFactoryName, 0, 10, 10);

        try {
            transaction.validateConfig();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getMessage(), containsString("MinThreadCount must be greater than zero"));
        }
    }

    @Test
    public void testValidationException2() throws InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createFlexible(transaction, instanceName, threadFactoryName, 0, 0, 10);

        try {
            transaction.validateConfig();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getMessage(), containsString("KeepAliveMillis must be greater than zero"));
        }
    }

    @Test
    public void testValidationException3() throws InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createFlexible(transaction, instanceName, threadFactoryName, 10, 50, 0);

        try {
            transaction.validateConfig();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getMessage(), containsString("MaxThreadCount must be greater than zero"));
        }
    }

    private ObjectName createFlexible(ConfigTransactionJMXClient transaction, String instanceName,
            String threadFactoryName, int minThreadCount, long keepAliveMillis, int maxThreadCount)
            throws InstanceAlreadyExistsException {

        ObjectName threadFactoryON = transaction.createModule(NamingThreadFactoryModuleFactory.NAME, threadFactoryName);
        NamingThreadFactoryModuleMXBean namingThreadFactoryModuleMXBean = transaction.newMXBeanProxy(threadFactoryON,
                NamingThreadFactoryModuleMXBean.class);
        namingThreadFactoryModuleMXBean.setNamePrefix("prefix");

        ObjectName flexibleON = transaction.createModule(flexibleFactory.getImplementationName(), instanceName);
        FlexibleThreadPoolModuleMXBean mxBean = transaction.newMBeanProxy(flexibleON,
                FlexibleThreadPoolModuleMXBean.class);
        mxBean.setKeepAliveMillis(keepAliveMillis);
        mxBean.setMaxThreadCount(maxThreadCount);
        mxBean.setMinThreadCount(minThreadCount);
        mxBean.setThreadFactory(threadFactoryON);
        return flexibleON;
    }

    @Test
    public void testReconfigurationInstance() throws InstanceAlreadyExistsException, ValidationException,
            ConflictingVersionException, InstanceNotFoundException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createFlexible(transaction, instanceName, threadFactoryName, 2, 2, 2);

        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        ObjectName databaseNew = transaction.lookupConfigBean(flexibleFactory.getImplementationName(), instanceName);
        FlexibleThreadPoolModuleMXBean proxy = transaction.newMXBeanProxy(databaseNew,
                FlexibleThreadPoolModuleMXBean.class);
        proxy.setMaxThreadCount(99);

        CommitStatus status = transaction.commit();

        assertBeanCount(1, flexibleFactory.getImplementationName());
        assertStatus(status, 0, 1, 1);
    }

}
