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
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.ClientRequestTracker;
import org.opendaylight.controller.cluster.raft.ClientRequestTrackerImpl;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;
import org.opendaylight.controller.cluster.raft.FollowerLogInformationImpl;
import org.opendaylight.controller.cluster.raft.PeerInfo;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.Snapshot;
import org.opendaylight.controller.cluster.raft.VotingState;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.messages.UnInitializedFollowerSnapshotReply;
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

    private final Map<String, FollowerLogInformation> followerToLog = new HashMap<>();
    private final Map<String, FollowerToSnapshot> mapFollowerToSnapshot = new HashMap<>();

    private Cancellable heartbeatSchedule = null;

    private final Collection<ClientRequestTracker> trackerList = new LinkedList<>();

    private int minReplicationCount;

    private Optional<SnapshotHolder> snapshot;

    protected AbstractLeader(RaftActorContext context, RaftState state) {
        super(context, state);

        setLeaderPayloadVersion(context.getPayloadVersion());

        for(PeerInfo peerInfo: context.getPeers()) {
            FollowerLogInformation followerLogInformation = new FollowerLogInformationImpl(peerInfo, -1, context);
            followerToLog.put(peerInfo.getId(), followerLogInformation);
        }

        leaderId = context.getId();

        LOG.debug("{}: Election: Leader has following peers: {}", logName(), getFollowerIds());

        updateMinReplicaCount();

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

    public void addFollower(String followerId) {
        FollowerLogInformation followerLogInformation = new FollowerLogInformationImpl(
                context.getPeerInfo(followerId), -1, context);
        followerToLog.put(followerId, followerLogInformation);

        if(heartbeatSchedule == null) {
            scheduleHeartBeat(context.getConfigParams().getHeartBeatInterval());
        }
    }

    public void removeFollower(String followerId) {
        followerToLog.remove(followerId);
        mapFollowerToSnapshot.remove(followerId);
    }

    public void updateMinReplicaCount() {
        int numVoting = 0;
        for(PeerInfo peer: context.getPeers()) {
            if(peer.isVoting()) {
                numVoting++;
            }
        }

        minReplicationCount = getMajorityVoteCount(numVoting);
    }

    protected int getMinIsolatedLeaderPeerCount(){
      //the isolated Leader peer count will be 1 less than the majority vote count.
        //this is because the vote count has the self vote counted in it
        //for e.g
        //0 peers = 1 votesRequired , minIsolatedLeaderPeerCount = 0
        //2 peers = 2 votesRequired , minIsolatedLeaderPeerCount = 1
        //4 peers = 3 votesRequired, minIsolatedLeaderPeerCount = 2

        return minReplicationCount > 0 ? (minReplicationCount - 1) : 0;
    }

    @VisibleForTesting
    void setSnapshot(@Nullable Snapshot snapshot) {
        if(snapshot != null) {
            this.snapshot = Optional.of(new SnapshotHolder(snapshot));
        } else {
            this.snapshot = Optional.absent();
        }
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
        followerLogInformation.setPayloadVersion(appendEntriesReply.getPayloadVersion());
        followerLogInformation.setRaftVersion(appendEntriesReply.getRaftVersion());

        boolean updated = false;
        if (appendEntriesReply.isSuccess()) {
            updated = updateFollowerLogInformation(followerLogInformation, appendEntriesReply);
        } else {
            LOG.debug("{}: handleAppendEntriesReply: received unsuccessful reply: {}", logName(), appendEntriesReply);

            long followerLastLogIndex = appendEntriesReply.getLogLastIndex();
            ReplicatedLogEntry followersLastLogEntry = context.getReplicatedLog().get(followerLastLogIndex);
            if(appendEntriesReply.isForceInstallSnapshot()) {
                // Reset the followers match and next index. This is to signal that this follower has nothing
                // in common with this Leader and so would require a snapshot to be installed
                followerLogInformation.setMatchIndex(-1);
                followerLogInformation.setNextIndex(-1);

                // Force initiate a snapshot capture
                initiateCaptureSnapshot(followerId);
            } else if(followerLastLogIndex < 0 || (followersLastLogEntry != null &&
                    followersLastLogEntry.getTerm() == appendEntriesReply.getLogLastTerm())) {
                // The follower's log is empty or the last entry is present in the leader's journal
                // and the terms match so the follower is just behind the leader's journal from
                // the last snapshot, if any. We'll catch up the follower quickly by starting at the
                // follower's last log index.

                updated = updateFollowerLogInformation(followerLogInformation, appendEntriesReply);
            } else {
                // TODO: When we find that the follower is out of sync with the
                // Leader we simply decrement that followers next index by 1.
                // Would it be possible to do better than this? The RAFT spec
                // does not explicitly deal with it but may be something for us to
                // think about.

                followerLogInformation.decrNextIndex();
            }
        }

        // Now figure out if this reply warrants a change in the commitIndex
        // If there exists an N such that N > commitIndex, a majority
        // of matchIndex[i] ≥ N, and log[N].term == currentTerm:
        // set commitIndex = N (§5.3, §5.4).
        for (long N = context.getCommitIndex() + 1; ; N++) {
            int replicatedCount = 1;

            for (FollowerLogInformation info : followerToLog.values()) {
                final PeerInfo peerInfo = context.getPeerInfo(info.getId());
                if(info.getMatchIndex() >= N && (peerInfo != null && peerInfo.isVoting())) {
                    replicatedCount++;
                }
            }

            if (replicatedCount >= minReplicationCount) {
                ReplicatedLogEntry replicatedLogEntry = context.getReplicatedLog().get(N);
                if (replicatedLogEntry == null) {
                    break;
                }

                // Don't update the commit index if the log entry is from a previous term, as per §5.4.1:
                // "Raft never commits log entries from previous terms by counting replicas".
                // However we keep looping so we can make progress when new entries in the current term
                // reach consensus, as per §5.4.1: "once an entry from the current term is committed by
                // counting replicas, then all prior entries are committed indirectly".
                if (replicatedLogEntry.getTerm() == currentTerm()) {
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

        if (!context.getSnapshotManager().isCapturing()) {
            purgeInMemoryLog();
        }

        //Send the next log entry immediately, if possible, no need to wait for heartbeat to trigger that event
        sendUpdatesToFollower(followerId, followerLogInformation, false, !updated);
        return this;
    }

    private boolean updateFollowerLogInformation(FollowerLogInformation followerLogInformation,
            AppendEntriesReply appendEntriesReply) {
        boolean updated = followerLogInformation.setMatchIndex(appendEntriesReply.getLogLastIndex());
        updated = followerLogInformation.setNextIndex(appendEntriesReply.getLogLastIndex() + 1) || updated;

        if(updated && LOG.isDebugEnabled()) {
            LOG.debug("{}: handleAppendEntriesReply - FollowerLogInformation for {} updated: matchIndex: {}, nextIndex: {}",
                    logName(), followerLogInformation.getId(), followerLogInformation.getMatchIndex(),
                    followerLogInformation.getNextIndex());
        }
        return updated;
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

                return internalSwitchBehavior(RaftState.Follower);
            }
        }

        if (message instanceof SendHeartBeat) {
            beforeSendHeartbeat();
            sendHeartBeat();
            scheduleHeartBeat(context.getConfigParams().getHeartBeatInterval());
            return this;

        } else if(message instanceof SendInstallSnapshot) {
            // received from RaftActor
            setSnapshot(((SendInstallSnapshot) message).getSnapshot());
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
            LOG.error("{}: FollowerToSnapshot not found for follower {} in InstallSnapshotReply",
                    logName(), followerId);
            return;
        }

        FollowerLogInformation followerLogInformation = followerToLog.get(followerId);
        if(followerLogInformation == null) {
            // This can happen during AddServer if it times out.
            LOG.error("{}: FollowerLogInformation not found for follower {} in InstallSnapshotReply",
                    logName(), followerId);
            mapFollowerToSnapshot.remove(followerId);
            return;
        }

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

                    long followerMatchIndex = snapshot.get().getLastIncludedIndex();
                    followerLogInformation.setMatchIndex(followerMatchIndex);
                    followerLogInformation.setNextIndex(followerMatchIndex + 1);
                    mapFollowerToSnapshot.remove(followerId);

                    LOG.debug("{}: follower: {}, matchIndex set to {}, nextIndex set to {}",
                        logName(), followerId, followerLogInformation.getMatchIndex(),
                        followerLogInformation.getNextIndex());

                    if (mapFollowerToSnapshot.isEmpty()) {
                        // once there are no pending followers receiving snapshots
                        // we can remove snapshot from the memory
                        setSnapshot(null);
                    }
                    wasLastChunk = true;
                    if(context.getPeerInfo(followerId).getVotingState() == VotingState.VOTING_NOT_INITIALIZED){
                        UnInitializedFollowerSnapshotReply unInitFollowerSnapshotSuccess =
                                             new UnInitializedFollowerSnapshotReply(followerId);
                        context.getActor().tell(unInitFollowerSnapshotSuccess, context.getActor());
                        LOG.debug("Sent message UnInitializedFollowerSnapshotReply to self");
                    }
                } else {
                    followerToSnapshot.markSendStatus(true);
                }
            } else {
                LOG.info("{}: InstallSnapshotReply received sending snapshot chunk failed, Will retry, Chunk: {}",
                        logName(), reply.getChunkIndex());

                followerToSnapshot.markSendStatus(false);
            }

            if (wasLastChunk && !context.getSnapshotManager().isCapturing()) {
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

        boolean applyModificationToState = followerToLog.isEmpty()
                || context.getRaftPolicy().applyModificationToStateBeforeConsensus();

        if(applyModificationToState){
            context.setCommitIndex(logIndex);
            applyLogToStateMachine(logIndex);
        }

        if (!followerToLog.isEmpty()) {
            sendAppendEntries(0, false);
        }
    }

    protected void sendAppendEntries(long timeSinceLastActivityInterval, boolean isHeartbeat) {
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
            boolean sendAppendEntries = false;
            List<ReplicatedLogEntry> entries = Collections.emptyList();

            if (mapFollowerToSnapshot.get(followerId) != null) {
                // if install snapshot is in process , then sent next chunk if possible
                if (isFollowerActive && mapFollowerToSnapshot.get(followerId).canSendNextChunk()) {
                    sendSnapshotChunk(followerActor, followerId);
                } else if(sendHeartbeat) {
                    // we send a heartbeat even if we have not received a reply for the last chunk
                    sendAppendEntries = true;
                }
            } else {
                long leaderLastIndex = context.getReplicatedLog().lastIndex();
                long leaderSnapShotIndex = context.getReplicatedLog().getSnapshotIndex();

                if((!isHeartbeat && LOG.isDebugEnabled()) || LOG.isTraceEnabled()) {
                    LOG.debug("{}: Checking sendAppendEntries for follower {}: active: {}, followerNextIndex: {}, leaderLastIndex: {}, leaderSnapShotIndex: {}",
                            logName(), followerId, isFollowerActive, followerNextIndex, leaderLastIndex, leaderSnapShotIndex);
                }

                if (isFollowerActive && context.getReplicatedLog().isPresent(followerNextIndex)) {

                    LOG.debug("{}: sendAppendEntries: {} is present for follower {}", logName(),
                            followerNextIndex, followerId);

                    if(followerLogInformation.okToReplicate()) {
                        // Try to send all the entries in the journal but not exceeding the max data size
                        // for a single AppendEntries message.
                        int maxEntries = (int) context.getReplicatedLog().size();
                        entries = context.getReplicatedLog().getFrom(followerNextIndex, maxEntries,
                                context.getConfigParams().getSnapshotChunkSize());
                        sendAppendEntries = true;
                    }
                } else if (isFollowerActive && followerNextIndex >= 0 &&
                    leaderLastIndex > followerNextIndex && !context.getSnapshotManager().isCapturing()) {
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
                    sendAppendEntries = true;
                    if (canInstallSnapshot(followerNextIndex)) {
                        initiateCaptureSnapshot(followerId);
                    }

                } else if(sendHeartbeat) {
                    // we send an AppendEntries, even if the follower is inactive
                    // in-order to update the followers timestamp, in case it becomes active again
                    sendAppendEntries = true;
                }

            }

            if(sendAppendEntries) {
                sendAppendEntriesToFollower(followerActor, followerNextIndex,
                        entries, followerId);
            }
        }
    }

    private void sendAppendEntriesToFollower(ActorSelection followerActor, long followerNextIndex,
        List<ReplicatedLogEntry> entries, String followerId) {
        AppendEntries appendEntries = new AppendEntries(currentTerm(), context.getId(),
            prevLogIndex(followerNextIndex),
            prevLogTerm(followerNextIndex), entries,
            context.getCommitIndex(), super.getReplicatedToAllIndex(), context.getPayloadVersion());

        if(!entries.isEmpty() || LOG.isTraceEnabled()) {
            LOG.debug("{}: Sending AppendEntries to follower {}: {}", logName(), followerId,
                    appendEntries);
        }

        followerActor.tell(appendEntries, actor());
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
     */
    public boolean initiateCaptureSnapshot(String followerId) {
        if (snapshot.isPresent()) {
            // if a snapshot is present in the memory, most likely another install is in progress
            // no need to capture snapshot.
            // This could happen if another follower needs an install when one is going on.
            final ActorSelection followerActor = context.getPeerActorSelection(followerId);
            sendSnapshotChunk(followerActor, followerId);
            return true;
        } else {
            return context.getSnapshotManager().captureToInstall(context.getReplicatedLog().last(),
                    this.getReplicatedToAllIndex(), followerId);
        }
    }

    private boolean canInstallSnapshot(long nextIndex){
        // If the follower's nextIndex is -1 then we might as well send it a snapshot
        // Otherwise send it a snapshot only if the nextIndex is not present in the log but is present
        // in the snapshot
        return (nextIndex == -1 ||
                (!context.getReplicatedLog().isPresent(nextIndex)
                        && context.getReplicatedLog().isInSnapshot(nextIndex)));

    }


    private void sendInstallSnapshot() {
        LOG.debug("{}: sendInstallSnapshot", logName());
        for (Entry<String, FollowerLogInformation> e : followerToLog.entrySet()) {
            String followerId = e.getKey();
            ActorSelection followerActor = context.getPeerActorSelection(followerId);
            FollowerLogInformation followerLogInfo = e.getValue();

            if (followerActor != null) {
                long nextIndex = followerLogInfo.getNextIndex();
                if (context.getPeerInfo(followerId).getVotingState() == VotingState.VOTING_NOT_INITIALIZED ||
                        canInstallSnapshot(nextIndex)) {
                    sendSnapshotChunk(followerActor, followerId);
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
                byte[] nextSnapshotChunk = getNextSnapshotChunk(followerId, snapshot.get().getSnapshotBytes());

                // Note: the previous call to getNextSnapshotChunk has the side-effect of adding
                // followerId to the followerToSnapshot map.
                FollowerToSnapshot followerToSnapshot = mapFollowerToSnapshot.get(followerId);

                followerActor.tell(
                    new InstallSnapshot(currentTerm(), context.getId(),
                        snapshot.get().getLastIncludedIndex(),
                        snapshot.get().getLastIncludedTerm(),
                        nextSnapshotChunk,
                        followerToSnapshot.incrementChunkIndex(),
                        followerToSnapshot.getTotalChunks(),
                        Optional.of(followerToSnapshot.getLastChunkHashCode())
                    ).toSerializable(followerToLog.get(followerId).getRaftVersion()),
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
    private byte[] getNextSnapshotChunk(String followerId, ByteString snapshotBytes) throws IOException {
        FollowerToSnapshot followerToSnapshot = mapFollowerToSnapshot.get(followerId);
        if (followerToSnapshot == null) {
            followerToSnapshot = new FollowerToSnapshot(snapshotBytes);
            mapFollowerToSnapshot.put(followerId, followerToSnapshot);
        }
        byte[] nextChunk = followerToSnapshot.getNextChunk();

        LOG.debug("{}: next snapshot chunk size for follower {}: {}", logName(), followerId, nextChunk.length);

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
            interval, context.getActor(), SendHeartBeat.INSTANCE,
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
        int minPresent = getMinIsolatedLeaderPeerCount();
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

        public byte[] getNextChunk() {
            int snapshotLength = getSnapshotBytes().size();
            int start = incrementOffset();
            int size = context.getConfigParams().getSnapshotChunkSize();
            if (context.getConfigParams().getSnapshotChunkSize() > snapshotLength) {
                size = snapshotLength;
            } else if ((start + context.getConfigParams().getSnapshotChunkSize()) > snapshotLength) {
                size = snapshotLength - start;
            }

            byte[] nextChunk = new byte[size];
            getSnapshotBytes().copyTo(nextChunk, start, 0, size);
            nextChunkHashCode = Arrays.hashCode(nextChunk);

            LOG.debug("{}: Next chunk: total length={}, offset={}, size={}, hashCode={}", logName(),
                    snapshotLength, start, size, nextChunkHashCode);
            return nextChunk;
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

    private static class SnapshotHolder {
        private final long lastIncludedTerm;
        private final long lastIncludedIndex;
        private final ByteString snapshotBytes;

        SnapshotHolder(Snapshot snapshot) {
            this.lastIncludedTerm = snapshot.getLastAppliedTerm();
            this.lastIncludedIndex = snapshot.getLastAppliedIndex();
            this.snapshotBytes = ByteString.copyFrom(snapshot.getState());
        }

        long getLastIncludedTerm() {
            return lastIncludedTerm;
        }

        long getLastIncludedIndex() {
            return lastIncludedIndex;
        }

        ByteString getSnapshotBytes() {
            return snapshotBytes;
        }
    }
}
