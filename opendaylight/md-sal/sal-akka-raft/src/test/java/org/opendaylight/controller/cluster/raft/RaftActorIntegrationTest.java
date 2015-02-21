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
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.common.persistence.InMemoryJournal;
import org.opendaylight.controller.cluster.common.persistence.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.RaftActor.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.RaftActorTest.MockRaftActor;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * @author Thomas Pantelis
 */
public class RaftActorIntegrationTest extends AbstractActorTest {

    public static class TestRaftActor extends MockRaftActor {

        private final TestActorRef<MessageCollectorActor> collectorActor;
        private final Map<Class<?>, Boolean> dropMessages = new ConcurrentHashMap<>();
        private volatile byte[] snapshot;

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

        @Override
        public void handleCommand(Object message) {
            if(message instanceof MockRaftActorContext.MockPayload) {
                MockRaftActorContext.MockPayload payload = (MockRaftActorContext.MockPayload)message;
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
        factory.close();
    }

    private DefaultConfigParamsImpl newLeaderConfigParams() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(1);
        configParams.setSnapshotBatchCount(4);
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

    @Test
    public void test() {
        String leaderId = factory.generateActorId("leader");
        String follower1Id = factory.generateActorId("follower");
        String follower2Id = factory.generateActorId("follower");

        // Setup the persistent journal for recovery.

        long initialTerm = 5;
        long seqId = 1;
        InMemoryJournal.addEntry(leaderId, seqId++, new UpdateElectionTerm(initialTerm, leaderId));
        MockRaftActorContext.MockPayload payload0 = new MockRaftActorContext.MockPayload("zero");
        InMemoryJournal.addEntry(leaderId, seqId++, new ReplicatedLogImplEntry(0, initialTerm, payload0));
        MockRaftActorContext.MockPayload payload1 = new MockRaftActorContext.MockPayload("one");
        InMemoryJournal.addEntry(leaderId, seqId++, new ReplicatedLogImplEntry(1, initialTerm, payload1));
        MockRaftActorContext.MockPayload payload2 = new MockRaftActorContext.MockPayload("two");
        InMemoryJournal.addEntry(leaderId, seqId++, new ReplicatedLogImplEntry(2, initialTerm, payload2));
        InMemoryJournal.addEntry(leaderId, seqId++, new ApplyLogEntries(2));

        TestActorRef<TestRaftActor> follower1Actor = newTestRaftActor(follower1Id,
                null, newFollowerConfigParams());

        TestActorRef<TestRaftActor> follower2Actor = newTestRaftActor(follower2Id,
                null, newFollowerConfigParams());

        Map<String, String> peerAddresses = ImmutableMap.<String, String>builder().
                put(follower1Id, follower1Actor.path().toString()).
                put(follower2Id, follower2Actor.path().toString()).build();

        DefaultConfigParamsImpl leaderConfigParams = newLeaderConfigParams();
        TestActorRef<TestRaftActor> leaderActor = newTestRaftActor(leaderId,
                peerAddresses, leaderConfigParams);

        waitUntilLeader(leaderActor);

        RaftActorContext leaderContext = leaderActor.underlyingActor().getRaftActorContext();

        long currentTerm = leaderContext.getTermInformation().getCurrentTerm();
        assertEquals("Current term > " + initialTerm, true, currentTerm > initialTerm);

        ActorRef leaderCollectorActor = leaderActor.underlyingActor().collectorActor();
        ActorRef follower1CollectorActor = follower1Actor.underlyingActor().collectorActor();
        ActorRef follower2CollectorActor = follower2Actor.underlyingActor().collectorActor();

        // The followers should receive AppendEntries for each leader log entry that was recovered from
        // persistence and apply each one.
        List<ApplyState> applyStates = MessageCollectorActor.expectMatching(
                follower1CollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), null, null, initialTerm, 0, payload0);
        verifyApplyState(applyStates.get(1), null, null, initialTerm, 1, payload1);
        verifyApplyState(applyStates.get(2), null, null, initialTerm, 2, payload2);

        applyStates = MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), null, null, initialTerm, 0, payload0);
        verifyApplyState(applyStates.get(1), null, null, initialTerm, 1, payload1);
        verifyApplyState(applyStates.get(2), null, null, initialTerm, 2, payload2);

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.clearMessages(follower2CollectorActor);

        // The leader should have performed fake snapshots due to the follower's AppendEntriesReplies and
        // trimmed in-memory log so that only the last entry remains.
        assertEquals("Snapshot term", initialTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Snapshot index", 1, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Journal log size", 1, leaderContext.getReplicatedLog().size());
        assertEquals("Journal last index", 2, leaderContext.getReplicatedLog().lastIndex());

        // Send a payload to the TestRaftActor to persist and replicate. Since snapshotBatchCount is set to
        // 4 and we already have 3 entries in the journal log, this should initiate a snapshot.

        byte[] snapshot = new byte[] {1,2,3,4};
        leaderActor.underlyingActor().setSnapshot(snapshot);

        // Delay the consensus by temporarily dropping the AppendEntries to both followers.

        follower1Actor.underlyingActor().startDropMessages(AppendEntries.class);
        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

        MockRaftActorContext.MockPayload payload3 = sendPayloadData(leaderActor, "three");

        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        // The snapshot index should be advanced nor the log trimmed because replicatedToAllIndex
        // is behind due the followers not being replicated yet.
        assertEquals("Snapshot term", initialTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        //assertEquals("Snapshot index", 2, leaderContext.getReplicatedLog().getSnapshotIndex());
        //assertEquals("Journal log size", 1, leaderContext.getReplicatedLog().size());
        assertEquals("Snapshot index", 1, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Journal log size", 2, leaderContext.getReplicatedLog().size());
        assertEquals("Journal last index", 3, leaderContext.getReplicatedLog().lastIndex());

        // Verify the persisted snapshot. This should reflect the advanced snapshot index as the last applied
        // log entry (2) even though the leader hasn't yet advanced its cached snapshot index.
        List<Snapshot> snapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
        assertEquals("Persisted snapshots size", 1, snapshots.size());
        assertEquals("Persisted Snapshot getLastAppliedTerm", initialTerm, snapshots.get(0).getLastAppliedTerm());
        assertEquals("Persisted Snapshot getLastAppliedIndex", 2, snapshots.get(0).getLastAppliedIndex());
        assertEquals("Persisted Snapshot getLastTerm", currentTerm, snapshots.get(0).getLastTerm());
        assertEquals("Persisted Snapshot getLastIndex", 3, snapshots.get(0).getLastIndex());
        assertArrayEquals("Persisted Snapshot getState", snapshot, snapshots.get(0).getState());
        List<ReplicatedLogEntry> unAppliedEntry = snapshots.get(0).getUnAppliedEntries();
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 1, unAppliedEntry.size());
        assertEquals("Persisted Snapshot unapplied entry term", currentTerm, unAppliedEntry.get(0).getTerm());
        assertEquals("Persisted Snapshot unapplied entry index", 3, unAppliedEntry.get(0).getIndex());
        assertEquals("Persisted Snapshot unapplied entry data", payload3, unAppliedEntry.get(0).getData());

        // Allow AppendEntries to both followers to proceed. This should catch up the followers and cause a
        // "fake" snapshot in the leader to advance the snapshot index to 2. Also the state should be applied
        // in all members.
        follower1Actor.underlyingActor().stopDropMessages(AppendEntries.class);
        follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
        verifyApplyState(applyState, leaderCollectorActor, payload3.toString(), currentTerm, 3, payload3);

        assertEquals("Leader commit index", 3, leaderContext.getCommitIndex());

        applyState = MessageCollectorActor.expectFirstMatching(follower1CollectorActor, ApplyState.class);
        verifyApplyState(applyState, null, null, currentTerm, 3, payload3);

        applyState = MessageCollectorActor.expectFirstMatching(follower2CollectorActor, ApplyState.class);
        verifyApplyState(applyState, null, null, currentTerm, 3, payload3);

        assertEquals("Snapshot term", initialTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Snapshot index", 2, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Journal log size", 1, leaderContext.getReplicatedLog().size());

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.clearMessages(follower2CollectorActor);

        // Send 3 more payload instances and verify they get applied by the leader.

        snapshot = new byte[] {5,6,7,8};
        leaderActor.underlyingActor().setSnapshot(snapshot);

        MockRaftActorContext.MockPayload payload4 = sendPayloadData(leaderActor, "four");
        MockRaftActorContext.MockPayload payload5 = sendPayloadData(leaderActor, "five");
        MockRaftActorContext.MockPayload payload6 = sendPayloadData(leaderActor, "six");

        applyStates = MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 3);
        verifyApplyState(applyStates.get(0), leaderCollectorActor, payload4.toString(), currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(1), leaderCollectorActor, payload5.toString(), currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(2), leaderCollectorActor, payload6.toString(), currentTerm, 6, payload6);

        // The leader should have performed fake snapshots due to the follower's AppendEntriesReplies and
        // trimmed in-memory log so that only the last entry remains.
        assertEquals("Snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Snapshot index", 5, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Journal log size", 1, leaderContext.getReplicatedLog().size());
        assertEquals("Journal last index", 6, leaderContext.getReplicatedLog().lastIndex());

        MessageCollectorActor.clearMessages(leaderCollectorActor);

        // Send one more payload to trigger another snapshot.

        // Delay the CaptureSnapshot message.
        leaderActor.underlyingActor().startDropMessages(CaptureSnapshot.class);

        // Disable heartbeats for now so they don't interfere.
        leaderActor.underlyingActor().startDropMessages(SendHeartBeat.class);

        MockRaftActorContext.MockPayload payload7 = sendPayloadData(leaderActor, "seven");

        // Capture the CaptureSnapshot message.
        CaptureSnapshot captureSnapshot = MessageCollectorActor.expectFirstMatching(
                leaderCollectorActor, CaptureSnapshot.class);

        // Expect AppendEntriesReply messages from the replicate from both followers
        MessageCollectorActor.expectMatching(leaderCollectorActor, AppendEntriesReply.class, 2);

        // Now deliver the CaptureSnapshot.
        leaderActor.underlyingActor().stopDropMessages(CaptureSnapshot.class);
        leaderActor.tell(captureSnapshot, leaderActor);

        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        // Since neither the leader nor the followers have applied the new state yet, the snapshot capture
        // should not have advanced the snapshot index and trimmed the log because replicatedToAllIndex
        // has not yet advanced.
        assertEquals("Snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Snapshot index", 5, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Journal log size", 2, leaderContext.getReplicatedLog().size());
        assertEquals("Journal last index", 7, leaderContext.getReplicatedLog().lastIndex());

        // Verify the persisted snapshot. This should reflect the advanced snapshot index as the last applied
        // log entry (6).
        snapshots = InMemorySnapshotStore.getSnapshots(leaderId, Snapshot.class);
        assertEquals("Persisted snapshots size", 1, snapshots.size());
        assertEquals("Persisted Snapshot getLastAppliedTerm", currentTerm, snapshots.get(0).getLastAppliedTerm());
        assertEquals("Persisted Snapshot getLastAppliedIndex", 6, snapshots.get(0).getLastAppliedIndex());
        assertEquals("Persisted Snapshot getLastTerm", currentTerm, snapshots.get(0).getLastTerm());
        assertEquals("Persisted Snapshot getLastIndex", 7, snapshots.get(0).getLastIndex());
        assertArrayEquals("Persisted Snapshot getState", snapshot, snapshots.get(0).getState());
        unAppliedEntry = snapshots.get(0).getUnAppliedEntries();
        assertEquals("Persisted Snapshot getUnAppliedEntries size", 1, unAppliedEntry.size());
        assertEquals("Persisted Snapshot unapplied entry term", currentTerm, unAppliedEntry.get(0).getTerm());
        assertEquals("Persisted Snapshot unapplied entry index", 7, unAppliedEntry.get(0).getIndex());
        assertEquals("Persisted Snapshot unapplied entry data", payload7, unAppliedEntry.get(0).getData());

        // Re-enable heartbeats. This should cause the new state to be applied and the leader's snapshot index
        // to advance.
        leaderActor.underlyingActor().stopDropMessages(SendHeartBeat.class);
        leaderActor.tell(new SendHeartBeat(), leaderActor);

        applyState = MessageCollectorActor.expectFirstMatching(leaderCollectorActor, ApplyState.class);
        verifyApplyState(applyState, leaderCollectorActor, payload7.toString(), currentTerm, 7, payload7);

        // Verify the followers apply all 4 new log entries.

        applyStates = MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 4);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 6, payload6);
        verifyApplyState(applyStates.get(3), null, null, currentTerm, 7, payload7);

        applyStates = MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, 4);
        verifyApplyState(applyStates.get(0), null, null, currentTerm, 4, payload4);
        verifyApplyState(applyStates.get(1), null, null, currentTerm, 5, payload5);
        verifyApplyState(applyStates.get(2), null, null, currentTerm, 6, payload6);
        verifyApplyState(applyStates.get(3), null, null, currentTerm, 7, payload7);

        // A "fake" snapshot should've advanced the snapshot index and trimmed the log.
        assertEquals("Snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Snapshot index", 6, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Journal log size", 1, leaderContext.getReplicatedLog().size());
        assertEquals("Journal last index", 7, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 7, leaderContext.getCommitIndex());

        // Ditto with the followers (after another AppendEntries).

        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, AppendEntries.class);
        RaftActorContext follower1Context = follower1Actor.underlyingActor().getRaftActorContext();
        assertEquals("Snapshot term", currentTerm, follower1Context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Snapshot index", 6, follower1Context.getReplicatedLog().getSnapshotIndex());
        assertEquals("Journal log size", 1, follower1Context.getReplicatedLog().size());
        assertEquals("Journal last index", 7, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 7, follower1Context.getCommitIndex());

        MessageCollectorActor.clearMessages(follower2CollectorActor);
        MessageCollectorActor.expectFirstMatching(follower2CollectorActor, AppendEntries.class);
        RaftActorContext follower2Context = follower1Actor.underlyingActor().getRaftActorContext();
        assertEquals("Snapshot term", currentTerm, follower2Context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Snapshot index", 6, follower2Context.getReplicatedLog().getSnapshotIndex());
        assertEquals("Journal log size", 1, follower2Context.getReplicatedLog().size());
        assertEquals("Journal last index", 7, follower2Context.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 7, follower2Context.getCommitIndex());

        // Kill the leader actor

        JavaTestKit testkit = new JavaTestKit(getSystem());
        testkit.watch(leaderActor);

        leaderActor.tell(PoisonPill.getInstance(), null);
        testkit.expectMsgClass(JavaTestKit.duration("5 seconds"), Terminated.class);

        testkit.unwatch(leaderActor);

        // Reinstate the leader actor and verify the recovered log.

        leaderActor = newTestRaftActor(leaderId, peerAddresses, leaderConfigParams);

        leaderActor.underlyingActor().waitForRecoveryComplete();

        assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", 6, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader commit index", 7, leaderContext.getCommitIndex());
        assertEquals("Leader journal log size", 1, leaderContext.getReplicatedLog().size());
        verifyReplicatedLogEntry(leaderContext.getReplicatedLog().last(), currentTerm, 7, payload7);
    }

    private MockRaftActorContext.MockPayload sendPayloadData(ActorRef leaderActor, String data) {
        MockRaftActorContext.MockPayload payload = new MockRaftActorContext.MockPayload(data);
        leaderActor.tell(payload, ActorRef.noSender());
        return payload;
    }

    private void verifyApplyState(ApplyState applyState, ActorRef expClientActor,
            String expId, long expTerm, long expIndex, MockRaftActorContext.MockPayload payload) {
        assertEquals("ApplyState getClientActor", expClientActor, applyState.getClientActor());
        assertEquals("ApplyState getIdentifier", expId, applyState.getIdentifier());
        ReplicatedLogEntry replicatedLogEntry = applyState.getReplicatedLogEntry();
        verifyReplicatedLogEntry(replicatedLogEntry, expTerm, expIndex, payload);
    }

    private void verifyReplicatedLogEntry(ReplicatedLogEntry replicatedLogEntry, long expTerm, long expIndex,
            MockRaftActorContext.MockPayload payload) {
        assertEquals("ReplicatedLogEntry getTerm", expTerm, replicatedLogEntry.getTerm());
        assertEquals("ReplicatedLogEntry getIndex", expIndex, replicatedLogEntry.getIndex());
        assertEquals("ReplicatedLogEntry getData", payload, replicatedLogEntry.getData());
    }
}
