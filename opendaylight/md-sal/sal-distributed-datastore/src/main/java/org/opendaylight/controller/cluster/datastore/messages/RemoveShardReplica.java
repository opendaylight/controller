/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import javax.annotation.Nonnull;
import com.google.common.base.Preconditions;

/**
 * A message sent to the ShardManager to dynamically remove a local shard
 *  replica available in this node.
 */

public class RemoveShardReplica {

    private final String shardName;

    /**
     * Constructor.
     *
     * @param shardName name of the local shard that is to be dynamically removed.
     */

    public RemoveShardReplica (@Nonnull String shardName){
        this.shardName = Preconditions.checkNotNull(shardName, "ShardName should not be null");
    }

    public String getShardName(){
        return this.shardName;
    }

    @Override
    public String toString(){
        return "RemoveShardReplica[ShardName=" + shardName + "]";
    }
}
