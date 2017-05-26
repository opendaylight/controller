/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.netty.threadgroup;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import io.netty.channel.EventLoopGroup;
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
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class NettyThreadgroupModuleTest extends AbstractConfigTest {

    private NettyThreadgroupModuleFactory factory;
    private final String instanceName = "netty1";

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Before
    public void setUp() throws Exception {
        factory = new NettyThreadgroupModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext,factory));

        Filter mockFilter = mock(Filter.class);
        doReturn("mock").when(mockFilter).toString();
        doReturn(mockFilter).when(mockedContext).createFilter(anyString());
        doNothing().when(mockedContext).addServiceListener(any(ServiceListener.class), anyString());
        ServiceReference mockServiceRef = mock(ServiceReference.class);
        doReturn(new ServiceReference[]{mockServiceRef}).when(mockedContext).
                getServiceReferences(anyString(), anyString());
        doReturn(mock(EventLoopGroup.class)).when(mockedContext).getService(mockServiceRef);
    }

    @Test
    public void testCreateBean() throws InstanceAlreadyExistsException, ValidationException, ConflictingVersionException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createInstance(transaction, instanceName, 2);
        createInstance(transaction, instanceName + 2, null);
        transaction.validateConfig();
        CommitStatus status = transaction.commit();

        assertBeanCount(2, factory.getImplementationName());
        assertStatus(status, 2, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws InstanceAlreadyExistsException, ConflictingVersionException, ValidationException {

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction, instanceName, null);

        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, factory.getImplementationName());
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 0, 0, 1);
    }

    @Test
    public void testReconfigure() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException, InstanceNotFoundException {

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction, instanceName, null);

        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, factory.getImplementationName());
        NettyThreadgroupModuleMXBean mxBean = transaction.newMBeanProxy(
                transaction.lookupConfigBean(AbstractNettyThreadgroupModuleFactory.NAME, instanceName),
                NettyThreadgroupModuleMXBean.class);
        mxBean.setThreadCount(1);
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 0, 1, 0);
    }

    private ObjectName createInstance(ConfigTransactionJMXClient transaction, String instanceName, Integer threads)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(factory.getImplementationName(), instanceName);
        NettyThreadgroupModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated, NettyThreadgroupModuleMXBean.class);
        mxBean.setThreadCount(threads);
        return nameCreated;
    }
}
