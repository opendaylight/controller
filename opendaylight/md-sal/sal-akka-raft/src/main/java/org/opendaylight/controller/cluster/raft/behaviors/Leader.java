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
import akka.actor.Cancellable;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;
import org.opendaylight.controller.cluster.raft.FollowerLogInformationImpl;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.internal.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import scala.concurrent.duration.FiniteDuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
public class Leader extends AbstractRaftActorBehavior {

    /**
     * The interval at which a heart beat message will be sent to the remote
     * RaftActor
     * <p/>
     * Since this is set to 100 milliseconds the Election timeout should be
     * at least 200 milliseconds
     */
    private static final FiniteDuration HEART_BEAT_INTERVAL =
        new FiniteDuration(100, TimeUnit.MILLISECONDS);

    private final Map<String, ActorRef> followerToReplicator = new HashMap<>();

    private final Map<String, FollowerLogInformation> followerToLog =
        new HashMap();

    private final Map<String, ActorSelection> followerToActor = new HashMap<>();

    private Cancellable heartbeatCancel = null;

    public Leader(RaftActorContext context, List<String> followers) {
        super(context);

        for (String follower : followers) {

            FollowerLogInformation followerLogInformation =
                new FollowerLogInformationImpl(follower,
                    new AtomicLong(0),
                    new AtomicLong(0));

            followerToActor.put(follower,
                context.actorSelection(followerLogInformation.getId()));
            followerToLog.put(follower, followerLogInformation);

        }

        // Immediately schedule a heartbeat
        // Upon election: send initial empty AppendEntries RPCs
        // (heartbeat) to each server; repeat during idle periods to
        // prevent election timeouts (§5.2)
        scheduleHeartBeat(new FiniteDuration(0, TimeUnit.SECONDS));


    }

    @Override protected RaftState handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries, RaftState suggestedState) {
        return suggestedState;
    }

    @Override protected RaftState handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply, RaftState suggestedState) {
        return suggestedState;
    }

    @Override protected RaftState handleRequestVote(ActorRef sender,
        RequestVote requestVote, RaftState suggestedState) {
        return suggestedState;
    }

    @Override protected RaftState handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply, RaftState suggestedState) {
        return suggestedState;
    }

    @Override protected RaftState state() {
        return RaftState.Leader;
    }

    @Override public RaftState handleMessage(ActorRef sender, Object message) {
        Preconditions.checkNotNull(sender, "sender should not be null");

        scheduleHeartBeat(HEART_BEAT_INTERVAL);

        if (message instanceof SendHeartBeat) {
            for (ActorSelection follower : followerToActor.values()) {
                follower.tell(new AppendEntries(
                    context.getTermInformation().getCurrentTerm().get(),
                    context.getId(),
                    context.getReplicatedLog().last().getIndex(),
                    context.getReplicatedLog().last().getTerm(),
                    Collections.EMPTY_LIST, context.getCommitIndex().get()),
                    context.getActor());
            }
            return state();
        }
        return super.handleMessage(sender, message);
    }

    private void scheduleHeartBeat(FiniteDuration interval) {
        if (heartbeatCancel != null && !heartbeatCancel.isCancelled()) {
            heartbeatCancel.cancel();
        }

        // Schedule a heartbeat. When the scheduler triggers the replicator
        // will let the RaftActor (leader) know that a new heartbeat needs to be sent
        // Scheduling the heartbeat only once here because heartbeats do not
        // need to be sent if there are other messages being sent to the remote
        // actor.
        heartbeatCancel =
            context.getActorSystem().scheduler().scheduleOnce(interval,
                context.getActor(), new SendHeartBeat(),
                context.getActorSystem().dispatcher(), context.getActor());
    }

}
