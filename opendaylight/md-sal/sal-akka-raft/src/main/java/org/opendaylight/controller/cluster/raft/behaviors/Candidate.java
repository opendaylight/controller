/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import org.apache.pekko.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.PeerInfo;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The behavior of a RaftActor when it is in the Candidate raft state.
 *
 * <p>Candidates (§5.2):
 * <ul>
 * <li> On conversion to candidate, start election:
 * <ul>
 * <li> Increment currentTerm
 * <li> Vote for self
 * <li> Reset election timer
 * <li> Send RequestVote RPCs to all other servers
 * </ul>
 * <li> If votes received from majority of servers: become leader
 * <li> If AppendEntries RPC received from new leader: convert to follower
 * <li> If election timeout elapses: start new election
 * </ul>
 */
public final class Candidate extends RaftActorBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(Candidate.class);

    private final ImmutableList<String> votingPeers;
    private final int votesRequired;

    private int voteCount;

    Candidate(final RaftActorContext context) {
        super(context, RaftState.Candidate);

        votingPeers = context.getPeers().stream()
            .filter(PeerInfo::isVoting)
            .map(PeerInfo::getId)
            .collect(ImmutableList.toImmutableList());

        LOG.debug("{}: Election: Candidate has following voting peers: {}", logName, votingPeers);

        votesRequired = getMajorityVoteCount(votingPeers.size());

        startNewTerm();

        if (votingPeers.isEmpty()) {
            actor().tell(ElectionTimeout.INSTANCE, actor());
        } else {
            scheduleElection(electionDuration());
        }
    }

    @Override
    public String getLeaderId() {
        return null;
    }

    @Override
    public short getLeaderPayloadVersion() {
        return -1;
    }

    @Override
    RaftActorBehavior handleAppendEntries(final ActorRef sender, final AppendEntries appendEntries) {
        LOG.debug("{}: handleAppendEntries: {}", logName, appendEntries);

        // Some other candidate for the same term became a leader and sent us an append entry
        if (currentTerm() == appendEntries.getTerm()) {
            LOG.info("{}: New Leader {} sent an AppendEntries to Candidate for term {} - will switch to Follower",
                    logName, appendEntries.getLeaderId(), currentTerm());

            return switchBehavior(new Follower(context));
        }

        return this;
    }

    @Override
    RaftActorBehavior handleAppendEntriesReply(final ActorRef sender, final AppendEntriesReply appendEntriesReply) {
        return this;
    }

    @Override
    RaftActorBehavior handleRequestVoteReply(final ActorRef sender, final RequestVoteReply requestVoteReply) {
        LOG.debug("{}: handleRequestVoteReply: {}, current voteCount: {}", logName, requestVoteReply, voteCount);

        if (requestVoteReply.isVoteGranted()) {
            voteCount++;
        }

        if (voteCount < votesRequired) {
            return this;
        }

        final var replLog = replicatedLog();
        final var lastApplied = replLog.getLastApplied();
        final var lastIndex = replLog.lastIndex();
        if (lastApplied >= lastIndex) {
            return switchBehavior(new Leader(context));
        }

        LOG.info("{}: LastApplied index {} is behind last index {} - switching to PreLeader", logName, lastApplied,
            lastIndex);
        return switchBehavior(new PreLeader(context));
    }

    @Override
    Duration electionDuration() {
        return super.electionDuration().dividedBy(context.getConfigParams().getCandidateElectionTimeoutDivisor());
    }


    @Override
    ApplyState getApplyStateFor(final ReplicatedLogEntry entry) {
        throw new IllegalStateException("A candidate should never attempt to apply " + entry);
    }

    @Override
    public RaftActorBehavior handleMessage(final ActorRef sender, final Object message) {
        if (message instanceof ElectionTimeout) {
            LOG.debug("{}: Received ElectionTimeout", logName);

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

        if (message instanceof RaftRPC rpc) {
            final var currentTerm = context.currentTerm();
            LOG.debug("{}: RaftRPC message received {}, my term is {}", logName, rpc, currentTerm);

            // If RPC request or response contains term T > currentTerm:
            // set currentTerm = T, convert to follower (§5.1)
            // This applies to all RPC messages and responses
            final var rpcTerm = rpc.getTerm();
            if (rpcTerm > currentTerm) {
                LOG.info("{}: Term {} in \"{}\" message is greater than Candidate's term {} - switching to Follower",
                        logName, rpcTerm, rpc, currentTerm);

                try {
                    context.persistTermInfo(new TermInfo(rpcTerm, null));
                } catch (IOException e) {
                    // FIXME: do not mask IOException
                    throw new UncheckedIOException(e);
                }

                // The raft paper does not say whether or not a Candidate can/should process a RequestVote in
                // this case but doing so gains quicker convergence when the sender's log is more up-to-date.
                if (message instanceof RequestVote) {
                    super.handleMessage(sender, message);
                }

                return switchBehavior(new Follower(context));
            }
        }

        return super.handleMessage(sender, message);
    }

    private void startNewTerm() {
        // set voteCount back to 1 (that is voting for self)
        voteCount = 1;

        // Increment the election term and vote for self
        final long currentTerm = context.currentTerm();
        final long newTerm = currentTerm + 1;
        try {
            // note: also updates current
            context.persistTermInfo(new TermInfo(newTerm, memberId()));
        } catch (IOException e) {
            // FIXME: do not mask IOException
            throw new UncheckedIOException(e);
        }

        LOG.info("{}: Starting new election term {}", logName, newTerm);

        // Request for a vote
        // TODO: Retry request for vote if replies do not arrive in a reasonable
        // amount of time TBD
        for (var peerId : votingPeers) {
            final var peerActor = context.getPeerActorSelection(peerId);
            if (peerActor != null) {
                final var replLog = replicatedLog();
                final var requestVote = new RequestVote(newTerm, memberId(), replLog.lastIndex(), replLog.lastTerm());

                LOG.debug("{}: Sending {} to peer {}", logName, requestVote, peerId);

                peerActor.tell(requestVote, context.getActor());
            }
        }
    }

    @Override
    public void close() {
        stopElection();
    }
}
