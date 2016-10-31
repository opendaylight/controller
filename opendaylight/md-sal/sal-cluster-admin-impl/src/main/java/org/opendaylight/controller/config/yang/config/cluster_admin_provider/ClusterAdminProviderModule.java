/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.cluster_admin_provider;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.sal.common.util.NoopAutoCloseable;

public class ClusterAdminProviderModule extends AbstractClusterAdminProviderModule {
    public ClusterAdminProviderModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ClusterAdminProviderModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver,
            ClusterAdminProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public boolean canReuseInstance(AbstractClusterAdminProviderModule oldModule) {
        return true;
    }

    @Override
    public AutoCloseable createInstance() {
        // The ClusterAdminRpcService is created via blueprint so return a noop here for backwards compatibility.
        return NoopAutoCloseable.INSTANCE;
    }
}
