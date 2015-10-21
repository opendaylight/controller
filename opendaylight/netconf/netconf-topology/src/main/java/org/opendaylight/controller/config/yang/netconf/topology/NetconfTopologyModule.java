/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.netconf.topology;

import org.opendaylight.controller.netconf.topology.impl.NetconfTopologyImpl;

public class NetconfTopologyModule extends org.opendaylight.controller.config.yang.netconf.topology.AbstractNetconfTopologyModule {
    public NetconfTopologyModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfTopologyModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final org.opendaylight.controller.config.yang.netconf.topology.NetconfTopologyModule oldModule,
            final AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
        this.getClientDispatcherDependency();
    }

    @Override
    public AutoCloseable createInstance() {
        return new NetconfTopologyImpl(getTopologyId(), getClientDispatcherDependency(),
                getBindingRegistryDependency(), getDomRegistryDependency(), getEventExecutorDependency(),
                getKeepaliveExecutorDependency(), getProcessingExecutorDependency(),
                getSharedSchemaRepositoryDependency());
    }

}

