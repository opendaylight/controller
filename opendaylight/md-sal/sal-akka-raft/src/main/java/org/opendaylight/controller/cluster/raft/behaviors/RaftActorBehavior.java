/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.RaftState;

/**
 * The interface for a class that implements a specific behavior of a RaftActor. The types of behaviors are enumerated
 * by {@link RaftState}. Each handles the same Raft messages differently.
 */
public interface RaftActorBehavior extends AutoCloseable {

    /**
     * Handle a message. If the processing of the message warrants a state
     * change then a new behavior should be returned otherwise this method should
     * return the current behavior.
     *
     * @param sender The sender of the message
     * @param message A message that needs to be processed
     *
     * @return The new behavior or current behavior, or null if the message was not handled.
     */
    @Nullable
    RaftActorBehavior handleMessage(ActorRef sender, Object message);

    /**
     * Returns the state associated with this behavior.
     *
     * @return the RaftState
     */
    RaftState state();

    /**
     * Returns the id of the leader.
     *
     * @return the id of the leader or null if not known
     */
    @Nullable
    String getLeaderId();

    /**
     * Sets the index of the last log entry that has been replicated to all peers.
     *
     * @param replicatedToAllIndex the index
     */
    void setReplicatedToAllIndex(long replicatedToAllIndex);

    /**
     * Returns the index of the last log entry that has been replicated to all peers.
     *
     * @return the index or -1 if not known
     */
    long getReplicatedToAllIndex();

    /**
     * Returns the leader's payload data version.
     *
     * @return a short representing the version
     */
    short getLeaderPayloadVersion();

    /**
     * Closes the current behavior and switches to the specified behavior, if possible.
     *
     * @param behavior the new behavior to switch to
     * @return the new behavior
     */
    RaftActorBehavior switchBehavior(RaftActorBehavior behavior);

    @Override
    void close();
}
