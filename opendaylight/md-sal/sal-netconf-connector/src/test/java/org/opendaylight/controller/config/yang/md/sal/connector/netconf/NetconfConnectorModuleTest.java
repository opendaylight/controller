/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.md.sal.connector.netconf;

import com.google.common.collect.Lists;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfiguration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.DomainName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaContextFactory;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceRegistry;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NetconfConnectorModuleTest {

    @Mock
    ModuleIdentifier moduleIdentifier;
    @Mock
    DependencyResolver dependencyResolver;
    @Mock
    YangModuleCapabilities moduleCaps;
    @Mock
    BindingAwareBroker bindingAwareBroker;
    @Mock
    ThreadPool threadPool;
    @Mock
    Broker broker;
    @Mock
    EventExecutor eventExecutor;
    @Mock
    NetconfClientDispatcher netconfClientDispatcher;
    @Mock
    ExecutorService executorService;
    @Mock
    Broker.ProviderSession providerSession;
    @Mock
    BindingAwareBroker.ProviderContext providerContext;
    @Mock
    SchemaSourceRegistry schemaRegistry;
    @Mock
    SchemaContextFactory schemaContextFactory;
    @Mock
    Future future;
    @Mock
    NetconfDeviceCommunicator deviceCommunicator;

    private NetconfConnectorModule module;
    private PortNumber port;
    private Host host;
    private BigDecimal sleepFactor;
    private ObjectName objName;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        module = new NetconfConnectorModule(moduleIdentifier, dependencyResolver);
        port = new PortNumber(32);
        host = new Host(new DomainName("domain"));
        sleepFactor = new BigDecimal(12345);
        objName = new ObjectName("domain: key1 = value1 , key2 = value2");

        module.setPort(port);
        module.setAddress(host);
        module.setConnectionTimeoutMillis(20L);
        module.setSleepFactor(sleepFactor);
        module.setUsername("user");
        module.setPassword("password");
        module.setTcpOnly(false);
        module.setMaxConnectionAttempts(new Long(2));
        module.setBetweenAttemptsTimeoutMillis(123);
        module.setEventExecutor(objName);
        module.setBindingRegistry(objName);
        module.setClientDispatcher(objName);
        module.setDomRegistry(objName);
        module.setProcessingExecutor(objName);
        module.setYangModuleCapabilities(moduleCaps);
        module.setSchemaRegistry(schemaRegistry);
        module.setSchemaContextFactory(schemaContextFactory);
        doNothing().when(dependencyResolver).validateDependency(any(Class.class), any(ObjectName.class), any(JmxAttribute.class));
        doReturn(Lists.newArrayList()).when(moduleCaps).getCapability();
        doReturn("istName").when(moduleIdentifier).getInstanceName();

        doReturn(bindingAwareBroker).when(dependencyResolver).resolveInstance(BindingAwareBroker.class, module.getBindingRegistry(), module.bindingRegistryJmxAttribute);
        doReturn(eventExecutor).when(dependencyResolver).resolveInstance(EventExecutor.class, module.getEventExecutor(), module.eventExecutorJmxAttribute);
        doReturn(threadPool).when(dependencyResolver).resolveInstance(ThreadPool.class, module.getProcessingExecutor(), module.processingExecutorJmxAttribute);
        doReturn(broker).when(dependencyResolver).resolveInstance(Broker.class, module.getDomRegistry(), module.domRegistryJmxAttribute);
        doReturn(netconfClientDispatcher).when(dependencyResolver).resolveInstance(NetconfClientDispatcher.class, module.getClientDispatcher(), module.clientDispatcherJmxAttribute);
        doReturn("depResolver").when(dependencyResolver).toString();
        doNothing().when(moduleCaps).injectDependencyResolver(any(DependencyResolver.class));
        doReturn(executorService).when(threadPool).getExecutor();
        doReturn(providerSession).when(broker).registerProvider(any(Provider.class), any(BundleContext.class));
        doReturn(providerContext).when(bindingAwareBroker).registerProvider(any(BindingAwareProvider.class), any(BundleContext.class));
        doReturn(future).when(netconfClientDispatcher).createReconnectingClient(any(NetconfReconnectingClientConfiguration.class));
    }

    @Test
    public void testConnetctorModule() throws Exception {
        module.validate();
        verify(dependencyResolver, times(4)).validateDependency(any(Class.class), any(ObjectName.class), any(JmxAttribute.class));
        assertNotNull(module.getInstance());
    }

    @Test
    public void testGetters() throws Exception {
        assertEquals(port, module.getPort());
        assertEquals(host, module.getAddress());
        assertEquals(new Long(20), module.getConnectionTimeoutMillis());
        assertEquals(sleepFactor, module.getSleepFactor());
        assertEquals("user", module.getUsername());
        assertEquals("password", module.getPassword());
        assertEquals(false, module.getTcpOnly());
        assertEquals(new Long(2), module.getMaxConnectionAttempts());
        assertEquals((long)123, (long)module.getBetweenAttemptsTimeoutMillis());
        assertEquals(objName, module.getEventExecutor());
        assertEquals(objName, module.getBindingRegistry());
        assertEquals(objName, module.getClientDispatcher());
        assertEquals(objName, module.getDomRegistry());
        assertEquals(objName, module.getProcessingExecutor());
        assertEquals(moduleCaps, module.getYangModuleCapabilities());
        assertEquals(module.getIdentifier(), moduleIdentifier);
    }
}
