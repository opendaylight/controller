/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Persisted data of local shards list
 */

public class ShardPersistData implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<ShardReplica> localShardList;

    public ShardPersistData(@Nonnull List<ShardReplica> shardList) {
        localShardList = Preconditions.checkNotNull(shardList);
    }

    public List<ShardReplica> getLocalShardList() {
        return this.localShardList;
    }

    @Override
    public String toString() {
        return "LocalShardsData [ShardList = " + localShardList + " ]";
    }

    public static enum ShardState {
        Initialing,
        Initialized
    };

    public static class ShardReplica implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String shardName;
        private ShardState shardState;

        public ShardReplica(String shardName, ShardState shardState) {
            this.shardName = shardName;
            this.shardState = shardState;
        }

        public String getShardName() {
            return this.shardName;
        }

        public void setShardState(ShardState newShardState) {
            this.shardState = newShardState;
        }

        public ShardState getShardState() {
            return this.shardState;
        }

        public String toString() {
            return "ShardReplica [shardName = "+ shardName + ", shardState = " + shardState +"]";
        }
    }
}
