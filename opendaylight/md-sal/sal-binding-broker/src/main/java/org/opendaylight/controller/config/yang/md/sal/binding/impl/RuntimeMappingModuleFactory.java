/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
public class RuntimeMappingModuleFactory extends
        org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractRuntimeMappingModuleFactory {

    private static RuntimeMappingModule SINGLETON = null;
    private static ModuleIdentifier IDENTIFIER = new ModuleIdentifier(NAME, "runtime-mapping-singleton");

    @Override
    public Module createModule(String instanceName, DependencyResolver dependencyResolver, BundleContext bundleContext) {
        throw new UnsupportedOperationException("Only default instance supported");
    }

    @Override
    public Module createModule(String instanceName, DependencyResolver dependencyResolver,
            DynamicMBeanWithInstance old, BundleContext bundleContext) throws Exception {
        RuntimeMappingModule module = (RuntimeMappingModule) super.createModule(instanceName, dependencyResolver, old,
                bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }

    @Override
    public Set<RuntimeMappingModule> getDefaultModules(DependencyResolverFactory dependencyResolverFactory,
            BundleContext bundleContext) {
        if (SINGLETON == null) {
            DependencyResolver dependencyResolver = dependencyResolverFactory.createDependencyResolver(IDENTIFIER);
            SINGLETON = new RuntimeMappingModule(IDENTIFIER, dependencyResolver);
            SINGLETON.setBundleContext(bundleContext);
        }

        return Collections.singleton(SINGLETON);
    }

}
