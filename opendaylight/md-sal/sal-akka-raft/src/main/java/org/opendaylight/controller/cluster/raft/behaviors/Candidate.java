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
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;

import java.util.Set;

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

    private int voteCount;

    private final int votesRequired;

    private final Set<String> peers;

    public Candidate(RaftActorContext context) {
        super(context);

        peers = context.getPeerAddresses().keySet();

        context.getLogger().debug("Election:Candidate has following peers:"+ peers);

        if(peers.size() > 0) {
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
            int noOfPeers = peers.size();
            int self = 1;
            votesRequired = (noOfPeers + self) / 2 + 1;
        } else {
            votesRequired = 0;
        }

        startNewTerm();
        scheduleElection(electionDuration());
    }

    @Override protected RaftState handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries) {

        context.getLogger().debug(appendEntries.toString());

        return state();
    }

    @Override protected RaftState handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply) {

        return state();
    }

    @Override protected RaftState handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply) {

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
    public RaftState handleMessage(ActorRef sender, Object originalMessage) {

        Object message = fromSerializableMessage(originalMessage);

        if (message instanceof RaftRPC) {

            RaftRPC rpc = (RaftRPC) message;

            context.getLogger().debug("RaftRPC message received {} my term is {}", rpc.toString(), context.getTermInformation().getCurrentTerm());

            // If RPC request or response contains term T > currentTerm:
            // set currentTerm = T, convert to follower (§5.1)
            // This applies to all RPC messages and responses
            if (rpc.getTerm() > context.getTermInformation().getCurrentTerm()) {
                context.getTermInformation().updateAndPersist(rpc.getTerm(), null);
                return RaftState.Follower;
            }
        }

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
        context.getTermInformation().updateAndPersist(currentTerm + 1,
            context.getId());

        context.getLogger().debug("Starting new term " + (currentTerm + 1));

        // Request for a vote
        // TODO: Retry request for vote if replies do not arrive in a reasonable
        // amount of time TBD
        for (String peerId : peers) {
            ActorSelection peerActor = context.getPeerActorSelection(peerId);
            if(peerActor != null) {
                peerActor.tell(new RequestVote(
                        context.getTermInformation().getCurrentTerm(),
                        context.getId(),
                        context.getReplicatedLog().lastIndex(),
                        context.getReplicatedLog().lastTerm()),
                    context.getActor()
                );
            }
        }


    }

    @Override public void close() throws Exception {
        stopElection();
    }
}
