/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.ShardPropsCreator;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;

/**
 * A message sent to the ShardManager to dynamically create a new shard.
 *
 * @author Thomas Pantelis
 */
public class CreateShard {
    private final ModuleShardConfiguration moduleShardConfig;
    private final ShardPropsCreator shardPropsCreator;
    private final DatastoreContext datastoreContext;

    /**
     * Constructor.
     *
     * @param moduleShardConfig the configuration of the new shard.
     * @param shardPropsCreator used to obtain the Props for creating the shard actor instance.
     * @param datastoreContext the DatastoreContext for the new shard. If null, the default is used.
     */
    public CreateShard(@Nonnull ModuleShardConfiguration moduleShardConfig,
            @Nonnull ShardPropsCreator shardPropsCreator, @Nullable DatastoreContext datastoreContext) {
        this.moduleShardConfig = Preconditions.checkNotNull(moduleShardConfig);
        this.shardPropsCreator = Preconditions.checkNotNull(shardPropsCreator);
        this.datastoreContext = datastoreContext;
    }

    @Nonnull public ModuleShardConfiguration getModuleShardConfig() {
        return moduleShardConfig;
    }

    @Nonnull public ShardPropsCreator getShardPropsCreator() {
        return shardPropsCreator;
    }

    @Nullable public DatastoreContext getDatastoreContext() {
        return datastoreContext;
    }

    @Override
    public String toString() {
        return "CreateShard [moduleShardConfig=" + moduleShardConfig + ", shardPropsCreator=" + shardPropsCreator + "]";
    }
}
