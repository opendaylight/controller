/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.yang.md.sal.connector.netconf.AbstractNetconfConnectorModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.connector.netconf.NetconfConnectorModule;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AbstractNetconfConnectorModuleFactoryTest {

    @Mock
    DependencyResolver dependencyResolver;
    @Mock
    BundleContext bundleContext;
    @Mock
    DynamicMBeanWithInstance dynamicMBeanWithInstance;
    @Mock
    ModuleIdentifier moduleIdentifier;
    @Mock
    AutoCloseable autoClosable;

    private NetconfConnectorModule netconfConnectorModule;
    private AbstractNetconfConnectorModuleImpl connectorModule;

    private class AbstractNetconfConnectorModuleImpl extends AbstractNetconfConnectorModuleFactory {

    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        netconfConnectorModule = new NetconfConnectorModule(moduleIdentifier, dependencyResolver);
        connectorModule = new AbstractNetconfConnectorModuleImpl();
        doReturn(netconfConnectorModule).when(dynamicMBeanWithInstance).getModule();
        doReturn(autoClosable).when(dynamicMBeanWithInstance).getInstance();
    }

    @Test
    public void test() throws Exception {
        assertNotNull(connectorModule.createModule("instanceName", dependencyResolver, bundleContext));
        assertNotNull(connectorModule.createModule("instName", dependencyResolver, dynamicMBeanWithInstance, bundleContext));
        assertNotNull(connectorModule.getImplementedServiceIntefaces());
        verify(dynamicMBeanWithInstance, times(1)).getModule();
        assertEquals("sal-netconf-connector", connectorModule.getImplementationName());
        assertNotNull(connectorModule.getDefaultModules(null, null));
    }
}
