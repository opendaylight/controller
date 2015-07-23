/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.RaftState;

/**
 * A RaftActorBehavior represents the specific behavior of a RaftActor
 * <p>
 * A RaftActor can behave as one of the following,
 * <ul>
 *     <li> Follower </li>
 *     <li> Candidate </li>
 *     <li> Leader </li>
 * </ul>
 * <p>
 * In each of these behaviors the Raft Actor handles the same Raft messages
 * differently.
 */
public interface RaftActorBehavior extends AutoCloseable{

    /**
     * Handle a message. If the processing of the message warrants a state
     * change then a new behavior should be returned otherwise this method should
     * return the current behavior.
     *
     * @param sender The sender of the message
     * @param message A message that needs to be processed
     *
     * @return The new behavior or current behavior
     */
    RaftActorBehavior handleMessage(ActorRef sender, Object message);

    /**
     *
     * @return The state associated with a given behavior
     */
    RaftState state();

    /**
     *
     * @return The Id of the Leader if known else null
     */
    String getLeaderId();

    /**
     * setting the index of the log entry which is replicated to all nodes
     * @param replicatedToAllIndex
     */
    void setReplicatedToAllIndex(long replicatedToAllIndex);

    /**
     * @return the index of the log entry which is replicated to all nodes
     */
    long getReplicatedToAllIndex();

    /**
     * @return the leader's payload data version.
     */
    short getLeaderPayloadVersion();

    /**
     * switchBehavior makes sure that the current behavior is shutdown before it switches to the new
     * behavior
     *
     * @param behavior The new behavior to switch to
     * @return The new behavior
     */
    RaftActorBehavior switchBehavior(RaftActorBehavior behavior);
}
