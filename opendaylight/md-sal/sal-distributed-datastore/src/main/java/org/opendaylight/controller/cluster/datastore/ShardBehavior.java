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

abstract class ShardBehavior extends ForwardingRaftActorBehavior {
    private final RaftActorBehavior delegate;

    // TODO: this is a temporary bridge to access information not available via behaviors
    private final Shard shard;

    ShardBehavior(final Shard shard, final RaftActorBehavior delegate) {
        this.shard = Preconditions.checkNotNull(shard);
        this.delegate = Preconditions.checkNotNull(delegate);
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
        if (delegate != raftBehavior) {
            // FIXME: change behavior
            return null;
            } else {
            return this;
        }
    }

    abstract void handleCreateTransaction(final ActorRef sender, final Object message);



}
