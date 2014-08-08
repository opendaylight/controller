/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
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
    public Follower(RaftActorContext context) {
        super(context);

        scheduleElection(electionDuration());
    }

    @Override protected RaftState handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries) {

        if(appendEntries.getEntries() != null && appendEntries.getEntries().size() > 0) {
            context.getLogger()
                .debug(appendEntries.toString());
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

        ReplicatedLogEntry previousEntry = context.getReplicatedLog()
            .get(appendEntries.getPrevLogIndex());


        boolean outOfSync = true;

        // First check if the logs are in sync or not
        if (lastIndex() == -1
            && appendEntries.getPrevLogIndex() != -1) {

            // The follower's log is out of sync because the leader does have
            // an entry at prevLogIndex and this follower has no entries in
            // it's log.

            context.getLogger().debug(
                "The followers log is empty and the senders prevLogIndex is {}",
                appendEntries.getPrevLogIndex());

        } else if (lastIndex() > -1
            && appendEntries.getPrevLogIndex() != -1
            && previousEntry == null) {

            // The follower's log is out of sync because the Leader's
            // prevLogIndex entry was not found in it's log

            context.getLogger().debug(
                "The log is not empty but the prevLogIndex {} was not found in it",
                appendEntries.getPrevLogIndex());

        } else if (lastIndex() > -1
            && previousEntry != null
            && previousEntry.getTerm()!= appendEntries.getPrevLogTerm()) {

            // The follower's log is out of sync because the Leader's
            // prevLogIndex entry does exist in the follower's log but it has
            // a different term in it

            context.getLogger().debug(
                "Cannot append entries because previous entry term {}  is not equal to append entries prevLogTerm {}"
                , previousEntry.getTerm()
                , appendEntries.getPrevLogTerm());
        } else {
            outOfSync = false;
        }

        if (outOfSync) {
            // We found that the log was out of sync so just send a negative
            // reply and return
            sender.tell(
                new AppendEntriesReply(context.getId(), currentTerm(), false,
                    lastIndex(), lastTerm()), actor()
            );
            return state();
        }

        if (appendEntries.getEntries() != null
            && appendEntries.getEntries().size() > 0) {
            context.getLogger().debug(
                "Number of entries to be appended = " + appendEntries
                    .getEntries().size()
            );

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

                    if (newEntry.getTerm() == matchEntry
                        .getTerm()) {
                        continue;
                    }

                    context.getLogger().debug(
                        "Removing entries from log starting at "
                            + matchEntry.getIndex()
                    );

                    // Entries do not match so remove all subsequent entries
                    context.getReplicatedLog()
                        .removeFromAndPersist(matchEntry.getIndex());
                    break;
                }
            }

            context.getLogger().debug(
                "After cleanup entries to be added from = " + (addEntriesFrom
                    + lastIndex())
            );

            // 4. Append any new entries not already in the log
            for (int i = addEntriesFrom;
                 i < appendEntries.getEntries().size(); i++) {

                context.getLogger().info(
                    "Append entry to log " + appendEntries.getEntries().get(
                        i).getData()
                        .toString()
                );
                context.getReplicatedLog()
                    .appendAndPersist(appendEntries.getEntries().get(i));
            }

            context.getLogger().debug(
                "Log size is now " + context.getReplicatedLog().size());
        }


        // 5. If leaderCommit > commitIndex, set commitIndex =
        // min(leaderCommit, index of last new entry)

        long prevCommitIndex = context.getCommitIndex();

        context.setCommitIndex(Math.min(appendEntries.getLeaderCommit(),
            context.getReplicatedLog().lastIndex()));

        if (prevCommitIndex != context.getCommitIndex()) {
            context.getLogger()
                .debug("Commit index set to " + context.getCommitIndex());
        }

        // If commitIndex > lastApplied: increment lastApplied, apply
        // log[lastApplied] to state machine (§5.3)
        if (appendEntries.getLeaderCommit() > context.getLastApplied()) {
            applyLogToStateMachine(appendEntries.getLeaderCommit());
        }

        sender.tell(new AppendEntriesReply(context.getId(), currentTerm(), true,
            lastIndex(), lastTerm()), actor());

        return state();
    }

    @Override protected RaftState handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply) {
        return state();
    }

    @Override protected RaftState handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply) {
        return state();
    }

    @Override public RaftState state() {
        return RaftState.Follower;
    }

    @Override public RaftState handleMessage(ActorRef sender, Object originalMessage) {

        Object message = fromSerializableMessage(originalMessage);

        if (message instanceof RaftRPC) {
            RaftRPC rpc = (RaftRPC) message;
            // If RPC request or response contains term T > currentTerm:
            // set currentTerm = T, convert to follower (§5.1)
            // This applies to all RPC messages and responses
            if (rpc.getTerm() > context.getTermInformation().getCurrentTerm()) {
                context.getTermInformation().updateAndPersist(rpc.getTerm(), null);
            }
        }

        if (message instanceof ElectionTimeout) {
            return RaftState.Candidate;

        } else if (message instanceof InstallSnapshot) {
            InstallSnapshot installSnapshot = (InstallSnapshot) message;
            actor().tell(new ApplySnapshot(installSnapshot.getData()), actor());
        }

        scheduleElection(electionDuration());

        return super.handleMessage(sender, message);
    }

    @Override public void close() throws Exception {
        stopElection();
    }
}
