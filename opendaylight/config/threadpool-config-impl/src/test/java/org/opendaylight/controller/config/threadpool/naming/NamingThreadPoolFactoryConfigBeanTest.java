/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.threadpool.naming;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
import org.opendaylight.controller.config.yang.threadpool.ThreadFactoryServiceInterface;
import org.opendaylight.controller.config.yang.threadpool.impl.NamingThreadFactoryModuleFactory;
import org.opendaylight.controller.config.yang.threadpool.impl.NamingThreadFactoryModuleMXBean;

public class NamingThreadPoolFactoryConfigBeanTest extends AbstractConfigTest {

    private NamingThreadFactoryModuleFactory factory;
    private final String instanceName = "named";

    @Before
    public void setUp() {

        factory = new NamingThreadFactoryModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, factory));
    }

    @Test
    public void testCreateBean() throws InstanceAlreadyExistsException, ValidationException,
            ConflictingVersionException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createNamed(transaction, instanceName, "prefixes");
        transaction.validateConfig();
        CommitStatus status = transaction.commit();

        assertEquals(1, configRegistry.lookupConfigBeans(factory.getImplementationName()).size());
        assertEquals(1, status.getNewInstances().size());
        assertEquals(0, status.getRecreatedInstances().size());
        assertEquals(0, status.getReusedInstances().size());
    }

    @Test
    public void testReusingOldInstance() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException {

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createNamed(transaction, instanceName, "prefixes");

        transaction.commit();

        assertEquals(1, configRegistry.lookupConfigBeans(factory.getImplementationName()).size());

        transaction = configRegistryClient.createTransaction();
        CommitStatus status = transaction.commit();

        assertEquals(1, configRegistry.lookupConfigBeans(factory.getImplementationName()).size());
        assertEquals(0, status.getNewInstances().size());
        assertEquals(0, status.getRecreatedInstances().size());
        assertEquals(1, status.getReusedInstances().size());

    }

    @Test
    public void testInstanceAlreadyExistsException() throws ConflictingVersionException, ValidationException,
            InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createNamed(transaction, instanceName, "prefixes");
        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        try {
            createNamed(transaction, instanceName, "prefixes1");
            fail();
        } catch (InstanceAlreadyExistsException e) {
            assertThat(
                    e.getMessage(),
                    containsString("There is an instance registered with name ModuleIdentifier{factoryName='threadfactory-naming', instanceName='named'}"));
        }
    }

    @Test
    public void testValidationException() throws InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        ObjectName nameCreated = transaction.createModule(factory.getImplementationName(), instanceName);
        transaction.newMXBeanProxy(nameCreated, ThreadFactoryServiceInterface.class);
        try {
            transaction.validateConfig();
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getFailedValidations().containsKey(factory.getImplementationName()));
            assertEquals(1, e.getFailedValidations().get(factory.getImplementationName()).keySet().size());
        }
    }

    @Test
    public void testReconfigurationInstance() throws InstanceAlreadyExistsException, ValidationException,
            ConflictingVersionException, InstanceNotFoundException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createNamed(transaction, instanceName, "pref");

        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        ObjectName databaseNew = transaction.lookupConfigBean(factory.getImplementationName(), instanceName);
        NamingThreadFactoryModuleMXBean proxy = transaction.newMXBeanProxy(databaseNew,
                NamingThreadFactoryModuleMXBean.class);
        proxy.setNamePrefix("pref1");

        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 0, 1, 0);
    }

    private ObjectName createNamed(ConfigTransactionJMXClient transaction, String instanceName, String prefixes)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(factory.getImplementationName(), instanceName);
        NamingThreadFactoryModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated,
                NamingThreadFactoryModuleMXBean.class);
        mxBean.setNamePrefix(prefixes);
        return nameCreated;
    }

}
