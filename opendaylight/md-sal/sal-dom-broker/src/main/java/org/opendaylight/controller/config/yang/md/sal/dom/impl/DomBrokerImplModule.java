/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import org.opendaylight.controller.config.yang.md.sal.dom.statistics.DomBrokerRuntimeMXBeanImpl;
import org.opendaylight.controller.sal.core.api.data.DataStore;
import org.opendaylight.controller.sal.dom.broker.BrokerConfigActivator;
import org.opendaylight.controller.sal.dom.broker.BrokerImpl;
import org.osgi.framework.BundleContext;

import static com.google.common.base.Preconditions.*;

/**
*
*/
public final class DomBrokerImplModule extends org.opendaylight.controller.config.yang.md.sal.dom.impl.AbstractDomBrokerImplModule
{

    private BundleContext bundleContext;

    public DomBrokerImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DomBrokerImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, DomBrokerImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate(){
        super.validate();
        checkArgument(getDataStore() != null, "Data Store needs to be provided for DomBroker");
    }
    
    @Override
    public java.lang.AutoCloseable createInstance() {
        final BrokerImpl broker = new BrokerImpl();
        final BrokerConfigActivator activator = new BrokerConfigActivator();
        final DataStore store = getDataStoreDependency();
        activator.start(broker, store, getBundleContext());
        
        final DomBrokerImplRuntimeMXBean domBrokerRuntimeMXBean = new DomBrokerRuntimeMXBeanImpl(activator.getDataService());
        getRootRuntimeBeanRegistratorWrapper().register(domBrokerRuntimeMXBean);
        return broker;
    }

    private BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
