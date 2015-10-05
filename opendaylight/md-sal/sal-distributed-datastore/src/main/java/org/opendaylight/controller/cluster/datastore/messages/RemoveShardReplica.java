/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import javax.annotation.Nonnull;

/**
 * A message sent to the ShardManager to dynamically remove a local shard
 *  replica available in this node.
 */

public class RemoveShardReplica {

    private String shardName;

    /**
     * Constructor.
     *
     * @param shardName name of the local shard that is to be dynamically removed.
     */

    public RemoveShardReplica (@Nonnull String shardName){
        this.shardName = shardName;
    }

    public String getShardName(){
        return this.shardName;
    }

    @Override
    public String toString(){
        String str = "RemoveShardReplica[ShardName=" + shardName + "]";
        return str;
    }
}

