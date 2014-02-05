/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.dynamicdependencies;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;

import java.util.Collections;
import java.util.Set;

public class ThreadPoolDependerModuleFactory implements ModuleFactory {
    public static final String NAME = "depender";

    @Override
    public String getImplementationName() {
        return NAME;
    }

    @Override
    public Module createModule(String instanceName, DependencyResolver dependencyResolver, BundleContext bundleContext) {
        ModuleIdentifier mi = new ModuleIdentifier(NAME, instanceName);
        return new ThreadPoolDependerModule(mi, dependencyResolver);
    }

    @Override
    public Module createModule(String instanceName, DependencyResolver dependencyResolver, DynamicMBeanWithInstance old,
                               BundleContext bundleContext) throws Exception {
        ModuleIdentifier mi = new ModuleIdentifier(NAME, instanceName);
        return new ThreadPoolDependerModule(mi, dependencyResolver);
    }

    @Override
    public boolean isModuleImplementingServiceInterface(Class<? extends AbstractServiceInterface> serviceInterface) {
        return false;
    }

    @Override
    public Set<Class<? extends AbstractServiceInterface>> getImplementedServiceIntefaces() {
        return Collections.emptySet();
    }

    @Override
    public Set<? extends Module> getDefaultModules(DependencyResolverFactory dependencyResolverFactory, BundleContext bundleContext) {
        return Collections.emptySet();
    }
}
