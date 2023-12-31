/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.RaftActorEvent.LeaderChanged;
import org.opendaylight.yangtools.yang.data.tree.api.ReadOnlyDataTree;

/**
 * A local message derived from LeaderStateChanged containing additional Shard-specific info that is sent
 * when some state of the shard leader has changed. This message is used by the ShardManager to maintain
 * current Shard information.
 *
 * @author Thomas Pantelis
 */
public final class ShardLeaderChanged extends LeaderChanged {
    private final @Nullable ReadOnlyDataTree localShardDataTree;

    public ShardLeaderChanged(final @NonNull ActorRef raftActor, final @NonNull String memberId,
            final @Nullable String leaderId, final @NonNull ReadOnlyDataTree localShardDataTree,
            final short leaderPayloadVersion) {
        super(raftActor, memberId, leaderId, leaderPayloadVersion);
        this.localShardDataTree = requireNonNull(localShardDataTree);
    }

    public ShardLeaderChanged(final @NonNull ActorRef raftActor, final @NonNull String memberId,
            final @Nullable String leaderId, final short leaderPayloadVersion) {
        super(raftActor, memberId, leaderId, leaderPayloadVersion);
        localShardDataTree = null;
    }

    public @Nullable ReadOnlyDataTree localShardDataTree() {
        return localShardDataTree;
    }
}
