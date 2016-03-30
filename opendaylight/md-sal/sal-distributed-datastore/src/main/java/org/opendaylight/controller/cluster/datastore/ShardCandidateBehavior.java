/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.RaftState;

final class ShardCandidateBehavior extends ShardBehavior {
    ShardCandidateBehavior(final Shard shard) {
        super(shard);
    }

    @Override
    ShardCandidateBehavior becomeCandidate() {
        return this;
    }

    @Override
    ShardFollowerBehavior becomeFollower() {
        return new ShardFollowerBehavior(getShard());
    }


    @Override
    ShardLeaderBehavior becomeIsolatedLeader() {
        return new ShardLeaderBehavior(getShard(), RaftState.IsolatedLeader);
    }

    @Override
    ShardLeaderBehavior becomeLeader() {
        return new ShardLeaderBehavior(getShard(), RaftState.Leader);
    }

    @Override
    void handleCreateTransaction(ActorRef sender, Object message) {
        // FIXME: what should we do here?
        throw new UnsupportedOperationException("Not implemented");
    }
}
