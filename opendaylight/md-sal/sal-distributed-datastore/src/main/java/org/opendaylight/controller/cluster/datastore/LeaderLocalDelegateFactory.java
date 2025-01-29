/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.ActorPath;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.Props;

/**
 * Base class for factories instantiating delegates which are local to the
 * shard leader.
 *
 * @param <D> delegate type
 * @param <M> message type
 */
abstract class LeaderLocalDelegateFactory<M> {
    private final Shard shard;

    protected LeaderLocalDelegateFactory(final Shard shard) {
        this.shard = requireNonNull(shard);
    }

    protected final ActorRef self() {
        return shard.self();
    }

    protected final Shard getShard() {
        return shard;
    }

    protected final String shardName() {
        return shard.memberId();
    }

    protected final void tellSender(final Object message) {
        shard.getSender().tell(message, self());
    }

    protected final ActorRef createActor(final Props props) {
        return shard.getContext().actorOf(props);
    }

    protected final ActorSelection selectActor(final ActorRef ref) {
        return shard.getContext().system().actorSelection(ref.path());
    }

    protected final ActorSelection selectActor(final ActorPath path) {
        return shard.getContext().system().actorSelection(path);
    }

    /**
     * Invoked whenever the local shard's leadership role changes.
     *
     * @param isLeader true if the shard has become leader, false if it has become a follower.
     * @param hasLeader true if the shard knows about leader ID
     */
    abstract void onLeadershipChange(boolean isLeader, boolean hasLeader);

    abstract void onMessage(M message, boolean isLeader, boolean hasLeader);
}
