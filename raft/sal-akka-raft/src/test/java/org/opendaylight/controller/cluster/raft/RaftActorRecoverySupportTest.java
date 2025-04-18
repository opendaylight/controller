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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.util.concurrent.MoreExecutors;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.persistence.RecoveryCompleted;
import org.apache.pekko.persistence.SnapshotMetadata;
import org.apache.pekko.persistence.SnapshotOffer;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.spi.StateCommand;
import org.opendaylight.raft.api.TermInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for RaftActorRecoverySupport.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class RaftActorRecoverySupportTest {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorRecoverySupportTest.class);

    private final DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
    private final String localId = "leader";

    @Mock
    private DataPersistenceProvider mockPersistence;
    @Mock
    private RaftActorRecoveryCohort mockCohort;
    @Mock
    private RaftActor mockActor;
    @Mock
    private RaftActorSnapshotCohort<?> mockSnapshotCohort;
    @TempDir
    private Path stateDir;

    private ActorRef mockActorRef;
    private ActorSystem mockActorSystem;
    private LocalAccess localAccess;
    private RaftActorContext context;

    private RaftActorRecoverySupport support;

    @BeforeEach
    void setup() {
        mockActorSystem = ActorSystem.create();
        mockActorRef = mockActorSystem.actorOf(Props.create(DoNothingActor.class));
        localAccess = new LocalAccess(localId, stateDir);
        context = new RaftActorContextImpl(mockActorRef, null, localAccess, Map.of(), configParams, (short) 0,
            mockPersistence, (identifier, entry) -> { }, MoreExecutors.directExecutor());
    }

    @AfterEach
    void tearDown() {
        TestKit.shutdownActorSystem(mockActorSystem);
    }

    private void sendMessageToSupport(final Object message) {
        sendMessageToSupport(message, false);
    }

    private void sendMessageToSupport(final Object message, final boolean expComplete) {
        boolean complete = support.handleRecoveryMessage(mockActor, message);
        assertEquals("complete", expComplete, complete);
    }

    @Test
    void testOnReplicatedLogEntry() {
        support = new RaftActorRecoverySupport(localAccess, context, mockCohort, true);

        final var logEntry = new SimpleReplicatedLogEntry(1, 1, new MockCommand("1", 5));

        sendMessageToSupport(logEntry);

        final var log = context.getReplicatedLog();
        assertEquals("Journal log size", 1, log.size());
        assertEquals("Journal data size", 5, log.dataSize());
        assertEquals("Last index", 1, log.lastIndex());
        assertEquals("Last applied", -1, log.getLastApplied());
        assertEquals("Commit index", -1, log.getCommitIndex());
        assertEquals("Snapshot term", -1, log.getSnapshotTerm());
        assertEquals("Snapshot index", -1, log.getSnapshotIndex());
    }

    @Test
    void testOnApplyJournalEntries() {
        support = new RaftActorRecoverySupport(localAccess, context, mockCohort, true);

        configParams.setJournalRecoveryLogBatchSize(5);

        final var replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new SimpleReplicatedLogEntry(0, 1, new MockCommand("0")));
        replicatedLog.append(new SimpleReplicatedLogEntry(1, 1, new MockCommand("1")));
        replicatedLog.append(new SimpleReplicatedLogEntry(2, 1, new MockCommand("2")));
        replicatedLog.append(new SimpleReplicatedLogEntry(3, 1, new MockCommand("3")));
        replicatedLog.append(new SimpleReplicatedLogEntry(4, 1, new MockCommand("4")));
        replicatedLog.append(new SimpleReplicatedLogEntry(5, 1, new MockCommand("5")));

        sendMessageToSupport(new ApplyJournalEntries(2));

        assertEquals("Last applied", 2, replicatedLog.getLastApplied());
        assertEquals("Commit index", 2, replicatedLog.getCommitIndex());

        sendMessageToSupport(new ApplyJournalEntries(4));

        assertEquals("Last applied", 4, replicatedLog.getLastApplied());
        assertEquals("Last applied", 4, replicatedLog.getLastApplied());

        sendMessageToSupport(new ApplyJournalEntries(5));

        assertEquals("Last index", 5, replicatedLog.lastIndex());
        assertEquals("Last applied", 5, replicatedLog.getLastApplied());
        assertEquals("Commit index", 5, replicatedLog.getCommitIndex());
        assertEquals("Snapshot term", -1, replicatedLog.getSnapshotTerm());
        assertEquals("Snapshot index", -1, replicatedLog.getSnapshotIndex());

        final var inOrder = Mockito.inOrder(mockCohort);
        inOrder.verify(mockCohort).startLogRecoveryBatch(5);

        for (int i = 0; i < replicatedLog.size() - 1; i++) {
            inOrder.verify(mockCohort).appendRecoveredCommand((StateCommand) replicatedLog.get(i).command());
        }

        inOrder.verify(mockCohort).applyCurrentLogRecoveryBatch();
        inOrder.verify(mockCohort).startLogRecoveryBatch(5);
        inOrder.verify(mockCohort).appendRecoveredCommand(
            (StateCommand) replicatedLog.get(replicatedLog.size() - 1).command());

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void testIncrementalRecovery() throws Exception {
        support = new RaftActorRecoverySupport(localAccess, context, mockCohort, true);

        int recoverySnapshotInterval = 3;
        int numberOfEntries = 5;
        configParams.setRecoverySnapshotIntervalSeconds(recoverySnapshotInterval);
        context.getSnapshotManager().setSnapshotCohort(mockSnapshotCohort);
        doReturn(new MockSnapshotState(List.of())).when(mockSnapshotCohort).takeSnapshot();

        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            final var replicatedLog = context.getReplicatedLog();

            for (int i = 0; i <= numberOfEntries; i++) {
                replicatedLog.append(new SimpleReplicatedLogEntry(i, 1,
                    new MockCommand(String.valueOf(i))));
            }

            final var entryCount = new AtomicInteger();
            final var applyEntriesFuture = executor.scheduleAtFixedRate(() -> {
                int run = entryCount.getAndIncrement();
                LOG.info("Sending entry number {}", run);
                sendMessageToSupport(new ApplyJournalEntries(run));
            }, 0, 1, TimeUnit.SECONDS);

            executor.schedule(() -> applyEntriesFuture.cancel(false), numberOfEntries, TimeUnit.SECONDS).get();

            verify(mockSnapshotCohort, times(1)).takeSnapshot();
        }
    }

    @Test
    void testOnSnapshotOffer() {
        support = new RaftActorRecoverySupport(localAccess, context, mockCohort, true);

        var replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new SimpleReplicatedLogEntry(1, 1, new MockCommand("1")));
        replicatedLog.append(new SimpleReplicatedLogEntry(2, 1, new MockCommand("2")));
        replicatedLog.append(new SimpleReplicatedLogEntry(3, 1, new MockCommand("3")));

        final var unAppliedEntry1 = new SimpleReplicatedLogEntry(4, 1, new MockCommand("4", 4));
        final var unAppliedEntry2 = new SimpleReplicatedLogEntry(5, 1, new MockCommand("5", 5));

        final long lastAppliedDuringSnapshotCapture = 3;
        final long lastIndexDuringSnapshotCapture = 5;
        final long electionTerm = 2;
        final var electionVotedFor = "member-2";

        final var snapshotState = new MockSnapshotState(List.of(new MockCommand("1")));
        final var snapshot = Snapshot.create(snapshotState,
                List.of(unAppliedEntry1, unAppliedEntry2), lastIndexDuringSnapshotCapture, 1,
                lastAppliedDuringSnapshotCapture, 1, new TermInfo(electionTerm, electionVotedFor), null);

        final var metadata = new SnapshotMetadata("test", 6, 12345);
        final var snapshotOffer = new SnapshotOffer(metadata, snapshot);

        sendMessageToSupport(snapshotOffer);

        assertEquals("Journal log size", 2, replicatedLog.size());
        assertEquals("Journal data size", 9, replicatedLog.dataSize());
        assertEquals("Last index", lastIndexDuringSnapshotCapture, replicatedLog.lastIndex());
        assertEquals("Last applied", lastAppliedDuringSnapshotCapture, replicatedLog.getLastApplied());
        assertEquals("Commit index", lastAppliedDuringSnapshotCapture, replicatedLog.getCommitIndex());
        assertEquals("Snapshot term", 1, replicatedLog.getSnapshotTerm());
        assertEquals("Snapshot index", lastAppliedDuringSnapshotCapture, replicatedLog.getSnapshotIndex());
        assertEquals("Election term", new TermInfo(electionTerm, electionVotedFor), context.termInfo());
        assertFalse("Dynamic server configuration", context.isDynamicServerConfigurationInUse());

        verify(mockCohort).applyRecoveredSnapshot(snapshotState);
    }

    @Test
    void testOnRecoveryCompletedWithRemainingBatch() {
        support = new RaftActorRecoverySupport(localAccess, context, mockCohort, true);

        final var replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new SimpleReplicatedLogEntry(0, 1, new MockCommand("0")));
        replicatedLog.append(new SimpleReplicatedLogEntry(1, 1, new MockCommand("1")));

        sendMessageToSupport(new ApplyJournalEntries(1));

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        assertEquals("Last applied", 1, replicatedLog.getLastApplied());
        assertEquals("Commit index", 1, replicatedLog.getCommitIndex());

        InOrder inOrder = Mockito.inOrder(mockCohort);
        inOrder.verify(mockCohort).startLogRecoveryBatch(anyInt());

        for (int i = 0; i < replicatedLog.size(); i++) {
            inOrder.verify(mockCohort).appendRecoveredCommand((StateCommand) replicatedLog.get(i).command());
        }

        inOrder.verify(mockCohort).applyCurrentLogRecoveryBatch();
        inOrder.verify(mockCohort).getRestoreFromSnapshot();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void testOnRecoveryCompletedWithNoRemainingBatch() {
        support = new RaftActorRecoverySupport(localAccess, context, mockCohort, false);

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        verify(mockCohort).getRestoreFromSnapshot();
        verifyNoMoreInteractions(mockCohort);
    }

    @Test
    void testOnDeleteEntries() {
        support = new RaftActorRecoverySupport(localAccess, context, mockCohort, true);

        ReplicatedLog replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new SimpleReplicatedLogEntry(0, 1, new MockCommand("0")));
        replicatedLog.append(new SimpleReplicatedLogEntry(1, 1, new MockCommand("1")));
        replicatedLog.append(new SimpleReplicatedLogEntry(2, 1, new MockCommand("2")));

        sendMessageToSupport(new DeleteEntries(1));

        assertEquals("Journal log size", 1, context.getReplicatedLog().size());
        assertEquals("Last index", 0, context.getReplicatedLog().lastIndex());
    }

    @Test
    void testUpdateElectionTerm() {
        support = new RaftActorRecoverySupport(localAccess, context, mockCohort, false);

        sendMessageToSupport(new UpdateElectionTerm(5, "member2"));

        assertEquals("Current term", new TermInfo(5, "member2"), context.termInfo());
    }

    @Test
    void testDataRecoveredWithPersistenceDisabled() {
        support = new RaftActorRecoverySupport(localAccess, context, mockCohort, false);

        doReturn(10L).when(mockActor).lastSequenceNr();

        Snapshot snapshot = Snapshot.create(new MockSnapshotState(List.of(new MockCommand("1"))),
                List.of(), 3, 1, 3, 1, new TermInfo(-1), null);
        SnapshotOffer snapshotOffer = new SnapshotOffer(new SnapshotMetadata("test", 6, 12345), snapshot);

        sendMessageToSupport(snapshotOffer);

        sendMessageToSupport(new UpdateElectionTerm(5, "member2"));

        sendMessageToSupport(new SimpleReplicatedLogEntry(4, 1, new MockCommand("4")));
        sendMessageToSupport(new SimpleReplicatedLogEntry(5, 1, new MockCommand("5")));

        sendMessageToSupport(new ApplyJournalEntries(4));

        sendMessageToSupport(new DeleteEntries(5));

        final var log = context.getReplicatedLog();
        assertEquals("Journal log size", 0, log.size());
        assertEquals("Last index", -1, log.lastIndex());
        assertEquals("Last applied", -1, log.getLastApplied());
        assertEquals("Commit index", -1, log.getCommitIndex());
        assertEquals("Snapshot term", -1, log.getSnapshotTerm());
        assertEquals("Snapshot index", -1, log.getSnapshotIndex());

        assertEquals("Current term", new TermInfo(5, "member2"), context.termInfo());

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        verify(mockCohort, never()).applyRecoveredSnapshot(any());
        verify(mockCohort, never()).getRestoreFromSnapshot();
        verifyNoMoreInteractions(mockCohort);

//        verify(mockPersistentProvider).deleteMessages(10L);
    }

    @Test
    void testNoDataRecoveredWithPersistenceDisabled() {
        support = new RaftActorRecoverySupport(localAccess, context, mockCohort, false);

        sendMessageToSupport(new UpdateElectionTerm(5, "member2"));

        assertEquals("Current term", new TermInfo(5, "member2"), context.termInfo());

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        verify(mockCohort).getRestoreFromSnapshot();
        verifyNoMoreInteractions(mockCohort);
    }

    @Test
    void testServerConfigurationPayloadApplied() {
        support = new RaftActorRecoverySupport(localAccess, context, mockCohort, true);

        String follower1 = "follower1";
        String follower2 = "follower2";
        String follower3 = "follower3";

        context.addToPeers(follower1, null, VotingState.VOTING);
        context.addToPeers(follower2, null, VotingState.VOTING);

        //add new Server
        var obj = new ClusterConfig(
                new ServerInfo(localId, true),
                new ServerInfo(follower1, true),
                new ServerInfo(follower2, false),
                new ServerInfo(follower3, true));

        sendMessageToSupport(new SimpleReplicatedLogEntry(0, 1, obj));

        //verify new peers
        assertTrue("Dynamic server configuration", context.isDynamicServerConfigurationInUse());
        assertEquals("New peer Ids", Set.of(follower1, follower2, follower3), Set.copyOf(context.getPeerIds()));
        assertTrue("follower1 isVoting", context.getPeerInfo(follower1).isVoting());
        assertFalse("follower2 isVoting", context.getPeerInfo(follower2).isVoting());
        assertTrue("follower3 isVoting", context.getPeerInfo(follower3).isVoting());

        sendMessageToSupport(new ApplyJournalEntries(0));

        verify(mockCohort, never()).startLogRecoveryBatch(anyInt());
        verify(mockCohort, never()).appendRecoveredCommand(any());

        //remove existing follower1
        obj = new ClusterConfig(
                new ServerInfo(localId, true),
                new ServerInfo("follower2", true),
                new ServerInfo("follower3", true));

        sendMessageToSupport(new SimpleReplicatedLogEntry(1, 1, obj));

        //verify new peers
        assertTrue("Dynamic server configuration", context.isDynamicServerConfigurationInUse());
        assertEquals("New peer Ids", Set.of(follower2, follower3), Set.copyOf(context.getPeerIds()));
    }

    @Test
    void testServerConfigurationPayloadAppliedWithPersistenceDisabled() {
        support = new RaftActorRecoverySupport(localAccess, context, mockCohort, false);

        final var obj = new ClusterConfig(new ServerInfo(localId, true), new ServerInfo("follower", true));

        sendMessageToSupport(new SimpleReplicatedLogEntry(0, 1, obj));

        //verify new peers
        assertEquals("New peer Ids", Set.of("follower"), Set.copyOf(context.getPeerIds()));
    }

    @Test
    void testOnSnapshotOfferWithServerConfiguration() {
        support = new RaftActorRecoverySupport(localAccess, context, mockCohort, true);

        long electionTerm = 2;
        String electionVotedFor = "member-2";
        final var serverPayload = new ClusterConfig(
                new ServerInfo(localId, true),
                new ServerInfo("follower1", true),
                new ServerInfo("follower2", true));

        MockSnapshotState snapshotState = new MockSnapshotState(List.of(new MockCommand("1")));
        Snapshot snapshot = Snapshot.create(snapshotState, List.of(),
                -1, -1, -1, -1, new TermInfo(electionTerm, electionVotedFor), serverPayload);

        SnapshotMetadata metadata = new SnapshotMetadata("test", 6, 12345);
        SnapshotOffer snapshotOffer = new SnapshotOffer(metadata, snapshot);

        sendMessageToSupport(snapshotOffer);

        assertEquals("Journal log size", 0, context.getReplicatedLog().size());
        assertEquals("Election term", new TermInfo(electionTerm, electionVotedFor), context.termInfo());
        assertTrue("Dynamic server configuration", context.isDynamicServerConfigurationInUse());
        assertEquals("Peer List", Set.of("follower1", "follower2"), Set.copyOf(context.getPeerIds()));
    }
}