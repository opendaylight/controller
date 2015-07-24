/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import com.google.common.base.Preconditions;

/**
 * Base class for factories instantiating delegates which are local to the
 * shard leader.
 *
 * <D> delegate type
 * <M> message type
 * <I> initial state type
 */
abstract class LeaderLocalDelegateFactory<M, D, I> extends DelegateFactory<M, D, I> {
    private final Shard shard;

    protected LeaderLocalDelegateFactory(final Shard shard) {
        this.shard = Preconditions.checkNotNull(shard);
    }

    protected final ActorRef getSelf() {
        return shard.getSelf();
    }

    protected final Shard getShard() {
        return shard;
    }

    protected final String persistenceId() {
        return shard.persistenceId();
    }

    protected final void tellSender(final Object message) {
        shard.getSender().tell(message, getSelf());
    }

    protected final ActorRef createActor(final Props props) {
        return shard.getContext().actorOf(props);
    }

    protected final ActorSelection selectActor(ActorRef ref) {
        return shard.getContext().system().actorSelection(ref.path());
    }

    protected final ActorSelection selectActor(ActorPath path) {
        return shard.getContext().system().actorSelection(path);
    }

    /**
     * Invoked whenever the local shard's leadership role changes.
     *
     * @param isLeader true if the shard has become leader, false if it has
 *                 become a follower.
     * @param hasLeader true if the shard knows about leader ID
     */
    abstract void onLeadershipChange(boolean isLeader, boolean hasLeader);
    abstract void onMessage(M message, boolean isLeader, boolean hasLeader);
}
