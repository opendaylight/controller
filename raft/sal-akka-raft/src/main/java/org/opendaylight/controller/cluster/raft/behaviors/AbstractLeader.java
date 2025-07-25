/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.Cancellable;
import org.apache.pekko.dispatch.ControlMessage;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.messaging.MessageSlicer;
import org.opendaylight.controller.cluster.messaging.SliceOptions;
import org.opendaylight.controller.cluster.raft.ClientRequestTracker;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;
import org.opendaylight.controller.cluster.raft.PeerInfo;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.VotingState;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.IdentifiablePayload;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.messages.UnInitializedFollowerSnapshotReply;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.raft.api.RaftRole;
import org.opendaylight.raft.api.TermInfo;
import org.opendaylight.raft.spi.ByteArray;
import org.opendaylight.raft.spi.InstallableSnapshot;
import org.opendaylight.raft.spi.InstallableSnapshotSource;
import org.opendaylight.raft.spi.PlainSnapshotSource;
import org.opendaylight.raft.spi.SharedFileBackedOutputStream;
import org.opendaylight.raft.spi.StreamSource;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The behavior of a RaftActor when it is in the Leader state.
 *
 * <p>Leaders:
 * <ul>
 *   <li>Upon election: send initial empty {@linkplain AppendEntries} RPCs (heartbeat) to each server; repeat during
 *       idle periods to prevent election timeouts (§5.2)</li>
 *   <li>If command received from client: append entry to local log, respond after entry applied to state machine (§5.3)
 *   </li>
 *   <li>If last log index ≥ nextIndex for a follower: send {@linkplain AppendEntries} RPC with log entries starting at
 *       nextIndex</li>
 *   <li>If successful: update nextIndex and matchIndex for follower (§5.3)</li>
 *   <li>If {@linkplain AppendEntries} fails because of log inconsistency: decrement nextIndex and retry (§5.3)</li>
 *   <li>If there exists an N such that {@code N > commitIndex}, a majority of {@code matchIndex[i] ≥ N}, and
 *       {@code log[N].term == currentTerm}: set {@code commitIndex = N} (§5.3, §5.4)</li>
 * </ul>
 */
public abstract sealed class AbstractLeader extends RaftActorBehavior permits IsolatedLeader, Leader, PreLeader {
    /**
     * This messages is sent via a schedule to the Leader to prompt it to send a heart beat to its followers.
     */
    @NonNullByDefault
    @VisibleForTesting
    public static final class SendHeartBeat implements ControlMessage {
        public static final SendHeartBeat INSTANCE = new SendHeartBeat();

        private SendHeartBeat() {
            // Hidden on purpose
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractLeader.class);

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
    private InstallableSnapshot snapshotHolder = null;
    private int minReplicationCount;

    AbstractLeader(final RaftActorContext context, final RaftRole state,
            final @Nullable AbstractLeader initializeFromLeader) {
        super(context, state);

        appendEntriesMessageSlicer = MessageSlicer.builder()
            .logContext(logName)
            .messageSliceSize(context.getConfigParams().getMaximumMessageSliceSize())
            .expireStateAfterInactivity(
                context.getConfigParams().getElectionTimeOutInterval().toMillis() * 3, TimeUnit.MILLISECONDS)
            .build();

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

        LOG.debug("{}: Election: Leader has following peers: {}", logName, getFollowerIds());

        updateMinReplicaCount();

        // Immediately schedule a heartbeat
        // Upon election: send initial empty AppendEntries RPCs
        // (heartbeat) to each server; repeat during idle periods to
        // prevent election timeouts (§5.2)
        sendAppendEntries(0, false);

        // It is important to schedule this heartbeat here
        scheduleHeartBeat(context.getConfigParams().getHeartBeatInterval());
    }

    AbstractLeader(final RaftActorContext context, final RaftRole state) {
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
        followerToLog.put(followerId, new FollowerLogInformation(context.getPeerInfo(followerId), context));
        if (heartbeatSchedule == null) {
            scheduleHeartBeat(context.getConfigParams().getHeartBeatInterval());
        }
    }

    public void removeFollower(final String followerId) {
        followerToLog.remove(followerId);
    }

    public final void updateMinReplicaCount() {
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

    @Override
    final RaftActorBehavior handleAppendEntries(final ActorRef sender, final AppendEntries appendEntries) {
        LOG.debug("{}: handleAppendEntries: {}", logName, appendEntries);
        return this;
    }

    // handleAppendEntriesReply() without switching behaviors
    final void processAppendEntriesReply(final ActorRef sender, final AppendEntriesReply appendEntriesReply) {
        LOG.trace("{}: handleAppendEntriesReply: {}", logName, appendEntriesReply);

        // Update the FollowerLogInformation
        String followerId = appendEntriesReply.getFollowerId();
        FollowerLogInformation followerLogInformation = followerToLog.get(followerId);

        if (followerLogInformation == null) {
            LOG.error("{}: handleAppendEntriesReply - unknown follower {}", logName, followerId);
            return;
        }

        final var followerRaftVersion = appendEntriesReply.getRaftVersion();
        if (followerRaftVersion < RaftVersions.FLUORINE_VERSION) {
            LOG.warn("{}: handleAppendEntriesReply - ignoring reply from follower {} raft version {}", logName,
                followerId, followerRaftVersion);
            return;
        }

        final var replLog = context.getReplicatedLog();
        final long lastActivityNanos = followerLogInformation.nanosSinceLastActivity();
        if (lastActivityNanos > context.getConfigParams().getElectionTimeOutInterval().toNanos()) {
            LOG.warn("{} : handleAppendEntriesReply delayed beyond election timeout, "
                    + "appendEntriesReply : {}, timeSinceLastActivity : {}, lastApplied : {}, commitIndex : {}",
                    logName, appendEntriesReply, TimeUnit.NANOSECONDS.toMillis(lastActivityNanos),
                    replLog.getLastApplied(), replLog.getCommitIndex());
        }

        followerLogInformation.markFollowerActive();
        followerLogInformation.setPayloadVersion(appendEntriesReply.getPayloadVersion());
        followerLogInformation.setRaftVersion(followerRaftVersion);
        followerLogInformation.setNeedsLeaderAddress(appendEntriesReply.isNeedsLeaderAddress());

        long followerLastLogIndex = appendEntriesReply.getLogLastIndex();
        boolean updated = false;
        if (appendEntriesReply.getLogLastIndex() > replLog.lastIndex()) {
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
            LOG.info("{}: handleAppendEntriesReply: follower {} lastIndex {} is ahead of our lastIndex {} "
                    + "(snapshotIndex {}, snapshotTerm {}) - forcing install snaphot", logName,
                    followerLogInformation.getId(), appendEntriesReply.getLogLastIndex(),
                    replLog.lastIndex(), replLog.getSnapshotIndex(), replLog.getSnapshotTerm());

            followerLogInformation.setMatchIndex(-1);
            followerLogInformation.setNextIndex(-1);

            initiateCaptureSnapshot(followerId);

            updated = true;
        } else if (appendEntriesReply.isSuccess()) {
            long followersLastLogTermInLeadersLog = replLog.getLogEntryTerm(followerLastLogIndex);
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

                LOG.info("{}: handleAppendEntriesReply: follower {} last log term {} for index {} conflicts with the "
                        + "leader's {} - set the follower's next index to {}", logName,
                        followerId, appendEntriesReply.getLogLastTerm(), appendEntriesReply.getLogLastIndex(),
                        followersLastLogTermInLeadersLog, followerLogInformation.getNextIndex());
            } else {
                updated = updateFollowerLogInformation(followerLogInformation, appendEntriesReply);
            }
        } else {
            LOG.info("{}: handleAppendEntriesReply - received unsuccessful reply: {}, leader snapshotIndex: {}, "
                    + "snapshotTerm: {}, replicatedToAllIndex: {}", logName, appendEntriesReply,
                    replLog.getSnapshotIndex(), replLog.getSnapshotTerm(), getReplicatedToAllIndex());

            long followersLastLogTermInLeadersLogOrSnapshot = replLog.getLogEntryOrSnapshotTerm(followerLastLogIndex);
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

                LOG.info("{}: follower {} appears to be behind the leader from the last snapshot - "
                    + "updated: matchIndex: {}, nextIndex: {}", logName, followerId,
                    followerLogInformation.getMatchIndex(), followerLogInformation.getNextIndex());
            } else if (followerLogInformation.decrNextIndex(appendEntriesReply.getLogLastIndex())) {
                // The follower's log conflicts with leader's log so decrement follower's next index in an attempt
                // to find where the logs match.
                updated = true;

                LOG.info("{}: follower {} last log term {} conflicts with the leader's {} - dec next index to {}",
                        logName, followerId, appendEntriesReply.getLogLastTerm(),
                        followersLastLogTermInLeadersLogOrSnapshot, followerLogInformation.getNextIndex());
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("{}: handleAppendEntriesReply from {}: commitIndex: {}, lastAppliedIndex: {}, currentTerm: {}",
                    logName, followerId, replLog.getCommitIndex(), replLog.getLastApplied(), currentTerm());
        }

        possiblyUpdateCommitIndex();

        //Send the next log entry immediately, if possible, no need to wait for heartbeat to trigger that event
        sendUpdatesToFollower(followerId, followerLogInformation, false, !updated);
    }

    // Invoked after persistence is complete to check if replication consensus has been reached.
    public final void checkConsensusReached() {
        possiblyUpdateCommitIndex();
    }

    private void possiblyUpdateCommitIndex() {
        // Figure out if we can update the the commitIndex as follows:
        //   If there exists an index N such that N > commitIndex, a majority of matchIndex[i] ≥ N,
        //     and log[N].term == currentTerm:
        //   set commitIndex = N (§5.3, §5.4).
        final var replLog = context.getReplicatedLog();
        for (long index = replLog.getCommitIndex() + 1; ; index++) {
            final var storedMeta = replLog.lookupStoredMeta(index);
            if (storedMeta == null) {
                LOG.trace("{}: ReplicatedLogEntry not found for index {} - snapshotIndex: {}, journal size: {}",
                        logName, index, replLog.getSnapshotIndex(), replLog.size());
                break;
            }

            final var logEntry = storedMeta.meta();
            int replicatedCount = storedMeta.durable() ? 1 : 0;
            if (replicatedCount == 0) {
                // FIXME: Revisit this piece of code once we have simplified ReplicatedLog/persistence/snapshotting
                //        interactions. We really should be counting ourselves as 0 in replicatedCount -- i.e. the state
                //        before CONTROLLER-1490's https://git.opendaylight.org/gerrit/c/controller/+/48441.
                //
                // Original comment explained the following:
                //
                //     We don't commit and apply a log entry until we've gotten the ack from our local persistence.
                //     Ideally we should be able to update the commit index if we get a consensus amongst the followers
                //     w/o the local persistence ack however this can cause timing issues with snapshot capture
                //     which may lead to an entry that is neither in the serialized snapshot state nor in the snapshot's
                //     unapplied entries. This can happen if the lastAppliedIndex is updated but the corresponding
                //     ApplyState message is still pending in the message queue and thus the corresponding log entry
                //     hasn't actually been applied to the state yet. This would be alleviated by eliminating the
                //     ApplyState message in lieu of synchronously updating lastAppliedIndex and applying to state.
                //
                // ApplyState/snapshotting was re-visited in CONTROLLER-1568, the comment was shorted to its current
                // form as part of https://git.opendaylight.org/gerrit/c/controller/+/49404
                //
                // Since then ApplyState has fewer instantiations and we have ExecuteInActor available, with state
                // snapshot being a direct call (via RaftActorSnapshotCohort.takeSnapshot()), so this should be a
                // non-issue.
                //
                // BUT followers respond before they reach local stability right now, so if we remove this block, we can
                // get into a state where consensus is reached, but there is no stable replica. See the corresponding
                // comment in Follower.processNewEntries().

                // We don't commit and apply a log entry until we have gotten the ack from our local persistence,
                // even though there *should not* be any issue with updating the commit index if we get a consensus
                // amongst the followers w/o the local persistence ack.
                LOG.trace("{}: log entry at index {} has not finished persisting", logName, index);
                break;
            }

            LOG.trace("{}: checking Nth index {}", logName, index);
            for (var logInfo : followerToLog.values()) {
                final var peerInfo = context.getPeerInfo(logInfo.getId());
                if (logInfo.getMatchIndex() >= index && peerInfo != null && peerInfo.isVoting()) {
                    replicatedCount++;
                } else if (LOG.isTraceEnabled()) {
                    LOG.trace("{}: Not counting follower {} - matchIndex: {}, {}", logName, logInfo.getId(),
                            logInfo.getMatchIndex(), peerInfo);
                }
            }

            if (LOG.isTraceEnabled()) {
                LOG.trace("{}: replicatedCount {}, minReplicationCount: {}", logName, replicatedCount,
                        minReplicationCount);
            }
            if (replicatedCount < minReplicationCount) {
                LOG.trace("{}: minReplicationCount not reached, actual {} - breaking", logName, replicatedCount);
                break;
            }

            // Don't update the commit index if the log entry is from a previous term, as per §5.4.1:
            // "Raft never commits log entries from previous terms by counting replicas".
            // However we keep looping so we can make progress when new entries in the current term
            // reach consensus, as per §5.4.1: "once an entry from the current term is committed by
            // counting replicas, then all prior entries are committed indirectly".
            if (logEntry.term() == currentTerm()) {
                LOG.trace("{}: Setting commit index to {}", logName, index);
                replLog.setCommitIndex(index);
            } else {
                LOG.debug("{}: Not updating commit index to {} - retrieved log entry with index {}, term {} does not "
                    + "match the current term {}", logName, index, logEntry.index(), logEntry.term(), currentTerm());
            }
        }

        // Apply the change to the state machine
        if (replLog.getCommitIndex() > replLog.getLastApplied()) {
            LOG.debug("{}: Applying to log - commitIndex: {}, lastAppliedIndex: {}", logName,
                replLog.getCommitIndex(), replLog.getLastApplied());

            applyLogToStateMachine(replLog.getCommitIndex());
        }

        if (!context.getSnapshotManager().isCapturing()) {
            purgeInMemoryLog();
        }
    }

    private boolean updateFollowerLogInformation(final FollowerLogInformation followerLogInformation,
            final AppendEntriesReply appendEntriesReply) {
        boolean updated = followerLogInformation.setMatchIndex(appendEntriesReply.getLogLastIndex());
        updated = followerLogInformation.setNextIndex(appendEntriesReply.getLogLastIndex() + 1) || updated;

        if (updated && LOG.isDebugEnabled()) {
            LOG.debug(
                "{}: handleAppendEntriesReply - FollowerLogInformation for {} updated: matchIndex: {}, nextIndex: {}",
                logName, followerLogInformation.getId(), followerLogInformation.getMatchIndex(),
                followerLogInformation.getNextIndex());
        }
        return updated;
    }

    private void purgeInMemoryLog() {
        //find the lowest index across followers which has been replicated to all.
        // lastApplied if there are no followers, so that we keep clearing the log for single-node
        // we would delete the in-mem log from that index on, in-order to minimize mem usage
        // we would also share this info thru AE with the followers so that they can delete their log entries as well.
        long minReplicatedToAllIndex = followerToLog.isEmpty() ? replicatedLog().getLastApplied() : Long.MAX_VALUE;
        for (var info : followerToLog.values()) {
            minReplicatedToAllIndex = Math.min(minReplicatedToAllIndex, info.getMatchIndex());
        }

        super.performSnapshotWithoutCapture(minReplicatedToAllIndex);
    }

    /**
     * Removes and returns the ClientRequestTracker for the specified log index.
     * @param logIndex the log index
     * @return the ClientRequestTracker or null if none available
     */
    private ClientRequestTracker removeClientRequestTracker(final long logIndex) {
        final var it = trackers.iterator();
        while (it.hasNext()) {
            final var tracker = it.next();
            if (tracker.logIndex() == logIndex) {
                it.remove();
                return tracker;
            }
        }
        return null;
    }

    @Override
    final Identifier applyIdentifierFor(final LogEntry entry) {
        // first check whether a ClientRequestTracker exists for this entry.
        // If it does that means the leader wasn't dropped before the transaction applied.
        // That means that this transaction can be safely applied as a local transaction since we
        // have the ClientRequestTracker.
        final var tracker = removeClientRequestTracker(entry.index());
        if (tracker != null) {
            return tracker.identifier();
        }

        // Tracker is missing, this means that we switched behaviours between replicate and applystate
        // and became the leader again,. We still want to apply this as a local modification because
        // we have resumed leadership with that log entry having been committed.
        if (entry.command() instanceof IdentifiablePayload<?> identifiable) {
            return identifiable.getIdentifier();
        }

        return null;
    }

    @Override
    RaftActorBehavior handleRequestVoteReply(final ActorRef sender, final RequestVoteReply requestVoteReply) {
        return this;
    }

    @Override
    public RaftActorBehavior handleMessage(final ActorRef sender, final Object message) {
        requireNonNull(sender, "sender should not be null");

        if (appendEntriesMessageSlicer.handleMessage(message)) {
            return this;
        }

        // If RPC request or response contains term T > currentTerm:
        // set currentTerm = T, convert to follower (§5.1)
        // This applies to all RPC messages and responses
        if (message instanceof RaftRPC rpc && rpc.getTerm() > context.currentTerm() && shouldUpdateTerm(rpc)) {
            LOG.info("{}: Term {} in \"{}\" message is greater than leader's term {} - switching to Follower",
                logName, rpc.getTerm(), rpc, context.currentTerm());

            try {
                context.persistTermInfo(new TermInfo(rpc.getTerm()));
            } catch (IOException e) {
                // FIXME: do not mask IOException
                throw new UncheckedIOException(e);
            }

            // This is a special case. Normally when stepping down as leader we don't process and reply to the
            // RaftRPC as per raft. But if we're in the process of transferring leadership and we get a
            // RequestVote, process the RequestVote before switching to Follower. This enables the requesting
            // candidate node to be elected the leader faster and avoids us possibly timing out in the Follower
            // state and starting a new election and grabbing leadership back before the other candidate node can
            // start a new election due to lack of responses. This case would only occur if there isn't a majority
            // of other nodes available that can elect the requesting candidate. Since we're transferring
            // leadership, we should make every effort to get the requesting node elected.
            if (rpc instanceof RequestVote requestVote && context.getRaftActorLeadershipTransferCohort() != null) {
                LOG.debug("{}: Leadership transfer in progress - processing RequestVote", logName);
                requestVote(sender, requestVote);
            }

            return switchBehavior(new Follower(context));
        }

        switch (message) {
            case InstallSnapshotReply msg -> handleInstallSnapshotReply(msg);
            case Replicate msg -> replicate(msg);
            case SendHeartBeat msg -> sendHeartBeat();
            default -> {
                return super.handleMessage(sender, message);
            }
        }
        return this;
    }

    private void handleInstallSnapshotReply(final InstallSnapshotReply reply) {
        LOG.debug("{}: handleInstallSnapshotReply: {}", logName, reply);

        final var followerId = reply.getFollowerId();
        final var followerLogInfo = followerToLog.get(followerId);
        if (followerLogInfo == null) {
            // This can happen during AddServer if it times out.
            LOG.error("{}: FollowerLogInformation not found for follower {} in InstallSnapshotReply", logName,
                followerId);
            return;
        }

        final var installSnapshotState = followerLogInfo.getInstallSnapshotState();
        if (installSnapshotState == null) {
            LOG.error("{}: LeaderInstallSnapshotState not found for follower {} in InstallSnapshotReply", logName,
                followerId);
            return;
        }

        installSnapshotState.resetChunkTimer();
        followerLogInfo.markFollowerActive();

        final var expectedChunkIndex = installSnapshotState.getChunkIndex();
        final var replyChunkIndex = reply.getChunkIndex();
        if (replyChunkIndex != expectedChunkIndex) {
            LOG.error("{}: Chunk index {} in InstallSnapshotReply from follower {} does not match expected index {}",
                logName, replyChunkIndex, followerId, expectedChunkIndex);

            if (replyChunkIndex == LeaderInstallSnapshotState.INVALID_CHUNK_INDEX) {
                // Since the Follower did not find this index to be valid we should reset the follower snapshot
                // so that Installing the snapshot can resume from the beginning
                installSnapshotState.reset();
            }
            return;
        }

        if (!reply.isSuccess()) {
            LOG.warn("{}: Received failed InstallSnapshotReply - will retry: {}", logName, reply);
            installSnapshotState.markSendStatus(false);
            sendNextSnapshotChunk(followerId, followerLogInfo);
            return;
        }

        if (!installSnapshotState.isLastChunk(replyChunkIndex)) {
            LOG.debug("{}: Success InstallSnapshotReply from {}, sending next chunk", logName, followerId);
            installSnapshotState.markSendStatus(true);
            sendNextSnapshotChunk(followerId, followerLogInfo);
            return;
        }

        // this was the last chunk reply
        final long followerMatchIndex = snapshotHolder.lastIncluded().index();
        followerLogInfo.setMatchIndex(followerMatchIndex);
        followerLogInfo.setNextIndex(followerMatchIndex + 1);
        followerLogInfo.clearLeaderInstallSnapshotState();

        LOG.info("{}: Snapshot successfully installed on follower {} (last chunk {}) - matchIndex set to {}, "
            + "nextIndex set to {}", logName, followerId, replyChunkIndex, followerLogInfo.getMatchIndex(),
            followerLogInfo.getNextIndex());

        if (!anyFollowersInstallingSnapshot()) {
            // once there are no pending followers receiving snapshots we can remove snapshot from the memory
            clearSnapshot();
        }

        if (context.getPeerInfo(followerId).getVotingState() == VotingState.VOTING_NOT_INITIALIZED) {
            context.getActor().tell(new UnInitializedFollowerSnapshotReply(followerId), context.getActor());
            LOG.debug("Sent message UnInitializedFollowerSnapshotReply to self");
        }

        if (!context.getSnapshotManager().isCapturing()) {
            // Since the follower is now caught up try to purge the log.
            purgeInMemoryLog();
        }
    }

    private void sendNextSnapshotChunk(final String followerId, final FollowerLogInformation followerLogInfo) {
        final var followerActor = context.getPeerActorSelection(followerId);
        if (followerActor != null) {
            sendSnapshotChunk(followerActor, followerLogInfo);
        }
    }

    private boolean anyFollowersInstallingSnapshot() {
        for (var info : followerToLog.values()) {
            if (info.getInstallSnapshotState() != null) {
                return true;
            }
        }
        return false;
    }

    private void replicate(final Replicate replicate) {
        final long logIndex = replicate.logIndex();

        LOG.debug("{}: Replicate message: identifier: {}, logIndex: {}, isSendImmediate: {}", logName,
                replicate.identifier(), logIndex, replicate.sendImmediate());

        // Create a tracker entry we will use this later to notify the
        // client actor
        final var identifier = replicate.identifier();
        if (identifier != null) {
            trackers.add(new ClientRequestTracker(logIndex, identifier));
        }

        boolean applyModificationToState = !context.anyVotingPeers()
                || context.getRaftPolicy().applyModificationToStateBeforeConsensus();

        if (applyModificationToState) {
            context.getReplicatedLog().setCommitIndex(logIndex);
            applyLogToStateMachine(logIndex);
        }

        if (replicate.sendImmediate() && !followerToLog.isEmpty()) {
            sendAppendEntries(0, false);
        }
    }

    protected void sendAppendEntries(final long timeSinceLastActivityIntervalNanos, final boolean isHeartbeat) {
        // Send an AppendEntries to all followers
        for (var entry : followerToLog.entrySet()) {
            final var followerId = entry.getKey();
            final var followerLogInformation = entry.getValue();
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
        final var followerActor = context.getPeerActorSelection(followerId);
        if (followerActor != null) {
            long followerNextIndex = followerLogInformation.getNextIndex();
            boolean isFollowerActive = followerLogInformation.isFollowerActive();
            boolean sendAppendEntries = false;
            @NonNull List<@NonNull LogEntry> entries = List.of();

            LeaderInstallSnapshotState installSnapshotState = followerLogInformation.getInstallSnapshotState();
            if (installSnapshotState != null) {

                // if install snapshot is in process , then sent next chunk if possible
                if (isFollowerActive) {
                    // 30 seconds with default settings, can be modified via heartbeat or election timeout factor
                    final var snapshotReplyTimeout = context.getConfigParams().getHeartBeatInterval()
                        .multipliedBy(context.getConfigParams().getElectionTimeoutFactor() * 3);

                    if (installSnapshotState.isChunkTimedOut(snapshotReplyTimeout)) {
                        sendAppendEntries = !resendSnapshotChunk(followerActor, followerLogInformation);
                    } else if (installSnapshotState.canSendNextChunk()) {
                        sendSnapshotChunk(followerActor, followerLogInformation);
                    }
                } else if (sendHeartbeat
                    || followerLogInformation.hasStaleCommitIndex(replicatedLog().getCommitIndex())) {
                    // we send a heartbeat even if we have not received a reply for the last chunk
                    sendAppendEntries = true;
                }
            } else if (followerLogInformation.isLogEntrySlicingInProgress()) {
                sendAppendEntries = sendHeartbeat;
            } else {
                final var replLog = replicatedLog();
                long leaderLastIndex = replLog.lastIndex();
                long leaderSnapShotIndex = replLog.getSnapshotIndex();

                if (!isHeartbeat && LOG.isDebugEnabled() || LOG.isTraceEnabled()) {
                    LOG.debug("{}: Checking sendAppendEntries for follower {}: active: {}, followerNextIndex: {}, "
                            + "leaderLastIndex: {}, leaderSnapShotIndex: {}", logName, followerId, isFollowerActive,
                            followerNextIndex, leaderLastIndex, leaderSnapShotIndex);
                }

                if (isFollowerActive && replLog.isPresent(followerNextIndex)) {
                    LOG.debug("{}: sendAppendEntries: {} is present for follower {}", logName, followerNextIndex,
                        followerId);

                    if (followerLogInformation.okToReplicate(replLog.getCommitIndex())) {
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

                    final var replLogSize = replLog.size();
                    if (canInstallSnapshot(followerNextIndex)) {
                        LOG.info("{}: Initiating install snapshot to follower {}: follower nextIndex: {}, leader "
                                + "snapshotIndex: {}, leader lastIndex: {}, leader log size: {}", logName, followerId,
                                followerNextIndex, leaderSnapShotIndex, leaderLastIndex, replLogSize);

                        initiateCaptureSnapshot(followerId);
                    } else {
                        // It doesn't seem like we should ever reach here - most likely indicates sonething is
                        // wrong.
                        LOG.info("{}: Follower {} is behind but cannot install snapshot: follower nextIndex: {}, "
                                + "leader snapshotIndex: {}, leader lastIndex: {}, leader log size: {}", logName,
                                followerId, followerNextIndex, leaderSnapShotIndex, leaderLastIndex, replLogSize);
                    }

                } else if (sendHeartbeat || followerLogInformation.hasStaleCommitIndex(replLog.getCommitIndex())) {
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

    @NonNullByDefault
    private List<LogEntry> getEntriesToSend(final FollowerLogInformation followerLogInfo,
            final ActorSelection followerActor) {
        // Try to get all the entries in the journal but not exceeding the max data size for a single AppendEntries
        // message.
        final var replLog = replicatedLog();

        int maxEntries = (int) replLog.size();
        final int maxDataSize = context.getConfigParams().getMaximumMessageSliceSize();
        final long followerNextIndex = followerLogInfo.getNextIndex();
        final var entries = replLog.getFrom(followerNextIndex, maxEntries, maxDataSize);

        // If the first entry's size exceeds the max data size threshold, it will be returned from the call above. If
        // that is the case, then we need to slice it into smaller chunks.
        if (entries.size() != 1 || entries.getFirst().command().serializedSize() <= maxDataSize) {
            // Don't need to slice.
            return List.copyOf(entries);
        }

        final var firstEntry = entries.getFirst();
        LOG.debug("{}: Log entry size {} exceeds max payload size {}", logName, firstEntry.command().size(),
                maxDataSize);

        // If an AppendEntries has already been serialized for the log index then reuse the
        // SharedFileBackedOutputStream.
        final Long logIndex = firstEntry.index();
        var fileBackedStream = sharedSerializedAppendEntriesStreams.get(logIndex);
        if (fileBackedStream == null) {
            fileBackedStream = context.getFileBackedOutputStreamFactory().newSharedInstance();

            final var appendEntries = new AppendEntries(currentTerm(), memberId(),
                    replLog.getLogEntryIndex(followerNextIndex - 1), replLog.getLogEntryTerm(followerNextIndex - 1),
                    entries, replLog.getCommitIndex(), getReplicatedToAllIndex(), context.getPayloadVersion());

            LOG.debug("{}: Serializing {} for slicing for follower {}", logName, appendEntries,
                    followerLogInfo.getId());

            try (var out = new ObjectOutputStream(fileBackedStream)) {
                out.writeObject(appendEntries);
            } catch (IOException e) {
                LOG.error("{}: Error serializing {}", logName, appendEntries, e);
                fileBackedStream.cleanup();
                return List.of();
            }

            sharedSerializedAppendEntriesStreams.put(logIndex, fileBackedStream);

            fileBackedStream.setOnCleanupCallback(() -> {
                LOG.debug("{}: On SharedFileBackedOutputStream cleanup for index {}", logName, logIndex);
                sharedSerializedAppendEntriesStreams.remove(logIndex);
            });
        } else {
            LOG.debug("{}: Reusing SharedFileBackedOutputStream for follower {}", logName, followerLogInfo.getId());
            fileBackedStream.incrementUsageCount();
        }

        LOG.debug("{}: Slicing stream for index {}, follower {}", logName, logIndex, followerLogInfo.getId());

        // Record that slicing is in progress for the follower.
        followerLogInfo.setSlicedLogEntryIndex(logIndex);

        final var identifier = new FollowerIdentifier(followerLogInfo.getId());
        appendEntriesMessageSlicer.slice(SliceOptions.builder().identifier(identifier)
                .fileBackedOutputStream(fileBackedStream).sendTo(followerActor).replyTo(actor())
                .onFailureCallback(failure -> {
                    LOG.error("{}: Error slicing AppendEntries for follower {}", logName, followerLogInfo.getId(),
                        failure);
                    followerLogInfo.setSlicedLogEntryIndex(FollowerLogInformation.NO_INDEX);
                }).build());

        return List.of();
    }

    @NonNullByDefault
    private void sendAppendEntriesToFollower(final ActorSelection followerActor, final List<? extends LogEntry> entries,
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
        final var isInstallingSnaphot = followerLogInformation.getInstallSnapshotState() != null;
        final var replLog = replicatedLog();
        final var leaderCommitIndex = isInstallingSnaphot || followerLogInformation.isLogEntrySlicingInProgress()
                || !followerLogInformation.isFollowerActive() ? -1 : replLog.getCommitIndex();

        long followerNextIndex = followerLogInformation.getNextIndex();
        final var appendEntries = new AppendEntries(currentTerm(), memberId(),
            replLog.getLogEntryIndex(followerNextIndex - 1), replLog.getLogEntryTerm(followerNextIndex - 1), entries,
            leaderCommitIndex, super.getReplicatedToAllIndex(), context.getPayloadVersion(),
            followerLogInformation.getRaftVersion(), followerLogInformation.needsLeaderAddress(memberId()));

        if (!entries.isEmpty() || LOG.isTraceEnabled()) {
            LOG.debug("{}: Sending AppendEntries to follower {}: {}", logName, followerLogInformation.getId(),
                    appendEntries);
        }

        followerLogInformation.setSentCommitIndex(leaderCommitIndex);
        followerActor.tell(appendEntries, actor());
    }

    /**
     * Initiates a snapshot capture to install on a follower. Install Snapshot works as follows:
     * <ol>
     *   <li>Leader initiates the capture snapshot by calling createSnapshot on the RaftActor.</li>
     *   <li>On receipt of the CaptureSnapshotReply message, the RaftActor persists the snapshot and makes a call to
     *       the Leader's handleMessage with a SendInstallSnapshot message.</li>
     *   <li>The Leader obtains and stores the Snapshot from the SendInstallSnapshot message and sends it in chunks to
     *       the Follower via InstallSnapshot messages.</li>
     *   <li>For each chunk, the Follower sends back an InstallSnapshotReply.</li>
     *   <li>On receipt of the InstallSnapshotReply for the last chunk, the Leader marks the install complete for that
     *       follower.</li>
     *   <li>If another follower requires a snapshot and a snapshot has been collected (via SendInstallSnapshot)
     *       then send the existing snapshot in chunks to the follower.</li>
     * </ol>
     *
     * @param followerId the id of the follower.
     * @return true if capture was initiated, false otherwise.
     */
    public boolean initiateCaptureSnapshot(final String followerId) {
        final var followerLogInfo = followerToLog.get(followerId);
        if (snapshotHolder != null) {
            // If a snapshot is present in the memory, most likely another install is in progress no need to capture
            // snapshot. This could happen if another follower needs an install when one is going on.
            final var followerActor = context.getPeerActorSelection(followerId);

            // Note: sendSnapshotChunk will set the LeaderInstallSnapshotState.
            sendSnapshotChunk(followerActor, followerLogInfo, snapshotHolder);
            return true;
        }

        final var captureInitiated = context.getSnapshotManager()
            .captureToInstall(replicatedLog().lastMeta(), getReplicatedToAllIndex(), followerId);
        if (captureInitiated) {
            followerLogInfo.setLeaderInstallSnapshotState(new LeaderInstallSnapshotState(
                context.getConfigParams().getMaximumMessageSliceSize(), logName));
        }
        return captureInitiated;
    }

    private boolean canInstallSnapshot(final long nextIndex) {
        // If the follower's nextIndex is -1 then we might as well send it a snapshot
        // Otherwise send it a snapshot only if the nextIndex is not present in the log but is present
        // in the snapshot
        final var replLog = replicatedLog();
        return nextIndex == -1 || !replLog.isPresent(nextIndex) && replLog.isInSnapshot(nextIndex);
    }

    @NonNullByDefault
    public final void sendInstallSnapshot(final InstallableSnapshot snapshot) {
        snapshotHolder = decompressIfNeeded(requireNonNull(snapshot));

        LOG.debug("{}: sendInstallSnapshot", logName);
        for (var entry : followerToLog.entrySet()) {
            final var followerId = entry.getKey();
            final var followerActor = context.getPeerActorSelection(followerId);
            final var followerLogInfo = entry.getValue();

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

    @NonNullByDefault
    private InstallableSnapshot decompressIfNeeded(final InstallableSnapshot fromStore) {
        // We do not try to compress if not already compressed
        return switch (context.getConfigParams().getPreferredCompression()) {
            case LZ4 -> fromStore;
            case NONE -> {
                final var source = fromStore.source();
                yield source == null || source instanceof PlainSnapshotSource ? fromStore
                    : new InstallableSnapshotSource(fromStore.lastIncluded(), source.toPlainSource());
            }
        };
    }

    private void sendSnapshotChunk(final ActorSelection followerActor, final FollowerLogInformation followerLogInfo) {
        if (snapshotHolder != null) {
            sendSnapshotChunk(followerActor, followerLogInfo, snapshotHolder);
        }
    }

    /**
     * Sends a snapshot chunk to a given follower. InstallSnapshot should qualify as a heartbeat too.
     */
    private void sendSnapshotChunk(final ActorSelection followerActor, final FollowerLogInformation followerLogInfo,
            final InstallableSnapshot snapshot) {
        var installSnapshotState = followerLogInfo.getInstallSnapshotState();
        if (installSnapshotState == null) {
            installSnapshotState = new LeaderInstallSnapshotState(
                context.getConfigParams().getMaximumMessageSliceSize(), logName);
            followerLogInfo.setLeaderInstallSnapshotState(installSnapshotState);
        }

        final byte[] data;
        try {
            // Ensure the snapshot bytes are set - this is a no-op.
            final var source = snapshot.source();
            installSnapshotState.setSnapshotBytes(source == null ? ByteArray.empty() : source.io());

            if (installSnapshotState.canSendNextChunk()) {
                data = installSnapshotState.getNextChunk();
            } else {
                return;
            }
        } catch (IOException e) {
            LOG.warn("{}: Unable to send chunk: {}/{}. Reseting snapshot progress. Snapshot state: {}", logName,
                installSnapshotState.getChunkIndex(), installSnapshotState.getTotalChunks(), installSnapshotState, e);
            installSnapshotState.reset();
            return;
        }

        LOG.debug("{}: next snapshot chunk size for follower {}: {}", logName, followerLogInfo.getId(), data.length);

        final int chunkIndex = installSnapshotState.incrementChunkIndex();
        final int totalChunks = installSnapshotState.getTotalChunks();
        final var votingConfig = chunkIndex == totalChunks ? context.getPeerServerInfo(true) : null;

        installSnapshotState.startChunkTimer();
        followerActor.tell(
            new InstallSnapshot(currentTerm(), memberId(),
                // snapshot term/index inforation
                snapshot.lastIncluded().index(), snapshot.lastIncluded().term(),
                // this chunk and its indexing info and previous hash code
                data, chunkIndex, totalChunks, OptionalInt.of(installSnapshotState.getLastChunkHashCode()),
                // voting configuration, if present
                votingConfig,
                // make sure the follower understands this message
                followerLogInfo.getRaftVersion()),
            actor());

        LOG.debug("{}: InstallSnapshot sent to follower {}, Chunk: {}/{}", logName, followerActor.path(),
            chunkIndex, totalChunks);
    }

    private boolean resendSnapshotChunk(final ActorSelection followerActor,
                                        final FollowerLogInformation followerLogInfo) {
        if (snapshotHolder == null) {
            // Seems like we should never hit this case, but just in case we do, reset the snapshot progress so that it
            // can restart from the next AppendEntries.
            LOG.warn("{}: Attempting to resend snapshot with no snapshot holder present.", logName);
            followerLogInfo.clearLeaderInstallSnapshotState();
            return false;
        }

        final var installSnapshotState = followerLogInfo.getInstallSnapshotState();
        // we are resending, timer needs to be reset
        installSnapshotState.resetChunkTimer();
        installSnapshotState.markSendStatus(false);

        sendSnapshotChunk(followerActor, followerLogInfo, snapshotHolder);
        return true;
    }

    void beforeSendHeartbeat() {
        // No-op, overridden in Leader
    }

    private void sendHeartBeat() {
        beforeSendHeartbeat();

        if (!followerToLog.isEmpty()) {
            LOG.trace("{}: Sending heartbeat", logName);
            sendAppendEntries(context.getConfigParams().getHeartBeatInterval().toNanos(), true);

            appendEntriesMessageSlicer.checkExpiredSlicedMessageState();
        }

        scheduleHeartBeat(context.getConfigParams().getHeartBeatInterval());
    }

    private void stopHeartBeat() {
        if (heartbeatSchedule != null && !heartbeatSchedule.isCancelled()) {
            heartbeatSchedule.cancel();
        }
    }

    private void scheduleHeartBeat(final Duration interval) {
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
        final var actor = context.getActor();
        final var actorSystem = context.getActorSystem();

        heartbeatSchedule = actorSystem.scheduler()
            .scheduleOnce(interval, actor, SendHeartBeat.INSTANCE, actorSystem.dispatcher(), actor);
    }

    @Override
    public void close() {
        stopHeartBeat();
        appendEntriesMessageSlicer.close();
    }

    @Override
    public final String getLeaderId() {
        return memberId();
    }

    @Override
    public final short getLeaderPayloadVersion() {
        return context.getPayloadVersion();
    }

    protected boolean isLeaderIsolated() {
        int minPresent = getMinIsolatedLeaderPeerCount();
        for (var followerLogInformation : followerToLog.values()) {
            final var peerInfo = context.getPeerInfo(followerLogInformation.getId());
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
        final var sb = new StringBuilder().append('[');
        for (var followerLogInformation : followerToLog.values()) {
            sb
                .append('{').append(followerLogInformation.getId())
                .append(" state:").append(followerLogInformation.isFollowerActive())
                .append("},");
        }
        return sb.append(']').toString();
    }

    @VisibleForTesting
    public FollowerLogInformation getFollower(final String followerId) {
        return followerToLog.get(followerId);
    }

    @VisibleForTesting
    public int followerLogSize() {
        return followerToLog.size();
    }

    @NonNullByDefault
    @VisibleForTesting
    final void setSnapshot(final long index, final long term, final StreamSource source) {
        snapshotHolder = new InstallableSnapshotSource(index, term, new PlainSnapshotSource(source));
    }

    @VisibleForTesting
    final boolean hasSnapshot() {
        return snapshotHolder != null;
    }

    @VisibleForTesting
    final void clearSnapshot() {
        snapshotHolder = null;
    }
}
