/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Uninterruptibles;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.protobufv3.internal.ByteString;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.AbstractRaftActorIntegrationTest.TestPersist;
import org.opendaylight.controller.cluster.raft.AbstractRaftActorIntegrationTest.TestRaftActor;
import org.opendaylight.controller.cluster.raft.SnapshotManager.ApplyLeaderSnapshot;
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
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.persisted.ByteStateSnapshotCohort;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.spi.DefaultLogEntry;
import org.opendaylight.controller.cluster.raft.spi.EntryJournalV1;
import org.opendaylight.controller.cluster.raft.spi.EntryStore;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.PropertiesTermInfoStore;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.RaftStorage;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFileFormat;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.api.RaftRole;
import org.opendaylight.raft.api.TermInfo;
import org.opendaylight.raft.spi.ByteArray;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.InstallableSnapshot;
import org.opendaylight.raft.spi.InstallableSnapshotSource;
import org.opendaylight.raft.spi.PlainSnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
//FIXME: remove this line
@MockitoSettings(strictness = Strictness.LENIENT)
class RaftActorTest extends AbstractActorTest {
    private static final Logger TEST_LOG = LoggerFactory.getLogger(RaftActorTest.class);
    private static final Duration ONE_DAY = Duration.ofDays(1);

    private TestActorFactory factory;

    @BeforeEach
    void beforeEach() {
        factory = new TestActorFactory(getSystem());
    }

    @AfterEach
    void afterEach() {
        factory.close();
    }

    @Test
    void testConstruction() {
        new RaftActorTestKit(stateDir(), getSystem(), "testConstruction").waitUntilLeader();
    }

    @Test
    void testFindLeaderWhenLeaderIsSelf() {
        final var kit = new RaftActorTestKit(stateDir(), getSystem(), "testFindLeader");
        kit.waitUntilLeader();
    }

    @Test
    void testRaftActorRecoveryWithPersistenceEnabled() throws Exception {
        TEST_LOG.info("testRaftActorRecoveryWithPersistenceEnabled starting");

        final var persistenceId = factory.generateActorId("follower-");

        final var config = new DefaultConfigParamsImpl();

        // Set the heartbeat interval high to essentially disable election otherwise the test
        // may fail if the actor is switched to Leader and the commitIndex is set to the last
        // log entry.
        config.setHeartBeatInterval(ONE_DAY);

        final var peerAddresses = Map.of("member1", "address");
        final var actorDir = stateDir().resolve(persistenceId);
        Files.createDirectories(actorDir);

        // 4 messages as part of snapshot, which are applied to state
        RaftStorage.saveSnapshot(persistenceId, actorDir, SnapshotFileFormat.latest(), CompressionType.NONE,
            new RaftSnapshot(null, List.of()), EntryInfo.of(3, 1),
            ToStorage.of(MockSnapshotState.SUPPORT.writer(), new MockSnapshotState(List.of(
                new MockCommand("A"), new MockCommand("B"), new MockCommand("C"), new MockCommand("D")))),
            Instant.now());

        // add more entries after snapshot is taken

        final int lastAppliedToState = 5;
        final int lastIndex = 7;

        try (var journal = new EntryJournalV1(persistenceId, actorDir, CompressionType.NONE, true)) {
            journal.appendEntry(new DefaultLogEntry(4, 1, new MockCommand("E")));
            journal.appendEntry(new DefaultLogEntry(5, 1, new MockCommand("F", 2)));
            journal.appendEntry(new DefaultLogEntry(6, 1, new MockCommand("G", 3)));
            journal.appendEntry(new DefaultLogEntry(7, 1, new MockCommand("H", 4)));
            // 2 entries are applied to state besides the 4 entries in snapshot
            journal.setApplyTo(2);
        }

        //reinstate the actor
        final var ref = factory.<MockRaftActor>createTestActor(MockRaftActor.props(persistenceId, stateDir(),
            peerAddresses, config));

        final var mockRaftActor = ref.underlyingActor();
        mockRaftActor.waitForRecoveryComplete();

        final var context = mockRaftActor.getRaftActorContext();
        final var log = context.getReplicatedLog();
        assertEquals("Journal log size", 4, log.size());
        assertEquals("Journal data size", 10, log.dataSize());
        assertEquals("Last index", lastIndex, log.lastIndex());
        assertEquals("Last applied", lastAppliedToState, log.getLastApplied());
        assertEquals("Commit index", lastAppliedToState, log.getCommitIndex());
        assertEquals("Recovered state size", 6, mockRaftActor.getState().size());

        mockRaftActor.waitForInitializeBehaviorComplete();

        assertEquals("getRaftState", RaftRole.Follower, mockRaftActor.getRaftState());

        TEST_LOG.info("testRaftActorRecoveryWithPersistenceEnabled ending");
    }

    @Test
    void testRaftActorRecoveryWithPersistenceDisabled() {
        String persistenceId = factory.generateActorId("follower-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        config.setHeartBeatInterval(ONE_DAY);

        TestActorRef<MockRaftActor> ref = factory.createTestActor(MockRaftActor.props(persistenceId, stateDir(),
                Map.of("member1", "address"), config, new TestPersistenceProvider()), persistenceId);

        MockRaftActor mockRaftActor = ref.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        mockRaftActor.waitForInitializeBehaviorComplete();

        assertEquals("getRaftState", RaftRole.Follower, mockRaftActor.getRaftState());
    }

    @Test
    void testUpdateElectionTermPersistedWithPersistenceDisabled() {
        final TestKit kit = new TestKit(getSystem());
        String persistenceId = factory.generateActorId("follower-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(Duration.ofMillis(100));
        config.setElectionTimeoutFactor(1);

        TestActorRef<MockRaftActor> ref = factory.createTestActor(MockRaftActor.props(persistenceId, stateDir(),
                Map.of("member1", "address"), config, new TestPersistenceProvider())
                .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        ref.underlyingActor().waitForRecoveryComplete();

        assertThat(stateDir().resolve(persistenceId).resolve("TermInfo.properties")).isRegularFile();

        factory.killActor(ref, kit);

        config.setHeartBeatInterval(ONE_DAY);
        ref = factory.createTestActor(MockRaftActor.props(persistenceId, stateDir(), Map.of("member1", "address"),
                config, new TestPersistenceProvider()).withDispatcher(Dispatchers.DefaultDispatcherId()),
                factory.generateActorId("follower-"));

        MockRaftActor actor = ref.underlyingActor();
        actor.waitForRecoveryComplete();

        RaftActorContext newContext = actor.getRaftActorContext();
        assertEquals("electionTerm", new TermInfo(0), newContext.termInfo());
    }

    @Test
    void testRaftActorForwardsToRaftActorSnapshotMessageSupport() throws Exception {
        String persistenceId = factory.generateActorId("leader-");

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

        config.setHeartBeatInterval(ONE_DAY);

        RaftActorSnapshotMessageSupport mockSupport = mock(RaftActorSnapshotMessageSupport.class);

        TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId)
            .config(config).snapshotMessageSupport(mockSupport).props(stateDir()));

        MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

        // Wait for akka's recovery to complete so it doesn't interfere.
        mockRaftActor.waitForRecoveryComplete();

        final var applySnapshot = new ApplyLeaderSnapshot(persistenceId, 0, EntryInfo.of(0, 0),
            new PlainSnapshotSource(ByteArray.wrap(new byte[1])), null, mock(ApplyLeaderSnapshot.Callback.class));

        when(mockSupport.handleSnapshotMessage(same(applySnapshot))).thenReturn(true);
        mockRaftActor.handleReceive(applySnapshot);

        when(mockSupport.handleSnapshotMessage(same(GetSnapshot.INSTANCE))).thenReturn(true);
        mockRaftActor.handleReceive(GetSnapshot.INSTANCE);

        verify(mockSupport).handleSnapshotMessage(same(applySnapshot));
        verify(mockSupport).handleSnapshotMessage(same(GetSnapshot.INSTANCE));
    }

    @Test
    void testApplyJournalEntriesCallsDataPersistence() throws Exception {
        final var persistenceId = factory.generateActorId("leader-");
        final var config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        final var mockRaftActor = factory.<MockRaftActor>createTestActor(MockRaftActor.props(persistenceId, stateDir(),
            Map.of(), config, mockProvider()), persistenceId)
            .underlyingActor();
        mockRaftActor.waitForInitializeBehaviorComplete();
        mockRaftActor.waitUntilLeader();

        mockRaftActor.getRaftActorContext().getReplicatedLog().markLastApplied();
    }

    @Test
    void testApplyState() {
        final var persistenceId = factory.generateActorId("leader-");
        final var config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);

        final var mockActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.props(persistenceId,
            stateDir(), Map.of(), config, mockProvider()), persistenceId);

        final var mockRaftActor = mockActorRef.underlyingActor();
        mockRaftActor.waitForInitializeBehaviorComplete();

        final var entry = new DefaultLogEntry(5, 1, new MockCommand("F"));
        final var id = new MockIdentifier("apply-state");
        mockRaftActor.getRaftActorContext().applyEntryMethod().applyEntry(id, entry);

        verify(mockRaftActor.actorDelegate).applyCommand(eq(id), any());
    }

    private static PersistenceProvider mockProvider() {
        final var provider = mock(PersistenceProvider.class);
        doReturn(mock(EntryStore.class)).when(provider).entryStore();
        doReturn(mock(SnapshotStore.class)).when(provider).snapshotStore();
        return provider;
    }

    @Test
    void testRaftRoleChangeNotifierWhenRaftActorHasNoPeers() throws Exception {
        final var notifierActor = factory.createActor(MessageCollectorActor.props());
        MessageCollectorActor.waitUntilReady(notifierActor);

        final var config = new DefaultConfigParamsImpl();
        final var heartBeatInterval = 100L;
        config.setHeartBeatInterval(Duration.ofMillis(heartBeatInterval));
        config.setElectionTimeoutFactor(20);

        final var persistenceId = factory.generateActorId("notifier-");

        final var raftActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.builder()
                .id(persistenceId).config(config).roleChangeNotifier(notifierActor)
                .dataPersistenceProvider(new TestPersistenceProvider()).props(stateDir())
                .withDispatcher(Dispatchers.DefaultDispatcherId()),
                persistenceId);

        final var matches =  MessageCollectorActor.expectMatching(notifierActor, RoleChanged.class, 3);

        // check if the notifier got a role change from null to Follower
        var raftRoleChanged = matches.get(0);
        assertEquals(persistenceId, raftRoleChanged.memberId());
        assertNull(raftRoleChanged.oldRole());
        assertEquals(RaftRole.Follower, raftRoleChanged.newRole());

        // check if the notifier got a role change from Follower to Candidate
        raftRoleChanged = matches.get(1);
        assertEquals(persistenceId, raftRoleChanged.memberId());
        assertEquals(RaftRole.Follower, raftRoleChanged.oldRole());
        assertEquals(RaftRole.Candidate, raftRoleChanged.newRole());

        // check if the notifier got a role change from Candidate to Leader
        raftRoleChanged = matches.get(2);
        assertEquals(persistenceId, raftRoleChanged.memberId());
        assertEquals(RaftRole.Candidate, raftRoleChanged.oldRole());
        assertEquals(RaftRole.Leader, raftRoleChanged.newRole());

        var leaderStateChange = MessageCollectorActor.expectFirstMatching(notifierActor, LeaderStateChanged.class);

        assertEquals(raftRoleChanged.memberId(), leaderStateChange.leaderId());
        assertEquals(MockRaftActor.PAYLOAD_VERSION, leaderStateChange.leaderPayloadVersion());

        MessageCollectorActor.clearMessages(notifierActor);

        final var raftActor = raftActorRef.underlyingActor();
        final var newLeaderId = "new-leader";
        final short newLeaderVersion = 6;
        var follower = new Follower(raftActor.getRaftActorContext()) {
            @Override
            public RaftActorBehavior handleMessage(final ActorRef sender, final Object message) {
                setLeaderId(newLeaderId);
                setLeaderPayloadVersion(newLeaderVersion);
                return this;
            }
        };

        raftActor.newBehavior(follower);

        leaderStateChange = MessageCollectorActor.expectFirstMatching(notifierActor, LeaderStateChanged.class);
        assertEquals(persistenceId, leaderStateChange.memberId());
        assertNull(leaderStateChange.leaderId());

        raftRoleChanged = MessageCollectorActor.expectFirstMatching(notifierActor, RoleChanged.class);
        assertEquals(RaftRole.Leader, raftRoleChanged.oldRole());
        assertEquals(RaftRole.Follower, raftRoleChanged.newRole());

        MessageCollectorActor.clearMessages(notifierActor);

        raftActor.handleReceive("any");

        leaderStateChange = MessageCollectorActor.expectFirstMatching(notifierActor, LeaderStateChanged.class);
        assertEquals(persistenceId, leaderStateChange.memberId());
        assertEquals(newLeaderId, leaderStateChange.leaderId());
        assertEquals(newLeaderVersion, leaderStateChange.leaderPayloadVersion());

        MessageCollectorActor.clearMessages(notifierActor);

        raftActor.handleReceive("any");

        Uninterruptibles.sleepUninterruptibly(505, TimeUnit.MILLISECONDS);
        leaderStateChange = MessageCollectorActor.getFirstMatching(notifierActor, LeaderStateChanged.class);
        assertNull(leaderStateChange);
    }

    @Test
    void testRaftRoleChangeNotifierWhenRaftActorHasPeers() throws Exception {
        final var notifierActor = factory.createActor(MessageCollectorActor.props());
        MessageCollectorActor.waitUntilReady(notifierActor);

        final var config = new DefaultConfigParamsImpl();
        final var heartBeatInterval = 100L;
        config.setHeartBeatInterval(Duration.ofMillis(heartBeatInterval));
        config.setElectionTimeoutFactor(1);

        final var persistenceId = factory.generateActorId("notifier-");

        factory.createActor(MockRaftActor.builder()
            .id(persistenceId)
            .peerAddresses(Map.of("leader", "fake/path"))
            .config(config)
            .roleChangeNotifier(notifierActor)
            .props(stateDir()));

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
        var raftRoleChanged = matches.get(0);
        assertEquals(persistenceId, raftRoleChanged.memberId());
        assertNull(raftRoleChanged.oldRole());
        assertEquals(RaftRole.Follower, raftRoleChanged.newRole());

        // check if the notifier got a role change from Follower to Candidate
        raftRoleChanged = matches.get(1);
        assertEquals(persistenceId, raftRoleChanged.memberId());
        assertEquals(RaftRole.Follower, raftRoleChanged.oldRole());
        assertEquals(RaftRole.Candidate, raftRoleChanged.newRole());
    }

    @Test
    void testFakeSnapshotsForLeaderWithInRealSnapshots() throws Exception {
        final var persistenceId = factory.generateActorId("leader-");
        final var follower1Id = factory.generateActorId("follower-");

        final var followerActor1 = factory.createActor(MessageCollectorActor.props());

        final var config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);

        final var provider = mockProvider();
        final var mockActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.props(persistenceId, stateDir(),
            Map.of(follower1Id, followerActor1.path().toString()), config, provider), persistenceId);

        final var leaderActor = mockActorRef.underlyingActor();
        final var leaderContext = leaderActor.getRaftActorContext();
        final var leaderLog = leaderContext.getReplicatedLog();
        leaderLog.setCommitIndex(4);
        leaderLog.setLastApplied(4);
        leaderContext.setTermInfo(new TermInfo(1, persistenceId));

        leaderActor.waitForInitializeBehaviorComplete();

        // create 8 entries in the log - 0 to 4 are applied and will get picked up as part of the capture snapshot

        final var leader = new Leader(leaderContext);
        leaderActor.setCurrentBehavior(leader);
        assertSame(leader, leaderActor.getCurrentBehavior());

        leaderLog.resetToSnapshot(createSnapshot(0, 8, 1));

        assertEquals(8, leaderLog.size());

        final var snapshotState = new MockSnapshotState(List.of(
            new MockCommand("foo-0"),
            new MockCommand("foo-1"),
            new MockCommand("foo-2"),
            new MockCommand("foo-3"),
            new MockCommand("foo-4")));

        doReturn(snapshotState).when(leaderActor.snapshotCohortDelegate).takeSnapshot();

        final var snapshotStore = provider.snapshotStore();
        doNothing().when(snapshotStore).streamToInstall(any(), any(), any());

        leaderContext.getSnapshotManager().captureToInstall(EntryInfo.of(6, 1), 4, "xyzzy");
        verify(leaderActor.snapshotCohortDelegate).takeSnapshot();

        final var lastIncludedCaptor = ArgumentCaptor.forClass(EntryInfo.class);
        final var snapshotCaptor = ArgumentCaptor.<ToStorage<?>>captor();
        final var callbackCaptor = ArgumentCaptor.<RaftCallback<InstallableSnapshot>>captor();
        verify(snapshotStore).streamToInstall(lastIncludedCaptor.capture(), snapshotCaptor.capture(),
            callbackCaptor.capture());
        assertTrue(leaderContext.getSnapshotManager().isCapturing());

        assertEquals(8, leaderLog.size());

        assertSame(leader, leaderActor.getCurrentBehavior());
        //fake snapshot on index 5
        leaderActor.handleReceive(new AppendEntriesReply(follower1Id, 1, true, 5, 1, (short)0));

        assertEquals(8, leaderLog.size());

        //fake snapshot on index 6
        assertSame(leader, leaderActor.getCurrentBehavior());
        leaderActor.handleReceive(new AppendEntriesReply(follower1Id, 1, true, 6, 1, (short)0));
        assertEquals(8, leaderLog.size());

        assertSame(leader, leaderActor.getCurrentBehavior());

        assertEquals(8, leaderLog.size());

        // Finish snapshot commit
        try (var baos = new ByteArrayOutputStream()) {
            snapshotCaptor.getValue().writeTo(baos);
            callbackCaptor.getValue().invoke(null, new InstallableSnapshotSource(lastIncludedCaptor.getValue(),
                new PlainSnapshotSource(ByteArray.wrap(baos.toByteArray()))));
        }

        // The commit is needed to complete the snapshot creation process
        leaderContext.getSnapshotManager().commit(Instant.MIN);
        assertFalse(leaderContext.getSnapshotManager().isCapturing());

        // capture snapshot reply should remove the snapshotted entries only
        assertEquals(3, leaderLog.size());
        assertEquals(7, leaderLog.lastIndex());

        // add another non-replicated entry
        leaderLog.append(new DefaultLogEntry(8, 1, new MockCommand("foo-8")));

        //fake snapshot on index 7, since lastApplied = 7 , we would keep the last applied
        leaderActor.handleReceive(new AppendEntriesReply(follower1Id, 1, true, 7, 1, (short)0));
        assertEquals(2, leaderLog.size());
        assertEquals(8, leaderLog.lastIndex());
    }

    private static @NonNull Snapshot createSnapshot(final int start, final int end, final int term) {
        final var entries = new ArrayList<LogEntry>();
        for (int i = start; i < end; i++) {
            entries.add(new DefaultLogEntry(i, term, new MockCommand(Integer.toString(i))));
        }
        return Snapshot.create(null, entries, end - 1, term, -1, -1, TermInfo.INITIAL, null);
    }

    @Test
    void testFakeSnapshotsForFollowerWithInRealSnapshots() throws Exception {
        final var persistenceId = factory.generateActorId("follower-");
        final var leaderId = factory.generateActorId("leader-");

        final var leaderActor1 = factory.createActor(MessageCollectorActor.props());

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);

        final var dataPersistenceProvider = new TestPersistenceProvider();
        final var mockActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.props(persistenceId, stateDir(),
            Map.of(leaderId, leaderActor1.path().toString()), config, dataPersistenceProvider), persistenceId);

        final var followerActor = mockActorRef.underlyingActor();
        final var followerContext = followerActor.getRaftActorContext();
        final var followerLog = followerContext.getReplicatedLog();
        followerLog.setCommitIndex(4);
        followerLog.setLastApplied(4);
        followerContext.setTermInfo(new TermInfo(1, persistenceId));

        followerActor.waitForInitializeBehaviorComplete();

        Follower follower = new Follower(followerContext);
        followerActor.setCurrentBehavior(follower);
        assertSame(follower, followerActor.getCurrentBehavior());

        // create 6 entries in the log - 0 to 4 are applied and will get picked up as part of the capture snapshot
        followerLog.resetToSnapshot(createSnapshot(0, 6, 1));

        // log has indices 0-5
        assertEquals(6, followerLog.size());

        //snapshot on 4
        doReturn(new MockSnapshotState(List.of(
            new MockCommand("foo-0"),
            new MockCommand("foo-1"),
            new MockCommand("foo-2"),
            new MockCommand("foo-3"),
            new MockCommand("foo-4")))).when(followerActor.snapshotCohortDelegate).takeSnapshot();
        final var runnables = new ArrayList<Runnable>();
        dataPersistenceProvider.setActor(runnables::add);
        followerContext.getSnapshotManager().captureToInstall(EntryInfo.of(5, 1), 4, "xyzzy");

        assertEquals(6, followerLog.size());
        assertEquals(1, runnables.size());
        dataPersistenceProvider.setActor(Runnable::run);

        //fake snapshot on index 6
        var entries = List.<LogEntry>of(new DefaultLogEntry(6, 1, new MockCommand("foo-6")));
        followerActor.handleReceive(new AppendEntries(1, leaderId, 5, 1, entries, 5, 5, (short)0));
        assertEquals(7, followerLog.size());

        //fake snapshot on index 7
        assertInstanceOf(Follower.class, followerActor.getCurrentBehavior());

        entries = List.of(new DefaultLogEntry(7, 1, new MockCommand("foo-7")));
        followerActor.handleReceive(new AppendEntries(1, leaderId, 6, 1, entries, 6, 6, (short) 0));
        assertEquals(8, followerLog.size());

        assertInstanceOf(Follower.class, followerActor.getCurrentBehavior());

        runnables.getLast().run();
        assertTrue(followerContext.getSnapshotManager().isCapturing());

        // The commit is needed to complete the snapshot creation process
        followerContext.getSnapshotManager().commit(Instant.MIN);

        // capture snapshot reply should remove the snapshotted entries only till replicatedToAllIndex
        assertEquals(3, followerLog.size()); //indexes 5,6,7 left in the log
        assertEquals(7, followerLog.lastIndex());

        entries = List.of(new DefaultLogEntry(8, 1, new MockCommand("foo-7")));
        // send an additional entry 8 with leaderCommit = 7
        followerActor.handleReceive(new AppendEntries(1, leaderId, 7, 1, entries, 7, 7, (short) 0));

        // 7 and 8, as lastapplied is 7
        assertEquals(2, followerLog.size());
    }

    @Test
    void testFakeSnapshotsForLeaderWithInInitiateSnapshots() throws Exception {
        final var persistenceId = factory.generateActorId("leader-");
        final var follower1Id = factory.generateActorId("follower-");
        final var follower2Id = factory.generateActorId("follower-");

        final var followerActor1 = factory.createActor(MessageCollectorActor.props(), follower1Id);
        final var followerActor2 = factory.createActor(MessageCollectorActor.props(), follower2Id);

        final var config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);

        final var dataPersistenceProvider = new TestPersistenceProvider();
        final var mockActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.props(persistenceId, stateDir(),
            Map.of(
                follower1Id, followerActor1.path().toString(),
                follower2Id, followerActor2.path().toString()),
            config, dataPersistenceProvider), persistenceId);

        final var leaderActor = mockActorRef.underlyingActor();
        final var leaderContext = leaderActor.getRaftActorContext();
        final var leaderLog = leaderContext.getReplicatedLog();
        leaderLog.setCommitIndex(9);
        leaderLog.setLastApplied(9);
        leaderContext.setTermInfo(new TermInfo(1, persistenceId));

        leaderActor.waitForInitializeBehaviorComplete();

        final var leader = new Leader(leaderContext);
        leaderActor.setCurrentBehavior(leader);
        assertSame(leader, leaderActor.getCurrentBehavior());

        // create 5 entries in the log
        leaderLog.resetToSnapshot(createSnapshot(5, 10, 1));

        //set the snapshot index to 4 , 0 to 4 are snapshotted
        leaderLog.setSnapshotIndex(4);
        //setting replicatedToAllIndex = 9, for the log to clear
        leader.setReplicatedToAllIndex(9);
        assertEquals(5, leaderLog.size());
        assertSame(leader, leaderActor.getCurrentBehavior());

        leaderActor.handleReceive(new AppendEntriesReply(follower1Id, 1, true, 9, 1, (short) 0));
        assertEquals(5, leaderLog.size());
        assertSame(leader, leaderActor.getCurrentBehavior());

        doReturn(new MockSnapshotState(List.of(
            new MockCommand("foo-0"),
            new MockCommand("foo-1"),
            new MockCommand("foo-2"),
            new MockCommand("foo-3"),
            new MockCommand("foo-4")))).when(leaderActor.snapshotCohortDelegate).takeSnapshot();

        final var runnables = new ArrayList<Runnable>();
        dataPersistenceProvider.setActor(runnables::add);

        // set the 2nd follower nextIndex to 1 which has been snapshotted
        leaderActor.handleReceive(new AppendEntriesReply(follower2Id, 1, true, 0, 1, (short) 0));
        assertEquals(5, leaderLog.size());
        assertSame(leader, leaderActor.getCurrentBehavior());
        assertEquals(1, runnables.size());
        dataPersistenceProvider.setActor(Runnable::run);

        // simulate a real snapshot
        leaderActor.handleReceive(SendHeartBeat.INSTANCE);
        assertEquals(5, leaderLog.size());
        assertSame(leader, leaderActor.getCurrentBehavior());

        //reply from a slow follower does not initiate a fake snapshot
        leaderActor.handleReceive(new AppendEntriesReply(follower2Id, 1, true, 9, 1, (short) 0));
        assertEquals("Fake snapshot should not happen when Initiate is in progress", 5, leaderLog.size());

        runnables.getFirst().run();
        assertTrue(leaderContext.getSnapshotManager().isCapturing());

        assertEquals("Real snapshot didn't clear the log till replicatedToAllIndex", 0, leaderLog.size());

        //reply from a slow follower after should not raise errors
        leaderActor.handleReceive(new AppendEntriesReply(follower2Id, 1, true, 5, 1, (short) 0));
        assertEquals(0, leaderLog.size());
    }

    @Test
    void testRealSnapshotWhenReplicatedToAllIndexMinusOne() throws Exception {
        final var persistenceId = factory.generateActorId("leader-");
        final var config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);
        config.setSnapshotBatchCount(5);

        final var mockActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.props(persistenceId, stateDir(),
            Map.of("member1", "address"), config, new TestPersistenceProvider()), persistenceId);
        final var leaderActor = mockActorRef.underlyingActor();
        final var leaderContext = leaderActor.getRaftActorContext();
        final var leaderLog = leaderContext.getReplicatedLog();
        leaderLog.setCommitIndex(3);
        leaderLog.setLastApplied(3);
        leaderContext.setTermInfo(new TermInfo(1, persistenceId));

        leaderActor.waitForInitializeBehaviorComplete();
        for (int i = 0; i < 4; i++) {
            leaderLog.append(new DefaultLogEntry(i, 1, new MockCommand("A")));
        }

        final var leader = new Leader(leaderContext);
        leaderActor.setCurrentBehavior(leader);
        assertSame(leader, leaderActor.getCurrentBehavior());

        // Simulate an install snaphost to a follower.
        final var mockState = ByteState.of(fromObject("foo").toByteArray());
        leaderContext.getSnapshotManager().setSnapshotCohort((ByteStateSnapshotCohort) () -> mockState);

        leaderContext.getSnapshotManager().captureToInstall(leaderLog.lastMeta(), -1, "member1");

        // Trimming log in this scenario is a no-op
        assertEquals(-1, leaderLog.getSnapshotIndex());
        assertTrue(leaderContext.getSnapshotManager().isCapturing());
        assertEquals(-1, leader.getReplicatedToAllIndex());
    }

    @Test
    void testRealSnapshotWhenReplicatedToAllIndexNotInReplicatedLog() throws Exception {
        final var persistenceId = factory.generateActorId("leader-");
        final var config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);
        config.setSnapshotBatchCount(5);

        final var mockActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.props(persistenceId, stateDir(),
            Map.of("member1", "address"), config, new TestPersistenceProvider()), persistenceId);

        final var leaderActor = mockActorRef.underlyingActor();
        leaderActor.waitForRecoveryComplete();

        final var leaderContext = leaderActor.getRaftActorContext();
        final var leaderLog = leaderContext.getReplicatedLog();
        leaderLog.setCommitIndex(3);
        leaderLog.setLastApplied(3);
        leaderContext.setTermInfo(new TermInfo(1, persistenceId));
        leaderLog.setSnapshotIndex(3);

        leaderActor.waitForInitializeBehaviorComplete();
        final var leader = new Leader(leaderActor.getRaftActorContext());
        leaderActor.setCurrentBehavior(leader);
        leader.setReplicatedToAllIndex(3);
        assertInstanceOf(Leader.class, leaderActor.getCurrentBehavior());

        // Persist another entry, this will cause a CaptureSnapshot to be triggered
        doReturn(new MockSnapshotState(List.of())).when(leaderActor.snapshotCohortDelegate).takeSnapshot();
        leaderActor.submitCommand(new MockIdentifier("x"), new MockCommand("duh"), false);
        verify(leaderActor.snapshotCohortDelegate).takeSnapshot();

        // Trimming log in this scenario is a no-op
        assertEquals(3, leaderLog.getSnapshotIndex());
        assertTrue(leaderContext.getSnapshotManager().isCapturing());
        assertEquals(3, leader.getReplicatedToAllIndex());
    }

    @Test
    void testSwitchBehavior() throws Exception {
        final var persistenceId = factory.generateActorId("leader-");
        final var config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);
        config.setSnapshotBatchCount(5);

        final var mockActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.props(persistenceId, stateDir(),
            Map.of(), config, new TestPersistenceProvider()), persistenceId);

        final var leaderActor = mockActorRef.underlyingActor();

        leaderActor.waitForRecoveryComplete();

        leaderActor.handleReceive(new BecomeFollower(100));

        assertEquals(100, leaderActor.getRaftActorContext().currentTerm());
        assertInstanceOf(Follower.class, leaderActor.getCurrentBehavior());

        leaderActor.handleReceive(new BecomeLeader(110));

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
    void testUpdateConfigParam() {
        final var emptyConfig = new DefaultConfigParamsImpl();
        final var persistenceId = factory.generateActorId("follower-");
        final var actorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.props(persistenceId, stateDir(),
            Map.of("member1", "address"), emptyConfig, mockProvider()), persistenceId);
        final var mockRaftActor = actorRef.underlyingActor();
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
    void testGetSnapshot() throws Exception {
        TEST_LOG.info("testGetSnapshot starting");

        final var kit = new TestKit(getSystem());

        final var persistenceId = factory.generateActorId("test-actor-");
        final var config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        final long term = 3;

        final var actorDir = stateDir().resolve(persistenceId);
        new PropertiesTermInfoStore(persistenceId, actorDir).storeAndSetTerm(new TermInfo(term, "member-1"));
        try (var journal = new EntryJournalV1(persistenceId, actorDir, CompressionType.LZ4, false)) {
            journal.appendEntry(new DefaultLogEntry(0, term, new MockCommand("A")));
            journal.appendEntry(new DefaultLogEntry(1, term, new MockCommand("B")));
            journal.appendEntry(new DefaultLogEntry(2, term, new MockCommand("C")));
            journal.setApplyTo(2);
        }

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

        var reply = kit.expectMsgClass(GetSnapshotReply.class);

        assertEquals("getId", persistenceId, reply.id());
        var replySnapshot = reply.snapshot();
        assertEquals("getElectionTerm", new TermInfo(term, "member-1"), replySnapshot.termInfo());
        assertEquals("getLastAppliedIndex", 1L, replySnapshot.getLastAppliedIndex());
        assertEquals("getLastAppliedTerm", term, replySnapshot.getLastAppliedTerm());
        assertEquals("getLastIndex", 2L, replySnapshot.getLastIndex());
        assertEquals("getLastTerm", term, replySnapshot.getLastTerm());
        assertSame("getState", stateSnapshot, replySnapshot.state());
        assertEquals("getUnAppliedEntries size", 1, replySnapshot.getUnAppliedEntries().size());
        assertEquals("UnApplied entry index ", 2L, replySnapshot.getUnAppliedEntries().get(0).index());

        // Test with persistence disabled.
        mockRaftActor.setPersistence(false);
        reset(mockRaftActor.snapshotCohortDelegate);

        raftActorRef.tell(GetSnapshot.INSTANCE, kit.getRef());
        reply = kit.expectMsgClass(GetSnapshotReply.class);
        verify(mockRaftActor.snapshotCohortDelegate, never()).takeSnapshot();

        assertEquals("getId", persistenceId, reply.id());
        replySnapshot = reply.snapshot();
        assertEquals("getElectionTerm", new TermInfo(term, "member-1"), replySnapshot.termInfo());
        assertEquals("getLastAppliedIndex", -1L, replySnapshot.getLastAppliedIndex());
        assertEquals("getLastAppliedTerm", -1L, replySnapshot.getLastAppliedTerm());
        assertEquals("getLastIndex", -1L, replySnapshot.getLastIndex());
        assertEquals("getLastTerm", -1L, replySnapshot.getLastTerm());
        assertNull("getState type", replySnapshot.state());
        assertEquals("getUnAppliedEntries size", 0, replySnapshot.getUnAppliedEntries().size());

        TEST_LOG.info("testGetSnapshot ending");
    }

    @Test
    void testRestoreFromSnapshot() throws Exception {
        TEST_LOG.info("testRestoreFromSnapshot starting");

        var persistenceId = factory.generateActorId("test-actor-");
        final var config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        var snapshotUnappliedEntries = List.<LogEntry>of(new DefaultLogEntry(4, 1, new MockCommand("E")));

        int snapshotLastApplied = 3;
        int snapshotLastIndex = 4;

        final var snapshotState = new MockSnapshotState(List.of(
            new MockCommand("A"), new MockCommand("B"), new MockCommand("C"), new MockCommand("D")));

        var snapshot = Snapshot.create(snapshotState, snapshotUnappliedEntries,
                snapshotLastIndex, 1, snapshotLastApplied, 1, new TermInfo(1, "member-1"), null);

        var raftActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.builder().id(persistenceId)
            .config(config).restoreFromSnapshot(snapshot).props(stateDir())
            .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        var mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        final var snapshotFile = mockRaftActor.lastSnapshot();
        assertNotNull(snapshotFile);

        final var savedSnapshot = Snapshot.ofRaft(new TermInfo(1, "member-1"),
            snapshotFile.readRaftSnapshot(OBJECT_STREAMS), snapshotFile.lastIncluded(),
            snapshotFile.readSnapshot(MockSnapshotState.SUPPORT.reader()));

        assertEquals("getElectionTerm", snapshot.termInfo(), savedSnapshot.termInfo());
        assertEquals("getLastAppliedIndex", snapshot.getLastAppliedIndex(), savedSnapshot.getLastAppliedIndex());
        assertEquals("getLastAppliedTerm", snapshot.getLastAppliedTerm(), savedSnapshot.getLastAppliedTerm());
        assertEquals("getLastIndex", snapshot.getLastIndex(), savedSnapshot.getLastIndex());
        assertEquals("getLastTerm", snapshot.getLastTerm(), savedSnapshot.getLastTerm());
        assertEquals("getState", snapshot.state(), savedSnapshot.state());
        assertEquals("getUnAppliedEntries", snapshot.getUnAppliedEntries(), savedSnapshot.getUnAppliedEntries());

        final var context = mockRaftActor.getRaftActorContext();
        final var log = context.getReplicatedLog();
        assertEquals("Journal log size", 1, log.size());
        assertEquals("Last index", snapshotLastIndex, log.lastIndex());
        assertEquals("Last applied", snapshotLastApplied, log.getLastApplied());
        assertEquals("Commit index", snapshotLastApplied, log.getCommitIndex());
        assertEquals("Recovered state", snapshotState.state(), mockRaftActor.getState());
        assertEquals("Current term", new TermInfo(1, "member-1"), context.termInfo());

        // Test with data persistence disabled

        snapshot = Snapshot.create(null, List.of(), -1, -1, -1, -1, new TermInfo(5, "member-1"), null);

        persistenceId = factory.generateActorId("test-actor-");

        raftActorRef = factory.createTestActor(MockRaftActor.builder().id(persistenceId).config(config)
            .restoreFromSnapshot(snapshot).persistent(Optional.of(Boolean.FALSE))
            .props(stateDir())
            .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        final var secondContext = mockRaftActor.getRaftActorContext();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(
            () -> assertEquals("Current term", new TermInfo(5, "member-1"), secondContext.termInfo()));

        TEST_LOG.info("testRestoreFromSnapshot ending");
    }

    @Test
    void testRestoreFromSnapshotWithRecoveredData() throws Exception {
        TEST_LOG.info("testRestoreFromSnapshotWithRecoveredData starting");

        final var persistenceId = factory.generateActorId("test-actor-");
        final var config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        final var state = List.of(new MockCommand("A"));
        final var snapshot = Snapshot.create(ByteState.of(fromObject(state).toByteArray()),
                List.of(), 5, 2, 5, 2, new TermInfo(2, "member-1"), null);

        try (var journal = new EntryJournalV1(persistenceId, stateDir().resolve(persistenceId), CompressionType.NONE,
                false)) {
            journal.appendEntry(new DefaultLogEntry(0, 1, new MockCommand("B")));
        }

        final var raftActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.builder().id(persistenceId)
            .config(config).restoreFromSnapshot(snapshot).props(stateDir())
            .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        final var mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verify(mockRaftActor.snapshotCohortDelegate, never()).applySnapshot(any());

        final var context = mockRaftActor.getRaftActorContext();
        final var log = context.getReplicatedLog();
        assertEquals("Journal log size", 1, log.size());
        assertEquals("Last index", 0, log.lastIndex());
        assertEquals("Last applied", -1, log.getLastApplied());
        assertEquals("Commit index", -1, log.getCommitIndex());
        assertEquals("Current term", TermInfo.INITIAL, context.termInfo());

        TEST_LOG.info("testRestoreFromSnapshotWithRecoveredData ending");
    }

    @Test
    void testNonVotingOnRecovery() throws Exception {
        TEST_LOG.info("testNonVotingOnRecovery starting");

        final var config = new DefaultConfigParamsImpl();
        config.setElectionTimeoutFactor(1);
        config.setHeartBeatInterval(Duration.ofMillis(1));

        final var persistenceId = factory.generateActorId("test-actor-");

        try (var journal = new EntryJournalV1(persistenceId, stateDir().resolve(persistenceId), CompressionType.NONE,
                true)) {
            journal.appendEntry(new DefaultLogEntry(0, 1, new VotingConfig(new ServerInfo(persistenceId, false))));
        }

        final var raftActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.builder().id(persistenceId)
            .config(config).props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        final var mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForInitializeBehaviorComplete();

        // Sleep a bit and verify it didn't get an election timeout and schedule an election.

        Uninterruptibles.sleepUninterruptibly(400, TimeUnit.MILLISECONDS);
        assertEquals("getRaftState", RaftRole.Follower, mockRaftActor.getRaftState());

        TEST_LOG.info("testNonVotingOnRecovery ending");
    }

    @Test
    void testLeaderTransitioning() {
        TEST_LOG.info("testLeaderTransitioning starting");

        final var notifierActor = factory.createActor(MessageCollectorActor.props());

        final var config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        final var persistenceId = factory.generateActorId("test-actor-");

        final var raftActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.builder().id(persistenceId)
            .config(config).roleChangeNotifier(notifierActor).props(stateDir())
            .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        final var mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForInitializeBehaviorComplete();

        raftActorRef.tell(new AppendEntries(1L, "leader", 0L, 1L, List.of(), 0L, -1L, (short)1), ActorRef.noSender());
        var leaderStateChange = MessageCollectorActor.expectFirstMatching(notifierActor, LeaderStateChanged.class);
        assertEquals("leaderId", "leader", leaderStateChange.leaderId());

        MessageCollectorActor.clearMessages(notifierActor);

        raftActorRef.tell(new LeaderTransitioning("leader"), ActorRef.noSender());

        leaderStateChange = MessageCollectorActor.expectFirstMatching(notifierActor, LeaderStateChanged.class);
        assertEquals(persistenceId, leaderStateChange.memberId());
        assertNull(leaderStateChange.leaderId());

        TEST_LOG.info("testLeaderTransitioning ending");
    }

    @Test
    void testReplicateWithPersistencePending() throws Exception {
        final var leaderId = factory.generateActorId("leader-");
        final var followerId = factory.generateActorId("follower-");

        final var followerActor = factory.createActor(MessageCollectorActor.props());

        final var config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);

        final var provider = mockProvider();
        final var leaderActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.props(leaderId, stateDir(),
            Map.of(followerId, followerActor.path().toString()), config, provider), leaderId);
        final var leaderActor = leaderActorRef.underlyingActor();
        leaderActor.waitForInitializeBehaviorComplete();

        final var leaderContext = leaderActor.getRaftActorContext();
        leaderContext.setTermInfo(new TermInfo(1, leaderId));

        final var leader = new Leader(leaderContext);
        leaderActor.setCurrentBehavior(leader);

        leaderActor.submitCommand(new MockIdentifier("1"), new MockCommand("1"), false);

        final var leaderLog = leaderContext.getReplicatedLog();
        final var storedMeta = leaderLog.lookupStoredMeta(0);
        assertNotNull(storedMeta);
        assertFalse(storedMeta.durable());
        assertEquals(-1, leaderLog.getCommitIndex());

        leaderActor.handleReceive(new AppendEntriesReply(followerId, 1, true, 0, 1, (short)0));
        assertEquals("getCommitIndex", -1, leaderLog.getCommitIndex());

        final var entryCaptor = ArgumentCaptor.forClass(ReplicatedLogEntry.class);
        final var callbackCaptor = ArgumentCaptor.<RaftCallback<Long>>captor();
        verify(provider.entryStore()).startPersistEntry(entryCaptor.capture(), callbackCaptor.capture());

        final var entry = entryCaptor.getValue();
        assertSame(storedMeta.meta(), entry);
        callbackCaptor.getValue().invoke(null, 1L);

        assertEquals("getCommitIndex", 0, leaderLog.getCommitIndex());
        assertEquals("getLastApplied", 0, leaderLog.getLastApplied());
    }

    @Test
    void testReplicateWithBatchHint() throws Exception {
        final var leaderId = factory.generateActorId("leader-");
        final var followerId = factory.generateActorId("follower-");
        final var followerActor = factory.createActor(MessageCollectorActor.props());

        final var config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        config.setIsolatedLeaderCheckInterval(ONE_DAY);

        final var leaderActorRef = factory.<MockRaftActor>createTestActor(MockRaftActor.props(leaderId, stateDir(),
            Map.of(followerId, followerActor.path().toString()), config), leaderId);
        final var leaderActor = leaderActorRef.underlyingActor();
        leaderActor.waitForInitializeBehaviorComplete();

        final var leaderContext = leaderActor.getRaftActorContext();
        leaderContext.setTermInfo(new TermInfo(1, leaderId));

        final var leader = new Leader(leaderContext);
        leaderActor.setCurrentBehavior(leader);

        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        MessageCollectorActor.clearMessages(followerActor);

        leaderActor.handleReceive(new AppendEntriesReply(followerId, 1, true, -1, -1, (short)0));

        leaderActor.submitCommand(new MockIdentifier("1"), new MockCommand("1"), true);
        MessageCollectorActor.assertNoneMatching(followerActor, AppendEntries.class, 500);

        leaderActor.submitCommand(new MockIdentifier("2"), new MockCommand("2"), true);
        MessageCollectorActor.assertNoneMatching(followerActor, AppendEntries.class, 500);

        leaderActor.submitCommand(new MockIdentifier("3"), new MockCommand("3"), false);
        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("AppendEntries size", 3, appendEntries.getEntries().size());
    }

    @Test
    void testApplyStateRace() throws Exception {
        final var leaderId = factory.generateActorId("leader-");
        final var followerId = factory.generateActorId("follower-");
        final var config = new DefaultConfigParamsImpl();
        config.setIsolatedLeaderCheckInterval(ONE_DAY);
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        final var mockFollowerActorRef = factory.createActor(MessageCollectorActor.props());

        final var builder = TestRaftActor.newBuilder()
            .id(leaderId)
            .peerAddresses(Map.of(followerId, mockFollowerActorRef.path().toString()))
            .config(config)
            .collectorActor(factory.createActor(MessageCollectorActor.props(),
                factory.generateActorId(leaderId + "-collector")));

        final var leaderActorRef = factory.<MockRaftActor>createTestActor(builder.props(stateDir()), leaderId);
        final var leaderActor = leaderActorRef.underlyingActor();
        leaderActor.waitForInitializeBehaviorComplete();

        final var leaderContext = leaderActor.getRaftActorContext();
        leaderContext.setTermInfo(new TermInfo(1, leaderId));
        final var leader = new Leader(leaderContext);
        leaderActor.setCurrentBehavior(leader);

        leader.getFollower(followerId).setNextIndex(0);
        leader.getFollower(followerId).setMatchIndex(-1);

        // Hitting this is flimsy so run multiple times to improve the chance of things blowing up while breaking actor
        // containment.
        // We have persistence enabled, which means we have a JournalWriteTask running in background, just as this test
        // used to mock with an executor
        final var message = new TestPersist(leaderActorRef, new MockIdentifier("1"), new MockCommand("1"));
        for (int i = 0; i < 100; i++) {
            leaderActorRef.tell(message, ActorRef.noSender());
            leaderActorRef.tell(new AppendEntriesReply(followerId, 1, true, i, 1, (short) 5), mockFollowerActorRef);
        }

        await("Persistence callback.").atMost(500, TimeUnit.SECONDS)
            .untilAsserted(() -> assertEquals(100, leaderActor.getState().size()));
    }

    @Test
    void recoveryRestoresAndSnapshots() throws Exception {
        final var persistenceId = factory.generateActorId("recover-");
        final var config = new DefaultConfigParamsImpl();
        config.setHeartBeatInterval(ONE_DAY);
        final var ref = factory.<MockRaftActor>createTestActor(MockRaftActor.builder()
            .id(persistenceId)
            .config(config)
            .persistent(Optional.of(true))
            .restoreFromSnapshot(Snapshot.ofTermLeader(new MockSnapshotState(List.of(3, 14, 42)), EntryInfo.of(3, 1),
                new TermInfo(3), null))
            .props(stateDir()));

        final var actor = ref.underlyingActor();
        actor.waitForRecoveryComplete();

        final var snapshot = actor.lastSnapshot();
        assertNotNull(snapshot);

        assertEquals(EntryInfo.of(3, 1), snapshot.lastIncluded());
        assertEquals(new MockSnapshotState(List.of(3, 14, 42)),
            snapshot.readSnapshot(MockSnapshotState.SUPPORT.reader()));
    }
}
