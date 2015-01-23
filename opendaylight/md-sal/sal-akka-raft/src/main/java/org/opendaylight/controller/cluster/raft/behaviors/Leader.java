/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.base.messages.InitiateInstallSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.IsolatedLeaderCheck;
import scala.concurrent.duration.FiniteDuration;

/**
 * The behavior of a RaftActor when it is in the Leader state
 * <p/>
 * Leaders:
 * <ul>
 * <li> Upon election: send initial empty AppendEntries RPCs
 * (heartbeat) to each server; repeat during idle periods to
 * prevent election timeouts (§5.2)
 * <li> If command received from client: append entry to local log,
 * respond after entry applied to state machine (§5.3)
 * <li> If last log index ≥ nextIndex for a follower: send
 * AppendEntries RPC with log entries starting at nextIndex
 * <ul>
 * <li> If successful: update nextIndex and matchIndex for
 * follower (§5.3)
 * <li> If AppendEntries fails because of log inconsistency:
 * decrement nextIndex and retry (§5.3)
 * </ul>
 * <li> If there exists an N such that N > commitIndex, a majority
 * of matchIndex[i] ≥ N, and log[N].term == currentTerm:
 * set commitIndex = N (§5.3, §5.4).
 */
public class Leader extends AbstractLeader {
    private Cancellable installSnapshotSchedule = null;
    private Cancellable isolatedLeaderCheckSchedule = null;

    public Leader(RaftActorContext context) {
        super(context);

        scheduleInstallSnapshotCheck(context.getConfigParams().getIsolatedCheckInterval());

        scheduleIsolatedLeaderCheck(
            new FiniteDuration(context.getConfigParams().getHeartBeatInterval().length() * 10,
                context.getConfigParams().getHeartBeatInterval().unit()));
    }

    @Override public RaftActorBehavior handleMessage(ActorRef sender, Object originalMessage) {
        Preconditions.checkNotNull(sender, "sender should not be null");

        if (originalMessage instanceof IsolatedLeaderCheck) {
            if (isLeaderIsolated()) {
                LOG.info("At least {} followers need to be active, Switching {} from Leader to IsolatedLeader",
                    minIsolatedLeaderPeerCount, leaderId);
                return switchBehavior(new IsolatedLeader(context));
            }
        }

        return super.handleMessage(sender, originalMessage);
    }

    protected void stopInstallSnapshotSchedule() {
        if (installSnapshotSchedule != null && !installSnapshotSchedule.isCancelled()) {
            installSnapshotSchedule.cancel();
        }
    }

    protected void scheduleInstallSnapshotCheck(FiniteDuration interval) {
        if(followers.size() == 0){
            // Optimization - do not bother scheduling a heartbeat as there are
            // no followers
            return;
        }

        stopInstallSnapshotSchedule();

        // Schedule a message to send append entries to followers that can
        // accept an append entries with some data in it
        installSnapshotSchedule =
            context.getActorSystem().scheduler().scheduleOnce(
                interval,
                context.getActor(), new InitiateInstallSnapshot(),
                context.getActorSystem().dispatcher(), context.getActor());
    }

    protected void stopIsolatedLeaderCheckSchedule() {
        if (isolatedLeaderCheckSchedule != null && !isolatedLeaderCheckSchedule.isCancelled()) {
            isolatedLeaderCheckSchedule.cancel();
        }
    }

    protected void scheduleIsolatedLeaderCheck(FiniteDuration isolatedCheckInterval) {
        isolatedLeaderCheckSchedule = context.getActorSystem().scheduler().schedule(isolatedCheckInterval, isolatedCheckInterval,
            context.getActor(), new IsolatedLeaderCheck(),
            context.getActorSystem().dispatcher(), context.getActor());
    }

    @Override
    public void close() throws Exception {
        stopInstallSnapshotSchedule();
        stopIsolatedLeaderCheckSchedule();
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
}
