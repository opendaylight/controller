/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The RaftActorContext contains that portion of the RaftActors state that
 * needs to be shared with it's behaviors. A RaftActorContext should NEVER be
 * used in any actor context outside the RaftActor that constructed it.
 */
public interface RaftActorContext {
    /**
     * Create a new local actor
      * @param props
     * @return
     */
    ActorRef actorOf(Props props);

    /**
     * Create a actor selection
     * @param path
     * @return
     */
    ActorSelection actorSelection(String path);

    /**
     * Get the identifier for the RaftActor. This identifier represents the
     * name of the actor whose common state is being shared. For example the
     * id could be 'inventory'
     * @return the identifier
     */
    String getId();

    /**
     * A reference to the RaftActor itself. This could be used to send messages
     * to the RaftActor
     * @return
     */
    ActorRef getActor();

    /**
     * Get the ElectionTerm information
     * @return
     */
    ElectionTerm getTermInformation();

    /**
     * index of highest log entry known to be
     * committed (initialized to 0, increases
     *    monotonically)
     * @return
     */
    AtomicLong getCommitIndex();

    /**
     * index of highest log entry applied to state
     * machine (initialized to 0, increases
     *    monotonically)
     * @return
     */
    AtomicLong getLastApplied();

    /**
     *
     */
    ReplicatedLog getReplicatedLog();
}
