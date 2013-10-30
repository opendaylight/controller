/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import org.opendaylight.controller.sal.binding.impl.BindingAwareBrokerImpl;
import org.osgi.framework.BundleContext;

import com.google.common.base.Preconditions;

/**
*
*/
public final class BindingBrokerImplSingletonModule extends org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractBindingBrokerImplSingletonModule
{

    private BundleContext bundleContext;

    public BindingBrokerImplSingletonModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BindingBrokerImplSingletonModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, BindingBrokerImplSingletonModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkNotNull(getBundleContext());
    }

    
    
    
    
    public java.lang.AutoCloseable createInstance() {
        BindingAwareBrokerImpl broker = new BindingAwareBrokerImpl(getBundleContext());
        broker.start();
        return broker;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
