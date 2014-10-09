/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.md.sal.connector.netconf;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NetconfConnectorModuleFactoryTest {

    @Mock
    private DependencyResolver dependencyResolver;
    @Mock
    private DynamicMBeanWithInstance beanWithInstance;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private ModuleIdentifier moduleIdentifier;
    @Mock
    private AutoCloseable autoCloseable;

    private NetconfConnectorModule module;
    private NetconfConnectorModuleFactory moduleFactory;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        moduleFactory = new NetconfConnectorModuleFactory();
        module = new NetconfConnectorModule(moduleIdentifier, dependencyResolver);
        doReturn(module).when(beanWithInstance).getModule();
        doReturn(autoCloseable).when(beanWithInstance).getInstance();
    }

    @Test
    public void testCreateModule() throws Exception {
        assertNotNull(moduleFactory.createModule("instName", dependencyResolver, beanWithInstance, bundleContext));
        assertNotNull(moduleFactory.createModule("iName", dependencyResolver, bundleContext));
        verify(beanWithInstance, times(1)).getModule();
        verify(beanWithInstance, times(1)).getInstance();
    }
}
