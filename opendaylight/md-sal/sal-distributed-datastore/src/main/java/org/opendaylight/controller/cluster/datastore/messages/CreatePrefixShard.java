/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Serializable;
import org.opendaylight.controller.cluster.datastore.PrefixShardConfiguration;

/**
 * Sent to the local {@link org.opendaylight.controller.cluster.datastore.ShardedDataTreeActor} when there was a shard created
 * on the local node. The local actor should notify the remote actors with {@link PrefixShardCreated} which should
 * create the required frontend/backend shards.
 */
public class CreatePrefixShard implements Serializable {

    private final PrefixShardConfiguration configuration;

    public CreatePrefixShard(final PrefixShardConfiguration configuration) {

        this.configuration = configuration;
    }

    public PrefixShardConfiguration getConfiguration() {
        return configuration;
    }
}
