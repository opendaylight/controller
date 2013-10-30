/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import java.util.Collections;
import java.util.Set;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.spi.Module;
import org.osgi.framework.BundleContext;

/**
*
*/
public class BindingBrokerImplSingletonModuleFactory extends
        org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractBindingBrokerImplSingletonModuleFactory {

    private static final String SINGLETON_NAME = "binding-broker-singleton";
    public static BindingBrokerImplSingletonModule SINGLETON;
    public static ModuleIdentifier SINGLETON_IDENTIFIER = new ModuleIdentifier(NAME, SINGLETON_NAME);

    @Override
    public Module createModule(String instanceName, DependencyResolver dependencyResolver, BundleContext bundleContext) {
        throw new UnsupportedOperationException("Only default instance supported.");
    }

    @Override
    public Module createModule(String instanceName, DependencyResolver dependencyResolver,
            DynamicMBeanWithInstance old, BundleContext bundleContext) throws Exception {
        throw new UnsupportedOperationException("Only default instance supported.");
    }
    
    @Override
    public Set<BindingBrokerImplSingletonModule> getDefaultModules(DependencyResolverFactory dependencyResolverFactory,
            BundleContext bundleContext) {
            if (SINGLETON == null) {

                DependencyResolver dependencyResolver = dependencyResolverFactory
                        .createDependencyResolver(SINGLETON_IDENTIFIER);
                SINGLETON = new BindingBrokerImplSingletonModule(SINGLETON_IDENTIFIER, dependencyResolver);
                SINGLETON.setBundleContext(bundleContext);
            }

            return Collections.singleton(SINGLETON);
    }

}
