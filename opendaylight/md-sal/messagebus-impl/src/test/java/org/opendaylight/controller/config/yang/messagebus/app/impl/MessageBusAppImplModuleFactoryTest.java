/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.messagebus.app.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.osgi.framework.BundleContext;

public class MessageBusAppImplModuleFactoryTest {

    DependencyResolver dependencyResolverMock;
    BundleContext bundleContextMock;
    MessageBusAppImplModuleFactory messageBusAppImplModuleFactory;
    DynamicMBeanWithInstance dynamicMBeanWithInstanceMock;

    @BeforeClass
    public static void initTestClass() throws IllegalAccessException, InstantiationException {
    }

    @Before
    public void setUp() throws Exception {
        dependencyResolverMock = mock(DependencyResolver.class);
        bundleContextMock = mock(BundleContext.class);
        dynamicMBeanWithInstanceMock = mock(DynamicMBeanWithInstance.class);
        messageBusAppImplModuleFactory = new MessageBusAppImplModuleFactory();
    }

    @Test
    public void createModuleTest() {
        assertNotNull("Module has not been created correctly.", messageBusAppImplModuleFactory.createModule("instanceName1", dependencyResolverMock, bundleContextMock));
    }

    @Test
    public void createModuleBTest() throws Exception{
        MessageBusAppImplModule messageBusAppImplModuleMock = mock(MessageBusAppImplModule.class);
        doReturn(messageBusAppImplModuleMock).when(dynamicMBeanWithInstanceMock).getModule();
        assertNotNull("Module has not been created correctly.", messageBusAppImplModuleFactory.createModule("instanceName1", dependencyResolverMock, dynamicMBeanWithInstanceMock, bundleContextMock));
    }

}