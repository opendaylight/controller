/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.netty.eventexecutor;

import static com.google.common.base.Preconditions.checkArgument;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.osgi.framework.BundleContext;

/**
 * @deprecated Replaced by blueprint wiring
 */
@Deprecated
public class ImmediateEventExecutorModuleFactory extends org.opendaylight.controller.config.yang.netty.eventexecutor.AbstractImmediateEventExecutorModuleFactory {
    public static final String SINGLETON_NAME = "singleton";

    @Override
    public ImmediateEventExecutorModule instantiateModule(String instanceName, DependencyResolver dependencyResolver, ImmediateEventExecutorModule oldModule, AutoCloseable oldInstance, BundleContext bundleContext) {
        checkArgument(SINGLETON_NAME.equals(instanceName), "Illegal instance name '" + instanceName + "', only allowed name is " + SINGLETON_NAME);
        return super.instantiateModule(instanceName, dependencyResolver, oldModule, oldInstance, bundleContext);
    }

    @Override
    public ImmediateEventExecutorModule instantiateModule(String instanceName, DependencyResolver dependencyResolver, BundleContext bundleContext) {
        checkArgument(SINGLETON_NAME.equals(instanceName), "Illegal instance name '" + instanceName + "', only allowed name is " + SINGLETON_NAME);
        return super.instantiateModule(instanceName, dependencyResolver, bundleContext);
    }
}
