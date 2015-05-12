/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.messagebus.app.impl;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;

import com.google.common.util.concurrent.CheckedFuture;

import javax.management.ObjectName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class MessageBusAppImplModuleTest {

    MessageBusAppImplModule messageBusAppImplModule;
    ModuleIdentifier moduleIdentifier;
    DependencyResolver dependencyResolverMock;

    @BeforeClass
    public static void initTestClass() throws IllegalAccessException, InstantiationException {
    }

    @Before
    public void setUp() throws Exception {
        moduleIdentifier = new ModuleIdentifier("factoryName1", "instanceName1");
        dependencyResolverMock = mock(DependencyResolver.class);
        messageBusAppImplModule = new MessageBusAppImplModule(moduleIdentifier, dependencyResolverMock);
    }

    @Test
    public void constructorTest() {
        assertNotNull("Instance has not been created correctly.", messageBusAppImplModule);
    }

    @Test
    public void constructorBTest() {
        MessageBusAppImplModule messageBusAppImplModuleOld = mock(MessageBusAppImplModule.class);
        java.lang.AutoCloseable oldInstanceAutocloseableMock = mock(AutoCloseable.class);
        MessageBusAppImplModule messageBusAppImplModule = new MessageBusAppImplModule(moduleIdentifier, dependencyResolverMock, messageBusAppImplModuleOld, oldInstanceAutocloseableMock);
        assertNotNull("Instance has not been created correctly.", messageBusAppImplModule);
    }

    @Test
    public void setGetBundleContextTest() {
        BundleContext bundleContext = mock(BundleContext.class);
        messageBusAppImplModule.setBundleContext(bundleContext);
        assertEquals("Set and/or get method/s don't work correctly.", bundleContext, messageBusAppImplModule.getBundleContext());
    }

    @Test
    public void createInstanceTest() throws Exception{
        org.opendaylight.controller.sal.binding.api.BindingAwareBroker bindingAwareBrokerMock = mock(org.opendaylight.controller.sal.binding.api.BindingAwareBroker.class);
        Broker brokerMock = mock(Broker.class);
        doReturn(brokerMock).when(dependencyResolverMock).resolveInstance(eq(org.opendaylight.controller.sal.core.api.Broker.class), any(ObjectName.class), any(JmxAttribute.class));
        doReturn(bindingAwareBrokerMock).when(dependencyResolverMock).resolveInstance(eq(org.opendaylight.controller.sal.binding.api.BindingAwareBroker.class), any(ObjectName.class), any(JmxAttribute.class));
        messageBusAppImplModule.resolveDependencies();

        BindingAwareBroker.ProviderContext providerContext = mock(BindingAwareBroker.ProviderContext.class);
        doReturn(providerContext).when(bindingAwareBrokerMock).registerProvider(any(BindingAwareProvider.class));
        Broker.ProviderSession providerSessionMock = mock(Broker.ProviderSession.class);
        doReturn(providerSessionMock).when(brokerMock).registerProvider(any(Provider.class));
        DataBroker dataBrokerMock = mock(DataBroker.class);
        doReturn(dataBrokerMock).when(providerContext).getSALService(eq(DataBroker.class));
        DOMNotificationPublishService domNotificationPublishServiceMock = mock(DOMNotificationPublishService.class);
        doReturn(domNotificationPublishServiceMock).when(providerSessionMock).getService(DOMNotificationPublishService.class);
        DOMMountPointService domMountPointServiceMock = mock(DOMMountPointService.class);
        doReturn(domMountPointServiceMock).when(providerSessionMock).getService(DOMMountPointService.class);
        MountPointService mountPointServiceMock = mock(MountPointService.class);
        doReturn(mountPointServiceMock).when(providerContext).getSALService(eq(MountPointService.class));
        RpcProviderRegistry rpcProviderRegistryMock = mock(RpcProviderRegistry.class);
        doReturn(rpcProviderRegistryMock).when(providerContext).getSALService(eq(RpcProviderRegistry.class));

        WriteTransaction writeTransactionMock = mock(WriteTransaction.class);
        doReturn(writeTransactionMock).when(dataBrokerMock).newWriteOnlyTransaction();
        doNothing().when(writeTransactionMock).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(DataObject.class), eq(true));
        CheckedFuture checkedFutureMock = mock(CheckedFuture.class);
        doReturn(checkedFutureMock).when(writeTransactionMock).submit();
        assertNotNull("EventSourceRegistryWrapper has not been created correctly.", messageBusAppImplModule.createInstance());
    }

}