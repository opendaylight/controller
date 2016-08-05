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
import akka.japi.Procedure;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.Snapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;

/**
 * The behavior of a RaftActor in the Follower state
 * <p/>
 * <ul>
 * <li> Respond to RPCs from candidates and leaders
 * <li> If election timeout elapses without receiving AppendEntries
 * RPC from current leader or granting vote to candidate:
 * convert to candidate
 * </ul>
 */
public class Follower extends AbstractRaftActorBehavior {
    private static final int SYNC_THRESHOLD = 10;

    private static final long MAX_ELECTION_TIMEOUT_FACTOR = 18;

    private final SyncStatusTracker initialSyncStatusTracker;

    private final Procedure<ReplicatedLogEntry> appendAndPersistCallback = new Procedure<ReplicatedLogEntry>() {
        @Override
        public void apply(ReplicatedLogEntry logEntry) {
            context.getReplicatedLog().captureSnapshotIfReady(logEntry);
        }
    };

    private final Stopwatch lastLeaderMessageTimer = Stopwatch.createStarted();
    private SnapshotTracker snapshotTracker = null;
    private String leaderId;
    private short leaderPayloadVersion;

    public Follower(RaftActorContext context) {
        this(context, null, (short)-1);
    }

    public Follower(RaftActorContext context, String initialLeaderId, short initialLeaderPayloadVersion) {
        super(context, RaftState.Follower);
        this.leaderId = initialLeaderId;
        this.leaderPayloadVersion = initialLeaderPayloadVersion;

        initialSyncStatusTracker = new SyncStatusTracker(context.getActor(), getId(), SYNC_THRESHOLD);

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
    protected final void setLeaderId(@Nullable final String leaderId) {
        this.leaderId = leaderId;
    }

    @Override
    public short getLeaderPayloadVersion() {
        return leaderPayloadVersion;
    }

    @VisibleForTesting
    protected final void setLeaderPayloadVersion(short leaderPayloadVersion) {
        this.leaderPayloadVersion = leaderPayloadVersion;
    }

    private void restartLastLeaderMessageTimer() {
        if (lastLeaderMessageTimer.isRunning()) {
            lastLeaderMessageTimer.reset();
        }

        lastLeaderMessageTimer.start();
    }

    private boolean isLogEntryPresent(long index){
        if(context.getReplicatedLog().isInSnapshot(index)) {
            return true;
        }

        ReplicatedLogEntry entry = context.getReplicatedLog().get(index);
        return entry != null;

    }

    private void updateInitialSyncStatus(long currentLeaderCommit, String leaderId){
        initialSyncStatusTracker.update(leaderId, currentLeaderCommit, context.getCommitIndex());
    }

    @Override
    protected RaftActorBehavior handleAppendEntries(ActorRef sender, AppendEntries appendEntries) {

        int numLogEntries = appendEntries.getEntries() != null ? appendEntries.getEntries().size() : 0;
        if(LOG.isTraceEnabled()) {
            LOG.trace("{}: handleAppendEntries: {}", logName(), appendEntries);
        } else if(LOG.isDebugEnabled() && numLogEntries > 0) {
            LOG.debug("{}: handleAppendEntries: {}", logName(), appendEntries);
        }

        // TODO : Refactor this method into a bunch of smaller methods
        // to make it easier to read. Before refactoring ensure tests
        // cover the code properly

        if (snapshotTracker != null || context.getSnapshotManager().isApplying()) {
            // if snapshot install is in progress, follower should just acknowledge append entries with a reply.
            AppendEntriesReply reply = new AppendEntriesReply(context.getId(), currentTerm(), true,
                    lastIndex(), lastTerm(), context.getPayloadVersion());

            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: snapshot install is in progress, replying immediately with {}", logName(), reply);
            }
            sender.tell(reply, actor());

            return this;
        }

        // If we got here then we do appear to be talking to the leader
        leaderId = appendEntries.getLeaderId();
        leaderPayloadVersion = appendEntries.getPayloadVersion();

        updateInitialSyncStatus(appendEntries.getLeaderCommit(), appendEntries.getLeaderId());
        // First check if the logs are in sync or not
        long lastIndex = lastIndex();

        if (isOutOfSync(appendEntries)) {
            // We found that the log was out of sync so just send a negative
            // reply and return

            LOG.debug("{}: Follower is out-of-sync, so sending negative reply, lastIndex: {}, lastTerm: {}",
                        logName(), lastIndex, lastTerm());

            sender.tell(new AppendEntriesReply(context.getId(), currentTerm(), false, lastIndex,
                    lastTerm(), context.getPayloadVersion()), actor());
            return this;
        }

        if (appendEntries.getEntries() != null && appendEntries.getEntries().size() > 0) {

            LOG.debug("{}: Number of entries to be appended = {}", logName(),
                        appendEntries.getEntries().size());

            // 3. If an existing entry conflicts with a new one (same index
            // but different terms), delete the existing entry and all that
            // follow it (§5.3)
            int addEntriesFrom = 0;
            if (context.getReplicatedLog().size() > 0) {

                // Find the entry up until the one that is not in the follower's log
                for (int i = 0;i < appendEntries.getEntries().size(); i++, addEntriesFrom++) {
                    ReplicatedLogEntry matchEntry = appendEntries.getEntries().get(i);

                    if(!isLogEntryPresent(matchEntry.getIndex())) {
                        // newEntry not found in the log
                        break;
                    }

                    long existingEntryTerm = getLogEntryTerm(matchEntry.getIndex());

                    LOG.debug("{}: matchEntry {} is present: existingEntryTerm: {}", logName(), matchEntry,
                            existingEntryTerm);

                    // existingEntryTerm == -1 means it's in the snapshot and not in the log. We don't know
                    // what the term was so we'll assume it matches.
                    if(existingEntryTerm == -1 || existingEntryTerm == matchEntry.getTerm()) {
                        continue;
                    }

                    if(!context.getRaftPolicy().applyModificationToStateBeforeConsensus()) {

                        LOG.debug("{}: Removing entries from log starting at {}", logName(),
                                matchEntry.getIndex());

                        // Entries do not match so remove all subsequent entries
                        if(!context.getReplicatedLog().removeFromAndPersist(matchEntry.getIndex())) {
                            // Could not remove the entries - this means the matchEntry index must be in the
                            // snapshot and not the log. In this case the prior entries are part of the state
                            // so we must send back a reply to force a snapshot to completely re-sync the
                            // follower's log and state.

                            LOG.debug("{}: Could not remove entries - sending reply to force snapshot", logName());
                            sender.tell(new AppendEntriesReply(context.getId(), currentTerm(), false, lastIndex,
                                    lastTerm(), context.getPayloadVersion(), true), actor());
                            return this;
                        }

                        break;
                    } else {
                        sender.tell(new AppendEntriesReply(context.getId(), currentTerm(), false, lastIndex,
                                lastTerm(), context.getPayloadVersion(), true), actor());
                        return this;
                    }
                }
            }

            lastIndex = lastIndex();
            LOG.debug("{}: After cleanup, lastIndex: {}, entries to be added from: {}", logName(),
                    lastIndex, addEntriesFrom);

            // 4. Append any new entries not already in the log
            for (int i = addEntriesFrom; i < appendEntries.getEntries().size(); i++) {
                ReplicatedLogEntry entry = appendEntries.getEntries().get(i);

                LOG.debug("{}: Append entry to log {}", logName(), entry.getData());

                context.getReplicatedLog().appendAndPersist(entry, appendAndPersistCallback);

                if(entry.getData() instanceof ServerConfigurationPayload) {
                    context.updatePeerIds((ServerConfigurationPayload)entry.getData());
                }
            }

            LOG.debug("{}: Log size is now {}", logName(), context.getReplicatedLog().size());
        }

        // 5. If leaderCommit > commitIndex, set commitIndex =
        // min(leaderCommit, index of last new entry)

        lastIndex = lastIndex();
        long prevCommitIndex = context.getCommitIndex();

        context.setCommitIndex(Math.min(appendEntries.getLeaderCommit(), lastIndex));

        if (prevCommitIndex != context.getCommitIndex()) {
            LOG.debug("{}: Commit index set to {}", logName(), context.getCommitIndex());
        }

        // If commitIndex > lastApplied: increment lastApplied, apply
        // log[lastApplied] to state machine (§5.3)
        // check if there are any entries to be applied. last-applied can be equal to last-index
        if (appendEntries.getLeaderCommit() > context.getLastApplied() &&
            context.getLastApplied() < lastIndex) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: applyLogToStateMachine, " +
                        "appendEntries.getLeaderCommit(): {}," +
                        "context.getLastApplied(): {}, lastIndex(): {}", logName(),
                    appendEntries.getLeaderCommit(), context.getLastApplied(), lastIndex);
            }

            applyLogToStateMachine(appendEntries.getLeaderCommit());
        }

        AppendEntriesReply reply = new AppendEntriesReply(context.getId(), currentTerm(), true,
            lastIndex, lastTerm(), context.getPayloadVersion());

        if(LOG.isTraceEnabled()) {
            LOG.trace("{}: handleAppendEntries returning : {}", logName(), reply);
        } else if(LOG.isDebugEnabled() && numLogEntries > 0) {
            LOG.debug("{}: handleAppendEntries returning : {}", logName(), reply);
        }

        sender.tell(reply, actor());

        if (!context.getSnapshotManager().isCapturing()) {
            super.performSnapshotWithoutCapture(appendEntries.getReplicatedToAllIndex());
        }

        return this;
    }

    private boolean isOutOfSync(AppendEntries appendEntries) {

        long prevLogTerm = getLogEntryTerm(appendEntries.getPrevLogIndex());
        boolean prevEntryPresent = isLogEntryPresent(appendEntries.getPrevLogIndex());
        long lastIndex = lastIndex();
        int numLogEntries = appendEntries.getEntries() != null ? appendEntries.getEntries().size() : 0;
        boolean outOfSync = true;

        if (lastIndex == -1 && appendEntries.getPrevLogIndex() != -1) {

            // The follower's log is out of sync because the leader does have
            // an entry at prevLogIndex and this follower has no entries in
            // it's log.

            LOG.debug("{}: The followers log is empty and the senders prevLogIndex is {}",
                        logName(), appendEntries.getPrevLogIndex());
        } else if (lastIndex > -1 && appendEntries.getPrevLogIndex() != -1 && !prevEntryPresent) {

            // The follower's log is out of sync because the Leader's
            // prevLogIndex entry was not found in it's log

            LOG.debug("{}: The log is not empty but the prevLogIndex {} was not found in it - lastIndex: {}, snapshotIndex: {}",
                        logName(), appendEntries.getPrevLogIndex(), lastIndex, context.getReplicatedLog().getSnapshotIndex());
        } else if (lastIndex > -1 && prevEntryPresent && prevLogTerm != appendEntries.getPrevLogTerm()) {

            // The follower's log is out of sync because the Leader's
            // prevLogIndex entry does exist in the follower's log but it has
            // a different term in it

            LOG.debug(
                    "{}: Cannot append entries because previous entry term {}  is not equal to append entries prevLogTerm {}",
                    logName(), prevLogTerm, appendEntries.getPrevLogTerm());
        } else if(appendEntries.getPrevLogIndex() == -1 && appendEntries.getPrevLogTerm() == -1
                && appendEntries.getReplicatedToAllIndex() != -1
                && !isLogEntryPresent(appendEntries.getReplicatedToAllIndex())) {
            // This append entry comes from a leader who has it's log aggressively trimmed and so does not have
            // the previous entry in it's in-memory journal

            LOG.debug(
                    "{}: Cannot append entries because the replicatedToAllIndex {} does not appear to be in the in-memory journal",
                    logName(), appendEntries.getReplicatedToAllIndex());
        } else if(appendEntries.getPrevLogIndex() == -1 && appendEntries.getPrevLogTerm() == -1
                && appendEntries.getReplicatedToAllIndex() != -1 && numLogEntries > 0
                && !isLogEntryPresent(appendEntries.getEntries().get(0).getIndex() - 1)) {
            LOG.debug(
                    "{}: Cannot append entries because the calculated previousIndex {} was not found in the in-memory journal",
                    logName(), appendEntries.getEntries().get(0).getIndex() - 1);
        } else {
            outOfSync = false;
        }
        return outOfSync;
    }

    @Override
    protected RaftActorBehavior handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply) {
        return this;
    }

    @Override
    protected RaftActorBehavior handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply) {
        return this;
    }

    @Override
    public RaftActorBehavior handleMessage(ActorRef sender, Object message) {
        if (message instanceof ElectionTimeout || message instanceof TimeoutNow) {
            return handleElectionTimeout(message);
        }

        if (!(message instanceof RaftRPC)) {
            // The rest of the processing requires the message to be a RaftRPC
            return null;
        }

        final RaftRPC rpc = (RaftRPC) message;
        // If RPC request or response contains term T > currentTerm:
        // set currentTerm = T, convert to follower (§5.1)
        // This applies to all RPC messages and responses
        if (rpc.getTerm() > context.getTermInformation().getCurrentTerm()) {
            LOG.debug("{}: Term {} in \"{}\" message is greater than follower's term {} - updating term",
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

    private RaftActorBehavior handleElectionTimeout(Object message) {
        // If the message is ElectionTimeout, verify we haven't actually seen a message from the leader
        // during the election timeout interval. It may that the election timer expired b/c this actor
        // was busy and messages got delayed, in which case leader messages would be backed up in the
        // queue but would be processed before the ElectionTimeout message and thus would restart the
        // lastLeaderMessageTimer.
        long lastLeaderMessageInterval = lastLeaderMessageTimer.elapsed(TimeUnit.MILLISECONDS);
        long electionTimeoutInMillis = context.getConfigParams().getElectionTimeOutInterval().toMillis();
        boolean noLeaderMessageReceived = !lastLeaderMessageTimer.isRunning() ||
                lastLeaderMessageInterval >= electionTimeoutInMillis;

        if(canStartElection()) {
            if(message instanceof TimeoutNow) {
                LOG.debug("{}: Received TimeoutNow - switching to Candidate", logName());
                return internalSwitchBehavior(RaftState.Candidate);
            } else if(noLeaderMessageReceived) {
                // Check the cluster state to see if the leader is known to be up before we go to Candidate.
                // However if we haven't heard from the leader in a long time even though the cluster state
                // indicates it's up then something is wrong - leader might be stuck indefinitely - so switch
                // to Candidate,
                long maxElectionTimeout = electionTimeoutInMillis * MAX_ELECTION_TIMEOUT_FACTOR;
                if(isLeaderAvailabilityKnown() && lastLeaderMessageInterval < maxElectionTimeout) {
                    LOG.debug("{}: Received ElectionTimeout but leader appears to be available", logName());
                    scheduleElection(electionDuration());
                } else {
                    LOG.debug("{}: Received ElectionTimeout - switching to Candidate", logName());
                    return internalSwitchBehavior(RaftState.Candidate);
                }
            } else {
                LOG.debug("{}: Received ElectionTimeout but lastLeaderMessageInterval {} < election timeout {}",
                        logName(), lastLeaderMessageInterval, context.getConfigParams().getElectionTimeOutInterval());
                scheduleElection(electionDuration());
            }
        } else if(message instanceof ElectionTimeout) {
            if(noLeaderMessageReceived) {
                setLeaderId(null);
            }

            scheduleElection(electionDuration());
        }

        return this;
    }

    private boolean isLeaderAvailabilityKnown() {
        if(leaderId == null) {
            return false;
        }

        Optional<Cluster> cluster = context.getCluster();
        if(!cluster.isPresent()) {
            return false;
        }

        ActorSelection leaderActor = context.getPeerActorSelection(leaderId);
        if(leaderActor == null) {
            return false;
        }

        Address leaderAddress = leaderActor.anchorPath().address();

        CurrentClusterState state = cluster.get().state();
        Set<Member> unreachable = state.getUnreachable();

        LOG.debug("{}: Checking for leader {} in the cluster unreachable set {}", logName(), leaderAddress,
                unreachable);

        for(Member m: unreachable) {
            if(leaderAddress.equals(m.address())) {
                LOG.info("{}: Leader {} is unreachable", logName(), leaderAddress);
                return false;
            }
        }

        for(Member m: state.getMembers()) {
            if(leaderAddress.equals(m.address())) {
                if(m.status() == MemberStatus.up() || m.status() == MemberStatus.weaklyUp()) {
                    LOG.debug("{}: Leader {} cluster status is {} - leader is available", logName(),
                            leaderAddress, m.status());
                    return true;
                } else {
                    LOG.debug("{}: Leader {} cluster status is {} - leader is unavailable", logName(),
                            leaderAddress, m.status());
                    return false;
                }
            }
        }

        LOG.debug("{}: Leader {} not found in the cluster member set", logName(), leaderAddress);

        return false;
    }

    private void handleInstallSnapshot(final ActorRef sender, InstallSnapshot installSnapshot) {

        LOG.debug("{}: handleInstallSnapshot: {}", logName(), installSnapshot);

        leaderId = installSnapshot.getLeaderId();

        if(snapshotTracker == null){
            snapshotTracker = new SnapshotTracker(LOG, installSnapshot.getTotalChunks());
        }

        updateInitialSyncStatus(installSnapshot.getLastIncludedIndex(), installSnapshot.getLeaderId());

        try {
            final InstallSnapshotReply reply = new InstallSnapshotReply(
                    currentTerm(), context.getId(), installSnapshot.getChunkIndex(), true);

            if(snapshotTracker.addChunk(installSnapshot.getChunkIndex(), installSnapshot.getData(),
                    installSnapshot.getLastChunkHashCode())){
                Snapshot snapshot = Snapshot.create(snapshotTracker.getSnapshot(),
                        new ArrayList<ReplicatedLogEntry>(),
                        installSnapshot.getLastIncludedIndex(),
                        installSnapshot.getLastIncludedTerm(),
                        installSnapshot.getLastIncludedIndex(),
                        installSnapshot.getLastIncludedTerm(),
                        context.getTermInformation().getCurrentTerm(),
                        context.getTermInformation().getVotedFor(),
                        installSnapshot.getServerConfig().orNull());

                ApplySnapshot.Callback applySnapshotCallback = new ApplySnapshot.Callback() {
                    @Override
                    public void onSuccess() {
                        LOG.debug("{}: handleInstallSnapshot returning: {}", logName(), reply);

                        sender.tell(reply, actor());
                    }

                    @Override
                    public void onFailure() {
                        sender.tell(new InstallSnapshotReply(currentTerm(), context.getId(), -1, false), actor());
                    }
                };

                actor().tell(new ApplySnapshot(snapshot, applySnapshotCallback), actor());

                snapshotTracker = null;
            } else {
                LOG.debug("{}: handleInstallSnapshot returning: {}", logName(), reply);

                sender.tell(reply, actor());
            }
        } catch (SnapshotTracker.InvalidChunkException e) {
            LOG.debug("{}: Exception in InstallSnapshot of follower", logName(), e);

            sender.tell(new InstallSnapshotReply(currentTerm(), context.getId(),
                    -1, false), actor());
            snapshotTracker = null;

        } catch (Exception e){
            LOG.error("{}: Exception in InstallSnapshot of follower", logName(), e);

            //send reply with success as false. The chunk will be sent again on failure
            sender.tell(new InstallSnapshotReply(currentTerm(), context.getId(),
                    installSnapshot.getChunkIndex(), false), actor());

        }
    }

    @Override
    public void close() {
        stopElection();
    }

    @VisibleForTesting
    SnapshotTracker getSnapshotTracker(){
        return snapshotTracker;
    }
}
