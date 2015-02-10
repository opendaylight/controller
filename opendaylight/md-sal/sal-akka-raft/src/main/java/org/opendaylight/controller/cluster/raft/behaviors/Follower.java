/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.Snapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;

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

    private SnapshotTracker snapshotTracker = null;

    public Follower(RaftActorContext context) {
        super(context, RaftState.Follower);

        scheduleElection(electionDuration());
    }

    private boolean isLogEntryPresent(long index){
        if(index == context.getReplicatedLog().getSnapshotIndex()){
            return true;
        }

        ReplicatedLogEntry previousEntry = context.getReplicatedLog()
                .get(index);

        return previousEntry != null;

    }

    private long getLogEntryTerm(long index){
        if(index == context.getReplicatedLog().getSnapshotIndex()){
            return context.getReplicatedLog().getSnapshotTerm();
        }

        ReplicatedLogEntry previousEntry = context.getReplicatedLog()
                .get(index);

        if(previousEntry != null){
            return previousEntry.getTerm();
        }

        return -1;
    }

    @Override protected RaftActorBehavior handleAppendEntries(ActorRef sender,
                                                              AppendEntries appendEntries) {

        int numLogEntries = appendEntries.getEntries() != null ? appendEntries.getEntries().size() : 0;
        if(LOG.isTraceEnabled()) {
            LOG.trace("{}: handleAppendEntries: {}", logName(), appendEntries);
        } else if(LOG.isDebugEnabled() && numLogEntries > 0) {
            LOG.debug("{}: handleAppendEntries: {}", logName(), appendEntries);
        }

        // TODO : Refactor this method into a bunch of smaller methods
        // to make it easier to read. Before refactoring ensure tests
        // cover the code properly

        // 1. Reply false if term < currentTerm (§5.1)
        // This is handled in the appendEntries method of the base class

        // If we got here then we do appear to be talking to the leader
        leaderId = appendEntries.getLeaderId();

        // 2. Reply false if log doesn’t contain an entry at prevLogIndex
        // whose term matches prevLogTerm (§5.3)

        long prevLogTerm = getLogEntryTerm(appendEntries.getPrevLogIndex());
        boolean prevEntryPresent = isLogEntryPresent(appendEntries.getPrevLogIndex());


        boolean outOfSync = true;

        // First check if the logs are in sync or not
        long lastIndex = lastIndex();
        if (lastIndex == -1 && appendEntries.getPrevLogIndex() != -1) {

            // The follower's log is out of sync because the leader does have
            // an entry at prevLogIndex and this follower has no entries in
            // it's log.

            LOG.debug("{}: The followers log is empty and the senders prevLogIndex is {}",
                        logName(), appendEntries.getPrevLogIndex());
        } else if (lastIndex > -1 && appendEntries.getPrevLogIndex() != -1 && !prevEntryPresent) {

            // The follower's log is out of sync because the Leader's
            // prevLogIndex entry was not found in it's log

            LOG.debug("{}: The log is not empty but the prevLogIndex {} was not found in it",
                        logName(), appendEntries.getPrevLogIndex());
        } else if (lastIndex > -1 && prevEntryPresent && prevLogTerm != appendEntries.getPrevLogTerm()) {

            // The follower's log is out of sync because the Leader's
            // prevLogIndex entry does exist in the follower's log but it has
            // a different term in it

            LOG.debug(
                "{}: Cannot append entries because previous entry term {}  is not equal to append entries prevLogTerm {}",
                 logName(), prevLogTerm, appendEntries.getPrevLogTerm());
        } else {
            outOfSync = false;
        }

        if (outOfSync) {
            // We found that the log was out of sync so just send a negative
            // reply and return

            LOG.debug("{}: Follower is out-of-sync, so sending negative reply, lastIndex: {}, lastTerm: {}",
                        logName(), lastIndex, lastTerm());

            sender.tell(new AppendEntriesReply(context.getId(), currentTerm(), false, lastIndex,
                    lastTerm()), actor());
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

                // Find the entry up until which the one that is not in the follower's log
                for (int i = 0;i < appendEntries.getEntries().size(); i++, addEntriesFrom++) {
                    ReplicatedLogEntry matchEntry = appendEntries.getEntries().get(i);
                    ReplicatedLogEntry newEntry = context.getReplicatedLog().get(matchEntry.getIndex());

                    if (newEntry == null) {
                        //newEntry not found in the log
                        break;
                    }

                    if (newEntry.getTerm() == matchEntry.getTerm()) {
                        continue;
                    }

                    LOG.debug("{}: Removing entries from log starting at {}", logName(),
                                matchEntry.getIndex());

                    // Entries do not match so remove all subsequent entries
                    context.getReplicatedLog().removeFromAndPersist(matchEntry.getIndex());
                    break;
                }
            }

            lastIndex = lastIndex();
            LOG.debug("{}: After cleanup entries to be added from = {}", logName(),
                        (addEntriesFrom + lastIndex));

            // 4. Append any new entries not already in the log
            for (int i = addEntriesFrom; i < appendEntries.getEntries().size(); i++) {
                ReplicatedLogEntry entry = appendEntries.getEntries().get(i);

                LOG.debug("{}: Append entry to log {}", logName(), entry.getData());

                context.getReplicatedLog().appendAndPersist(entry);
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
            lastIndex, lastTerm());

        if(LOG.isTraceEnabled()) {
            LOG.trace("{}: handleAppendEntries returning : {}", logName(), reply);
        } else if(LOG.isDebugEnabled() && numLogEntries > 0) {
            LOG.debug("{}: handleAppendEntries returning : {}", logName(), reply);
        }

        sender.tell(reply, actor());

        if (!context.isSnapshotCaptureInitiated()) {
            fakeSnapshot(appendEntries.getReplicatedToAllIndex(), appendEntries.getReplicatedToAllIndex());
        }

        return this;
    }

    @Override protected RaftActorBehavior handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply) {
        return this;
    }

    @Override protected RaftActorBehavior handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply) {
        return this;
    }

    @Override public RaftActorBehavior handleMessage(ActorRef sender, Object originalMessage) {

        Object message = fromSerializableMessage(originalMessage);

        if (message instanceof RaftRPC) {
            RaftRPC rpc = (RaftRPC) message;
            // If RPC request or response contains term T > currentTerm:
            // set currentTerm = T, convert to follower (§5.1)
            // This applies to all RPC messages and responses
            if (rpc.getTerm() > context.getTermInformation().getCurrentTerm()) {
                LOG.debug("{}: Term {} in \"{}\" message is greater than follower's term {} - updating term",
                        logName(), rpc.getTerm(), rpc, context.getTermInformation().getCurrentTerm());

                context.getTermInformation().updateAndPersist(rpc.getTerm(), null);
            }
        }

        if (message instanceof ElectionTimeout) {
            LOG.debug("{}: Received ElectionTimeout - switching to Candidate", logName());
            return switchBehavior(new Candidate(context));

        } else if (message instanceof InstallSnapshot) {
            InstallSnapshot installSnapshot = (InstallSnapshot) message;
            handleInstallSnapshot(sender, installSnapshot);
        }

        scheduleElection(electionDuration());

        return super.handleMessage(sender, message);
    }

    private void handleInstallSnapshot(ActorRef sender, InstallSnapshot installSnapshot) {


        LOG.debug("{}: InstallSnapshot received from leader {}, datasize: {} , Chunk: {}/{}",
                    logName(), installSnapshot.getLeaderId(), installSnapshot.getData().size(),
                    installSnapshot.getChunkIndex(), installSnapshot.getTotalChunks());

        if(snapshotTracker == null){
            snapshotTracker = new SnapshotTracker(LOG, installSnapshot.getTotalChunks());
        }

        try {
            if(snapshotTracker.addChunk(installSnapshot.getChunkIndex(), installSnapshot.getData(),
                    installSnapshot.getLastChunkHashCode())){
                Snapshot snapshot = Snapshot.create(snapshotTracker.getSnapshot(),
                        new ArrayList<ReplicatedLogEntry>(),
                        installSnapshot.getLastIncludedIndex(),
                        installSnapshot.getLastIncludedTerm(),
                        installSnapshot.getLastIncludedIndex(),
                        installSnapshot.getLastIncludedTerm());

                actor().tell(new ApplySnapshot(snapshot), actor());

                snapshotTracker = null;

            }

            InstallSnapshotReply reply = new InstallSnapshotReply(
                    currentTerm(), context.getId(), installSnapshot.getChunkIndex(), true);

            LOG.debug("{}: handleInstallSnapshot returning: {}", logName(), reply);

            sender.tell(reply, actor());

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

    @Override public void close() throws Exception {
        stopElection();
    }

    @VisibleForTesting
    ByteString getSnapshotChunksCollected(){
        return snapshotTracker != null ? snapshotTracker.getCollectedChunks() : ByteString.EMPTY;
    }
}
