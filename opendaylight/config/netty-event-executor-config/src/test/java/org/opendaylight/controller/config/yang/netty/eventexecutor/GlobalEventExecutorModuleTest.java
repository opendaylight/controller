/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.netty.eventexecutor;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

public class GlobalEventExecutorModuleTest extends AbstractConfigTest {

    private GlobalEventExecutorModuleFactory factory;
    private final String instanceName = GlobalEventExecutorModuleFactory.SINGLETON_NAME;

    @Before
    public void setUp() {
        factory = new GlobalEventExecutorModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext,factory));
    }

    @Test
    public void testCreateBean() throws InstanceAlreadyExistsException, ValidationException,
            ConflictingVersionException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createInstance(transaction, instanceName);

        transaction.validateConfig();
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 1, 0, 0);
    }

    @Test
    public void testConflictingName() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        try {
            createInstance(transaction, instanceName + "x");
            fail();
        }catch(IllegalArgumentException e){
            assertTrue(e.getMessage() + " failure", e.getMessage().contains("only allowed name is singleton"));
        }
    }

    @Test
    public void testReusingOldInstance() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException {

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction, instanceName);

        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, factory.getImplementationName());
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 0, 0, 1);
    }

    private ObjectName createInstance(ConfigTransactionJMXClient transaction, String instanceName)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(factory.getImplementationName(), instanceName);
        transaction.newMXBeanProxy(nameCreated, GlobalEventExecutorModuleMXBean.class);
        return nameCreated;
    }

}
