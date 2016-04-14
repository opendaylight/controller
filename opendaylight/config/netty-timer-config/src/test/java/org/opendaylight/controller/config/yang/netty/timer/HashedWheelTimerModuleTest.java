/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.netty.timer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import io.netty.util.Timer;
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
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class HashedWheelTimerModuleTest extends AbstractConfigTest {

    private HashedWheelTimerModuleFactory factory;
    private NamingThreadFactoryModuleFactory threadFactory;
    private final String instanceName = "hashed-wheel-timer1";

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Before
    public void setUp() throws Exception {
        factory = new HashedWheelTimerModuleFactory();
        threadFactory = new NamingThreadFactoryModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, factory, threadFactory));

        Filter mockFilter = mock(Filter.class);
        doReturn("mock").when(mockFilter).toString();
        doReturn(mockFilter).when(mockedContext).createFilter(anyString());
        doNothing().when(mockedContext).addServiceListener(any(ServiceListener.class), anyString());
        ServiceReference mockServiceRef = mock(ServiceReference.class);
        doReturn(new ServiceReference[]{mockServiceRef}).when(mockedContext).
                getServiceReferences(anyString(), anyString());
        doReturn(mock(Timer.class)).when(mockedContext).getService(mockServiceRef);
    }

    public void testValidationExceptionTickDuration() throws InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        try {
            createInstance(transaction, instanceName, 0L, 10, true);
            transaction.validateConfig();
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("TickDuration value must be greater than 0"));
        }
    }

    public void testValidationExceptionTicksPerWheel() throws InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        try {
            createInstance(transaction, instanceName, 500L, 0, true);
            transaction.validateConfig();
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("TicksPerWheel value must be greater than 0"));
        }
    }

    @Test
    public void testCreateBean() throws InstanceAlreadyExistsException, ValidationException,
            ConflictingVersionException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createInstance(transaction, instanceName, 500L, 10, true);
        createInstance(transaction, instanceName + 1, null, null, false);
        createInstance(transaction, instanceName + 2, 500L, 10, false);
        createInstance(transaction, instanceName + 3, 500L, null, false);
        transaction.validateConfig();
        CommitStatus status = transaction.commit();

        assertBeanCount(4, factory.getImplementationName());
        assertStatus(status, 5, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException {

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction, instanceName, 500L, 10, true);

        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, factory.getImplementationName());
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 0, 0, 2);
    }

    @Test
    public void testReconfigure() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException, InstanceNotFoundException {

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction, instanceName, 500L, 10, true);
        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, factory.getImplementationName());
        HashedWheelTimerModuleMXBean mxBean = transaction.newMBeanProxy(
                transaction.lookupConfigBean(factory.getImplementationName(), instanceName),
                HashedWheelTimerModuleMXBean.class);
        mxBean.setTicksPerWheel(20);
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 0, 1, 1);
    }

    private ObjectName createInstance(final ConfigTransactionJMXClient transaction, final String instanceName,
            final Long tickDuration, final Integer ticksPerWheel, final boolean hasThreadfactory)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(factory.getImplementationName(), instanceName);
        HashedWheelTimerModuleMXBean mxBean = transaction
                .newMBeanProxy(nameCreated, HashedWheelTimerModuleMXBean.class);
        mxBean.setTickDuration(tickDuration);
        mxBean.setTicksPerWheel(ticksPerWheel);
        if (hasThreadfactory) {
            mxBean.setThreadFactory(createThreadfactoryInstance(transaction, "thread-factory1", "th"));
        }
        return nameCreated;
    }

    private ObjectName createThreadfactoryInstance(final ConfigTransactionJMXClient transaction, final String instanceName,
            final String namePrefix) throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(threadFactory.getImplementationName(), instanceName);
        NamingThreadFactoryModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated,
                NamingThreadFactoryModuleMXBean.class);
        mxBean.setNamePrefix(namePrefix);
        return nameCreated;
    }

}
