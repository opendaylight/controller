package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;

/**
 * The FindPrimary message is used to locate the primary of any given shard
 *
 * TODO : Make this serializable
 */
public class FindPrimary{
    private final String shardName;

    public FindPrimary(String shardName){

        Preconditions.checkNotNull(shardName, "shardName should not be null");

        this.shardName = shardName;
    }

    public String getShardName() {
        return shardName;
    }
}
