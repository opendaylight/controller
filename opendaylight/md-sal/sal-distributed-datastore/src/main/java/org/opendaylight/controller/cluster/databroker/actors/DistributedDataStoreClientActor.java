/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors;

import java.util.concurrent.CompletableFuture;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.datastore.actors.client.AbstractClientActor;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActorBehavior;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActorContext;

/**
 * A {@link AbstractClientActor} which acts as the point of contact for DistributedDataStore.
 */
final class DistributedDataStoreClientActor extends AbstractClientActor<DistributedDataStoreFrontend> {
    private final CompletableFuture<DistributedDataStoreClient> future;

    private DistributedDataStoreClientActor(final FrontendIdentifier<DistributedDataStoreFrontend> frontendId,
            final CompletableFuture<DistributedDataStoreClient> future) {
        super(frontendId);
        this.future = future;
    }

    @Override
    protected ClientActorBehavior<DistributedDataStoreFrontend> initialBehavior(
            final ClientActorContext<DistributedDataStoreFrontend> context) {
        final DistributedDataStoreClientBehavior behavior = new DistributedDataStoreClientBehavior(context);
        if (future != null) {
            future.complete(behavior);
        }
        return behavior;
    }
}
