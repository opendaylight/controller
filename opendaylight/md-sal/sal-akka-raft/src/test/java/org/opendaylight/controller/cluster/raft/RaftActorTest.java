/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Terminated;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.persistence.AbstractPersistentActor;
import org.apache.pekko.persistence.SaveSnapshotFailure;
import org.apache.pekko.persistence.SaveSnapshotSuccess;
import org.apache.pekko.persistence.SnapshotMetadata;
import org.apache.pekko.persistence.SnapshotOffer;
import org.apache.pekko.protobuf.ByteString;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.AbstractRaftActorIntegrationTest.TestPersist;
import org.opendaylight.controller.cluster.raft.AbstractRaftActorIntegrationTest.TestRaftActor;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.SnapshotManager.ApplyLeaderSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.LeaderTransitioning;
import org.opendaylight.controller.cluster.raft.base.messages.SwitchBehavior.BecomeFollower;
import org.opendaylight.controller.cluster.raft.base.messages.SwitchBehavior.BecomeLeader;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.spi.ImmutableRaftEntryMeta;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaftActorTest extends AbstractActorTest {
    private static final Logger TEST_LOG = LoggerFactory.getLogger(RaftActorTest.class);
    private static final Duration ONE_DAY = Duration.ofDays(1);

    private TestActorFactory factory;

    @Before
    public void setUp() {
        factory = new TestActorFactory(getSystem());
    }

    @After
    public void tearDown() {
        factory.close();
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    @Test
    public void testConstruction() {
        new RaftActorTestKit(stateDir(), getSystem(), "testConstruction").waitUntilLeader();
    }

    @Test
    public void testFindLeaderWhenLeaderIsSelf() {
        final var kit = new RaftActorTestKit(stateDir(), getSystem(), "testFindLeader");
        kit.waitUntilLeader();
    }

    @Test
    public void testRaftActorRecoveryWithPersistenceEnabled() {
        TEST_LOG.info("testRaftActorRecoveryWithPersistenceEnabled starting");

        TestKit kit = new TestKit(getSystem());
        String persistenceId = factory.generateActorId("follower-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        // Set the heartbeat interval high to essentially disable election otherwise the test
        // may fail if the actor is switched to Leader and the commitIndex is set to the last
        // log entry.
        config.setHeartBeatInterval(ONE_DAY);

        Map<String, String> peerAddresses = Map.of("member1", "address");
        ActorRef followerActor = factory.createActor(MockRaftActor.props(persistenceId, stateDir(),
                peerAddresses, config), persistenceId);

        kit.watch(followerActor);

        List<ReplicatedLogEntry> snapshotUnappliedEntries = List.of(
            new SimpleReplicatedLogEntry(4, 1, new MockRaftActorContext.MockPayload("E")));

        int lastAppliedDuringSnapshotCapture = 3;
        int lastIndexDuringSnapshotCapture = 4;

        // 4 messages as part of snapshot, which are applied to state
        MockSnapshotState snapshotState = new MockSnapshotState(List.of(
                new MockRaftActorContext.MockPayload("A"),
                new MockRaftActorContext.MockPayload("B"),
                new MockRaftActorContext.MockPayload("C"),
                new MockRaftActorContext.MockPayload("D")));

        Snapshot snapshot = Snapshot.create(snapshotState, snapshotUnappliedEntries, lastIndexDuringSnapshotCapture, 1,
                lastAppliedDuringSnapshotCapture, 1, new TermInfo(-1), null);
        InMemorySnapshotStore.addSnapshot(persistenceId, snapshot);

        // add more entries after snapshot is taken
        ReplicatedLogEntry entry2 = new SimpleReplicatedLogEntry(5, 1, new MockRaftActorContext.MockPayload("F", 2));
        ReplicatedLogEntry entry3 = new SimpleReplicatedLogEntry(6, 1, new MockRaftActorContext.MockPayload("G", 3));
        ReplicatedLogEntry entry4 = new SimpleReplicatedLogEntry(7, 1, new MockRaftActorContext.MockPayload("H", 4));

        final int lastAppliedToState = 5;
        final int lastIndex = 7;

        InMemoryJournal.addEntry(persistenceId, 5, entry2);
        // 2 entries are applied to state besides the 4 entries in snapshot
        InMemoryJournal.addEntry(persistenceId, 6, new ApplyJournalEntries(lastAppliedToState));
        InMemoryJournal.addEntry(persistenceId, 7, entry3);
        InMemoryJournal.addEntry(persistenceId, 8, entry4);

        // kill the actor
        followerActor.tell(PoisonPill.getInstance(), null);
        kit.expectMsgClass(Duration.ofSeconds(5), Terminated.class);

        kit.unwatch(followerActor);

        //reinstate the actor
        TestActorRef<MockRaftActor> ref = factory.createTestActor(
                MockRaftActor.props(persistenceId, stateDir(), peerAddresses, config));

        MockRaftActor mockRaftActor = ref.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        final var context = mockRaftActor.getRaftActorContext();
        final var log = context.getReplicatedLog();
        assertEquals("Journal log size", snapshotUnappliedEntries.size() + 3, log.size());
        assertEquals("Journal data size", 10, log.dataSize());
        assertEquals("Last index", lastIndex, log.lastIndex());
        assertEquals("Last applied", lastAppliedToState, log.getLastApplied());
        assertEquals("Commit index", lastAppliedToState, log.getCommitIndex());
        assertEquals("Recovered state size", 6, mockRaftActor.getState().size());

        mockRaftActor.waitForInitializeBehaviorComplete();

        assertEquals("getRaftState", RaftState.Follower, mockRaftActor.getRaftState());

        TEST_LOG.info("testRaftActorRecoveryWithPersistenceEnabled ending");
    }

    @Test
    public void testRaftActorRecoveryWithPersistenceDisabled() {
        String persistenceId = factory.generateActorId("follower-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        config.setHeartBeatInterval(ONE_DAY);

        TestActorRef<MockRaftActor> ref = factory.createTestActor(MockRaftActor.props(persistenceId, stateDir(),
                Map.of("member1", "address"), config, TestDataProvider.INSTANCE), persistenceId);

        MockRaftActor mockRaftActor = ref.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        mockRaftActor.waitForInitializeBehaviorComplete();

        assertEquals("getRaftState", RaftState.Follower, mockRaftActor.getRaftState());
    }

    @Test
    public void testUpdateElectionTermPersistedWithPersistenceDisabled() {
        final TestKit kit = new TestKit(getSystem());
        String persistenceId = factory.generateActorId("follower-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(Duration.ofMillis(100));
        config.setElectionTimeoutFactor(1);

        TestActorRef<MockRaftActor> ref = factory.createTestActor(MockRaftActor.props(persistenceId, stateDir(),
                Map.of("member1", "address"), config, TestDataProvider.INSTANCE)
                .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        ref.underlyingActor().waitForRecoveryComplete();

        assertEquals("UpdateElectionTerm entries", List.of(),
            InMemoryJournal.get(persistenceId, UpdateElectionTerm.class));

        assertTrue(Files.exists(stateDir().resolve(persistenceId).resolve("TermInfo.properties")));

        factory.killActor(ref, kit);

        config.setHeartBeatInterval(ONE_DAY);
        ref = factory.createTestActor(MockRaftActor.props(persistenceId, stateDir(), Map.of("member1", "address"),
                config, TestDataProvider.INSTANCE).withDispatcher(Dispatchers.DefaultDispatcherId()),
                factory.generateActorId("follower-"));

        MockRaftActor actor = ref.underlyingActor();
        actor.waitForRecoveryComplete();

        RaftActorContext newContext = actor.getRaftActorContext();
        assertEquals("electionTerm", new TermInfo(0), newContext.termInfo());

        assertEquals("UpdateElectionTerm entries", List.of(),
            InMemoryJournal.get(persistenceId, UpdateElectionTerm.class));
    }

    @Test
    public void testRaftActorForwardsToRaftActorRecoverySupport() {
        String persistenceId = factory.generateActorId("leader-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        config.setHeartBeatInterval(ONE_DAY);

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
            stateDir(), Map.of(), config), persistenceId);

        MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

        // Wait for akka's recovery to complete so it doesn't interfere.
        mockRaftActor.waitForRecoveryComplete();

        RaftActorRecoverySupport mockSupport = mock(RaftActorRecoverySupport.class);
        mockRaftActor.setRaftActorRecoverySupport(mockSupport);

        Snapshot snapshot = Snapshot.create(ByteState.of(new byte[]{1}), List.of(), 3, 1, 3, 1, new TermInfo(-1), null);
        SnapshotOffer snapshotOffer = new SnapshotOffer(new SnapshotMetadata("test", 6, 12345), snapshot);
        mockRaftActor.handleRecover(snapshotOffer);

        ReplicatedLogEntry logEntry = new SimpleReplicatedLogEntry(1, 1, new MockRaftActorContext.MockPayload("1", 5));
        mockRaftActor.handleRecover(logEntry);

        ApplyJournalEntries applyJournalEntries = new ApplyJournalEntries(2);
        mockRaftActor.handleRecover(applyJournalEntries);

        DeleteEntries deleteEntries = new DeleteEntries(1);
        mockRaftActor.handleRecover(deleteEntries);

        UpdateElectionTerm updateElectionTerm = new UpdateElectionTerm(5, "member2");
        mockRaftActor.handleRecover(updateElectionTerm);

        verify(mockSupport).handleRecoveryMessage(any(AbstractPersistentActor.class), same(snapshotOffer));
        verify(mockSupport).handleRecoveryMessage(any(AbstractPersistentActor.class), same(logEntry));
        verify(mockSupport).handleRecoveryMessage(any(AbstractPersistentActor.class), same(applyJournalEntries));
        verify(mockSupport).handleRecoveryMessage(any(AbstractPersistentActor.class), same(deleteEntries));
        verify(mockSupport).handleRecoveryMessage(any(AbstractPersistentActor.class), same(updateElectionTerm));
    }

    @Test
    public void testRaftActorForwardsToRaftActorSnapshotMessageSupport() {
        String persistenceId = factory.generateActorId("leader-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        config.setHeartBeatInterval(ONE_DAY);

        RaftActorSnapshotMessageSupport mockSupport = mock(RaftActorSnapshotMessageSupport.class);

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId)
            .config(config).snapshotMessageSupport(mockSupport).props(stateDir()));

        MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

        // Wait for akka's recovery to complete so it doesn't interfere.
        mockRaftActor.waitForRecoveryComplete();

        final var applySnapshot = new ApplyLeaderSnapshot(persistenceId, 0, ImmutableRaftEntryMeta.of(0, 0),
            ByteSource.wrap(new byte[1]), null, mock(ApplyLeaderSnapshot.Callback.class));

        when(mockSupport.handleSnapshotMessage(same(applySnapshot))).thenReturn(true);
        mockRaftActor.handleCommand(applySnapshot);

        CaptureSnapshotReply captureSnapshotReply = new CaptureSnapshotReply(ByteState.empty(), null);
        when(mockSupport.handleSnapshotMessage(same(captureSnapshotReply))).thenReturn(true);
        mockRaftActor.handleCommand(captureSnapshotReply);

        SaveSnapshotSuccess saveSnapshotSuccess = new SaveSnapshotSuccess(new SnapshotMetadata("", 0L, 0L));
        when(mockSupport.handleSnapshotMessage(same(saveSnapshotSuccess))).thenReturn(true);
        mockRaftActor.handleCommand(saveSnapshotSuccess);

        SaveSnapshotFailure saveSnapshotFailure = new SaveSnapshotFailure(new SnapshotMetadata("", 0L, 0L),
                new Throwable());
        when(mockSupport.handleSnapshotMessage(same(saveSnapshotFailure))).thenReturn(true);
        mockRaftActor.handleCommand(saveSnapshotFailure);

        when(mockSupport.handleSnapshotMessage(same(SnapshotManager.CommitSnapshot.INSTANCE)))
            .thenReturn(true);
        mockRaftActor.handleCommand(SnapshotManager.CommitSnapshot.INSTANCE);

        when(mockSupport.handleSnapshotMessage(same(GetSnapshot.INSTANCE))).thenReturn(true);
        mockRaftActor.handleCommand(GetSnapshot.INSTANCE);

        verify(mockSupport).handleSnapshotMessage(same(applySnapshot));
        verify(mockSupport).handleSnapshotMessage(same(captureSnapshotReply));
        verify(mockSupport).handleSnapshotMessage(same(saveSnapshotSuccess));
        verify(mockSupport).handleSnapshotMessage(same(saveSnapshotFailure));
        verify(mockSupport).handleSnapshotMessage(same(SnapshotManager.CommitSnapshot.INSTANCE));
        verify(mockSupport).handleSnapshotMessage(same(GetSnapshot.INSTANCE));
    }

    @Test
    public void testApplyJournalEntriesCallsDataPersistence() throws Exception {
        String persistenceId = factory.generateActorId("leader-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        config.setHeartBeatInterval(ONE_DAY);

        DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
            stateDir(), Map.of(), config, dataPersistenceProvider), persistenceId);

        MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

        mockRaftActor.waitForInitializeBehaviorComplete();

        mockRaftActor.waitUntilLeader();

        mockRaftActor.handleCommand(new ApplyJournalEntries(10));

        verify(dataPersistenceProvider).persistAsync(any(ApplyJournalEntries.class), any(Consumer.class));
    }

    @Test
    public void testApplyState() {
        String persistenceId = factory.generateActorId("leader-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        config.setHeartBeatInterval(ONE_DAY);

        DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
            stateDir(), Map.of(), config, dataPersistenceProvider), persistenceId);

        MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

        mockRaftActor.waitForInitializeBehaviorComplete();

        ReplicatedLogEntry entry = new SimpleReplicatedLogEntry(5, 1, new MockRaftActorContext.MockPayload("F"));

        final Identifier id = new MockIdentifier("apply-state");
        mockRaftActor.getRaftActorContext().getApplyStateConsumer().accept(new ApplyState(mockActorRef, id, entry));

        verify(mockRaftActor.actorDelegate).applyState(eq(mockActorRef), eq(id), any());
    }

    @Test
    public void testRaftRoleChangeNotifierWhenRaftActorHasNoPeers() throws Exception {
        ActorRef notifierActor = factory.createActor(MessageCollectorActor.props());
        MessageCollectorActor.waitUntilReady(notifierActor);

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        long heartBeatInterval = 100;
        config.setHeartBeatInterval(Duration.ofMillis(heartBeatInterval));
        config.setElectionTimeoutFactor(20);

        String persistenceId = factory.generateActorId("notifier-");

        final TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder()
                .id(persistenceId).config(config).roleChangeNotifier(notifierActor)
                .dataPersistenceProvider(TestDataProvider.INSTANCE).props(stateDir())
                .withDispatcher(Dispatchers.DefaultDispatcherId()),
                persistenceId);

        final var matches =  MessageCollectorActor.expectMatching(notifierActor, RoleChanged.class, 3);

        // check if the notifier got a role change from null to Follower
        RoleChanged raftRoleChanged = matches.get(0);
        assertEquals(persistenceId, raftRoleChanged.getMemberId());
        assertNull(raftRoleChanged.getOldRole());
        assertEquals(RaftState.Follower.name(), raftRoleChanged.getNewRole());

        // check if the notifier got a role change from Follower to Candidate
        raftRoleChanged = matches.get(1);
        assertEquals(persistenceId, raftRoleChanged.getMemberId());
        assertEquals(RaftState.Follower.name(), raftRoleChanged.getOldRole());
        assertEquals(RaftState.Candidate.name(), raftRoleChanged.getNewRole());

        // check if the notifier got a role change from Candidate to Leader
        raftRoleChanged = matches.get(2);
        assertEquals(persistenceId, raftRoleChanged.getMemberId());
        assertEquals(RaftState.Candidate.name(), raftRoleChanged.getOldRole());
        assertEquals(RaftState.Leader.name(), raftRoleChanged.getNewRole());

        LeaderStateChanged leaderStateChange = MessageCollectorActor.expectFirstMatching(
                notifierActor, LeaderStateChanged.class);

        assertEquals(raftRoleChanged.getMemberId(), leaderStateChange.getLeaderId());
        assertEquals(MockRaftActor.PAYLOAD_VERSION, leaderStateChange.getLeaderPayloadVersion());

        MessageCollectorActor.clearMessages(notifierActor);

        MockRaftActor raftActor = raftActorRef.underlyingActor();
        final String newLeaderId = "new-leader";
        final short newLeaderVersion = 6;
        Follower follower = new Follower(raftActor.getRaftActorContext()) {
            @Override
            public RaftActorBehavior handleMessage(final ActorRef sender, final Object message) {
                setLeaderId(newLeaderId);
                setLeaderPayloadVersion(newLeaderVersion);
                return this;
            }
        };

        raftActor.newBehavior(follower);

        leaderStateChange = MessageCollectorActor.expectFirstMatching(notifierActor, LeaderStateChanged.class);
        assertEquals(persistenceId, leaderStateChange.getMemberId());
        assertEquals(null, leaderStateChange.getLeaderId());

        raftRoleChanged = MessageCollectorActor.expectFirstMatching(notifierActor, RoleChanged.class);
        assertEquals(RaftState.Leader.name(), raftRoleChanged.getOldRole());
        assertEquals(RaftState.Follower.name(), raftRoleChanged.getNewRole());

        MessageCollectorActor.clearMessages(notifierActor);

        raftActor.handleCommand("any");

        leaderStateChange = MessageCollectorActor.expectFirstMatching(notifierActor, LeaderStateChanged.class);
        assertEquals(persistenceId, leaderStateChange.getMemberId());
        assertEquals(newLeaderId, leaderStateChange.getLeaderId());
        assertEquals(newLeaderVersion, leaderStateChange.getLeaderPayloadVersion());

        MessageCollectorActor.clearMessages(notifierActor);

        raftActor.handleCommand("any");

        Uninterruptibles.sleepUninterruptibly(505, TimeUnit.MILLISECONDS);
        leaderStateChange = MessageCollectorActor.getFirstMatching(notifierActor, LeaderStateChanged.class);
        assertNull(leaderStateChange);
    }

    @Test
    public void testRaftRoleChangeNotifierWhenRaftActorHasPeers() throws Exception {
        ActorRef notifierActor = factory.createActor(MessageCollectorActor.props());
        MessageCollectorActor.waitUntilReady(notifierActor);

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        long heartBeatInterval = 100;
        config.setHeartBeatInterval(Duration.ofMillis(heartBeatInterval));
        config.setElectionTimeoutFactor(1);

        String persistenceId = factory.generateActorId("notifier-");

        factory.createActor(MockRaftActor.builder().id(persistenceId).peerAddresses(Map.of("leader", "fake/path"))
                .config(config).roleChangeNotifier(notifierActor).props(stateDir()));

        List<RoleChanged> matches =  null;
        for (int i = 0; i < 5000 / heartBeatInterval; i++) {
            matches = MessageCollectorActor.getAllMatching(notifierActor, RoleChanged.class);
            assertNotNull(matches);
            if (matches.size() == 3) {
                break;
            }
            Uninterruptibles.sleepUninterruptibly(heartBeatInterval, TimeUnit.MILLISECONDS);
        }

        assertNotNull(matches);
        assertEquals(2, matches.size());

        // check if the notifier got a role change from null to Follower
        RoleChanged raftRoleChanged = matches.get(0);
        assertEquals(persistenceId, raftRoleChanged.getMemberId());
        assertNull(raftRoleChanged.getOldRole());
        assertEquals(RaftState.Follower.name(), raftRoleChanged.getNewRole());

        // check if the notifier got a role change from Follower to Candidate
        raftRoleChanged = matches.get(1);
        assertEquals(persistenceId, raftRoleChanged.getMemberId());
        assertEquals(RaftState.Follower.name(), raftRoleChanged.getOldRole());
        assertEquals(RaftState.Candidate.name(), raftRoleChanged.getNewRole());
    }

    @Test
    public void testFakeSnapshotsForLeaderWithInRealSnapshots() throws Exception {
        final String persistenceId = factory.generateActorId("leader-");
        final String follower1Id = factory.generateActorId("follower-");

        ActorRef followerActor1 = factory.createActor(MessageCollectorActor.props());

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);

        DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

        Map<String, String> peerAddresses = Map.of(follower1Id, followerActor1.path().toString());

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                MockRaftActor.props(persistenceId, stateDir(), peerAddresses, config, dataPersistenceProvider),
                persistenceId);

        MockRaftActor leaderActor = mockActorRef.underlyingActor();

        final var leaderContext = leaderActor.getRaftActorContext();
        final var leaderLog = leaderContext.getReplicatedLog();
        leaderLog.setCommitIndex(4);
        leaderLog.setLastApplied(4);
        leaderContext.setTermInfo(new TermInfo(1, persistenceId));

        leaderActor.waitForInitializeBehaviorComplete();

        // create 8 entries in the log - 0 to 4 are applied and will get picked up as part of the capture snapshot

        Leader leader = new Leader(leaderActor.getRaftActorContext());
        leaderActor.setCurrentBehavior(leader);
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        MockRaftActorContext.MockReplicatedLogBuilder logBuilder = new MockRaftActorContext.MockReplicatedLogBuilder();
        leaderActor.getRaftActorContext().setReplicatedLog(logBuilder.createEntries(0, 8, 1).build());

        assertEquals(8, leaderActor.getReplicatedLog().size());

        leaderActor.getRaftActorContext().getSnapshotManager().captureToInstall(ImmutableRaftEntryMeta.of(6, 1), 4,
            "xyzzy");
        verify(leaderActor.snapshotCohortDelegate).createSnapshot(any(), any());

        assertEquals(8, leaderActor.getReplicatedLog().size());

        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());
        //fake snapshot on index 5
        leaderActor.handleCommand(new AppendEntriesReply(follower1Id, 1, true, 5, 1, (short)0));

        assertEquals(8, leaderActor.getReplicatedLog().size());

        //fake snapshot on index 6
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());
        leaderActor.handleCommand(new AppendEntriesReply(follower1Id, 1, true, 6, 1, (short)0));
        assertEquals(8, leaderActor.getReplicatedLog().size());

        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        assertEquals(8, leaderActor.getReplicatedLog().size());

        MockSnapshotState snapshotState = new MockSnapshotState(List.of(
                new MockRaftActorContext.MockPayload("foo-0"),
                new MockRaftActorContext.MockPayload("foo-1"),
                new MockRaftActorContext.MockPayload("foo-2"),
                new MockRaftActorContext.MockPayload("foo-3"),
                new MockRaftActorContext.MockPayload("foo-4")));

        leaderActor.getRaftActorContext().getSnapshotManager().persist(snapshotState, null);

        assertTrue(leaderActor.getRaftActorContext().getSnapshotManager().isCapturing());

        // The commit is needed to complete the snapshot creation process
        leaderActor.getRaftActorContext().getSnapshotManager().commit(-1, -1);

        // capture snapshot reply should remove the snapshotted entries only
        assertEquals(3, leaderActor.getReplicatedLog().size());
        assertEquals(7, leaderActor.getReplicatedLog().lastIndex());

        // add another non-replicated entry
        leaderActor.getReplicatedLog().append(
                new SimpleReplicatedLogEntry(8, 1, new MockRaftActorContext.MockPayload("foo-8")));

        //fake snapshot on index 7, since lastApplied = 7 , we would keep the last applied
        leaderActor.handleCommand(new AppendEntriesReply(follower1Id, 1, true, 7, 1, (short)0));
        assertEquals(2, leaderActor.getReplicatedLog().size());
        assertEquals(8, leaderActor.getReplicatedLog().lastIndex());
    }

    @Test
    public void testFakeSnapshotsForFollowerWithInRealSnapshots() throws Exception {
        final String persistenceId = factory.generateActorId("follower-");
        final String leaderId = factory.generateActorId("leader-");

        ActorRef leaderActor1 = factory.createActor(MessageCollectorActor.props());

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);

        DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

        Map<String, String> peerAddresses = Map.of(leaderId, leaderActor1.path().toString());

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
            MockRaftActor.props(persistenceId, stateDir(), peerAddresses, config, dataPersistenceProvider),
            persistenceId);

        final var followerActor = mockActorRef.underlyingActor();
        final var followerContext = followerActor.getRaftActorContext();
        final var followerLog = followerContext.getReplicatedLog();
        followerLog.setCommitIndex(4);
        followerLog.setLastApplied(4);
        followerContext.setTermInfo(new TermInfo(1, persistenceId));

        followerActor.waitForInitializeBehaviorComplete();

        Follower follower = new Follower(followerActor.getRaftActorContext());
        followerActor.setCurrentBehavior(follower);
        assertEquals(RaftState.Follower, followerActor.getCurrentBehavior().state());

        // create 6 entries in the log - 0 to 4 are applied and will get picked up as part of the capture snapshot
        MockRaftActorContext.MockReplicatedLogBuilder logBuilder = new MockRaftActorContext.MockReplicatedLogBuilder();
        followerActor.getRaftActorContext().setReplicatedLog(logBuilder.createEntries(0, 6, 1).build());

        // log has indices 0-5
        assertEquals(6, followerActor.getReplicatedLog().size());

        //snapshot on 4
        followerActor.getRaftActorContext().getSnapshotManager().captureToInstall(ImmutableRaftEntryMeta.of(5, 1), 4,
            "xyzzy");
        verify(followerActor.snapshotCohortDelegate).createSnapshot(any(), any());

        assertEquals(6, followerActor.getReplicatedLog().size());

        //fake snapshot on index 6
        List<ReplicatedLogEntry> entries = List.of(
                new SimpleReplicatedLogEntry(6, 1, new MockRaftActorContext.MockPayload("foo-6")));
        followerActor.handleCommand(new AppendEntries(1, leaderId, 5, 1, entries, 5, 5, (short)0));
        assertEquals(7, followerActor.getReplicatedLog().size());

        //fake snapshot on index 7
        assertEquals(RaftState.Follower, followerActor.getCurrentBehavior().state());

        entries = List.of(new SimpleReplicatedLogEntry(7, 1,
                new MockRaftActorContext.MockPayload("foo-7")));
        followerActor.handleCommand(new AppendEntries(1, leaderId, 6, 1, entries, 6, 6, (short) 0));
        assertEquals(8, followerActor.getReplicatedLog().size());

        assertEquals(RaftState.Follower, followerActor.getCurrentBehavior().state());


        ByteString snapshotBytes = fromObject(List.of(
                new MockRaftActorContext.MockPayload("foo-0"),
                new MockRaftActorContext.MockPayload("foo-1"),
                new MockRaftActorContext.MockPayload("foo-2"),
                new MockRaftActorContext.MockPayload("foo-3"),
                new MockRaftActorContext.MockPayload("foo-4")));
        followerActor.handleCommand(new CaptureSnapshotReply(ByteState.of(snapshotBytes.toByteArray()), null));
        assertTrue(followerActor.getRaftActorContext().getSnapshotManager().isCapturing());

        // The commit is needed to complete the snapshot creation process
        followerActor.getRaftActorContext().getSnapshotManager().commit(-1, -1);

        // capture snapshot reply should remove the snapshotted entries only till replicatedToAllIndex
        assertEquals(3, followerActor.getReplicatedLog().size()); //indexes 5,6,7 left in the log
        assertEquals(7, followerActor.getReplicatedLog().lastIndex());

        entries = List.of(new SimpleReplicatedLogEntry(8, 1, new MockRaftActorContext.MockPayload("foo-7")));
        // send an additional entry 8 with leaderCommit = 7
        followerActor.handleCommand(new AppendEntries(1, leaderId, 7, 1, entries, 7, 7, (short) 0));

        // 7 and 8, as lastapplied is 7
        assertEquals(2, followerActor.getReplicatedLog().size());
    }

    @Test
    public void testFakeSnapshotsForLeaderWithInInitiateSnapshots() throws Exception {
        final String persistenceId = factory.generateActorId("leader-");
        final String follower1Id = factory.generateActorId("follower-");
        final String follower2Id = factory.generateActorId("follower-");

        final ActorRef followerActor1 = factory.createActor(MessageCollectorActor.props(), follower1Id);
        final ActorRef followerActor2 = factory.createActor(MessageCollectorActor.props(), follower2Id);

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);

        DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

        Map<String, String> peerAddresses = Map.of(
            follower1Id, followerActor1.path().toString(),
            follower2Id, followerActor2.path().toString());

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                MockRaftActor.props(persistenceId, stateDir(), peerAddresses, config, dataPersistenceProvider),
                persistenceId);

        final var leaderActor = mockActorRef.underlyingActor();
        final var leaderContext = leaderActor.getRaftActorContext();
        final var leaderLog = leaderContext.getReplicatedLog();
        leaderLog.setCommitIndex(9);
        leaderLog.setLastApplied(9);
        leaderContext.setTermInfo(new TermInfo(1, persistenceId));

        leaderActor.waitForInitializeBehaviorComplete();

        Leader leader = new Leader(leaderActor.getRaftActorContext());
        leaderActor.setCurrentBehavior(leader);
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        // create 5 entries in the log
        MockRaftActorContext.MockReplicatedLogBuilder logBuilder = new MockRaftActorContext.MockReplicatedLogBuilder();
        leaderActor.getRaftActorContext().setReplicatedLog(logBuilder.createEntries(5, 10, 1).build());

        //set the snapshot index to 4 , 0 to 4 are snapshotted
        leaderActor.getRaftActorContext().getReplicatedLog().setSnapshotIndex(4);
        //setting replicatedToAllIndex = 9, for the log to clear
        leader.setReplicatedToAllIndex(9);
        assertEquals(5, leaderActor.getReplicatedLog().size());
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        leaderActor.handleCommand(new AppendEntriesReply(follower1Id, 1, true, 9, 1, (short) 0));
        assertEquals(5, leaderActor.getReplicatedLog().size());
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        // set the 2nd follower nextIndex to 1 which has been snapshotted
        leaderActor.handleCommand(new AppendEntriesReply(follower2Id, 1, true, 0, 1, (short)0));
        assertEquals(5, leaderActor.getReplicatedLog().size());
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        // simulate a real snapshot
        leaderActor.handleCommand(SendHeartBeat.INSTANCE);
        assertEquals(5, leaderActor.getReplicatedLog().size());
        assertEquals(String.format("expected to be Leader but was %s. Current Leader = %s ",
                leaderActor.getCurrentBehavior().state(), leaderActor.getLeaderId()),
                RaftState.Leader, leaderActor.getCurrentBehavior().state());


        //reply from a slow follower does not initiate a fake snapshot
        leaderActor.handleCommand(new AppendEntriesReply(follower2Id, 1, true, 9, 1, (short)0));
        assertEquals("Fake snapshot should not happen when Initiate is in progress", 5,
                leaderActor.getReplicatedLog().size());

        ByteString snapshotBytes = fromObject(List.of(
                new MockRaftActorContext.MockPayload("foo-0"),
                new MockRaftActorContext.MockPayload("foo-1"),
                new MockRaftActorContext.MockPayload("foo-2"),
                new MockRaftActorContext.MockPayload("foo-3"),
                new MockRaftActorContext.MockPayload("foo-4")));
        leaderActor.handleCommand(new CaptureSnapshotReply(ByteState.of(snapshotBytes.toByteArray()), null));
        assertTrue(leaderActor.getRaftActorContext().getSnapshotManager().isCapturing());

        assertEquals("Real snapshot didn't clear the log till replicatedToAllIndex", 0,
                leaderActor.getReplicatedLog().size());

        //reply from a slow follower after should not raise errors
        leaderActor.handleCommand(new AppendEntriesReply(follower2Id, 1, true, 5, 1, (short) 0));
        assertEquals(0, leaderActor.getReplicatedLog().size());
    }

    @Test
    public void testRealSnapshotWhenReplicatedToAllIndexMinusOne() throws Exception {
        String persistenceId = factory.generateActorId("leader-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);
        config.setSnapshotBatchCount(5);

        final var peerAddresses = Map.of("member1", "address");

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                MockRaftActor.props(persistenceId, stateDir(), peerAddresses, config, TestDataProvider.INSTANCE),
                persistenceId);

        MockRaftActor leaderActor = mockActorRef.underlyingActor();
        final var leaderContext = leaderActor.getRaftActorContext();
        final var leaderLog = leaderContext.getReplicatedLog();
        leaderLog.setCommitIndex(3);
        leaderLog.setLastApplied(3);
        leaderActor.getRaftActorContext().setTermInfo(new TermInfo(1, persistenceId));

        leaderActor.waitForInitializeBehaviorComplete();
        for (int i = 0; i < 4; i++) {
            leaderLog.append(new SimpleReplicatedLogEntry(i, 1, new MockRaftActorContext.MockPayload("A")));
        }

        final var leader = new Leader(leaderActor.getRaftActorContext());
        leaderActor.setCurrentBehavior(leader);
        assertSame(leader, leaderActor.getCurrentBehavior());

        // Simulate an install snaphost to a follower.
        leaderActor.getRaftActorContext().getSnapshotManager().captureToInstall(
                leaderActor.getReplicatedLog().lastMeta(), -1, "member1");

        // Now send a CaptureSnapshotReply
        mockActorRef.tell(new CaptureSnapshotReply(ByteState.of(fromObject("foo").toByteArray()), null), mockActorRef);

        // Trimming log in this scenario is a no-op
        assertEquals(-1, leaderLog.getSnapshotIndex());
        assertTrue(leaderActor.getRaftActorContext().getSnapshotManager().isCapturing());
        assertEquals(-1, leader.getReplicatedToAllIndex());
    }

    @Test
    public void testRealSnapshotWhenReplicatedToAllIndexNotInReplicatedLog() throws Exception {
        String persistenceId = factory.generateActorId("leader-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);
        config.setSnapshotBatchCount(5);

        final var peerAddresses = Map.of("member1", "address");

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                MockRaftActor.props(persistenceId, stateDir(), peerAddresses, config, TestDataProvider.INSTANCE),
                persistenceId);

        MockRaftActor leaderActor = mockActorRef.underlyingActor();
        final var leaderContext = leaderActor.getRaftActorContext();
        final var leaderLog = leaderContext.getReplicatedLog();
        leaderLog.setCommitIndex(3);
        leaderLog.setLastApplied(3);
        leaderContext.setTermInfo(new TermInfo(1, persistenceId));
        leaderLog.setSnapshotIndex(3);

        leaderActor.waitForInitializeBehaviorComplete();
        Leader leader = new Leader(leaderActor.getRaftActorContext());
        leaderActor.setCurrentBehavior(leader);
        leader.setReplicatedToAllIndex(3);
        assertInstanceOf(Leader.class, leaderActor.getCurrentBehavior());

        // Persist another entry, this will cause a CaptureSnapshot to be triggered
        doReturn(new MockSnapshotState(List.of())).when(leaderActor.snapshotCohortDelegate).takeSnapshot();
        leaderActor.persistData(mockActorRef, new MockIdentifier("x"),
                new MockRaftActorContext.MockPayload("duh"), false);
        verify(leaderActor.snapshotCohortDelegate).takeSnapshot();

        // Trimming log in this scenario is a no-op
        assertEquals(3, leaderLog.getSnapshotIndex());
        assertTrue(leaderContext.getSnapshotManager().isCapturing());
        assertEquals(3, leader.getReplicatedToAllIndex());
    }

    @Test
    public void testSwitchBehavior() {
        String persistenceId = factory.generateActorId("leader-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);
        config.setSnapshotBatchCount(5);

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                MockRaftActor.props(persistenceId, stateDir(), Map.of(), config, TestDataProvider.INSTANCE),
                persistenceId);

        MockRaftActor leaderActor = mockActorRef.underlyingActor();

        leaderActor.waitForRecoveryComplete();

        leaderActor.handleCommand(new BecomeFollower(100));

        assertEquals(100, leaderActor.getRaftActorContext().currentTerm());
        assertInstanceOf(Follower.class, leaderActor.getCurrentBehavior());

        leaderActor.handleCommand(new BecomeLeader(110));

        assertEquals(110, leaderActor.getRaftActorContext().currentTerm());
        assertInstanceOf(Leader.class, leaderActor.getCurrentBehavior());
    }

    public static ByteString fromObject(final Object snapshot) throws Exception {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream os = null;
        try {
            bos = new ByteArrayOutputStream();
            os = new ObjectOutputStream(bos);
            os.writeObject(snapshot);
            byte[] snapshotBytes = bos.toByteArray();
            return ByteString.copyFrom(snapshotBytes);
        } finally {
            if (os != null) {
                os.flush();
                os.close();
            }
            if (bos != null) {
                bos.close();
            }
        }
    }

    @Test
    public void testUpdateConfigParam() {
        final var emptyConfig = new DefaultConfigParamsImpl();
        final var persistenceId = factory.generateActorId("follower-");
        final var peerAddresses = Map.of("member1", "address");
        final var dataPersistenceProvider = mock(DataPersistenceProvider.class);

        TestActorRef<MockRaftActor> actorRef = factory.createTestActor(
                MockRaftActor.props(persistenceId, stateDir(), peerAddresses, emptyConfig, dataPersistenceProvider),
                persistenceId);
        MockRaftActor mockRaftActor = actorRef.underlyingActor();
        mockRaftActor.waitForInitializeBehaviorComplete();

        var behavior = assertInstanceOf(Follower.class, mockRaftActor.getCurrentBehavior());
        mockRaftActor.updateConfigParams(emptyConfig);
        assertSame("Same Behavior", behavior, mockRaftActor.getCurrentBehavior());

        final var disableConfig = new DefaultConfigParamsImpl();
        disableConfig.setCustomRaftPolicyImplementationClass(
            "org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy");
        mockRaftActor.updateConfigParams(disableConfig);
        assertNotSame("Different Behavior", behavior, mockRaftActor.getCurrentBehavior());
        behavior = assertInstanceOf(Follower.class, mockRaftActor.getCurrentBehavior());

        mockRaftActor.updateConfigParams(disableConfig);
        assertSame("Same Behavior", behavior, mockRaftActor.getCurrentBehavior());

        final var defaultConfig = new DefaultConfigParamsImpl();
        defaultConfig.setCustomRaftPolicyImplementationClass(
            "org.opendaylight.controller.cluster.raft.policy.DefaultRaftPolicy");
        mockRaftActor.updateConfigParams(defaultConfig);
        assertNotSame("Different Behavior", behavior, mockRaftActor.getCurrentBehavior());
        behavior = assertInstanceOf(Follower.class, mockRaftActor.getCurrentBehavior());

        mockRaftActor.updateConfigParams(defaultConfig);
        assertSame("Same Behavior", behavior, mockRaftActor.getCurrentBehavior());
    }

    @Test
    public void testGetSnapshot() {
        TEST_LOG.info("testGetSnapshot starting");

        final TestKit kit = new TestKit(getSystem());

        String persistenceId = factory.generateActorId("test-actor-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        long term = 3;
        long seqN = 1;
        InMemoryJournal.addEntry(persistenceId, seqN++, new UpdateElectionTerm(term, "member-1"));
        InMemoryJournal.addEntry(persistenceId, seqN++, new SimpleReplicatedLogEntry(0, term,
                new MockRaftActorContext.MockPayload("A")));
        InMemoryJournal.addEntry(persistenceId, seqN++, new SimpleReplicatedLogEntry(1, term,
                new MockRaftActorContext.MockPayload("B")));
        InMemoryJournal.addEntry(persistenceId, seqN++, new ApplyJournalEntries(1));
        InMemoryJournal.addEntry(persistenceId, seqN++, new SimpleReplicatedLogEntry(2, term,
                new MockRaftActorContext.MockPayload("C")));

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
            stateDir(), Map.of("member1", "address"), config)
                    .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        final var stateSnapshot = new MockSnapshotState(List.of(new byte[] { 1, 2, 3 }));
        mockRaftActor.snapshotCohortDelegate = mock(MockRaftActorSnapshotCohort.class);
        doReturn(stateSnapshot).when(mockRaftActor.snapshotCohortDelegate).takeSnapshot();

        raftActorRef.tell(GetSnapshot.INSTANCE, kit.getRef());

        verify(mockRaftActor.snapshotCohortDelegate, timeout(5000)).takeSnapshot();

        GetSnapshotReply reply = kit.expectMsgClass(GetSnapshotReply.class);

        assertEquals("getId", persistenceId, reply.id());
        var replySnapshot = reply.snapshot();
        assertEquals("getElectionTerm", new TermInfo(term, "member-1"), replySnapshot.termInfo());
        assertEquals("getLastAppliedIndex", 1L, replySnapshot.getLastAppliedIndex());
        assertEquals("getLastAppliedTerm", term, replySnapshot.getLastAppliedTerm());
        assertEquals("getLastIndex", 2L, replySnapshot.getLastIndex());
        assertEquals("getLastTerm", term, replySnapshot.getLastTerm());
        assertSame("getState", stateSnapshot, replySnapshot.getState());
        assertEquals("getUnAppliedEntries size", 1, replySnapshot.getUnAppliedEntries().size());
        assertEquals("UnApplied entry index ", 2L, replySnapshot.getUnAppliedEntries().get(0).index());

        // Test with persistence disabled.
        mockRaftActor.setPersistence(false);
        reset(mockRaftActor.snapshotCohortDelegate);

        raftActorRef.tell(GetSnapshot.INSTANCE, kit.getRef());
        reply = kit.expectMsgClass(GetSnapshotReply.class);
        verify(mockRaftActor.snapshotCohortDelegate, never()).createSnapshot(any(), any());

        assertEquals("getId", persistenceId, reply.id());
        replySnapshot = reply.snapshot();
        assertEquals("getElectionTerm", new TermInfo(term, "member-1"), replySnapshot.termInfo());
        assertEquals("getLastAppliedIndex", -1L, replySnapshot.getLastAppliedIndex());
        assertEquals("getLastAppliedTerm", -1L, replySnapshot.getLastAppliedTerm());
        assertEquals("getLastIndex", -1L, replySnapshot.getLastIndex());
        assertEquals("getLastTerm", -1L, replySnapshot.getLastTerm());
        assertEquals("getState type", EmptyState.INSTANCE, replySnapshot.getState());
        assertEquals("getUnAppliedEntries size", 0, replySnapshot.getUnAppliedEntries().size());

        TEST_LOG.info("testGetSnapshot ending");
    }

    @Test
    public void testRestoreFromSnapshot() {
        TEST_LOG.info("testRestoreFromSnapshot starting");

        String persistenceId = factory.generateActorId("test-actor-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        List<ReplicatedLogEntry> snapshotUnappliedEntries = List.of(
            new SimpleReplicatedLogEntry(4, 1, new MockRaftActorContext.MockPayload("E")));

        int snapshotLastApplied = 3;
        int snapshotLastIndex = 4;

        MockSnapshotState snapshotState = new MockSnapshotState(List.of(
                new MockRaftActorContext.MockPayload("A"),
                new MockRaftActorContext.MockPayload("B"),
                new MockRaftActorContext.MockPayload("C"),
                new MockRaftActorContext.MockPayload("D")));

        Snapshot snapshot = Snapshot.create(snapshotState, snapshotUnappliedEntries,
                snapshotLastIndex, 1, snapshotLastApplied, 1, new TermInfo(1, "member-1"), null);

        InMemorySnapshotStore.addSnapshotSavedLatch(persistenceId);

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId)
            .config(config).restoreFromSnapshot(snapshot).props(stateDir())
            .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        Snapshot savedSnapshot = InMemorySnapshotStore.waitForSavedSnapshot(persistenceId, Snapshot.class);
        assertEquals("getElectionTerm", snapshot.termInfo(), savedSnapshot.termInfo());
        assertEquals("getLastAppliedIndex", snapshot.getLastAppliedIndex(), savedSnapshot.getLastAppliedIndex());
        assertEquals("getLastAppliedTerm", snapshot.getLastAppliedTerm(), savedSnapshot.getLastAppliedTerm());
        assertEquals("getLastIndex", snapshot.getLastIndex(), savedSnapshot.getLastIndex());
        assertEquals("getLastTerm", snapshot.getLastTerm(), savedSnapshot.getLastTerm());
        assertEquals("getState", snapshot.getState(), savedSnapshot.getState());
        assertEquals("getUnAppliedEntries", snapshot.getUnAppliedEntries(), savedSnapshot.getUnAppliedEntries());

        verify(mockRaftActor.snapshotCohortDelegate, timeout(5000)).applySnapshot(any());

        var context = mockRaftActor.getRaftActorContext();
        final var log = context.getReplicatedLog();
        assertEquals("Journal log size", 1, log.size());
        assertEquals("Last index", snapshotLastIndex, log.lastIndex());
        assertEquals("Last applied", snapshotLastApplied, log.getLastApplied());
        assertEquals("Commit index", snapshotLastApplied, log.getCommitIndex());
        assertEquals("Recovered state", snapshotState.state(), mockRaftActor.getState());
        assertEquals("Current term", new TermInfo(1, "member-1"), context.termInfo());

        // Test with data persistence disabled

        snapshot = Snapshot.create(EmptyState.INSTANCE, List.of(), -1, -1, -1, -1, new TermInfo(5, "member-1"), null);

        persistenceId = factory.generateActorId("test-actor-");

        raftActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId).config(config)
            .restoreFromSnapshot(snapshot).persistent(Optional.of(Boolean.FALSE))
            .props(stateDir())
            .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();
        assertTrue("snapshot committed",
                Uninterruptibles.awaitUninterruptibly(mockRaftActor.snapshotCommitted, 5, TimeUnit.SECONDS));

        context = mockRaftActor.getRaftActorContext();
        assertEquals("Current term", new TermInfo(5, "member-1"), context.termInfo());

        TEST_LOG.info("testRestoreFromSnapshot ending");
    }

    @Test
    public void testRestoreFromSnapshotWithRecoveredData() throws Exception {
        TEST_LOG.info("testRestoreFromSnapshotWithRecoveredData starting");

        String persistenceId = factory.generateActorId("test-actor-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        final var state = List.of(new MockRaftActorContext.MockPayload("A"));
        Snapshot snapshot = Snapshot.create(ByteState.of(fromObject(state).toByteArray()),
                List.of(), 5, 2, 5, 2, new TermInfo(2, "member-1"), null);

        InMemoryJournal.addEntry(persistenceId, 1, new SimpleReplicatedLogEntry(0, 1,
                new MockRaftActorContext.MockPayload("B")));

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId)
            .config(config).restoreFromSnapshot(snapshot).props(stateDir())
                    .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verify(mockRaftActor.snapshotCohortDelegate, never()).applySnapshot(any());

        RaftActorContext context = mockRaftActor.getRaftActorContext();
        final var log = context.getReplicatedLog();
        assertEquals("Journal log size", 1, log.size());
        assertEquals("Last index", 0, log.lastIndex());
        assertEquals("Last applied", -1, log.getLastApplied());
        assertEquals("Commit index", -1, log.getCommitIndex());
        assertEquals("Current term", TermInfo.INITIAL, context.termInfo());

        TEST_LOG.info("testRestoreFromSnapshotWithRecoveredData ending");
    }

    @Test
    public void testNonVotingOnRecovery() {
        TEST_LOG.info("testNonVotingOnRecovery starting");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setElectionTimeoutFactor(1);
        config.setHeartBeatInterval(Duration.ofMillis(1));

        String persistenceId = factory.generateActorId("test-actor-");
        InMemoryJournal.addEntry(persistenceId, 1,  new SimpleReplicatedLogEntry(0, 1,
                new ClusterConfig(new ServerInfo(persistenceId, false))));

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId)
            .config(config).props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForInitializeBehaviorComplete();

        // Sleep a bit and verify it didn't get an election timeout and schedule an election.

        Uninterruptibles.sleepUninterruptibly(400, TimeUnit.MILLISECONDS);
        assertEquals("getRaftState", RaftState.Follower, mockRaftActor.getRaftState());

        TEST_LOG.info("testNonVotingOnRecovery ending");
    }

    @Test
    public void testLeaderTransitioning() {
        TEST_LOG.info("testLeaderTransitioning starting");

        ActorRef notifierActor = factory.createActor(MessageCollectorActor.props());

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        String persistenceId = factory.generateActorId("test-actor-");

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId)
            .config(config).roleChangeNotifier(notifierActor).props(stateDir())
            .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForInitializeBehaviorComplete();

        raftActorRef.tell(new AppendEntries(1L, "leader", 0L, 1L, List.of(),
                0L, -1L, (short)1), ActorRef.noSender());
        LeaderStateChanged leaderStateChange = MessageCollectorActor.expectFirstMatching(
                notifierActor, LeaderStateChanged.class);
        assertEquals("getLeaderId", "leader", leaderStateChange.getLeaderId());

        MessageCollectorActor.clearMessages(notifierActor);

        raftActorRef.tell(new LeaderTransitioning("leader"), ActorRef.noSender());

        leaderStateChange = MessageCollectorActor.expectFirstMatching(notifierActor, LeaderStateChanged.class);
        assertEquals("getMemberId", persistenceId, leaderStateChange.getMemberId());
        assertEquals("getLeaderId", null, leaderStateChange.getLeaderId());

        TEST_LOG.info("testLeaderTransitioning ending");
    }

    @Test
    public void testReplicateWithPersistencePending() throws Exception {
        final String leaderId = factory.generateActorId("leader-");
        final String followerId = factory.generateActorId("follower-");

        final ActorRef followerActor = factory.createActor(MessageCollectorActor.props());

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);

        DataPersistenceProvider mockPersistenceProvider = mock(DataPersistenceProvider.class);
        doReturn(true).when(mockPersistenceProvider).isRecoveryApplicable();

        TestActorRef<MockRaftActor> leaderActorRef = factory.createTestActor(
                MockRaftActor.props(leaderId, stateDir(), Map.of(followerId, followerActor.path().toString()), config,
                        mockPersistenceProvider), leaderId);
        MockRaftActor leaderActor = leaderActorRef.underlyingActor();
        leaderActor.waitForInitializeBehaviorComplete();

        final var leaderContext = leaderActor.getRaftActorContext();
        leaderContext.setTermInfo(new TermInfo(1, leaderId));

        Leader leader = new Leader(leaderActor.getRaftActorContext());
        leaderActor.setCurrentBehavior(leader);

        leaderActor.persistData(leaderActorRef, new MockIdentifier("1"), new MockRaftActorContext.MockPayload("1"),
                false);

        final var leaderLog = leaderActor.getReplicatedLog();
        final var logEntry = leaderLog.get(0);
        assertNotNull("ReplicatedLogEntry not found", logEntry);
        assertTrue("isPersistencePending", logEntry.isPersistencePending());
        assertEquals("getCommitIndex", -1, leaderLog.getCommitIndex());

        leaderActor.handleCommand(new AppendEntriesReply(followerId, 1, true, 0, 1, (short)0));
        assertEquals("getCommitIndex", -1, leaderLog.getCommitIndex());

        ArgumentCaptor<Consumer<Object>> callbackCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(mockPersistenceProvider).persistAsync(eq(logEntry), callbackCaptor.capture());

        callbackCaptor.getValue().accept(logEntry);

        assertEquals("getCommitIndex", 0, leaderLog.getCommitIndex());
        assertEquals("getLastApplied", 0, leaderLog.getLastApplied());
    }

    @Test
    public void testReplicateWithBatchHint() throws Exception {
        final String leaderId = factory.generateActorId("leader-");
        final String followerId = factory.generateActorId("follower-");

        final ActorRef followerActor = factory.createActor(MessageCollectorActor.props());

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);

        TestActorRef<MockRaftActor> leaderActorRef = factory.createTestActor(
                MockRaftActor.props(leaderId, stateDir(), Map.of(followerId, followerActor.path().toString()), config),
                    leaderId);
        MockRaftActor leaderActor = leaderActorRef.underlyingActor();
        leaderActor.waitForInitializeBehaviorComplete();

        leaderActor.getRaftActorContext().setTermInfo(new TermInfo(1, leaderId));

        Leader leader = new Leader(leaderActor.getRaftActorContext());
        leaderActor.setCurrentBehavior(leader);

        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        MessageCollectorActor.clearMessages(followerActor);

        leaderActor.handleCommand(new AppendEntriesReply(followerId, 1, true, -1, -1, (short)0));

        leaderActor.persistData(leaderActorRef, new MockIdentifier("1"), new MockPayload("1"), true);
        MessageCollectorActor.assertNoneMatching(followerActor, AppendEntries.class, 500);

        leaderActor.persistData(leaderActorRef, new MockIdentifier("2"), new MockPayload("2"), true);
        MessageCollectorActor.assertNoneMatching(followerActor, AppendEntries.class, 500);

        leaderActor.persistData(leaderActorRef, new MockIdentifier("3"), new MockPayload("3"), false);
        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("AppendEntries size", 3, appendEntries.getEntries().size());
    }

    @Test
    @SuppressWarnings("checkstyle:illegalcatch")
    public void testApplyStateRace() throws Exception {
        final String leaderId = factory.generateActorId("leader-");
        final String followerId = factory.generateActorId("follower-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setIsolatedLeaderCheckInterval(ONE_DAY);
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        ActorRef mockFollowerActorRef = factory.createActor(MessageCollectorActor.props());

        TestRaftActor.Builder builder = TestRaftActor.newBuilder()
                .id(leaderId)
                .peerAddresses(Map.of(followerId, mockFollowerActorRef.path().toString()))
                .config(config)
                .collectorActor(factory.createActor(
                        MessageCollectorActor.props(), factory.generateActorId(leaderId + "-collector")));

        TestActorRef<MockRaftActor> leaderActorRef = factory.createTestActor(builder.props(stateDir()), leaderId);
        MockRaftActor leaderActor = leaderActorRef.underlyingActor();
        leaderActor.waitForInitializeBehaviorComplete();

        leaderActor.getRaftActorContext().setTermInfo(new TermInfo(1, leaderId));
        Leader leader = new Leader(leaderActor.getRaftActorContext());
        leaderActor.setCurrentBehavior(leader);

        final var executorService = Executors.newSingleThreadExecutor();

        leaderActor.setPersistence(new PersistentDataProvider(leaderActor) {
            @Override
            public <T> void persistAsync(final T entry, final Consumer<T> procedure) {
                // needs to be executed from another thread to simulate the persistence actor calling this callback
                executorService.submit(() -> procedure.accept(entry), "persistence-callback");
            }
        });

        leader.getFollower(followerId).setNextIndex(0);
        leader.getFollower(followerId).setMatchIndex(-1);

        // hitting this is flimsy so run multiple times to improve the chance of things
        // blowing up while breaking actor containment
        final TestPersist message =
                new TestPersist(leaderActorRef, new MockIdentifier("1"), new MockPayload("1"));
        for (int i = 0; i < 100; i++) {
            leaderActorRef.tell(message, null);

            AppendEntriesReply reply =
                    new AppendEntriesReply(followerId, 1, true, i, 1, (short) 5);
            leaderActorRef.tell(reply, mockFollowerActorRef);
        }

        await("Persistence callback.").atMost(5, TimeUnit.SECONDS).until(() -> leaderActor.getState().size() == 100);
        executorService.shutdown();
    }
}
