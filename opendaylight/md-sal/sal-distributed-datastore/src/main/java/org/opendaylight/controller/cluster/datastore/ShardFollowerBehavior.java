/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Status;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ShardFollowerBehavior extends AbstractForwardingShardBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(ShardFollowerBehavior.class);

    ShardFollowerBehavior(final Shard shard) {
        super(shard);
    }

    @Override
    ShardCandidateBehavior becomeCandidate() {
        return new ShardCandidateBehavior(getShard());
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
    void handleCreateTransaction(final ActorRef sender, final Object message) {
        final ActorSelection leader = getShard().getLeader();

        if (leader != null) {
            leader.forward(message, getShard().getContext());
        } else {
            sender.tell(new Status.Failure(
                new NoShardLeaderException("Could not create a shard transaction", persistenceId())), getSelf());
        }
    }

}
