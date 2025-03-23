/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.cluster.Member;
import org.apache.pekko.cluster.MemberStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.messaging.MessageAssembler;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.SnapshotManager.ApplyLeaderSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.spi.ImmutableRaftEntryMeta;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.raft.api.RaftRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The behavior of a RaftActor in the Follower raft state.
 * <ul>
 * <li> Respond to RPCs from candidates and leaders
 * <li> If election timeout elapses without receiving AppendEntries
 * RPC from current leader or granting vote to candidate:
 * convert to candidate
 * </ul>
 */
// Non-final for testing
public class Follower extends RaftActorBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(Follower.class);
    private static final long MAX_ELECTION_TIMEOUT_FACTOR = 18;

    private final Stopwatch lastLeaderMessageTimer = Stopwatch.createStarted();
    private final SyncStatusTracker initialSyncStatusTracker;
    private final MessageAssembler appendEntriesMessageAssembler;

    private SnapshotTracker snapshotTracker = null;
    private String leaderId;
    private short leaderPayloadVersion;

    public Follower(final RaftActorContext context) {
        this(context, null, (short)-1);
    }

    @VisibleForTesting
    Follower(final RaftActorContext context, final String initialLeaderId, final short initialLeaderPayloadVersion) {
        super(context, RaftRole.Follower);
        leaderId = initialLeaderId;
        leaderPayloadVersion = initialLeaderPayloadVersion;

        initialSyncStatusTracker = new SyncStatusTracker(context.getActor(), memberId(),
            context.getConfigParams().getSyncIndexThreshold());

        appendEntriesMessageAssembler = MessageAssembler.builder()
            .logContext(logName)
            .fileBackedStreamFactory(context.getFileBackedOutputStreamFactory())
            .assembledMessageCallback((message, sender) -> handleMessage(sender, message))
            .build();

        if (context.getPeerIds().isEmpty() && getLeaderId() == null) {
            actor().tell(TimeoutNow.INSTANCE, actor());
        } else {
            // Note: call to 'super' instead of 'this' to side-step MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR
            scheduleElection(super.electionDuration());
        }
    }

    public final @NonNull Follower copy() {
        return new Follower(context, leaderId, leaderPayloadVersion);
    }

    @Override
    public final String getLeaderId() {
        return leaderId;
    }

    @VisibleForTesting
    protected final void setLeaderId(final @Nullable String leaderId) {
        this.leaderId = leaderId;
    }

    @Override
    public final short getLeaderPayloadVersion() {
        return leaderPayloadVersion;
    }

    @VisibleForTesting
    protected final void setLeaderPayloadVersion(final short leaderPayloadVersion) {
        this.leaderPayloadVersion = leaderPayloadVersion;
    }

    private void restartLastLeaderMessageTimer() {
        if (lastLeaderMessageTimer.isRunning()) {
            lastLeaderMessageTimer.reset();
        }

        lastLeaderMessageTimer.start();
    }

    private boolean isLogEntryPresent(final long index) {
        final var replLog = replicatedLog();
        return replLog.isInSnapshot(index) || replLog.get(index) != null;
    }

    private void updateInitialSyncStatus(final long currentLeaderCommit, final String newLeaderId) {
        initialSyncStatusTracker.update(newLeaderId, currentLeaderCommit, replicatedLog().getCommitIndex());
    }

    @Override
    final RaftActorBehavior handleAppendEntries(final ActorRef sender, final AppendEntries appendEntries) {
        int numLogEntries = appendEntries.getEntries().size();
        if (LOG.isTraceEnabled()) {
            LOG.trace("{}: handleAppendEntries: {}", logName, appendEntries);
        } else if (LOG.isDebugEnabled() && numLogEntries > 0) {
            LOG.debug("{}: handleAppendEntries: {}", logName, appendEntries);
        }

        if (snapshotTracker != null && !snapshotTracker.getLeaderId().equals(appendEntries.getLeaderId())) {
            LOG.debug("{}: snapshot install is in progress but the prior snapshot leaderId {} does not match the "
                + "AppendEntries leaderId {}", logName, snapshotTracker.getLeaderId(), appendEntries.getLeaderId());
            closeSnapshotTracker();
        }

        if (snapshotTracker != null || context.getSnapshotManager().isApplying()) {
            // if snapshot install is in progress, follower should just acknowledge append entries with a reply.
            final var reply = new AppendEntriesReply(memberId(), currentTerm(), true, lastIndex(), lastTerm(),
                context.getPayloadVersion(), false, needsLeaderAddress(), appendEntries.getLeaderRaftVersion());

            LOG.debug("{}: snapshot install is in progress, replying immediately with {}", logName, reply);
            sender.tell(reply, actor());
            return this;
        }

        // If we got here then we do appear to be talking to the leader
        leaderId = appendEntries.getLeaderId();
        leaderPayloadVersion = appendEntries.getPayloadVersion();

        final var leaderAddress = appendEntries.leaderAddress();
        if (leaderAddress != null) {
            LOG.debug("New leader address: {}", leaderAddress);
            context.setPeerAddress(leaderId, leaderAddress);
            context.getConfigParams().getPeerAddressResolver().setResolved(leaderId, leaderAddress);
        }

        // First check if the logs are in sync or not
        if (isOutOfSync(appendEntries, sender)) {
            updateInitialSyncStatus(appendEntries.getLeaderCommit(), appendEntries.getLeaderId());
            return this;
        }

        if (!processNewEntries(appendEntries, sender)) {
            updateInitialSyncStatus(appendEntries.getLeaderCommit(), appendEntries.getLeaderId());
            return this;
        }

        final var replLog = replicatedLog();
        final var lastIndex = replLog.lastIndex();
        final var prevCommitIndex = replLog.getCommitIndex();

        // If leaderCommit > commitIndex, set commitIndex = min(leaderCommit, index of last new entry)
        if (appendEntries.getLeaderCommit() > prevCommitIndex) {
            replLog.setCommitIndex(Math.min(appendEntries.getLeaderCommit(), lastIndex));
        }

        if (prevCommitIndex != replLog.getCommitIndex()) {
            LOG.debug("{}: Commit index set to {}", logName, replLog.getCommitIndex());
        }

        final var reply = new AppendEntriesReply(memberId(), currentTerm(), true, lastIndex, replLog.lastTerm(),
            context.getPayloadVersion(), false, needsLeaderAddress(), appendEntries.getLeaderRaftVersion());

        if (LOG.isTraceEnabled()) {
            LOG.trace("{}: handleAppendEntries returning : {}", logName, reply);
        } else if (LOG.isDebugEnabled() && numLogEntries > 0) {
            LOG.debug("{}: handleAppendEntries returning : {}", logName, reply);
        }

        // Reply to the leader before applying any previous state so as not to hold up leader consensus.
        sender.tell(reply, actor());

        updateInitialSyncStatus(appendEntries.getLeaderCommit(), appendEntries.getLeaderId());

        // If leaderCommit > lastApplied, increment lastApplied and apply log[lastApplied] to state machine (§5.3).
        // lastApplied can be equal to lastIndex.
        if (appendEntries.getLeaderCommit() > replLog.getLastApplied() && replLog.getLastApplied() < lastIndex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("{}: applyLogToStateMachine, appendEntries.getLeaderCommit(): {}, "
                        + "context.getLastApplied(): {}, lastIndex(): {}", logName,
                    appendEntries.getLeaderCommit(), replLog.getLastApplied(), lastIndex);
            }

            applyLogToStateMachine(appendEntries.getLeaderCommit());
        }

        if (!context.getSnapshotManager().isCapturing()) {
            super.performSnapshotWithoutCapture(appendEntries.getReplicatedToAllIndex());
        }

        appendEntriesMessageAssembler.checkExpiredAssembledMessageState();

        return this;
    }

    private boolean processNewEntries(final AppendEntries appendEntries, final ActorRef sender) {
        final var entries = appendEntries.getEntries();
        final int numLogEntries = entries.size();
        if (numLogEntries == 0) {
            return true;
        }

        LOG.debug("{}: Number of entries to be appended = {}", logName, numLogEntries);

        final var replLog = replicatedLog();
        long lastIndex = replLog.lastIndex();
        int addEntriesFrom = 0;

        // First check for conflicting entries. If an existing entry conflicts with a new one (same index but different
        // term), delete the existing entry and all that follow it (§5.3)
        if (replLog.size() > 0) {
            // Find the entry up until the one that is not in the follower's log
            for (int i = 0; i < numLogEntries; i++, addEntriesFrom++) {
                final var matchEntry = entries.get(i);

                if (!isLogEntryPresent(matchEntry.index())) {
                    // newEntry not found in the log
                    break;
                }

                long existingEntryTerm = getLogEntryTerm(matchEntry.index());

                LOG.debug("{}: matchEntry {} is present: existingEntryTerm: {}", logName, matchEntry,
                        existingEntryTerm);

                // existingEntryTerm == -1 means it's in the snapshot and not in the log. We don't know
                // what the term was so we'll assume it matches.
                if (existingEntryTerm == -1 || existingEntryTerm == matchEntry.term()) {
                    continue;
                }

                if (!context.getRaftPolicy().applyModificationToStateBeforeConsensus()) {
                    LOG.info("{}: Removing entries from log starting at {}, commitIndex: {}, lastApplied: {}",
                            logName, matchEntry.index(), replLog.getCommitIndex(), replLog.getLastApplied());

                    // Entries do not match so remove all subsequent entries but only if the existing entries haven't
                    // been applied to the state yet.
                    if (matchEntry.index() <= replLog.getLastApplied()
                            || !replLog.removeFromAndPersist(matchEntry.index())) {
                        // Could not remove the entries - this means the matchEntry index must be in the
                        // snapshot and not the log. In this case the prior entries are part of the state
                        // so we must send back a reply to force a snapshot to completely re-sync the
                        // follower's log and state.

                        LOG.info("{}: Could not remove entries - sending reply to force snapshot", logName);
                        sender.tell(new AppendEntriesReply(memberId(), currentTerm(), false, lastIndex, lastTerm(),
                            context.getPayloadVersion(), true, needsLeaderAddress(),
                            appendEntries.getLeaderRaftVersion()), actor());
                        return false;
                    }

                    break;
                } else {
                    sender.tell(new AppendEntriesReply(memberId(), currentTerm(), false, lastIndex, lastTerm(),
                        context.getPayloadVersion(), true, needsLeaderAddress(), appendEntries.getLeaderRaftVersion()),
                        actor());
                    return false;
                }
            }
        }

        lastIndex = replLog.lastIndex();
        LOG.debug("{}: After cleanup, lastIndex: {}, entries to be added from: {}", logName, lastIndex, addEntriesFrom);

        // When persistence successfully completes for each new log entry appended, we need to determine if we
        // should capture a snapshot to compact the persisted log. shouldCaptureSnapshot tracks whether or not
        // one of the log entries has exceeded the log size threshold whereby a snapshot should be taken. However
        // we don't initiate the snapshot at that log entry but rather after the last log entry has been persisted.
        // This is done because subsequent log entries after the one that tripped the threshold may have been
        // applied to the state already, as the persistence callback occurs async, and we want those entries
        // purged from the persisted log as well.
        final var shouldCaptureSnapshot = new AtomicBoolean(false);
        final Consumer<ReplicatedLogEntry> appendAndPersistCallback = logEntry -> {
            if (shouldCaptureSnapshot.get() && logEntry == entries.getLast()) {
                context.getSnapshotManager().capture(replLog.lastMeta(), getReplicatedToAllIndex());
            }
        };

        // Append any new entries not already in the log
        for (int i = addEntriesFrom; i < numLogEntries; i++) {
            final var entry = entries.get(i);

            LOG.debug("{}: Append entry to log {}", logName, entry.getData());

            replLog.appendAndPersist(entry, appendAndPersistCallback, false);

            shouldCaptureSnapshot.compareAndSet(false, replLog.shouldCaptureSnapshot(entry.index()));

            if (entry.getData() instanceof ClusterConfig serverConfiguration) {
                context.updatePeerIds(serverConfiguration);
            }
        }

        LOG.debug("{}: Log size is now {}", logName, replLog.size());
        return true;
    }

    private boolean isOutOfSync(final AppendEntries appendEntries, final ActorRef sender) {

        final long lastIndex = lastIndex();
        if (lastIndex == -1 && appendEntries.getPrevLogIndex() != -1) {

            // The follower's log is out of sync because the leader does have an entry at prevLogIndex and this
            // follower has no entries in it's log.

            LOG.info("{}: The followers log is empty and the senders prevLogIndex is {}", logName,
                appendEntries.getPrevLogIndex());

            sendOutOfSyncAppendEntriesReply(sender, false, appendEntries.getLeaderRaftVersion());
            return true;
        }

        if (lastIndex > -1) {
            if (isLogEntryPresent(appendEntries.getPrevLogIndex())) {
                final long leadersPrevLogTermInFollowersLogOrSnapshot =
                        getLogEntryOrSnapshotTerm(appendEntries.getPrevLogIndex());
                if (leadersPrevLogTermInFollowersLogOrSnapshot != appendEntries.getPrevLogTerm()) {
                    // The follower's log is out of sync because the Leader's prevLogIndex entry does exist
                    // in the follower's log or snapshot but it has a different term.
                    final var replLog = context.getReplicatedLog();
                    LOG.info("{}: The prevLogIndex {} was found in the log but the term {} is not equal to the append "
                        + "entries prevLogTerm {} - lastIndex: {}, snapshotIndex: {}, snapshotTerm: {}", logName,
                        appendEntries.getPrevLogIndex(), leadersPrevLogTermInFollowersLogOrSnapshot,
                        appendEntries.getPrevLogTerm(), lastIndex, replLog.getSnapshotIndex(),
                        replLog.getSnapshotTerm());

                    sendOutOfSyncAppendEntriesReply(sender, false, appendEntries.getLeaderRaftVersion());
                    return true;
                }
            } else if (appendEntries.getPrevLogIndex() != -1) {
                // The follower's log is out of sync because the Leader's prevLogIndex entry was not found in it's log
                final var replLog = context.getReplicatedLog();
                LOG.info("{}: The log is not empty but the prevLogIndex {} was not found in it - lastIndex: {}, "
                        + "snapshotIndex: {}, snapshotTerm: {}", logName, appendEntries.getPrevLogIndex(), lastIndex,
                        replLog.getSnapshotIndex(), replLog.getSnapshotTerm());

                sendOutOfSyncAppendEntriesReply(sender, false, appendEntries.getLeaderRaftVersion());
                return true;
            }
        }

        if (appendEntries.getPrevLogIndex() == -1 && appendEntries.getPrevLogTerm() == -1
                && appendEntries.getReplicatedToAllIndex() != -1) {
            if (!isLogEntryPresent(appendEntries.getReplicatedToAllIndex())) {
                // This append entry comes from a leader who has it's log aggressively trimmed and so does not have
                // the previous entry in it's in-memory journal
                final var replLog = context.getReplicatedLog();
                LOG.info("{}: Cannot append entries because the replicatedToAllIndex {} does not appear to be in the "
                        + "in-memory journal - lastIndex: {}, snapshotIndex: {}, snapshotTerm: {}", logName,
                        appendEntries.getReplicatedToAllIndex(), lastIndex, replLog.getSnapshotIndex(),
                        replLog.getSnapshotTerm());

                sendOutOfSyncAppendEntriesReply(sender, false, appendEntries.getLeaderRaftVersion());
                return true;
            }

            final var entries = appendEntries.getEntries();
            if (entries.size() > 0 && !isLogEntryPresent(entries.get(0).index() - 1)) {
                final var replLog = context.getReplicatedLog();
                LOG.info("{}: Cannot append entries because the calculated previousIndex {} was not found in the "
                        + "in-memory journal - lastIndex: {}, snapshotIndex: {}, snapshotTerm: {}", logName,
                        entries.get(0).index() - 1, lastIndex, replLog.getSnapshotIndex(), replLog.getSnapshotTerm());

                sendOutOfSyncAppendEntriesReply(sender, false, appendEntries.getLeaderRaftVersion());
                return true;
            }
        }

        return false;
    }

    private void sendOutOfSyncAppendEntriesReply(final ActorRef sender, final boolean forceInstallSnapshot,
            final short leaderRaftVersion) {
        // We found that the log was out of sync so just send a negative reply.
        final var reply = new AppendEntriesReply(memberId(), currentTerm(), false, lastIndex(), lastTerm(),
            context.getPayloadVersion(), forceInstallSnapshot, needsLeaderAddress(), leaderRaftVersion);

        LOG.info("{}: Follower is out-of-sync so sending negative reply: {}", logName, reply);
        sender.tell(reply, actor());
    }

    private boolean needsLeaderAddress() {
        return context.getPeerAddress(leaderId) == null;
    }

    @Override
    final RaftActorBehavior handleAppendEntriesReply(final ActorRef sender,
            final AppendEntriesReply appendEntriesReply) {
        return this;
    }

    @Override
    final RaftActorBehavior handleRequestVoteReply(final ActorRef sender, final RequestVoteReply requestVoteReply) {
        return this;
    }

    @Override
    final ApplyState getApplyStateFor(final ReplicatedLogEntry entry) {
        return new ApplyState(null, null, entry);
    }

    @Override
    public RaftActorBehavior handleMessage(final ActorRef sender, final Object message) {
        if (message instanceof ElectionTimeout || message instanceof TimeoutNow) {
            return handleElectionTimeout(message);
        }

        if (appendEntriesMessageAssembler.handleMessage(message, actor())) {
            return this;
        }

        if (!(message instanceof RaftRPC rpc)) {
            // The rest of the processing requires the message to be a RaftRPC
            return null;
        }

        // If RPC request or response contains term T > currentTerm:
        // set currentTerm = T, convert to follower (§5.1)
        // This applies to all RPC messages and responses
        final var currentTerm = context.currentTerm();
        final var rpcTerm = rpc.getTerm();
        if (rpcTerm > currentTerm && shouldUpdateTerm(rpc)) {
            LOG.info("{}: Term {} in \"{}\" message is greater than follower's term {} - updating term",
                logName, rpcTerm, rpc, currentTerm);
            try {
                context.persistTermInfo(new TermInfo(rpcTerm));
            } catch (IOException e) {
                // FIXME: do not mask IOException
                throw new UncheckedIOException(e);
            }
        }

        if (rpc instanceof InstallSnapshot installSnapshot) {
            handleInstallSnapshot(sender, installSnapshot);
            restartLastLeaderMessageTimer();
            scheduleElection(electionDuration());
            return this;
        }

        if (!(rpc instanceof RequestVote requestVote) || canGrantVote(requestVote)) {
            restartLastLeaderMessageTimer();
            scheduleElection(electionDuration());
        }

        return super.handleMessage(sender, rpc);
    }

    private RaftActorBehavior handleElectionTimeout(final Object message) {
        // If the message is ElectionTimeout, verify we haven't actually seen a message from the leader
        // during the election timeout interval. It may that the election timer expired b/c this actor
        // was busy and messages got delayed, in which case leader messages would be backed up in the
        // queue but would be processed before the ElectionTimeout message and thus would restart the
        // lastLeaderMessageTimer.
        long lastLeaderMessageInterval = lastLeaderMessageTimer.elapsed(TimeUnit.MILLISECONDS);
        long electionTimeoutInMillis = context.getConfigParams().getElectionTimeOutInterval().toMillis();
        boolean noLeaderMessageReceived = !lastLeaderMessageTimer.isRunning()
                || lastLeaderMessageInterval >= electionTimeoutInMillis;

        if (canStartElection()) {
            if (message instanceof TimeoutNow) {
                LOG.debug("{}: Received TimeoutNow - switching to Candidate", logName);
                return switchBehavior(new Candidate(context));
            } else if (noLeaderMessageReceived) {
                // Check the cluster state to see if the leader is known to be up before we go to Candidate.
                // However if we haven't heard from the leader in a long time even though the cluster state
                // indicates it's up then something is wrong - leader might be stuck indefinitely - so switch
                // to Candidate,
                long maxElectionTimeout = electionTimeoutInMillis * MAX_ELECTION_TIMEOUT_FACTOR;
                if (isLeaderAvailabilityKnown() && lastLeaderMessageInterval < maxElectionTimeout) {
                    LOG.debug("{}: Received ElectionTimeout but leader appears to be available", logName);
                    scheduleElection(electionDuration());
                } else if (isThisFollowerIsolated()) {
                    LOG.debug("{}: this follower is isolated. Do not switch to Candidate for now.", logName);
                    setLeaderId(null);
                    scheduleElection(electionDuration());
                } else {
                    LOG.debug("{}: Received ElectionTimeout - switching to Candidate", logName);
                    return switchBehavior(new Candidate(context));
                }
            } else {
                LOG.debug("{}: Received ElectionTimeout but lastLeaderMessageInterval {} < election timeout {}",
                        logName, lastLeaderMessageInterval, context.getConfigParams().getElectionTimeOutInterval());
                scheduleElection(electionDuration());
            }
        } else if (message instanceof ElectionTimeout) {
            if (noLeaderMessageReceived) {
                setLeaderId(null);
            }

            scheduleElection(electionDuration());
        }

        return this;
    }

    private boolean isLeaderAvailabilityKnown() {
        if (leaderId == null) {
            return false;
        }

        final var cluster = context.cluster();
        if (cluster == null) {
            return false;
        }

        final var leaderActor = context.getPeerActorSelection(leaderId);
        if (leaderActor == null) {
            return false;
        }

        final var leaderAddress = leaderActor.anchorPath().address();

        final var state = cluster.state();
        final var unreachable = state.getUnreachable();

        LOG.debug("{}: Checking for leader {} in the cluster unreachable set {}", logName, leaderAddress,
                unreachable);

        for (var member : unreachable) {
            if (leaderAddress.equals(member.address())) {
                LOG.info("{}: Leader {} is unreachable", logName, leaderAddress);
                return false;
            }
        }

        for (var member : state.getMembers()) {
            if (leaderAddress.equals(member.address())) {
                final var status = member.status();
                if (status == MemberStatus.up() || status == MemberStatus.weaklyUp()) {
                    LOG.debug("{}: Leader {} cluster status is {} - leader is available", logName, leaderAddress,
                        status);
                    return true;
                }

                LOG.debug("{}: Leader {} cluster status is {} - leader is unavailable", logName, leaderAddress, status);
                return false;
            }
        }

        LOG.debug("{}: Leader {} not found in the cluster member set", logName, leaderAddress);
        return false;
    }

    private boolean isThisFollowerIsolated() {
        final var cluster = context.cluster();
        if (cluster == null) {
            return false;
        }

        final var selfMember = cluster.selfMember();
        final var state = cluster.state();
        final var unreachable = state.getUnreachable();
        final var members = state.getMembers();

        LOG.debug("{}: Checking if this node is isolated in the cluster unreachable set {},"
            + "all members {} self member: {}", logName, unreachable, members, selfMember);

        // no unreachable peers means we cannot be isolated
        if (unreachable.isEmpty()) {
            return false;
        }

        final var membersToCheck = new HashSet<Member>();
        members.forEach(membersToCheck::add);

        membersToCheck.removeAll(unreachable);

        // check if the only member not unreachable is us
        return membersToCheck.size() == 1 && membersToCheck.iterator().next().equals(selfMember);
    }

    private void handleInstallSnapshot(final ActorRef sender, final InstallSnapshot installSnapshot) {
        LOG.debug("{}: handleInstallSnapshot: {}", logName, installSnapshot);

        // update leader
        leaderId = installSnapshot.getLeaderId();
        if (snapshotTracker == null) {
            snapshotTracker = new SnapshotTracker(logName, installSnapshot.getTotalChunks(), leaderId, context);
        }

        updateInitialSyncStatus(installSnapshot.getLastIncludedIndex(), leaderId);

        final boolean isLastChunk;
        try {
            isLastChunk = snapshotTracker.addChunk(installSnapshot.getChunkIndex(), installSnapshot.getData(),
                installSnapshot.getLastChunkHashCode());
        } catch (IOException e) {
            LOG.debug("{}: failed to add InstallSnapshot chunk", logName, e);
            closeSnapshotTracker();
            sender.tell(new InstallSnapshotReply(currentTerm(), memberId(), -1, false), actor());
            return;
        }

        final var successReply = new InstallSnapshotReply(currentTerm(), memberId(), installSnapshot.getChunkIndex(),
            true);
        if (!isLastChunk) {
            LOG.debug("{}: handleInstallSnapshot returning: {}", logName, successReply);
            sender.tell(successReply, actor());
            return;
        }

        // Disconnect tracker from this instance
        final var tracker = snapshotTracker;
        snapshotTracker = null;

        LOG.info("{}: Snapshot received from leader: {}", logName, leaderId);
        final ByteSource snapshotBytes;
        try {
            snapshotBytes = tracker.getSnapshotBytes();
        } catch (IOException e) {
            LOG.debug("{}: failed to reconstract InstallSnapshot state", logName, e);
            tracker.close();
            sender.tell(new InstallSnapshotReply(currentTerm(), memberId(), -1, false), actor());
            return;
        }

        actor().tell(new ApplyLeaderSnapshot(leaderId, installSnapshot.getTerm(),
            ImmutableRaftEntryMeta.of(installSnapshot.getLastIncludedIndex(), installSnapshot.getLastIncludedTerm()),
            snapshotBytes, installSnapshot.serverConfig(), new ApplyLeaderSnapshot.Callback() {
                @Override
                public void onSuccess() {
                    LOG.debug("{}: handleInstallSnapshot returning: {}", logName, successReply);
                    tracker.close();
                    sender.tell(successReply, actor());
                }

                @Override
                public void onFailure() {
                    tracker.close();
                    sender.tell(new InstallSnapshotReply(currentTerm(), memberId(), -1, false), actor());
                }
            }), actor());
    }

    private void closeSnapshotTracker() {
        if (snapshotTracker != null) {
            snapshotTracker.close();
            snapshotTracker = null;
        }
    }

    @Override
    public final void close() {
        closeSnapshotTracker();
        stopElection();
        appendEntriesMessageAssembler.close();
    }

    @VisibleForTesting
    final SnapshotTracker getSnapshotTracker() {
        return snapshotTracker;
    }
}
