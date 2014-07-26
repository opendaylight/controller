/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.persistence.UntypedEventsourcedProcessor;
import org.opendaylight.controller.cluster.raft.behaviors.Candidate;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RaftActor encapsulates a state machine that needs to be kept synchronized
 * in a cluster. It implements the RAFT algorithm as described in the paper
 * <a href='https://ramcloud.stanford.edu/wiki/download/attachments/11370504/raft.pdf'>
 *     In Search of an Understandable Consensus Algorithm</a>
 * <p>
 * RaftActor has 3 states and each state has a certain behavior associated
 * with it. A Raft actor can behave as,
 * <ul>
 *     <li> A Leader </li>
 *     <li> A Follower (or) </li>
 *     <li> A Candidate </li>
 * </ul>
 *
 * <p>
 * A RaftActor MUST be a Leader in order to accept requests from clients to
 * change the state of it's encapsulated state machine. Once a RaftActor becomes
 * a Leader it is also responsible for ensuring that all followers ultimately
 * have the same log and therefore the same state machine as itself.
 *
 * <p>
 * The current behavior of a RaftActor determines how election for leadership
 * is initiated and how peer RaftActors react to request for votes.
 *
 * <p>
 * Each RaftActor also needs to know the current election term. It uses this
 * information for a couple of things. One is to simply figure out who it
 * voted for in the last election. Another is to figure out if the message
 * it received to update it's state is stale.
 *
 * <p>
 * The RaftActor uses akka-persistence to store it's replicated log.
 * Furthermore through it's behaviors a Raft Actor determines
 *
 * <ul>
 * <li> when a log entry should be persisted </li>
 * <li> when a log entry should be applied to the state machine (and) </li>
 * <li> when a snapshot should be saved </li>
 * </ul>
 *
 * <a href="http://doc.akka.io/api/akka/2.3.3/index.html#akka.persistence.UntypedEventsourcedProcessor">UntypeEventSourceProcessor</a>
 */
public abstract class RaftActor extends UntypedEventsourcedProcessor {

    /**
     *  The current state determines the current behavior of a RaftActor
     * A Raft Actor always starts off in the Follower State
     */
    private RaftActorBehavior currentBehavior;

    /**
     * This context should NOT be passed directly to any other actor it is
     * only to be consumed by the RaftActorBehaviors
     */
    private RaftActorContext context;

    public RaftActor(String id){
        context = new RaftActorContextImpl(this.getSelf(),
            this.getContext(),
            id, new ElectionTermImpl(id),
            new AtomicLong(0), new AtomicLong(0), new ReplicatedLogImpl());
        currentBehavior = switchBehavior(RaftState.Follower);
    }

    @Override public void onReceiveRecover(Object message) {
        throw new UnsupportedOperationException("onReceiveRecover");
    }

    @Override public void onReceiveCommand(Object message) {
        RaftState state = currentBehavior.handleMessage(getSender(), message);
        currentBehavior = switchBehavior(state);
    }

    private RaftActorBehavior switchBehavior(RaftState state){
        RaftActorBehavior behavior = null;
        if(state == RaftState.Candidate){
            behavior = new Candidate(context, Collections.EMPTY_LIST);
        } else if(state == RaftState.Follower){
            behavior = new Follower(context);
        } else {
            behavior = new Leader(context, Collections.EMPTY_LIST);
        }
        return behavior;
    }

    private class ReplicatedLogImpl implements ReplicatedLog {

        @Override public ReplicatedLogEntry getReplicatedLogEntry(long index) {
            throw new UnsupportedOperationException("getReplicatedLogEntry");
        }

        @Override public ReplicatedLogEntry last() {
            throw new UnsupportedOperationException("last");
        }
    }
}
