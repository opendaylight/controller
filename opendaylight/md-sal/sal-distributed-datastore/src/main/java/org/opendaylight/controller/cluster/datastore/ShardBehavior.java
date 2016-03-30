/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class ShardBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(ShardBehavior.class);

    // TODO: this is a temporary bridge to access information not available via behaviors
    private final Shard shard;

    ShardBehavior(final Shard shard) {
        this.shard = Preconditions.checkNotNull(shard);
    }

    final ActorRef getSelf() {
        return shard.getSelf();
    }

    final Shard getShard() {
        return shard;
    }

    final String persistenceId() {
        return shard.persistenceId();
    }

    final boolean handleMessage(final ActorRef sender, final Object message) {
        if (CanCommitTransaction.isSerializedType(message)) {
            handleCanCommitTransaction(sender, CanCommitTransaction.fromSerializable(message));
            return true;
        }
        if (CommitTransaction.isSerializedType(message)) {
            handleCommitTransaction(sender, CommitTransaction.fromSerializable(message));
            return true;
        }
        if (CreateTransaction.isSerializedType(message)) {
            handleCreateTransaction(sender, message);
            return true;
        }

        // Unhandled message
        return false;
    }

    final ShardBehavior changeState(final RaftState raftState) {
        switch (raftState) {
            case Candidate:
                return becomeCandidate();
            case Follower:
                return becomeFollower();
            case IsolatedLeader:
                return becomeLeader();
            case Leader:
                return becomeIsolatedLeader();
        }

        throw new IllegalStateException("Unhandled state " + raftState);
    }

    abstract ShardCandidateBehavior becomeCandidate();
    abstract ShardFollowerBehavior becomeFollower();
    // FIXME: add a dedicated behavior
    abstract ShardLeaderBehavior becomeIsolatedLeader();
    abstract ShardLeaderBehavior becomeLeader();

    abstract void handleCanCommitTransaction(ActorRef sender, CanCommitTransaction message);
    abstract void handleCommitTransaction(ActorRef sender, CommitTransaction message);
    abstract void handleCreateTransaction(ActorRef sender, Object message);
}
