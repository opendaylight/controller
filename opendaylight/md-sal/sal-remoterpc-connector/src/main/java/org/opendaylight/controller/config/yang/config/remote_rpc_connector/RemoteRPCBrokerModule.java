/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.remote_rpc_connector;

import org.opendaylight.controller.sal.common.util.NoopAutoCloseable;

public class RemoteRPCBrokerModule extends org.opendaylight.controller.config.yang.config.remote_rpc_connector.AbstractRemoteRPCBrokerModule {
    public RemoteRPCBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public RemoteRPCBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.remote_rpc_connector.RemoteRPCBrokerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public boolean canReuseInstance(AbstractRemoteRPCBrokerModule oldModule) {
        return true;
    }

    @Override
    public AutoCloseable createInstance() {
        // The RemoteRpcProvider is created via blueprint and doesn't advertise any services so return a
        // no-op here for backwards compatibility.
        return NoopAutoCloseable.INSTANCE;
    }
}
