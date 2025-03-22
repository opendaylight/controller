/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorLeadershipTransferCohort;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The behavior of a RaftActor when it is in the Leader state.
 *
 * <p>Leaders:
 * <ul>
 * <li> Upon election: send initial empty AppendEntries RPCs
 * (heartbeat) to each server; repeat during idle periods to
 * prevent election timeouts (§5.2)
 * <li> If command received from client: append entry to local log,
 * respond after entry applied to state machine (§5.3)
 * <li> If last log index ≥ nextIndex for a follower: send
 * AppendEntries RPC with log entries starting at nextIndex
 * <li> If successful: update nextIndex and matchIndex for
 * follower (§5.3)
 * <li> If AppendEntries fails because of log inconsistency:
 * decrement nextIndex and retry (§5.3)
 * <li> If there exists an N such that N &gt; commitIndex, a majority
 * of matchIndex[i] ≥ N, and log[N].term == currentTerm:
 * set commitIndex = N (§5.3, §5.4).
 * </ul>
 */
// Non-final for testing
public non-sealed class Leader extends AbstractLeader {
    private static final Logger LOG = LoggerFactory.getLogger(Leader.class);

    /**
     * Internal message sent to periodically check if this leader has become isolated and should transition
     * to {@link IsolatedLeader}.
     */
    @VisibleForTesting
    static final Object ISOLATED_LEADER_CHECK = new Object();

    private final Stopwatch isolatedLeaderCheck = Stopwatch.createStarted();
    private @Nullable LeadershipTransferContext leadershipTransferContext;

    Leader(final RaftActorContext context, final IsolatedLeader initializeFromLeader) {
        super(context, RaftState.Leader, requireNonNull(initializeFromLeader));
    }

    Leader(final RaftActorContext context, final PreLeader initializeFromLeader) {
        super(context, RaftState.Leader, requireNonNull(initializeFromLeader));
    }

    @VisibleForTesting
    public Leader(final RaftActorContext context) {
        super(context, RaftState.Leader);
    }

    @Override
    public RaftActorBehavior handleMessage(final ActorRef sender, final Object message) {
        requireNonNull(sender, "sender should not be null");
        return ISOLATED_LEADER_CHECK.equals(message) ? checkIsolatedLeader() : super.handleMessage(sender, message);
    }

    private @NonNull RaftActorBehavior checkIsolatedLeader() {
        if (!isLeaderIsolated()) {
            return this;
        }

        LOG.warn("{}: At least {} followers need to be active, Switching {} from Leader to IsolatedLeader", logName,
            getMinIsolatedLeaderPeerCount(), getLeaderId());
        return switchBehavior(new IsolatedLeader(context, this));
    }

    @Override
    final void beforeSendHeartbeat() {
        if (isolatedLeaderCheck.elapsed(TimeUnit.MILLISECONDS)
                > context.getConfigParams().getIsolatedCheckIntervalInMillis()) {
            context.getActor().tell(ISOLATED_LEADER_CHECK, context.getActor());
            isolatedLeaderCheck.reset().start();
        }

        if (leadershipTransferContext != null && leadershipTransferContext.isExpired(
                context.getConfigParams().getElectionTimeOutInterval().toMillis())) {
            LOG.debug("{}: Leadership transfer expired", logName);
            leadershipTransferContext = null;
        }
    }

    @Override
    final Leader handleAppendEntriesReply(final ActorRef sender, final AppendEntriesReply appendEntriesReply) {
        processAppendEntriesReply(sender, appendEntriesReply);
        tryToCompleteLeadershipTransfer(appendEntriesReply.getFollowerId());
        return this;
    }

    /**
     * Attempts to transfer leadership to a follower as per the raft paper (§3.10) as follows:
     * <ul>
     * <li>Start a timer (Stopwatch).</li>
     * <li>Send an initial AppendEntries heartbeat to all followers.</li>
     * <li>On AppendEntriesReply, check if the follower's new match Index matches the leader's last index</li>
     * <li>If it matches,
     *   <ul>
     *   <li>Send an additional AppendEntries to ensure the follower has applied all its log entries to its state.</li>
     *   <li>Send an ElectionTimeout to the follower to immediately start an election.</li>
     *   <li>Notify {@link RaftActorLeadershipTransferCohort#transferComplete}.</li>
     *   </ul></li>
     * <li>Otherwise if the election time out period elapses, notify
     *     {@link RaftActorLeadershipTransferCohort#abortTransfer}.</li>
     * </ul>
     *
     * @param leadershipTransferCohort the cohort participating in the leadership transfer
     */
    public void transferLeadership(@NonNull final RaftActorLeadershipTransferCohort leadershipTransferCohort) {
        LOG.debug("{}: Attempting to transfer leadership", logName);

        leadershipTransferContext = new LeadershipTransferContext(leadershipTransferCohort);

        // Send an immediate heart beat to the followers.
        sendAppendEntries(0, false);
    }

    private void tryToCompleteLeadershipTransfer(final String followerId) {
        if (leadershipTransferContext == null) {
            return;
        }

        final Optional<String> requestedFollowerIdOptional
                = leadershipTransferContext.transferCohort.getRequestedFollowerId();
        if (requestedFollowerIdOptional.isPresent() && !requestedFollowerIdOptional.orElseThrow().equals(followerId)) {
            // we want to transfer leadership to specific follower
            return;
        }

        FollowerLogInformation followerInfo = getFollower(followerId);
        if (followerInfo == null) {
            return;
        }

        long lastIndex = context.getReplicatedLog().lastIndex();
        boolean isVoting = context.getPeerInfo(followerId).isVoting();

        LOG.debug("{}: tryToCompleteLeadershipTransfer: followerId: {}, matchIndex: {}, lastIndex: {}, isVoting: {}",
                logName, followerId, followerInfo.getMatchIndex(), lastIndex, isVoting);

        if (isVoting && followerInfo.getMatchIndex() == lastIndex) {
            LOG.debug("{}: Follower's log matches - sending ElectionTimeout", logName);

            // We can't be sure if the follower has applied all its log entries to its state so send an
            // additional AppendEntries with the latest commit index.
            sendAppendEntries(0, false);

            // Now send a TimeoutNow message to the matching follower to immediately start an election.
            final var followerActor = context.getPeerActorSelection(followerId);
            followerActor.tell(TimeoutNow.INSTANCE, context.getActor());

            LOG.debug("{}: Leader transfer complete", logName);

            leadershipTransferContext.transferCohort.transferComplete();
            leadershipTransferContext = null;
        }
    }

    @Override
    public final void close() {
        if (leadershipTransferContext != null) {
            LeadershipTransferContext localLeadershipTransferContext = leadershipTransferContext;
            leadershipTransferContext = null;
            localLeadershipTransferContext.transferCohort.abortTransfer();
        }

        super.close();
    }

    @VisibleForTesting
    final void markFollowerActive(final String followerId) {
        getFollower(followerId).markFollowerActive();
    }

    @VisibleForTesting
    final void markFollowerInActive(final String followerId) {
        getFollower(followerId).markFollowerInActive();
    }

    private static class LeadershipTransferContext {
        RaftActorLeadershipTransferCohort transferCohort;
        Stopwatch timer = Stopwatch.createStarted();

        LeadershipTransferContext(final RaftActorLeadershipTransferCohort transferCohort) {
            this.transferCohort = transferCohort;
        }

        boolean isExpired(final long timeout) {
            if (timer.elapsed(TimeUnit.MILLISECONDS) >= timeout) {
                transferCohort.abortTransfer();
                return true;
            }

            return false;
        }
    }
}
