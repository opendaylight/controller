/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.internal.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;

/**
 * The behavior of a RaftActor in the Follower state
 *
 * <ul>
 * <li> Respond to RPCs from candidates and leaders
 * <li> If election timeout elapses without receiving AppendEntries
 * RPC from current leader or granting vote to candidate:
 * convert to candidate
 * </ul>
 *
 */
public class Follower extends AbstractRaftActorBehavior {
    public Follower(RaftActorContext context) {
        super(context);

        scheduleElection(electionDuration());
    }

    @Override protected RaftState handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries, RaftState suggestedState) {
        return suggestedState;
    }

    @Override protected RaftState handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply, RaftState suggestedState) {
        return suggestedState;
    }

    @Override protected RaftState handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply, RaftState suggestedState) {
        return suggestedState;
    }

    @Override protected RaftState state() {
        return RaftState.Follower;
    }

    @Override public RaftState handleMessage(ActorRef sender, Object message) {
        if(message instanceof ElectionTimeout){
            return RaftState.Candidate;
        }

        scheduleElection(electionDuration());

        return super.handleMessage(sender, message);
    }
}
