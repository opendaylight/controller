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
import akka.actor.Address;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.messaging.MessageAssembler;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotFinished;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * The behavior of a RaftActor in the Follower raft state.
 * <ul>
 * <li> Respond to RPCs from candidates and leaders
 * <li> If election timeout elapses without receiving AppendEntries
 * RPC from current leader or granting vote to candidate:
 * convert to candidate
 * </ul>
 */
public class Follower extends AbstractRaftActorBehavior {
    private static final long MAX_ELECTION_TIMEOUT_FACTOR = 18;

    private final SyncStatusTracker initialSyncStatusTracker;

    private final MessageAssembler appendEntriesMessageAssembler;

    private final Stopwatch lastLeaderMessageTimer = Stopwatch.createStarted();
    private SnapshotTracker snapshotTracker = null;
    private String leaderId;
    private short leaderPayloadVersion;

    public Follower(final RaftActorContext context) {
        this(context, null, (short)-1);
    }

    public Follower(final RaftActorContext context, final String initialLeaderId,
            final short initialLeaderPayloadVersion) {
        super(context, RaftState.Follower);
        this.leaderId = initialLeaderId;
        this.leaderPayloadVersion = initialLeaderPayloadVersion;

        initialSyncStatusTracker = new SyncStatusTracker(context.getActor(), getId(), context.getConfigParams()
            .getSyncIndexThreshold());

        appendEntriesMessageAssembler = MessageAssembler.builder().logContext(logName())
                .fileBackedStreamFactory(context.getFileBackedOutputStreamFactory())
                .assembledMessageCallback((message, sender) -> handleMessage(sender, message)).build();

        if (context.getPeerIds().isEmpty() && getLeaderId() == null) {
            actor().tell(TimeoutNow.INSTANCE, actor());
        } else {
            scheduleElection(electionDuration());
        }
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
    public short getLeaderPayloadVersion() {
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
        if (context.getReplicatedLog().isInSnapshot(index)) {
            return true;
        }

        ReplicatedLogEntry entry = context.getReplicatedLog().get(index);
        return entry != null;

    }

    private void updateInitialSyncStatus(final long currentLeaderCommit, final String newLeaderId) {
        initialSyncStatusTracker.update(newLeaderId, currentLeaderCommit, context.getCommitIndex());
    }

    @Override
    protected RaftActorBehavior handleAppendEntries(final ActorRef sender, final AppendEntries appendEntries) {
        int numLogEntries = appendEntries.getEntries().size();
        if (log.isTraceEnabled()) {
            log.trace("{}: handleAppendEntries: {}", logName(), appendEntries);
        } else if (log.isDebugEnabled() && numLogEntries > 0) {
            log.debug("{}: handleAppendEntries: {}", logName(), appendEntries);
        }

        if (snapshotTracker != null && !snapshotTracker.getLeaderId().equals(appendEntries.getLeaderId())) {
            log.debug("{}: snapshot install is in progress but the prior snapshot leaderId {} does not match the "
                + "AppendEntries leaderId {}", logName(), snapshotTracker.getLeaderId(), appendEntries.getLeaderId());
            closeSnapshotTracker();
        }

        if (snapshotTracker != null || context.getSnapshotManager().isApplying()) {
            // if snapshot install is in progress, follower should just acknowledge append entries with a reply.
            AppendEntriesReply reply = new AppendEntriesReply(context.getId(), currentTerm(), true,
                    lastIndex(), lastTerm(), context.getPayloadVersion(), false, needsLeaderAddress(),
                    appendEntries.getLeaderRaftVersion());

            log.debug("{}: snapshot install is in progress, replying immediately with {}", logName(), reply);
            sender.tell(reply, actor());

            return this;
        }

        // If we got here then we do appear to be talking to the leader
        leaderId = appendEntries.getLeaderId();
        leaderPayloadVersion = appendEntries.getPayloadVersion();

        if (appendEntries.getLeaderAddress().isPresent()) {
            final String address = appendEntries.getLeaderAddress().get();
            log.debug("New leader address: {}", address);

            context.setPeerAddress(leaderId, address);
            context.getConfigParams().getPeerAddressResolver().setResolved(leaderId, address);
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

        long lastIndex = lastIndex();
        long prevCommitIndex = context.getCommitIndex();

        // If leaderCommit > commitIndex, set commitIndex = min(leaderCommit, index of last new entry)
        if (appendEntries.getLeaderCommit() > prevCommitIndex) {
            context.setCommitIndex(Math.min(appendEntries.getLeaderCommit(), lastIndex));
        }

        if (prevCommitIndex != context.getCommitIndex()) {
            log.debug("{}: Commit index set to {}", logName(), context.getCommitIndex());
        }

        AppendEntriesReply reply = new AppendEntriesReply(context.getId(), currentTerm(), true,
                lastIndex, lastTerm(), context.getPayloadVersion(), false, needsLeaderAddress(),
                appendEntries.getLeaderRaftVersion());

        if (log.isTraceEnabled()) {
            log.trace("{}: handleAppendEntries returning : {}", logName(), reply);
        } else if (log.isDebugEnabled() && numLogEntries > 0) {
            log.debug("{}: handleAppendEntries returning : {}", logName(), reply);
        }

        // Reply to the leader before applying any previous state so as not to hold up leader consensus.
        sender.tell(reply, actor());

        updateInitialSyncStatus(appendEntries.getLeaderCommit(), appendEntries.getLeaderId());

        // If leaderCommit > lastApplied, increment lastApplied and apply log[lastApplied] to state machine (§5.3).
        // lastApplied can be equal to lastIndex.
        if (appendEntries.getLeaderCommit() > context.getLastApplied() && context.getLastApplied() < lastIndex) {
            if (log.isDebugEnabled()) {
                log.debug("{}: applyLogToStateMachine, appendEntries.getLeaderCommit(): {}, "
                        + "context.getLastApplied(): {}, lastIndex(): {}", logName(),
                    appendEntries.getLeaderCommit(), context.getLastApplied(), lastIndex);
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
        int numLogEntries = appendEntries.getEntries().size();
        if (numLogEntries == 0) {
            return true;
        }

        log.debug("{}: Number of entries to be appended = {}", logName(), numLogEntries);

        long lastIndex = lastIndex();
        int addEntriesFrom = 0;

        // First check for conflicting entries. If an existing entry conflicts with a new one (same index but different
        // term), delete the existing entry and all that follow it (§5.3)
        if (context.getReplicatedLog().size() > 0) {
            // Find the entry up until the one that is not in the follower's log
            for (int i = 0;i < numLogEntries; i++, addEntriesFrom++) {
                ReplicatedLogEntry matchEntry = appendEntries.getEntries().get(i);

                if (!isLogEntryPresent(matchEntry.getIndex())) {
                    // newEntry not found in the log
                    break;
                }

                long existingEntryTerm = getLogEntryTerm(matchEntry.getIndex());

                log.debug("{}: matchEntry {} is present: existingEntryTerm: {}", logName(), matchEntry,
                        existingEntryTerm);

                // existingEntryTerm == -1 means it's in the snapshot and not in the log. We don't know
                // what the term was so we'll assume it matches.
                if (existingEntryTerm == -1 || existingEntryTerm == matchEntry.getTerm()) {
                    continue;
                }

                if (!context.getRaftPolicy().applyModificationToStateBeforeConsensus()) {
                    log.info("{}: Removing entries from log starting at {}, commitIndex: {}, lastApplied: {}",
                            logName(), matchEntry.getIndex(), context.getCommitIndex(), context.getLastApplied());

                    // Entries do not match so remove all subsequent entries but only if the existing entries haven't
                    // been applied to the state yet.
                    if (matchEntry.getIndex() <= context.getLastApplied()
                            || !context.getReplicatedLog().removeFromAndPersist(matchEntry.getIndex())) {
                        // Could not remove the entries - this means the matchEntry index must be in the
                        // snapshot and not the log. In this case the prior entries are part of the state
                        // so we must send back a reply to force a snapshot to completely re-sync the
                        // follower's log and state.

                        log.info("{}: Could not remove entries - sending reply to force snapshot", logName());
                        sender.tell(new AppendEntriesReply(context.getId(), currentTerm(), false, lastIndex,
                                lastTerm(), context.getPayloadVersion(), true, needsLeaderAddress(),
                                appendEntries.getLeaderRaftVersion()), actor());
                        return false;
                    }

                    break;
                } else {
                    sender.tell(new AppendEntriesReply(context.getId(), currentTerm(), false, lastIndex,
                            lastTerm(), context.getPayloadVersion(), true, needsLeaderAddress(),
                            appendEntries.getLeaderRaftVersion()), actor());
                    return false;
                }
            }
        }

        lastIndex = lastIndex();
        log.debug("{}: After cleanup, lastIndex: {}, entries to be added from: {}", logName(), lastIndex,
                addEntriesFrom);

        // When persistence successfully completes for each new log entry appended, we need to determine if we
        // should capture a snapshot to compact the persisted log. shouldCaptureSnapshot tracks whether or not
        // one of the log entries has exceeded the log size threshold whereby a snapshot should be taken. However
        // we don't initiate the snapshot at that log entry but rather after the last log entry has been persisted.
        // This is done because subsequent log entries after the one that tripped the threshold may have been
        // applied to the state already, as the persistence callback occurs async, and we want those entries
        // purged from the persisted log as well.
        final AtomicBoolean shouldCaptureSnapshot = new AtomicBoolean(false);
        final Consumer<ReplicatedLogEntry> appendAndPersistCallback = logEntry -> {
            final List<ReplicatedLogEntry> entries = appendEntries.getEntries();
            final ReplicatedLogEntry lastEntryToAppend = entries.get(entries.size() - 1);
            if (shouldCaptureSnapshot.get() && logEntry == lastEntryToAppend) {
                context.getSnapshotManager().capture(context.getReplicatedLog().last(), getReplicatedToAllIndex());
            }
        };

        // Append any new entries not already in the log
        for (int i = addEntriesFrom; i < numLogEntries; i++) {
            ReplicatedLogEntry entry = appendEntries.getEntries().get(i);

            log.debug("{}: Append entry to log {}", logName(), entry.getData());

            context.getReplicatedLog().appendAndPersist(entry, appendAndPersistCallback, false);

            shouldCaptureSnapshot.compareAndSet(false,
                    context.getReplicatedLog().shouldCaptureSnapshot(entry.getIndex()));

            if (entry.getData() instanceof ServerConfigurationPayload) {
                context.updatePeerIds((ServerConfigurationPayload)entry.getData());
            }
        }

        log.debug("{}: Log size is now {}", logName(), context.getReplicatedLog().size());

        return true;
    }

    private boolean isOutOfSync(final AppendEntries appendEntries, final ActorRef sender) {

        final long lastIndex = lastIndex();
        if (lastIndex == -1 && appendEntries.getPrevLogIndex() != -1) {

            // The follower's log is out of sync because the leader does have an entry at prevLogIndex and this
            // follower has no entries in it's log.

            log.info("{}: The followers log is empty and the senders prevLogIndex is {}", logName(),
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

                    log.info("{}: The prevLogIndex {} was found in the log but the term {} is not equal to the append "
                        + "entries prevLogTerm {} - lastIndex: {}, snapshotIndex: {}, snapshotTerm: {}", logName(),
                        appendEntries.getPrevLogIndex(), leadersPrevLogTermInFollowersLogOrSnapshot,
                        appendEntries.getPrevLogTerm(), lastIndex, context.getReplicatedLog().getSnapshotIndex(),
                        context.getReplicatedLog().getSnapshotTerm());

                    sendOutOfSyncAppendEntriesReply(sender, false, appendEntries.getLeaderRaftVersion());
                    return true;
                }
            } else if (appendEntries.getPrevLogIndex() != -1) {

                // The follower's log is out of sync because the Leader's prevLogIndex entry was not found in it's log

                log.info("{}: The log is not empty but the prevLogIndex {} was not found in it - lastIndex: {}, "
                        + "snapshotIndex: {}, snapshotTerm: {}", logName(), appendEntries.getPrevLogIndex(), lastIndex,
                        context.getReplicatedLog().getSnapshotIndex(), context.getReplicatedLog().getSnapshotTerm());

                sendOutOfSyncAppendEntriesReply(sender, false, appendEntries.getLeaderRaftVersion());
                return true;
            }
        }

        if (appendEntries.getPrevLogIndex() == -1 && appendEntries.getPrevLogTerm() == -1
                && appendEntries.getReplicatedToAllIndex() != -1) {
            if (!isLogEntryPresent(appendEntries.getReplicatedToAllIndex())) {
                // This append entry comes from a leader who has it's log aggressively trimmed and so does not have
                // the previous entry in it's in-memory journal

                log.info("{}: Cannot append entries because the replicatedToAllIndex {} does not appear to be in the "
                        + "in-memory journal - lastIndex: {}, snapshotIndex: {}, snapshotTerm: {}", logName(),
                        appendEntries.getReplicatedToAllIndex(), lastIndex,
                        context.getReplicatedLog().getSnapshotIndex(), context.getReplicatedLog().getSnapshotTerm());

                sendOutOfSyncAppendEntriesReply(sender, false, appendEntries.getLeaderRaftVersion());
                return true;
            }

            final List<ReplicatedLogEntry> entries = appendEntries.getEntries();
            if (entries.size() > 0 && !isLogEntryPresent(entries.get(0).getIndex() - 1)) {
                log.info("{}: Cannot append entries because the calculated previousIndex {} was not found in the "
                        + "in-memory journal - lastIndex: {}, snapshotIndex: {}, snapshotTerm: {}", logName(),
                        entries.get(0).getIndex() - 1, lastIndex, context.getReplicatedLog().getSnapshotIndex(),
                        context.getReplicatedLog().getSnapshotTerm());

                sendOutOfSyncAppendEntriesReply(sender, false, appendEntries.getLeaderRaftVersion());
                return true;
            }
        }

        return false;
    }

    private void sendOutOfSyncAppendEntriesReply(final ActorRef sender, final boolean forceInstallSnapshot,
            final short leaderRaftVersion) {
        // We found that the log was out of sync so just send a negative reply.
        final AppendEntriesReply reply = new AppendEntriesReply(context.getId(), currentTerm(), false, lastIndex(),
                lastTerm(), context.getPayloadVersion(), forceInstallSnapshot, needsLeaderAddress(),
                leaderRaftVersion);

        log.info("{}: Follower is out-of-sync so sending negative reply: {}", logName(), reply);
        sender.tell(reply, actor());
    }

    private boolean needsLeaderAddress() {
        return context.getPeerAddress(leaderId) == null;
    }

    @Override
    protected RaftActorBehavior handleAppendEntriesReply(final ActorRef sender,
        final AppendEntriesReply appendEntriesReply) {
        return this;
    }

    @Override
    protected RaftActorBehavior handleRequestVoteReply(final ActorRef sender,
        final RequestVoteReply requestVoteReply) {
        return this;
    }

    @Override
    public RaftActorBehavior handleMessage(final ActorRef sender, final Object message) {
        if (message instanceof ElectionTimeout || message instanceof TimeoutNow) {
            return handleElectionTimeout(message);
        }

        if (appendEntriesMessageAssembler.handleMessage(message, actor())) {
            return this;
        }

        if (!(message instanceof RaftRPC)) {
            // The rest of the processing requires the message to be a RaftRPC
            return null;
        }

        final RaftRPC rpc = (RaftRPC) message;
        // If RPC request or response contains term T > currentTerm:
        // set currentTerm = T, convert to follower (§5.1)
        // This applies to all RPC messages and responses
        if (rpc.getTerm() > context.getTermInformation().getCurrentTerm() && shouldUpdateTerm(rpc)) {
            log.info("{}: Term {} in \"{}\" message is greater than follower's term {} - updating term",
                logName(), rpc.getTerm(), rpc, context.getTermInformation().getCurrentTerm());

            context.getTermInformation().updateAndPersist(rpc.getTerm(), null);
        }

        if (rpc instanceof InstallSnapshot) {
            handleInstallSnapshot(sender, (InstallSnapshot) rpc);
            restartLastLeaderMessageTimer();
            scheduleElection(electionDuration());
            return this;
        }

        if (!(rpc instanceof RequestVote) || canGrantVote((RequestVote) rpc)) {
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
                log.debug("{}: Received TimeoutNow - switching to Candidate", logName());
                return internalSwitchBehavior(RaftState.Candidate);
            } else if (noLeaderMessageReceived) {
                // Check the cluster state to see if the leader is known to be up before we go to Candidate.
                // However if we haven't heard from the leader in a long time even though the cluster state
                // indicates it's up then something is wrong - leader might be stuck indefinitely - so switch
                // to Candidate,
                long maxElectionTimeout = electionTimeoutInMillis * MAX_ELECTION_TIMEOUT_FACTOR;
                if (isLeaderAvailabilityKnown() && lastLeaderMessageInterval < maxElectionTimeout) {
                    log.debug("{}: Received ElectionTimeout but leader appears to be available", logName());
                    scheduleElection(electionDuration());
                } else {
                    log.debug("{}: Received ElectionTimeout - switching to Candidate", logName());
                    return internalSwitchBehavior(RaftState.Candidate);
                }
            } else {
                log.debug("{}: Received ElectionTimeout but lastLeaderMessageInterval {} < election timeout {}",
                        logName(), lastLeaderMessageInterval, context.getConfigParams().getElectionTimeOutInterval());
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

        Optional<Cluster> cluster = context.getCluster();
        if (!cluster.isPresent()) {
            return false;
        }

        ActorSelection leaderActor = context.getPeerActorSelection(leaderId);
        if (leaderActor == null) {
            return false;
        }

        Address leaderAddress = leaderActor.anchorPath().address();

        CurrentClusterState state = cluster.get().state();
        Set<Member> unreachable = state.getUnreachable();

        log.debug("{}: Checking for leader {} in the cluster unreachable set {}", logName(), leaderAddress,
                unreachable);

        for (Member m: unreachable) {
            if (leaderAddress.equals(m.address())) {
                log.info("{}: Leader {} is unreachable", logName(), leaderAddress);
                return false;
            }
        }

        for (Member m: state.getMembers()) {
            if (leaderAddress.equals(m.address())) {
                if (m.status() == MemberStatus.up() || m.status() == MemberStatus.weaklyUp()) {
                    log.debug("{}: Leader {} cluster status is {} - leader is available", logName(),
                            leaderAddress, m.status());
                    return true;
                } else {
                    log.debug("{}: Leader {} cluster status is {} - leader is unavailable", logName(),
                            leaderAddress, m.status());
                    return false;
                }
            }
        }

        log.debug("{}: Leader {} not found in the cluster member set", logName(), leaderAddress);

        return false;
    }

    private void handleInstallSnapshot(final ActorRef sender, final InstallSnapshot installSnapshot) {

        log.debug("{}: handleInstallSnapshot: {}", logName(), installSnapshot);

        leaderId = installSnapshot.getLeaderId();

        if (snapshotTracker == null) {
            snapshotTracker = new SnapshotTracker(log, installSnapshot.getTotalChunks(), installSnapshot.getLeaderId(),
                    context);
        }

        updateInitialSyncStatus(installSnapshot.getLastIncludedIndex(), installSnapshot.getLeaderId());

        try {
            final InstallSnapshotReply reply = new InstallSnapshotReply(
                    currentTerm(), context.getId(), installSnapshot.getChunkIndex(), true);

            if (snapshotTracker.addChunk(installSnapshot.getChunkIndex(), installSnapshot.getData(),
                    installSnapshot.getLastChunkHashCode())) {

                log.info("{}: Snapshot installed from leader: {}", logName(), installSnapshot.getLeaderId());

                Snapshot snapshot = Snapshot.create(
                        context.getSnapshotManager().convertSnapshot(snapshotTracker.getSnapshotBytes()),
                        new ArrayList<>(),
                        installSnapshot.getLastIncludedIndex(),
                        installSnapshot.getLastIncludedTerm(),
                        installSnapshot.getLastIncludedIndex(),
                        installSnapshot.getLastIncludedTerm(),
                        context.getTermInformation().getCurrentTerm(),
                        context.getTermInformation().getVotedFor(),
                        installSnapshot.getServerConfig().orElse(null));

                ApplySnapshot.Callback applySnapshotCallback = new ApplySnapshot.Callback() {
                    @Override
                    public void onSuccess() {
                        log.debug("{}: handleInstallSnapshot returning: {}", logName(), reply);

                        sender.tell(new InstallSnapshotFinished(currentTerm(), context.getId(), true), actor());
                    }

                    @Override
                    public void onFailure() {
                        sender.tell(new InstallSnapshotFinished(currentTerm(), context.getId(), false), actor());
                    }
                };

                sender.tell(reply, actor());
                actor().tell(new ApplySnapshot(snapshot, applySnapshotCallback), actor());

                closeSnapshotTracker();
            } else {
                log.debug("{}: handleInstallSnapshot returning: {}", logName(), reply);

                sender.tell(reply, actor());
            }
        } catch (IOException e) {
            log.debug("{}: Exception in InstallSnapshot of follower", logName(), e);

            sender.tell(new InstallSnapshotReply(currentTerm(), context.getId(),
                    -1, false), actor());

            closeSnapshotTracker();
        }
    }

    private void closeSnapshotTracker() {
        if (snapshotTracker != null) {
            snapshotTracker.close();
            snapshotTracker = null;
        }
    }

    @Override
    public void close() {
        closeSnapshotTracker();
        stopElection();
        appendEntriesMessageAssembler.close();
    }

    @VisibleForTesting
    SnapshotTracker getSnapshotTracker() {
        return snapshotTracker;
    }
}
