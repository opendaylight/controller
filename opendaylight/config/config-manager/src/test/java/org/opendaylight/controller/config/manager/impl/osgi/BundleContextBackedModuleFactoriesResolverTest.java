/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.impl.osgi;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import com.google.common.collect.Lists;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class BundleContextBackedModuleFactoriesResolverTest {

    @Mock
    private BundleContext bundleContext;
    private BundleContextBackedModuleFactoriesResolver resolver;
    private ServiceReference<?> s1;
    private ServiceReference<?> s2;
    private ModuleFactory f1;
    private ModuleFactory f2;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        s1 = getServiceRef();
        s2 = getServiceRef();
        doReturn(Lists.newArrayList(s1, s2)).when(bundleContext).getServiceReferences(ModuleFactory.class, null);
        f1 = getMockFactory("f1");
        doReturn(f1).when(bundleContext).getService(s1);
        f2 = getMockFactory("f2");
        doReturn(f2).when(bundleContext).getService(s2);
        resolver = new BundleContextBackedModuleFactoriesResolver(bundleContext);
    }

    private ModuleFactory getMockFactory(final String name) {
        ModuleFactory mock = mock(ModuleFactory.class);
        doReturn(name).when(mock).toString();
        doReturn(name).when(mock).getImplementationName();
        return mock;
    }

    private ServiceReference<?> getServiceRef() {
        ServiceReference<?> mock = mock(ServiceReference.class);
        doReturn("serviceRef").when(mock).toString();
        final Bundle bundle = mock(Bundle.class);
        doReturn(bundleContext).when(bundle).getBundleContext();
        doReturn(bundle).when(mock).getBundle();
        return mock;
    }

    @Test
    public void testGetAllFactories() throws Exception {
        Map<String, Map.Entry<ModuleFactory, BundleContext>> allFactories = resolver.getAllFactories();
        assertEquals(2, allFactories.size());
        assertTrue(allFactories.containsKey(f1.getImplementationName()));
        assertEquals(f1, allFactories.get(f1.getImplementationName()).getKey());
        assertEquals(bundleContext, allFactories.get(f1.getImplementationName()).getValue());
        assertTrue(allFactories.containsKey(f2.getImplementationName()));
        assertEquals(f2, allFactories.get(f2.getImplementationName()).getKey());
        assertEquals(bundleContext, allFactories.get(f2.getImplementationName()).getValue());
    }

    @Test
    public void testDuplicateFactories() throws Exception {
        doReturn(f1).when(bundleContext).getService(s2);
        try {
            resolver.getAllFactories();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(f1.getImplementationName()));
            assertThat(e.getMessage(), containsString("unique"));
            return;
        }

        fail("Should fail with duplicate factory name");
    }

    @Test(expected = NullPointerException.class)
    public void testNullFactory() throws Exception {
        doReturn(null).when(bundleContext).getService(s2);
        resolver.getAllFactories();
    }

    @Test(expected = IllegalStateException.class)
    public void testNullFactoryName() throws Exception {
        doReturn(null).when(f1).getImplementationName();
        resolver.getAllFactories();
    }

    @Test(expected = NullPointerException.class)
    public void testNullBundleName() throws Exception {
        doReturn(null).when(s1).getBundle();
        resolver.getAllFactories();
    }
}
