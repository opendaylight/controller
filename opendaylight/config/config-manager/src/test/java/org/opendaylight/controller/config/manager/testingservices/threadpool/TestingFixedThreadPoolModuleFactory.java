/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.threadpool;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.ModifiableThreadPoolServiceInterface;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.TestingThreadPoolServiceInterface;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;

public class TestingFixedThreadPoolModuleFactory extends AbstractTestingFixedThreadPoolModuleFactory implements ModuleFactory {
    public static final String NAME = "fixed";

    private static final Set<Class<? extends AbstractServiceInterface>> ifc = ImmutableSet.of(
            (Class<? extends AbstractServiceInterface>) ModifiableThreadPoolServiceInterface.class,
            TestingThreadPoolServiceInterface.class);

    @Override
    public String getImplementationName() {
        return NAME;
    }

    @Override
    public TestingFixedThreadPoolModule createModule(final String instanceName,
            final DependencyResolver dependencyResolver, final BundleContext bundleContext) {
        return new TestingFixedThreadPoolModule(new ModuleIdentifier(NAME,
                instanceName), null, null);
    }

    @Override
    public Module createModule(final String instanceName,
            final DependencyResolver dependencyResolver, final DynamicMBeanWithInstance old, final BundleContext bundleContext)
            throws Exception {
        int threadCount = (Integer) old.getAttribute("ThreadCount");
        // is the instance compatible?
        TestingFixedThreadPool oldInstance;
        try {
            // reconfigure existing instance
            oldInstance = (TestingFixedThreadPool) old.getInstance();
        } catch (ClassCastException e) {
            // old instance will be closed, new needs to be created
            oldInstance = null;
        }
        TestingFixedThreadPoolModule result = new TestingFixedThreadPoolModule(
                new ModuleIdentifier(NAME, instanceName), old.getInstance(),
                oldInstance);
        result.setThreadCount(threadCount);
        return result;
    }

    @Override
    public boolean isModuleImplementingServiceInterface(
            final Class<? extends AbstractServiceInterface> serviceInterface) {
        return ifc.contains(serviceInterface);
    }

    @Override
    public Set<Module> getDefaultModules(final DependencyResolverFactory dependencyResolverFactory, final BundleContext bundleContext) {
        return new HashSet<>();
    }

    @Override
    public Set<Class<? extends AbstractServiceInterface>> getImplementedServiceIntefaces() {
        return ifc;
    }
}
