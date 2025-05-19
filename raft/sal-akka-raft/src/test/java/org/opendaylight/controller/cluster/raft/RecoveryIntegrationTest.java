/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import org.apache.pekko.actor.ActorRef;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.SnapshotManager.ApplyLeaderSnapshot;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;

/**
 * Tests raft actor persistence recovery end-to-end using real RaftActors and behavior communication.
 *
 * @author Thomas Pantelis
 */
public class RecoveryIntegrationTest extends AbstractRaftActorIntegrationTest {
    private MockCommand payload0;
    private MockCommand payload1;

    @Before
    public void setup() {
        follower1Actor = newTestRaftActor(follower1Id, Map.of(leaderId, testActorPath(leaderId)),
                newFollowerConfigParams());

        leaderConfigParams = newLeaderConfigParams();
        leaderActor = newTestRaftActor(leaderId, Map.of(follower1Id, follower1Actor.path().toString(), follower2Id, ""),
            leaderConfigParams);

        follower1CollectorActor = follower1Actor.underlyingActor().collectorActor();
        leaderCollectorActor = leaderActor.underlyingActor().collectorActor();

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();
    }

    @Test
    public void testStatePersistedBetweenSnapshotCaptureAndPersist() {

        send2InitialPayloads();

        // Block these messages initially so we can control the sequence.
        final var leaderPersistence = CapturingDataPersistenceProvider.installTo(leaderActor.underlyingActor());

        follower1Actor.underlyingActor().startDropMessages(AppendEntries.class);

        final MockCommand payload2 = sendPayloadData(leaderActor, "two");

        // This should trigger a snapshot.
        final MockCommand payload3 = sendPayloadData(leaderActor, "three");

        MessageCollectorActor.expectMatching(follower1CollectorActor, AppendEntries.class, 3);

        // Send another payload.
        final MockCommand payload4 = sendPayloadData(leaderActor, "four");

        // Now deliver the AppendEntries to the follower
        follower1Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        verifyApplyIndex(leaderActor, 4);

        // Now complete the snapshot
        leaderPersistence.awaitSaveSnapshot().complete();

        reinstateLeaderActor();

        final var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader snapshot term", currentTerm, leaderLog.getSnapshotTerm());
        assertEquals("Leader snapshot index", 1, leaderLog.getSnapshotIndex());
        assertEquals("Leader journal log size", 3, leaderLog.size());
        assertEquals("Leader journal last index", 4, leaderLog.lastIndex());
        assertEquals("Leader commit index", 4, leaderLog.getCommitIndex());
        assertEquals("Leader last applied", 4, leaderLog.getLastApplied());

        assertEquals("Leader state", List.of(payload0, payload1, payload2, payload3, payload4),
                leaderActor.underlyingActor().getState());
    }

    @Test
    public void testStatePersistedAfterSnapshotPersisted() {

        send2InitialPayloads();

        // Block these messages initially so we can control the sequence.
        follower1Actor.underlyingActor().startDropMessages(AppendEntries.class);

        final MockCommand payload2 = sendPayloadData(leaderActor, "two");

        // This should trigger a snapshot.
        final MockCommand payload3 = sendPayloadData(leaderActor, "three");

        // Send another payload.
        final MockCommand payload4 = sendPayloadData(leaderActor, "four");

        MessageCollectorActor.expectMatching(follower1CollectorActor, AppendEntries.class, 3);

        // Wait for snapshot complete.
        awaitSnapshot(leaderActor);

        // Now deliver the AppendEntries to the follower
        follower1Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        verifyApplyIndex(leaderActor, 4);

        reinstateLeaderActor();

        final var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader snapshot term", currentTerm, leaderLog.getSnapshotTerm());
        assertEquals("Leader snapshot index", 1, leaderLog.getSnapshotIndex());
        assertEquals("Leader journal log size", 3, leaderLog.size());
        assertEquals("Leader journal last index", 4, leaderLog.lastIndex());
        assertEquals("Leader commit index", 4, leaderLog.getCommitIndex());
        assertEquals("Leader last applied", 4, leaderLog.getLastApplied());

        assertEquals("Leader state", List.of(payload0, payload1, payload2, payload3, payload4),
                leaderActor.underlyingActor().getState());
    }

    @Test
    public void testFollowerRecoveryAfterInstallSnapshot() {

        send2InitialPayloads();

        leader = leaderActor.underlyingActor().getCurrentBehavior();

        follower2Actor = newTestRaftActor(follower2Id,
                Map.of(leaderId, testActorPath(leaderId)), newFollowerConfigParams());
        follower2CollectorActor = follower2Actor.underlyingActor().collectorActor();

        leaderActor.tell(new SetPeerAddress(follower2Id, follower2Actor.path().toString()), ActorRef.noSender());

        final MockCommand payload2 = sendPayloadData(leaderActor, "two");

        // Verify the leader applies the 3rd payload state.
        verifyApplyIndex(leaderActor, 2);
        verifyApplyIndex(follower2Actor, 2);

        final var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader commit index", 2, leaderLog.getCommitIndex());
        assertEquals("Leader last applied", 2, leaderLog.getLastApplied());
        assertEquals("Leader snapshot index", 1, leaderLog.getSnapshotIndex());
        assertEquals("Leader replicatedToAllIndex", 1, leader.getReplicatedToAllIndex());

        killActor(follower2Actor);

        InMemoryJournal.clear();

        follower2Actor = newTestRaftActor(follower2Id,
                Map.of(leaderId, testActorPath(leaderId)), newFollowerConfigParams());
        TestRaftActor follower2Underlying = follower2Actor.underlyingActor();
        follower2CollectorActor = follower2Underlying.collectorActor();
        follower2Context = follower2Underlying.getRaftActorContext();

        leaderActor.tell(new SetPeerAddress(follower2Id, follower2Actor.path().toString()), ActorRef.noSender());

        // The leader should install a snapshot so wait for the follower to receive ApplySnapshot.
        MessageCollectorActor.expectFirstMatching(follower2CollectorActor, ApplyLeaderSnapshot.class);

        // Wait for the follower to persist the snapshot.
        awaitSnapshot(follower2Actor);

        final List<MockCommand> expFollowerState = List.of(payload0, payload1, payload2);

        var follower2log = follower2Context.getReplicatedLog();
        assertEquals("Follower commit index", 2, follower2log.getCommitIndex());
        assertEquals("Follower last applied", 2, follower2log.getLastApplied());
        assertEquals("Follower snapshot index", 2, follower2log.getSnapshotIndex());
        assertEquals("Follower state", expFollowerState, follower2Underlying.getState());

        killActor(follower2Actor);

        follower2Actor = newTestRaftActor(follower2Id, Map.of(leaderId, testActorPath(leaderId)),
                newFollowerConfigParams());

        follower2Underlying = follower2Actor.underlyingActor();
        follower2Underlying.waitForRecoveryComplete();
        follower2Context = follower2Underlying.getRaftActorContext();

        follower2log = follower2Context.getReplicatedLog();
        assertEquals("Follower commit index", 2, follower2log.getCommitIndex());
        assertEquals("Follower last applied", 2, follower2log.getLastApplied());
        assertEquals("Follower snapshot index", 2, follower2log.getSnapshotIndex());
        assertEquals("Follower state", expFollowerState, follower2Underlying.getState());
    }

    @Test
    public void testRecoveryDeleteEntries() {
        send2InitialPayloads();

        sendPayloadData(leaderActor, "two");

        // This should trigger a snapshot.
        sendPayloadData(leaderActor, "three");

        awaitSnapshot(leaderActor);
        verifyApplyIndex(leaderActor, 3);

        // Disconnect follower from leader
        killActor(follower1Actor);

        // Send another payloads
        sendPayloadData(leaderActor, "four");
        sendPayloadData(leaderActor, "five");

        verifyRaftState(leaderActor, raftState -> {
            assertEquals("leader journal last index", 5, leaderContext.getReplicatedLog().lastIndex());
        });

        // Remove entries started from 4 index
        leaderActor.underlyingActor().getReplicatedLog().trimToReceive(4);

        verifyRaftState(leaderActor, raftState -> {
            assertEquals("leader journal last index", 3, leaderContext.getReplicatedLog().lastIndex());
        });

        // Send new payloads
        final MockCommand payload4 = sendPayloadData(leaderActor, "newFour");
        await().untilAsserted(() -> assertEquals(
                "leader journal last index", 4, leaderContext.getReplicatedLog().lastIndex()));

        final MockCommand payload5 = sendPayloadData(leaderActor, "newFive");
        await().untilAsserted(() -> assertEquals(
                "leader journal last index", 5, leaderContext.getReplicatedLog().lastIndex()));

        verifyRaftState(leaderActor, raftState -> {
            assertEquals("leader journal last index", 5, leaderContext.getReplicatedLog().lastIndex());
        });

        reinstateLeaderActor();

        final var log = leaderActor.underlyingActor().getReplicatedLog();
        assertEquals("Leader last index", 5, log.lastIndex());
        assertEquals(List.of(payload4, payload5), List.of(log.get(4).command(), log.get(5).command()));
    }

    private void reinstateLeaderActor() {
        killActor(leaderActor);

        leaderActor = newTestRaftActor(leaderId, peerAddresses, leaderConfigParams);

        leaderActor.underlyingActor().waitForRecoveryComplete();

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();
    }

    private void send2InitialPayloads() {
        waitUntilLeader(leaderActor);
        currentTerm = leaderContext.currentTerm();

        payload0 = sendPayloadData(leaderActor, "zero");

        verifyApplyIndex(leaderActor, 0);

        payload1 = sendPayloadData(leaderActor, "one");

        // Verify the leader applies the states.
        verifyApplyIndex(leaderActor, 1);

        assertEquals("Leader last applied", 1, leaderContext.getReplicatedLog().getLastApplied());

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
    }
}
