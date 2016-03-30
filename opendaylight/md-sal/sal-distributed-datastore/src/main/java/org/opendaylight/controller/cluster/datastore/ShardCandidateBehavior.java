/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;

final class ShardCandidateBehavior extends AbstractForwardingShardBehavior {
    ShardCandidateBehavior(Shard shard, RaftActorBehavior delegate) {
        super(shard, delegate);
    }

    @Override
    ShardCandidateBehavior becomeCandidate(RaftActorBehavior raftBehavior) {
        return new ShardCandidateBehavior(getShard(), raftBehavior);
    }

    @Override
    ShardFollowerBehavior becomeFollower(final RaftActorBehavior raftBehavior) {
        return new ShardFollowerBehavior(getShard(), raftBehavior);
    }

    @Override
    ShardLeaderBehavior becomeLeader(final RaftActorBehavior raftBehavior) {
        return new ShardLeaderBehavior(getShard(), raftBehavior);
    }

    @Override
    void handleCreateTransaction(ActorRef sender, Object message) {
        // FIXME: what should we do here?
        throw new UnsupportedOperationException("Not implemented");
    }
}
