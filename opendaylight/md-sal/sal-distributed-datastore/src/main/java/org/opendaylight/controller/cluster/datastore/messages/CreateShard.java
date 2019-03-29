/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.Shard.AbstractBuilder;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;

/**
 * A message sent to the ShardManager to dynamically create a new shard.
 *
 * @author Thomas Pantelis
 */
public class CreateShard {
    private final ModuleShardConfiguration moduleShardConfig;
    private final Shard.AbstractBuilder<?, ?> shardBuilder;
    private final DatastoreContext datastoreContext;

    /**
     * Constructor.
     *
     * @param moduleShardConfig the configuration of the new shard.
     * @param shardBuilder used to obtain the Props for creating the shard actor instance.
     * @param datastoreContext the DatastoreContext for the new shard. If null, the default is used.
     */
    public CreateShard(@NonNull ModuleShardConfiguration moduleShardConfig, @NonNull AbstractBuilder<?, ?> shardBuilder,
            @Nullable DatastoreContext datastoreContext) {
        this.moduleShardConfig = requireNonNull(moduleShardConfig);
        this.shardBuilder = requireNonNull(shardBuilder);
        this.datastoreContext = datastoreContext;
    }

    public @NonNull ModuleShardConfiguration getModuleShardConfig() {
        return moduleShardConfig;
    }

    public @NonNull AbstractBuilder<?, ?> getShardBuilder() {
        return shardBuilder;
    }

    public @Nullable DatastoreContext getDatastoreContext() {
        return datastoreContext;
    }

    @Override
    public String toString() {
        return "CreateShard [moduleShardConfig=" + moduleShardConfig + ", shardPropsCreator=" + shardBuilder + "]";
    }
}
