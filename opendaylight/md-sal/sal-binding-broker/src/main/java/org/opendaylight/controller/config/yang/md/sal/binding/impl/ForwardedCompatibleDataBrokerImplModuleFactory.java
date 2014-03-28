/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.spi.Module;
import org.osgi.framework.BundleContext;


/**
*
*/
public class ForwardedCompatibleDataBrokerImplModuleFactory extends org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractForwardedCompatibleDataBrokerImplModuleFactory
{


    @Override
    public Module createModule(final String instanceName, final DependencyResolver dependencyResolver, final BundleContext bundleContext) {
        ForwardedCompatibleDataBrokerImplModule module = (ForwardedCompatibleDataBrokerImplModule) super.createModule(instanceName, dependencyResolver, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }

    @Override
    public Module createModule(final String instanceName, final DependencyResolver dependencyResolver,
            final DynamicMBeanWithInstance old, final BundleContext bundleContext) throws Exception {
        ForwardedCompatibleDataBrokerImplModule module = (ForwardedCompatibleDataBrokerImplModule)  super.createModule(instanceName, dependencyResolver, old, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }

}
