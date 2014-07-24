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
import org.opendaylight.controller.cluster.raft.internal.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.protobuff.ProtoBuffEncoderDecoderUtil;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
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


    private final Map<String, FollowerLogInformation> followerToLog =
        new HashMap();

    private final Map<String, ActorSelection> followerToActor = new HashMap<>();

    private Cancellable heartbeatSchedule = null;
    private Cancellable appendEntriesSchedule = null;
    private Cancellable installSnapshotSchedule = null;

    private List<ClientRequestTracker> trackerList = new ArrayList<>();

    private final int minReplicationCount;

    public Leader(RaftActorContext context) {
        super(context);

        if (lastIndex() >= 0) {
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

        context.getLogger().debug("Election:Leader has following peers:"+followerToActor.keySet());

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

        scheduleInstallSnapshotCheck(
            new FiniteDuration(HEART_BEAT_INTERVAL.length() * 1000,
                HEART_BEAT_INTERVAL.unit())
        );

    }

    @Override protected RaftState handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries) {

        return state();
    }

    @Override protected RaftState handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply) {

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

            if (replicatedCount >= minReplicationCount) {
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

        // Apply the change to the state machine
        if (context.getCommitIndex() > context.getLastApplied()) {
            applyLogToStateMachine(context.getCommitIndex());
        }

        return state();
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
        RequestVoteReply requestVoteReply) {
        return state();
    }

    @Override public RaftState state() {
        return RaftState.Leader;
    }

    @Override public RaftState handleMessage(ActorRef sender, Object protoBuffMessage) {
        Preconditions.checkNotNull(sender, "sender should not be null");

        Object message = ProtoBuffEncoderDecoderUtil.decode(protoBuffMessage);

        if (message instanceof RaftRPC) {
            RaftRPC rpc = (RaftRPC) message;
            // If RPC request or response contains term T > currentTerm:
            // set currentTerm = T, convert to follower (§5.1)
            // This applies to all RPC messages and responses
            if (rpc.getTerm() > context.getTermInformation().getCurrentTerm()) {
                context.getTermInformation().updateAndPersist(rpc.getTerm(), null);
                return RaftState.Follower;
            }
        }

        try {
            if (message instanceof SendHeartBeat) {
                return sendHeartBeat();
            } else if(message instanceof SendInstallSnapshot) {
                installSnapshotIfNeeded();
            } else if (message instanceof Replicate) {
                replicate((Replicate) message);
            } else if (message instanceof InstallSnapshotReply){
                // FIXME : Should I be checking the term here too?
                handleInstallSnapshotReply(
                    (InstallSnapshotReply) message);
            }
        } finally {
            scheduleHeartBeat(HEART_BEAT_INTERVAL);
        }

        return super.handleMessage(sender, message);
    }

    private void handleInstallSnapshotReply(InstallSnapshotReply message) {
        InstallSnapshotReply reply = message;
        String followerId = reply.getFollowerId();
        FollowerLogInformation followerLogInformation =
            followerToLog.get(followerId);

        followerLogInformation
            .setMatchIndex(context.getReplicatedLog().getSnapshotIndex());
        followerLogInformation
            .setNextIndex(context.getReplicatedLog().getSnapshotIndex() + 1);
    }

    private void replicate(Replicate replicate) {
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

            // Create a tracker entry we will use this later to notify the
            // client actor
            trackerList.add(
                new ClientRequestTrackerImpl(replicate.getClientActor(),
                    replicate.getIdentifier(),
                    logIndex)
            );

            sendAppendEntries();
        }
    }

    private void sendAppendEntries() {
        // Send an AppendEntries to all followers
        for (String followerId : followerToActor.keySet()) {
            ActorSelection followerActor =
                followerToActor.get(followerId);

            FollowerLogInformation followerLogInformation =
                followerToLog.get(followerId);

            long nextIndex = followerLogInformation.getNextIndex().get();

            List<ReplicatedLogEntry> entries = Collections.emptyList();

            if(context.getReplicatedLog().isPresent(nextIndex)){
                entries =
                    context.getReplicatedLog().getFrom(nextIndex);
            }

            followerActor.tell(ProtoBuffEncoderDecoderUtil.encode(
                new AppendEntries(currentTerm(), context.getId(), prevLogIndex(nextIndex),
                    prevLogTerm(nextIndex), entries, context.getCommitIndex())),
                actor());
        }
    }

    private void installSnapshotIfNeeded(){
        for (String followerId : followerToActor.keySet()) {
            ActorSelection followerActor =
                followerToActor.get(followerId);

            FollowerLogInformation followerLogInformation =
                followerToLog.get(followerId);

            long nextIndex = followerLogInformation.getNextIndex().get();

            if(!context.getReplicatedLog().isPresent(nextIndex) && context.getReplicatedLog().isInSnapshot(nextIndex)){
                followerActor.tell(
                    new InstallSnapshot(currentTerm(), context.getId(),
                        context.getReplicatedLog().getSnapshotIndex(),
                        context.getReplicatedLog().getSnapshotTerm(),
                        context.getReplicatedLog().getSnapshot()
                    ),
                    actor()
                );
            }
        }
    }

    private RaftState sendHeartBeat() {
        if (followerToActor.size() > 0) {
            sendAppendEntries();
        }
        return state();
    }

    private void stopHeartBeat() {
        if (heartbeatSchedule != null && !heartbeatSchedule.isCancelled()) {
            heartbeatSchedule.cancel();
        }
    }

    private void stopInstallSnapshotSchedule() {
        if (installSnapshotSchedule != null && !installSnapshotSchedule.isCancelled()) {
            installSnapshotSchedule.cancel();
        }
    }

    private void scheduleHeartBeat(FiniteDuration interval) {
        if(followerToActor.keySet().size() == 0){
            // Optimization - do not bother scheduling a heartbeat as there are
            // no followers
            return;
        }

        stopHeartBeat();

        // Schedule a heartbeat. When the scheduler triggers a SendHeartbeat
        // message is sent to itself.
        // Scheduling the heartbeat only once here because heartbeats do not
        // need to be sent if there are other messages being sent to the remote
        // actor.
        heartbeatSchedule =
            context.getActorSystem().scheduler().scheduleOnce(
                interval,
                context.getActor(), new SendHeartBeat(),
                context.getActorSystem().dispatcher(), context.getActor());
    }


    private void scheduleInstallSnapshotCheck(FiniteDuration interval) {
        if(followerToActor.keySet().size() == 0){
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
                context.getActor(), new SendInstallSnapshot(),
                context.getActorSystem().dispatcher(), context.getActor());
    }



    @Override public void close() throws Exception {
        stopHeartBeat();
    }

    @Override public String getLeaderId() {
        return context.getId();
    }

}
