/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf;

import io.netty.util.concurrent.EventExecutor;
import java.math.BigDecimal;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.controller.config.yang.md.sal.connector.netconf.AbstractNetconfConnectorModule;
import org.opendaylight.controller.config.yang.md.sal.connector.netconf.YangModuleCapabilities;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.DomainName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AbstractNetconfConnectorModuleTest {

    @Mock
    AutoCloseable autoCloseable;

    private NetconfConnectorModule module;

    class NetconfConnectorModule extends AbstractNetconfConnectorModule {
        public NetconfConnectorModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
            super(identifier, dependencyResolver);
        }

        public NetconfConnectorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,AbstractNetconfConnectorModule oldModule,java.lang.AutoCloseable oldInstance) {
            super(identifier, dependencyResolver, oldModule, oldInstance);
        }

        @Override
        public AutoCloseable createInstance() {
            return autoCloseable;
        }
    }

    @Mock
    DependencyResolver dependencyResolver;
    @Mock
    ModuleIdentifier moduleIdentifier;
    @Mock
    AutoCloseable closableInstance;
    @Mock
    AbstractNetconfConnectorModule connModule;
    @Mock
    EventExecutor eventExecutor;
    @Mock
    YangModuleCapabilities moduleCaps;
    @Mock
    BindingAwareBroker bindingAwareBroker;
    @Mock
    ThreadPool threadPool;
    @Mock
    Broker broker;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doNothing().when(dependencyResolver).validateDependency(any(Class.class), any(ObjectName.class), any(JmxAttribute.class));
        module = new NetconfConnectorModule(moduleIdentifier, dependencyResolver);

        doReturn(bindingAwareBroker).when(dependencyResolver).resolveInstance(BindingAwareBroker.class, module.getBindingRegistry(), module.bindingRegistryJmxAttribute);
        doReturn(eventExecutor).when(dependencyResolver).resolveInstance(EventExecutor.class, module.getEventExecutor(), module.eventExecutorJmxAttribute);
        doReturn(threadPool).when(dependencyResolver).resolveInstance(ThreadPool.class, module.getProcessingExecutor(), module.processingExecutorJmxAttribute);
        doReturn(broker).when(dependencyResolver).resolveInstance(Broker.class, module.getDomRegistry(), module.domRegistryJmxAttribute);
    }

    @Test
    public void testGetInstance() throws Exception {
        assertEquals(autoCloseable, module.getInstance());
    }

    @Test
    public void testConstructor() throws Exception {
        NetconfConnectorModule mod2 = new NetconfConnectorModule(moduleIdentifier, dependencyResolver, connModule, closableInstance);
        assertEquals(moduleIdentifier, mod2.getIdentifier());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsSameException() throws Exception {
        NetconfConnectorModule module = new NetconfConnectorModule(moduleIdentifier, dependencyResolver);
        module.isSame(null);
    }

    @Test
    public void testIsSame() throws Exception {
        NetconfConnectorModule module = new NetconfConnectorModule(moduleIdentifier, dependencyResolver);
        NetconfConnectorModule module2 = new NetconfConnectorModule(moduleIdentifier, dependencyResolver);
        assertTrue(module.isSame(module2));
        assertTrue(module.equals(module));
        assertEquals(module.hashCode(), module2.hashCode());
    }

    @Test
    public void testValidate() throws Exception {
        module.validate();
        verify(dependencyResolver, times(4)).validateDependency(any(Class.class), any(ObjectName.class), any(JmxAttribute.class));
    }

    @Test
    public void testGettersSetters() throws Exception {
        PortNumber port = new PortNumber(32);
        Host host = new Host(new DomainName("domain"));
        BigDecimal sleepFactor = new BigDecimal(12345);
        ObjectName objName = new ObjectName("domain: key1 = value1 , key2 = value2");

        module.setPort(port);
        module.setAddress(host);
        module.setConnectionTimeoutMillis(20L);
        module.setSleepFactor(sleepFactor);
        module.setUsername("user");
        module.setPassword("password");
        module.setTcpOnly(true);
        module.setMaxConnectionAttempts(new Long(2));
        module.setBetweenAttemptsTimeoutMillis(123);
        module.setEventExecutor(objName);
        module.setBindingRegistry(objName);
        module.setClientDispatcher(objName);
        module.setDomRegistry(objName);
        module.setProcessingExecutor(objName);
        module.setYangModuleCapabilities(moduleCaps);

        assertEquals(port, module.getPort());
        assertEquals(host, module.getAddress());
        assertEquals(new Long(20), module.getConnectionTimeoutMillis());
        assertEquals(sleepFactor, module.getSleepFactor());
        assertEquals("user", module.getUsername());
        assertEquals("password", module.getPassword());
        assertEquals(true, module.getTcpOnly());
        assertEquals(new Long(2), module.getMaxConnectionAttempts());
        assertEquals((long)123, (long)module.getBetweenAttemptsTimeoutMillis());
        assertEquals(objName, module.getEventExecutor());
        assertEquals(objName, module.getBindingRegistry());
        assertEquals(objName, module.getClientDispatcher());
        assertEquals(objName, module.getDomRegistry());
        assertEquals(objName, module.getProcessingExecutor());
        assertEquals(moduleCaps, module.getYangModuleCapabilities());
        assertEquals(module.getIdentifier(), moduleIdentifier);

        NetconfConnectorModule module2 = new NetconfConnectorModule(moduleIdentifier, dependencyResolver);
        assertFalse(module.isSame(module2));
        module2.setPort(new PortNumber(32));
        assertFalse(module.isSame(module2));
        module2.setConnectionTimeoutMillis(20L);
        assertFalse(module.isSame(module2));
        module2.setBetweenAttemptsTimeoutMillis(123);
        assertFalse(module.isSame(module2));
        module2.setSleepFactor(sleepFactor);
        assertFalse(module.isSame(module2));
        module2.setPassword("password");

        assertFalse(module.isSame(module2));
        module2.setDomRegistry(objName);
        assertFalse(module.isSame(module2));
        module2.setClientDispatcher(objName);
        assertFalse(module.isSame(module2));
        module2.setUsername("user");
        assertFalse(module.isSame(module2));
        module2.setAddress(host);
        assertFalse(module.isSame(module2));
        module2.setTcpOnly(false);
        assertFalse(module.isSame(module2));
        module2.setMaxConnectionAttempts(new Long(2));
    }
}
