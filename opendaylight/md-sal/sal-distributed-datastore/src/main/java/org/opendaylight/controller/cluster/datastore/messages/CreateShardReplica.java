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
 * A message sent to the ShardManager to dynamically create a new local shard
 *  that is a replica for an existing shard that is already available in the
 *  cluster.
 */

public class CreateShardReplica {

    private String shardName;

    /**
     * Constructor.
     *
     * @param shardName name of the shard that is to be locally replicated.
     */

    public CreateShardReplica (@Nonnull String shardName){
        Preconditions.checkNotNull(shardName, "ShardName should not be null in CreateShardReplica");
        this.shardName = shardName;
    }

    public String getShardName(){
        return this.shardName;
    }

    @Override
    public String toString(){
        return "CreateShardReplica[ShardName=" + shardName + "]";
    }
}
