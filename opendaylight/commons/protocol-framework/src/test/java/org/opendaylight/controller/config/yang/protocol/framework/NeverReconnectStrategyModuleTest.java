/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.protocol.framework;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NeverReconnectStrategyModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "never-reconect-strategy-factory-impl";
    private static final String FACTORY_NAME = NeverReconnectStrategyFactoryModuleFactory.NAME;

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext,
                new NeverReconnectStrategyFactoryModuleFactory(), new GlobalEventExecutorModuleFactory()));
    }

    @Test
    public void testValidationExceptionTimeoutNotSet() throws Exception {
        try {
            createInstance(null);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Timeout value is not set."));
        }
    }

    @Test
    public void testValidationExceptionTimeoutMinValue() throws Exception {
        try {
            createInstance(-1);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("is less than 0"));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 2, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 2);
    }

    @Test
    public void testReconfigure() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final NeverReconnectStrategyFactoryModuleMXBean mxBean = transaction.newMXBeanProxy(
                transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME), NeverReconnectStrategyFactoryModuleMXBean.class);
        mxBean.setTimeout(200);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 1);
    }

    private CommitStatus createInstance() throws Exception {
        return createInstance(500);
    }

    private CommitStatus createInstance(final Integer timeout) throws InstanceAlreadyExistsException,
            ConflictingVersionException, ValidationException {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createInstance(transaction, timeout);
        return transaction.commit();
    }

    private static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final Integer timeout)
            throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final NeverReconnectStrategyFactoryModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated,
                NeverReconnectStrategyFactoryModuleMXBean.class);
        mxBean.setTimeout(timeout);
        mxBean.setExecutor(GlobalEventExecutorUtil.create(transaction));
        return nameCreated;
    }

}
