/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.parallelapsp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;

@ThreadSafe
public class TestingParallelAPSPModuleFactory implements ModuleFactory {

    public static final String NAME = "parallel";

    @Override
    public String getImplementationName() {
        return NAME;
    }

    @Override
    public TestingParallelAPSPModule createModule(String instanceName,
            DependencyResolver dependencyResolver, BundleContext bundleContext) {
        return new TestingParallelAPSPModule(new ModuleIdentifier(NAME,
                instanceName), dependencyResolver, null, null);
    }

    @Override
    public TestingParallelAPSPModule createModule(String instanceName,
            DependencyResolver dependencyResolver, DynamicMBeanWithInstance old, BundleContext context)
            throws Exception {
        TestingParallelAPSPImpl oldInstance;
        try {
            oldInstance = (TestingParallelAPSPImpl) old.getInstance();
        } catch (ClassCastException e) {
            oldInstance = null;
        }
        TestingParallelAPSPModule result = new TestingParallelAPSPModule(
                new ModuleIdentifier(NAME, instanceName), dependencyResolver,
                old.getInstance(), oldInstance);
        // copy attributes
        String someParam = (String) old.getAttribute("SomeParam");
        result.setSomeParam(someParam);
        ObjectName threadPool = (ObjectName) old.getAttribute("ThreadPool");
        result.setThreadPool(threadPool);
        return result;
    }

    @Override
    public boolean isModuleImplementingServiceInterface(
            Class<? extends AbstractServiceInterface> serviceInterface) {
        return false;
    }

    @Override
    public Set<Module> getDefaultModules(DependencyResolverFactory dependencyResolverFactory, BundleContext context) {
        return new HashSet<>();
    }

    @Override
    public Set<Class<? extends AbstractServiceInterface>> getImplementedServiceIntefaces() {
        return Collections.emptySet();
    }
}
