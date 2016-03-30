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
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ShardFollowerBehavior extends AbstractForwardingShardBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(ShardFollowerBehavior.class);

    ShardFollowerBehavior(final Shard shard, final RaftActorBehavior delegate) {
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
