/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;

public class DistributedDataStoreClientBehaviorTest extends AbstractDataStoreClientBehaviorTest {
    @Override
    protected AbstractDataStoreClientBehavior createBehavior(final ClientActorContext clientContext,
                                                             final ActorContext context) {
        final ShardStrategyFactory factory = mock(ShardStrategyFactory.class);
        final ShardStrategy strategy = mock(ShardStrategy.class);
        when(strategy.findShard(any())).thenReturn(SHARD);
        when(factory.getStrategy(any())).thenReturn(strategy);
        when(context.getShardStrategyFactory()).thenReturn(factory);
        return new DistributedDataStoreClientBehavior(clientContext, context);
    }
}
