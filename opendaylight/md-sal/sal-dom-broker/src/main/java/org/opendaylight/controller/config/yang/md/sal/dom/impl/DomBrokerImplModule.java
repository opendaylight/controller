/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.core.api.data.DataStore;
import org.opendaylight.controller.sal.dom.broker.BrokerConfigActivator;
import org.opendaylight.controller.sal.dom.broker.BrokerImpl;
import org.osgi.framework.BundleContext;

/**
*
*/
public final class DomBrokerImplModule extends org.opendaylight.controller.config.yang.md.sal.dom.impl.AbstractDomBrokerImplModule
{

    private BundleContext bundleContext;

    public DomBrokerImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DomBrokerImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final DomBrokerImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate(){
        super.validate();
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BrokerImpl broker = new BrokerImpl();
        final BrokerConfigActivator activator = new BrokerConfigActivator();
        final DataStore store = getDataStoreDependency();
        final DOMDataBroker asyncBroker= getAsyncDataBrokerDependency();

        activator.start(broker, store, asyncBroker,getBundleContext());

//        final DomBrokerImplRuntimeMXBean domBrokerRuntimeMXBean = new DomBrokerRuntimeMXBeanImpl(activator.getDataService());
//        getRootRuntimeBeanRegistratorWrapper().register(domBrokerRuntimeMXBean);
        return broker;
    }

    private BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
