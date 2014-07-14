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
import org.opendaylight.controller.cluster.raft.ClientRequestTracker;
import org.opendaylight.controller.cluster.raft.ClientRequestTrackerImpl;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;
import org.opendaylight.controller.cluster.raft.FollowerLogInformationImpl;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.internal.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.internal.messages.Replicate;
import org.opendaylight.controller.cluster.raft.internal.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
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


    private final Map<String, FollowerLogInformation> followerToLog =
        new HashMap();

    private final Map<String, ActorSelection> followerToActor = new HashMap<>();

    private Cancellable heartbeatCancel = null;

    private List<ClientRequestTracker> trackerList = new ArrayList<>();

    private final int minReplicationCount;

    public Leader(RaftActorContext context) {
        super(context);

        if(lastIndex() >= 0) {
            context.setCommitIndex(lastIndex());
        }

        for (String followerId : context.getPeerAddresses().keySet()) {
            FollowerLogInformation followerLogInformation =
                new FollowerLogInformationImpl(followerId,
                    new AtomicLong(lastIndex()),
                    new AtomicLong(-1));

            followerToActor.put(followerId,
                context.actorSelection(context.getPeerAddress(followerId)));

            followerToLog.put(followerId, followerLogInformation);

        }

        if (followerToActor.size() > 0) {
            minReplicationCount = (followerToActor.size() + 1) / 2 + 1;
        } else {
            minReplicationCount = 0;
        }


        // Immediately schedule a heartbeat
        // Upon election: send initial empty AppendEntries RPCs
        // (heartbeat) to each server; repeat during idle periods to
        // prevent election timeouts (§5.2)
        scheduleHeartBeat(new FiniteDuration(0, TimeUnit.SECONDS));


    }

    @Override protected RaftState handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries, RaftState suggestedState) {

        context.getLogger()
            .error("An unexpected AppendEntries received in state " + state());

        return suggestedState;
    }

    @Override protected RaftState handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply, RaftState suggestedState) {

        // Do not take any other action since a behavior change is coming
        if (suggestedState != state())
            return suggestedState;

        // Update the FollowerLogInformation
        String followerId = appendEntriesReply.getFollowerId();
        FollowerLogInformation followerLogInformation =
            followerToLog.get(followerId);
        if (appendEntriesReply.isSuccess()) {
            followerLogInformation
                .setMatchIndex(appendEntriesReply.getLogLastIndex());
            followerLogInformation
                .setNextIndex(appendEntriesReply.getLogLastIndex() + 1);
        } else {
            followerLogInformation.decrNextIndex();
        }

        // Now figure out if this reply warrants a change in the commitIndex
        // If there exists an N such that N > commitIndex, a majority
        // of matchIndex[i] ≥ N, and log[N].term == currentTerm:
        // set commitIndex = N (§5.3, §5.4).
        for (long N = context.getCommitIndex() + 1; ; N++) {
            int replicatedCount = 1;

            for (FollowerLogInformation info : followerToLog.values()) {
                if (info.getMatchIndex().get() >= N) {
                    replicatedCount++;
                }
            }

            if (replicatedCount >= minReplicationCount){
                ReplicatedLogEntry replicatedLogEntry =
                    context.getReplicatedLog().get(N);
                if (replicatedLogEntry != null
                    && replicatedLogEntry.getTerm()
                    == currentTerm()) {
                    context.setCommitIndex(N);
                }
            } else {
                break;
            }
        }

        if(context.getCommitIndex() > context.getLastApplied()){
            applyLogToStateMachine(context.getCommitIndex());
        }

        return suggestedState;
    }

    protected ClientRequestTracker findClientRequestTracker(long logIndex) {
        for (ClientRequestTracker tracker : trackerList) {
            if (tracker.getIndex() == logIndex) {
                return tracker;
            }
        }

        return null;
    }

    @Override protected RaftState handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply, RaftState suggestedState) {
        return suggestedState;
    }

    @Override public RaftState state() {
        return RaftState.Leader;
    }

    @Override public RaftState handleMessage(ActorRef sender, Object message) {
        Preconditions.checkNotNull(sender, "sender should not be null");

        try {
            if (message instanceof SendHeartBeat) {
                return sendHeartBeat();
            } else if (message instanceof Replicate) {

                Replicate replicate = (Replicate) message;
                long logIndex = replicate.getReplicatedLogEntry().getIndex();

                context.getLogger().debug("Replicate message " + logIndex);

                if (followerToActor.size() == 0) {
                    context.setCommitIndex(
                        replicate.getReplicatedLogEntry().getIndex());

                    context.getActor()
                        .tell(new ApplyState(replicate.getClientActor(),
                                replicate.getIdentifier(),
                                replicate.getReplicatedLogEntry()),
                            context.getActor()
                        );
                } else {

                    trackerList.add(
                        new ClientRequestTrackerImpl(replicate.getClientActor(),
                            replicate.getIdentifier(),
                            logIndex)
                    );

                    ReplicatedLogEntry prevEntry =
                        context.getReplicatedLog().get(lastIndex() - 1);
                    long prevLogIndex = -1;
                    long prevLogTerm = -1;
                    if (prevEntry != null) {
                        prevLogIndex = prevEntry.getIndex();
                        prevLogTerm = prevEntry.getTerm();
                    }
                    // Send an AppendEntries to all followers
                    for (String followerId : followerToActor.keySet()) {
                        ActorSelection followerActor =
                            followerToActor.get(followerId);
                        FollowerLogInformation followerLogInformation =
                            followerToLog.get(followerId);
                        followerActor.tell(
                            new AppendEntries(currentTerm(), context.getId(),
                                prevLogIndex, prevLogTerm,
                                context.getReplicatedLog().getFrom(
                                    followerLogInformation.getNextIndex()
                                        .get()
                                ), context.getCommitIndex()
                            ),
                            actor()
                        );
                    }
                }
            }
        } finally {
            scheduleHeartBeat(HEART_BEAT_INTERVAL);
        }

        return super.handleMessage(sender, message);
    }

    private RaftState sendHeartBeat() {
        if (followerToActor.size() > 0) {
            for (String follower : followerToActor.keySet()) {

                FollowerLogInformation followerLogInformation =
                    followerToLog.get(follower);

                AtomicLong nextIndex =
                    followerLogInformation.getNextIndex();

                List<ReplicatedLogEntry> entries =
                    context.getReplicatedLog().getFrom(nextIndex.get());

                followerToActor.get(follower).tell(new AppendEntries(
                        context.getTermInformation().getCurrentTerm(),
                        context.getId(),
                        context.getReplicatedLog().lastIndex(),
                        context.getReplicatedLog().lastTerm(),
                        entries, context.getCommitIndex()),
                    context.getActor()
                );
            }
        }
        return state();
    }

    private void stopHeartBeat() {
        if (heartbeatCancel != null && !heartbeatCancel.isCancelled()) {
            heartbeatCancel.cancel();
        }
    }

    private void scheduleHeartBeat(FiniteDuration interval) {
        stopHeartBeat();

        // Schedule a heartbeat. When the scheduler triggers a SendHeartbeat
        // message is sent to itself.
        // Scheduling the heartbeat only once here because heartbeats do not
        // need to be sent if there are other messages being sent to the remote
        // actor.
        heartbeatCancel =
            context.getActorSystem().scheduler().scheduleOnce(
                interval,
                context.getActor(), new SendHeartBeat(),
                context.getActorSystem().dispatcher(), context.getActor());
    }

    @Override public void close() throws Exception {
        stopHeartBeat();
    }

    @Override public String getLeaderId() {
        return context.getId();
    }

}
