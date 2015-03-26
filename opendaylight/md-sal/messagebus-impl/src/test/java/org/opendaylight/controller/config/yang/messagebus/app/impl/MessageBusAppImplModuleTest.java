/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.messagebus.app.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.osgi.framework.BundleContext;

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

    //TODO: create MessageBusAppImplModule.createInstance test
}
