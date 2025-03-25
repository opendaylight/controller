/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.persistence.SaveSnapshotSuccess;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.SnapshotManager.ApplyLeaderSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.raft.api.TermInfo;

/**
 * Tests replication and snapshots end-to-end using real RaftActors and behavior communication with a
 * lagging follower.
 *
 * @author Thomas Pantelis
 */
public class ReplicationAndSnapshotsWithLaggingFollowerIntegrationTest extends AbstractRaftActorIntegrationTest {

    private void setup() {
        leaderId = factory.generateActorId("leader");
        follower1Id = factory.generateActorId("follower");
        follower2Id = factory.generateActorId("follower");

        // Setup the persistent journal for the leader - just an election term and no journal/snapshots.
        InMemoryJournal.addEntry(leaderId, 1, new UpdateElectionTerm(initialTerm, leaderId));

        // Create the leader and 2 follower actors.
        follower1Actor = newTestRaftActor(follower1Id, Map.of(leaderId, testActorPath(leaderId),
                follower2Id, testActorPath(follower2Id)), newFollowerConfigParams());

        follower2Actor = newTestRaftActor(follower2Id, Map.of(leaderId, testActorPath(leaderId),
                follower1Id, testActorPath(follower1Id)), newFollowerConfigParams());

        Map<String, String> leaderPeerAddresses = Map.of(
                follower1Id, follower1Actor.path().toString(),
                follower2Id, follower2Actor.path().toString());

        leaderConfigParams = newLeaderConfigParams();
        leaderActor = newTestRaftActor(leaderId, leaderPeerAddresses, leaderConfigParams);

        waitUntilLeader(leaderActor);

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();
        leader = leaderActor.underlyingActor().getCurrentBehavior();

        follower1Context = follower1Actor.underlyingActor().getRaftActorContext();
        follower1 = follower1Actor.underlyingActor().getCurrentBehavior();

        follower2Context = follower2Actor.underlyingActor().getRaftActorContext();
        follower2 = follower2Actor.underlyingActor().getCurrentBehavior();

        currentTerm = leaderContext.currentTerm();
        assertTrue("Current term > " + initialTerm, currentTerm > initialTerm);

        leaderCollectorActor = leaderActor.underlyingActor().collectorActor();
        follower1CollectorActor = follower1Actor.underlyingActor().collectorActor();
        follower2CollectorActor = follower2Actor.underlyingActor().collectorActor();

        testLog.info("Leader created and elected");
    }

    private void setupFollower2() {
        follower2Actor = newTestRaftActor(follower2Id, Map.of(leaderId, testActorPath(leaderId),
                follower1Id, testActorPath(follower1Id)), newFollowerConfigParams());

        follower2Context = follower2Actor.underlyingActor().getRaftActorContext();
        follower2 = follower2Actor.underlyingActor().getCurrentBehavior();

        follower2CollectorActor = follower2Actor.underlyingActor().collectorActor();
    }

    /**
     * Send 2 payload instances with follower 2 lagging then resume the follower and verifies it gets
     * caught up via AppendEntries.
     */
    @Test
    public void testReplicationsWithLaggingFollowerCaughtUpViaAppendEntries() {
        testLog.info("testReplicationsWithLaggingFollowerCaughtUpViaAppendEntries starting: sending 2 new payloads");

        setup();

        // Simulate lagging by dropping AppendEntries messages in follower 2.
        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

        // Send the payloads.
        MockPayload payload0 = sendPayloadData(leaderActor, "zero");
        MockPayload payload1 = sendPayloadData(leaderActor, "one");

        // Verify the leader got consensus and applies each log entry even though follower 2 didn't respond.
        List<ApplyState> applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 2);
        verifyApplyState(applyStates.get(0), leaderCollectorActor, payload0.toString(), currentTerm, 0, payload0);
        verifyApplyState(applyStates.get(1), leaderCollectorActor, payload1.toString(), currentTerm, 1, payload1);

        // Verify follower 1 applies each log entry.
        applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 2);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 0, payload0);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 1, payload1);

        // Ensure there's at least 1 more heartbeat.
        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

        // The leader should not have performed fake snapshots to trim the log because the entries have not
        // been replicated to follower 2.
        final var log = leaderContext.getReplicatedLog();
        assertEquals("Leader snapshot term", -1, log.getSnapshotTerm());
        assertEquals("Leader snapshot index", -1, log.getSnapshotIndex());
        assertEquals("Leader journal log size", 2, log.size());
        assertEquals("Leader journal last index", 1, log.lastIndex());
        assertEquals("Leader commit index", 1, log.getCommitIndex());
        assertEquals("Leader last applied", 1, log.getLastApplied());
        assertEquals("Leader replicatedToAllIndex", -1, leader.getReplicatedToAllIndex());

        testLog.info(
            "testReplicationsWithLaggingFollowerCaughtUpViaAppendEntries: new entries applied - resuming follower {}",
            follower2Id);

        // Now stop dropping AppendEntries in follower 2.
        follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        // Verify follower 2 applies each log entry.
        applyStates = MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, 2);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 0, payload0);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 1, payload1);

        // Ensure there's at least 1 more heartbeat.
        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

        // The leader should now have performed fake snapshots to trim the log.
        verifyLeadersTrimmedLog(1);

        // Even though follower 2 lagged behind, the leader should not have tried to install a snapshot
        // to catch it up because no snapshotting was done so the follower's next index was present in the log.
        InstallSnapshot installSnapshot = MessageCollectorActor.getFirstMatching(follower2CollectorActor,
                InstallSnapshot.class);
        assertNull("Follower 2 received unexpected InstallSnapshot", installSnapshot);

        testLog.info("testReplicationsWithLaggingFollowerCaughtUpViaAppendEntries complete");
    }

    /**
     * Send payloads to trigger a leader snapshot due to snapshotBatchCount reached with follower 2
     * lagging but not enough for the leader to trim its log from the last applied index. Follower 2's log
     * will be behind by several entries and, when it is resumed, it should be caught up via AppendEntries
     * sent by the leader.
     */
    @Test
    public void testLeaderSnapshotWithLaggingFollowerCaughtUpViaAppendEntries() {
        testLog.info("testLeaderSnapshotWithLaggingFollowerCaughtUpViaAppendEntries starting");

        setup();

        sendInitialPayloadsReplicatedToAllFollowers("zero", "one");

        // Configure follower 2 to drop messages and lag.
        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

        // Send the first payload and verify it gets applied by the leader and follower 1.
        MockPayload payload2 = sendPayloadData(leaderActor, "two");

        ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
        verifyApplyState(applyState, leaderCollectorActor, payload2.toString(), currentTerm, 2, payload2);

        applyState = MessageCollectorActor.expectFirstMatching(follower1CollectorActor, ApplyState.class);
        verifyApplyState(applyState, null, null, currentTerm, 2, payload2);

        expSnapshotState.add(payload2);

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);

        // Send another payload - this should cause a snapshot due to snapshotBatchCount reached.
        MockPayload payload3 = sendPayloadData(leaderActor, "three");

        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        testLog.info("testLeaderSnapshotWithLaggingFollowerCaughtUpViaAppendEntries: sending 2 more payloads");

        // Send 2 more payloads - not enough to trigger another snapshot.
        MockPayload payload4 = sendPayloadData(leaderActor, "four");
        MockPayload payload5 = sendPayloadData(leaderActor, "five");

        // Verify the leader got consensus and applies each log entry even though follower 2 didn't respond.
        List<ApplyState> applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), leaderCollectorActor, payload3.toString(), currentTerm, 3, payload3);
        verifyApplyState(applyStates.get(1), leaderCollectorActor, payload4.toString(), currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(2), leaderCollectorActor, payload5.toString(), currentTerm, 5, payload5);

        // Verify follower 1 applies each log entry.
        applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 3, payload3);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 5, payload5);

        // The snapshot should have caused the leader to advanced the snapshot index to the
        // last previously applied index (1) that was replicated to all followers at the time of capture.
        // Note: since the log size (3) did not exceed the snapshot batch count (4), the leader should not
        // have trimmed the log to the last index actually applied (5).
        final var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader snapshot term", currentTerm, leaderLog.getSnapshotTerm());
        assertEquals("Leader snapshot index", 1, leaderLog.getSnapshotIndex());
        assertEquals("Leader journal log size", 4, leaderLog.size());
        assertEquals("Leader journal last index", 5, leaderLog.lastIndex());
        assertEquals("Leader commit index", 5, leaderLog.getCommitIndex());
        assertEquals("Leader last applied", 5, leaderLog.getLastApplied());
        assertEquals("Leader replicatedToAllIndex", 1, leader.getReplicatedToAllIndex());

        // Now stop dropping AppendEntries in follower 2.
        follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        // Verify follower 2 applies each log entry. The leader should not install a snapshot b/c
        // follower 2's next index (3) is still present in the log.
        applyStates = MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, 4);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 2, payload2);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 3, payload3);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(3), null, null, currentTerm, 5, payload5);

        // Verify the leader did not try to install a snapshot to catch up follower 2.
        InstallSnapshot installSnapshot = MessageCollectorActor.getFirstMatching(follower2CollectorActor,
                InstallSnapshot.class);
        assertNull("Follower 2 received unexpected InstallSnapshot", installSnapshot);

        // Ensure there's at least 1 more heartbeat.
        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

        // The leader should now have performed fake snapshots to advance the snapshot index and to trim
        // the log. In addition replicatedToAllIndex should've advanced.
        verifyLeadersTrimmedLog(5);

        // Verify the leader's persisted snapshot.
        List<Snapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
        assertEquals("Persisted snapshots size", 1, persistedSnapshots.size());
        verifySnapshot("Persisted", persistedSnapshots.get(0), currentTerm, 2, currentTerm, 3);
        List<ReplicatedLogEntry> unAppliedEntry = persistedSnapshots.get(0).getUnAppliedEntries();
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 1, unAppliedEntry.size());
        verifyReplicatedLogEntry(unAppliedEntry.get(0), currentTerm, 3, payload3);

        // Verify follower 1's log and snapshot indexes.
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, AppendEntries.class);
        verifyFollowersTrimmedLog(1, follower1Actor, 5);

        // Verify follower 2's log and snapshot indexes.
        MessageCollectorActor.clearMessages(follower2CollectorActor);
        MessageCollectorActor.expectFirstMatching(follower2CollectorActor, AppendEntries.class);
        verifyFollowersTrimmedLog(2, follower2Actor, 5);

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.clearMessages(follower2CollectorActor);

        expSnapshotState.add(payload3);
        expSnapshotState.add(payload4);
        expSnapshotState.add(payload5);

        testLog.info("testLeaderSnapshotWithLaggingFollowerCaughtUpViaAppendEntries complete");
    }

    /**
     * Send payloads to trigger a leader snapshot due to snapshotBatchCount reached with follower 2
     * lagging where the leader trims its log from the last applied index. Follower 2's log
     * will be behind by several entries and, when it is resumed, it should be caught up via a snapshot
     * installed by the leader.
     */
    @Test
    public void testLeaderSnapshotWithLaggingFollowerCaughtUpViaInstallSnapshot() {
        testLog.info("testLeaderSnapshotWithLaggingFollowerCaughtUpViaInstallSnapshot starting");

        setup();

        sendInitialPayloadsReplicatedToAllFollowers("zero", "one");

        // Configure follower 2 to drop messages and lag.
        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

        // Sleep for at least the election timeout interval so follower 2 is deemed inactive by the leader.
        Uninterruptibles.sleepUninterruptibly(leaderConfigParams.getElectionTimeOutInterval().toMillis() + 5,
                TimeUnit.MILLISECONDS);

        // Send 5 payloads - the second should cause a leader snapshot.
        final MockPayload payload2 = sendPayloadData(leaderActor, "two");
        final MockPayload payload3 = sendPayloadData(leaderActor, "three");
        final MockPayload payload4 = sendPayloadData(leaderActor, "four");
        final MockPayload payload5 = sendPayloadData(leaderActor, "five");
        final MockPayload payload6 = sendPayloadData(leaderActor, "six");

        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        // Verify the leader got consensus and applies each log entry even though follower 2 didn't respond.
        List<ApplyState> applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 5);
        verifyApplyState(applyStates.get(0), leaderCollectorActor, payload2.toString(), currentTerm, 2, payload2);
        verifyApplyState(applyStates.get(2), leaderCollectorActor, payload4.toString(), currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(4), leaderCollectorActor, payload6.toString(), currentTerm, 6, payload6);

        MessageCollectorActor.clearMessages(leaderCollectorActor);

        testLog.info("testLeaderSnapshotWithLaggingFollowerCaughtUpViaInstallSnapshot: "
                + "sending 1 more payload to trigger second snapshot");

        // Send another payload to trigger a second leader snapshot.
        MockPayload payload7 = sendPayloadData(leaderActor, "seven");

        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
        verifyApplyState(applyState, leaderCollectorActor, payload7.toString(), currentTerm, 7, payload7);

        // Verify follower 1 applies each log entry.
        applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 6);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 2, payload2);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(5), null, null, currentTerm, 7, payload7);

        // The snapshot should have caused the leader to advanced the snapshot index to the leader's last
        // applied index (6) since the log size should have exceed the snapshot batch count (4).
        // replicatedToAllIndex should remain at 1 since follower 2 is lagging.
        verifyLeadersTrimmedLog(7, 1);

        expSnapshotState.add(payload2);
        expSnapshotState.add(payload3);
        expSnapshotState.add(payload4);
        expSnapshotState.add(payload5);
        expSnapshotState.add(payload6);

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);

        // Send a server config change to test that the install snapshot includes the server config.

        final var serverConfig = new ClusterConfig(
                new ServerInfo(leaderId, true),
                new ServerInfo(follower1Id, false),
                new ServerInfo(follower2Id, false));
        leaderContext.updatePeerIds(serverConfig);
        ((AbstractLeader)leader).updateMinReplicaCount();
        leaderActor.tell(serverConfig, ActorRef.noSender());

        applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
        verifyApplyState(applyState, leaderCollectorActor, "serverConfig", currentTerm, 8, serverConfig);

        applyState = MessageCollectorActor.expectFirstMatching(follower1CollectorActor, ApplyState.class);
        verifyApplyState(applyState, null, null, currentTerm, 8, serverConfig);

        // Verify the leader's persisted snapshot.
        List<Snapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
        assertEquals("Persisted snapshots size", 1, persistedSnapshots.size());
        verifySnapshot("Persisted", persistedSnapshots.get(0), currentTerm, 6, currentTerm, 7);
        List<ReplicatedLogEntry> unAppliedEntry = persistedSnapshots.get(0).getUnAppliedEntries();
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 1, unAppliedEntry.size());
        verifyReplicatedLogEntry(unAppliedEntry.get(0), currentTerm, 7, payload7);

        expSnapshotState.add(payload7);

        verifyInstallSnapshotToLaggingFollower(8, serverConfig);

        testLog.info("testLeaderSnapshotWithLaggingFollowerCaughtUpViaInstallSnapshot complete");
    }

    /**
     * Tests whether the leader reattempts to send a snapshot when a follower crashes before replying with
     * InstallSnapshotReply after the last chunk has been sent.
     */
    @Test
    public void testLeaderInstallsSnapshotWithRestartedFollowerDuringSnapshotInstallation() throws Exception {
        testLog.info("testLeaderInstallsSnapshotWithRestartedFollowerDuringSnapshotInstallation starting");

        setup();

        sendInitialPayloadsReplicatedToAllFollowers("zero", "one");

        // Configure follower 2 to drop messages and lag.
        follower2Actor.stop();

        // Sleep for at least the election timeout interval so follower 2 is deemed inactive by the leader.
        Uninterruptibles.sleepUninterruptibly(leaderConfigParams.getElectionTimeOutInterval().toMillis() + 5,
                TimeUnit.MILLISECONDS);

        // Send 5 payloads - the second should cause a leader snapshot.
        final MockPayload payload2 = sendPayloadData(leaderActor, "two");
        final MockPayload payload3 = sendPayloadData(leaderActor, "three");
        final MockPayload payload4 = sendPayloadData(leaderActor, "four");
        final MockPayload payload5 = sendPayloadData(leaderActor, "five");
        final MockPayload payload6 = sendPayloadData(leaderActor, "six");

        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        // Verify the leader got consensus and applies each log entry even though follower 2 didn't respond.
        List<ApplyState> applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 5);
        verifyApplyState(applyStates.get(0), leaderCollectorActor, payload2.toString(), currentTerm, 2, payload2);
        verifyApplyState(applyStates.get(2), leaderCollectorActor, payload4.toString(), currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(4), leaderCollectorActor, payload6.toString(), currentTerm, 6, payload6);

        MessageCollectorActor.clearMessages(leaderCollectorActor);

        testLog.info("testLeaderInstallsSnapshotWithRestartedFollowerDuringSnapshotInstallation: "
                + "sending 1 more payload to trigger second snapshot");

        // Send another payload to trigger a second leader snapshot.
        MockPayload payload7 = sendPayloadData(leaderActor, "seven");

        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);


        ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
        verifyApplyState(applyState, leaderCollectorActor, payload7.toString(), currentTerm, 7, payload7);

        // Verify follower 1 applies each log entry.
        applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 6);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 2, payload2);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(5), null, null, currentTerm, 7, payload7);

        leaderActor.underlyingActor()
                .startDropMessages(InstallSnapshotReply.class, reply -> reply.getChunkIndex() == 5);

        setupFollower2();

        MessageCollectorActor.expectMatching(follower2CollectorActor, InstallSnapshot.class, 1);

        follower2Actor.stop();

        // need to get rid of persistence for follower2
        InMemorySnapshotStore.clearSnapshotsFor(follower2Id);

        leaderActor.underlyingActor().stopDropMessages(InstallSnapshotReply.class);

        MessageCollectorActor.clearMessages(follower2CollectorActor);
        setupFollower2();

        MessageCollectorActor.expectMatching(follower2CollectorActor, SaveSnapshotSuccess.class, 1);
    }

    /**
     * Send payloads with follower 2 lagging with the last payload having a large enough size to trigger a
     * leader snapshot such that the leader trims its log from the last applied index.. Follower 2's log will
     * be behind by several entries and, when it is resumed, it should be caught up via a snapshot installed
     * by the leader.
     */
    @Test
    public void testLeaderSnapshotTriggeredByMemoryThresholdExceededWithLaggingFollower() {
        testLog.info("testLeaderSnapshotTriggeredByMemoryThresholdExceededWithLaggingFollower starting");

        snapshotBatchCount = 5;
        setup();

        sendInitialPayloadsReplicatedToAllFollowers("zero");

        leaderActor.underlyingActor().setMockTotalMemory(1000);

        // We'll expect a ReplicatedLogImplEntry message and an ApplyJournalEntries message added to the journal.
        InMemoryJournal.addWriteMessagesCompleteLatch(leaderId, 2);

        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

        // Sleep for at least the election timeout interval so follower 2 is deemed inactive by the leader.
        Uninterruptibles.sleepUninterruptibly(leaderConfigParams.getElectionTimeOutInterval().toMillis() + 5,
                TimeUnit.MILLISECONDS);

        // Send a payload with a large relative size but not enough to trigger a snapshot.
        MockPayload payload1 = sendPayloadData(leaderActor, "one", 500);

        // Verify the leader got consensus and applies the first log entry even though follower 2 didn't respond.
        List<ApplyState> applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 1);
        verifyApplyState(applyStates.get(0), leaderCollectorActor, payload1.toString(), currentTerm, 1, payload1);

        // Wait for all the ReplicatedLogImplEntry and ApplyJournalEntries messages to be added to the journal
        // before the snapshot so the snapshot sequence # will be higher to ensure the snapshot gets
        // purged from the snapshot store after subsequent snapshots.
        InMemoryJournal.waitForWriteMessagesComplete(leaderId);

        // Verify a snapshot is not triggered.
        CaptureSnapshot captureSnapshot = MessageCollectorActor.getFirstMatching(leaderCollectorActor,
                CaptureSnapshot.class);
        assertNull("Leader received unexpected CaptureSnapshot", captureSnapshot);

        expSnapshotState.add(payload1);

        // Sleep for at least the election timeout interval so follower 2 is deemed inactive by the leader.
        Uninterruptibles.sleepUninterruptibly(leaderConfigParams.getElectionTimeOutInterval().toMillis() + 5,
                TimeUnit.MILLISECONDS);

        // Send another payload with a large enough relative size in combination with the last payload
        // that exceeds the memory threshold (70% * 1000 = 700) - this should do a snapshot.
        MockPayload payload2 = sendPayloadData(leaderActor, "two", 201);

        // Verify the leader applies the last log entry.
        applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 2);
        verifyApplyState(applyStates.get(1), leaderCollectorActor, payload2.toString(), currentTerm, 2, payload2);

        // Verify follower 1 applies each log entry.
        applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 2);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 1, payload1);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 2, payload2);

        // A snapshot should've occurred - wait for it to complete.
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        // Because the snapshot was triggered by exceeding the memory threshold the leader should've advanced
        // the snapshot index to the last applied index and trimmed the log even though the entries weren't
        // replicated to all followers.
        verifyLeadersTrimmedLog(2, 0);

        // Verify the leader's persisted snapshot.
        List<Snapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
        assertEquals("Persisted snapshots size", 1, persistedSnapshots.size());
        verifySnapshot("Persisted", persistedSnapshots.get(0), currentTerm, 1, currentTerm, 2);
        List<ReplicatedLogEntry> unAppliedEntry = persistedSnapshots.get(0).getUnAppliedEntries();
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 1, unAppliedEntry.size());
        verifyReplicatedLogEntry(unAppliedEntry.get(0), currentTerm, 2, payload2);

        expSnapshotState.add(payload2);

        verifyInstallSnapshotToLaggingFollower(2L, null);

        // Sends a payload with index 3.
        verifyNoSubsequentSnapshotAfterMemoryThresholdExceededSnapshot();

        // Sends 3 payloads with indexes 4, 5 and 6.
        long leadersSnapshotIndexOnRecovery = verifyReplicationsAndSnapshotWithNoLaggingAfterInstallSnapshot();

        // Recover the leader from persistence and verify.
        long leadersLastIndexOnRecovery = 6;

        long leadersFirstJournalEntryIndexOnRecovery = leadersSnapshotIndexOnRecovery + 1;

        verifyLeaderRecoveryAfterReinstatement(leadersLastIndexOnRecovery, leadersSnapshotIndexOnRecovery,
                leadersFirstJournalEntryIndexOnRecovery);

        testLog.info("testLeaderSnapshotTriggeredByMemoryThresholdExceeded ending");
    }

    /**
     * Send another payload to verify another snapshot is not done since the last snapshot trimmed the
     * first log entry so the memory threshold should not be exceeded.
     */
    private void verifyNoSubsequentSnapshotAfterMemoryThresholdExceededSnapshot() {
        ApplyState applyState;
        CaptureSnapshot captureSnapshot;

        MockPayload payload3 = sendPayloadData(leaderActor, "three");

        // Verify the leader applies the state.
        applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
        verifyApplyState(applyState, leaderCollectorActor, payload3.toString(), currentTerm, 3, payload3);

        captureSnapshot = MessageCollectorActor.getFirstMatching(leaderCollectorActor, CaptureSnapshot.class);
        assertNull("Leader received unexpected CaptureSnapshot", captureSnapshot);

        // Verify the follower 1 applies the state.
        applyState = MessageCollectorActor.expectFirstMatching(follower1CollectorActor, ApplyState.class);
        verifyApplyState(applyState, null, null, currentTerm, 3, payload3);

        // Verify the follower 2 applies the state.
        applyState = MessageCollectorActor.expectFirstMatching(follower2CollectorActor, ApplyState.class);
        verifyApplyState(applyState, null, null, currentTerm, 3, payload3);

        // Verify the leader's state.
        verifyLeadersTrimmedLog(3);

        // Verify follower 1's state.
        verifyFollowersTrimmedLog(1, follower1Actor, 3);

        // Verify follower 2's state.
        verifyFollowersTrimmedLog(2, follower2Actor, 3);

        // Revert back to JVM total memory.
        leaderActor.underlyingActor().setMockTotalMemory(0);

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.clearMessages(follower2CollectorActor);

        expSnapshotState.add(payload3);
    }

    /**
     * Resume the lagging follower 2 and verify it receives an install snapshot from the leader.
     */
    private void verifyInstallSnapshotToLaggingFollower(final long lastAppliedIndex,
            final @Nullable ClusterConfig expServerConfig) {
        testLog.info("verifyInstallSnapshotToLaggingFollower starting");

        MessageCollectorActor.clearMessages(leaderCollectorActor);

        // Now stop dropping AppendEntries in follower 2.
        follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);


        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        // Verify the leader's persisted snapshot. The previous snapshot (currently) won't be deleted from
        // the snapshot store because the second snapshot was initiated by the follower install snapshot and
        // not because the batch count was reached so the persisted journal sequence number wasn't advanced
        // far enough to cause the previous snapshot to be deleted. This is because
        // RaftActor#trimPersistentData subtracts the snapshotBatchCount from the snapshot's sequence number.
        // This is OK - the next snapshot should delete it. In production, even if the system restarted
        // before another snapshot, they would both get applied which wouldn't hurt anything.
        List<Snapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
        assertFalse("Expected at least 1 persisted snapshots", persistedSnapshots.isEmpty());
        Snapshot persistedSnapshot = persistedSnapshots.get(persistedSnapshots.size() - 1);
        verifySnapshot("Persisted", persistedSnapshot, currentTerm, lastAppliedIndex, currentTerm, lastAppliedIndex);
        List<ReplicatedLogEntry> unAppliedEntry = persistedSnapshot.getUnAppliedEntries();
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 0, unAppliedEntry.size());

        int snapshotSize = SerializationUtils.serialize(persistedSnapshot.getState()).length;
        final int expTotalChunks = snapshotSize / MAXIMUM_MESSAGE_SLICE_SIZE
                + (snapshotSize % MAXIMUM_MESSAGE_SLICE_SIZE > 0 ? 1 : 0);

        InstallSnapshot installSnapshot = MessageCollectorActor.expectFirstMatching(follower2CollectorActor,
                InstallSnapshot.class);
        assertEquals("InstallSnapshot getTerm", currentTerm, installSnapshot.getTerm());
        assertEquals("InstallSnapshot getLeaderId", leaderId, installSnapshot.getLeaderId());
        assertEquals("InstallSnapshot getChunkIndex", 1, installSnapshot.getChunkIndex());
        assertEquals("InstallSnapshot getTotalChunks", expTotalChunks, installSnapshot.getTotalChunks());
        assertEquals("InstallSnapshot getLastIncludedTerm", currentTerm, installSnapshot.getLastIncludedTerm());
        assertEquals("InstallSnapshot getLastIncludedIndex", lastAppliedIndex, installSnapshot.getLastIncludedIndex());
        //assertArrayEquals("InstallSnapshot getData", snapshot, installSnapshot.getData().toByteArray());

        List<InstallSnapshotReply> installSnapshotReplies = MessageCollectorActor.expectMatching(
                leaderCollectorActor, InstallSnapshotReply.class, expTotalChunks);
        int index = 1;
        for (InstallSnapshotReply installSnapshotReply : installSnapshotReplies) {
            assertEquals("InstallSnapshotReply getTerm", currentTerm, installSnapshotReply.getTerm());
            assertEquals("InstallSnapshotReply getChunkIndex", index++, installSnapshotReply.getChunkIndex());
            assertEquals("InstallSnapshotReply getFollowerId", follower2Id, installSnapshotReply.getFollowerId());
            assertTrue("InstallSnapshotReply isSuccess", installSnapshotReply.isSuccess());
        }

        // Verify follower 2 applies the snapshot.
        final var applySnapshot = MessageCollectorActor.expectFirstMatching(follower2CollectorActor,
                ApplyLeaderSnapshot.class);
        final MockSnapshotState state;
        try (var ois = new ObjectInputStream(applySnapshot.snapshot().openStream())) {
            state = (MockSnapshotState) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new AssertionError(e);
        }

        final var snapshot = Snapshot.ofTermLeader(state, applySnapshot.lastEntry(), TermInfo.INITIAL,
            applySnapshot.serverConfig());
        verifySnapshot("Follower 2", snapshot, currentTerm, lastAppliedIndex, currentTerm, lastAppliedIndex);
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 0, snapshot.getUnAppliedEntries().size());

        // Wait for the snapshot to complete.
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        // Ensure there's at least 1 more heartbeat.
        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

        // The leader should now have performed fake snapshots to advance the snapshot index and to trim
        // the log. In addition replicatedToAllIndex should've advanced.
        verifyLeadersTrimmedLog(lastAppliedIndex);

        if (expServerConfig != null) {
            Set<ServerInfo> expServerInfo = Set.copyOf(expServerConfig.serverInfo());
            assertEquals("Leader snapshot server config", expServerInfo,
                Set.copyOf(persistedSnapshot.getServerConfiguration().serverInfo()));

            assertEquals("Follower 2 snapshot server config", expServerInfo,
                Set.copyOf(applySnapshot.serverConfig().serverInfo()));

            ClusterConfig follower2ServerConfig = follower2Context.getPeerServerInfo(true);
            assertNotNull("Follower 2 server config is null", follower2ServerConfig);

            assertEquals("Follower 2 server config", expServerInfo, Set.copyOf(follower2ServerConfig.serverInfo()));
        }

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.clearMessages(follower2CollectorActor);

        testLog.info("verifyInstallSnapshotToLaggingFollower complete");
    }

    /**
     * Do another round of payloads and snapshot to verify replicatedToAllIndex gets back on track and
     * snapshots works as expected after doing a follower snapshot. In this step we don't lag a follower.
     */
    private long verifyReplicationsAndSnapshotWithNoLaggingAfterInstallSnapshot() {
        testLog.info(
                "verifyReplicationsAndSnapshotWithNoLaggingAfterInstallSnapshot starting: replicatedToAllIndex: {}",
                leader.getReplicatedToAllIndex());

        // Send another payload - a snapshot should occur.
        MockPayload payload4 = sendPayloadData(leaderActor, "four");

        // Wait for the snapshot to complete.
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
        verifyApplyState(applyState, leaderCollectorActor, payload4.toString(), currentTerm, 4, payload4);

        // Verify the leader's last persisted snapshot (previous ones may not be purged yet).
        List<Snapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
        Snapshot persistedSnapshot = persistedSnapshots.get(persistedSnapshots.size() - 1);
        // The last (fourth) payload may or may not have been applied when the snapshot is captured depending on the
        // timing when the async persistence completes.
        List<ReplicatedLogEntry> unAppliedEntry = persistedSnapshot.getUnAppliedEntries();
        long leadersSnapshotIndex;
        if (unAppliedEntry.isEmpty()) {
            leadersSnapshotIndex = 4;
            expSnapshotState.add(payload4);
            verifySnapshot("Persisted", persistedSnapshot, currentTerm, 4, currentTerm, 4);
        } else {
            leadersSnapshotIndex = 3;
            verifySnapshot("Persisted", persistedSnapshot, currentTerm, 3, currentTerm, 4);
            assertEquals("Persisted Snapshot getUnAppliedEntries size", 1, unAppliedEntry.size());
            verifyReplicatedLogEntry(unAppliedEntry.get(0), currentTerm, 4, payload4);
            expSnapshotState.add(payload4);
        }

        // Send a couple more payloads.
        MockPayload payload5 = sendPayloadData(leaderActor, "five");
        MockPayload payload6 = sendPayloadData(leaderActor, "six");

        // Verify the leader applies the 2 log entries.
        List<ApplyState> applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(1), leaderCollectorActor, payload5.toString(), currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(2), leaderCollectorActor, payload6.toString(), currentTerm, 6, payload6);

        // Verify the leader applies a log entry for at least the last entry index.
        verifyApplyJournalEntries(leaderCollectorActor, 6);

        // Ensure there's at least 1 more heartbeat to trim the log.
        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

        // Verify the leader's final state.
        verifyLeadersTrimmedLog(6);

        InMemoryJournal.dumpJournal(leaderId);

        // Verify the leaders's persisted journal log - it should only contain the last 2 ReplicatedLogEntries
        // added after the snapshot as the persisted journal should've been purged to the snapshot
        // sequence number.
        verifyPersistedJournal(leaderId, List.of(
            new SimpleReplicatedLogEntry(5, currentTerm, payload5),
            new SimpleReplicatedLogEntry(6, currentTerm, payload6)));

        // Verify the leaders's persisted journal contains an ApplyJournalEntries for at least the last entry index.
        List<ApplyJournalEntries> persistedApplyJournalEntries =
                InMemoryJournal.get(leaderId, ApplyJournalEntries.class);
        boolean found = false;
        for (ApplyJournalEntries entry: persistedApplyJournalEntries) {
            if (entry.getToIndex() == 6) {
                found = true;
                break;
            }
        }

        assertTrue("ApplyJournalEntries with index 6 not found in leader's persisted journal", found);

        // Verify follower 1 applies the 3 log entries.
        applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 6, payload6);

        // Verify follower 1's log state.
        verifyFollowersTrimmedLog(1, follower1Actor, 6);

        // Verify follower 2 applies the 3 log entries.
        applyStates = MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 6, payload6);

        // Verify follower 2's log state.
        verifyFollowersTrimmedLog(2, follower2Actor, 6);

        expSnapshotState.add(payload5);
        expSnapshotState.add(payload6);

        testLog.info("verifyReplicationsAndSnapshotWithNoLaggingAfterInstallSnapshot ending");

        return leadersSnapshotIndex;
    }

    /**
     * Kill the leader actor, reinstate it and verify the recovered journal.
     */
    private void verifyLeaderRecoveryAfterReinstatement(final long lastIndex, final long snapshotIndex,
            final long firstJournalEntryIndex) {
        testLog.info("verifyLeaderRecoveryAfterReinstatement starting: lastIndex: {}, snapshotIndex: {}, "
            + "firstJournalEntryIndex: {}", lastIndex, snapshotIndex, firstJournalEntryIndex);

        killActor(leaderActor);

        leaderActor = newTestRaftActor(leaderId, peerAddresses, leaderConfigParams);
        TestRaftActor testRaftActor = leaderActor.underlyingActor();

        testRaftActor.startDropMessages(RequestVoteReply.class);

        leaderContext = testRaftActor.getRaftActorContext();

        testRaftActor.waitForRecoveryComplete();

        int logSize = (int) (expSnapshotState.size() - firstJournalEntryIndex);
        final var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader snapshot term", currentTerm, leaderLog.getSnapshotTerm());
        assertEquals("Leader snapshot index", snapshotIndex, leaderLog.getSnapshotIndex());
        assertEquals("Leader journal log size", logSize, leaderLog.size());
        assertEquals("Leader journal last index", lastIndex, leaderLog.lastIndex());
        assertEquals("Leader commit index", lastIndex, leaderLog.getCommitIndex());
        assertEquals("Leader last applied", lastIndex, leaderLog.getLastApplied());

        for (long i = firstJournalEntryIndex; i < expSnapshotState.size(); i++) {
            verifyReplicatedLogEntry(leaderLog.get(i), currentTerm, i, expSnapshotState.get((int) i));
        }

        assertEquals("Leader applied state", expSnapshotState, testRaftActor.getState());

        testLog.info("verifyLeaderRecoveryAfterReinstatement ending");
    }

    private void sendInitialPayloadsReplicatedToAllFollowers(final String... data) {
        // Send the payloads.
        for (String d: data) {
            expSnapshotState.add(sendPayloadData(leaderActor, d));
        }

        int numEntries = data.length;

        // Verify the leader got consensus and applies each log entry even though follower 2 didn't respond.
        final var leaderStates = MessageCollectorActor.expectMatching(leaderCollectorActor,
            ApplyState.class, numEntries);
        for (int i = 0; i < expSnapshotState.size(); i++) {
            final MockPayload payload = expSnapshotState.get(i);
            verifyApplyState(leaderStates.get(i), leaderCollectorActor, payload.toString(), currentTerm, i, payload);
        }

        // Verify follower 1 applies each log entry.
        final var follower1States = MessageCollectorActor.expectMatching(follower1CollectorActor,
            ApplyState.class, numEntries);
        for (int i = 0; i < expSnapshotState.size(); i++) {
            final MockPayload payload = expSnapshotState.get(i);
            verifyApplyState(follower1States.get(i), null, null, currentTerm, i, payload);
        }

        // Verify follower 2 applies each log entry.
        final var follower2States = MessageCollectorActor.expectMatching(follower2CollectorActor,
            ApplyState.class, numEntries);
        for (int i = 0; i < expSnapshotState.size(); i++) {
            final MockPayload payload = expSnapshotState.get(i);
            verifyApplyState(follower2States.get(i), null, null, currentTerm, i, payload);
        }

        // Ensure there's at least 1 more heartbeat.
        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

        // The leader should have performed fake snapshots to trim the log to the last index replicated to
        // all followers.
        verifyLeadersTrimmedLog(numEntries - 1);

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.clearMessages(follower2CollectorActor);
    }
}
