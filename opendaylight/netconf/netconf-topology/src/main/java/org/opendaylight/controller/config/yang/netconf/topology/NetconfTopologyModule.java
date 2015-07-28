/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.netconf.topology;

import io.netty.util.concurrent.EventExecutor;
import org.opendaylight.controller.config.threadpool.ScheduledThreadPool;
import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.topology.NetconfTopology;
import org.opendaylight.controller.netconf.topology.SchemaRepositoryProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.core.api.Broker;

public class NetconfTopologyModule extends org.opendaylight.controller.config.yang.netconf.topology.AbstractNetconfTopologyModule {
    public NetconfTopologyModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfTopologyModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.netconf.topology.NetconfTopologyModule oldModule, AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public AutoCloseable createInstance() {
        return new NetconfTopologyCloseable(this);
    }

    private static class NetconfTopologyCloseable implements NetconfTopology, AutoCloseable {

        private final NetconfTopologyModule module;

        public NetconfTopologyCloseable(NetconfTopologyModule module) {
            this.module = module;
        }

        @Override
        public void close() throws Exception {
            //NOOP
        }

        @Override
        public NetconfClientDispatcher getNetconfClientDispatcherDependency() {
            return module.getClientDispatcherDependency();
        }

        @Override
        public BindingAwareBroker getBindingAwareBroker() {
            return module.getBindingRegistryDependency();
        }

        @Override
        public Broker getDomRegistryDependency() {
            return module.getDomRegistryDependency();
        }

        @Override
        public EventExecutor getEventExecutorDependency() {
            return module.getEventExecutorDependency();
        }

        @Override
        public ScheduledThreadPool getKeepaliveExecutorDependency() {
            return module.getKeepaliveExecutorDependency();
        }

        @Override
        public ThreadPool getProcessingExecutorDependency() {
            return module.getProcessingExecutorDependency();
        }

        @Override
        public SchemaRepositoryProvider getSharedSchemaRepository() {
            return module.getSharedSchemaRepositoryDependency();
        }
    }

}
