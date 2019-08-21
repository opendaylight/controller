/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.io.SharedFileBackedOutputStream;
import org.opendaylight.controller.cluster.messaging.MessageSlicer;
import org.opendaylight.controller.cluster.messaging.SliceOptions;
import org.opendaylight.controller.cluster.raft.ClientRequestTracker;
import org.opendaylight.controller.cluster.raft.ClientRequestTrackerImpl;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;
import org.opendaylight.controller.cluster.raft.PeerInfo;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.VotingState;
import org.opendaylight.controller.cluster.raft.base.messages.CheckConsensusReached;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotFinished;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.messages.UnInitializedFollowerSnapshotReply;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import scala.concurrent.duration.FiniteDuration;

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
public abstract class AbstractLeader extends AbstractRaftActorBehavior {
    private final Map<String, FollowerLogInformation> followerToLog = new HashMap<>();

    /**
     * Lookup table for request contexts based on journal index. We could use a {@link Map} here, but we really
     * expect the entries to be modified in sequence, hence we open-code the lookup.
     * TODO: Evaluate the use of ArrayDeque(), as that has lower memory overhead. Non-head removals are more costly,
     *       but we already expect those to be far from frequent.
     */
    private final Queue<ClientRequestTracker> trackers = new LinkedList<>();

    /**
     * Map of serialized AppendEntries output streams keyed by log index. This is used in conjunction with the
     * appendEntriesMessageSlicer for slicing single ReplicatedLogEntry payloads that exceed the message size threshold.
     * This Map allows the SharedFileBackedOutputStreams to be reused for multiple followers.
     */
    private final Map<Long, SharedFileBackedOutputStream> sharedSerializedAppendEntriesStreams = new HashMap<>();
    private final MessageSlicer appendEntriesMessageSlicer;

    private Cancellable heartbeatSchedule = null;
    private Optional<SnapshotHolder> snapshotHolder = Optional.empty();
    private int minReplicationCount;

    protected AbstractLeader(final RaftActorContext context, final RaftState state,
            final @Nullable AbstractLeader initializeFromLeader) {
        super(context, state);

        appendEntriesMessageSlicer = MessageSlicer.builder().logContext(logName())
            .messageSliceSize(context.getConfigParams().getSnapshotChunkSize())
            .expireStateAfterInactivity(context.getConfigParams().getElectionTimeOutInterval().toMillis() * 3,
                    TimeUnit.MILLISECONDS).build();

        if (initializeFromLeader != null) {
            followerToLog.putAll(initializeFromLeader.followerToLog);
            snapshotHolder = initializeFromLeader.snapshotHolder;
            trackers.addAll(initializeFromLeader.trackers);
        } else {
            for (PeerInfo peerInfo: context.getPeers()) {
                FollowerLogInformation followerLogInformation = new FollowerLogInformation(peerInfo, context);
                followerToLog.put(peerInfo.getId(), followerLogInformation);
            }
        }

        log.debug("{}: Election: Leader has following peers: {}", logName(), getFollowerIds());

        updateMinReplicaCount();

        // Immediately schedule a heartbeat
        // Upon election: send initial empty AppendEntries RPCs
        // (heartbeat) to each server; repeat during idle periods to
        // prevent election timeouts (§5.2)
        sendAppendEntries(0, false);

        // It is important to schedule this heartbeat here
        scheduleHeartBeat(context.getConfigParams().getHeartBeatInterval());
    }

    protected AbstractLeader(final RaftActorContext context, final RaftState state) {
        this(context, state, null);
    }

    /**
     * Return an immutable collection of follower identifiers.
     *
     * @return Collection of follower IDs
     */
    public final Collection<String> getFollowerIds() {
        return followerToLog.keySet();
    }

    public void addFollower(final String followerId) {
        FollowerLogInformation followerLogInformation = new FollowerLogInformation(context.getPeerInfo(followerId),
            context);
        followerToLog.put(followerId, followerLogInformation);

        if (heartbeatSchedule == null) {
            scheduleHeartBeat(context.getConfigParams().getHeartBeatInterval());
        }
    }

    public void removeFollower(final String followerId) {
        followerToLog.remove(followerId);
    }

    public void updateMinReplicaCount() {
        int numVoting = 0;
        for (PeerInfo peer: context.getPeers()) {
            if (peer.isVoting()) {
                numVoting++;
            }
        }

        minReplicationCount = getMajorityVoteCount(numVoting);
    }

    protected int getMinIsolatedLeaderPeerCount() {
      //the isolated Leader peer count will be 1 less than the majority vote count.
        //this is because the vote count has the self vote counted in it
        //for e.g
        //0 peers = 1 votesRequired , minIsolatedLeaderPeerCount = 0
        //2 peers = 2 votesRequired , minIsolatedLeaderPeerCount = 1
        //4 peers = 3 votesRequired, minIsolatedLeaderPeerCount = 2

        return minReplicationCount > 0 ? minReplicationCount - 1 : 0;
    }

    @VisibleForTesting
    void setSnapshotHolder(final @Nullable SnapshotHolder snapshotHolder) {
        this.snapshotHolder = Optional.ofNullable(snapshotHolder);
    }

    @VisibleForTesting
    boolean hasSnapshot() {
        return snapshotHolder.isPresent();
    }

    @Override
    protected RaftActorBehavior handleAppendEntries(final ActorRef sender,
        final AppendEntries appendEntries) {

        log.debug("{}: handleAppendEntries: {}", logName(), appendEntries);

        return this;
    }

    @Override
    protected RaftActorBehavior handleAppendEntriesReply(final ActorRef sender,
            final AppendEntriesReply appendEntriesReply) {
        log.trace("{}: handleAppendEntriesReply: {}", logName(), appendEntriesReply);

        // Update the FollowerLogInformation
        String followerId = appendEntriesReply.getFollowerId();
        FollowerLogInformation followerLogInformation = followerToLog.get(followerId);

        if (followerLogInformation == null) {
            log.error("{}: handleAppendEntriesReply - unknown follower {}", logName(), followerId);
            return this;
        }

        final long lastActivityNanos = followerLogInformation.nanosSinceLastActivity();
        if (lastActivityNanos > context.getConfigParams().getElectionTimeOutInterval().toNanos()) {
            log.warn("{} : handleAppendEntriesReply delayed beyond election timeout, "
                    + "appendEntriesReply : {}, timeSinceLastActivity : {}, lastApplied : {}, commitIndex : {}",
                    logName(), appendEntriesReply, TimeUnit.NANOSECONDS.toMillis(lastActivityNanos),
                    context.getLastApplied(), context.getCommitIndex());
        }

        followerLogInformation.markFollowerActive();
        followerLogInformation.setPayloadVersion(appendEntriesReply.getPayloadVersion());
        followerLogInformation.setRaftVersion(appendEntriesReply.getRaftVersion());
        followerLogInformation.setNeedsLeaderAddress(appendEntriesReply.isNeedsLeaderAddress());

        long followerLastLogIndex = appendEntriesReply.getLogLastIndex();
        boolean updated = false;
        if (appendEntriesReply.getLogLastIndex() > context.getReplicatedLog().lastIndex()) {
            // The follower's log is actually ahead of the leader's log. Normally this doesn't happen
            // in raft as a node cannot become leader if it's log is behind another's. However, the
            // non-voting semantics deviate a bit from raft. Only voting members participate in
            // elections and can become leader so it's possible for a non-voting follower to be ahead
            // of the leader. This can happen if persistence is disabled and all voting members are
            // restarted. In this case, the voting leader will start out with an empty log however
            // the non-voting followers still retain the previous data in memory. On the first
            // AppendEntries, the non-voting follower returns a successful reply b/c the prevLogIndex
            // sent by the leader is -1 and thus the integrity checks pass. However the follower's returned
            // lastLogIndex may be higher in which case we want to reset the follower by installing a
            // snapshot. It's also possible that the follower's last log index is behind the leader's.
            // However in this case the log terms won't match and the logs will conflict - this is handled
            // elsewhere.
            log.info("{}: handleAppendEntriesReply: follower {} lastIndex {} is ahead of our lastIndex {} "
                    + "(snapshotIndex {}, snapshotTerm {}) - forcing install snaphot", logName(),
                    followerLogInformation.getId(), appendEntriesReply.getLogLastIndex(),
                    context.getReplicatedLog().lastIndex(), context.getReplicatedLog().getSnapshotIndex(),
                    context.getReplicatedLog().getSnapshotTerm());

            followerLogInformation.setMatchIndex(-1);
            followerLogInformation.setNextIndex(-1);

            initiateCaptureSnapshot(followerId);

            updated = true;
        } else if (appendEntriesReply.isSuccess()) {
            long followersLastLogTermInLeadersLog = getLogEntryTerm(followerLastLogIndex);
            if (followerLastLogIndex >= 0 && followersLastLogTermInLeadersLog >= 0
                    && followersLastLogTermInLeadersLog != appendEntriesReply.getLogLastTerm()) {
                // The follower's last entry is present in the leader's journal but the terms don't match so the
                // follower has a conflicting entry. Since the follower didn't report that it's out of sync, this means
                // either the previous leader entry sent didn't conflict or the previous leader entry is in the snapshot
                // and no longer in the journal. Either way, we set the follower's next index to 1 less than the last
                // index reported by the follower. For the former case, the leader will send all entries starting with
                // the previous follower's index and the follower will remove and replace the conflicting entries as
                // needed. For the latter, the leader will initiate an install snapshot.

                followerLogInformation.setNextIndex(followerLastLogIndex - 1);
                updated = true;

                log.info("{}: handleAppendEntriesReply: follower {} last log term {} for index {} conflicts with the "
                        + "leader's {} - set the follower's next index to {}", logName(),
                        followerId, appendEntriesReply.getLogLastTerm(), appendEntriesReply.getLogLastIndex(),
                        followersLastLogTermInLeadersLog, followerLogInformation.getNextIndex());
            } else {
                updated = updateFollowerLogInformation(followerLogInformation, appendEntriesReply);
            }
        } else {
            log.info("{}: handleAppendEntriesReply - received unsuccessful reply: {}, leader snapshotIndex: {}, "
                    + "snapshotTerm: {}, replicatedToAllIndex: {}", logName(), appendEntriesReply,
                    context.getReplicatedLog().getSnapshotIndex(), context.getReplicatedLog().getSnapshotTerm(),
                    getReplicatedToAllIndex());

            long followersLastLogTermInLeadersLogOrSnapshot = getLogEntryOrSnapshotTerm(followerLastLogIndex);
            if (appendEntriesReply.isForceInstallSnapshot()) {
                // Reset the followers match and next index. This is to signal that this follower has nothing
                // in common with this Leader and so would require a snapshot to be installed
                followerLogInformation.setMatchIndex(-1);
                followerLogInformation.setNextIndex(-1);

                // Force initiate a snapshot capture
                initiateCaptureSnapshot(followerId);
            } else if (followerLastLogIndex < 0 || followersLastLogTermInLeadersLogOrSnapshot >= 0
                    && followersLastLogTermInLeadersLogOrSnapshot == appendEntriesReply.getLogLastTerm()) {
                // The follower's log is empty or the follower's last entry is present in the leader's journal or
                // snapshot and the terms match so the follower is just behind the leader's journal from the last
                // snapshot, if any. We'll catch up the follower quickly by starting at the follower's last log index.

                updated = updateFollowerLogInformation(followerLogInformation, appendEntriesReply);

                log.info("{}: follower {} appears to be behind the leader from the last snapshot - "
                    + "updated: matchIndex: {}, nextIndex: {}", logName(), followerId,
                    followerLogInformation.getMatchIndex(), followerLogInformation.getNextIndex());
            } else {
                // The follower's log conflicts with leader's log so decrement follower's next index
                // in an attempt to find where the logs match.
                if (followerLogInformation.decrNextIndex(appendEntriesReply.getLogLastIndex())) {
                    updated = true;

                    log.info("{}: follower {} last log term {} conflicts with the leader's {} - dec next index to {}",
                            logName(), followerId, appendEntriesReply.getLogLastTerm(),
                            followersLastLogTermInLeadersLogOrSnapshot, followerLogInformation.getNextIndex());
                }
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("{}: handleAppendEntriesReply from {}: commitIndex: {}, lastAppliedIndex: {}, currentTerm: {}",
                    logName(), followerId, context.getCommitIndex(), context.getLastApplied(), currentTerm());
        }

        possiblyUpdateCommitIndex();

        //Send the next log entry immediately, if possible, no need to wait for heartbeat to trigger that event
        sendUpdatesToFollower(followerId, followerLogInformation, false, !updated);

        return this;
    }

    private void possiblyUpdateCommitIndex() {
        // Figure out if we can update the the commitIndex as follows:
        //   If there exists an index N such that N > commitIndex, a majority of matchIndex[i] ≥ N,
        //     and log[N].term == currentTerm:
        //   set commitIndex = N (§5.3, §5.4).
        for (long index = context.getCommitIndex() + 1; ; index++) {
            ReplicatedLogEntry replicatedLogEntry = context.getReplicatedLog().get(index);
            if (replicatedLogEntry == null) {
                log.trace("{}: ReplicatedLogEntry not found for index {} - snapshotIndex: {}, journal size: {}",
                        logName(), index, context.getReplicatedLog().getSnapshotIndex(),
                        context.getReplicatedLog().size());
                break;
            }

            // Count our entry if it has been persisted.
            int replicatedCount = replicatedLogEntry.isPersistencePending() ? 0 : 1;

            if (replicatedCount == 0) {
                // We don't commit and apply a log entry until we've gotten the ack from our local persistence,
                // even though there *shouldn't* be any issue with updating the commit index if we get a consensus
                // amongst the followers w/o the local persistence ack.
                break;
            }

            log.trace("{}: checking Nth index {}", logName(), index);
            for (FollowerLogInformation info : followerToLog.values()) {
                final PeerInfo peerInfo = context.getPeerInfo(info.getId());
                if (info.getMatchIndex() >= index && peerInfo != null && peerInfo.isVoting()) {
                    replicatedCount++;
                } else if (log.isTraceEnabled()) {
                    log.trace("{}: Not counting follower {} - matchIndex: {}, {}", logName(), info.getId(),
                            info.getMatchIndex(), peerInfo);
                }
            }

            if (log.isTraceEnabled()) {
                log.trace("{}: replicatedCount {}, minReplicationCount: {}", logName(), replicatedCount,
                        minReplicationCount);
            }

            if (replicatedCount >= minReplicationCount) {
                // Don't update the commit index if the log entry is from a previous term, as per §5.4.1:
                // "Raft never commits log entries from previous terms by counting replicas".
                // However we keep looping so we can make progress when new entries in the current term
                // reach consensus, as per §5.4.1: "once an entry from the current term is committed by
                // counting replicas, then all prior entries are committed indirectly".
                if (replicatedLogEntry.getTerm() == currentTerm()) {
                    log.trace("{}: Setting commit index to {}", logName(), index);
                    context.setCommitIndex(index);
                } else {
                    log.debug("{}: Not updating commit index to {} - retrieved log entry with index {}, "
                            + "term {} does not match the current term {}", logName(), index,
                            replicatedLogEntry.getIndex(), replicatedLogEntry.getTerm(), currentTerm());
                }
            } else {
                log.trace("{}: minReplicationCount not reached, actual {} - breaking", logName(), replicatedCount);
                break;
            }
        }

        // Apply the change to the state machine
        if (context.getCommitIndex() > context.getLastApplied()) {
            log.debug("{}: Applying to log - commitIndex: {}, lastAppliedIndex: {}", logName(),
                    context.getCommitIndex(), context.getLastApplied());

            applyLogToStateMachine(context.getCommitIndex());
        }

        if (!context.getSnapshotManager().isCapturing()) {
            purgeInMemoryLog();
        }
    }

    private boolean updateFollowerLogInformation(final FollowerLogInformation followerLogInformation,
            final AppendEntriesReply appendEntriesReply) {
        boolean updated = followerLogInformation.setMatchIndex(appendEntriesReply.getLogLastIndex());
        updated = followerLogInformation.setNextIndex(appendEntriesReply.getLogLastIndex() + 1) || updated;

        if (updated && log.isDebugEnabled()) {
            log.debug(
                "{}: handleAppendEntriesReply - FollowerLogInformation for {} updated: matchIndex: {}, nextIndex: {}",
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
    protected ClientRequestTracker removeClientRequestTracker(final long logIndex) {
        final Iterator<ClientRequestTracker> it = trackers.iterator();
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
    protected RaftActorBehavior handleRequestVoteReply(final ActorRef sender, final RequestVoteReply requestVoteReply) {
        return this;
    }

    protected void beforeSendHeartbeat() {
        // No-op
    }

    @Override
    public RaftActorBehavior handleMessage(final ActorRef sender, final Object message) {
        requireNonNull(sender, "sender should not be null");

        if (appendEntriesMessageSlicer.handleMessage(message)) {
            return this;
        }

        if (message instanceof RaftRPC) {
            RaftRPC rpc = (RaftRPC) message;
            // If RPC request or response contains term T > currentTerm:
            // set currentTerm = T, convert to follower (§5.1)
            // This applies to all RPC messages and responses
            if (rpc.getTerm() > context.getTermInformation().getCurrentTerm() && shouldUpdateTerm(rpc)) {
                log.info("{}: Term {} in \"{}\" message is greater than leader's term {} - switching to Follower",
                        logName(), rpc.getTerm(), rpc, context.getTermInformation().getCurrentTerm());

                context.getTermInformation().updateAndPersist(rpc.getTerm(), null);

                // This is a special case. Normally when stepping down as leader we don't process and reply to the
                // RaftRPC as per raft. But if we're in the process of transferring leadership and we get a
                // RequestVote, process the RequestVote before switching to Follower. This enables the requesting
                // candidate node to be elected the leader faster and avoids us possibly timing out in the Follower
                // state and starting a new election and grabbing leadership back before the other candidate node can
                // start a new election due to lack of responses. This case would only occur if there isn't a majority
                // of other nodes available that can elect the requesting candidate. Since we're transferring
                // leadership, we should make every effort to get the requesting node elected.
                if (rpc instanceof RequestVote && context.getRaftActorLeadershipTransferCohort() != null) {
                    log.debug("{}: Leadership transfer in progress - processing RequestVote", logName());
                    super.handleMessage(sender, rpc);
                }

                return internalSwitchBehavior(RaftState.Follower);
            }
        }

        if (message instanceof SendHeartBeat) {
            beforeSendHeartbeat();
            sendHeartBeat();
            scheduleHeartBeat(context.getConfigParams().getHeartBeatInterval());
        } else if (message instanceof SendInstallSnapshot) {
            SendInstallSnapshot sendInstallSnapshot = (SendInstallSnapshot) message;
            setSnapshotHolder(new SnapshotHolder(sendInstallSnapshot.getSnapshot(),
                sendInstallSnapshot.getSnapshotBytes()));
            sendInstallSnapshot();
        } else if (message instanceof Replicate) {
            replicate((Replicate) message);
        } else if (message instanceof InstallSnapshotReply) {
            handleInstallSnapshotReply((InstallSnapshotReply) message);
        } else if (message instanceof InstallSnapshotFinished) {
            handleInstallSnapshotFinished((InstallSnapshotFinished) message);
        } else if (message instanceof CheckConsensusReached) {
            possiblyUpdateCommitIndex();
        } else {
            return super.handleMessage(sender, message);
        }

        return this;
    }

    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS",
            justification = "JDT nullness with SpotBugs at setSnapshotHolder(null)")
    private void handleInstallSnapshotReply(final InstallSnapshotReply reply) {
        log.debug("{}: handleInstallSnapshotReply: {}", logName(), reply);

        String followerId = reply.getFollowerId();
        FollowerLogInformation followerLogInformation = followerToLog.get(followerId);
        if (followerLogInformation == null) {
            // This can happen during AddServer if it times out.
            log.error("{}: FollowerLogInformation not found for follower {} in InstallSnapshotReply",
                    logName(), followerId);
            return;
        }

        LeaderInstallSnapshotState installSnapshotState = followerLogInformation.getInstallSnapshotState();
        if (installSnapshotState == null) {
            log.error("{}: LeaderInstallSnapshotState not found for follower {} in InstallSnapshotReply",
                    logName(), followerId);
            return;
        }

        installSnapshotState.resetChunkTimer();
        followerLogInformation.markFollowerActive();

        if (installSnapshotState.getChunkIndex() == reply.getChunkIndex()) {
            if (reply.isSuccess()) {
                installSnapshotState.markSendStatus(true);
            } else {
                log.warn("{}: Received failed InstallSnapshotReply - will retry: {}", logName(), reply);

                installSnapshotState.markSendStatus(false);
            }

            if (!installSnapshotState.isLastChunk(reply.getChunkIndex())) {
                ActorSelection followerActor = context.getPeerActorSelection(followerId);
                if (followerActor != null) {
                    sendSnapshotChunk(followerActor, followerLogInformation);
                }
            }

        } else {
            log.error("{}: Chunk index {} in InstallSnapshotReply from follower {} does not match expected index {}",
                    logName(), reply.getChunkIndex(), followerId,
                    installSnapshotState.getChunkIndex());

            if (reply.getChunkIndex() == LeaderInstallSnapshotState.INVALID_CHUNK_INDEX) {
                // Since the Follower did not find this index to be valid we should reset the follower snapshot
                // so that Installing the snapshot can resume from the beginning
                installSnapshotState.reset();
            }
        }
    }

    private void handleInstallSnapshotFinished(final InstallSnapshotFinished reply) {
        log.debug("{}: handleInstallSnapshotReply: {}", logName(), reply);

        String followerId = reply.getFollowerId();
        FollowerLogInformation followerLogInformation = followerToLog.get(followerId);
        if (followerLogInformation == null) {
            // This can happen during AddServer if it times out.
            log.error("{}: FollowerLogInformation not found for follower {} in InstallSnapshotReply",
                    logName(), followerId);
            return;
        }

        LeaderInstallSnapshotState installSnapshotState = followerLogInformation.getInstallSnapshotState();
        if (installSnapshotState == null) {
            log.error("{}: LeaderInstallSnapshotState not found for follower {} in InstallSnapshotReply",
                    logName(), followerId);
            return;
        }

        if (!reply.isSuccess()) {
            log.warn("{}: Snapshot installation failed, resetting and retrying. {}", logName(), reply);

            installSnapshotState.reset();

            ActorSelection followerActor = context.getPeerActorSelection(followerId);
            if (followerActor != null) {
                sendSnapshotChunk(followerActor, followerLogInformation);
            }
            return;
        }

        long followerMatchIndex = snapshotHolder.get().getLastIncludedIndex();
        followerLogInformation.setMatchIndex(followerMatchIndex);
        followerLogInformation.setNextIndex(followerMatchIndex + 1);
        followerLogInformation.clearLeaderInstallSnapshotState();

        log.info("{}: Snapshot successfully installed on follower {} (last chunk {}) - "
                        + "matchIndex set to {}, nextIndex set to {}", logName(), followerId,
                installSnapshotState.getChunkIndex(), followerLogInformation.getMatchIndex(),
                followerLogInformation.getNextIndex());

        if (!anyFollowersInstallingSnapshot()) {
            // once there are no pending followers receiving snapshots
            // we can remove snapshot from the memory
            setSnapshotHolder(null);
        }

        if (context.getPeerInfo(followerId).getVotingState() == VotingState.VOTING_NOT_INITIALIZED) {
            UnInitializedFollowerSnapshotReply unInitFollowerSnapshotSuccess =
                    new UnInitializedFollowerSnapshotReply(followerId);
            context.getActor().tell(unInitFollowerSnapshotSuccess, context.getActor());
            log.debug("Sent message UnInitializedFollowerSnapshotReply to self");
        }

        if (!context.getSnapshotManager().isCapturing()) {
            // Since the follower is now caught up try to purge the log.
            purgeInMemoryLog();
        }
    }

    private boolean anyFollowersInstallingSnapshot() {
        for (FollowerLogInformation info: followerToLog.values()) {
            if (info.getInstallSnapshotState() != null) {
                return true;
            }

        }

        return false;
    }

    private void replicate(final Replicate replicate) {
        long logIndex = replicate.getReplicatedLogEntry().getIndex();

        log.debug("{}: Replicate message: identifier: {}, logIndex: {}, payload: {}, isSendImmediate: {}", logName(),
                replicate.getIdentifier(), logIndex, replicate.getReplicatedLogEntry().getData().getClass(),
                replicate.isSendImmediate());

        // Create a tracker entry we will use this later to notify the
        // client actor
        if (replicate.getClientActor() != null) {
            trackers.add(new ClientRequestTrackerImpl(replicate.getClientActor(), replicate.getIdentifier(),
                    logIndex));
        }

        boolean applyModificationToState = !context.anyVotingPeers()
                || context.getRaftPolicy().applyModificationToStateBeforeConsensus();

        if (applyModificationToState) {
            context.setCommitIndex(logIndex);
            applyLogToStateMachine(logIndex);
        }

        if (replicate.isSendImmediate() && !followerToLog.isEmpty()) {
            sendAppendEntries(0, false);
        }
    }

    protected void sendAppendEntries(final long timeSinceLastActivityIntervalNanos, final boolean isHeartbeat) {
        // Send an AppendEntries to all followers
        for (Entry<String, FollowerLogInformation> e : followerToLog.entrySet()) {
            final String followerId = e.getKey();
            final FollowerLogInformation followerLogInformation = e.getValue();
            // This checks helps not to send a repeat message to the follower
            if (!followerLogInformation.isFollowerActive()
                    || followerLogInformation.nanosSinceLastActivity() >= timeSinceLastActivityIntervalNanos) {
                sendUpdatesToFollower(followerId, followerLogInformation, true, isHeartbeat);
            }
        }
    }

    /**
     * This method checks if any update needs to be sent to the given follower. This includes append log entries,
     * sending next snapshot chunk, and initiating a snapshot.
     */
    private void sendUpdatesToFollower(final String followerId, final FollowerLogInformation followerLogInformation,
                                       final boolean sendHeartbeat, final boolean isHeartbeat) {

        ActorSelection followerActor = context.getPeerActorSelection(followerId);
        if (followerActor != null) {
            long followerNextIndex = followerLogInformation.getNextIndex();
            boolean isFollowerActive = followerLogInformation.isFollowerActive();
            boolean sendAppendEntries = false;
            List<ReplicatedLogEntry> entries = Collections.emptyList();

            LeaderInstallSnapshotState installSnapshotState = followerLogInformation.getInstallSnapshotState();
            if (installSnapshotState != null) {

                // if install snapshot is in process , then sent next chunk if possible
                if (isFollowerActive) {

                    if (installSnapshotState.isChunkTimedOut()) {
                        sendAppendEntries = !resendSnapshotChunk(followerActor, followerLogInformation);
                    } else if (installSnapshotState.canSendNextChunk()) {
                        sendSnapshotChunk(followerActor, followerLogInformation);
                    }
                } else if (sendHeartbeat || followerLogInformation.hasStaleCommitIndex(context.getCommitIndex())) {
                    // we send a heartbeat even if we have not received a reply for the last chunk
                    sendAppendEntries = true;
                }
            } else if (followerLogInformation.isLogEntrySlicingInProgress()) {
                sendAppendEntries = sendHeartbeat;
            } else {
                long leaderLastIndex = context.getReplicatedLog().lastIndex();
                long leaderSnapShotIndex = context.getReplicatedLog().getSnapshotIndex();

                if (!isHeartbeat && log.isDebugEnabled() || log.isTraceEnabled()) {
                    log.debug("{}: Checking sendAppendEntries for follower {}: active: {}, followerNextIndex: {}, "
                            + "leaderLastIndex: {}, leaderSnapShotIndex: {}", logName(), followerId, isFollowerActive,
                            followerNextIndex, leaderLastIndex, leaderSnapShotIndex);
                }

                if (isFollowerActive && context.getReplicatedLog().isPresent(followerNextIndex)) {

                    log.debug("{}: sendAppendEntries: {} is present for follower {}", logName(),
                            followerNextIndex, followerId);

                    if (followerLogInformation.okToReplicate(context.getCommitIndex())) {
                        entries = getEntriesToSend(followerLogInformation, followerActor);
                        sendAppendEntries = true;
                    }
                } else if (isFollowerActive && followerNextIndex >= 0
                        && leaderLastIndex > followerNextIndex && !context.getSnapshotManager().isCapturing()) {
                    // if the followers next index is not present in the leaders log, and
                    // if the follower is just not starting and if leader's index is more than followers index
                    // then snapshot should be sent

                    // Send heartbeat to follower whenever install snapshot is initiated.
                    sendAppendEntries = true;
                    if (canInstallSnapshot(followerNextIndex)) {
                        log.info("{}: Initiating install snapshot to follower {}: follower nextIndex: {}, leader "
                                + "snapshotIndex: {}, leader lastIndex: {}, leader log size: {}", logName(), followerId,
                                followerNextIndex, leaderSnapShotIndex, leaderLastIndex,
                                context.getReplicatedLog().size());

                        initiateCaptureSnapshot(followerId);
                    } else {
                        // It doesn't seem like we should ever reach here - most likely indicates sonething is
                        // wrong.
                        log.info("{}: Follower {} is behind but cannot install snapshot: follower nextIndex: {}, "
                                + "leader snapshotIndex: {}, leader lastIndex: {}, leader log size: {}", logName(),
                                followerId, followerNextIndex, leaderSnapShotIndex, leaderLastIndex,
                                context.getReplicatedLog().size());
                    }

                } else if (sendHeartbeat || followerLogInformation.hasStaleCommitIndex(context.getCommitIndex())) {
                    // we send an AppendEntries, even if the follower is inactive
                    // in-order to update the followers timestamp, in case it becomes active again
                    sendAppendEntries = true;
                }

            }

            if (sendAppendEntries) {
                sendAppendEntriesToFollower(followerActor, entries, followerLogInformation);
            }
        }
    }

    private List<ReplicatedLogEntry> getEntriesToSend(final FollowerLogInformation followerLogInfo,
            final ActorSelection followerActor) {
        // Try to get all the entries in the journal but not exceeding the max data size for a single AppendEntries
        // message.
        int maxEntries = (int) context.getReplicatedLog().size();
        final int maxDataSize = context.getConfigParams().getSnapshotChunkSize();
        final long followerNextIndex = followerLogInfo.getNextIndex();
        List<ReplicatedLogEntry> entries = context.getReplicatedLog().getFrom(followerNextIndex,
                maxEntries, maxDataSize);

        // If the first entry's size exceeds the max data size threshold, it will be returned from the call above. If
        // that is the case, then we need to slice it into smaller chunks.
        if (!(entries.size() == 1 && entries.get(0).getData().size() > maxDataSize)) {
            // Don't need to slice.
            return entries;
        }

        log.debug("{}: Log entry size {} exceeds max payload size {}", logName(), entries.get(0).getData().size(),
                maxDataSize);

        // If an AppendEntries has already been serialized for the log index then reuse the
        // SharedFileBackedOutputStream.
        final Long logIndex = entries.get(0).getIndex();
        SharedFileBackedOutputStream fileBackedStream = sharedSerializedAppendEntriesStreams.get(logIndex);
        if (fileBackedStream == null) {
            fileBackedStream = context.getFileBackedOutputStreamFactory().newSharedInstance();

            final AppendEntries appendEntries = new AppendEntries(currentTerm(), context.getId(),
                    getLogEntryIndex(followerNextIndex - 1), getLogEntryTerm(followerNextIndex - 1), entries,
                    context.getCommitIndex(), getReplicatedToAllIndex(), context.getPayloadVersion());

            log.debug("{}: Serializing {} for slicing for follower {}", logName(), appendEntries,
                    followerLogInfo.getId());

            try (ObjectOutputStream out = new ObjectOutputStream(fileBackedStream)) {
                out.writeObject(appendEntries);
            } catch (IOException e) {
                log.error("{}: Error serializing {}", logName(), appendEntries, e);
                fileBackedStream.cleanup();
                return Collections.emptyList();
            }

            sharedSerializedAppendEntriesStreams.put(logIndex, fileBackedStream);

            fileBackedStream.setOnCleanupCallback(index -> {
                log.debug("{}: On SharedFileBackedOutputStream cleanup for index {}", logName(), index);
                sharedSerializedAppendEntriesStreams.remove(index);
            }, logIndex);
        } else {
            log.debug("{}: Reusing SharedFileBackedOutputStream for follower {}", logName(), followerLogInfo.getId());
            fileBackedStream.incrementUsageCount();
        }

        log.debug("{}: Slicing stream for index {}, follower {}", logName(), logIndex, followerLogInfo.getId());

        // Record that slicing is in progress for the follower.
        followerLogInfo.setSlicedLogEntryIndex(logIndex);

        final FollowerIdentifier identifier = new FollowerIdentifier(followerLogInfo.getId());
        appendEntriesMessageSlicer.slice(SliceOptions.builder().identifier(identifier)
                .fileBackedOutputStream(fileBackedStream).sendTo(followerActor).replyTo(actor())
                .onFailureCallback(failure -> {
                    log.error("{}: Error slicing AppendEntries for follower {}", logName(),
                            followerLogInfo.getId(), failure);
                    followerLogInfo.setSlicedLogEntryIndex(FollowerLogInformation.NO_INDEX);
                }).build());

        return Collections.emptyList();
    }

    private void sendAppendEntriesToFollower(final ActorSelection followerActor, final List<ReplicatedLogEntry> entries,
            final FollowerLogInformation followerLogInformation) {
        // In certain cases outlined below we don't want to send the actual commit index to prevent the follower from
        // possibly committing and applying conflicting entries (those with same index, different term) from a prior
        // term that weren't replicated to a majority, which would be a violation of raft.
        //     - if the follower isn't active. In this case we don't know the state of the follower and we send an
        //       empty AppendEntries as a heart beat to prevent election.
        //     - if we're in the process of installing a snapshot. In this case we don't send any new entries but still
        //       need to send AppendEntries to prevent election.
        //     - if we're in the process of slicing an AppendEntries with a large log entry payload. In this case we
        //       need to send an empty AppendEntries to prevent election.
        boolean isInstallingSnaphot = followerLogInformation.getInstallSnapshotState() != null;
        long leaderCommitIndex = isInstallingSnaphot || followerLogInformation.isLogEntrySlicingInProgress()
                || !followerLogInformation.isFollowerActive() ? -1 : context.getCommitIndex();

        long followerNextIndex = followerLogInformation.getNextIndex();
        AppendEntries appendEntries = new AppendEntries(currentTerm(), context.getId(),
            getLogEntryIndex(followerNextIndex - 1),
            getLogEntryTerm(followerNextIndex - 1), entries,
            leaderCommitIndex, super.getReplicatedToAllIndex(), context.getPayloadVersion(),
            followerLogInformation.getRaftVersion(), followerLogInformation.needsLeaderAddress(getId()));

        if (!entries.isEmpty() || log.isTraceEnabled()) {
            log.debug("{}: Sending AppendEntries to follower {}: {}", logName(), followerLogInformation.getId(),
                    appendEntries);
        }

        followerLogInformation.setSentCommitIndex(leaderCommitIndex);
        followerActor.tell(appendEntries, actor());
    }

    /**
     * Initiates a snapshot capture to install on a follower.
     *
     * <p>
     * Install Snapshot works as follows
     *   1. Leader initiates the capture snapshot by calling createSnapshot on the RaftActor.
     *   2. On receipt of the CaptureSnapshotReply message, the RaftActor persists the snapshot and makes a call to
     *      the Leader's handleMessage with a SendInstallSnapshot message.
     *   3. The Leader obtains and stores the Snapshot from the SendInstallSnapshot message and sends it in chunks to
     *      the Follower via InstallSnapshot messages.
     *   4. For each chunk, the Follower sends back an InstallSnapshotReply.
     *   5. On receipt of the InstallSnapshotReply for the last chunk, the Leader marks the install complete for that
     *      follower.
     *   6. If another follower requires a snapshot and a snapshot has been collected (via SendInstallSnapshot)
     *      then send the existing snapshot in chunks to the follower.
     *
     * @param followerId the id of the follower.
     * @return true if capture was initiated, false otherwise.
     */
    public boolean initiateCaptureSnapshot(final String followerId) {
        FollowerLogInformation followerLogInfo = followerToLog.get(followerId);
        if (snapshotHolder.isPresent()) {
            // If a snapshot is present in the memory, most likely another install is in progress no need to capture
            // snapshot. This could happen if another follower needs an install when one is going on.
            final ActorSelection followerActor = context.getPeerActorSelection(followerId);

            // Note: sendSnapshotChunk will set the LeaderInstallSnapshotState.
            sendSnapshotChunk(followerActor, followerLogInfo);
            return true;
        }

        boolean captureInitiated = context.getSnapshotManager().captureToInstall(context.getReplicatedLog().last(),
            this.getReplicatedToAllIndex(), followerId);
        if (captureInitiated) {
            followerLogInfo.setLeaderInstallSnapshotState(new LeaderInstallSnapshotState(
                context.getConfigParams().getSnapshotChunkSize(), createChunkTimeout(), logName()));
        }

        return captureInitiated;
    }

    private boolean canInstallSnapshot(final long nextIndex) {
        // If the follower's nextIndex is -1 then we might as well send it a snapshot
        // Otherwise send it a snapshot only if the nextIndex is not present in the log but is present
        // in the snapshot
        return nextIndex == -1 || !context.getReplicatedLog().isPresent(nextIndex)
                && context.getReplicatedLog().isInSnapshot(nextIndex);

    }


    private void sendInstallSnapshot() {
        log.debug("{}: sendInstallSnapshot", logName());
        for (Entry<String, FollowerLogInformation> e : followerToLog.entrySet()) {
            String followerId = e.getKey();
            ActorSelection followerActor = context.getPeerActorSelection(followerId);
            FollowerLogInformation followerLogInfo = e.getValue();

            if (followerActor != null) {
                long nextIndex = followerLogInfo.getNextIndex();
                if (followerLogInfo.getInstallSnapshotState() != null
                        || context.getPeerInfo(followerId).getVotingState() == VotingState.VOTING_NOT_INITIALIZED
                        || canInstallSnapshot(nextIndex)) {
                    sendSnapshotChunk(followerActor, followerLogInfo);
                }
            }
        }
    }

    /**
     *  Sends a snapshot chunk to a given follower
     *  InstallSnapshot should qualify as a heartbeat too.
     */
    private void sendSnapshotChunk(final ActorSelection followerActor, final FollowerLogInformation followerLogInfo) {
        if (snapshotHolder.isPresent()) {
            LeaderInstallSnapshotState installSnapshotState = followerLogInfo.getInstallSnapshotState();
            if (installSnapshotState == null) {
                installSnapshotState = new LeaderInstallSnapshotState(context.getConfigParams().getSnapshotChunkSize(),
                        createChunkTimeout(), logName());
                followerLogInfo.setLeaderInstallSnapshotState(installSnapshotState);
            }

            try {
                // Ensure the snapshot bytes are set - this is a no-op.
                installSnapshotState.setSnapshotBytes(snapshotHolder.get().getSnapshotBytes());

                if (!installSnapshotState.canSendNextChunk()) {
                    return;
                }

                byte[] nextSnapshotChunk = installSnapshotState.getNextChunk();

                log.debug("{}: next snapshot chunk size for follower {}: {}", logName(), followerLogInfo.getId(),
                        nextSnapshotChunk.length);

                int nextChunkIndex = installSnapshotState.incrementChunkIndex();
                Optional<ServerConfigurationPayload> serverConfig = Optional.empty();
                if (installSnapshotState.isLastChunk(nextChunkIndex)) {
                    serverConfig = Optional.ofNullable(context.getPeerServerInfo(true));
                }

                sendSnapshotChunk(followerActor, followerLogInfo, nextSnapshotChunk, nextChunkIndex, serverConfig);

                log.debug("{}: InstallSnapshot sent to follower {}, Chunk: {}/{}", logName(), followerActor.path(),
                        installSnapshotState.getChunkIndex(), installSnapshotState.getTotalChunks());

            } catch (IOException e) {
                log.warn("{}: Unable to send chunk: {}/{}. Reseting snapshot progress. Snapshot state: {}", logName(),
                        installSnapshotState.getChunkIndex(), installSnapshotState.getTotalChunks(),
                        installSnapshotState);
                installSnapshotState.reset();
            }
        }
    }

    private void sendSnapshotChunk(final ActorSelection followerActor, final FollowerLogInformation followerLogInfo,
                                   final byte[] snapshotChunk, final int chunkIndex,
                                   final Optional<ServerConfigurationPayload> serverConfig) {
        LeaderInstallSnapshotState installSnapshotState = followerLogInfo.getInstallSnapshotState();

        installSnapshotState.startChunkTimer();
        followerActor.tell(
                new InstallSnapshot(currentTerm(), context.getId(),
                        snapshotHolder.get().getLastIncludedIndex(),
                        snapshotHolder.get().getLastIncludedTerm(),
                        snapshotChunk,
                        chunkIndex,
                        installSnapshotState.getTotalChunks(),
                        OptionalInt.of(installSnapshotState.getLastChunkHashCode()),
                        serverConfig
                ).toSerializable(followerLogInfo.getRaftVersion()),
                actor()
        );
    }

    private boolean resendSnapshotChunk(final ActorSelection followerActor,
                                        final FollowerLogInformation followerLogInfo) {
        if (!snapshotHolder.isPresent()) {
            // Seems like we should never hit this case, but just in case we do, reset the snapshot progress so that it
            // can restart from the next AppendEntries.
            log.warn("{}: Attempting to resend snapshot with no snapshot holder present.", logName());
            followerLogInfo.clearLeaderInstallSnapshotState();
            return false;
        }

        LeaderInstallSnapshotState installSnapshotState = followerLogInfo.getInstallSnapshotState();
        // we are resending, timer needs to be reset
        installSnapshotState.resetChunkTimer();
        installSnapshotState.markSendStatus(false);

        sendSnapshotChunk(followerActor, followerLogInfo);

        return true;
    }

    private void sendHeartBeat() {
        if (!followerToLog.isEmpty()) {
            log.trace("{}: Sending heartbeat", logName());
            sendAppendEntries(context.getConfigParams().getHeartBeatInterval().toNanos(), true);

            appendEntriesMessageSlicer.checkExpiredSlicedMessageState();
        }
    }

    private void stopHeartBeat() {
        if (heartbeatSchedule != null && !heartbeatSchedule.isCancelled()) {
            heartbeatSchedule.cancel();
        }
    }

    private void scheduleHeartBeat(final FiniteDuration interval) {
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

    private FiniteDuration createChunkTimeout() {
        // 30 seconds with default settings, can be modified via heartbeat or election timeout factor
        return context.getConfigParams().getHeartBeatInterval()
                .$times(context.getConfigParams().getElectionTimeoutFactor() * 3);
    }

    @Override
    public void close() {
        stopHeartBeat();
        appendEntriesMessageSlicer.close();
    }

    @Override
    public final String getLeaderId() {
        return context.getId();
    }

    @Override
    public final short getLeaderPayloadVersion() {
        return context.getPayloadVersion();
    }

    protected boolean isLeaderIsolated() {
        int minPresent = getMinIsolatedLeaderPeerCount();
        for (FollowerLogInformation followerLogInformation : followerToLog.values()) {
            final PeerInfo peerInfo = context.getPeerInfo(followerLogInformation.getId());
            if (peerInfo != null && peerInfo.isVoting() && followerLogInformation.isFollowerActive()) {
                --minPresent;
                if (minPresent == 0) {
                    return false;
                }
            }
        }
        return minPresent != 0;
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
    public FollowerLogInformation getFollower(final String followerId) {
        return followerToLog.get(followerId);
    }

    @VisibleForTesting
    public int followerLogSize() {
        return followerToLog.size();
    }

    static class SnapshotHolder {
        private final long lastIncludedTerm;
        private final long lastIncludedIndex;
        private final ByteSource snapshotBytes;

        SnapshotHolder(final Snapshot snapshot, final ByteSource snapshotBytes) {
            this.lastIncludedTerm = snapshot.getLastAppliedTerm();
            this.lastIncludedIndex = snapshot.getLastAppliedIndex();
            this.snapshotBytes = snapshotBytes;
        }

        long getLastIncludedTerm() {
            return lastIncludedTerm;
        }

        long getLastIncludedIndex() {
            return lastIncludedIndex;
        }

        ByteSource getSnapshotBytes() {
            return snapshotBytes;
        }
    }
}
