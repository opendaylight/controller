/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.protocol.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
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
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleFactory;

public class TimedReconnectStrategyModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "timed-reconect-stategy-facotry-impl";
    private static final String FACTORY_NAME = TimedReconnectStrategyFactoryModuleFactory.NAME;

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext,
                new TimedReconnectStrategyFactoryModuleFactory(), new GlobalEventExecutorModuleFactory()));
    }

    @Test
    public void testValidationExceptionSleepFactorNotSet() throws Exception {
        try {
            createInstance(500, 100L, null, 500L, 10L, 10000L);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("SleepFactor value is not set."));
        }
    }

    @Test
    public void testValidationExceptionSleepFactorMinValue() throws Exception {
        try {
            createInstance(500, 100L, new BigDecimal(0.5), 500L, 10L, 10000L);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("is less than 1"));
        }
    }

    @Test
    public void testValidationExceptionConnectTimeNotSet() throws Exception {
        try {
            createInstance(null, 100L, new BigDecimal(1.0), 500L, 10L, 10000L);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("ConnectTime value is not set."));
        }
    }

    @Test
    public void testValidationExceptionConnectTimeMinValue() throws Exception {
        try {
            createInstance(-1, 100L, new BigDecimal(1.0), 500L, 10L, 10000L);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("is less than 0"));
        }
    }

    @Test
    public void testValidationExceptionMinSleepNotSet() throws Exception {
        try {
            createInstance(100, null, new BigDecimal(1.0), 100L, 10L, 10000L);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("MinSleep value is not set."));
        }
    }

    @Test
    public void testValidationExceptionMaxSleep() throws Exception {
        try {
            createInstance(100, 300L, new BigDecimal(1.0), 100L, 10L, 10000L);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("is greter than MaxSleep"));
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
        final TimedReconnectStrategyFactoryModuleMXBean mxBean = transaction.newMXBeanProxy(
                transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME), TimedReconnectStrategyFactoryModuleMXBean.class);
        assertEquals(mxBean.getMinSleep(), new Long(100));
        mxBean.setMinSleep(200L);
        assertEquals(mxBean.getMinSleep(), new Long(200));
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 1);

    }

    private CommitStatus createInstance() throws Exception {
        return createInstance(500, 100L, new BigDecimal(1.0), 500L, 10L, 10000L);
    }

    private CommitStatus createInstance(final Integer connectTime, final Long minSleep, final BigDecimal sleepFactor,
            final Long maxSleep, final Long maxAttempts, final Long deadline) throws ConflictingVersionException,
            ValidationException, InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction, INSTANCE_NAME, connectTime, minSleep, sleepFactor, maxSleep, maxAttempts, deadline);
        return transaction.commit();
    }

    public static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final String InstanceName)
            throws Exception {
        return createInstance(transaction, InstanceName, 500, 100L, new BigDecimal(1.0), 500L, 10L, 10000L);
    }

    private static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final String instanceName,
            final Integer connectTime, final Long minSleep, final BigDecimal sleepFactor, final Long maxSleep,
            final Long maxAttempts, final Long deadline) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, instanceName);
        final TimedReconnectStrategyFactoryModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated,
                TimedReconnectStrategyFactoryModuleMXBean.class);
        mxBean.setConnectTime(connectTime);
        mxBean.setDeadline(deadline);
        mxBean.setMaxAttempts(maxAttempts);
        mxBean.setMaxSleep(maxSleep);
        mxBean.setMinSleep(minSleep);
        mxBean.setSleepFactor(sleepFactor);
        mxBean.setTimedReconnectExecutor(GlobalEventExecutorUtil.create(transaction));
        return nameCreated;
    }

}
