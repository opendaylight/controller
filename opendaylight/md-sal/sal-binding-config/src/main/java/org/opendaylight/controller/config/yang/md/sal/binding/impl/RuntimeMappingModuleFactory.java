/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import static com.google.common.base.Preconditions.checkArgument;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.osgi.framework.BundleContext;

/**
 * @deprecated Replaced by blueprint wiring
 */
@Deprecated
public class RuntimeMappingModuleFactory extends
        org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractRuntimeMappingModuleFactory {

    public static final String SINGLETON_NAME = "runtime-mapping-singleton";

    @Override
    public RuntimeMappingModule instantiateModule(String instanceName, DependencyResolver dependencyResolver, RuntimeMappingModule  oldModule, AutoCloseable oldInstance, BundleContext bundleContext) {
        checkArgument(SINGLETON_NAME.equals(instanceName),"Illegal instance name '" + instanceName + "', only allowed name is " + SINGLETON_NAME);
        RuntimeMappingModule module = super.instantiateModule(instanceName, dependencyResolver, oldModule, oldInstance, bundleContext);
        // FIXME bundle context should not be passed around
        module.setBundleContext(bundleContext);
        return module;
    }

    @Override
    public RuntimeMappingModule  instantiateModule(String instanceName, DependencyResolver dependencyResolver, BundleContext bundleContext) {
        checkArgument(SINGLETON_NAME.equals(instanceName),"Illegal instance name '" + instanceName + "', only allowed name is " + SINGLETON_NAME);
        RuntimeMappingModule module = super.instantiateModule(instanceName, dependencyResolver, bundleContext);
        // FIXME bundle context should not be passed around
        module.setBundleContext(bundleContext);
        return module;
    }
}
