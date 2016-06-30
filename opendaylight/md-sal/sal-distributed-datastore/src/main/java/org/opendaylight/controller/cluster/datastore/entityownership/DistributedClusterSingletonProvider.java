/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership;

import com.google.common.base.Preconditions;
import org.opendaylight.mdsal.binding.api.clustering.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.binding.api.clustering.ClusterSingletonServiceProviderImpl;
import org.opendaylight.mdsal.binding.api.clustering.EntityOwnershipChange;
import org.opendaylight.mdsal.binding.api.clustering.EntityOwnershipService;
import org.opendaylight.mdsal.common.api.clustering.ClusterSingletonService;
import org.opendaylight.mdsal.common.api.clustering.ClusterSingletonServiceRegistration;

/**
 * Blueprint activator for {@link ClusterSingletonServiceProvider} OSGi service
 */
public class DistributedClusterSingletonProvider implements ClusterSingletonServiceProvider {

    private ClusterSingletonServiceProvider delegator;

    /**
     * Method is defined for bluepring OSGi service activator
     *
     * @param entityOwnershipService - DOMEos
     *
     * @return {@link ClusterSingletonServiceProvider} OSGi service
     */
    public static DistributedClusterSingletonProvider start(final EntityOwnershipService entityOwnershipService) {
        return new DistributedClusterSingletonProvider(entityOwnershipService);
    }

    private DistributedClusterSingletonProvider(final EntityOwnershipService entityOwnershipService) {
        Preconditions.checkArgument(entityOwnershipService != null, "EntityOwnershipService can not be null!");
        this.delegator = new ClusterSingletonServiceProviderImpl(entityOwnershipService);
    }

    @Override
    public ClusterSingletonServiceRegistration registerClusterSingletonService(final ClusterSingletonService service) {
        return delegator.registerClusterSingletonService(service);
    }

    @Override
    public void close() throws Exception {
        if (delegator != null) {
            delegator.close();
            delegator = null;
        }
    }

    @Override
    public void ownershipChanged(final EntityOwnershipChange ownershipChange) {
        // NOOP : this instance is not registered to EOS as listener
    }

}
