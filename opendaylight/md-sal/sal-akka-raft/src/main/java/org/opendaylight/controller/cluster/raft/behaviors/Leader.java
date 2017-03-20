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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorLeadershipTransferCohort;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;

/**
 * The behavior of a RaftActor when it is in the Leader state.
 *
 * <p>
 * Leaders:
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
public class Leader extends AbstractLeader {
    /**
     * Internal message sent to periodically check if this leader has become isolated and should transition
     * to {@link IsolatedLeader}.
     */
    @VisibleForTesting
    static final Object ISOLATED_LEADER_CHECK = new Object();

    private final Stopwatch isolatedLeaderCheck = Stopwatch.createStarted();
    @Nullable private LeadershipTransferContext leadershipTransferContext;

    Leader(RaftActorContext context, @Nullable AbstractLeader initializeFromLeader) {
        super(context, RaftState.Leader, initializeFromLeader);
    }

    public Leader(RaftActorContext context) {
        this(context, null);
    }

    @Override
    public RaftActorBehavior handleMessage(ActorRef sender, Object originalMessage) {
        Preconditions.checkNotNull(sender, "sender should not be null");

        if (ISOLATED_LEADER_CHECK.equals(originalMessage)) {
            if (isLeaderIsolated()) {
                log.warn("{}: At least {} followers need to be active, Switching {} from Leader to IsolatedLeader",
                    context.getId(), getMinIsolatedLeaderPeerCount(), getLeaderId());
                return internalSwitchBehavior(new IsolatedLeader(context, this));
            } else {
                return this;
            }
        } else {
            return super.handleMessage(sender, originalMessage);
        }
    }

    @Override
    protected void beforeSendHeartbeat() {
        if (isolatedLeaderCheck.elapsed(TimeUnit.MILLISECONDS)
                > context.getConfigParams().getIsolatedCheckIntervalInMillis()) {
            context.getActor().tell(ISOLATED_LEADER_CHECK, context.getActor());
            isolatedLeaderCheck.reset().start();
        }

        if (leadershipTransferContext != null && leadershipTransferContext.isExpired(
                context.getConfigParams().getElectionTimeOutInterval().toMillis())) {
            log.debug("{}: Leadership transfer expired", logName());
            leadershipTransferContext = null;
        }
    }

    @Override
    protected RaftActorBehavior handleAppendEntriesReply(ActorRef sender, AppendEntriesReply appendEntriesReply) {
        RaftActorBehavior returnBehavior = super.handleAppendEntriesReply(sender, appendEntriesReply);
        tryToCompleteLeadershipTransfer(appendEntriesReply.getFollowerId());
        return returnBehavior;
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
    public void transferLeadership(@Nonnull RaftActorLeadershipTransferCohort leadershipTransferCohort) {
        log.debug("{}: Attempting to transfer leadership", logName());

        leadershipTransferContext = new LeadershipTransferContext(leadershipTransferCohort);

        // Send an immediate heart beat to the followers.
        sendAppendEntries(0, false);
    }

    private void tryToCompleteLeadershipTransfer(String followerId) {
        if (leadershipTransferContext == null) {
            return;
        }

        final Optional<String> requestedFollowerIdOptional
                = leadershipTransferContext.transferCohort.getRequestedFollowerId();
        if (requestedFollowerIdOptional.isPresent() && !requestedFollowerIdOptional.get().equals(followerId)) {
            // we want to transfer leadership to specific follower
            return;
        }

        FollowerLogInformation followerInfo = getFollower(followerId);
        if (followerInfo == null) {
            return;
        }

        long lastIndex = context.getReplicatedLog().lastIndex();
        boolean isVoting = context.getPeerInfo(followerId).isVoting();

        log.debug("{}: tryToCompleteLeadershipTransfer: followerId: {}, matchIndex: {}, lastIndex: {}, isVoting: {}",
                logName(), followerId, followerInfo.getMatchIndex(), lastIndex, isVoting);

        if (isVoting && followerInfo.getMatchIndex() == lastIndex) {
            log.debug("{}: Follower's log matches - sending ElectionTimeout", logName());

            // We can't be sure if the follower has applied all its log entries to its state so send an
            // additional AppendEntries with the latest commit index.
            sendAppendEntries(0, false);

            // Now send a TimeoutNow message to the matching follower to immediately start an election.
            ActorSelection followerActor = context.getPeerActorSelection(followerId);
            followerActor.tell(TimeoutNow.INSTANCE, context.getActor());

            log.debug("{}: Leader transfer complete", logName());

            leadershipTransferContext.transferCohort.transferComplete();
            leadershipTransferContext = null;
        }
    }

    @Override
    public void close() {
        if (leadershipTransferContext != null) {
            LeadershipTransferContext localLeadershipTransferContext = leadershipTransferContext;
            leadershipTransferContext = null;
            localLeadershipTransferContext.transferCohort.abortTransfer();
        }

        super.close();
    }

    @VisibleForTesting
    void markFollowerActive(String followerId) {
        getFollower(followerId).markFollowerActive();
    }

    @VisibleForTesting
    void markFollowerInActive(String followerId) {
        getFollower(followerId).markFollowerInActive();
    }

    private static class LeadershipTransferContext {
        RaftActorLeadershipTransferCohort transferCohort;
        Stopwatch timer = Stopwatch.createStarted();

        LeadershipTransferContext(RaftActorLeadershipTransferCohort transferCohort) {
            this.transferCohort = transferCohort;
        }

        boolean isExpired(long timeout) {
            if (timer.elapsed(TimeUnit.MILLISECONDS) >= timeout) {
                transferCohort.abortTransfer();
                return true;
            }

            return false;
        }
    }
}
