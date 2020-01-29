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
import java.util.ArrayList;
import java.util.Collection;
import org.opendaylight.controller.cluster.raft.PeerInfo;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import scala.concurrent.duration.FiniteDuration;

/**
 * The behavior of a RaftActor when it is in the Candidate raft state.
 *
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

    private int voteCount;

    private final int votesRequired;

    private final Collection<String> votingPeers = new ArrayList<>();

    public Candidate(RaftActorContext context) {
        super(context, RaftState.Candidate);

        for (PeerInfo peer: context.getPeers()) {
            if (peer.isVoting()) {
                votingPeers.add(peer.getId());
            }
        }

        log.debug("{}: Election: Candidate has following voting peers: {}", logName(), votingPeers);

        votesRequired = getMajorityVoteCount(votingPeers.size());

        startNewTerm();

        if (votingPeers.isEmpty()) {
            actor().tell(ElectionTimeout.INSTANCE, actor());
        } else {
            scheduleElection(electionDuration());
        }
    }

    @Override
    public final String getLeaderId() {
        return null;
    }

    @Override
    public final short getLeaderPayloadVersion() {
        return -1;
    }

    @Override
    protected RaftActorBehavior handleAppendEntries(ActorRef sender, AppendEntries appendEntries) {

        log.debug("{}: handleAppendEntries: {}", logName(), appendEntries);

        // Some other candidate for the same term became a leader and sent us an append entry
        if (currentTerm() == appendEntries.getTerm()) {
            log.info("{}: New Leader {} sent an AppendEntries to Candidate for term {} - will switch to Follower",
                    logName(), appendEntries.getLeaderId(), currentTerm());

            return switchBehavior(new Follower(context));
        }

        return this;
    }

    @Override
    protected RaftActorBehavior handleAppendEntriesReply(ActorRef sender, AppendEntriesReply appendEntriesReply) {
        return this;
    }

    @Override
    protected RaftActorBehavior handleRequestVoteReply(ActorRef sender, RequestVoteReply requestVoteReply) {
        log.debug("{}: handleRequestVoteReply: {}, current voteCount: {}", logName(), requestVoteReply, voteCount);

        if (requestVoteReply.isVoteGranted()) {
            voteCount++;
        }

        if (voteCount >= votesRequired) {
            if (context.getLastApplied() < context.getReplicatedLog().lastIndex()) {
                log.info("{}: LastApplied index {} is behind last index {} - switching to PreLeader",
                        logName(), context.getLastApplied(), context.getReplicatedLog().lastIndex());
                return internalSwitchBehavior(RaftState.PreLeader);
            } else {
                return internalSwitchBehavior(RaftState.Leader);
            }
        }

        return this;
    }

    @Override
    protected FiniteDuration electionDuration() {
        return super.electionDuration().$div(context.getConfigParams().getCandidateElectionTimeoutDivisor());
    }

    @Override
    public RaftActorBehavior handleMessage(ActorRef sender, Object message) {
        if (message instanceof ElectionTimeout) {
            log.debug("{}: Received ElectionTimeout", logName());

            if (votesRequired == 0) {
                // If there are no peers then we should be a Leader
                // We wait for the election timeout to occur before declare
                // ourselves the leader. This gives enough time for a leader
                // who we do not know about (as a peer)
                // to send a message to the candidate

                return internalSwitchBehavior(RaftState.Leader);
            }

            startNewTerm();
            scheduleElection(electionDuration());
            return this;
        }

        if (message instanceof RaftRPC) {

            RaftRPC rpc = (RaftRPC) message;

            log.debug("{}: RaftRPC message received {}, my term is {}", logName(), rpc,
                        context.getTermInformation().getCurrentTerm());

            // If RPC request or response contains term T > currentTerm:
            // set currentTerm = T, convert to follower (§5.1)
            // This applies to all RPC messages and responses
            if (rpc.getTerm() > context.getTermInformation().getCurrentTerm()) {
                log.info("{}: Term {} in \"{}\" message is greater than Candidate's term {} - switching to Follower",
                        logName(), rpc.getTerm(), rpc, context.getTermInformation().getCurrentTerm());

                context.getTermInformation().updateAndPersist(rpc.getTerm(), null);

                // The raft paper does not say whether or not a Candidate can/should process a RequestVote in
                // this case but doing so gains quicker convergence when the sender's log is more up-to-date.
                if (message instanceof RequestVote) {
                    super.handleMessage(sender, message);
                }

                return internalSwitchBehavior(RaftState.Follower);
            }
        }

        return super.handleMessage(sender, message);
    }


    private void startNewTerm() {


        // set voteCount back to 1 (that is voting for self)
        voteCount = 1;

        // Increment the election term and vote for self
        long currentTerm = context.getTermInformation().getCurrentTerm();
        long newTerm = currentTerm + 1;
        context.getTermInformation().updateAndPersist(newTerm, context.getId());

        log.info("{}: Starting new election term {}", logName(), newTerm);

        // Request for a vote
        // TODO: Retry request for vote if replies do not arrive in a reasonable
        // amount of time TBD
        for (String peerId : votingPeers) {
            ActorSelection peerActor = context.getPeerActorSelection(peerId);
            if (peerActor != null) {
                RequestVote requestVote = new RequestVote(
                        context.getTermInformation().getCurrentTerm(),
                        context.getId(),
                        context.getReplicatedLog().lastIndex(),
                        context.getReplicatedLog().lastTerm());

                log.debug("{}: Sending {} to peer {}", logName(), requestVote, peerId);

                peerActor.tell(requestVote, context.getActor());
            }
        }
    }

    @Override
    public void close() {
        stopElection();
    }
}
