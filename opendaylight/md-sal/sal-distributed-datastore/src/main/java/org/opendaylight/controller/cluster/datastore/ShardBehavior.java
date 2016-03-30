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
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.raft.behaviors.ForwardingRaftActorBehavior;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class ShardBehavior extends ForwardingRaftActorBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(ShardBehavior.class);
    private final RaftActorBehavior delegate;

    // TODO: this is a temporary bridge to access information not available via behaviors
    private final Shard shard;

    ShardBehavior(final Shard shard, final RaftActorBehavior delegate) {
        this.shard = Preconditions.checkNotNull(shard);
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    static ShardBehavior forRaftBehavior(final Shard shard, final RaftActorBehavior raftBehavior) {
        if (raftBehavior instanceof ShardBehavior) {
            return (ShardBehavior) raftBehavior;
        }

        LOG.debug("Wrapping RAFT behavior {} for shard use", raftBehavior);
        switch (raftBehavior.state()) {
            case Candidate:
                // FIXME: not implemented yet
                break;
            case Follower:
                return new ShardFollowerBehavior(shard, raftBehavior);
            case IsolatedLeader:
                // FIXME: not implemented yet
                break;
            case Leader:
                return new ShardLeaderBehavior(shard, raftBehavior);
        }

        throw new IllegalStateException("Unhandled behavior " + raftBehavior);
    }

    @Override
    protected final RaftActorBehavior delegate() {
        return delegate;
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

    @Override
    public final ShardBehavior handleMessage(final ActorRef sender, final Object message) {
        if (CreateTransaction.isSerializedType(message)) {
            handleCreateTransaction(sender, message);
            return this;
        }

        final RaftActorBehavior raftBehavior = super.handleMessage(sender, message);
        if (delegate == raftBehavior) {
            return this;
        }

        switch (raftBehavior.state()) {
            case Candidate:
                // FIXME: not implemented yet
                break;
            case Follower:
                return becomeFollower(raftBehavior);
            case IsolatedLeader:
                // FIXME: not implemented yet
                break;
            case Leader:
                return becomeLeader(raftBehavior);
        }

        throw new IllegalStateException("Unhandled behavior " + raftBehavior);
    }

    abstract ShardFollowerBehavior becomeFollower(RaftActorBehavior raftBehavior);
    abstract ShardLeaderBehavior becomeLeader(RaftActorBehavior raftBehavior);

    abstract void handleCreateTransaction(final ActorRef sender, final Object message);
}
