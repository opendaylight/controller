/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import akka.persistence.SaveSnapshotSuccess;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.RaftActor.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

/**
 * Tests replication and snapshots end-to-end using real RaftActors and behavior communication with a
 * lagging follower.
 *
 * @author Thomas Pantelis
 */
public class ReplicationAndSnapshotsWithLaggingFollowerIntegrationTest extends AbstractRaftActorIntegrationTest {

    private MockPayload payload9;
    private MockPayload payload11;
    private MockPayload payload12;
    private MockPayload payload13;

    @Test
    public void runTest() throws Exception {
        testLog.info("testReplicationAndSnapshotsWithLaggingFollower starting");

        leaderId = factory.generateActorId("leader");
        follower1Id = factory.generateActorId("follower");
        follower2Id = factory.generateActorId("follower");

        // Setup the persistent journal for the leader - just an election term and no journal/snapshots.
        InMemoryJournal.addEntry(leaderId, 1, new UpdateElectionTerm(initialTerm, leaderId));

        // Create the leader and 2 follower actors.

        follower1Actor = newTestRaftActor(follower1Id, null, newFollowerConfigParams());

        follower2Actor = newTestRaftActor(follower2Id, null, newFollowerConfigParams());

        Map<String, String> peerAddresses = ImmutableMap.<String, String>builder().
                put(follower1Id, follower1Actor.path().toString()).
                put(follower2Id, follower2Actor.path().toString()).build();

        leaderConfigParams = newLeaderConfigParams();
        leaderActor = newTestRaftActor(leaderId, peerAddresses, leaderConfigParams);

        waitUntilLeader(leaderActor);

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();
        leader = leaderActor.underlyingActor().getCurrentBehavior();

        follower1Context = follower1Actor.underlyingActor().getRaftActorContext();
        follower1 = follower1Actor.underlyingActor().getCurrentBehavior();

        follower2Context = follower2Actor.underlyingActor().getRaftActorContext();
        follower2 = follower2Actor.underlyingActor().getCurrentBehavior();

        currentTerm = leaderContext.getTermInformation().getCurrentTerm();
        assertEquals("Current term > " + initialTerm, true, currentTerm > initialTerm);

        leaderCollectorActor = leaderActor.underlyingActor().collectorActor();
        follower1CollectorActor = follower1Actor.underlyingActor().collectorActor();
        follower2CollectorActor = follower2Actor.underlyingActor().collectorActor();

        testLog.info("Leader created and elected");

        testInitialReplications();

        testSubsequentReplicationsAndSnapshots();

        testLeaderSnapshotTriggeredByMemoryThresholdExceeded();

        testInstallSnapshotToLaggingFollower();

        verifyNoSubsequentSnapshotAfterMemoryThresholdExceededSnapshot();

        testFinalReplicationsAndSnapshot();

        testLeaderReinstatement();

        testLog.info("testReplicationAndSnapshotsWithLaggingFollower ending");
    }

    /**
     * Send 3 payload instances with follower 2 temporarily lagging.
     *
     * @throws Exception
     */
    private void testInitialReplications() throws Exception {

        testLog.info("testInitialReplications starting: sending 2 new payloads");

        // Simulate lagging by dropping AppendEntries messages in follower 2.
        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

        // Send the payloads.
        MockPayload payload0 = sendPayloadData(leaderActor, "zero");
        MockPayload payload1 = sendPayloadData(leaderActor, "one");
        MockPayload payload2 = sendPayloadData(leaderActor, "two");

        // Verify the leader got consensus and applies each log entry even though follower 2 didn't respond.
        List<ApplyState> applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), leaderCollectorActor, payload0.toString(), currentTerm, 0, payload0);
        verifyApplyState(applyStates.get(1), leaderCollectorActor, payload1.toString(), currentTerm, 1, payload1);
        verifyApplyState(applyStates.get(2), leaderCollectorActor, payload2.toString(), currentTerm, 2, payload2);

        // Verify follower 1 applies each log entry.
        applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 0, payload0);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 1, payload1);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 2, payload2);

        // Ensure there's at least 1 more heartbeat.
        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

        // The leader should not have performed fake snapshots to trim the log because the entries have not
        // been replicated to follower 2.
        assertEquals("Leader snapshot term", -1, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", -1, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader journal log size", 3, leaderContext.getReplicatedLog().size());
        assertEquals("Leader journal last index", 2, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 2, leaderContext.getCommitIndex());
        assertEquals("Leader last applied", 2, leaderContext.getLastApplied());
        assertEquals("Leader replicatedToAllIndex", -1, leader.getReplicatedToAllIndex());

        testLog.info("Step 3: new entries applied - re-enabling follower {}", follower2Id);

        // Now stop dropping AppendEntries in follower 2.
        follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        // Verify follower 2 applies each log entry.
        applyStates = MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 0, payload0);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 1, payload1);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 2, payload2);

        // Ensure there's at least 1 more heartbeat.
        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

        // The leader should now have performed fake snapshots to trim the log.
        assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", 1, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader journal log size", 1, leaderContext.getReplicatedLog().size());
        assertEquals("Leader journal last index", 2, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 2, leaderContext.getCommitIndex());
        assertEquals("Leader last applied", 2, leaderContext.getLastApplied());
        // Note - replicatedToAllIndex always lags 1 behind last applied since it trims the log up to the
        // last applied index. The next entry successfully replicated to followers woild advance it.
        assertEquals("Leader replicatedToAllIndex", 1, leader.getReplicatedToAllIndex());

        // Even though follower 2 lagged behind, the leader should not have tried to install a snapshot
        // to catch it up because no snapshotting was done so the follower's next index was present in the log.
        InstallSnapshot installSnapshot = MessageCollectorActor.getFirstMatching(follower2CollectorActor,
                InstallSnapshot.class);
        Assert.assertNull("Follower 2 received unexpected InstallSnapshot", installSnapshot);

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.clearMessages(follower2CollectorActor);

        testLog.info("testInitialReplications complete");
    }

    /**
     * Send 5 more payloads with follower 2 lagging. Since the snapshotBatch count is 4, this should cause
     * 2 leader snapshots and follower 2's log will be behind by 5 entries.
     *
     * @throws Exception
     */
    private void testSubsequentReplicationsAndSnapshots() throws Exception {
        testLog.info("testSubsequentReplicationsAndSnapshots starting: sending first payload, replicatedToAllIndex: {}",
                leader.getReplicatedToAllIndex());

        leaderActor.underlyingActor().setSnapshot(new byte[] {2});

        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

        // Send the first payload - this should cause the first snapshot.
        MockPayload payload3 = sendPayloadData(leaderActor, "three");

        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        byte[] snapshot = new byte[] {6};
        leaderActor.underlyingActor().setSnapshot(snapshot);

        testLog.info("testSubsequentReplicationsAndSnapshots: sending 4 more payloads");

        // Send the next 4. The last one should cause the second snapshot.
        MockPayload payload4 = sendPayloadData(leaderActor, "four");
        MockPayload payload5 = sendPayloadData(leaderActor, "five");
        MockPayload payload6 = sendPayloadData(leaderActor, "six");
        MockPayload payload7 = sendPayloadData(leaderActor, "seven");

        // Verify the leader got consensus and applies each log entry even though follower 2 didn't respond.
        List<ApplyState> applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 5);
        verifyApplyState(applyStates.get(0), leaderCollectorActor, payload3.toString(), currentTerm, 3, payload3);
        verifyApplyState(applyStates.get(1), leaderCollectorActor, payload4.toString(), currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(2), leaderCollectorActor, payload5.toString(), currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(3), leaderCollectorActor, payload6.toString(), currentTerm, 6, payload6);
        verifyApplyState(applyStates.get(4), leaderCollectorActor, payload7.toString(), currentTerm, 7, payload7);

        // Verify follower 1 applies each log entry.
        applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 5);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 3, payload3);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(3), null, null, currentTerm, 6, payload6);
        verifyApplyState(applyStates.get(4), null, null, currentTerm, 7, payload7);

        // Wait for snapshot completion.
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        // The first new entry applied should have caused the leader to advanced the snapshot index to the
        // last previously applied index (2) that was replicated to all followers.
        assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", 2, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader journal log size", 5, leaderContext.getReplicatedLog().size());
        assertEquals("Leader journal last index", 7, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 7, leaderContext.getCommitIndex());
        assertEquals("Leader last applied", 7, leaderContext.getLastApplied());
        assertEquals("Leader replicatedToAllIndex", 2, leader.getReplicatedToAllIndex());

        // Now stop dropping AppendEntries in follower 2.
        follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        // Verify follower 2 applies each log entry.
        applyStates = MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, 5);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 3, payload3);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(3), null, null, currentTerm, 6, payload6);
        verifyApplyState(applyStates.get(4), null, null, currentTerm, 7, payload7);

        // Ensure there's at least 1 more heartbeat.
        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

        // The leader should now have performed fake snapshots to advance the snapshot index and to trim
        // the log. In addition replicatedToAllIndex should've advanced.
        assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", 6, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader journal log size", 1, leaderContext.getReplicatedLog().size());
        assertEquals("Leader journal last index", 7, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader replicatedToAllIndex", 6, leader.getReplicatedToAllIndex());

        // Verify the leader's persisted snapshot.
        List<Snapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
        assertEquals("Persisted snapshots size", 1, persistedSnapshots.size());
        verifySnapshot("Persisted", persistedSnapshots.get(0), currentTerm, 3, currentTerm, 7, snapshot);
        List<ReplicatedLogEntry> unAppliedEntry = persistedSnapshots.get(0).getUnAppliedEntries();
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 4, unAppliedEntry.size());
        verifyReplicatedLogEntry(unAppliedEntry.get(0), currentTerm, 4, payload4);
        verifyReplicatedLogEntry(unAppliedEntry.get(1), currentTerm, 5, payload5);
        verifyReplicatedLogEntry(unAppliedEntry.get(2), currentTerm, 6, payload6);
        verifyReplicatedLogEntry(unAppliedEntry.get(3), currentTerm, 7, payload7);

        // Even though follower 2's log was behind by 5 entries and 2 snapshots were done, the leader
        // should not have tried to install a snapshot to catch it up because replicatedToAllIndex was also
        // behind. Instead of installing a snapshot the leader would've sent AppendEntries with the log entries.
        InstallSnapshot installSnapshot = MessageCollectorActor.getFirstMatching(follower2CollectorActor, InstallSnapshot.class);
        Assert.assertNull("Follower 2 received unexpected InstallSnapshot", installSnapshot);

        // Verify follower 1's log and snapshot indexes.
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, AppendEntries.class);
        assertEquals("Follower 1 snapshot term", currentTerm, follower1Context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Follower 1 snapshot index", 6, follower1Context.getReplicatedLog().getSnapshotIndex());
        assertEquals("Follower 1 journal log size", 1, follower1Context.getReplicatedLog().size());
        assertEquals("Follower 1 journal last index", 7, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower 1 commit index", 7, follower1Context.getCommitIndex());
        assertEquals("Follower 1 last applied", 7, follower1Context.getLastApplied());
        assertEquals("Follower 1 replicatedToAllIndex", 6, follower1.getReplicatedToAllIndex());

        // Verify follower 2's log and snapshot indexes.
        MessageCollectorActor.clearMessages(follower2CollectorActor);
        MessageCollectorActor.expectFirstMatching(follower2CollectorActor, AppendEntries.class);
        assertEquals("Follower 2 snapshot term", currentTerm, follower2Context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Follower 2 snapshot index", 6, follower2Context.getReplicatedLog().getSnapshotIndex());
        assertEquals("Follower 2 journal log size", 1, follower2Context.getReplicatedLog().size());
        assertEquals("Follower 2 journal last index", 7, follower2Context.getReplicatedLog().lastIndex());
        assertEquals("Follower 2 commit index", 7, follower2Context.getCommitIndex());
        assertEquals("Follower 2 last applied", 7, follower2Context.getLastApplied());
        assertEquals("Follower 2 replicatedToAllIndex", 6, follower2.getReplicatedToAllIndex());

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.clearMessages(follower2CollectorActor);

        testLog.info("testSubsequentReplicationsAndSnapshots complete");
    }

    /**
     * Send a couple more payloads with follower 2 lagging. The last payload will have a large enough size
     * to trigger a leader snapshot.
     *
     * @throws Exception
     */
    private void testLeaderSnapshotTriggeredByMemoryThresholdExceeded() throws Exception {
        testLog.info("testLeaderSnapshotTriggeredByMemoryThresholdExceeded starting: sending 3 payloads, replicatedToAllIndex: {}",
                leader.getReplicatedToAllIndex());

        leaderActor.underlyingActor().setMockTotalMemory(1000);
        byte[] snapshot = new byte[] {6};
        leaderActor.underlyingActor().setSnapshot(snapshot);

        // We'll expect a ReplicatedLogImplEntry message and an ApplyJournalEntries message added to the journal.
        InMemoryJournal.addWriteMessagesCompleteLatch(leaderId, 2);

        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

        // Send a payload with a large relative size but not enough to trigger a snapshot.
        MockPayload payload8 = sendPayloadData(leaderActor, "eight", 500);

        // Verify the leader got consensus and applies the first log entry even though follower 2 didn't respond.
        List<ApplyState> applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 1);
        verifyApplyState(applyStates.get(0), leaderCollectorActor, payload8.toString(), currentTerm, 8, payload8);

        // Wait for all the ReplicatedLogImplEntry and ApplyJournalEntries messages to be added to the journal
        // before the snapshot so the snapshot sequence # will be higher to ensure the snapshot gets
        // purged from the snapshot store after subsequent snapshots.
        InMemoryJournal.waitForWriteMessagesComplete(leaderId);

        // Verify a snapshot is not triggered.
        CaptureSnapshot captureSnapshot = MessageCollectorActor.getFirstMatching(leaderCollectorActor, CaptureSnapshot.class);
        Assert.assertNull("Leader received unexpected CaptureSnapshot", captureSnapshot);

        // Send another payload with a large enough relative size in combination with the last payload
        // that exceeds the memory threshold (70% * 1000 = 700) - this should do a snapshot.
        payload9 = sendPayloadData(leaderActor, "nine", 201);

        // Verify the leader applies the last log entry.
        applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 2);
        verifyApplyState(applyStates.get(1), leaderCollectorActor, payload9.toString(), currentTerm, 9, payload9);

        // Verify follower 1 applies each log entry.
        applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 2);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 8, payload8);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 9, payload9);

        // A snapshot should've occurred - wait for it to complete.
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        // Because the snapshot was triggered by exceeding the memory threshold the leader should've advanced
        // the snapshot index to the last applied index and trimmed the log even though the entries weren't
        // replicated to all followers.
        assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", 8, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader journal log size", 1, leaderContext.getReplicatedLog().size());
        assertEquals("Leader journal last index", 9, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 9, leaderContext.getCommitIndex());
        assertEquals("Leader last applied", 9, leaderContext.getLastApplied());
        // Note: replicatedToAllIndex should not be advanced since log entries 8 and 9 haven't yet been
        // replicated to follower 2.
        assertEquals("Leader replicatedToAllIndex", 7, leader.getReplicatedToAllIndex());

        // Verify the leader's persisted snapshot.
        List<Snapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
        assertEquals("Persisted snapshots size", 1, persistedSnapshots.size());
        verifySnapshot("Persisted", persistedSnapshots.get(0), currentTerm, 8, currentTerm, 9, snapshot);
        List<ReplicatedLogEntry> unAppliedEntry = persistedSnapshots.get(0).getUnAppliedEntries();
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 1, unAppliedEntry.size());
        verifyReplicatedLogEntry(unAppliedEntry.get(0), currentTerm, 9, payload9);

        testLog.info("testLeaderSnapshotTriggeredByMemoryThresholdExceeded ending");
    }

    /**
     * Send another payload to verify another snapshot is not done since the last snapshot trimmed the
     * first log entry so the memory threshold should not be exceeded.
     *
     * @throws Exception
     */
    private void verifyNoSubsequentSnapshotAfterMemoryThresholdExceededSnapshot() throws Exception {
        ApplyState applyState;
        CaptureSnapshot captureSnapshot;

        MockPayload payload10 = sendPayloadData(leaderActor, "ten");

        // Verify the leader applies the state.
        applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
        verifyApplyState(applyState, leaderCollectorActor, payload10.toString(), currentTerm, 10, payload10);

        captureSnapshot = MessageCollectorActor.getFirstMatching(leaderCollectorActor, CaptureSnapshot.class);
        Assert.assertNull("Leader received unexpected CaptureSnapshot", captureSnapshot);

        // Verify the follower 1 applies the state.
        applyState = MessageCollectorActor.expectFirstMatching(follower1CollectorActor, ApplyState.class);
        verifyApplyState(applyState, null, null, currentTerm, 10, payload10);

        // Verify the follower 2 applies the state.
        applyState = MessageCollectorActor.expectFirstMatching(follower2CollectorActor, ApplyState.class);
        verifyApplyState(applyState, null, null, currentTerm, 10, payload10);

        // Verify the leader's state.
        assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", 9, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader journal log size", 1, leaderContext.getReplicatedLog().size());
        assertEquals("Leader journal last index", 10, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 10, leaderContext.getCommitIndex());
        assertEquals("Leader last applied", 10, leaderContext.getLastApplied());
        assertEquals("Leader replicatedToAllIndex", 9, leader.getReplicatedToAllIndex());

        // Verify follower 1's state.
        assertEquals("Follower 1 snapshot term", currentTerm, follower1Context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Follower 1 snapshot index", 9, follower1Context.getReplicatedLog().getSnapshotIndex());
        assertEquals("Follower 1 journal log size", 1, follower1Context.getReplicatedLog().size());
        assertEquals("Follower 1 journal last index", 10, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower 1 commit index", 10, follower1Context.getCommitIndex());
        assertEquals("Follower 1 last applied", 10, follower1Context.getLastApplied());
        assertEquals("Follower 1 replicatedToAllIndex", 9, follower1.getReplicatedToAllIndex());

        // Verify follower 2's state.
        assertEquals("Follower 2 snapshot term", currentTerm, follower2Context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Follower 2 snapshot index", 9, follower2Context.getReplicatedLog().getSnapshotIndex());
        assertEquals("Follower 2 journal log size", 1, follower2Context.getReplicatedLog().size());
        assertEquals("Follower 2 journal last index", 10, follower2Context.getReplicatedLog().lastIndex());
        assertEquals("Follower 2 commit index", 10, follower2Context.getCommitIndex());
        assertEquals("Follower 2 last applied", 10, follower2Context.getLastApplied());
        assertEquals("Follower 2 replicatedToAllIndex", 9, follower2.getReplicatedToAllIndex());

        // Revert back to JVM total memory.
        leaderActor.underlyingActor().setMockTotalMemory(0);

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.clearMessages(follower2CollectorActor);
    }

    /**
     * Following a snapshot due memory threshold exceeded, resume the lagging follower and verify it receives
     * an install snapshot from the leader.
     *
     * @throws Exception
     */
    private void testInstallSnapshotToLaggingFollower() throws Exception {
        List<Snapshot> persistedSnapshots;
        List<ReplicatedLogEntry> unAppliedEntry;
        ApplyState applyState;
        ApplySnapshot applySnapshot;
        InstallSnapshot installSnapshot;
        InstallSnapshotReply installSnapshotReply;

        byte[] snapshot = new byte[] {10};
        leaderActor.underlyingActor().setSnapshot(snapshot);

        // Now stop dropping AppendEntries in follower 2.
        follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        installSnapshot = MessageCollectorActor.expectFirstMatching(follower2CollectorActor, InstallSnapshot.class);
        assertEquals("InstallSnapshot getTerm", currentTerm, installSnapshot.getTerm());
        assertEquals("InstallSnapshot getLeaderId", leaderId, installSnapshot.getLeaderId());
        assertEquals("InstallSnapshot getChunkIndex", 1, installSnapshot.getChunkIndex());
        assertEquals("InstallSnapshot getTotalChunks", 1, installSnapshot.getTotalChunks());
        assertEquals("InstallSnapshot getLastIncludedTerm", currentTerm, installSnapshot.getLastIncludedTerm());
        assertEquals("InstallSnapshot getLastIncludedIndex", 8, installSnapshot.getLastIncludedIndex());
        assertArrayEquals("InstallSnapshot getData", snapshot, installSnapshot.getData().toByteArray());

        installSnapshotReply = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, InstallSnapshotReply.class);
        assertEquals("InstallSnapshotReply getTerm", currentTerm, installSnapshotReply.getTerm());
        assertEquals("InstallSnapshotReply getChunkIndex", 1, installSnapshotReply.getChunkIndex());
        assertEquals("InstallSnapshotReply getFollowerId", follower2Id, installSnapshotReply.getFollowerId());
        assertEquals("InstallSnapshotReply isSuccess", true, installSnapshotReply.isSuccess());

        // Verify follower 2 applies the snapshot.
        applySnapshot = MessageCollectorActor.expectFirstMatching(follower2CollectorActor, ApplySnapshot.class);
        verifySnapshot("Follower 2", applySnapshot.getSnapshot(), currentTerm, 8, currentTerm, 8, snapshot);
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 0, applySnapshot.getSnapshot().getUnAppliedEntries().size());

        // Verify follower 2 only applies the second log entry (9) as the first one (8) was in the snapshot.
        applyState = MessageCollectorActor.expectFirstMatching(follower2CollectorActor, ApplyState.class);
        verifyApplyState(applyState, null, null, currentTerm, 9, payload9);

        // Wait for the snapshot to complete.
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        // Ensure there's at least 1 more heartbeat.
        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

        // The leader should now have performed fake snapshots to advance the snapshot index and to trim
        // the log. In addition replicatedToAllIndex should've advanced.
        assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", 8, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader journal log size", 1, leaderContext.getReplicatedLog().size());
        assertEquals("Leader commit index", 9, leaderContext.getCommitIndex());
        assertEquals("Leader last applied", 9, leaderContext.getLastApplied());
        assertEquals("Leader replicatedToAllIndex", 8, leader.getReplicatedToAllIndex());

        // Verify the leader's persisted snapshot. The previous snapshot (currently) won't be deleted from
        // the snapshot store because the second snapshot was initiated by the follower install snapshot and
        // not because the batch count was reached so the persisted journal sequence number wasn't advanced
        // far enough to cause the previous snapshot to be deleted. This is because
        // RaftActor#trimPersistentData subtracts the snapshotBatchCount from the snapshot's sequence number.
        // This is OK - the next snapshot should delete it. In production, even if the system restarted
        // before another snapshot, they would both get applied which wouldn't hurt anything.
        persistedSnapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
        Assert.assertTrue("Expected at least 1 persisted snapshots", persistedSnapshots.size() > 0);
        Snapshot persistedSnapshot = persistedSnapshots.get(persistedSnapshots.size() - 1);
        verifySnapshot("Persisted", persistedSnapshot, currentTerm, 9, currentTerm, 9, snapshot);
        unAppliedEntry = persistedSnapshot.getUnAppliedEntries();
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 0, unAppliedEntry.size());

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.clearMessages(follower2CollectorActor);
    }

    /**
     * Do another round of payloads and snapshot to verify replicatedToAllIndex gets back on track and
     * snapshots works as expected after doing a follower snapshot. In this step we don't lag a follower.
     */
    private void testFinalReplicationsAndSnapshot() {
        List<ApplyState> applyStates;
        ApplyState applyState;

        testLog.info("testFinalReplicationsAndSnapshot starting: replicatedToAllIndex: {}", leader.getReplicatedToAllIndex());

        byte[] snapshot = new byte[] {14};
        leaderActor.underlyingActor().setSnapshot(snapshot);

        // Send another payload - a snapshot should occur.
        payload11 = sendPayloadData(leaderActor, "eleven");

        // Wait for the snapshot to complete.
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
        verifyApplyState(applyState, leaderCollectorActor, payload11.toString(), currentTerm, 11, payload11);

        // Verify the leader's last persisted snapshot (previous ones may not be purged yet).
        List<Snapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
        Snapshot persistedSnapshot = persistedSnapshots.get(persistedSnapshots.size() - 1);
        verifySnapshot("Persisted", persistedSnapshot, currentTerm, 10, currentTerm, 11, snapshot);
        List<ReplicatedLogEntry> unAppliedEntry = persistedSnapshot.getUnAppliedEntries();
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 1, unAppliedEntry.size());
        verifyReplicatedLogEntry(unAppliedEntry.get(0), currentTerm, 11, payload11);

        // Send a couple more payloads.
        payload12 = sendPayloadData(leaderActor, "twelve");
        payload13 = sendPayloadData(leaderActor, "thirteen");

        // Verify the leader applies the 2 log entries.
        applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(1), leaderCollectorActor, payload12.toString(), currentTerm, 12, payload12);
        verifyApplyState(applyStates.get(2), leaderCollectorActor, payload13.toString(), currentTerm, 13, payload13);

        // Verify the leader applies a log entry for at least the last entry index.
        verifyApplyJournalEntries(leaderCollectorActor, 13);

        // Ensure there's at least 1 more heartbeat to trim the log.
        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

        // Verify the leader's final snapshot index et al.
        assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", 12, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader journal log size", 1, leaderContext.getReplicatedLog().size());
        assertEquals("Leader journal last index", 13, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 13, leaderContext.getCommitIndex());
        assertEquals("Leader last applied", 13, leaderContext.getLastApplied());
        assertEquals("Leader replicatedToAllIndex", 12, leader.getReplicatedToAllIndex());

        InMemoryJournal.dumpJournal(leaderId);

        // Verify the leaders's persisted journal log - should only contain the last 2 ReplicatedLogEntries
        // added after the snapshot as the persisted journal should've been purged to the snapshot
        // sequence number.
        verifyPersistedJournal(leaderId, Arrays.asList(new ReplicatedLogImplEntry(12, currentTerm, payload12),
                new ReplicatedLogImplEntry(13, currentTerm, payload13)));

        // Verify the leaders's persisted journal contains an ApplyJournalEntries for at least the last entry index.
        List<ApplyJournalEntries> persistedApplyJournalEntries = InMemoryJournal.get(leaderId, ApplyJournalEntries.class);
        boolean found = false;
        for(ApplyJournalEntries entry: persistedApplyJournalEntries) {
            if(entry.getToIndex() == 13) {
                found = true;
                break;
            }
        }

        Assert.assertTrue(String.format("ApplyJournalEntries with index %d not found in leader's persisted journal", 13), found);

        // Verify follower 1 applies the 2 log entries.
        applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 11, payload11);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 12, payload12);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 13, payload13);

        // Verify follower 1's log state.
        assertEquals("Follower 1 snapshot term", currentTerm, follower1Context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Follower 1 snapshot index", 12, follower1Context.getReplicatedLog().getSnapshotIndex());
        assertEquals("Follower 1 journal log size", 1, follower1Context.getReplicatedLog().size());
        assertEquals("Follower 1 journal last index", 13, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower 1 commit index", 13, follower1Context.getCommitIndex());
        assertEquals("Follower 1 last applied", 13, follower1Context.getLastApplied());
        assertEquals("Follower 1 replicatedToAllIndex", 12, follower1.getReplicatedToAllIndex());

        // Verify follower 2 applies the 2 log entries.
        applyStates = MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 11, payload11);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 12, payload12);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 13, payload13);

        // Verify follower 2's log state.
        assertEquals("Follower 2 snapshot term", currentTerm, follower2Context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Follower 2 snapshot index", 12, follower2Context.getReplicatedLog().getSnapshotIndex());
        assertEquals("Follower 2 journal log size", 1, follower2Context.getReplicatedLog().size());
        assertEquals("Follower 2 journal last index", 13, follower2Context.getReplicatedLog().lastIndex());
        assertEquals("Follower 2 commit index", 13, follower2Context.getCommitIndex());
        assertEquals("Follower 2 last applied", 13, follower2Context.getLastApplied());
        assertEquals("Follower 2 replicatedToAllIndex", 12, follower2.getReplicatedToAllIndex());

        testLog.info("testFinalReplicationsAndSnapshot ending");
    }

    /**
     * Kill the leader actor, reinstate it and verify the recovered journal.
     */
    private void testLeaderReinstatement() {
        testLog.info("testLeaderReinstatement starting");

        killActor(leaderActor);

        leaderActor = newTestRaftActor(leaderId, peerAddresses, leaderConfigParams);

        leaderActor.underlyingActor().startDropMessages(RequestVoteReply.class);

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();

        leaderActor.underlyingActor().waitForRecoveryComplete();

        assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", 10, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader journal log size", 3, leaderContext.getReplicatedLog().size());
        assertEquals("Leader journal last index", 13, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 13, leaderContext.getCommitIndex());
        assertEquals("Leader last applied", 13, leaderContext.getLastApplied());
        verifyReplicatedLogEntry(leaderContext.getReplicatedLog().get(11), currentTerm, 11, payload11);
        verifyReplicatedLogEntry(leaderContext.getReplicatedLog().get(12), currentTerm, 12, payload12);
        verifyReplicatedLogEntry(leaderContext.getReplicatedLog().get(13), currentTerm, 13, payload13);

        testLog.info("testLeaderReinstatement ending");
    }
}
