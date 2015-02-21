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
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.dispatch.Dispatchers;
import akka.pattern.Patterns;
import akka.persistence.SaveSnapshotSuccess;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.RaftActor.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.RaftActorTest.MockRaftActor;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Integration tests that test end-to-end RaftActor and behavior functionality.
 *
 * @author Thomas Pantelis
 */
public class RaftActorIntegrationTest extends AbstractActorTest {

    public static class TestRaftActor extends MockRaftActor {

        private final TestActorRef<MessageCollectorActor> collectorActor;
        private final Map<Class<?>, Boolean> dropMessages = new ConcurrentHashMap<>();
        private volatile byte[] snapshot;
        private volatile long mockTotalMemory;

        private TestRaftActor(String id, Map<String, String> peerAddresses, ConfigParams config,
                TestActorRef<MessageCollectorActor> collectorActor) {
            super(id, peerAddresses, Optional.of(config), null);
            dataPersistenceProvider = new PersistentDataProvider();
            this.collectorActor = collectorActor;
        }

        public static Props props(String id, Map<String, String> peerAddresses, ConfigParams config,
                TestActorRef<MessageCollectorActor> collectorActor) {
            return Props.create(TestRaftActor.class, id, peerAddresses, config, collectorActor).
                    withDispatcher(Dispatchers.DefaultDispatcherId());
        }

        void startDropMessages(Class<?> msgClass) {
            dropMessages.put(msgClass, Boolean.TRUE);
        }

        void stopDropMessages(Class<?> msgClass) {
            dropMessages.remove(msgClass);
        }

        void setMockTotalMemory(long mockTotalMemory) {
            this.mockTotalMemory = mockTotalMemory;
        }

        @Override
        protected long getTotalMemory() {
            return mockTotalMemory > 0 ? mockTotalMemory : super.getTotalMemory();
        }

        @Override
        public void handleCommand(Object message) {
            if(message instanceof MockPayload) {
                MockPayload payload = (MockPayload)message;
                super.persistData(collectorActor, payload.toString(), payload);
                return;
            }

            try {
                if(!dropMessages.containsKey(message.getClass())) {
                    super.handleCommand(message);
                }
            } finally {
                if(!(message instanceof SendHeartBeat)) {
                    try {
                        collectorActor.tell(message, ActorRef.noSender());
                    } catch (Exception e) {
                        testLog.error("MessageCollectorActor error", e);
                    }
                }
            }
        }

        @Override
        protected void createSnapshot() {
            if(snapshot != null) {
                getSelf().tell(new CaptureSnapshotReply(snapshot), ActorRef.noSender());
            }
        }

        @Override
        protected void applyRecoverySnapshot(byte[] bytes) {
        }

        void setSnapshot(byte[] snapshot) {
            this.snapshot = snapshot;
        }

        public ActorRef collectorActor() {
            return collectorActor;
        }
    }

    private final static Logger testLog = LoggerFactory.getLogger(RaftActorIntegrationTest.class);

    private final TestActorFactory factory = new TestActorFactory(getSystem());

    @After
    public void tearDown() {
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
        factory.close();
    }

    private DefaultConfigParamsImpl newLeaderConfigParams() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(1);
        configParams.setSnapshotBatchCount(4);
        configParams.setSnapshotDataThresholdPercentage(70);
        configParams.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));
        return configParams;
    }

    private DefaultConfigParamsImpl newFollowerConfigParams() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(500, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(1000);
        return configParams;
    }

    private void waitUntilLeader(ActorRef actorRef) {
        FiniteDuration duration = Duration.create(100, TimeUnit.MILLISECONDS);
        for(int i = 0; i < 20 * 5; i++) {
            Future<Object> future = Patterns.ask(actorRef, new FindLeader(), new Timeout(duration));
            try {
                FindLeaderReply resp = (FindLeaderReply) Await.result(future, duration);
                if(resp.getLeaderActor() != null) {
                    return;
                }
            } catch(TimeoutException e) {
            } catch(Exception e) {
                testLog.error("FindLeader threw ex", e);
            }


            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        Assert.fail("Leader not found for actorRef " + actorRef.path());
    }

    private TestActorRef<TestRaftActor> newTestRaftActor(String id, Map<String, String> peerAddresses,
            ConfigParams configParams) {
        TestActorRef<MessageCollectorActor> collectorActor = factory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        factory.generateActorId(id + "-collector"));
        return factory.createTestActor(TestRaftActor.props(id,
                peerAddresses != null ? peerAddresses : Collections.<String, String>emptyMap(),
                        configParams, collectorActor), id);
    }

    /**
     * Tests end-to-end replication and snapshots using real RaftActor and behavior communication. The major
     * steps of the test are delineated in methods.
     */
    private class TestReplicationAndSnapshots {
        String leaderId = factory.generateActorId("leader");
        DefaultConfigParamsImpl leaderConfigParams;
        TestActorRef<TestRaftActor> leaderActor;
        ActorRef leaderCollectorActor;
        RaftActorContext leaderContext;
        RaftActorBehavior leader;

        String follower1Id = factory.generateActorId("follower");
        TestActorRef<TestRaftActor> follower1Actor;
        ActorRef follower1CollectorActor;

        String follower2Id = factory.generateActorId("follower");
        TestActorRef<TestRaftActor> follower2Actor;
        ActorRef follower2CollectorActor;

        List<ReplicatedLogImplEntry> origLeaderJournal;

        ImmutableMap<String, String> peerAddresses;

        long initialTerm = 5;
        long currentTerm;

        MockPayload recoveredPayload0;
        MockPayload recoveredPayload1;
        MockPayload recoveredPayload2;
        MockPayload payload4;
        MockPayload payload5;
        MockPayload payload6;
        MockPayload payload7;

        void run() {
            testLog.info("testReplicationAndSnapshots starting");

            // Setup the persistent journal for the leader. We'll start up with 3 journal log entries (one less
            // than the snapshotBatchCount).
            long seqId = 1;
            InMemoryJournal.addEntry(leaderId, seqId++, new UpdateElectionTerm(initialTerm, leaderId));
            recoveredPayload0 = new MockPayload("zero");
            InMemoryJournal.addEntry(leaderId, seqId++, new ReplicatedLogImplEntry(0, initialTerm, recoveredPayload0));
            recoveredPayload1 = new MockPayload("one");
            InMemoryJournal.addEntry(leaderId, seqId++, new ReplicatedLogImplEntry(1, initialTerm, recoveredPayload1));
            recoveredPayload2 = new MockPayload("two");
            InMemoryJournal.addEntry(leaderId, seqId++, new ReplicatedLogImplEntry(2, initialTerm, recoveredPayload2));
            InMemoryJournal.addEntry(leaderId, seqId++, new ApplyLogEntries(2));

            origLeaderJournal = InMemoryJournal.get(leaderId, ReplicatedLogImplEntry.class);

            // Create the leader and 2 follower actors and verify initial syncing of the followers after leader
            // persistence recovery.

            follower1Actor = newTestRaftActor(follower1Id, null, newFollowerConfigParams());

            follower2Actor = newTestRaftActor(follower2Id, null, newFollowerConfigParams());

            peerAddresses = ImmutableMap.<String, String>builder().
                    put(follower1Id, follower1Actor.path().toString()).
                    put(follower2Id, follower2Actor.path().toString()).build();

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

            currentTerm = leaderContext.getTermInformation().getCurrentTerm();
            assertEquals("Current term > " + initialTerm, true, currentTerm > initialTerm);

            leader = leaderActor.underlyingActor().getCurrentBehavior();

            // The followers should receive AppendEntries for each leader log entry that was recovered from
            // persistence and apply each one.
            List<ApplyState> applyStates = MessageCollectorActor.expectMatching(
                    follower1CollectorActor, ApplyState.class, 3);
            verifyApplyState(applyStates.get(0), null, null, initialTerm, 0, recoveredPayload0);
            verifyApplyState(applyStates.get(1), null, null, initialTerm, 1, recoveredPayload1);
            verifyApplyState(applyStates.get(2), null, null, initialTerm, 2, recoveredPayload2);

            // Verify follower 1 applies a log entry for at least the last entry index.
            verifyApplyLogEntry(follower1CollectorActor, 2);

            applyStates = MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, 3);
            verifyApplyState(applyStates.get(0), null, null, initialTerm, 0, recoveredPayload0);
            verifyApplyState(applyStates.get(1), null, null, initialTerm, 1, recoveredPayload1);
            verifyApplyState(applyStates.get(2), null, null, initialTerm, 2, recoveredPayload2);

            // Verify follower 1]2 applies a log entry for at least the last entry index.
            verifyApplyLogEntry(follower2CollectorActor, 2);

            MessageCollectorActor.clearMessages(leaderCollectorActor);
            MessageCollectorActor.clearMessages(follower1CollectorActor);
            MessageCollectorActor.clearMessages(follower2CollectorActor);

            // The leader should have performed fake snapshots due to the follower's AppendEntriesReplies and
            // trimmed the in-memory log so that only the last entry remains.
            assertEquals("Leader snapshot term", initialTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
            assertEquals("Leader snapshot index", 1, leaderContext.getReplicatedLog().getSnapshotIndex());
            assertEquals("Leader journal log size", 1, leaderContext.getReplicatedLog().size());
            assertEquals("Leader journal last index", 2, leaderContext.getReplicatedLog().lastIndex());
            assertEquals("Leader commit index", 2, leaderContext.getCommitIndex());
            assertEquals("Leader last applied", 2, leaderContext.getLastApplied());
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
        void testFirstSnapshot() {
            testLog.info("testFirstSnapshot starting");

            byte[] snapshot = new byte[] {1,2,3,4};
            leaderActor.underlyingActor().setSnapshot(snapshot);

            // Delay the consensus by temporarily dropping the AppendEntries to both followers.
            follower1Actor.underlyingActor().startDropMessages(AppendEntries.class);
            follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

            // Send the payload.
            MockPayload payload3 = sendPayloadData(leaderActor, "three");

            // Wait for snapshot complete.
            MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

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
            verifySnapshot("Persisted", persistedSnapshots.get(0), initialTerm, 2, currentTerm, 3, snapshot);
            List<ReplicatedLogEntry> unAppliedEntry = persistedSnapshots.get(0).getUnAppliedEntries();
            assertEquals("Persisted Snapshot getUnAppliedEntries size", 1, unAppliedEntry.size());
            verifyReplicatedLogEntry(unAppliedEntry.get(0), currentTerm, 3, payload3);

            // The leader's persisted journal log should be cleared since we snapshotted.
            List<ReplicatedLogImplEntry> persistedLeaderJournal = InMemoryJournal.get(leaderId, ReplicatedLogImplEntry.class);
            assertEquals("Persisted journal log size", 0, persistedLeaderJournal.size());

            // Allow AppendEntries to both followers to proceed. This should catch up the followers and cause a
            // "fake" snapshot in the leader to advance the snapshot index to 2. Also the state should be applied
            // in all members (via ApplyState).
            follower1Actor.underlyingActor().stopDropMessages(AppendEntries.class);
            follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);

            ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
            verifyApplyState(applyState, leaderCollectorActor, payload3.toString(), currentTerm, 3, payload3);

            verifyApplyLogEntry(leaderCollectorActor, 3);

            assertEquals("Leader commit index", 3, leaderContext.getCommitIndex());

            applyState = MessageCollectorActor.expectFirstMatching(follower1CollectorActor, ApplyState.class);
            verifyApplyState(applyState, null, null, currentTerm, 3, payload3);

            verifyApplyLogEntry(follower1CollectorActor, 3);

            applyState = MessageCollectorActor.expectFirstMatching(follower2CollectorActor, ApplyState.class);
            verifyApplyState(applyState, null, null, currentTerm, 3, payload3);

            verifyApplyLogEntry(follower2CollectorActor, 3);

            assertEquals("Leader snapshot term", initialTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
            assertEquals("Leader snapshot index", 2, leaderContext.getReplicatedLog().getSnapshotIndex());
            assertEquals("Leader journal log size", 1, leaderContext.getReplicatedLog().size());
            assertEquals("Leader commit index", 3, leaderContext.getCommitIndex());
            assertEquals("Leader last applied", 3, leaderContext.getLastApplied());
            assertEquals("Leader replicatedToAllIndex", 2, leader.getReplicatedToAllIndex());

            MessageCollectorActor.clearMessages(leaderCollectorActor);
            MessageCollectorActor.clearMessages(follower1CollectorActor);
            MessageCollectorActor.clearMessages(follower2CollectorActor);

            testLog.info("testFirstSnapshot ending");
        }

        /**
         * Send 3 more payload instances and verify they get applied by all members.
         */
        void testSubsequentReplications() {
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
            verifyApplyLogEntry(leaderCollectorActor, 6);

            // The leader should have performed fake snapshots due to the follower's AppendEntriesReplies and
            // trimmed the in-memory log so that only the last entry remains.
            assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
            assertEquals("Leader snapshot index", 5, leaderContext.getReplicatedLog().getSnapshotIndex());
            assertEquals("Leader journal log size", 1, leaderContext.getReplicatedLog().size());
            assertEquals("Leader journal last index", 6, leaderContext.getReplicatedLog().lastIndex());
            assertEquals("Leader commit index", 6, leaderContext.getCommitIndex());
            assertEquals("Leader last applied", 6, leaderContext.getLastApplied());
            assertEquals("Leader replicatedToAllIndex", 5, leader.getReplicatedToAllIndex());

            // Verify follower 1 applies the states.
            applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 3);
            verifyApplyState(applyStates.get(0), null, null, currentTerm, 4, payload4);
            verifyApplyState(applyStates.get(1), null, null, currentTerm, 5, payload5);
            verifyApplyState(applyStates.get(2), null, null, currentTerm, 6, payload6);

            // Verify follower 1 applies a log entry for at least the last entry index.
            verifyApplyLogEntry(follower1CollectorActor, 6);

            // Verify follower 2 applies the states.
            applyStates = MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, 3);
            verifyApplyState(applyStates.get(0), null, null, currentTerm, 4, payload4);
            verifyApplyState(applyStates.get(1), null, null, currentTerm, 5, payload5);
            verifyApplyState(applyStates.get(2), null, null, currentTerm, 6, payload6);

            // Verify follower 2 applies a log entry for at least the last entry index.
            verifyApplyLogEntry(follower2CollectorActor, 6);

            MessageCollectorActor.clearMessages(leaderCollectorActor);

            testLog.info("testSubsequentReplications ending");
        }

        /**
         * Send one more payload to trigger another snapshot. In this scenario, we delay the snapshot until
         * consensus occurs and the leader applies the state.
         */
        void testSecondSnapshot() {
            testLog.info("testSecondSnapshot starting");

            byte[] snapshot = new byte[] {5,6,7,8};
            leaderActor.underlyingActor().setSnapshot(snapshot);

            // Delay the CaptureSnapshot message to the leader actor.
            leaderActor.underlyingActor().startDropMessages(CaptureSnapshot.class);

            // Send the payload.
            payload7 = sendPayloadData(leaderActor, "seven");

            // Capture the CaptureSnapshot message so we can send it later.
            CaptureSnapshot captureSnapshot = MessageCollectorActor.expectFirstMatching(
                    leaderCollectorActor, CaptureSnapshot.class);

            // Wait for the state to be applied in the leader.
            ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
            verifyApplyState(applyState, leaderCollectorActor, payload7.toString(), currentTerm, 7, payload7);

            // At this point the leader has applied the new state but the cached snapshot index should not be
            // advanced by a "fake" snapshot because we're in the middle of a snapshot. We'll wait for at least
            // one more heartbeat AppendEntriesReply to ensure this does not occur.
            MessageCollectorActor.clearMessages(leaderCollectorActor);
            MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);

            assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
            assertEquals("Leader snapshot index", 5, leaderContext.getReplicatedLog().getSnapshotIndex());
            assertEquals("Leader journal log size", 2, leaderContext.getReplicatedLog().size());
            assertEquals("Leader journal last index", 7, leaderContext.getReplicatedLog().lastIndex());
            assertEquals("Leader commit index", 7, leaderContext.getCommitIndex());
            assertEquals("Leader last applied", 7, leaderContext.getLastApplied());
            assertEquals("Leader replicatedToAllIndex", 5, leader.getReplicatedToAllIndex());

            // Now deliver the CaptureSnapshot.
            leaderActor.underlyingActor().stopDropMessages(CaptureSnapshot.class);
            leaderActor.tell(captureSnapshot, leaderActor);

            // Wait for CaptureSnapshotReply to complete.
            MessageCollectorActor.expectFirstMatching(leaderCollectorActor, CaptureSnapshotReply.class);

            // Wait for snapshot complete.
            MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

            // Wait for another heartbeat AppendEntriesReply. This should cause a "fake" snapshot to advance the
            // snapshot index and trimmed the log since we're no longer in a snapshot.
            MessageCollectorActor.clearMessages(leaderCollectorActor);
            MessageCollectorActor.expectFirstMatching(leaderCollectorActor, AppendEntriesReply.class);
            assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
            assertEquals("Leader snapshot index", 6, leaderContext.getReplicatedLog().getSnapshotIndex());
            assertEquals("Leader journal log size", 1, leaderContext.getReplicatedLog().size());
            assertEquals("Leader journal last index", 7, leaderContext.getReplicatedLog().lastIndex());
            assertEquals("Leader commit index", 7, leaderContext.getCommitIndex());

            // Verify the persisted snapshot. This should reflect the advanced snapshot index as the last applied
            // log entry (6).
            List<Snapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
            assertEquals("Persisted snapshots size", 1, persistedSnapshots.size());
            verifySnapshot("Persisted", persistedSnapshots.get(0), currentTerm, 6, currentTerm, 7, snapshot);
            List<ReplicatedLogEntry> unAppliedEntry = persistedSnapshots.get(0).getUnAppliedEntries();
            assertEquals("Persisted Snapshot getUnAppliedEntries size", 1, unAppliedEntry.size());
            verifyReplicatedLogEntry(unAppliedEntry.get(0), currentTerm, 7, payload7);

            // The leader's persisted journal log should be cleared since we did a snapshot.
            List<ReplicatedLogImplEntry> persistedLeaderJournal = InMemoryJournal.get(
                    leaderId, ReplicatedLogImplEntry.class);
            assertEquals("Persisted journal log size", 0, persistedLeaderJournal.size());

            // Verify the followers apply all 4 new log entries.
            List<ApplyState> applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 4);
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
            RaftActorContext follower1Context = follower1Actor.underlyingActor().getRaftActorContext();
            assertEquals("Follower 1 snapshot term", currentTerm, follower1Context.getReplicatedLog().getSnapshotTerm());
            assertEquals("Follower 1 snapshot index", 6, follower1Context.getReplicatedLog().getSnapshotIndex());
            assertEquals("Follower 1 journal log size", 1, follower1Context.getReplicatedLog().size());
            assertEquals("Follower 1 journal last index", 7, follower1Context.getReplicatedLog().lastIndex());
            assertEquals("Follower 1 commit index", 7, follower1Context.getCommitIndex());

            MessageCollectorActor.clearMessages(follower2CollectorActor);
            MessageCollectorActor.expectFirstMatching(follower2CollectorActor, AppendEntries.class);
            RaftActorContext follower2Context = follower2Actor.underlyingActor().getRaftActorContext();
            assertEquals("Follower 2 snapshot term", currentTerm, follower2Context.getReplicatedLog().getSnapshotTerm());
            assertEquals("Follower 2 snapshot index", 6, follower2Context.getReplicatedLog().getSnapshotIndex());
            assertEquals("Follower 2 journal log size", 1, follower2Context.getReplicatedLog().size());
            assertEquals("Follower 2 journal last index", 7, follower2Context.getReplicatedLog().lastIndex());
            assertEquals("Follower 2 commit index", 7, follower2Context.getCommitIndex());

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

            assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
            assertEquals("Leader snapshot index", 6, leaderContext.getReplicatedLog().getSnapshotIndex());
            assertEquals("Leader journal log size", 1, leaderContext.getReplicatedLog().size());
            assertEquals("Leader journal last index", 7, leaderContext.getReplicatedLog().lastIndex());
            assertEquals("Leader commit index", 7, leaderContext.getCommitIndex());
            assertEquals("Leader last applied", 7, leaderContext.getLastApplied());
            verifyReplicatedLogEntry(leaderContext.getReplicatedLog().last(), currentTerm, 7, payload7);

            testLog.info("testLeaderReinstatement ending");
        }
    }

    @Test
    public void testReplicationAndSnapshots() {
        new TestReplicationAndSnapshots().run();
    }

    /**
     * Tests end-to-end replication and snapshots using real RaftActor and behavior communication with a
     * lagging follower. The major steps of the test are numbered below.
     */
    private class TestReplicationAndSnapshotsWithLaggingFollower {
        String leaderId = factory.generateActorId("leader");
        DefaultConfigParamsImpl leaderConfigParams;
        TestActorRef<TestRaftActor> leaderActor;
        ActorRef leaderCollectorActor;
        RaftActorContext leaderContext;
        RaftActorBehavior leader;

        String follower1Id = factory.generateActorId("follower");
        TestActorRef<TestRaftActor> follower1Actor;
        ActorRef follower1CollectorActor;
        RaftActorBehavior follower1;
        RaftActorContext follower1Context;

        String follower2Id = factory.generateActorId("follower");
        TestActorRef<TestRaftActor> follower2Actor;
        ActorRef follower2CollectorActor;
        RaftActorBehavior follower2;
        RaftActorContext follower2Context;

        ImmutableMap<String, String> peerAddresses;

        long initialTerm = 5;
        long currentTerm;

        MockPayload payload11;
        MockPayload payload12;
        MockPayload payload13;

        void run() throws Exception {
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

            testSubsequentReplicationsAndInstallSnapshot();

            testFinalReplicationsAndSnapshot();

            testLeaderReinstatement();

            testLog.info("testReplicationAndSnapshotsWithLaggingFollower ending");
        }

        /**
         * Send 3 payload instances with follower 2 temporarily lagging.
         *
         * @throws Exception
         */
        void testInitialReplications() throws Exception {

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
         * to trigger a leader snapshot and a subsequent install snapshot to the lagging follower.
         *
         * @throws Exception
         */
        void testSubsequentReplicationsAndInstallSnapshot() throws Exception {

            testLog.info("testSubsequentReplicationsAndInstallSnapshot starting: sending 3 payloads, replicatedToAllIndex: {}",
                    leader.getReplicatedToAllIndex());

            leaderActor.underlyingActor().setMockTotalMemory(1000);
            byte[] snapshot = new byte[] {6};
            leaderActor.underlyingActor().setSnapshot(snapshot);

            // We'll expect a ReplicatedLogImplEntry message and an ApplyLogEntries message added to the journal.
            InMemoryJournal.addWriteMessagesCompleteLatch(leaderId, 2);

            follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

            // Send a payload with a large relative size but not enough to trigger a snapshot.
            MockPayload payload8 = sendPayloadData(leaderActor, "eight", 500);

            // Verify the leader got consensus and applies the first log entry even though follower 2 didn't respond.
            List<ApplyState> applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 1);
            verifyApplyState(applyStates.get(0), leaderCollectorActor, payload8.toString(), currentTerm, 8, payload8);

            // Wait for all the ReplicatedLogImplEntry and ApplyLogEntries messages to be added to the journal
            // before the snapshot so the snapshot sequence # will be higher to ensure the snapshot gets
            // purged from the snapshot store after subsequent snapshots.
            InMemoryJournal.waitForWriteMessagesComplete(leaderId);

            // Verify a snapshot is not triggered.
            CaptureSnapshot captureSnapshot = MessageCollectorActor.getFirstMatching(leaderCollectorActor, CaptureSnapshot.class);
            Assert.assertNull("Leader received unexpected CaptureSnapshot", captureSnapshot);

            // Send another payload with a large enough relative size in combination with the last payload
            // that exceeds the memory threshold (70% * 1000 = 700) - this should do a snapshot.
            MockPayload payload9 = sendPayloadData(leaderActor, "nine", 201);

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

            snapshot = new byte[] {10};
            leaderActor.underlyingActor().setSnapshot(snapshot);

            // Now stop dropping AppendEntries in follower 2.
            follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);

            InstallSnapshot installSnapshot = MessageCollectorActor.expectFirstMatching(follower2CollectorActor, InstallSnapshot.class);
            assertEquals("InstallSnapshot getTerm", currentTerm, installSnapshot.getTerm());
            assertEquals("InstallSnapshot getLeaderId", leaderId, installSnapshot.getLeaderId());
            assertEquals("InstallSnapshot getChunkIndex", 1, installSnapshot.getChunkIndex());
            assertEquals("InstallSnapshot getTotalChunks", 1, installSnapshot.getTotalChunks());
            assertEquals("InstallSnapshot getLastIncludedTerm", currentTerm, installSnapshot.getLastIncludedTerm());
            assertEquals("InstallSnapshot getLastIncludedIndex", 8, installSnapshot.getLastIncludedIndex());
            assertArrayEquals("InstallSnapshot getData", snapshot, installSnapshot.getData().toByteArray());

            InstallSnapshotReply installSnapshotReply = MessageCollectorActor.expectFirstMatching(
                    leaderCollectorActor, InstallSnapshotReply.class);
            assertEquals("InstallSnapshotReply getTerm", currentTerm, installSnapshotReply.getTerm());
            assertEquals("InstallSnapshotReply getChunkIndex", 1, installSnapshotReply.getChunkIndex());
            assertEquals("InstallSnapshotReply getFollowerId", follower2Id, installSnapshotReply.getFollowerId());
            assertEquals("InstallSnapshotReply isSuccess", true, installSnapshotReply.isSuccess());

            // Verify follower 2 applies the snapshot.
            ApplySnapshot applySnapshot = MessageCollectorActor.expectFirstMatching(follower2CollectorActor, ApplySnapshot.class);
            verifySnapshot("Follower 2", applySnapshot.getSnapshot(), currentTerm, 8, currentTerm, 8, snapshot);
            assertEquals("Persisted Snapshot getUnAppliedEntries size", 0, applySnapshot.getSnapshot().getUnAppliedEntries().size());

            // Verify follower 2 only applies the second log entry (9) as the first one (8) was in the snapshot.
            ApplyState applyState = MessageCollectorActor.expectFirstMatching(follower2CollectorActor, ApplyState.class);
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

            // Send another payload to verify log size since the last snapshot is reset and another snapshot
            // is not taken.

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

            testLog.info("testSubsequentReplicationsAndInstallSnapshot ending");
        }

        /**
         * Do another round of payloads and snapshot to verify replicatedToAllIndex gets back on track and
         * snapshots works as expected after doing a follower snapshot. In this step we don't lag a follower.
         */
        void testFinalReplicationsAndSnapshot() {
            testLog.info("testFinalReplicationsAndSnapshot starting: replicatedToAllIndex: {}", leader.getReplicatedToAllIndex());

            byte[] snapshot = new byte[] {14};
            leaderActor.underlyingActor().setSnapshot(snapshot);

            // Send another payload - a snapshot should occur.
            payload11 = sendPayloadData(leaderActor, "eleven");

            // Wait for the snapshot to complete.
            MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

            ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
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
            List<ApplyState> applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 3);
            verifyApplyState(applyStates.get(1), leaderCollectorActor, payload12.toString(), currentTerm, 12, payload12);
            verifyApplyState(applyStates.get(2), leaderCollectorActor, payload13.toString(), currentTerm, 13, payload13);

            // Verify the leader applies a log entry for at least the last entry index.
            verifyApplyLogEntry(leaderCollectorActor, 13);

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

            // Verify the leaders's persisted journal contains an ApplyLogEntries for at least the last entry index.
            List<ApplyLogEntries> persistedApplyLogEntries = InMemoryJournal.get(leaderId, ApplyLogEntries.class);
            boolean found = false;
            for(ApplyLogEntries entry: persistedApplyLogEntries) {
                if(entry.getToIndex() == 13) {
                    found = true;
                    break;
                }
            }

            Assert.assertTrue(String.format("ApplyLogEntries with index %d not found in leader's persisted journal", 13), found);

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
        void testLeaderReinstatement() {
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

    @Test
    public void testReplicationAndSnapshotsWithLaggingFollower() throws Exception {
        new TestReplicationAndSnapshotsWithLaggingFollower().run();
    }

    private void killActor(TestActorRef<TestRaftActor> leaderActor) {
        JavaTestKit testkit = new JavaTestKit(getSystem());
        testkit.watch(leaderActor);

        leaderActor.tell(PoisonPill.getInstance(), null);
        testkit.expectMsgClass(JavaTestKit.duration("5 seconds"), Terminated.class);

        testkit.unwatch(leaderActor);
    }

    private void verifyApplyLogEntry(ActorRef actor, final int expIndex) {
        MessageCollectorActor.expectFirstMatching(actor, ApplyLogEntries.class, new Predicate<ApplyLogEntries>() {
            @Override
            public boolean apply(ApplyLogEntries msg) {
                return msg.getToIndex() == expIndex;
            }
        });
    }

    private void verifySnapshot(String prefix, Snapshot snapshot, long lastAppliedTerm,
            int lastAppliedIndex, long lastTerm, long lastIndex, byte[] data) {
        assertEquals(prefix + " Snapshot getLastAppliedTerm", lastAppliedTerm, snapshot.getLastAppliedTerm());
        assertEquals(prefix + " Snapshot getLastAppliedIndex", lastAppliedIndex, snapshot.getLastAppliedIndex());
        assertEquals(prefix + " Snapshot getLastTerm", lastTerm, snapshot.getLastTerm());
        assertEquals(prefix + " Snapshot getLastIndex", lastIndex, snapshot.getLastIndex());
        assertArrayEquals(prefix + " Snapshot getState", data, snapshot.getState());
    }

    private void verifyPersistedJournal(String persistenceId, List<? extends ReplicatedLogEntry> expJournal) {
        List<ReplicatedLogEntry> journal = InMemoryJournal.get(persistenceId, ReplicatedLogEntry.class);
        assertEquals("Journal ReplicatedLogEntry count", expJournal.size(), journal.size());
        for(int i = 0; i < expJournal.size(); i++) {
            ReplicatedLogEntry expected = expJournal.get(i);
            ReplicatedLogEntry actual = journal.get(i);
            verifyReplicatedLogEntry(expected, actual.getTerm(), actual.getIndex(), actual.getData());
        }
    }

    private MockPayload sendPayloadData(ActorRef leaderActor, String data) {
        return sendPayloadData(leaderActor, data, 0);
    }

    private MockPayload sendPayloadData(ActorRef leaderActor, String data, int size) {
        MockPayload payload;
        if(size > 0) {
            payload = new MockPayload(data, size);
        } else {
            payload = new MockPayload(data);
        }

        leaderActor.tell(payload, ActorRef.noSender());
        return payload;
    }

    private void verifyApplyState(ApplyState applyState, ActorRef expClientActor,
            String expId, long expTerm, long expIndex, MockPayload payload) {
        assertEquals("ApplyState getClientActor", expClientActor, applyState.getClientActor());
        assertEquals("ApplyState getIdentifier", expId, applyState.getIdentifier());
        ReplicatedLogEntry replicatedLogEntry = applyState.getReplicatedLogEntry();
        verifyReplicatedLogEntry(replicatedLogEntry, expTerm, expIndex, payload);
    }

    private void verifyReplicatedLogEntry(ReplicatedLogEntry replicatedLogEntry, long expTerm, long expIndex,
            Payload payload) {
        assertEquals("ReplicatedLogEntry getTerm", expTerm, replicatedLogEntry.getTerm());
        assertEquals("ReplicatedLogEntry getIndex", expIndex, replicatedLogEntry.getIndex());
        assertEquals("ReplicatedLogEntry getData", payload, replicatedLogEntry.getData());
    }
}
