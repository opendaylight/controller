/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.shutdown.impl;

import java.util.Arrays;
import java.util.Set;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class ShutdownModuleFactory extends AbstractShutdownModuleFactory {

    public ShutdownModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
                                            ShutdownModule oldModule, AutoCloseable oldInstance,
                                            BundleContext bundleContext) {
        Bundle systemBundle = bundleContext.getBundle(0);
        return new ShutdownModule(new ModuleIdentifier(NAME, instanceName), oldModule, oldInstance, systemBundle);
    }


    public ShutdownModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
                                            BundleContext bundleContext) {
        Bundle systemBundle = bundleContext.getBundle(0);
        return new ShutdownModule(new ModuleIdentifier(NAME, instanceName), systemBundle);
    }

    @Override
    public Set<ShutdownModule> getDefaultModules(DependencyResolverFactory dependencyResolverFactory, BundleContext bundleContext) {
        ModuleIdentifier id = new ModuleIdentifier(NAME, NAME);
        DependencyResolver dependencyResolver = dependencyResolverFactory.createDependencyResolver(id);
        ShutdownModule shutdownModule = instantiateModule(NAME, dependencyResolver, bundleContext);
        return new java.util.HashSet<>(Arrays.asList(shutdownModule));
    }
}
