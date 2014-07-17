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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The behavior of a RaftActor when it is in the CandidateState
 * <p/>
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

    public Candidate(RaftActorContext context) {
        super(context);

        Collection<String> peerPaths = context.getPeerAddresses().values();

        for (String peerPath : peerPaths) {
            peerToActor.put(peerPath,
                context.actorSelection(peerPath));
        }

        context.getLogger().debug("Election:Candidate has following peers:"+peerToActor.keySet());
        if(peerPaths.size() > 0) {
            // Votes are required from a majority of the peers including self.
            // The votesRequired field therefore stores a calculated value
            // of the number of votes required for this candidate to win an
            // election based on it's known peers.
            // If a peer was added during normal operation and raft replicas
            // came to know about them then the new peer would also need to be
            // taken into consideration when calculating this value.
            // Here are some examples for what the votesRequired would be for n
            // peers
            // 0 peers = 1 votesRequired (0 + 1) / 2 + 1 = 1
            // 2 peers = 2 votesRequired (2 + 1) / 2 + 1 = 2
            // 4 peers = 3 votesRequired (4 + 1) / 2 + 1 = 3
            int noOfPeers = peerPaths.size();
            int self = 1;
            votesRequired = (noOfPeers + self) / 2 + 1;
        } else {
            votesRequired = 0;
        }

        startNewTerm();
        scheduleElection(electionDuration());
    }

    @Override protected RaftState handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries, RaftState suggestedState) {

        context.getLogger().error("An unexpected AppendEntries received in state " + state());

        return suggestedState;
    }

    @Override protected RaftState handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply, RaftState suggestedState) {

        // Some peer thinks I was a leader and sent me a reply

        return suggestedState;
    }

    @Override protected RaftState handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply, RaftState suggestedState) {
        if (suggestedState == RaftState.Follower) {
            // If base class thinks I should be follower then I am
            return suggestedState;
        }

        if (requestVoteReply.isVoteGranted()) {
            voteCount++;
        }

        if (voteCount >= votesRequired) {
            return RaftState.Leader;
        }

        return state();
    }

    @Override public RaftState state() {
        return RaftState.Candidate;
    }

    @Override
    public RaftState handleMessage(ActorRef sender, Object message) {
        if (message instanceof ElectionTimeout) {
            if (votesRequired == 0) {
                // If there are no peers then we should be a Leader
                // We wait for the election timeout to occur before declare
                // ourselves the leader. This gives enough time for a leader
                // who we do not know about (as a peer)
                // to send a message to the candidate
                return RaftState.Leader;
            }
            startNewTerm();
            scheduleElection(electionDuration());
            return state();
        }
        return super.handleMessage(sender, message);
    }


    private void startNewTerm() {


        // set voteCount back to 1 (that is voting for self)
        voteCount = 1;

        // Increment the election term and vote for self
        long currentTerm = context.getTermInformation().getCurrentTerm();
        context.getTermInformation().update(currentTerm + 1, context.getId());

        context.getLogger().debug("Starting new term " + (currentTerm+1));

        // Request for a vote
        for (ActorSelection peerActor : peerToActor.values()) {
            peerActor.tell(new RequestVote(
                    context.getTermInformation().getCurrentTerm(),
                    context.getId(),
                    context.getReplicatedLog().lastIndex(),
                    context.getReplicatedLog().lastTerm()),
                context.getActor()
            );
        }


    }

    @Override public void close() throws Exception {
        stopElection();
    }
}
