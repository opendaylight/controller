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
import org.opendaylight.controller.cluster.raft.base.messages.LeaderTransitioning;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * A raft actor support class that participates in leadership transfer. An instance is created upon
 * initialization of leadership transfer.
 * <p>
 * The transfer process is as follows:
 * <ol>
 * <li>Send a LeaderStateChanged message with a null leader Id to the local RoleChangeNotifier to notify
 *     clients that we no longer have a working leader.</li>
 * <li>Send a LeaderTransitioning message to each follower so each can send LeaderStateChanged messages to
 *     their local RoleChangeNotifiers.</li>
 * <li>Call {@link RaftActor#pauseLeader} passing this RaftActorLeadershipTransferCohort
 *     instance. This allows derived classes to perform work prior to transferring leadership.</li>
 * <li>When the pause is complete, the {@link #run} method is called which in turn calls
 *     {@link Leader#transferLeadership}.</li>
 * <li>The Leader calls {@link #transferComplete} on successful completion.</li>
 * <li>Wait a short period of time for the new leader to be elected to give the derived class a chance to
 *     possibly complete work that was suspended while we were transferring.</li>
 * <li>On notification of the new leader from the RaftActor or on time out, notify {@link OnComplete} callbacks.</li>
 * </ol>
 * <p>
 * NOTE: All methods on this class must be called on the actor's thread dispatcher as they may access/modify
 * internal state.
 *
 * @author Thomas Pantelis
 */
public class RaftActorLeadershipTransferCohort {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorLeadershipTransferCohort.class);

    private final RaftActor raftActor;
    private final ActorRef replyTo;
    private Cancellable newLeaderTimer;
    private final List<OnComplete> onCompleteCallbacks = new ArrayList<>();
    private long newLeaderTimeoutInMillis = 2000;
    private final Stopwatch transferTimer = Stopwatch.createUnstarted();
    private boolean isTransferring;

    RaftActorLeadershipTransferCohort(RaftActor raftActor, ActorRef replyTo) {
        this.raftActor = raftActor;
        this.replyTo = replyTo;
    }

    void init() {
        RaftActorContext context = raftActor.getRaftActorContext();
        RaftActorBehavior currentBehavior = raftActor.getCurrentBehavior();

        transferTimer.start();

        Optional<ActorRef> roleChangeNotifier = raftActor.getRoleChangeNotifier();
        if(roleChangeNotifier.isPresent()) {
            roleChangeNotifier.get().tell(raftActor.newLeaderStateChanged(context.getId(), null,
                    currentBehavior.getLeaderPayloadVersion()), raftActor.self());
        }

        for(String peerId: context.getPeerIds()) {
            ActorSelection followerActor = context.getPeerActorSelection(peerId);
            if(followerActor != null) {
                followerActor.tell(LeaderTransitioning.INSTANCE, context.getActor());
            }
        }

        raftActor.pauseLeader(new TimedRunnable(context.getConfigParams().getElectionTimeOutInterval(), raftActor) {
            @Override
            protected void doRun() {
                doTransfer();
            }

            @Override
            protected void doCancel() {
                LOG.debug("{}: pauseLeader timed out - aborting transfer", raftActor.persistenceId());
                abortTransfer();
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
        if(behavior instanceof Leader) {
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

        // Add a timer in case we don't get a leader change - 2 sec should be plenty of time if a new
        // leader is elected. Note: the Runnable is sent as a message to the raftActor which executes it
        // safely run on the actor's thread dispatcher.
        FiniteDuration timeout = FiniteDuration.create(newLeaderTimeoutInMillis, TimeUnit.MILLISECONDS);
        newLeaderTimer = raftActor.getContext().system().scheduler().scheduleOnce(timeout, raftActor.self(),
                new Runnable() {
                    @Override
                    public void run() {
                        LOG.debug("{}: leader not elected in time", raftActor.persistenceId());
                        finish(true);
                    }
                }, raftActor.getContext().system().dispatcher(), raftActor.self());
    }

    void onNewLeader(String newLeader) {
        if(newLeader != null && newLeaderTimer != null) {
            LOG.debug("{}: leader changed to {}", raftActor.persistenceId(), newLeader);
            newLeaderTimer.cancel();
            finish(true);
        }
    }

    private void finish(boolean success) {
        isTransferring = false;
        if(transferTimer.isRunning()) {
            transferTimer.stop();
            if(success) {
                LOG.info("{}: Successfully transferred leadership to {} in {}", raftActor.persistenceId(),
                        raftActor.getLeaderId(), transferTimer.toString());
            } else {
                LOG.warn("{}: Failed to transfer leadership in {}", raftActor.persistenceId(),
                        transferTimer.toString());
            }
        }

        for(OnComplete onComplete: onCompleteCallbacks) {
            if(success) {
                onComplete.onSuccess(raftActor.self(), replyTo);
            } else {
                onComplete.onFailure(raftActor.self(), replyTo);
            }
        }
    }

    void addOnComplete(OnComplete onComplete) {
        onCompleteCallbacks.add(onComplete);
    }

    boolean isTransferring() {
        return isTransferring;
    }

    @VisibleForTesting
    void setNewLeaderTimeoutInMillis(long newLeaderTimeoutInMillis) {
        this.newLeaderTimeoutInMillis = newLeaderTimeoutInMillis;
    }

    interface OnComplete {
        void onSuccess(ActorRef raftActorRef, ActorRef replyTo);
        void onFailure(ActorRef raftActorRef, ActorRef replyTo);
    }
}
