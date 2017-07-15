/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.base.messages.LeaderTransitioning;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * A raft actor support class that participates in leadership transfer. An instance is created upon
 * initialization of leadership transfer.
 *
 * <p>
 * The transfer process is as follows:
 * <ol>
 * <li>Send a LeaderStateChanged message with a null leader Id to the local RoleChangeNotifier to notify
 *     clients that we no longer have a working leader.</li>
 * <li>Send a LeaderTransitioning message to each follower so each can send LeaderStateChanged messages to
 *     their local RoleChangeNotifiers.</li>
 * <li>Call {@link RaftActor#pauseLeader} passing this RaftActorLeadershipTransferCohort
 *     instance. This allows derived classes to perform work prior to transferring leadership.</li>
 * <li>When the pause is complete, the run method is called which in turn calls
 *     {@link Leader#transferLeadership(RaftActorLeadershipTransferCohort)}.</li>
 * <li>The Leader calls {@link #transferComplete} on successful completion.</li>
 * <li>Wait a short period of time for the new leader to be elected to give the derived class a chance to
 *     possibly complete work that was suspended while we were transferring.</li>
 * <li>On notification of the new leader from the RaftActor or on time out, notify {@link OnComplete} callbacks.</li>
 * </ol>
 *
 * <p>
 * NOTE: All methods on this class must be called on the actor's thread dispatcher as they may access/modify
 * internal state.
 *
 * @author Thomas Pantelis
 */
public class RaftActorLeadershipTransferCohort {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorLeadershipTransferCohort.class);

    static final long USE_DEFAULT_LEADER_TIMEOUT = -1;

    private final List<OnComplete> onCompleteCallbacks = new ArrayList<>();
    private final Stopwatch transferTimer = Stopwatch.createUnstarted();
    private final RaftActor raftActor;
    private final String requestedFollowerId;

    private long newLeaderTimeoutInMillis = 2000;
    private Cancellable newLeaderTimer;
    private boolean isTransferring;

    RaftActorLeadershipTransferCohort(final RaftActor raftActor) {
        this(raftActor, null);
    }

    RaftActorLeadershipTransferCohort(final RaftActor raftActor, @Nullable final String requestedFollowerId) {
        this.raftActor = raftActor;
        this.requestedFollowerId = requestedFollowerId;

        // We'll wait an election timeout period for a new leader to be elected plus some cushion to take into
        // account the variance.
        final long electionTimeout = raftActor.getRaftActorContext().getConfigParams()
                .getElectionTimeOutInterval().toMillis();
        final int variance = raftActor.getRaftActorContext().getConfigParams().getElectionTimeVariance();
        newLeaderTimeoutInMillis = 2 * (electionTimeout + variance);
    }

    void init() {
        RaftActorContext context = raftActor.getRaftActorContext();
        RaftActorBehavior currentBehavior = raftActor.getCurrentBehavior();

        transferTimer.start();

        Optional<ActorRef> roleChangeNotifier = raftActor.getRoleChangeNotifier();
        if (roleChangeNotifier.isPresent()) {
            roleChangeNotifier.get().tell(raftActor.newLeaderStateChanged(context.getId(), null,
                    currentBehavior.getLeaderPayloadVersion()), raftActor.self());
        }

        for (String peerId: context.getPeerIds()) {
            ActorSelection followerActor = context.getPeerActorSelection(peerId);
            if (followerActor != null) {
                followerActor.tell(new LeaderTransitioning(context.getId()), context.getActor());
            }
        }

        raftActor.pauseLeader(new TimedRunnable(context.getConfigParams().getElectionTimeOutInterval(), raftActor) {
            @Override
            protected void doRun() {
                LOG.debug("{}: pauseLeader successfully completed - doing transfer", raftActor.persistenceId());
                doTransfer();
            }

            @Override
            protected void doCancel() {
                LOG.debug("{}: pauseLeader timed out - continuing with transfer", raftActor.persistenceId());
                doTransfer();
            }
        });
    }

    /**
     * This method is invoked to perform the leadership transfer.
     */
    @VisibleForTesting
    void doTransfer() {
        RaftActorBehavior behavior = raftActor.getCurrentBehavior();
        // Sanity check...
        if (behavior instanceof Leader) {
            isTransferring = true;
            ((Leader)behavior).transferLeadership(this);
        } else {
            LOG.debug("{}: No longer the leader - skipping transfer", raftActor.persistenceId());
            finish(true);
        }
    }

    /**
     * This method is invoked to abort leadership transfer on failure.
     */
    public void abortTransfer() {
        LOG.debug("{}: leader transfer aborted", raftActor.persistenceId());
        finish(false);
    }

    /**
     * This method is invoked when leadership transfer was carried out and complete.
     */
    public void transferComplete() {
        LOG.debug("{}: leader transfer complete - waiting for new leader", raftActor.persistenceId());

        // We'll give it a little time for the new leader to be elected to give the derived class a
        // chance to possibly complete work that was suspended while we were transferring. The
        // RequestVote message from the new leader candidate should cause us to step down as leader
        // and convert to follower due to higher term. We should then get an AppendEntries heart
        // beat with the new leader id.

        // Add a timer in case we don't get a leader change. Note: the Runnable is sent as a message to the raftActor
        // which executes it safely run on the actor's thread dispatcher.
        FiniteDuration timeout = FiniteDuration.create(newLeaderTimeoutInMillis, TimeUnit.MILLISECONDS);
        newLeaderTimer = raftActor.getContext().system().scheduler().scheduleOnce(timeout, raftActor.self(),
            (Runnable) () -> {
                LOG.debug("{}: leader not elected in time", raftActor.persistenceId());
                finish(true);
            }, raftActor.getContext().system().dispatcher(), raftActor.self());
    }

    void onNewLeader(final String newLeader) {
        if (newLeader != null && newLeaderTimer != null) {
            LOG.debug("{}: leader changed to {}", raftActor.persistenceId(), newLeader);
            newLeaderTimer.cancel();
            finish(true);
        }
    }

    private void finish(final boolean success) {
        isTransferring = false;
        if (transferTimer.isRunning()) {
            transferTimer.stop();
            if (success) {
                LOG.info("{}: Successfully transferred leadership to {} in {}", raftActor.persistenceId(),
                        raftActor.getLeaderId(), transferTimer);
            } else {
                LOG.warn("{}: Failed to transfer leadership in {}", raftActor.persistenceId(), transferTimer);
                raftActor.unpauseLeader();
            }
        }

        for (OnComplete onComplete: onCompleteCallbacks) {
            if (success) {
                onComplete.onSuccess(raftActor.self());
            } else {
                onComplete.onFailure(raftActor.self());
            }
        }
    }

    void addOnComplete(final OnComplete onComplete) {
        onCompleteCallbacks.add(onComplete);
    }

    boolean isTransferring() {
        return isTransferring;
    }

    void setNewLeaderTimeoutInMillis(final long newLeaderTimeoutInMillis) {
        if (newLeaderTimeoutInMillis != USE_DEFAULT_LEADER_TIMEOUT) {
            this.newLeaderTimeoutInMillis = newLeaderTimeoutInMillis;
        }
    }

    public Optional<String> getRequestedFollowerId() {
        return Optional.fromNullable(requestedFollowerId);
    }

    interface OnComplete {
        void onSuccess(ActorRef raftActorRef);

        void onFailure(ActorRef raftActorRef);
    }
}
