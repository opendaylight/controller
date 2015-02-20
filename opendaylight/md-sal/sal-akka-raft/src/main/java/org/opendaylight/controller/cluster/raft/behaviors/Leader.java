/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.base.messages.IsolatedLeaderCheck;

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
    private static final IsolatedLeaderCheck ISOLATED_LEADER_CHECK = new IsolatedLeaderCheck();
    private final Stopwatch isolatedLeaderCheck;

    public Leader(RaftActorContext context) {
        super(context);
        isolatedLeaderCheck = Stopwatch.createStarted();
    }

    @Override public RaftActorBehavior handleMessage(ActorRef sender, Object originalMessage) {
        Preconditions.checkNotNull(sender, "sender should not be null");

        if (originalMessage instanceof IsolatedLeaderCheck) {
            if (isLeaderIsolated()) {
                LOG.error("{}: At least {} followers need to be active, Switching {} from Leader to IsolatedLeader",
                        context.getId(), minIsolatedLeaderPeerCount, leaderId);

                return switchBehavior(new IsolatedLeader(context));
            }
        }

        return super.handleMessage(sender, originalMessage);
    }

    @Override
    protected void beforeSendHeartbeat(){
        if(isolatedLeaderCheck.elapsed(TimeUnit.MILLISECONDS) > context.getConfigParams().getIsolatedCheckInterval().toMillis()){
            context.getActor().tell(ISOLATED_LEADER_CHECK, context.getActor());
            isolatedLeaderCheck.reset().start();
        }

    }

    @Override
    public void close() throws Exception {
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
