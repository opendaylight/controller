/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import org.apache.pekko.persistence.SaveSnapshotSuccess;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;

/**
 * Tests replication and snapshots end-to-end using real RaftActors and behavior communication.
 *
 * @author Thomas Pantelis
 */
public class ReplicationAndSnapshotsIntegrationTest extends AbstractRaftActorIntegrationTest {

    private List<SimpleReplicatedLogEntry> origLeaderJournal;

    private MockCommand recoveredPayload0;
    private MockCommand recoveredPayload1;
    private MockCommand recoveredPayload2;
    private MockCommand payload3;
    private MockCommand payload4;
    private MockCommand payload5;
    private MockCommand payload6;
    private MockCommand payload7;

    @Test
    public void runTest() {
        testLog.info("testReplicationAndSnapshots starting");

        // Setup the persistent journal for the leader. We'll start up with 3 journal log entries (one less
        // than the snapshotBatchCount).
        long seqId = 1;
        InMemoryJournal.addEntry(leaderId, seqId++, new UpdateElectionTerm(initialTerm, leaderId));
        recoveredPayload0 = new MockCommand("zero");
        InMemoryJournal.addEntry(leaderId, seqId++, new SimpleReplicatedLogEntry(0, initialTerm, recoveredPayload0));
        recoveredPayload1 = new MockCommand("one");
        InMemoryJournal.addEntry(leaderId, seqId++, new SimpleReplicatedLogEntry(1, initialTerm, recoveredPayload1));
        recoveredPayload2 = new MockCommand("two");
        InMemoryJournal.addEntry(leaderId, seqId++, new SimpleReplicatedLogEntry(2, initialTerm, recoveredPayload2));
        InMemoryJournal.addEntry(leaderId, seqId++, new ApplyJournalEntries(2));

        origLeaderJournal = InMemoryJournal.get(leaderId, SimpleReplicatedLogEntry.class);

        // Create the leader and 2 follower actors and verify initial syncing of the followers after leader
        // persistence recovery.

        DefaultConfigParamsImpl followerConfigParams = newFollowerConfigParams();
        followerConfigParams.setSnapshotBatchCount(snapshotBatchCount);
        follower1Actor = newTestRaftActor(follower1Id, Map.of(leaderId, testActorPath(leaderId),
                follower2Id, testActorPath(follower2Id)), followerConfigParams);

        follower2Actor = newTestRaftActor(follower2Id, Map.of(leaderId, testActorPath(leaderId),
                follower1Id, testActorPath(follower1Id)), followerConfigParams);

        peerAddresses = Map.of(
                follower1Id, follower1Actor.path().toString(),
                follower2Id, follower2Actor.path().toString());

        leaderConfigParams = newLeaderConfigParams();
        leaderActor = newTestRaftActor(leaderId, peerAddresses, leaderConfigParams);

        follower1CollectorActor = follower1Actor.underlyingActor().collectorActor();
        follower2CollectorActor = follower2Actor.underlyingActor().collectorActor();
        leaderCollectorActor = leaderActor.underlyingActor().collectorActor();

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();

        verifyLeaderRecoveryAndInitialization();

        testFirstSnapshot();

        testSubsequentReplications();

        testSecondSnapshot();

        testLeaderReinstatement();

        testLog.info("testReplicationAndSnapshots ending");
    }

    /**
     * Verify the expected leader is elected as the leader and verify initial syncing of the followers
     * from the leader's persistence recovery.
     */
    void verifyLeaderRecoveryAndInitialization() {
        testLog.info("verifyLeaderRecoveryAndInitialization starting");

        waitUntilLeader(leaderActor);

        currentTerm = leaderContext.currentTerm();
        assertEquals("Current term > " + initialTerm, true, currentTerm > initialTerm);

        leader = leaderActor.underlyingActor().getCurrentBehavior();

        // The followers should receive AppendEntries for each leader log entry that was recovered from
        // persistence and apply each one.
        var applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), null, null, initialTerm, 0, recoveredPayload0);
        verifyApplyState(applyStates.get(1), null, null, initialTerm, 1, recoveredPayload1);
        verifyApplyState(applyStates.get(2), null, null, initialTerm, 2, recoveredPayload2);

        // Verify follower 1 applies a log entry for at least the last entry index.
        verifyApplyIndex(follower1Actor, 2);

        applyStates = MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), null, null, initialTerm, 0, recoveredPayload0);
        verifyApplyState(applyStates.get(1), null, null, initialTerm, 1, recoveredPayload1);
        verifyApplyState(applyStates.get(2), null, null, initialTerm, 2, recoveredPayload2);

        // Verify follower 1]2 applies a log entry for at least the last entry index.
        verifyApplyIndex(follower2Actor, 2);

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.clearMessages(follower2CollectorActor);

        // The leader should have performed fake snapshots due to the follower's AppendEntriesReplies and
        // trimmed the in-memory log so that only the last entry remains.
        final var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader snapshot term", initialTerm, leaderLog.getSnapshotTerm());
        assertEquals("Leader snapshot index", 1, leaderLog.getSnapshotIndex());
        assertEquals("Leader journal log size", 1, leaderLog.size());
        assertEquals("Leader journal last index", 2, leaderLog.lastIndex());
        assertEquals("Leader commit index", 2, leaderLog.getCommitIndex());
        assertEquals("Leader last applied", 2, leaderLog.getLastApplied());
        assertEquals("Leader replicatedToAllIndex", 1, leader.getReplicatedToAllIndex());

        // Verify the follower's persisted journal log.
        verifyPersistedJournal(follower1Id, origLeaderJournal);
        verifyPersistedJournal(follower2Id, origLeaderJournal);

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.clearMessages(follower2CollectorActor);

        testLog.info("verifyLeaderRecoveryAndInitialization ending");
    }

    /**
     * Send a payload to the TestRaftActor to persist and replicate. Since snapshotBatchCount is set to
     * 4 and we already have 3 entries in the journal log, this should initiate a snapshot. In this
     * scenario, the follower consensus and application of state is delayed until after the snapshot
     * completes.
     */
    private void testFirstSnapshot() {
        testLog.info("testFirstSnapshot starting");

        expSnapshotState.add(recoveredPayload0);
        expSnapshotState.add(recoveredPayload1);
        expSnapshotState.add(recoveredPayload2);

        // Delay the consensus by temporarily dropping the AppendEntries to both followers.
        follower1Actor.underlyingActor().startDropMessages(AppendEntries.class);
        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

        // Send the payload.
        payload3 = sendPayloadData(leaderActor, "three");

        // Wait for snapshot complete.
        awaitSnapshot(leaderActor);

        // The snapshot index should not be advanced nor the log trimmed because replicatedToAllIndex
        // is behind due the followers not being replicated yet via AppendEntries.
        assertEquals("Leader snapshot term", initialTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", 1, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader journal log size", 2, leaderContext.getReplicatedLog().size());
        assertEquals("Leader journal last index", 3, leaderContext.getReplicatedLog().lastIndex());

        // Verify the persisted snapshot in the leader. This should reflect the advanced snapshot index as
        // the last applied log entry (2) even though the leader hasn't yet advanced its cached snapshot index.
        List<Snapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
        assertEquals("Persisted snapshots size", 1, persistedSnapshots.size());
        verifySnapshot("Persisted", persistedSnapshots.get(0), initialTerm, 2, currentTerm, 3);
        List<ReplicatedLogEntry> unAppliedEntry = persistedSnapshots.get(0).getUnAppliedEntries();
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 1, unAppliedEntry.size());
        verifyReplicatedLogEntry(unAppliedEntry.get(0), currentTerm, 3, payload3);

        // The leader's persisted journal log should be cleared since we snapshotted.
        List<SimpleReplicatedLogEntry> persistedLeaderJournal =
                InMemoryJournal.get(leaderId, SimpleReplicatedLogEntry.class);
        assertEquals("Persisted journal log size", 0, persistedLeaderJournal.size());

        // Allow AppendEntries to both followers to proceed. This should catch up the followers and cause a
        // "fake" snapshot in the leader to advance the snapshot index to 2. Also the state should be applied
        // in all members (via ApplyState).
        follower1Actor.underlyingActor().stopDropMessages(AppendEntries.class);
        follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
        verifyApplyState(applyState, leaderCollectorActor, payload3.toString(), currentTerm, 3, payload3);

        verifyApplyIndex(leaderActor, 3);

        final var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader commit index", 3, leaderLog.getCommitIndex());

        applyState = MessageCollectorActor.expectFirstMatching(follower1CollectorActor, ApplyState.class);
        verifyApplyState(applyState, null, null, currentTerm, 3, payload3);

        verifyApplyIndex(follower1Actor, 3);

        applyState = MessageCollectorActor.expectFirstMatching(follower2CollectorActor, ApplyState.class);
        verifyApplyState(applyState, null, null, currentTerm, 3, payload3);

        verifyApplyIndex(follower2Actor, 3);

        assertEquals("Leader snapshot term", initialTerm, leaderLog.getSnapshotTerm());
        assertEquals("Leader snapshot index", 2, leaderLog.getSnapshotIndex());
        assertEquals("Leader journal log size", 1, leaderLog.size());
        assertEquals("Leader commit index", 3, leaderLog.getCommitIndex());
        assertEquals("Leader last applied", 3, leaderLog.getLastApplied());
        assertEquals("Leader replicatedToAllIndex", 2, leader.getReplicatedToAllIndex());

        // The followers should also snapshot so verify.

        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, SaveSnapshotSuccess.class);
        persistedSnapshots = InMemorySnapshotStore.getSnapshots(follower1Id, Snapshot.class);
        assertEquals("Persisted snapshots size", 1, persistedSnapshots.size());
        // The last applied index in the snapshot may or may not be the last log entry depending on
        // timing so to avoid intermittent test failures, we'll just verify the snapshot's last term/index.
        assertEquals("Follower1 Snapshot getLastTerm", currentTerm, persistedSnapshots.get(0).getLastTerm());
        assertEquals("Follower1 Snapshot getLastIndex", 3, persistedSnapshots.get(0).getLastIndex());

        MessageCollectorActor.expectFirstMatching(follower2CollectorActor, SaveSnapshotSuccess.class);

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.clearMessages(follower2CollectorActor);

        testLog.info("testFirstSnapshot ending");
    }

    /**
     * Send 3 more payload instances and verify they get applied by all members.
     */
    private void testSubsequentReplications() {
        testLog.info("testSubsequentReplications starting");

        payload4 = sendPayloadData(leaderActor, "four");
        payload5 = sendPayloadData(leaderActor, "five");
        payload6 = sendPayloadData(leaderActor, "six");

        // Verify the leader applies the states.
        List<ApplyState> applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), leaderCollectorActor, payload4.toString(), currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(1), leaderCollectorActor, payload5.toString(), currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(2), leaderCollectorActor, payload6.toString(), currentTerm, 6, payload6);

        // Verify the leader applies a log entry for at least the last entry index.
        verifyApplyIndex(leaderActor, 6);

        // The leader should have performed fake snapshots due to the follower's AppendEntriesReplies and
        // trimmed the in-memory log so that only the last entry remains.
        final var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader snapshot term", currentTerm, leaderLog.getSnapshotTerm());
        assertEquals("Leader snapshot index", 5, leaderLog.getSnapshotIndex());
        assertEquals("Leader journal log size", 1, leaderLog.size());
        assertEquals("Leader journal last index", 6, leaderLog.lastIndex());
        assertEquals("Leader commit index", 6, leaderLog.getCommitIndex());
        assertEquals("Leader last applied", 6, leaderLog.getLastApplied());
        assertEquals("Leader replicatedToAllIndex", 5, leader.getReplicatedToAllIndex());

        // Verify follower 1 applies the states.
        applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 6, payload6);

        // Verify follower 1 applies a log entry for at least the last entry index.
        verifyApplyIndex(follower1Actor, 6);

        // Verify follower 2 applies the states.
        applyStates = MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 6, payload6);

        // Verify follower 2 applies a log entry for at least the last entry index.
        verifyApplyIndex(follower2Actor, 6);

        MessageCollectorActor.clearMessages(leaderCollectorActor);

        testLog.info("testSubsequentReplications ending");
    }

    /**
     * Send one more payload to trigger another snapshot. In this scenario, we delay the snapshot until
     * consensus occurs and the leader applies the state.
     */
    private void testSecondSnapshot() {
        testLog.info("testSecondSnapshot starting");

        expSnapshotState.add(payload3);
        expSnapshotState.add(payload4);
        expSnapshotState.add(payload5);
        expSnapshotState.add(payload6);

        // Delay the CaptureSnapshot message to the leader actor.
        leaderActor.underlyingActor().startDropMessages(SaveSnapshotSuccess.class);

        // Send the payload.
        payload7 = sendPayloadData(leaderActor, "seven");

        // Capture the SaveSnapshotSuccess message so we can send it later.
        final var saveSuccess = MessageCollectorActor.expectFirstMatching(
                leaderCollectorActor, SaveSnapshotSuccess.class);

        // Wait for the state to be applied in the leader.
        ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
        verifyApplyState(applyState, leaderCollectorActor, payload7.toString(), currentTerm, 7, payload7);

        // At this point the leader has applied the new state but the cached snapshot index should not be
        // advanced by a "fake" snapshot because we're in the middle of a snapshot. We'll wait for at least
        // one more heartbeat AppendEntriesReply to ensure this does not occur.
        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

        var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader snapshot term", currentTerm, leaderLog.getSnapshotTerm());
        assertEquals("Leader snapshot index", 5, leaderLog.getSnapshotIndex());
        assertEquals("Leader journal log size", 2, leaderLog.size());
        assertEquals("Leader journal last index", 7, leaderLog.lastIndex());
        assertEquals("Leader commit index", 7, leaderLog.getCommitIndex());
        assertEquals("Leader last applied", 7, leaderLog.getLastApplied());
        assertEquals("Leader replicatedToAllIndex", 5, leader.getReplicatedToAllIndex());

        // Now deliver the CaptureSnapshotReply.
        leaderActor.underlyingActor().stopDropMessages(SaveSnapshotSuccess.class);
        leaderActor.tell(saveSuccess, leaderActor);

        // Wait for snapshot complete.
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        // Wait for another heartbeat AppendEntriesReply. This should cause a "fake" snapshot to advance the
        // snapshot index and trimmed the log since we're no longer in a snapshot.
        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

        leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader snapshot term", currentTerm, leaderLog.getSnapshotTerm());
        assertEquals("Leader snapshot index", 6, leaderLog.getSnapshotIndex());
        assertEquals("Leader journal log size", 1, leaderLog.size());
        assertEquals("Leader journal last index", 7, leaderLog.lastIndex());
        assertEquals("Leader commit index", 7, leaderLog.getCommitIndex());

        // Verify the persisted snapshot. This should reflect the snapshot index as the last applied
        // log entry (7) and shouldn't contain any unapplied entries as we capture persisted the snapshot data
        // when the snapshot is created (ie when the CaptureSnapshot is processed).
        List<Snapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
        assertEquals("Persisted snapshots size", 1, persistedSnapshots.size());
        verifySnapshot("Persisted", persistedSnapshots.get(0), currentTerm, 6, currentTerm, 7);
        List<ReplicatedLogEntry> unAppliedEntry = persistedSnapshots.get(0).getUnAppliedEntries();
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 1, unAppliedEntry.size());
        verifyReplicatedLogEntry(unAppliedEntry.get(0), currentTerm, 7, payload7);

        // The leader's persisted journal log should be cleared since we did a snapshot.
        List<SimpleReplicatedLogEntry> persistedLeaderJournal = InMemoryJournal.get(
                leaderId, SimpleReplicatedLogEntry.class);
        assertEquals("Persisted journal log size", 0, persistedLeaderJournal.size());

        // Verify the followers apply all 4 new log entries.
        List<ApplyState> applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor,
                ApplyState.class, 4);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 6, payload6);
        verifyApplyState(applyStates.get(3), null, null, currentTerm, 7, payload7);

        applyStates = MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, 4);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 6, payload6);
        verifyApplyState(applyStates.get(3), null, null, currentTerm, 7, payload7);

        // Verify the follower's snapshot index has also advanced. (after another AppendEntries heartbeat
        // to be safe).

        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, AppendEntries.class);
        follower1Context = follower1Actor.underlyingActor().getRaftActorContext();
        final var folloer1log = follower1Context.getReplicatedLog();
        assertEquals("Follower 1 snapshot term", currentTerm, folloer1log.getSnapshotTerm());
        assertEquals("Follower 1 snapshot index", 6, folloer1log.getSnapshotIndex());
        assertEquals("Follower 1 journal log size", 1, folloer1log.size());
        assertEquals("Follower 1 journal last index", 7, folloer1log.lastIndex());
        assertEquals("Follower 1 commit index", 7, folloer1log.getCommitIndex());

        MessageCollectorActor.clearMessages(follower2CollectorActor);
        MessageCollectorActor.expectFirstMatching(follower2CollectorActor, AppendEntries.class);
        follower2Context = follower2Actor.underlyingActor().getRaftActorContext();
        final var folloer2log = follower2Context.getReplicatedLog();
        assertEquals("Follower 2 snapshot term", currentTerm, folloer2log.getSnapshotTerm());
        assertEquals("Follower 2 snapshot index", 6, folloer2log.getSnapshotIndex());
        assertEquals("Follower 2 journal log size", 1, folloer2log.size());
        assertEquals("Follower 2 journal last index", 7, folloer2log.lastIndex());
        assertEquals("Follower 2 commit index", 7, folloer2log.getCommitIndex());

        expSnapshotState.add(payload7);

        testLog.info("testSecondSnapshot ending");
    }

    /**
     * Kill the leader actor, reinstate it and verify the recovered journal.
     */
    private void testLeaderReinstatement() {
        testLog.info("testLeaderReinstatement starting");

        killActor(leaderActor);

        leaderActor = newTestRaftActor(leaderId, peerAddresses, leaderConfigParams);

        leaderActor.underlyingActor().waitForRecoveryComplete();

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();

        final var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader snapshot term", currentTerm, leaderLog.getSnapshotTerm());
        assertEquals("Leader snapshot index", 6, leaderLog.getSnapshotIndex());
        assertEquals("Leader journal log size", 1, leaderLog.size());
        assertEquals("Leader journal last index", 7, leaderLog.lastIndex());
        assertEquals("Leader commit index", 7, leaderLog.getCommitIndex());
        assertEquals("Leader last applied", 7, leaderLog.getLastApplied());
        verifyReplicatedLogEntry(leaderContext.getReplicatedLog().last(), currentTerm, 7, payload7);

        testLog.info("testLeaderReinstatement ending");
    }
}
