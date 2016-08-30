/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import java.util.function.Function;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * {@link AbstractDataStoreClientBehavior} which performs module-based sharding.
 *
 * @author Robert Varga
 */
final class DistributedDataStoreClientBehavior extends AbstractDataStoreClientBehavior {
    private final Function<YangInstanceIdentifier, Long> pathToShard;

    private DistributedDataStoreClientBehavior(final ClientActorContext context,
            final ModuleShardBackendResolver resolver) {
        super(context, resolver);
        pathToShard = resolver::resolveShardForPath;
    }

    DistributedDataStoreClientBehavior(final ClientActorContext context, final ActorContext actorContext) {
        this(context, new ModuleShardBackendResolver(context.getIdentifier(), actorContext));
    }

    @Override
    Long resolveShardForPath(final YangInstanceIdentifier path) {
        return pathToShard.apply(path);
    }
}
