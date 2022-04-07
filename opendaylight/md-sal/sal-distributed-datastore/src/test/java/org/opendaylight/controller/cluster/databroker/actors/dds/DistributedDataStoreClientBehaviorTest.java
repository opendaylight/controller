/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Set;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;

public class DistributedDataStoreClientBehaviorTest extends AbstractDataStoreClientBehaviorTest {
    @Override
    protected AbstractDataStoreClientBehavior createBehavior(final ClientActorContext clientContext,
                                                             final ActorUtils context) {
        final ShardStrategy strategy = mock(ShardStrategy.class);
        doReturn(SHARD).when(strategy).findShard(any());
        final ShardStrategyFactory factory = mock(ShardStrategyFactory.class);
        doReturn(strategy).when(factory).getStrategy(any());
        doReturn(factory).when(context).getShardStrategyFactory();

        final Configuration config = mock(Configuration.class);
        doReturn(Set.of(SHARD)).when(config).getAllShardNames();
        doReturn(config).when(context).getConfiguration();

        return new DistributedDataStoreClientBehavior(clientContext, context);
    }
}
