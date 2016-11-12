/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * {@link AbstractDataStoreClientBehavior} which connects to a single shard only.
 *
 * @author Robert Varga
 */
final class SimpleDataStoreClientBehavior extends AbstractDataStoreClientBehavior {
    // Pre-boxed instance
    private static final Long ZERO = Long.valueOf(0);

    private SimpleDataStoreClientBehavior(final ClientActorContext context,
            final SimpleShardBackendResolver resolver) {
        super(context, resolver);
    }

    SimpleDataStoreClientBehavior(final ClientActorContext context, final ActorContext actorContext,
            final String shardName) {
        this(context, new SimpleShardBackendResolver(context.getIdentifier(), actorContext, shardName));
    }

    @Override
    Long resolveShardForPath(final YangInstanceIdentifier path) {
        return ZERO;
    }
}
