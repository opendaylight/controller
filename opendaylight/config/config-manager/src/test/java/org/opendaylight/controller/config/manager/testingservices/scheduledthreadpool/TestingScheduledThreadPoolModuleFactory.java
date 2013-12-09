/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool;

import com.google.common.collect.Sets;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.TestingScheduledThreadPoolServiceInterface;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.TestingThreadPoolServiceInterface;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestingScheduledThreadPoolModuleFactory implements ModuleFactory {
    public static final String NAME = "scheduled";

    private static Set<Class<? extends AbstractServiceInterface>> ifc = Collections.unmodifiableSet(Sets.newHashSet(
            (Class<? extends AbstractServiceInterface>) TestingScheduledThreadPoolServiceInterface.class,
                    TestingThreadPoolServiceInterface.class));

    @Override
    public boolean isModuleImplementingServiceInterface(
            Class<? extends AbstractServiceInterface> serviceInterface) {
        return ifc.contains(serviceInterface);
    }

    @Override
    public String getImplementationName() {
        return NAME;
    }

    @Override
    public Module createModule(String instanceName,
            DependencyResolver dependencyResolver, BundleContext bundleContext) {
        return new TestingScheduledThreadPoolModule(new ModuleIdentifier(NAME,
                instanceName), null, null);
    }

    @Override
    public Module createModule(String instanceName,
            DependencyResolver dependencyResolver, DynamicMBeanWithInstance old, BundleContext bundleContext)
            throws Exception {
        TestingScheduledThreadPoolImpl oldInstance;
        try {
            oldInstance = (TestingScheduledThreadPoolImpl) old.getInstance();
        } catch (ClassCastException e) {// happens after OSGi update
            oldInstance = null;
        }

        TestingScheduledThreadPoolModule configBean = new TestingScheduledThreadPoolModule(
                new ModuleIdentifier(NAME, instanceName), old.getInstance(),
                oldInstance);
        // copy attributes
        configBean.setRecreate((Boolean) old.getAttribute("Recreate"));
        return configBean;
    }

    @Override
    public Set<Module> getDefaultModules(DependencyResolverFactory dependencyResolverFactory, BundleContext bundleContext) {
        return new HashSet<Module>();
    }

    @Override
    public Set<Class<? extends AbstractServiceInterface>> getImplementedServiceIntefaces() {
        return ifc;
    }


}
