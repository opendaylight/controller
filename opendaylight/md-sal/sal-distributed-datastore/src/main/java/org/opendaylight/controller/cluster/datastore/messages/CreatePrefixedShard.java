/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.config.PrefixShardConfiguration;

/**
 * Message sent to the ShardManager to create a shard located at a certain logical position in the dataTree
 */
public class CreatePrefixedShard {

    private final PrefixShardConfiguration config;
    private final DatastoreContext context;
    private final Shard.Builder builder;

    public CreatePrefixedShard(final PrefixShardConfiguration config, final DatastoreContext context, final Shard.Builder builder) {
        this.config = config;
        this.context = context;
        this.builder = builder;
    }

    public PrefixShardConfiguration getConfig() {
        return config;
    }

    public DatastoreContext getContext() {
        return context;
    }

    public Shard.Builder getShardBuilder() {
        return builder;
    }
}
