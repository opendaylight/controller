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
import java.util.Set;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;

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

        if(LOG.isDebugEnabled()) {
            LOG.debug("{}: Election: Candidate has following peers: {}", context.getId(), peers);
        }

        votesRequired = getMajorityVoteCount(peers.size());

        startNewTerm();
        scheduleElection(electionDuration());
    }

    @Override protected RaftActorBehavior handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries) {

        if(LOG.isDebugEnabled()) {
            LOG.debug("{}: handleAppendEntries: {}", context.getId(), appendEntries);
        }

        return this;
    }

    @Override protected RaftActorBehavior handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply) {

        return this;
    }

    @Override protected RaftActorBehavior handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply) {

        if (requestVoteReply.isVoteGranted()) {
            voteCount++;
        }

        if (voteCount >= votesRequired) {
            return switchBehavior(new Leader(context));
        }

        return this;
    }

    @Override public RaftState state() {
        return RaftState.Candidate;
    }

    @Override
    public RaftActorBehavior handleMessage(ActorRef sender, Object originalMessage) {

        Object message = fromSerializableMessage(originalMessage);

        if (message instanceof RaftRPC) {

            RaftRPC rpc = (RaftRPC) message;

            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: RaftRPC message received {} my term is {}", context.getId(), rpc,
                        context.getTermInformation().getCurrentTerm());
            }

            // If RPC request or response contains term T > currentTerm:
            // set currentTerm = T, convert to follower (§5.1)
            // This applies to all RPC messages and responses
            if (rpc.getTerm() > context.getTermInformation().getCurrentTerm()) {
                context.getTermInformation().updateAndPersist(rpc.getTerm(), null);

                return switchBehavior(new Follower(context));
            }
        }

        if (message instanceof ElectionTimeout) {
            if (votesRequired == 0) {
                // If there are no peers then we should be a Leader
                // We wait for the election timeout to occur before declare
                // ourselves the leader. This gives enough time for a leader
                // who we do not know about (as a peer)
                // to send a message to the candidate

                return switchBehavior(new Leader(context));
            }
            startNewTerm();
            scheduleElection(electionDuration());
            return this;
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

        if(LOG.isDebugEnabled()) {
            LOG.debug("{}: Starting new term {}", context.getId(), (currentTerm + 1));
        }

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
