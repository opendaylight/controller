/*
 * Copyright (c) 2016 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.assertNoneMatching;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.clearMessages;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectFirstMatching;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectMatching;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.getAllMatching;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.List;
import org.apache.pekko.actor.ActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

/**
 * Tests isolation of nodes end-to-end.
 *
 * @author Thomas Pantelis
 */
public class IsolationScenarioTest extends AbstractRaftActorIntegrationTest {
    private ActorRef follower1NotifierActor;
    private ActorRef leaderNotifierActor;

    /**
     * Isolates the leader after all initial payload entries have been committed and applied on all nodes. While
     * isolated, the majority partition elects a new leader and both sides of the partition attempt to commit one entry
     * independently. After isolation is removed, the entry will conflict and both sides should reconcile their logs
     * appropriately.
     */
    @Test
    public void testLeaderIsolationWithAllPriorEntriesCommitted() {
        testLog.info("testLeaderIsolationWithAllPriorEntriesCommitted starting");

        createRaftActors();

        // Send an initial payloads and verify replication.

        final MockPayload payload0 = sendPayloadData(leaderActor, "zero");
        final MockPayload payload1 = sendPayloadData(leaderActor, "one");
        verifyApplyJournalEntries(leaderCollectorActor, 1);
        verifyApplyJournalEntries(follower1CollectorActor, 1);
        verifyApplyJournalEntries(follower2CollectorActor, 1);

        isolateLeader();

        // Send a payload to the isolated leader so it has an uncommitted log entry with index 2.

        testLog.info("Sending payload to isolated leader");

        final MockPayload isolatedLeaderPayload2 = sendPayloadData(leaderActor, "two");

        // Wait for the isolated leader to send AppendEntries to follower1 with the entry at index 2. Note the message
        // is collected but not forwarded to the follower RaftActor.

        AppendEntries appendEntries = expectFirstMatching(follower1CollectorActor, AppendEntries.class);
        assertEquals("getTerm", currentTerm, appendEntries.getTerm());
        assertEquals("getLeaderId", leaderId, appendEntries.getLeaderId());
        assertEquals("getEntries().size()", 1, appendEntries.getEntries().size());
        verifyReplicatedLogEntry(appendEntries.getEntries().get(0), currentTerm, 2, isolatedLeaderPayload2);

        // The leader should transition to IsolatedLeader.

        expectFirstMatching(leaderNotifierActor, RoleChanged.class,
            rc -> rc.getNewRole().equals(RaftState.IsolatedLeader.name()));

        forceElectionOnFollower1();

        // Send a payload to the new leader follower1 with index 2 and verify it's replicated to follower2
        // and committed.

        testLog.info("Sending payload to new leader");

        final MockPayload newLeaderPayload2 = sendPayloadData(follower1Actor, "two-new");
        verifyApplyJournalEntries(follower1CollectorActor, 2);
        verifyApplyJournalEntries(follower2CollectorActor, 2);

        assertEquals("Follower 1 journal last term", currentTerm, follower1Context.getReplicatedLog().lastTerm());
        assertEquals("Follower 1 journal last index", 2, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower 1 commit index", 2, follower1Context.getCommitIndex());
        verifyReplicatedLogEntry(follower1Context.getReplicatedLog().get(2), currentTerm, 2, newLeaderPayload2);

        assertEquals("Follower 1 state", Lists.newArrayList(payload0, payload1, newLeaderPayload2),
                follower1Actor.underlyingActor().getState());

        removeIsolation();

        // Previous leader should switch to follower b/c it will receive either an AppendEntries or AppendEntriesReply
        // with a higher term.

        expectFirstMatching(leaderNotifierActor, RoleChanged.class,
            rc -> rc.getNewRole().equals(RaftState.Follower.name()));

        // The previous leader has a conflicting log entry at index 2 with a different term which should get
        // replaced by the new leader's index 1 entry.

        verifyApplyJournalEntries(leaderCollectorActor, 2);

        assertEquals("Prior leader journal last term", currentTerm, leaderContext.getReplicatedLog().lastTerm());
        assertEquals("Prior leader journal last index", 2, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Prior leader commit index", 2, leaderContext.getCommitIndex());
        verifyReplicatedLogEntry(leaderContext.getReplicatedLog().get(2), currentTerm, 2, newLeaderPayload2);

        assertEquals("Prior leader state", Lists.newArrayList(payload0, payload1, newLeaderPayload2),
                leaderActor.underlyingActor().getState());

        testLog.info("testLeaderIsolationWithAllPriorEntriesCommitted ending");
    }

    /**
     * Isolates the leader with a payload entry that's replicated to all followers and committed on the leader but
     * uncommitted on the followers. While isolated, the majority partition elects a new leader and both sides of the
     * partition attempt to commit one entry independently. After isolation is removed, the entry will conflict and both
     * sides should reconcile their logs appropriately.
     */
    @Test
    public void testLeaderIsolationWithPriorUncommittedEntryAndOneConflictingEntry() {
        testLog.info("testLeaderIsolationWithPriorUncommittedEntryAndOneConflictingEntry starting");

        createRaftActors();

        // Submit an initial payload that is committed/applied on all nodes.

        final MockPayload payload0 = sendPayloadData(leaderActor, "zero");
        verifyApplyJournalEntries(leaderCollectorActor, 0);
        verifyApplyJournalEntries(follower1CollectorActor, 0);
        verifyApplyJournalEntries(follower2CollectorActor, 0);

        // Submit another payload that is replicated to all followers and committed on the leader but the leader is
        // isolated before the entry is committed on the followers. To accomplish this we drop the AppendEntries
        // with the updated leader commit index.

        follower1Actor.underlyingActor().startDropMessages(AppendEntries.class, ae -> ae.getLeaderCommit() == 1);
        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class, ae -> ae.getLeaderCommit() == 1);

        MockPayload payload1 = sendPayloadData(leaderActor, "one");

        // Wait for the isolated leader to send AppendEntries to the followers with the new entry with index 1. This
        // message is forwarded to the followers.

        expectFirstMatching(follower1CollectorActor, AppendEntries.class, ae ->
                ae.getEntries().size() == 1 && ae.getEntries().get(0).index() == 1
                        && ae.getEntries().get(0).getData().equals(payload1));

        expectFirstMatching(follower2CollectorActor, AppendEntries.class, ae ->
                ae.getEntries().size() == 1 && ae.getEntries().get(0).index() == 1
                        && ae.getEntries().get(0).getData().equals(payload1));

        verifyApplyJournalEntries(leaderCollectorActor, 1);

        isolateLeader();

        // Send a payload to the isolated leader so it has an uncommitted log entry with index 2.

        testLog.info("Sending payload to isolated leader");

        final MockPayload isolatedLeaderPayload2 = sendPayloadData(leaderActor, "two");

        // Wait for the isolated leader to send AppendEntries to follower1 with the entry at index 2. Note the message
        // is collected but not forwarded to the follower RaftActor.

        AppendEntries appendEntries = expectFirstMatching(follower1CollectorActor, AppendEntries.class);
        assertEquals("getTerm", currentTerm, appendEntries.getTerm());
        assertEquals("getLeaderId", leaderId, appendEntries.getLeaderId());
        assertEquals("getEntries().size()", 1, appendEntries.getEntries().size());
        verifyReplicatedLogEntry(appendEntries.getEntries().get(0), currentTerm, 2, isolatedLeaderPayload2);

        // The leader should transition to IsolatedLeader.

        expectFirstMatching(leaderNotifierActor, RoleChanged.class,
            rc -> rc.getNewRole().equals(RaftState.IsolatedLeader.name()));

        forceElectionOnFollower1();

        // Send a payload to the new leader follower1 and verify it's replicated to follower2 and committed. Since the
        // entry with index 1 from the previous term was uncommitted, the new leader should've also committed a
        // NoopPayload entry with index 2 in the PreLeader state. Thus the new payload will have index 3.

        testLog.info("Sending payload to new leader");

        final MockPayload newLeaderPayload2 = sendPayloadData(follower1Actor, "two-new");
        verifyApplyJournalEntries(follower1CollectorActor, 3);
        verifyApplyJournalEntries(follower2CollectorActor, 3);

        assertEquals("Follower 1 journal last term", currentTerm, follower1Context.getReplicatedLog().lastTerm());
        assertEquals("Follower 1 journal last index", 3, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower 1 commit index", 3, follower1Context.getCommitIndex());
        verifyReplicatedLogEntry(follower1Context.getReplicatedLog().get(3), currentTerm, 3, newLeaderPayload2);

        assertEquals("Follower 1 state", Lists.newArrayList(payload0, payload1, newLeaderPayload2),
                follower1Actor.underlyingActor().getState());

        removeIsolation();

        // Previous leader should switch to follower b/c it will receive either an AppendEntries or AppendEntriesReply
        // with a higher term.

        expectFirstMatching(leaderNotifierActor, RoleChanged.class,
            rc -> rc.getNewRole().equals(RaftState.Follower.name()));

        // The previous leader has a conflicting log entry at index 2 with a different term which should get
        // replaced by the new leader's entry.

        verifyApplyJournalEntries(leaderCollectorActor, 3);

        verifyRaftState(leaderActor, raftState -> {
            assertEquals("Prior leader journal last term", currentTerm, leaderContext.getReplicatedLog().lastTerm());
            assertEquals("Prior leader journal last index", 3, leaderContext.getReplicatedLog().lastIndex());
            assertEquals("Prior leader commit index", 3, leaderContext.getCommitIndex());
        });

        assertEquals("Prior leader state", Lists.newArrayList(payload0, payload1, newLeaderPayload2),
                leaderActor.underlyingActor().getState());

        // Ensure the prior leader didn't apply its conflicting entry with index 2, term 1.

        List<ApplyState> applyState = getAllMatching(leaderCollectorActor, ApplyState.class);
        for (ApplyState as: applyState) {
            if (as.getReplicatedLogEntry().index() == 2 && as.getReplicatedLogEntry().term() == 1) {
                fail("Got unexpected ApplyState: " + as);
            }
        }

        // The prior leader should not have needed a snapshot installed in order to get it synced.

        assertNoneMatching(leaderCollectorActor, InstallSnapshot.class);

        testLog.info("testLeaderIsolationWithPriorUncommittedEntryAndOneConflictingEntry ending");
    }

    /**
     * Isolates the leader with a payload entry that's replicated to all followers and committed on the leader but
     * uncommitted on the followers. While isolated, the majority partition elects a new leader and both sides of the
     * partition attempt to commit multiple entries independently. After isolation is removed, the entries will conflict
     * and both sides should reconcile their logs appropriately.
     */
    @Test
    public void testLeaderIsolationWithPriorUncommittedEntryAndMultipleConflictingEntries() {
        testLog.info("testLeaderIsolationWithPriorUncommittedEntryAndMultipleConflictingEntries starting");

        createRaftActors();

        // Submit an initial payload that is committed/applied on all nodes.

        final MockPayload payload0 = sendPayloadData(leaderActor, "zero");
        verifyApplyJournalEntries(leaderCollectorActor, 0);
        verifyApplyJournalEntries(follower1CollectorActor, 0);
        verifyApplyJournalEntries(follower2CollectorActor, 0);

        // Submit another payload that is replicated to all followers and committed on the leader but the leader is
        // isolated before the entry is committed on the followers. To accomplish this we drop the AppendEntries
        // with the updated leader commit index.

        follower1Actor.underlyingActor().startDropMessages(AppendEntries.class, ae -> ae.getLeaderCommit() == 1);
        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class, ae -> ae.getLeaderCommit() == 1);

        MockPayload payload1 = sendPayloadData(leaderActor, "one");

        // Wait for the isolated leader to send AppendEntries to the followers with the new entry with index 1. This
        // message is forwarded to the followers.

        expectFirstMatching(follower1CollectorActor, AppendEntries.class, ae ->
                ae.getEntries().size() == 1 && ae.getEntries().get(0).index() == 1
                        && ae.getEntries().get(0).getData().equals(payload1));

        expectFirstMatching(follower2CollectorActor, AppendEntries.class, ae ->
                ae.getEntries().size() == 1 && ae.getEntries().get(0).index() == 1
                        && ae.getEntries().get(0).getData().equals(payload1));

        verifyApplyJournalEntries(leaderCollectorActor, 1);

        isolateLeader();

        // Send 3 payloads to the isolated leader so it has uncommitted log entries.

        testLog.info("Sending 3 payloads to isolated leader");

        sendPayloadData(leaderActor, "two");
        sendPayloadData(leaderActor, "three");
        sendPayloadData(leaderActor, "four");

        // Wait for the isolated leader to send AppendEntries to follower1 for each new entry. Note the messages
        // are collected but not forwarded to the follower RaftActor.

        expectFirstMatching(follower1CollectorActor, AppendEntries.class, ae -> {
            for (var entry : ae.getEntries()) {
                if (entry.index() == 4) {
                    return true;
                }
            }
            return false;
        });

        // The leader should transition to IsolatedLeader.

        expectFirstMatching(leaderNotifierActor, RoleChanged.class,
            rc -> rc.getNewRole().equals(RaftState.IsolatedLeader.name()));

        forceElectionOnFollower1();

        // Send 3 payloads to the new leader follower1 and verify they're replicated to follower2 and committed. Since
        // the entry with index 1 from the previous term was uncommitted, the new leader should've also committed a
        // NoopPayload entry with index 2 in the PreLeader state. Thus the new payload indices will start at 3.

        testLog.info("Sending 3 payloads to new leader");

        final MockPayload newLeaderPayload2 = sendPayloadData(follower1Actor, "two-new");
        final MockPayload newLeaderPayload3 = sendPayloadData(follower1Actor, "three-new");
        final MockPayload newLeaderPayload4 = sendPayloadData(follower1Actor, "four-new");
        verifyApplyJournalEntries(follower1CollectorActor, 5);
        verifyApplyJournalEntries(follower2CollectorActor, 5);

        assertEquals("Follower 1 journal last term", currentTerm, follower1Context.getReplicatedLog().lastTerm());
        assertEquals("Follower 1 journal last index", 5, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower 1 commit index", 5, follower1Context.getCommitIndex());
        verifyReplicatedLogEntry(follower1Context.getReplicatedLog().get(5), currentTerm, 5, newLeaderPayload4);

        assertEquals("Follower 1 state", Lists.newArrayList(payload0, payload1, newLeaderPayload2, newLeaderPayload3,
                newLeaderPayload4), follower1Actor.underlyingActor().getState());

        removeIsolation();

        // Previous leader should switch to follower b/c it will receive either an AppendEntries or AppendEntriesReply
        // with a higher term.

        expectFirstMatching(leaderNotifierActor, RoleChanged.class,
            rc -> rc.getNewRole().equals(RaftState.Follower.name()));

        // The previous leader has conflicting log entries starting at index 2 with different terms which should get
        // replaced by the new leader's entries.

        verifyApplyJournalEntries(leaderCollectorActor, 5);

        verifyRaftState(leaderActor, raftState -> {
            assertEquals("Prior leader journal last term", currentTerm, leaderContext.getReplicatedLog().lastTerm());
            assertEquals("Prior leader journal last index", 5, leaderContext.getReplicatedLog().lastIndex());
            assertEquals("Prior leader commit index", 5, leaderContext.getCommitIndex());
        });

        assertEquals("Prior leader state", Lists.newArrayList(payload0, payload1, newLeaderPayload2, newLeaderPayload3,
                newLeaderPayload4), leaderActor.underlyingActor().getState());

        // Ensure the prior leader didn't apply any of its conflicting entries with term 1.

        List<ApplyState> applyState = getAllMatching(leaderCollectorActor, ApplyState.class);
        for (ApplyState as: applyState) {
            if (as.getReplicatedLogEntry().term() == 1) {
                fail("Got unexpected ApplyState: " + as);
            }
        }

        // The prior leader should not have needed a snapshot installed in order to get it synced.

        assertNoneMatching(leaderCollectorActor, InstallSnapshot.class);

        testLog.info("testLeaderIsolationWithPriorUncommittedEntryAndMultipleConflictingEntries ending");
    }

    private void removeIsolation() {
        testLog.info("Removing isolation");

        clearMessages(leaderNotifierActor);
        clearMessages(leaderCollectorActor);

        leaderActor.underlyingActor().stopDropMessages(AppendEntries.class);
        leaderActor.underlyingActor().stopDropMessages(RequestVote.class);
        follower1Actor.underlyingActor().stopDropMessages(AppendEntries.class);
        follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);
    }

    private void forceElectionOnFollower1() {
        // Force follower1 to start an election. follower2 should grant the vote.

        testLog.info("Forcing election on {}", follower1Id);

        follower1Actor.tell(TimeoutNow.INSTANCE, ActorRef.noSender());

        expectFirstMatching(follower1NotifierActor, RoleChanged.class,
            rc -> rc.getNewRole().equals(RaftState.Leader.name()));

        currentTerm = follower1Context.currentTerm();
    }

    private void isolateLeader() {
        // Isolate the leader by dropping AppendEntries to the followers and incoming messages from the followers.

        testLog.info("Isolating the leader");

        leaderActor.underlyingActor().startDropMessages(AppendEntries.class);
        leaderActor.underlyingActor().startDropMessages(RequestVote.class);

        follower1Actor.underlyingActor().startDropMessages(AppendEntries.class,
            ae -> ae.getLeaderId().equals(leaderId));
        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class,
            ae -> ae.getLeaderId().equals(leaderId));

        clearMessages(follower1CollectorActor);
        clearMessages(follower1NotifierActor);
        clearMessages(leaderNotifierActor);
    }

    private void createRaftActors() {
        testLog.info("createRaftActors starting");

        follower1NotifierActor = factory.createActor(MessageCollectorActor.props(),
                factory.generateActorId(follower1Id + "-notifier"));

        DefaultConfigParamsImpl followerConfigParams = new DefaultConfigParamsImpl();
        followerConfigParams.setHeartBeatInterval(Duration.ofMillis(100));
        followerConfigParams.setElectionTimeoutFactor(1000);
        follower1Actor = newTestRaftActor(follower1Id, TestRaftActor.newBuilder()
            .peerAddresses(ImmutableMap.of(leaderId, testActorPath(leaderId), follower2Id, testActorPath(follower2Id)))
                .config(followerConfigParams).roleChangeNotifier(follower1NotifierActor));

        follower2Actor = newTestRaftActor(follower2Id, ImmutableMap.of(leaderId, testActorPath(leaderId),
                follower1Id, testActorPath(follower1Id)), followerConfigParams);

        peerAddresses = ImmutableMap.<String, String>builder()
                .put(follower1Id, follower1Actor.path().toString())
                .put(follower2Id, follower2Actor.path().toString()).build();

        leaderConfigParams = newLeaderConfigParams();
        leaderConfigParams.setIsolatedLeaderCheckInterval(Duration.ofMillis(500));

        leaderNotifierActor = factory.createActor(MessageCollectorActor.props(),
                factory.generateActorId(leaderId + "-notifier"));

        leaderActor = newTestRaftActor(leaderId, TestRaftActor.newBuilder()
            .peerAddresses(peerAddresses).config(leaderConfigParams).roleChangeNotifier(leaderNotifierActor));

        follower1CollectorActor = follower1Actor.underlyingActor().collectorActor();
        follower2CollectorActor = follower2Actor.underlyingActor().collectorActor();
        leaderCollectorActor = leaderActor.underlyingActor().collectorActor();

        leaderActor.tell(TimeoutNow.INSTANCE, ActorRef.noSender());
        waitUntilLeader(leaderActor);

        expectMatching(leaderCollectorActor, AppendEntriesReply.class, 2);

        clearMessages(leaderCollectorActor);
        clearMessages(follower1CollectorActor);
        clearMessages(follower2CollectorActor);

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();
        currentTerm = leaderContext.currentTerm();

        follower1Context = follower1Actor.underlyingActor().getRaftActorContext();
        follower2Context = follower2Actor.underlyingActor().getRaftActorContext();

        testLog.info("createRaftActors ending");
    }
}
