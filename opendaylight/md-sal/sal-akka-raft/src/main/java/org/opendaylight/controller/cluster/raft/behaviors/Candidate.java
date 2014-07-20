/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.internal.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The behavior of a RaftActor when it is in the CandidateState
 * <p>
 * Candidates (§5.2):
 * <ul>
 * <li> On conversion to candidate, start election:
 * <ul>
 * <li> Increment currentTerm
 * <li> Vote for self
 * <li> Reset election timer
 * <li> Send RequestVote RPCs to all other servers
 * </ul>
 * <li> If votes received from majority of servers: become leader
 * <li> If AppendEntries RPC received from new leader: convert to
 * follower
 * <li> If election timeout elapses: start new election
 * </ul>
 */
public class Candidate extends AbstractRaftActorBehavior {

    private final Map<String, ActorSelection> peerToActor = new HashMap<>();

    private int voteCount;

    private final int votesRequired;

    public Candidate(RaftActorContext context, List<String> peerPaths) {
        super(context);

        for (String peerPath : peerPaths) {
            peerToActor.put(peerPath,
                context.actorSelection(peerPath));
        }

        if(peerPaths.size() > 0) {
            votesRequired = (peerPaths.size() + 1) / 2 + 1;
        } else {
            votesRequired = 0;
        }

        startNewTerm();
        scheduleElection(electionDuration());
    }

    @Override protected RaftState handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries, RaftState suggestedState) {

        // There is some peer who thinks it's a leader but is not
        // I will not accept this append entries
        sender.tell(new AppendEntriesReply(
            context.getTermInformation().getCurrentTerm(), false),
            context.getActor());

        return suggestedState;
    }

    @Override protected RaftState handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply, RaftState suggestedState) {

        // Some peer thinks I was a leader and sent me a reply

        return suggestedState;
    }

    @Override protected RaftState handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply, RaftState suggestedState) {
        if(suggestedState == RaftState.Follower) {
            // If base class thinks I should be follower then I am
            return suggestedState;
        }

        if(requestVoteReply.isVoteGranted()){
            voteCount++;
        }

        if(voteCount >= votesRequired){
            return RaftState.Leader;
        }

        return state();
    }

    @Override protected RaftState state() {
        return RaftState.Candidate;
    }

    @Override
    public RaftState handleMessage(ActorRef sender, Object message) {
        if(message instanceof ElectionTimeout){
            if(votesRequired == 0){
                // If there are no peers then we should be a Leader
                // FIXME : Do I really need to wait for the Election to timeout to consider myself a leader?
                return RaftState.Leader;
            }
            startNewTerm();
            scheduleElection(electionDuration());
            return state();
        }
        return super.handleMessage(sender, message);
    }


    private void startNewTerm(){
        // set voteCount back to 1 (that is voting for self)
        voteCount = 1;

        // Increment the election term and vote for self
        long currentTerm = context.getTermInformation().getCurrentTerm();
        context.getTermInformation().update(currentTerm+1, context.getId());

        // Request for a vote
        for(ActorSelection peerActor : peerToActor.values()){
            peerActor.tell(new RequestVote(
                    context.getTermInformation().getCurrentTerm(),
                    context.getId(), context.getReplicatedLog().last().getIndex(),
                    context.getReplicatedLog().last().getTerm()),
                context.getActor());
        }


    }

}
