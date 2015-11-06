/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.netconf.topology;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.netconf.topology.impl.NetconfTopologyImpl;
import org.osgi.framework.BundleContext;

public class NetconfTopologyModule extends org.opendaylight.controller.config.yang.netconf.topology.AbstractNetconfTopologyModule {

    private BundleContext bundleContext;

    public NetconfTopologyModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfTopologyModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver,
            final NetconfTopologyModule oldModule, final AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    public NetconfTopologyModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver,
            final NetconfTopologyModule oldModule, final AutoCloseable oldInstance, final BundleContext bundleContext) {
        this(identifier, dependencyResolver, oldModule, oldInstance);
        this.bundleContext = bundleContext;

    }

    public NetconfTopologyModule(final ModuleIdentifier moduleIdentifier, final DependencyResolver dependencyResolver,
            final BundleContext bundleContext) {
        this(moduleIdentifier, dependencyResolver);
        this.bundleContext = bundleContext;
    }

    @Override
    public void customValidation() {
        Preconditions.checkNotNull(bundleContext, "BundleContext was not properly set up");
        // add custom validation form module attributes here.
        this.getClientDispatcherDependency();
    }

    @Override
    public AutoCloseable createInstance() {
        return new NetconfTopologyImpl(getTopologyId(), getListenForConfigChanges(), getClientDispatcherDependency(),
                getBindingRegistryDependency(), getDomRegistryDependency(), getEventExecutorDependency(),
                getKeepaliveExecutorDependency(), getProcessingExecutorDependency(),
                getSharedSchemaRepositoryDependency(), bundleContext);
    }

}

