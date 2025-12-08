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
import static org.opendaylight.controller.cluster.raft.MessageCollectorActor.clearMessages;
import static org.opendaylight.controller.cluster.raft.MessageCollectorActor.expectFirstMatching;
import static org.opendaylight.controller.cluster.raft.MessageCollectorActor.expectMatching;
import static org.opendaylight.controller.cluster.raft.RaftActorTestKit.awaitLastApplied;

import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.List;
import org.apache.pekko.actor.ActorRef;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.raft.api.RaftRole;

/**
 * Tests isolation of nodes end-to-end.
 *
 * @author Thomas Pantelis
 */
class IsolationScenarioTest extends AbstractRaftActorIntegrationTest {
    private ActorRef follower1NotifierActor;
    private ActorRef leaderNotifierActor;

    /**
     * Isolates the leader after all initial payload entries have been committed and applied on all nodes. While
     * isolated, the majority partition elects a new leader and both sides of the partition attempt to commit one entry
     * independently. After isolation is removed, the entry will conflict and both sides should reconcile their logs
     * appropriately.
     */
    @Test
    void testLeaderIsolationWithAllPriorEntriesCommitted() {
        testLog.info("testLeaderIsolationWithAllPriorEntriesCommitted starting");

        createRaftActors();

        // Send an initial payloads and verify replication.

        final var payload0 = sendPayloadData(leaderActor, "zero");
        final var payload1 = sendPayloadData(leaderActor, "one");
        awaitLastApplied(leaderActor, 1);
        awaitLastApplied(follower1Actor, 1);
        awaitLastApplied(follower2Actor, 1);

        isolateLeader();

        // Send a payload to the isolated leader so it has an uncommitted log entry with index 2.

        testLog.info("Sending payload to isolated leader");

        final MockCommand isolatedLeaderPayload2 = sendPayloadData(leaderActor, "two");

        // Wait for the isolated leader to send AppendEntries to follower1 with the entry at index 2. Note the message
        // is collected but not forwarded to the follower RaftActor.

        AppendEntries appendEntries = follower1Collector.expectFirstMatching(AppendEntries.class);
        assertEquals("getTerm", currentTerm, appendEntries.getTerm());
        assertEquals("getLeaderId", leaderId, appendEntries.getLeaderId());
        assertEquals("getEntries().size()", 1, appendEntries.getEntries().size());
        verifyReplicatedLogEntry(appendEntries.getEntries().get(0), currentTerm, 2, isolatedLeaderPayload2);

        // The leader should transition to IsolatedLeader.

        expectFirstMatching(leaderNotifierActor, RoleChanged.class, rc -> rc.newRole().equals(RaftRole.IsolatedLeader));

        forceElectionOnFollower1();

        // Send a payload to the new leader follower1 with index 2 and verify it's replicated to follower2
        // and committed.

        testLog.info("Sending payload to new leader");

        final var newLeaderPayload2 = sendPayloadData(follower1Actor, "two-new");
        awaitLastApplied(follower1Actor, 2);
        awaitLastApplied(follower2Actor, 2);

        final var follower1log = follower1Context.getReplicatedLog();
        assertEquals("Follower 1 journal last term", currentTerm, follower1log.lastTerm());
        assertEquals("Follower 1 journal last index", 2, follower1log.lastIndex());
        assertEquals("Follower 1 commit index", 2, follower1log.getCommitIndex());
        verifyReplicatedLogEntry(follower1log.lookup(2), currentTerm, 2, newLeaderPayload2);

        assertEquals("Follower 1 state", List.of(payload0, payload1, newLeaderPayload2),
                follower1Actor.underlyingActor().getState());

        removeIsolation();

        // Previous leader should switch to follower b/c it will receive either an AppendEntries or AppendEntriesReply
        // with a higher term.

        expectFirstMatching(leaderNotifierActor, RoleChanged.class, rc -> rc.newRole().equals(RaftRole.Follower));

        // The previous leader has a conflicting log entry at index 2 with a different term which should get
        // replaced by the new leader's index 1 entry.

        awaitLastApplied(leaderActor, 2);

        final var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Prior leader journal last term", currentTerm, leaderLog.lastTerm());
        assertEquals("Prior leader journal last index", 2, leaderLog.lastIndex());
        assertEquals("Prior leader commit index", 2, leaderLog.getCommitIndex());
        verifyReplicatedLogEntry(leaderLog.lookup(2), currentTerm, 2, newLeaderPayload2);

        assertEquals("Prior leader state", List.of(payload0, payload1, newLeaderPayload2),
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
    void testLeaderIsolationWithPriorUncommittedEntryAndOneConflictingEntry() {
        testLog.info("testLeaderIsolationWithPriorUncommittedEntryAndOneConflictingEntry starting");

        createRaftActors();

        // Submit an initial payload that is committed/applied on all nodes.

        final MockCommand payload0 = sendPayloadData(leaderActor, "zero");
        awaitLastApplied(leaderActor, 0);
        awaitLastApplied(follower1Actor, 0);
        awaitLastApplied(follower2Actor, 0);

        // Submit another payload that is replicated to all followers and committed on the leader but the leader is
        // isolated before the entry is committed on the followers. To accomplish this we drop the AppendEntries
        // with the updated leader commit index.

        follower1Actor.underlyingActor().startDropMessages(AppendEntries.class, ae -> ae.getLeaderCommit() == 1);
        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class, ae -> ae.getLeaderCommit() == 1);

        MockCommand payload1 = sendPayloadData(leaderActor, "one");

        // Wait for the isolated leader to send AppendEntries to the followers with the new entry with index 1. This
        // message is forwarded to the followers.

        follower1Collector.expectFirstMatching(AppendEntries.class, ae ->
                ae.getEntries().size() == 1 && ae.getEntries().getFirst().index() == 1
                        && ae.getEntries().getFirst().command().equals(payload1));

        follower2CollectorActor.expectFirstMatching(AppendEntries.class, ae ->
                ae.getEntries().size() == 1 && ae.getEntries().getFirst().index() == 1
                        && ae.getEntries().getFirst().command().equals(payload1));

        awaitLastApplied(leaderActor, 1);

        isolateLeader();

        // Send a payload to the isolated leader so it has an uncommitted log entry with index 2.

        testLog.info("Sending payload to isolated leader");

        final MockCommand isolatedLeaderPayload2 = sendPayloadData(leaderActor, "two");

        // Wait for the isolated leader to send AppendEntries to follower1 with the entry at index 2. Note the message
        // is collected but not forwarded to the follower RaftActor.

        AppendEntries appendEntries = follower1Collector.expectFirstMatching(AppendEntries.class);
        assertEquals("getTerm", currentTerm, appendEntries.getTerm());
        assertEquals("getLeaderId", leaderId, appendEntries.getLeaderId());
        assertEquals("getEntries().size()", 1, appendEntries.getEntries().size());
        verifyReplicatedLogEntry(appendEntries.getEntries().get(0), currentTerm, 2, isolatedLeaderPayload2);

        // The leader should transition to IsolatedLeader.

        expectFirstMatching(leaderNotifierActor, RoleChanged.class, rc -> rc.newRole().equals(RaftRole.IsolatedLeader));

        forceElectionOnFollower1();

        // Send a payload to the new leader follower1 and verify it's replicated to follower2 and committed. Since the
        // entry with index 1 from the previous term was uncommitted, the new leader should've also committed a
        // NoopPayload entry with index 2 in the PreLeader state. Thus the new payload will have index 3.

        testLog.info("Sending payload to new leader");

        final var newLeaderPayload2 = sendPayloadData(follower1Actor, "two-new");
        awaitLastApplied(follower1Actor, 3);
        awaitLastApplied(follower2Actor, 3);

        final var follower1log = follower1Context.getReplicatedLog();
        assertEquals("Follower 1 journal last term", currentTerm, follower1log.lastTerm());
        assertEquals("Follower 1 journal last index", 3, follower1log.lastIndex());
        assertEquals("Follower 1 commit index", 3, follower1log.getCommitIndex());
        verifyReplicatedLogEntry(follower1log.lookup(3), currentTerm, 3, newLeaderPayload2);

        assertEquals("Follower 1 state", List.of(payload0, payload1, newLeaderPayload2),
                follower1Actor.underlyingActor().getState());

        removeIsolation();

        // Previous leader should switch to follower b/c it will receive either an AppendEntries or AppendEntriesReply
        // with a higher term.

        expectFirstMatching(leaderNotifierActor, RoleChanged.class, rc -> rc.newRole().equals(RaftRole.Follower));

        // The previous leader has a conflicting log entry at index 2 with a different term which should get
        // replaced by the new leader's entry.

        awaitLastApplied(leaderActor, 3);

        verifyRaftState(leaderActor, raftState -> {
            final var leaderLog = leaderContext.getReplicatedLog();
            assertEquals("Prior leader journal last term", currentTerm, leaderLog.lastTerm());
            assertEquals("Prior leader journal last index", 3, leaderLog.lastIndex());
            assertEquals("Prior leader commit index", 3, leaderLog.getCommitIndex());
        });

        assertEquals("Prior leader state", List.of(payload0, payload1, newLeaderPayload2),
                leaderActor.underlyingActor().getState());

        // Ensure the prior leader didn't apply its conflicting entry with index 2, term 1.

        final var applyState = leaderCollector.getAllMatching(ApplyState.class);
        for (var as : applyState) {
            if (as.entry().index() == 2 && as.entry().term() == 1) {
                fail("Got unexpected ApplyState: " + as);
            }
        }

        // The prior leader should not have needed a snapshot installed in order to get it synced.

        leaderCollector.assertNoneMatching(InstallSnapshot.class);

        testLog.info("testLeaderIsolationWithPriorUncommittedEntryAndOneConflictingEntry ending");
    }

    /**
     * Isolates the leader with a payload entry that's replicated to all followers and committed on the leader but
     * uncommitted on the followers. While isolated, the majority partition elects a new leader and both sides of the
     * partition attempt to commit multiple entries independently. After isolation is removed, the entries will conflict
     * and both sides should reconcile their logs appropriately.
     */
    @Test
    void testLeaderIsolationWithPriorUncommittedEntryAndMultipleConflictingEntries() {
        testLog.info("testLeaderIsolationWithPriorUncommittedEntryAndMultipleConflictingEntries starting");

        createRaftActors();

        // Submit an initial payload that is committed/applied on all nodes.

        final MockCommand payload0 = sendPayloadData(leaderActor, "zero");
        awaitLastApplied(leaderActor, 0);
        awaitLastApplied(follower1Actor, 0);
        awaitLastApplied(follower2Actor, 0);

        // Submit another payload that is replicated to all followers and committed on the leader but the leader is
        // isolated before the entry is committed on the followers. To accomplish this we drop the AppendEntries
        // with the updated leader commit index.

        follower1Actor.underlyingActor().startDropMessages(AppendEntries.class, ae -> ae.getLeaderCommit() == 1);
        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class, ae -> ae.getLeaderCommit() == 1);

        MockCommand payload1 = sendPayloadData(leaderActor, "one");

        // Wait for the isolated leader to send AppendEntries to the followers with the new entry with index 1. This
        // message is forwarded to the followers.

        follower1Collector.expectFirstMatching(AppendEntries.class, ae ->
                ae.getEntries().size() == 1 && ae.getEntries().getFirst().index() == 1
                        && ae.getEntries().getFirst().command().equals(payload1));

        follower2Collector.expectFirstMatching(AppendEntries.class, ae ->
                ae.getEntries().size() == 1 && ae.getEntries().getFirst().index() == 1
                        && ae.getEntries().getFirst().command().equals(payload1));

        awaitLastApplied(leaderActor, 1);

        isolateLeader();

        // Send 3 payloads to the isolated leader so it has uncommitted log entries.

        testLog.info("Sending 3 payloads to isolated leader");

        sendPayloadData(leaderActor, "two");
        sendPayloadData(leaderActor, "three");
        sendPayloadData(leaderActor, "four");

        // Wait for the isolated leader to send AppendEntries to follower1 for each new entry. Note the messages
        // are collected but not forwarded to the follower RaftActor.

        follower1Collector.expectFirstMatching(AppendEntries.class, ae -> {
            for (var entry : ae.getEntries()) {
                if (entry.index() == 4) {
                    return true;
                }
            }
            return false;
        });

        // The leader should transition to IsolatedLeader.

        expectFirstMatching(leaderNotifierActor, RoleChanged.class, rc -> rc.newRole().equals(RaftRole.IsolatedLeader));

        forceElectionOnFollower1();

        // Send 3 payloads to the new leader follower1 and verify they're replicated to follower2 and committed. Since
        // the entry with index 1 from the previous term was uncommitted, the new leader should've also committed a
        // NoopPayload entry with index 2 in the PreLeader state. Thus the new payload indices will start at 3.

        testLog.info("Sending 3 payloads to new leader");

        final var newLeaderPayload2 = sendPayloadData(follower1Actor, "two-new");
        final var newLeaderPayload3 = sendPayloadData(follower1Actor, "three-new");
        final var newLeaderPayload4 = sendPayloadData(follower1Actor, "four-new");
        awaitLastApplied(follower1Actor, 5);
        awaitLastApplied(follower2Actor, 5);

        final var follower1log = follower1Context.getReplicatedLog();
        assertEquals("Follower 1 journal last term", currentTerm, follower1log.lastTerm());
        assertEquals("Follower 1 journal last index", 5, follower1log.lastIndex());
        assertEquals("Follower 1 commit index", 5, follower1log.getCommitIndex());
        verifyReplicatedLogEntry(follower1Context.getReplicatedLog().lookup(5), currentTerm, 5, newLeaderPayload4);

        assertEquals("Follower 1 state", List.of(payload0, payload1, newLeaderPayload2, newLeaderPayload3,
                newLeaderPayload4), follower1Actor.underlyingActor().getState());

        removeIsolation();

        // Previous leader should switch to follower b/c it will receive either an AppendEntries or AppendEntriesReply
        // with a higher term.

        expectFirstMatching(leaderNotifierActor, RoleChanged.class, rc -> rc.newRole().equals(RaftRole.Follower));

        // The previous leader has conflicting log entries starting at index 2 with different terms which should get
        // replaced by the new leader's entries.

        awaitLastApplied(leaderActor, 5);

        verifyRaftState(leaderActor, raftState -> {
            final var leaderLog = leaderContext.getReplicatedLog();
            assertEquals("Prior leader journal last term", currentTerm, leaderLog.lastTerm());
            assertEquals("Prior leader journal last index", 5, leaderLog.lastIndex());
            assertEquals("Prior leader commit index", 5, leaderLog.getCommitIndex());
        });

        assertEquals("Prior leader state",
            List.of(payload0, payload1, newLeaderPayload2, newLeaderPayload3, newLeaderPayload4),
            leaderActor.underlyingActor().getState());

        // Ensure the prior leader didn't apply any of its conflicting entries with term 1.

        final var applyState = leaderCollector.getAllMatching(ApplyState.class);
        for (var as : applyState) {
            if (as.entry().term() == 1) {
                fail("Got unexpected ApplyState: " + as);
            }
        }

        // The prior leader should not have needed a snapshot installed in order to get it synced.

        leaderCollector.assertNoneMatching(InstallSnapshot.class);

        testLog.info("testLeaderIsolationWithPriorUncommittedEntryAndMultipleConflictingEntries ending");
    }

    private void removeIsolation() {
        testLog.info("Removing isolation");

        clearMessages(leaderNotifierActor);
        leaderCollector.clearMessages();

        leaderActor.underlyingActor().stopDropMessages(AppendEntries.class);
        leaderActor.underlyingActor().stopDropMessages(RequestVote.class);
        follower1Actor.underlyingActor().stopDropMessages(AppendEntries.class);
        follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);
    }

    private void forceElectionOnFollower1() {
        // Force follower1 to start an election. follower2 should grant the vote.

        testLog.info("Forcing election on {}", follower1Id);

        follower1Actor.tell(TimeoutNow.INSTANCE, ActorRef.noSender());

        expectFirstMatching(follower1NotifierActor, RoleChanged.class, rc -> rc.newRole().equals(RaftRole.Leader));

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

        follower1Collector.clearMessages();
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
