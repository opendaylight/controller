/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import java.util.stream.Stream;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * {@link AbstractDataStoreClientBehavior} which performs module-based sharding.
 *
 * @author Robert Varga
 */
final class DistributedDataStoreClientBehavior extends AbstractDataStoreClientBehavior {
    private final ModuleShardBackendResolver resolver;

    private DistributedDataStoreClientBehavior(final ClientActorContext context,
            final ModuleShardBackendResolver resolver) {
        super(context, resolver);
        this.resolver = resolver;
    }

    DistributedDataStoreClientBehavior(final ClientActorContext context, final ActorUtils actorUtils) {
        this(context, new ModuleShardBackendResolver(context.getIdentifier(), actorUtils));
    }

    @Override
    Long resolveShardForPath(final YangInstanceIdentifier path) {
        return resolver.resolveShardForPath(path);
    }

    @Override
    Stream<Long> resolveAllShards() {
        return resolver.resolveAllShards();
    }

    @Override
    public void close() {
        super.close();
        resolver().close();
    }
}
