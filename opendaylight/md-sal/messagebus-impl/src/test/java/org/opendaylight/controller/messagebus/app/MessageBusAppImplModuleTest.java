/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app;

import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.yang.messagebus.app.impl.AbstractMessageBusAppImplModule;
import org.opendaylight.controller.config.yang.messagebus.app.impl.MessageBusAppImplModule;
import org.opendaylight.controller.config.yang.messagebus.app.impl.NamespaceToStream;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.messagebus.app.impl.EventSourceTopology;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.EventAggregatorService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;

import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

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
    public void createInstanceTest() {
        createInstanceTestHelper();
        messageBusAppImplModule.getInstance();
        assertNotNull("AutoCloseable instance has not been created correctly.", messageBusAppImplModule.createInstance());
    }

    private void createInstanceTestHelper(){
        NamespaceToStream namespaceToStream = mock(NamespaceToStream.class);
        List<NamespaceToStream> listNamespaceToStreamMock = new ArrayList<>();
        listNamespaceToStreamMock.add(namespaceToStream);
        messageBusAppImplModule.setNamespaceToStream(listNamespaceToStreamMock);
        ObjectName objectName = mock(ObjectName.class);
        org.opendaylight.controller.sal.core.api.Broker domBrokerDependency = mock(Broker.class);
        org.opendaylight.controller.sal.binding.api.BindingAwareBroker bindingBrokerDependency = mock(BindingAwareBroker.class);
        when(dependencyResolverMock.resolveInstance((java.lang.Class) notNull(), (javax.management.ObjectName) notNull(), eq(AbstractMessageBusAppImplModule.domBrokerJmxAttribute))).thenReturn(domBrokerDependency);
        when(dependencyResolverMock.resolveInstance((java.lang.Class) notNull(), (javax.management.ObjectName) notNull(), eq(AbstractMessageBusAppImplModule.bindingBrokerJmxAttribute))).thenReturn(bindingBrokerDependency);
        messageBusAppImplModule.setBindingBroker(objectName);
        messageBusAppImplModule.setDomBroker(objectName);
        BindingAwareBroker.ProviderContext providerContextMock = mock(BindingAwareBroker.ProviderContext.class);
        doReturn(providerContextMock).when(bindingBrokerDependency).registerProvider(any(BindingAwareProvider.class));
        Broker.ProviderSession providerSessionMock = mock(Broker.ProviderSession.class);
        doReturn(providerSessionMock).when(domBrokerDependency).registerProvider(any(Provider.class));

        DataBroker dataBrokerMock = mock(DataBroker.class);
        doReturn(dataBrokerMock).when(providerContextMock).getSALService(DataBroker.class);
        RpcProviderRegistry rpcProviderRegistryMock = mock(RpcProviderRegistry.class);
        doReturn(rpcProviderRegistryMock).when(providerContextMock).getSALService(RpcProviderRegistry.class);
        BindingAwareBroker.RpcRegistration rpcRegistrationMock = mock(BindingAwareBroker.RpcRegistration.class);
        doReturn(rpcRegistrationMock).when(rpcProviderRegistryMock).addRpcImplementation(eq(EventAggregatorService.class), any(EventSourceTopology.class));
        EventSourceService eventSourceServiceMock = mock(EventSourceService.class);
        doReturn(eventSourceServiceMock).when(rpcProviderRegistryMock).getRpcService(EventSourceService.class);

        WriteTransaction writeTransactionMock = mock(WriteTransaction.class);
        doReturn(writeTransactionMock).when(dataBrokerMock).newWriteOnlyTransaction();
        doNothing().when(writeTransactionMock).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(DataObject.class));
        CheckedFuture checkedFutureMock = mock(CheckedFuture.class);
        doReturn(checkedFutureMock).when(writeTransactionMock).submit();
    }
}
