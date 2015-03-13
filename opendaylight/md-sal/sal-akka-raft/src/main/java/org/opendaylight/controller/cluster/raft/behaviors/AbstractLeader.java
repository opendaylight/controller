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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.cluster.raft.ClientRequestTracker;
import org.opendaylight.controller.cluster.raft.ClientRequestTrackerImpl;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;
import org.opendaylight.controller.cluster.raft.FollowerLogInformationImpl;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
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
public abstract class AbstractLeader extends AbstractRaftActorBehavior {

    // The index of the first chunk that is sent when installing a snapshot
    public static final int FIRST_CHUNK_INDEX = 1;

    // The index that the follower should respond with if it needs the install snapshot to be reset
    public static final int INVALID_CHUNK_INDEX = -1;

    // This would be passed as the hash code of the last chunk when sending the first chunk
    public static final int INITIAL_LAST_CHUNK_HASH_CODE = -1;

    private final Map<String, FollowerLogInformation> followerToLog;
    private final Map<String, FollowerToSnapshot> mapFollowerToSnapshot = new HashMap<>();

    private Cancellable heartbeatSchedule = null;

    private final Collection<ClientRequestTracker> trackerList = new LinkedList<>();

    protected final int minReplicationCount;

    protected final int minIsolatedLeaderPeerCount;

    private Optional<ByteString> snapshot;

    public AbstractLeader(RaftActorContext context) {
        super(context, RaftState.Leader);

        final Builder<String, FollowerLogInformation> ftlBuilder = ImmutableMap.builder();
        for (String followerId : context.getPeerAddresses().keySet()) {
            FollowerLogInformation followerLogInformation =
                new FollowerLogInformationImpl(followerId, -1, context);

            ftlBuilder.put(followerId, followerLogInformation);
        }
        followerToLog = ftlBuilder.build();

        leaderId = context.getId();

        LOG.debug("{}: Election: Leader has following peers: {}", logName(), getFollowerIds());

        minReplicationCount = getMajorityVoteCount(getFollowerIds().size());

        // the isolated Leader peer count will be 1 less than the majority vote count.
        // this is because the vote count has the self vote counted in it
        // for e.g
        // 0 peers = 1 votesRequired , minIsolatedLeaderPeerCount = 0
        // 2 peers = 2 votesRequired , minIsolatedLeaderPeerCount = 1
        // 4 peers = 3 votesRequired, minIsolatedLeaderPeerCount = 2
        minIsolatedLeaderPeerCount = minReplicationCount > 0 ? (minReplicationCount - 1) : 0;

        snapshot = Optional.absent();

        // Immediately schedule a heartbeat
        // Upon election: send initial empty AppendEntries RPCs
        // (heartbeat) to each server; repeat during idle periods to
        // prevent election timeouts (§5.2)
        sendAppendEntries(0, false);

        // It is important to schedule this heartbeat here
        scheduleHeartBeat(context.getConfigParams().getHeartBeatInterval());
    }

    /**
     * Return an immutable collection of follower identifiers.
     *
     * @return Collection of follower IDs
     */
    public final Collection<String> getFollowerIds() {
        return followerToLog.keySet();
    }

    @VisibleForTesting
    void setSnapshot(Optional<ByteString> snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    protected RaftActorBehavior handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries) {

        LOG.debug("{}: handleAppendEntries: {}", logName(), appendEntries);

        return this;
    }

    @Override
    protected RaftActorBehavior handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply) {

        if(LOG.isTraceEnabled()) {
            LOG.trace("{}: handleAppendEntriesReply: {}", logName(), appendEntriesReply);
        }

        // Update the FollowerLogInformation
        String followerId = appendEntriesReply.getFollowerId();
        FollowerLogInformation followerLogInformation =
            followerToLog.get(followerId);

        if(followerLogInformation == null){
            LOG.error("{}: handleAppendEntriesReply - unknown follower {}", logName(), followerId);
            return this;
        }

        if(followerLogInformation.timeSinceLastActivity() >
                context.getConfigParams().getElectionTimeOutInterval().toMillis()) {
            LOG.warn("{} : handleAppendEntriesReply delayed beyond election timeout, " +
                            "appendEntriesReply : {}, timeSinceLastActivity : {}, lastApplied : {}, commitIndex : {}",
                    logName(), appendEntriesReply, followerLogInformation.timeSinceLastActivity(),
                    context.getLastApplied(), context.getCommitIndex());
        }

        followerLogInformation.markFollowerActive();

        boolean updated = false;
        if (appendEntriesReply.isSuccess()) {
            updated = followerLogInformation.setMatchIndex(appendEntriesReply.getLogLastIndex());
            updated = followerLogInformation.setNextIndex(appendEntriesReply.getLogLastIndex() + 1) || updated;

            if(updated && LOG.isDebugEnabled()) {
                LOG.debug("{}: handleAppendEntriesReply - FollowerLogInformation for {} updated: matchIndex: {}, nextIndex: {}", logName(),
                        followerId, followerLogInformation.getMatchIndex(), followerLogInformation.getNextIndex());
            }
        } else {
            LOG.debug("{}: handleAppendEntriesReply: received unsuccessful reply: {}", logName(), appendEntriesReply);

            // TODO: When we find that the follower is out of sync with the
            // Leader we simply decrement that followers next index by 1.
            // Would it be possible to do better than this? The RAFT spec
            // does not explicitly deal with it but may be something for us to
            // think about

            followerLogInformation.decrNextIndex();
        }

        // Now figure out if this reply warrants a change in the commitIndex
        // If there exists an N such that N > commitIndex, a majority
        // of matchIndex[i] ≥ N, and log[N].term == currentTerm:
        // set commitIndex = N (§5.3, §5.4).
        for (long N = context.getCommitIndex() + 1; ; N++) {
            int replicatedCount = 1;

            for (FollowerLogInformation info : followerToLog.values()) {
                if (info.getMatchIndex() >= N) {
                    replicatedCount++;
                }
            }

            if (replicatedCount >= minReplicationCount) {
                ReplicatedLogEntry replicatedLogEntry = context.getReplicatedLog().get(N);
                if (replicatedLogEntry != null &&
                    replicatedLogEntry.getTerm() == currentTerm()) {
                    context.setCommitIndex(N);
                }
            } else {
                break;
            }
        }

        // Apply the change to the state machine
        if (context.getCommitIndex() > context.getLastApplied()) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: handleAppendEntriesReply from {}: applying to log - commitIndex: {}, lastAppliedIndex: {}",
                        logName(), followerId, context.getCommitIndex(), context.getLastApplied());
            }

            applyLogToStateMachine(context.getCommitIndex());
        }

        if (!context.isSnapshotCaptureInitiated()) {
            purgeInMemoryLog();
        }

        //Send the next log entry immediately, if possible, no need to wait for heartbeat to trigger that event
        sendUpdatesToFollower(followerId, followerLogInformation, false, !updated);
        return this;
    }

    private void purgeInMemoryLog() {
        //find the lowest index across followers which has been replicated to all.
        // lastApplied if there are no followers, so that we keep clearing the log for single-node
        // we would delete the in-mem log from that index on, in-order to minimize mem usage
        // we would also share this info thru AE with the followers so that they can delete their log entries as well.
        long minReplicatedToAllIndex = followerToLog.isEmpty() ? context.getLastApplied() : Long.MAX_VALUE;
        for (FollowerLogInformation info : followerToLog.values()) {
            minReplicatedToAllIndex = Math.min(minReplicatedToAllIndex, info.getMatchIndex());
        }

        super.performSnapshotWithoutCapture(minReplicatedToAllIndex);
    }

    @Override
    protected ClientRequestTracker removeClientRequestTracker(long logIndex) {
        final Iterator<ClientRequestTracker> it = trackerList.iterator();
        while (it.hasNext()) {
            final ClientRequestTracker t = it.next();
            if (t.getIndex() == logIndex) {
                it.remove();
                return t;
            }
        }

        return null;
    }

    @Override
    protected ClientRequestTracker findClientRequestTracker(long logIndex) {
        for (ClientRequestTracker tracker : trackerList) {
            if (tracker.getIndex() == logIndex) {
                return tracker;
            }
        }
        return null;
    }

    @Override
    protected RaftActorBehavior handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply) {
        return this;
    }

    protected void beforeSendHeartbeat(){}

    @Override
    public RaftActorBehavior handleMessage(ActorRef sender, Object originalMessage) {
        Preconditions.checkNotNull(sender, "sender should not be null");

        Object message = fromSerializableMessage(originalMessage);

        if (message instanceof RaftRPC) {
            RaftRPC rpc = (RaftRPC) message;
            // If RPC request or response contains term T > currentTerm:
            // set currentTerm = T, convert to follower (§5.1)
            // This applies to all RPC messages and responses
            if (rpc.getTerm() > context.getTermInformation().getCurrentTerm()) {
                LOG.debug("{}: Term {} in \"{}\" message is greater than leader's term {} - switching to Follower",
                        logName(), rpc.getTerm(), rpc, context.getTermInformation().getCurrentTerm());

                context.getTermInformation().updateAndPersist(rpc.getTerm(), null);

                return switchBehavior(new Follower(context));
            }
        }

        if (message instanceof SendHeartBeat) {
            beforeSendHeartbeat();
            sendHeartBeat();
            scheduleHeartBeat(context.getConfigParams().getHeartBeatInterval());
            return this;

        } else if(message instanceof SendInstallSnapshot) {
            // received from RaftActor
            setSnapshot(Optional.of(((SendInstallSnapshot) message).getSnapshot()));
            sendInstallSnapshot();

        } else if (message instanceof Replicate) {
            replicate((Replicate) message);

        } else if (message instanceof InstallSnapshotReply){
            handleInstallSnapshotReply((InstallSnapshotReply) message);

        }


        return super.handleMessage(sender, message);
    }

    private void handleInstallSnapshotReply(InstallSnapshotReply reply) {
        LOG.debug("{}: handleInstallSnapshotReply: {}", logName(), reply);

        String followerId = reply.getFollowerId();
        FollowerToSnapshot followerToSnapshot = mapFollowerToSnapshot.get(followerId);

        if (followerToSnapshot == null) {
            LOG.error("{}: FollowerId {} in InstallSnapshotReply not known to Leader",
                    logName(), followerId);
            return;
        }

        FollowerLogInformation followerLogInformation = followerToLog.get(followerId);
        followerLogInformation.markFollowerActive();

        if (followerToSnapshot.getChunkIndex() == reply.getChunkIndex()) {
            boolean wasLastChunk = false;
            if (reply.isSuccess()) {
                if(followerToSnapshot.isLastChunk(reply.getChunkIndex())) {
                    //this was the last chunk reply
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("{}: InstallSnapshotReply received, " +
                                "last chunk received, Chunk: {}. Follower: {} Setting nextIndex: {}",
                                logName(), reply.getChunkIndex(), followerId,
                            context.getReplicatedLog().getSnapshotIndex() + 1
                        );
                    }

                    followerLogInformation.setMatchIndex(
                        context.getReplicatedLog().getSnapshotIndex());
                    followerLogInformation.setNextIndex(
                        context.getReplicatedLog().getSnapshotIndex() + 1);
                    mapFollowerToSnapshot.remove(followerId);

                    LOG.debug("{}: follower: {}, matchIndex set to {}, nextIndex set to {}",
                                logName(), followerId, followerLogInformation.getMatchIndex(),
                                followerLogInformation.getNextIndex());

                    if (mapFollowerToSnapshot.isEmpty()) {
                        // once there are no pending followers receiving snapshots
                        // we can remove snapshot from the memory
                        setSnapshot(Optional.<ByteString>absent());
                    }
                    wasLastChunk = true;

                } else {
                    followerToSnapshot.markSendStatus(true);
                }
            } else {
                LOG.info("{}: InstallSnapshotReply received sending snapshot chunk failed, Will retry, Chunk: {}",
                        logName(), reply.getChunkIndex());

                followerToSnapshot.markSendStatus(false);
            }

            if (wasLastChunk && !context.isSnapshotCaptureInitiated()) {
                // Since the follower is now caught up try to purge the log.
                purgeInMemoryLog();
            } else if (!wasLastChunk && followerToSnapshot.canSendNextChunk()) {
                ActorSelection followerActor = context.getPeerActorSelection(followerId);
                if(followerActor != null) {
                    sendSnapshotChunk(followerActor, followerId);
                }
            }

        } else {
            LOG.error("{}: Chunk index {} in InstallSnapshotReply from follower {} does not match expected index {}",
                    logName(), reply.getChunkIndex(), followerId,
                    followerToSnapshot.getChunkIndex());

            if(reply.getChunkIndex() == INVALID_CHUNK_INDEX){
                // Since the Follower did not find this index to be valid we should reset the follower snapshot
                // so that Installing the snapshot can resume from the beginning
                followerToSnapshot.reset();
            }
        }
    }

    private void replicate(Replicate replicate) {
        long logIndex = replicate.getReplicatedLogEntry().getIndex();

        LOG.debug("{}: Replicate message: identifier: {}, logIndex: {}", logName(),
                replicate.getIdentifier(), logIndex);

        // Create a tracker entry we will use this later to notify the
        // client actor
        trackerList.add(
            new ClientRequestTrackerImpl(replicate.getClientActor(),
                replicate.getIdentifier(),
                logIndex)
        );

        if (followerToLog.isEmpty()) {
            context.setCommitIndex(logIndex);
            applyLogToStateMachine(logIndex);
        } else {
            sendAppendEntries(0, false);
        }
    }

    private void sendAppendEntries(long timeSinceLastActivityInterval, boolean isHeartbeat) {
        // Send an AppendEntries to all followers
        for (Entry<String, FollowerLogInformation> e : followerToLog.entrySet()) {
            final String followerId = e.getKey();
            final FollowerLogInformation followerLogInformation = e.getValue();
            // This checks helps not to send a repeat message to the follower
            if(!followerLogInformation.isFollowerActive() ||
                    followerLogInformation.timeSinceLastActivity() >= timeSinceLastActivityInterval) {
                sendUpdatesToFollower(followerId, followerLogInformation, true, isHeartbeat);
            }
        }
    }

    /**
     *
     * This method checks if any update needs to be sent to the given follower. This includes append log entries,
     * sending next snapshot chunk, and initiating a snapshot.
     * @return true if any update is sent, false otherwise
     */

    private void sendUpdatesToFollower(String followerId, FollowerLogInformation followerLogInformation,
                                       boolean sendHeartbeat, boolean isHeartbeat) {

        ActorSelection followerActor = context.getPeerActorSelection(followerId);
        if (followerActor != null) {
            long followerNextIndex = followerLogInformation.getNextIndex();
            boolean isFollowerActive = followerLogInformation.isFollowerActive();

            if (mapFollowerToSnapshot.get(followerId) != null) {
                // if install snapshot is in process , then sent next chunk if possible
                if (isFollowerActive && mapFollowerToSnapshot.get(followerId).canSendNextChunk()) {
                    sendSnapshotChunk(followerActor, followerId);
                } else if(sendHeartbeat) {
                    // we send a heartbeat even if we have not received a reply for the last chunk
                    sendAppendEntriesToFollower(followerActor, followerLogInformation.getNextIndex(),
                        Collections.<ReplicatedLogEntry>emptyList(), followerId);
                }
            } else {
                long leaderLastIndex = context.getReplicatedLog().lastIndex();
                long leaderSnapShotIndex = context.getReplicatedLog().getSnapshotIndex();

                if((!isHeartbeat && LOG.isDebugEnabled()) || LOG.isTraceEnabled()) {
                    LOG.debug("{}: Checking sendAppendEntries for follower {}, followerNextIndex {}, leaderLastIndex: {}, leaderSnapShotIndex: {}",
                            logName(), followerId, followerNextIndex, leaderLastIndex, leaderSnapShotIndex);
                }

                if (isFollowerActive && context.getReplicatedLog().isPresent(followerNextIndex)) {

                    LOG.debug("{}: sendAppendEntries: {} is present for follower {}", logName(),
                            followerNextIndex, followerId);

                    // FIXME : Sending one entry at a time
                    final List<ReplicatedLogEntry> entries = context.getReplicatedLog().getFrom(followerNextIndex, 1);

                    sendAppendEntriesToFollower(followerActor, followerNextIndex, entries, followerId);

                } else if (isFollowerActive && followerNextIndex >= 0 &&
                    leaderLastIndex > followerNextIndex && !context.isSnapshotCaptureInitiated()) {
                    // if the followers next index is not present in the leaders log, and
                    // if the follower is just not starting and if leader's index is more than followers index
                    // then snapshot should be sent

                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("%s: InitiateInstallSnapshot to follower: %s," +
                                    "follower-nextIndex: %d, leader-snapshot-index: %d,  " +
                                    "leader-last-index: %d", logName(), followerId,
                                    followerNextIndex, leaderSnapShotIndex, leaderLastIndex));
                    }

                    // Send heartbeat to follower whenever install snapshot is initiated.
                    sendAppendEntriesToFollower(followerActor, followerLogInformation.getNextIndex(),
                            Collections.<ReplicatedLogEntry>emptyList(), followerId);

                    initiateCaptureSnapshot(followerId, followerNextIndex);

                } else if(sendHeartbeat) {
                    //we send an AppendEntries, even if the follower is inactive
                    // in-order to update the followers timestamp, in case it becomes active again
                    sendAppendEntriesToFollower(followerActor, followerLogInformation.getNextIndex(),
                        Collections.<ReplicatedLogEntry>emptyList(), followerId);
                }

            }
        }
    }

    private void sendAppendEntriesToFollower(ActorSelection followerActor, long followerNextIndex,
        List<ReplicatedLogEntry> entries, String followerId) {
        AppendEntries appendEntries = new AppendEntries(currentTerm(), context.getId(),
            prevLogIndex(followerNextIndex),
            prevLogTerm(followerNextIndex), entries,
            context.getCommitIndex(), super.getReplicatedToAllIndex());

        if(!entries.isEmpty() || LOG.isTraceEnabled()) {
            LOG.debug("{}: Sending AppendEntries to follower {}: {}", logName(), followerId,
                    appendEntries);
        }

        followerActor.tell(appendEntries.toSerializable(), actor());
    }

    /**
     * Install Snapshot works as follows
     * 1. Leader initiates the capture snapshot by sending a CaptureSnapshot message to actor
     * 2. RaftActor on receipt of the CaptureSnapshotReply (from Shard), stores the received snapshot in the replicated log
     * and makes a call to Leader's handleMessage , with SendInstallSnapshot message.
     * 3. Leader , picks the snapshot from im-mem ReplicatedLog and sends it in chunks to the Follower
     * 4. On complete, Follower sends back a InstallSnapshotReply.
     * 5. On receipt of the InstallSnapshotReply for the last chunk, Leader marks the install complete for that follower
     * and replenishes the memory by deleting the snapshot in Replicated log.
     * 6. If another follower requires a snapshot and a snapshot has been collected (via CaptureSnapshotReply)
     * then send the existing snapshot in chunks to the follower.
     * @param followerId
     * @param followerNextIndex
     */
    private void initiateCaptureSnapshot(String followerId, long followerNextIndex) {
        if (!context.getReplicatedLog().isPresent(followerNextIndex) &&
                context.getReplicatedLog().isInSnapshot(followerNextIndex)) {

            if (snapshot.isPresent()) {
                // if a snapshot is present in the memory, most likely another install is in progress
                // no need to capture snapshot.
                // This could happen if another follower needs an install when one is going on.
                final ActorSelection followerActor = context.getPeerActorSelection(followerId);
                sendSnapshotChunk(followerActor, followerId);

            } else if (!context.isSnapshotCaptureInitiated()) {

                ReplicatedLogEntry lastAppliedEntry = context.getReplicatedLog().get(context.getLastApplied());
                long lastAppliedIndex = -1;
                long lastAppliedTerm = -1;

                if (lastAppliedEntry != null) {
                    lastAppliedIndex = lastAppliedEntry.getIndex();
                    lastAppliedTerm = lastAppliedEntry.getTerm();
                } else if (context.getReplicatedLog().getSnapshotIndex() > -1) {
                    lastAppliedIndex = context.getReplicatedLog().getSnapshotIndex();
                    lastAppliedTerm = context.getReplicatedLog().getSnapshotTerm();
                }

                boolean isInstallSnapshotInitiated = true;
                long replicatedToAllIndex = super.getReplicatedToAllIndex();
                ReplicatedLogEntry replicatedToAllEntry = context.getReplicatedLog().get(replicatedToAllIndex);

                CaptureSnapshot captureSnapshot = new CaptureSnapshot(
                        lastIndex(), lastTerm(), lastAppliedIndex, lastAppliedTerm,
                        (replicatedToAllEntry != null ? replicatedToAllEntry.getIndex() : -1),
                        (replicatedToAllEntry != null ? replicatedToAllEntry.getTerm() : -1),
                        isInstallSnapshotInitiated);

                if(LOG.isDebugEnabled()) {
                    LOG.debug("{}: Initiating install snapshot to follower {}: {}", logName(), followerId,
                            captureSnapshot);
                }

                actor().tell(captureSnapshot, actor());
                context.setSnapshotCaptureInitiated(true);
            }
        }
    }


    private void sendInstallSnapshot() {
        LOG.debug("{}: sendInstallSnapshot", logName());
        for (Entry<String, FollowerLogInformation> e : followerToLog.entrySet()) {
            ActorSelection followerActor = context.getPeerActorSelection(e.getKey());

            if (followerActor != null) {
                long nextIndex = e.getValue().getNextIndex();

                if (!context.getReplicatedLog().isPresent(nextIndex) &&
                    context.getReplicatedLog().isInSnapshot(nextIndex)) {
                    sendSnapshotChunk(followerActor, e.getKey());
                }
            }
        }
    }

    /**
     *  Sends a snapshot chunk to a given follower
     *  InstallSnapshot should qualify as a heartbeat too.
     */
    private void sendSnapshotChunk(ActorSelection followerActor, String followerId) {
        try {
            if (snapshot.isPresent()) {
                ByteString nextSnapshotChunk = getNextSnapshotChunk(followerId,snapshot.get());

                // Note: the previous call to getNextSnapshotChunk has the side-effect of adding
                // followerId to the followerToSnapshot map.
                FollowerToSnapshot followerToSnapshot = mapFollowerToSnapshot.get(followerId);

                followerActor.tell(
                    new InstallSnapshot(currentTerm(), context.getId(),
                        context.getReplicatedLog().getSnapshotIndex(),
                        context.getReplicatedLog().getSnapshotTerm(),
                        nextSnapshotChunk,
                        followerToSnapshot.incrementChunkIndex(),
                        followerToSnapshot.getTotalChunks(),
                        Optional.of(followerToSnapshot.getLastChunkHashCode())
                    ).toSerializable(),
                    actor()
                );

                if(LOG.isDebugEnabled()) {
                    LOG.debug("{}: InstallSnapshot sent to follower {}, Chunk: {}/{}",
                            logName(), followerActor.path(), followerToSnapshot.getChunkIndex(),
                            followerToSnapshot.getTotalChunks());
                }
            }
        } catch (IOException e) {
            LOG.error("{}: InstallSnapshot failed for Leader.", logName(), e);
        }
    }

    /**
     * Acccepts snaphot as ByteString, enters into map for future chunks
     * creates and return a ByteString chunk
     */
    private ByteString getNextSnapshotChunk(String followerId, ByteString snapshotBytes) throws IOException {
        FollowerToSnapshot followerToSnapshot = mapFollowerToSnapshot.get(followerId);
        if (followerToSnapshot == null) {
            followerToSnapshot = new FollowerToSnapshot(snapshotBytes);
            mapFollowerToSnapshot.put(followerId, followerToSnapshot);
        }
        ByteString nextChunk = followerToSnapshot.getNextChunk();

        LOG.debug("{}: next snapshot chunk size for follower {}: {}", logName(), followerId, nextChunk.size());

        return nextChunk;
    }

    private void sendHeartBeat() {
        if (!followerToLog.isEmpty()) {
            LOG.trace("{}: Sending heartbeat", logName());
            sendAppendEntries(context.getConfigParams().getHeartBeatInterval().toMillis(), true);
        }
    }

    private void stopHeartBeat() {
        if (heartbeatSchedule != null && !heartbeatSchedule.isCancelled()) {
            heartbeatSchedule.cancel();
        }
    }

    private void scheduleHeartBeat(FiniteDuration interval) {
        if (followerToLog.isEmpty()) {
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
        heartbeatSchedule = context.getActorSystem().scheduler().scheduleOnce(
            interval, context.getActor(), new SendHeartBeat(),
            context.getActorSystem().dispatcher(), context.getActor());
    }

    @Override
    public void close() throws Exception {
        stopHeartBeat();
    }

    @Override
    public String getLeaderId() {
        return context.getId();
    }

    protected boolean isLeaderIsolated() {
        int minPresent = minIsolatedLeaderPeerCount;
        for (FollowerLogInformation followerLogInformation : followerToLog.values()) {
            if (followerLogInformation.isFollowerActive()) {
                --minPresent;
                if (minPresent == 0) {
                    break;
                }
            }
        }
        return (minPresent != 0);
    }

    /**
     * Encapsulates the snapshot bytestring and handles the logic of sending
     * snapshot chunks
     */
    protected class FollowerToSnapshot {
        private final ByteString snapshotBytes;
        private int offset = 0;
        // the next snapshot chunk is sent only if the replyReceivedForOffset matches offset
        private int replyReceivedForOffset;
        // if replyStatus is false, the previous chunk is attempted
        private boolean replyStatus = false;
        private int chunkIndex;
        private final int totalChunks;
        private int lastChunkHashCode = AbstractLeader.INITIAL_LAST_CHUNK_HASH_CODE;
        private int nextChunkHashCode = AbstractLeader.INITIAL_LAST_CHUNK_HASH_CODE;

        public FollowerToSnapshot(ByteString snapshotBytes) {
            this.snapshotBytes = snapshotBytes;
            int size = snapshotBytes.size();
            totalChunks = ( size / context.getConfigParams().getSnapshotChunkSize()) +
                ((size % context.getConfigParams().getSnapshotChunkSize()) > 0 ? 1 : 0);
            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: Snapshot {} bytes, total chunks to send:{}",
                        logName(), size, totalChunks);
            }
            replyReceivedForOffset = -1;
            chunkIndex = AbstractLeader.FIRST_CHUNK_INDEX;
        }

        public ByteString getSnapshotBytes() {
            return snapshotBytes;
        }

        public int incrementOffset() {
            if(replyStatus) {
                // if prev chunk failed, we would want to sent the same chunk again
                offset = offset + context.getConfigParams().getSnapshotChunkSize();
            }
            return offset;
        }

        public int incrementChunkIndex() {
            if (replyStatus) {
                // if prev chunk failed, we would want to sent the same chunk again
                chunkIndex =  chunkIndex + 1;
            }
            return chunkIndex;
        }

        public int getChunkIndex() {
            return chunkIndex;
        }

        public int getTotalChunks() {
            return totalChunks;
        }

        public boolean canSendNextChunk() {
            // we only send a false if a chunk is sent but we have not received a reply yet
            return replyReceivedForOffset == offset;
        }

        public boolean isLastChunk(int chunkIndex) {
            return totalChunks == chunkIndex;
        }

        public void markSendStatus(boolean success) {
            if (success) {
                // if the chunk sent was successful
                replyReceivedForOffset = offset;
                replyStatus = true;
                lastChunkHashCode = nextChunkHashCode;
            } else {
                // if the chunk sent was failure
                replyReceivedForOffset = offset;
                replyStatus = false;
            }
        }

        public ByteString getNextChunk() {
            int snapshotLength = getSnapshotBytes().size();
            int start = incrementOffset();
            int size = context.getConfigParams().getSnapshotChunkSize();
            if (context.getConfigParams().getSnapshotChunkSize() > snapshotLength) {
                size = snapshotLength;
            } else {
                if ((start + context.getConfigParams().getSnapshotChunkSize()) > snapshotLength) {
                    size = snapshotLength - start;
                }
            }


            LOG.debug("{}: Next chunk: length={}, offset={},size={}", logName(),
                    snapshotLength, start, size);

            ByteString substring = getSnapshotBytes().substring(start, start + size);
            nextChunkHashCode = substring.hashCode();
            return substring;
        }

        /**
         * reset should be called when the Follower needs to be sent the snapshot from the beginning
         */
        public void reset(){
            offset = 0;
            replyStatus = false;
            replyReceivedForOffset = offset;
            chunkIndex = AbstractLeader.FIRST_CHUNK_INDEX;
            lastChunkHashCode = AbstractLeader.INITIAL_LAST_CHUNK_HASH_CODE;
        }

        public int getLastChunkHashCode() {
            return lastChunkHashCode;
        }
    }

    // called from example-actor for printing the follower-states
    public String printFollowerStates() {
        final StringBuilder sb = new StringBuilder();

        sb.append('[');
        for (FollowerLogInformation followerLogInformation : followerToLog.values()) {
            sb.append('{');
            sb.append(followerLogInformation.getId());
            sb.append(" state:");
            sb.append(followerLogInformation.isFollowerActive());
            sb.append("},");
        }
        sb.append(']');

        return sb.toString();
    }

    @VisibleForTesting
    public FollowerLogInformation getFollower(String followerId) {
        return followerToLog.get(followerId);
    }

    @VisibleForTesting
    protected void setFollowerSnapshot(String followerId, FollowerToSnapshot snapshot) {
        mapFollowerToSnapshot.put(followerId, snapshot);
    }

    @VisibleForTesting
    public int followerSnapshotSize() {
        return mapFollowerToSnapshot.size();
    }

    @VisibleForTesting
    public int followerLogSize() {
        return followerToLog.size();
    }
}
