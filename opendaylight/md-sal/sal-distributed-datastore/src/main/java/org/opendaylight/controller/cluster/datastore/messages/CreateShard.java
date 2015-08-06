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
import org.opendaylight.controller.cluster.datastore.ShardCreator;

/**
 * A message sent to the ShardManager to dynamically create a new shard.
 *
 * @author Thomas Pantelis
 */
public class CreateShard {
    private final String shardName;
    private final ShardCreator shardCreator;

    /**
     * Constructor.
     *
     * Note that the ShardManager takes ownership of ShardCreator and will set fields appropriately. The
     * peerAddresses is required to be set by the caller. The datastoreContext will be set by the ShardManager
     * if not set by the caller. The remaining fields are always set by the ShardManager.
     *
     * @param shardName the name of the new shard.
     * @param shardCreator the ShardCreator.
     */
    public CreateShard(@Nonnull String shardName, @Nonnull ShardCreator shardCreator) {
        this.shardName = Preconditions.checkNotNull(shardName);
        this.shardCreator = Preconditions.checkNotNull(shardCreator);
    }

    @Nonnull public String getShardName() {
        return shardName;
    }

    @Nonnull public ShardCreator getShardCreator() {
        return shardCreator;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AddShard [shardName=").append(shardName).append("]");
        return builder.toString();
    }
}
