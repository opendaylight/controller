/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.impl.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import java.util.Dictionary;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;

public class ModuleFactoryBundleTrackerTest {

    @Mock
    private Bundle bundle;
    @Mock
    private BundleContext context;
    @Mock
    private ServiceRegistration<?> reg;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                return getClass().getClassLoader().loadClass((String) invocation.getArguments()[0]);
            }
        }).when(bundle).loadClass(anyString());
        doReturn("mockBundle").when(bundle).toString();
        doReturn(context).when(bundle).getBundleContext();
        doReturn(reg).when(context).registerService(anyString(), anyObject(), any(Dictionary.class));
    }

    @Test
    public void testRegisterFactory() throws Exception {
        ModuleFactoryBundleTracker.registerFactory(TestingFactory.class.getName(), bundle);
        verify(context).registerService(ModuleFactory.class.getName(), TestingFactory.currentInstance, null);
    }

    @Test
    public void testRegisterFactoryInstantiateEx() throws Exception {
        try {
            ModuleFactoryBundleTracker.registerFactory(WrongConstructorTestingFactory.class.getName(), bundle);
        } catch (Exception e) {
            verifyZeroInteractions(context);
            assertNotNull(e.getCause());
            assertEquals(InstantiationException.class, e.getCause().getClass());
            return;
        }

        fail("Cannot register without proper constructor");
    }

    @Test
    public void testRegisterFactoryInstantiateExAccess() throws Exception {
        try {
            ModuleFactoryBundleTracker.registerFactory(NoAccessConstructorTestingFactory.class.getName(), bundle);
        } catch (Exception e) {
            verifyZeroInteractions(context);
            assertNotNull(e.getCause());
            assertEquals(IllegalAccessException.class, e.getCause().getClass());
            return;
        }

        fail("Cannot register without proper constructor");
    }

    @Test
    public void testRegisterFactoryNotExtending() throws Exception {
        try {
            ModuleFactoryBundleTracker.registerFactory(NotExtendingTestingFactory.class.getName(), bundle);
        } catch (Exception e) {
            verifyZeroInteractions(context);
            return;
        }

        fail("Cannot register without extend");
    }

    @Test
    public void testRegisterFactoryNotExisting() throws Exception {
        try {
            ModuleFactoryBundleTracker.registerFactory("Unknown class", bundle);
        } catch (Exception e) {
            verifyZeroInteractions(context);
            assertNotNull(e.getCause());
            assertEquals(ClassNotFoundException.class, e.getCause().getClass());
            return;
        }

        fail("Cannot register without extend");
    }

    @Mock
    private BlankTransactionServiceTracker blankTxTracker;

    @Test
    public void testAddingBundle() throws Exception {
        final ModuleFactoryBundleTracker tracker = new ModuleFactoryBundleTracker(blankTxTracker);
        doReturn(getClass().getResource("/module-factories/module-factory-ok")).when(bundle).getEntry(anyString());
        tracker.addingBundle(bundle, mock(BundleEvent.class));
        verify(context).registerService(ModuleFactory.class.getName(), TestingFactory.currentInstance, null);
    }

    @Test
    public void testAddingBundleError() throws Exception {
        final ModuleFactoryBundleTracker tracker = new ModuleFactoryBundleTracker(blankTxTracker);
        doReturn(getClass().getResource("/module-factories/module-factory-fail")).when(bundle).getEntry(anyString());
        try {
            tracker.addingBundle(bundle, mock(BundleEvent.class));
        } catch (Exception e) {
            verifyZeroInteractions(context);
            return;
        }

        fail("Cannot register");
    }

    static class WrongConstructorTestingFactory extends TestingFactory {
        WrongConstructorTestingFactory(final String randomParam) {
        }
    }

    static class NotExtendingTestingFactory {}

    static class NoAccessConstructorTestingFactory extends TestingFactory {
        private NoAccessConstructorTestingFactory() {
        }
    }

    static class TestingFactory implements ModuleFactory {

        static TestingFactory currentInstance;

        TestingFactory() {
            currentInstance = this;
        }

        @Override
        public String getImplementationName() {
            return "Testing";
        }

        @Override
        public Module createModule(final String instanceName, final DependencyResolver dependencyResolver, final BundleContext bundleContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Module createModule(final String instanceName, final DependencyResolver dependencyResolver, final DynamicMBeanWithInstance old, final BundleContext bundleContext) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isModuleImplementingServiceInterface(final Class<? extends AbstractServiceInterface> serviceInterface) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Class<? extends AbstractServiceInterface>> getImplementedServiceIntefaces() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<? extends Module> getDefaultModules(final DependencyResolverFactory dependencyResolverFactory, final BundleContext bundleContext) {
            throw new UnsupportedOperationException();
        }
    }
}
