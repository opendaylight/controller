/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Status.Failure;
import akka.actor.Terminated;
import akka.dispatch.Dispatchers;
import akka.japi.Procedure;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotOffer;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.NonPersistentDataProvider;
import org.opendaylight.controller.cluster.PersistentDataProvider;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.MockRaftActor.MockSnapshotState;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.LeaderTransitioning;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.base.messages.SwitchBehavior;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class RaftActorTest extends AbstractActorTest {

    static final Logger TEST_LOG = LoggerFactory.getLogger(RaftActorTest.class);

    private TestActorFactory factory;

    @Before
    public void setUp() {
        factory = new TestActorFactory(getSystem());
    }

    @After
    public void tearDown() throws Exception {
        factory.close();
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    @Test
    public void testConstruction() {
        new RaftActorTestKit(getSystem(), "testConstruction").waitUntilLeader();
    }

    @Test
    public void testFindLeaderWhenLeaderIsSelf() {
        RaftActorTestKit kit = new RaftActorTestKit(getSystem(), "testFindLeader");
        kit.waitUntilLeader();
    }


    @Test
    public void testRaftActorRecoveryWithPersistenceEnabled() throws Exception {
        TEST_LOG.info("testRaftActorRecoveryWithPersistenceEnabled starting");

        JavaTestKit kit = new JavaTestKit(getSystem());
        String persistenceId = factory.generateActorId("follower-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        // Set the heartbeat interval high to essentially disable election otherwise the test
        // may fail if the actor is switched to Leader and the commitIndex is set to the last
        // log entry.
        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        ImmutableMap<String, String> peerAddresses = ImmutableMap.<String, String>builder()
                .put("member1", "address").build();
        ActorRef followerActor = factory.createActor(MockRaftActor.props(persistenceId,
                peerAddresses, config), persistenceId);

        kit.watch(followerActor);

        List<ReplicatedLogEntry> snapshotUnappliedEntries = new ArrayList<>();
        ReplicatedLogEntry entry1 = new SimpleReplicatedLogEntry(4, 1, new MockRaftActorContext.MockPayload("E"));
        snapshotUnappliedEntries.add(entry1);

        int lastAppliedDuringSnapshotCapture = 3;
        int lastIndexDuringSnapshotCapture = 4;

        // 4 messages as part of snapshot, which are applied to state
        MockSnapshotState snapshotState = new MockSnapshotState(Arrays.asList(
                new MockRaftActorContext.MockPayload("A"),
                new MockRaftActorContext.MockPayload("B"),
                new MockRaftActorContext.MockPayload("C"),
                new MockRaftActorContext.MockPayload("D")));

        Snapshot snapshot = Snapshot.create(snapshotState, snapshotUnappliedEntries, lastIndexDuringSnapshotCapture, 1,
                lastAppliedDuringSnapshotCapture, 1, -1, null, null);
        InMemorySnapshotStore.addSnapshot(persistenceId, snapshot);

        // add more entries after snapshot is taken
        List<ReplicatedLogEntry> entries = new ArrayList<>();
        ReplicatedLogEntry entry2 = new SimpleReplicatedLogEntry(5, 1, new MockRaftActorContext.MockPayload("F", 2));
        ReplicatedLogEntry entry3 = new SimpleReplicatedLogEntry(6, 1, new MockRaftActorContext.MockPayload("G", 3));
        ReplicatedLogEntry entry4 = new SimpleReplicatedLogEntry(7, 1, new MockRaftActorContext.MockPayload("H", 4));
        entries.add(entry2);
        entries.add(entry3);
        entries.add(entry4);

        final int lastAppliedToState = 5;
        final int lastIndex = 7;

        InMemoryJournal.addEntry(persistenceId, 5, entry2);
        // 2 entries are applied to state besides the 4 entries in snapshot
        InMemoryJournal.addEntry(persistenceId, 6, new ApplyJournalEntries(lastAppliedToState));
        InMemoryJournal.addEntry(persistenceId, 7, entry3);
        InMemoryJournal.addEntry(persistenceId, 8, entry4);

        // kill the actor
        followerActor.tell(PoisonPill.getInstance(), null);
        kit.expectMsgClass(JavaTestKit.duration("5 seconds"), Terminated.class);

        kit.unwatch(followerActor);

        //reinstate the actor
        TestActorRef<MockRaftActor> ref = factory.createTestActor(
                MockRaftActor.props(persistenceId, peerAddresses, config));

        MockRaftActor mockRaftActor = ref.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        RaftActorContext context = mockRaftActor.getRaftActorContext();
        assertEquals("Journal log size", snapshotUnappliedEntries.size() + entries.size(),
                context.getReplicatedLog().size());
        assertEquals("Journal data size", 10, context.getReplicatedLog().dataSize());
        assertEquals("Last index", lastIndex, context.getReplicatedLog().lastIndex());
        assertEquals("Last applied", lastAppliedToState, context.getLastApplied());
        assertEquals("Commit index", lastAppliedToState, context.getCommitIndex());
        assertEquals("Recovered state size", 6, mockRaftActor.getState().size());

        mockRaftActor.waitForInitializeBehaviorComplete();

        assertEquals("getRaftState", RaftState.Follower, mockRaftActor.getRaftState());

        TEST_LOG.info("testRaftActorRecoveryWithPersistenceEnabled ending");
    }

    @Test
    public void testRaftActorRecoveryWithPersistenceDisabled() throws Exception {
        String persistenceId = factory.generateActorId("follower-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        TestActorRef<MockRaftActor> ref = factory.createTestActor(MockRaftActor.props(persistenceId,
                ImmutableMap.<String, String>builder().put("member1", "address").build(),
                config, createProvider()), persistenceId);

        MockRaftActor mockRaftActor = ref.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        mockRaftActor.waitForInitializeBehaviorComplete();

        assertEquals("getRaftState", RaftState.Follower, mockRaftActor.getRaftState());
    }

    @Test
    public void testUpdateElectionTermPersistedWithPersistenceDisabled() throws Exception {
        final JavaTestKit kit = new JavaTestKit(getSystem());
        String persistenceId = factory.generateActorId("follower-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        config.setElectionTimeoutFactor(1);

        InMemoryJournal.addWriteMessagesCompleteLatch(persistenceId, 1);

        TestActorRef<MockRaftActor> ref = factory.createTestActor(MockRaftActor.props(persistenceId,
                ImmutableMap.<String, String>builder().put("member1", "address").build(),
                config, createProvider())
                .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);

        InMemoryJournal.waitForWriteMessagesComplete(persistenceId);
        List<UpdateElectionTerm> entries = InMemoryJournal.get(persistenceId, UpdateElectionTerm.class);
        assertEquals("UpdateElectionTerm entries", 1, entries.size());
        final UpdateElectionTerm updateEntry = entries.get(0);

        factory.killActor(ref, kit);

        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        ref = factory.createTestActor(MockRaftActor.props(persistenceId,
                ImmutableMap.<String, String>builder().put("member1", "address").build(), config,
                createProvider()).withDispatcher(Dispatchers.DefaultDispatcherId()),
                factory.generateActorId("follower-"));

        MockRaftActor actor = ref.underlyingActor();
        actor.waitForRecoveryComplete();

        RaftActorContext newContext = actor.getRaftActorContext();
        assertEquals("electionTerm", updateEntry.getCurrentTerm(),
                newContext.getTermInformation().getCurrentTerm());
        assertEquals("votedFor", updateEntry.getVotedFor(), newContext.getTermInformation().getVotedFor());

        entries = InMemoryJournal.get(persistenceId, UpdateElectionTerm.class);
        assertEquals("UpdateElectionTerm entries", 1, entries.size());
    }

    @Test
    public void testRaftActorForwardsToRaftActorRecoverySupport() {
        String persistenceId = factory.generateActorId("leader-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                Collections.<String, String>emptyMap(), config), persistenceId);

        MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

        // Wait for akka's recovery to complete so it doesn't interfere.
        mockRaftActor.waitForRecoveryComplete();

        RaftActorRecoverySupport mockSupport = mock(RaftActorRecoverySupport.class);
        mockRaftActor.setRaftActorRecoverySupport(mockSupport);

        Snapshot snapshot = Snapshot.create(ByteState.of(new byte[]{1}),
                Collections.<ReplicatedLogEntry>emptyList(), 3, 1, 3, 1, -1, null, null);
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

        verify(mockSupport).handleRecoveryMessage(same(snapshotOffer), any(PersistentDataProvider.class));
        verify(mockSupport).handleRecoveryMessage(same(logEntry), any(PersistentDataProvider.class));
        verify(mockSupport).handleRecoveryMessage(same(applyJournalEntries), any(PersistentDataProvider.class));
        verify(mockSupport).handleRecoveryMessage(same(deleteEntries), any(PersistentDataProvider.class));
        verify(mockSupport).handleRecoveryMessage(same(updateElectionTerm), any(PersistentDataProvider.class));
    }

    @Test
    public void testRaftActorForwardsToRaftActorSnapshotMessageSupport() {
        String persistenceId = factory.generateActorId("leader-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        RaftActorSnapshotMessageSupport mockSupport = mock(RaftActorSnapshotMessageSupport.class);

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId)
                .config(config).snapshotMessageSupport(mockSupport).props());

        MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

        // Wait for akka's recovery to complete so it doesn't interfere.
        mockRaftActor.waitForRecoveryComplete();

        ApplySnapshot applySnapshot = new ApplySnapshot(mock(Snapshot.class));
        doReturn(true).when(mockSupport).handleSnapshotMessage(same(applySnapshot), any(ActorRef.class));
        mockRaftActor.handleCommand(applySnapshot);

        CaptureSnapshotReply captureSnapshotReply = new CaptureSnapshotReply(ByteState.empty(),
                java.util.Optional.empty());
        doReturn(true).when(mockSupport).handleSnapshotMessage(same(captureSnapshotReply), any(ActorRef.class));
        mockRaftActor.handleCommand(captureSnapshotReply);

        SaveSnapshotSuccess saveSnapshotSuccess = new SaveSnapshotSuccess(new SnapshotMetadata("", 0L, 0L));
        doReturn(true).when(mockSupport).handleSnapshotMessage(same(saveSnapshotSuccess), any(ActorRef.class));
        mockRaftActor.handleCommand(saveSnapshotSuccess);

        SaveSnapshotFailure saveSnapshotFailure = new SaveSnapshotFailure(new SnapshotMetadata("", 0L, 0L),
                new Throwable());
        doReturn(true).when(mockSupport).handleSnapshotMessage(same(saveSnapshotFailure), any(ActorRef.class));
        mockRaftActor.handleCommand(saveSnapshotFailure);

        doReturn(true).when(mockSupport).handleSnapshotMessage(same(RaftActorSnapshotMessageSupport.COMMIT_SNAPSHOT),
                any(ActorRef.class));
        mockRaftActor.handleCommand(RaftActorSnapshotMessageSupport.COMMIT_SNAPSHOT);

        doReturn(true).when(mockSupport).handleSnapshotMessage(same(GetSnapshot.INSTANCE), any(ActorRef.class));
        mockRaftActor.handleCommand(GetSnapshot.INSTANCE);

        verify(mockSupport).handleSnapshotMessage(same(applySnapshot), any(ActorRef.class));
        verify(mockSupport).handleSnapshotMessage(same(captureSnapshotReply), any(ActorRef.class));
        verify(mockSupport).handleSnapshotMessage(same(saveSnapshotSuccess), any(ActorRef.class));
        verify(mockSupport).handleSnapshotMessage(same(saveSnapshotFailure), any(ActorRef.class));
        verify(mockSupport).handleSnapshotMessage(same(RaftActorSnapshotMessageSupport.COMMIT_SNAPSHOT),
                any(ActorRef.class));
        verify(mockSupport).handleSnapshotMessage(same(GetSnapshot.INSTANCE), any(ActorRef.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testApplyJournalEntriesCallsDataPersistence() throws Exception {
        String persistenceId = factory.generateActorId("leader-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                Collections.<String, String>emptyMap(), config, dataPersistenceProvider), persistenceId);

        MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

        mockRaftActor.waitForInitializeBehaviorComplete();

        mockRaftActor.waitUntilLeader();

        mockRaftActor.onReceiveCommand(new ApplyJournalEntries(10));

        verify(dataPersistenceProvider).persistAsync(any(ApplyJournalEntries.class), any(Procedure.class));
    }

    @Test
    public void testApplyState() throws Exception {
        String persistenceId = factory.generateActorId("leader-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                Collections.<String, String>emptyMap(), config, dataPersistenceProvider), persistenceId);

        MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

        mockRaftActor.waitForInitializeBehaviorComplete();

        ReplicatedLogEntry entry = new SimpleReplicatedLogEntry(5, 1, new MockRaftActorContext.MockPayload("F"));

        final Identifier id = new MockIdentifier("apply-state");
        mockRaftActor.getRaftActorContext().getApplyStateConsumer().accept(new ApplyState(mockActorRef, id, entry));

        verify(mockRaftActor.actorDelegate).applyState(eq(mockActorRef), eq(id), anyObject());
    }

    @Test
    public void testRaftRoleChangeNotifierWhenRaftActorHasNoPeers() throws Exception {
        TestActorRef<MessageCollectorActor> notifierActor = factory.createTestActor(
                Props.create(MessageCollectorActor.class));
        MessageCollectorActor.waitUntilReady(notifierActor);

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        long heartBeatInterval = 100;
        config.setHeartBeatInterval(FiniteDuration.create(heartBeatInterval, TimeUnit.MILLISECONDS));
        config.setElectionTimeoutFactor(20);

        String persistenceId = factory.generateActorId("notifier-");

        final TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder()
                .id(persistenceId).config(config).roleChangeNotifier(notifierActor).dataPersistenceProvider(
                    createProvider()).props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                persistenceId);

        List<RoleChanged> matches =  MessageCollectorActor.expectMatching(notifierActor, RoleChanged.class, 3);


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

        notifierActor.underlyingActor().clear();

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

        notifierActor.underlyingActor().clear();

        raftActor.handleCommand("any");

        leaderStateChange = MessageCollectorActor.expectFirstMatching(notifierActor, LeaderStateChanged.class);
        assertEquals(persistenceId, leaderStateChange.getMemberId());
        assertEquals(newLeaderId, leaderStateChange.getLeaderId());
        assertEquals(newLeaderVersion, leaderStateChange.getLeaderPayloadVersion());

        notifierActor.underlyingActor().clear();

        raftActor.handleCommand("any");

        Uninterruptibles.sleepUninterruptibly(505, TimeUnit.MILLISECONDS);
        leaderStateChange = MessageCollectorActor.getFirstMatching(notifierActor, LeaderStateChanged.class);
        assertNull(leaderStateChange);
    }

    @Test
    public void testRaftRoleChangeNotifierWhenRaftActorHasPeers() throws Exception {
        ActorRef notifierActor = factory.createActor(Props.create(MessageCollectorActor.class));
        MessageCollectorActor.waitUntilReady(notifierActor);

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        long heartBeatInterval = 100;
        config.setHeartBeatInterval(FiniteDuration.create(heartBeatInterval, TimeUnit.MILLISECONDS));
        config.setElectionTimeoutFactor(1);

        String persistenceId = factory.generateActorId("notifier-");

        factory.createActor(MockRaftActor.builder().id(persistenceId)
                .peerAddresses(ImmutableMap.of("leader", "fake/path"))
                .config(config).roleChangeNotifier(notifierActor).props());

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

        ActorRef followerActor1 =
                factory.createActor(Props.create(MessageCollectorActor.class));

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));

        DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put(follower1Id, followerActor1.path().toString());

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                MockRaftActor.props(persistenceId, peerAddresses, config, dataPersistenceProvider), persistenceId);

        MockRaftActor leaderActor = mockActorRef.underlyingActor();

        leaderActor.getRaftActorContext().setCommitIndex(4);
        leaderActor.getRaftActorContext().setLastApplied(4);
        leaderActor.getRaftActorContext().getTermInformation().update(1, persistenceId);

        leaderActor.waitForInitializeBehaviorComplete();

        // create 8 entries in the log - 0 to 4 are applied and will get picked up as part of the capture snapshot

        Leader leader = new Leader(leaderActor.getRaftActorContext());
        leaderActor.setCurrentBehavior(leader);
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        MockRaftActorContext.MockReplicatedLogBuilder logBuilder = new MockRaftActorContext.MockReplicatedLogBuilder();
        leaderActor.getRaftActorContext().setReplicatedLog(logBuilder.createEntries(0, 8, 1).build());

        assertEquals(8, leaderActor.getReplicatedLog().size());

        leaderActor.getRaftActorContext().getSnapshotManager().capture(
                new SimpleReplicatedLogEntry(6, 1, new MockRaftActorContext.MockPayload("x")), 4);

        verify(leaderActor.snapshotCohortDelegate).createSnapshot(anyObject(), anyObject());

        assertEquals(8, leaderActor.getReplicatedLog().size());

        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());
        //fake snapshot on index 5
        leaderActor.onReceiveCommand(new AppendEntriesReply(follower1Id, 1, true, 5, 1, (short)0));

        assertEquals(8, leaderActor.getReplicatedLog().size());

        //fake snapshot on index 6
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());
        leaderActor.onReceiveCommand(new AppendEntriesReply(follower1Id, 1, true, 6, 1, (short)0));
        assertEquals(8, leaderActor.getReplicatedLog().size());

        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        assertEquals(8, leaderActor.getReplicatedLog().size());

        MockSnapshotState snapshotState = new MockSnapshotState(Arrays.asList(
                new MockRaftActorContext.MockPayload("foo-0"),
                new MockRaftActorContext.MockPayload("foo-1"),
                new MockRaftActorContext.MockPayload("foo-2"),
                new MockRaftActorContext.MockPayload("foo-3"),
                new MockRaftActorContext.MockPayload("foo-4")));

        leaderActor.getRaftActorContext().getSnapshotManager().persist(snapshotState, java.util.Optional.empty(),
                Runtime.getRuntime().totalMemory());

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
        leaderActor.onReceiveCommand(new AppendEntriesReply(follower1Id, 1, true, 7, 1, (short)0));
        assertEquals(2, leaderActor.getReplicatedLog().size());
        assertEquals(8, leaderActor.getReplicatedLog().lastIndex());
    }

    @Test
    public void testFakeSnapshotsForFollowerWithInRealSnapshots() throws Exception {
        final String persistenceId = factory.generateActorId("follower-");
        final String leaderId = factory.generateActorId("leader-");


        ActorRef leaderActor1 =
                factory.createActor(Props.create(MessageCollectorActor.class));

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));

        DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put(leaderId, leaderActor1.path().toString());

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                MockRaftActor.props(persistenceId, peerAddresses, config, dataPersistenceProvider), persistenceId);

        MockRaftActor followerActor = mockActorRef.underlyingActor();
        followerActor.getRaftActorContext().setCommitIndex(4);
        followerActor.getRaftActorContext().setLastApplied(4);
        followerActor.getRaftActorContext().getTermInformation().update(1, persistenceId);

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
        followerActor.getRaftActorContext().getSnapshotManager().capture(
                new SimpleReplicatedLogEntry(5, 1, new MockRaftActorContext.MockPayload("D")), 4);

        verify(followerActor.snapshotCohortDelegate).createSnapshot(anyObject(), anyObject());

        assertEquals(6, followerActor.getReplicatedLog().size());

        //fake snapshot on index 6
        List<ReplicatedLogEntry> entries = Arrays.asList(
                (ReplicatedLogEntry) new SimpleReplicatedLogEntry(6, 1, new MockRaftActorContext.MockPayload("foo-6")));
        followerActor.onReceiveCommand(new AppendEntries(1, leaderId, 5, 1, entries, 5, 5, (short)0));
        assertEquals(7, followerActor.getReplicatedLog().size());

        //fake snapshot on index 7
        assertEquals(RaftState.Follower, followerActor.getCurrentBehavior().state());

        entries = Arrays.asList((ReplicatedLogEntry) new SimpleReplicatedLogEntry(7, 1,
                new MockRaftActorContext.MockPayload("foo-7")));
        followerActor.onReceiveCommand(new AppendEntries(1, leaderId, 6, 1, entries, 6, 6, (short) 0));
        assertEquals(8, followerActor.getReplicatedLog().size());

        assertEquals(RaftState.Follower, followerActor.getCurrentBehavior().state());


        ByteString snapshotBytes = fromObject(Arrays.asList(
                new MockRaftActorContext.MockPayload("foo-0"),
                new MockRaftActorContext.MockPayload("foo-1"),
                new MockRaftActorContext.MockPayload("foo-2"),
                new MockRaftActorContext.MockPayload("foo-3"),
                new MockRaftActorContext.MockPayload("foo-4")));
        followerActor.onReceiveCommand(new CaptureSnapshotReply(ByteState.of(snapshotBytes.toByteArray()),
                java.util.Optional.empty()));
        assertTrue(followerActor.getRaftActorContext().getSnapshotManager().isCapturing());

        // The commit is needed to complete the snapshot creation process
        followerActor.getRaftActorContext().getSnapshotManager().commit(-1, -1);

        // capture snapshot reply should remove the snapshotted entries only till replicatedToAllIndex
        assertEquals(3, followerActor.getReplicatedLog().size()); //indexes 5,6,7 left in the log
        assertEquals(7, followerActor.getReplicatedLog().lastIndex());

        entries = Arrays.asList((ReplicatedLogEntry) new SimpleReplicatedLogEntry(8, 1,
                new MockRaftActorContext.MockPayload("foo-7")));
        // send an additional entry 8 with leaderCommit = 7
        followerActor.onReceiveCommand(new AppendEntries(1, leaderId, 7, 1, entries, 7, 7, (short) 0));

        // 7 and 8, as lastapplied is 7
        assertEquals(2, followerActor.getReplicatedLog().size());
    }

    @Test
    public void testFakeSnapshotsForLeaderWithInInitiateSnapshots() throws Exception {
        final String persistenceId = factory.generateActorId("leader-");
        final String follower1Id = factory.generateActorId("follower-");
        final String follower2Id = factory.generateActorId("follower-");

        final ActorRef followerActor1 = factory.createActor(Props.create(MessageCollectorActor.class), follower1Id);
        final ActorRef followerActor2 = factory.createActor(Props.create(MessageCollectorActor.class), follower2Id);

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));

        DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put(follower1Id, followerActor1.path().toString());
        peerAddresses.put(follower2Id, followerActor2.path().toString());

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                MockRaftActor.props(persistenceId, peerAddresses, config, dataPersistenceProvider), persistenceId);

        MockRaftActor leaderActor = mockActorRef.underlyingActor();
        leaderActor.getRaftActorContext().setCommitIndex(9);
        leaderActor.getRaftActorContext().setLastApplied(9);
        leaderActor.getRaftActorContext().getTermInformation().update(1, persistenceId);

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

        leaderActor.onReceiveCommand(new AppendEntriesReply(follower1Id, 1, true, 9, 1, (short) 0));
        assertEquals(5, leaderActor.getReplicatedLog().size());
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        // set the 2nd follower nextIndex to 1 which has been snapshotted
        leaderActor.onReceiveCommand(new AppendEntriesReply(follower2Id, 1, true, 0, 1, (short)0));
        assertEquals(5, leaderActor.getReplicatedLog().size());
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        // simulate a real snapshot
        leaderActor.onReceiveCommand(SendHeartBeat.INSTANCE);
        assertEquals(5, leaderActor.getReplicatedLog().size());
        assertEquals(String.format("expected to be Leader but was %s. Current Leader = %s ",
                leaderActor.getCurrentBehavior().state(), leaderActor.getLeaderId()),
                RaftState.Leader, leaderActor.getCurrentBehavior().state());


        //reply from a slow follower does not initiate a fake snapshot
        leaderActor.onReceiveCommand(new AppendEntriesReply(follower2Id, 1, true, 9, 1, (short)0));
        assertEquals("Fake snapshot should not happen when Initiate is in progress", 5,
                leaderActor.getReplicatedLog().size());

        ByteString snapshotBytes = fromObject(Arrays.asList(
                new MockRaftActorContext.MockPayload("foo-0"),
                new MockRaftActorContext.MockPayload("foo-1"),
                new MockRaftActorContext.MockPayload("foo-2"),
                new MockRaftActorContext.MockPayload("foo-3"),
                new MockRaftActorContext.MockPayload("foo-4")));
        leaderActor.onReceiveCommand(new CaptureSnapshotReply(ByteState.of(snapshotBytes.toByteArray()),
                java.util.Optional.empty()));
        assertTrue(leaderActor.getRaftActorContext().getSnapshotManager().isCapturing());

        assertEquals("Real snapshot didn't clear the log till replicatedToAllIndex", 0,
                leaderActor.getReplicatedLog().size());

        //reply from a slow follower after should not raise errors
        leaderActor.onReceiveCommand(new AppendEntriesReply(follower2Id, 1, true, 5, 1, (short) 0));
        assertEquals(0, leaderActor.getReplicatedLog().size());
    }

    @Test
    public void testRealSnapshotWhenReplicatedToAllIndexMinusOne() throws Exception {
        String persistenceId = factory.generateActorId("leader-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));
        config.setSnapshotBatchCount(5);

        DataPersistenceProvider dataPersistenceProvider = createProvider();

        Map<String, String> peerAddresses = ImmutableMap.<String, String>builder().put("member1", "address").build();

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                MockRaftActor.props(persistenceId, peerAddresses, config, dataPersistenceProvider), persistenceId);

        MockRaftActor leaderActor = mockActorRef.underlyingActor();
        leaderActor.getRaftActorContext().setCommitIndex(3);
        leaderActor.getRaftActorContext().setLastApplied(3);
        leaderActor.getRaftActorContext().getTermInformation().update(1, persistenceId);

        leaderActor.waitForInitializeBehaviorComplete();
        for (int i = 0; i < 4; i++) {
            leaderActor.getReplicatedLog().append(new SimpleReplicatedLogEntry(i, 1,
                    new MockRaftActorContext.MockPayload("A")));
        }

        Leader leader = new Leader(leaderActor.getRaftActorContext());
        leaderActor.setCurrentBehavior(leader);
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        // Simulate an install snaphost to a follower.
        leaderActor.getRaftActorContext().getSnapshotManager().captureToInstall(
                leaderActor.getReplicatedLog().last(), -1, "member1");

        // Now send a CaptureSnapshotReply
        mockActorRef.tell(new CaptureSnapshotReply(ByteState.of(fromObject("foo").toByteArray()),
                java.util.Optional.empty()), mockActorRef);

        // Trimming log in this scenario is a no-op
        assertEquals(-1, leaderActor.getReplicatedLog().getSnapshotIndex());
        assertTrue(leaderActor.getRaftActorContext().getSnapshotManager().isCapturing());
        assertEquals(-1, leader.getReplicatedToAllIndex());
    }

    @Test
    public void testRealSnapshotWhenReplicatedToAllIndexNotInReplicatedLog() throws Exception {
        String persistenceId = factory.generateActorId("leader-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));
        config.setSnapshotBatchCount(5);

        DataPersistenceProvider dataPersistenceProvider = createProvider();

        Map<String, String> peerAddresses = ImmutableMap.<String, String>builder().put("member1", "address").build();

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                MockRaftActor.props(persistenceId, peerAddresses, config, dataPersistenceProvider), persistenceId);

        MockRaftActor leaderActor = mockActorRef.underlyingActor();
        leaderActor.getRaftActorContext().setCommitIndex(3);
        leaderActor.getRaftActorContext().setLastApplied(3);
        leaderActor.getRaftActorContext().getTermInformation().update(1, persistenceId);
        leaderActor.getReplicatedLog().setSnapshotIndex(3);

        leaderActor.waitForInitializeBehaviorComplete();
        Leader leader = new Leader(leaderActor.getRaftActorContext());
        leaderActor.setCurrentBehavior(leader);
        leader.setReplicatedToAllIndex(3);
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        // Persist another entry (this will cause a CaptureSnapshot to be triggered
        leaderActor.persistData(mockActorRef, new MockIdentifier("x"),
                new MockRaftActorContext.MockPayload("duh"), false);

        // Now send a CaptureSnapshotReply
        mockActorRef.tell(new CaptureSnapshotReply(ByteState.of(fromObject("foo").toByteArray()),
                java.util.Optional.empty()), mockActorRef);

        // Trimming log in this scenario is a no-op
        assertEquals(3, leaderActor.getReplicatedLog().getSnapshotIndex());
        assertTrue(leaderActor.getRaftActorContext().getSnapshotManager().isCapturing());
        assertEquals(3, leader.getReplicatedToAllIndex());
    }

    private static DataPersistenceProvider createProvider() {
        return new NonPersistentDataProvider(Runnable::run);
    }

    @Test
    public void testSwitchBehavior() {
        String persistenceId = factory.generateActorId("leader-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());
        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));
        config.setSnapshotBatchCount(5);

        DataPersistenceProvider dataPersistenceProvider = createProvider();

        Map<String, String> peerAddresses = ImmutableMap.<String, String>builder().build();

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                MockRaftActor.props(persistenceId, peerAddresses, config, dataPersistenceProvider), persistenceId);

        MockRaftActor leaderActor = mockActorRef.underlyingActor();

        leaderActor.waitForRecoveryComplete();

        leaderActor.handleCommand(new SwitchBehavior(RaftState.Follower, 100));

        assertEquals(100, leaderActor.getRaftActorContext().getTermInformation().getCurrentTerm());
        assertEquals(RaftState.Follower, leaderActor.getCurrentBehavior().state());

        leaderActor.handleCommand(new SwitchBehavior(RaftState.Leader, 110));

        assertEquals(110, leaderActor.getRaftActorContext().getTermInformation().getCurrentTerm());
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        leaderActor.handleCommand(new SwitchBehavior(RaftState.Candidate, 125));

        assertEquals(110, leaderActor.getRaftActorContext().getTermInformation().getCurrentTerm());
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

        leaderActor.handleCommand(new SwitchBehavior(RaftState.IsolatedLeader, 125));

        assertEquals(110, leaderActor.getRaftActorContext().getTermInformation().getCurrentTerm());
        assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());
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
    public void testUpdateConfigParam() throws Exception {
        DefaultConfigParamsImpl emptyConfig = new DefaultConfigParamsImpl();
        String persistenceId = factory.generateActorId("follower-");
        ImmutableMap<String, String> peerAddresses =
            ImmutableMap.<String, String>builder().put("member1", "address").build();
        DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

        TestActorRef<MockRaftActor> actorRef = factory.createTestActor(
                MockRaftActor.props(persistenceId, peerAddresses, emptyConfig, dataPersistenceProvider), persistenceId);
        MockRaftActor mockRaftActor = actorRef.underlyingActor();
        mockRaftActor.waitForInitializeBehaviorComplete();

        RaftActorBehavior behavior = mockRaftActor.getCurrentBehavior();
        mockRaftActor.updateConfigParams(emptyConfig);
        assertSame("Same Behavior", behavior, mockRaftActor.getCurrentBehavior());
        assertEquals("Behavior State", RaftState.Follower,
            mockRaftActor.getCurrentBehavior().state());

        DefaultConfigParamsImpl disableConfig = new DefaultConfigParamsImpl();
        disableConfig.setCustomRaftPolicyImplementationClass(
            "org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy");
        mockRaftActor.updateConfigParams(disableConfig);
        assertNotSame("Different Behavior", behavior, mockRaftActor.getCurrentBehavior());
        assertEquals("Behavior State", RaftState.Follower,
            mockRaftActor.getCurrentBehavior().state());

        behavior = mockRaftActor.getCurrentBehavior();
        mockRaftActor.updateConfigParams(disableConfig);
        assertSame("Same Behavior", behavior, mockRaftActor.getCurrentBehavior());
        assertEquals("Behavior State", RaftState.Follower,
            mockRaftActor.getCurrentBehavior().state());

        DefaultConfigParamsImpl defaultConfig = new DefaultConfigParamsImpl();
        defaultConfig.setCustomRaftPolicyImplementationClass(
            "org.opendaylight.controller.cluster.raft.policy.DefaultRaftPolicy");
        mockRaftActor.updateConfigParams(defaultConfig);
        assertNotSame("Different Behavior", behavior, mockRaftActor.getCurrentBehavior());
        assertEquals("Behavior State", RaftState.Follower,
            mockRaftActor.getCurrentBehavior().state());

        behavior = mockRaftActor.getCurrentBehavior();
        mockRaftActor.updateConfigParams(defaultConfig);
        assertSame("Same Behavior", behavior, mockRaftActor.getCurrentBehavior());
        assertEquals("Behavior State", RaftState.Follower,
            mockRaftActor.getCurrentBehavior().state());
    }

    @Test
    public void testGetSnapshot() throws Exception {
        TEST_LOG.info("testGetSnapshot starting");

        final JavaTestKit kit = new JavaTestKit(getSystem());

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
                ImmutableMap.<String, String>builder().put("member1", "address").build(), config)
                    .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        mockRaftActor.snapshotCohortDelegate = mock(RaftActorSnapshotCohort.class);

        raftActorRef.tell(GetSnapshot.INSTANCE, kit.getRef());

        ArgumentCaptor<ActorRef> replyActor = ArgumentCaptor.forClass(ActorRef.class);
        verify(mockRaftActor.snapshotCohortDelegate, timeout(5000)).createSnapshot(replyActor.capture(),
                eq(java.util.Optional.empty()));

        byte[] stateSnapshot = new byte[]{1,2,3};
        replyActor.getValue().tell(new CaptureSnapshotReply(ByteState.of(stateSnapshot), java.util.Optional.empty()),
                ActorRef.noSender());

        GetSnapshotReply reply = kit.expectMsgClass(GetSnapshotReply.class);

        assertEquals("getId", persistenceId, reply.getId());
        Snapshot replySnapshot = reply.getSnapshot();
        assertEquals("getElectionTerm", term, replySnapshot.getElectionTerm());
        assertEquals("getElectionVotedFor", "member-1", replySnapshot.getElectionVotedFor());
        assertEquals("getLastAppliedIndex", 1L, replySnapshot.getLastAppliedIndex());
        assertEquals("getLastAppliedTerm", term, replySnapshot.getLastAppliedTerm());
        assertEquals("getLastIndex", 2L, replySnapshot.getLastIndex());
        assertEquals("getLastTerm", term, replySnapshot.getLastTerm());
        assertEquals("getState", ByteState.of(stateSnapshot), replySnapshot.getState());
        assertEquals("getUnAppliedEntries size", 1, replySnapshot.getUnAppliedEntries().size());
        assertEquals("UnApplied entry index ", 2L, replySnapshot.getUnAppliedEntries().get(0).getIndex());

        // Test with timeout

        mockRaftActor.getSnapshotMessageSupport().setSnapshotReplyActorTimeout(
                Duration.create(200, TimeUnit.MILLISECONDS));
        reset(mockRaftActor.snapshotCohortDelegate);

        raftActorRef.tell(GetSnapshot.INSTANCE, kit.getRef());
        Failure failure = kit.expectMsgClass(akka.actor.Status.Failure.class);
        assertEquals("Failure cause type", TimeoutException.class, failure.cause().getClass());

        mockRaftActor.getSnapshotMessageSupport().setSnapshotReplyActorTimeout(Duration.create(30, TimeUnit.SECONDS));

        // Test with persistence disabled.

        mockRaftActor.setPersistence(false);
        reset(mockRaftActor.snapshotCohortDelegate);

        raftActorRef.tell(GetSnapshot.INSTANCE, kit.getRef());
        reply = kit.expectMsgClass(GetSnapshotReply.class);
        verify(mockRaftActor.snapshotCohortDelegate, never()).createSnapshot(anyObject(), anyObject());

        assertEquals("getId", persistenceId, reply.getId());
        replySnapshot = reply.getSnapshot();
        assertEquals("getElectionTerm", term, replySnapshot.getElectionTerm());
        assertEquals("getElectionVotedFor", "member-1", replySnapshot.getElectionVotedFor());
        assertEquals("getLastAppliedIndex", -1L, replySnapshot.getLastAppliedIndex());
        assertEquals("getLastAppliedTerm", -1L, replySnapshot.getLastAppliedTerm());
        assertEquals("getLastIndex", -1L, replySnapshot.getLastIndex());
        assertEquals("getLastTerm", -1L, replySnapshot.getLastTerm());
        assertEquals("getState type", EmptyState.INSTANCE, replySnapshot.getState());
        assertEquals("getUnAppliedEntries size", 0, replySnapshot.getUnAppliedEntries().size());

        TEST_LOG.info("testGetSnapshot ending");
    }

    @Test
    public void testRestoreFromSnapshot() throws Exception {
        TEST_LOG.info("testRestoreFromSnapshot starting");

        String persistenceId = factory.generateActorId("test-actor-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        List<ReplicatedLogEntry> snapshotUnappliedEntries = new ArrayList<>();
        snapshotUnappliedEntries.add(new SimpleReplicatedLogEntry(4, 1, new MockRaftActorContext.MockPayload("E")));

        int snapshotLastApplied = 3;
        int snapshotLastIndex = 4;

        MockSnapshotState snapshotState = new MockSnapshotState(Arrays.asList(
                new MockRaftActorContext.MockPayload("A"),
                new MockRaftActorContext.MockPayload("B"),
                new MockRaftActorContext.MockPayload("C"),
                new MockRaftActorContext.MockPayload("D")));

        Snapshot snapshot = Snapshot.create(snapshotState, snapshotUnappliedEntries,
                snapshotLastIndex, 1, snapshotLastApplied, 1, 1, "member-1", null);

        InMemorySnapshotStore.addSnapshotSavedLatch(persistenceId);

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId)
                .config(config).restoreFromSnapshot(snapshot).props()
                    .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        Snapshot savedSnapshot = InMemorySnapshotStore.waitForSavedSnapshot(persistenceId, Snapshot.class);
        assertEquals("getElectionTerm", snapshot.getElectionTerm(), savedSnapshot.getElectionTerm());
        assertEquals("getElectionVotedFor", snapshot.getElectionVotedFor(), savedSnapshot.getElectionVotedFor());
        assertEquals("getLastAppliedIndex", snapshot.getLastAppliedIndex(), savedSnapshot.getLastAppliedIndex());
        assertEquals("getLastAppliedTerm", snapshot.getLastAppliedTerm(), savedSnapshot.getLastAppliedTerm());
        assertEquals("getLastIndex", snapshot.getLastIndex(), savedSnapshot.getLastIndex());
        assertEquals("getLastTerm", snapshot.getLastTerm(), savedSnapshot.getLastTerm());
        assertEquals("getState", snapshot.getState(), savedSnapshot.getState());
        assertEquals("getUnAppliedEntries", snapshot.getUnAppliedEntries(), savedSnapshot.getUnAppliedEntries());

        verify(mockRaftActor.snapshotCohortDelegate, timeout(5000)).applySnapshot(any(Snapshot.State.class));

        RaftActorContext context = mockRaftActor.getRaftActorContext();
        assertEquals("Journal log size", 1, context.getReplicatedLog().size());
        assertEquals("Last index", snapshotLastIndex, context.getReplicatedLog().lastIndex());
        assertEquals("Last applied", snapshotLastApplied, context.getLastApplied());
        assertEquals("Commit index", snapshotLastApplied, context.getCommitIndex());
        assertEquals("Recovered state", snapshotState.getState(), mockRaftActor.getState());
        assertEquals("Current term", 1L, context.getTermInformation().getCurrentTerm());
        assertEquals("Voted for", "member-1", context.getTermInformation().getVotedFor());

        // Test with data persistence disabled

        snapshot = Snapshot.create(EmptyState.INSTANCE, Collections.<ReplicatedLogEntry>emptyList(),
                -1, -1, -1, -1, 5, "member-1", null);

        persistenceId = factory.generateActorId("test-actor-");

        raftActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId)
                .config(config).restoreFromSnapshot(snapshot)
                .persistent(Optional.of(Boolean.FALSE)).props()
                    .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();
        assertEquals("snapshot committed", true,
                Uninterruptibles.awaitUninterruptibly(mockRaftActor.snapshotCommitted, 5, TimeUnit.SECONDS));

        context = mockRaftActor.getRaftActorContext();
        assertEquals("Current term", 5L, context.getTermInformation().getCurrentTerm());
        assertEquals("Voted for", "member-1", context.getTermInformation().getVotedFor());

        TEST_LOG.info("testRestoreFromSnapshot ending");
    }

    @Test
    public void testRestoreFromSnapshotWithRecoveredData() throws Exception {
        TEST_LOG.info("testRestoreFromSnapshotWithRecoveredData starting");

        String persistenceId = factory.generateActorId("test-actor-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        List<MockPayload> state = Arrays.asList(new MockRaftActorContext.MockPayload("A"));
        Snapshot snapshot = Snapshot.create(ByteState.of(fromObject(state).toByteArray()),
                Arrays.<ReplicatedLogEntry>asList(), 5, 2, 5, 2, 2, "member-1", null);

        InMemoryJournal.addEntry(persistenceId, 1, new SimpleReplicatedLogEntry(0, 1,
                new MockRaftActorContext.MockPayload("B")));

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId)
                .config(config).restoreFromSnapshot(snapshot).props()
                    .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verify(mockRaftActor.snapshotCohortDelegate, never()).applySnapshot(any(Snapshot.State.class));

        RaftActorContext context = mockRaftActor.getRaftActorContext();
        assertEquals("Journal log size", 1, context.getReplicatedLog().size());
        assertEquals("Last index", 0, context.getReplicatedLog().lastIndex());
        assertEquals("Last applied", -1, context.getLastApplied());
        assertEquals("Commit index", -1, context.getCommitIndex());
        assertEquals("Current term", 0, context.getTermInformation().getCurrentTerm());
        assertEquals("Voted for", null, context.getTermInformation().getVotedFor());

        TEST_LOG.info("testRestoreFromSnapshotWithRecoveredData ending");
    }

    @Test
    public void testNonVotingOnRecovery() throws Exception {
        TEST_LOG.info("testNonVotingOnRecovery starting");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setElectionTimeoutFactor(1);
        config.setHeartBeatInterval(FiniteDuration.create(1, TimeUnit.MILLISECONDS));

        String persistenceId = factory.generateActorId("test-actor-");
        InMemoryJournal.addEntry(persistenceId, 1,  new SimpleReplicatedLogEntry(0, 1,
                new ServerConfigurationPayload(Arrays.asList(new ServerInfo(persistenceId, false)))));

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId)
                .config(config).props().withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForInitializeBehaviorComplete();

        // Sleep a bit and verify it didn't get an election timeout and schedule an election.

        Uninterruptibles.sleepUninterruptibly(400, TimeUnit.MILLISECONDS);
        assertEquals("getRaftState", RaftState.Follower, mockRaftActor.getRaftState());

        TEST_LOG.info("testNonVotingOnRecovery ending");
    }

    @Test
    public void testLeaderTransitioning() throws Exception {
        TEST_LOG.info("testLeaderTransitioning starting");

        TestActorRef<MessageCollectorActor> notifierActor = factory.createTestActor(
                Props.create(MessageCollectorActor.class));

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        String persistenceId = factory.generateActorId("test-actor-");

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId)
                .config(config).roleChangeNotifier(notifierActor).props()
                .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForInitializeBehaviorComplete();

        raftActorRef.tell(new AppendEntries(1L, "leader", 0L, 1L, Collections.<ReplicatedLogEntry>emptyList(),
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testReplicateWithPersistencePending() throws Exception {
        final String leaderId = factory.generateActorId("leader-");
        final String followerId = factory.generateActorId("follower-");

        final ActorRef followerActor = factory.createActor(Props.create(MessageCollectorActor.class));

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));

        DataPersistenceProvider mockPersistenceProvider = mock(DataPersistenceProvider.class);
        doReturn(true).when(mockPersistenceProvider).isRecoveryApplicable();

        TestActorRef<MockRaftActor> leaderActorRef = factory.createTestActor(
                MockRaftActor.props(leaderId, ImmutableMap.of(followerId, followerActor.path().toString()), config,
                        mockPersistenceProvider), leaderId);
        MockRaftActor leaderActor = leaderActorRef.underlyingActor();
        leaderActor.waitForInitializeBehaviorComplete();

        leaderActor.getRaftActorContext().getTermInformation().update(1, leaderId);

        Leader leader = new Leader(leaderActor.getRaftActorContext());
        leaderActor.setCurrentBehavior(leader);

        leaderActor.persistData(leaderActorRef, new MockIdentifier("1"), new MockRaftActorContext.MockPayload("1"),
                false);

        ReplicatedLogEntry logEntry = leaderActor.getReplicatedLog().get(0);
        assertNotNull("ReplicatedLogEntry not found", logEntry);
        assertEquals("isPersistencePending", true, logEntry.isPersistencePending());
        assertEquals("getCommitIndex", -1, leaderActor.getRaftActorContext().getCommitIndex());

        leaderActor.onReceiveCommand(new AppendEntriesReply(followerId, 1, true, 0, 1, (short)0));
        assertEquals("getCommitIndex", -1, leaderActor.getRaftActorContext().getCommitIndex());

        ArgumentCaptor<Procedure> callbackCaptor = ArgumentCaptor.forClass(Procedure.class);
        verify(mockPersistenceProvider).persistAsync(eq(logEntry), callbackCaptor.capture());

        callbackCaptor.getValue().apply(logEntry);

        assertEquals("getCommitIndex", 0, leaderActor.getRaftActorContext().getCommitIndex());
        assertEquals("getLastApplied", 0, leaderActor.getRaftActorContext().getLastApplied());
    }

    @Test
    public void testReplicateWithBatchHint() throws Exception {
        final String leaderId = factory.generateActorId("leader-");
        final String followerId = factory.generateActorId("follower-");

        final ActorRef followerActor = factory.createActor(Props.create(MessageCollectorActor.class));

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));

        TestActorRef<MockRaftActor> leaderActorRef = factory.createTestActor(
                MockRaftActor.props(leaderId, ImmutableMap.of(followerId, followerActor.path().toString()), config),
                    leaderId);
        MockRaftActor leaderActor = leaderActorRef.underlyingActor();
        leaderActor.waitForInitializeBehaviorComplete();

        leaderActor.getRaftActorContext().getTermInformation().update(1, leaderId);

        Leader leader = new Leader(leaderActor.getRaftActorContext());
        leaderActor.setCurrentBehavior(leader);

        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        MessageCollectorActor.clearMessages(followerActor);

        leaderActor.onReceiveCommand(new AppendEntriesReply(followerId, 1, true, -1, -1, (short)0));

        leaderActor.persistData(leaderActorRef, new MockIdentifier("1"), new MockPayload("1"), true);
        MessageCollectorActor.assertNoneMatching(followerActor, AppendEntries.class, 500);

        leaderActor.persistData(leaderActorRef, new MockIdentifier("2"), new MockPayload("2"), true);
        MessageCollectorActor.assertNoneMatching(followerActor, AppendEntries.class, 500);

        leaderActor.persistData(leaderActorRef, new MockIdentifier("3"), new MockPayload("3"), false);
        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("AppendEntries size", 3, appendEntries.getEntries().size());
    }
}
