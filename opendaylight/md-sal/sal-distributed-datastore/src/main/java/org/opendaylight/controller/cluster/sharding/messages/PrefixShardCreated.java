/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding.messages;

import org.opendaylight.controller.cluster.datastore.config.PrefixShardConfiguration;
import org.opendaylight.controller.cluster.sharding.ShardedDataTreeActor;

/**
 * Message sent to the local {@link ShardedDataTreeActor} when a clustered
 * shard was created locally. The backend shards/replicas will be handled by the ShardManager but the
 * {@link ShardedDataTreeActor} needs to handle the registration of the
 * frontends into the {@link org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService}. The configuration only contains
 * the Member nodes that this is still yet to be distributed to. The last node will receive PrefixShardConfiguration
 * with only it's member present.
 */
public class PrefixShardCreated {
    private final PrefixShardConfiguration configuration;

    public PrefixShardCreated(final PrefixShardConfiguration configuration) {
        this.configuration = configuration;
    }

    public PrefixShardConfiguration getConfiguration() {
        return configuration;
    }
}
