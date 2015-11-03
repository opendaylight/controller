/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import javax.annotation.Nullable;

/**
 * Stores a snapshot of the internal state of a data store.
 *
 * @author Thomas Pantelis
 */
public class DatastoreSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String type;
    private final byte[] shardManagerSnapshot;
    private final List<ShardSnapshot> shardSnapshots;

    public DatastoreSnapshot(@Nonnull String type, @Nullable byte[] shardManagerSnapshot,
            @Nonnull List<ShardSnapshot> shardSnapshots) {
        this.type = Preconditions.checkNotNull(type);
        this.shardManagerSnapshot = shardManagerSnapshot;
        this.shardSnapshots = Preconditions.checkNotNull(shardSnapshots);
    }

    @Nonnull
    public String getType() {
        return type;
    }

    @Nullable
    public byte[] getShardManagerSnapshot() {
        return shardManagerSnapshot;
    }

    @Nonnull
    public List<ShardSnapshot> getShardSnapshots() {
        return shardSnapshots;
    }

    public static class ShardSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final byte[] snapshot;

        public ShardSnapshot(@Nonnull String name, @Nonnull byte[] snapshot) {
            this.name = Preconditions.checkNotNull(name);
            this.snapshot = Preconditions.checkNotNull(snapshot);
        }

        @Nonnull
        public String getName() {
            return name;
        }

        @Nonnull
        public byte[] getSnapshot() {
            return snapshot;
        }
    }
}
