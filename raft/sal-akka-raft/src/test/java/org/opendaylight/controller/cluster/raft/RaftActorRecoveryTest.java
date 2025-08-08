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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.DefaultLogEntry;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;
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
class RaftActorRecoveryTest {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorRecoveryTest.class);

    private final DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
    private final String localId = "leader";

    @Mock
    private SnapshotStore snapshotStore;
    @Mock
    private PersistenceProvider persistence;
    @Mock
    private RaftActor raftActor;
    @Mock
    private RaftActorRecoveryCohort recoveryCohort;
    @Mock
    private RaftActorSnapshotCohort<?> snapshotCohort;
    @TempDir
    private Path stateDir;

    private ActorRef mockActorRef;
    private ActorSystem mockActorSystem;
    private LocalAccess localAccess;

    private PeerInfos peerInfos;

    private PekkoRecoverySupport<?> support;
    private PekkoRecovery<?> recovery;

    @BeforeEach
    void setup() {
        localAccess = new LocalAccess(localId, stateDir);
        doReturn(localId).when(raftActor).memberId();
        doReturn(localAccess).when(raftActor).localAccess();
        doReturn(persistence).when(raftActor).persistence();
        doReturn(snapshotStore).when(persistence).snapshotStore();

        mockActorSystem = ActorSystem.create();
        mockActorRef = mockActorSystem.actorOf(Props.create(DoNothingActor.class));

        peerInfos = new PeerInfos(localId, Map.of());

        support = new PekkoRecoverySupport<>(raftActor, snapshotCohort, recoveryCohort, configParams);
    }

    private RaftActorContext createContext() {
        final var ret = new RaftActorContextImpl(mockActorRef, null, localAccess, peerInfos, configParams, (short) 0,
            AbstractActorTest.OBJECT_STREAMS, persistence, (identifier, entry) -> { });
        ret.getReplicatedLog().resetToLog(recovery.recoveryLog);
        return ret;
    }

    @AfterEach
    void tearDown() {
        TestKit.shutdownActorSystem(mockActorSystem);
    }

    private void sendMessageToSupport(final Object message) {
        sendMessageToSupport(message, false);
    }

    private void sendMessageToSupport(final Object message, final boolean expComplete) {
        final var recoveredLog = recovery.handleRecoveryMessage(message);
        if (expComplete) {
            assertNotNull(recoveredLog);
        } else {
            assertNull(recoveredLog);
        }
    }

    @Test
    void testOnReplicatedLogEntry() throws Exception {
        recovery = support.recoverToPersistent();

        final var logEntry = new SimpleReplicatedLogEntry(1, 1, new MockCommand("1", 5));

        sendMessageToSupport(logEntry);

        final var recoveryLog = recovery.recoveryLog;
        assertEquals("Journal log size", 1, recoveryLog.size());
        assertEquals("Journal data size", 5, recoveryLog.dataSize());
        assertEquals("Last index", 1, recoveryLog.lastIndex());
        assertEquals("Last applied", -1, recoveryLog.getLastApplied());
        assertEquals("Commit index", -1, recoveryLog.getCommitIndex());
        assertEquals("Snapshot term", -1, recoveryLog.getSnapshotTerm());
        assertEquals("Snapshot index", -1, recoveryLog.getSnapshotIndex());
    }

    @Test
    void testOnApplyJournalEntries() throws Exception {
        configParams.setJournalRecoveryLogBatchSize(5);

        recovery = support.recoverToPersistent();

        final var replicatedLog = recovery.recoveryLog;
        replicatedLog.append(new DefaultLogEntry(0, 1, new MockCommand("0")));
        replicatedLog.append(new DefaultLogEntry(1, 1, new MockCommand("1")));
        replicatedLog.append(new DefaultLogEntry(2, 1, new MockCommand("2")));
        replicatedLog.append(new DefaultLogEntry(3, 1, new MockCommand("3")));
        replicatedLog.append(new DefaultLogEntry(4, 1, new MockCommand("4")));
        replicatedLog.append(new DefaultLogEntry(5, 1, new MockCommand("5")));

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

        final var inOrder = Mockito.inOrder(recoveryCohort);
        inOrder.verify(recoveryCohort).startLogRecoveryBatch(5);

        for (int i = 0; i < replicatedLog.size() - 1; i++) {
            inOrder.verify(recoveryCohort).appendRecoveredCommand((StateCommand) replicatedLog.lookup(i).command());
        }

        inOrder.verify(recoveryCohort).applyCurrentLogRecoveryBatch();
        inOrder.verify(recoveryCohort).startLogRecoveryBatch(5);
        inOrder.verify(recoveryCohort).appendRecoveredCommand(
            (StateCommand) replicatedLog.lookup(replicatedLog.size() - 1).command());

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    // FIXME: inject a Ticker and eliminate explicit sleeps
    void testIncrementalRecovery() throws Exception {
        int recoverySnapshotInterval = 3;
        configParams.setRecoverySnapshotIntervalSeconds(recoverySnapshotInterval);
        doReturn(peerInfos).when(raftActor).peerInfos();
        doReturn(MockSnapshotState.SUPPORT).when(snapshotCohort).support();

        recovery = support.recoverToPersistent();
        final var replicatedLog = recovery.recoveryLog;

        int numberOfEntries = 5;
        doReturn(new MockSnapshotState(List.of())).when(snapshotCohort).takeSnapshot();

        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            for (int i = 0; i <= numberOfEntries; i++) {
                replicatedLog.append(new DefaultLogEntry(i, 1, new MockCommand(String.valueOf(i))));
            }

            final var entryCount = new AtomicInteger();
            final var applyEntriesFuture = executor.scheduleAtFixedRate(() -> {
                int run = entryCount.getAndIncrement();
                LOG.info("Sending entry number {}", run);
                sendMessageToSupport(new ApplyJournalEntries(run));
            }, 0, 1, TimeUnit.SECONDS);

            executor.schedule(() -> applyEntriesFuture.cancel(false), numberOfEntries, TimeUnit.SECONDS).get();

            verify(snapshotCohort, times(1)).takeSnapshot();
        }
    }

    @Test
    void testOnSnapshotOffer() throws Exception {
        doReturn(MockSnapshotState.SUPPORT).when(snapshotCohort).support();
        recovery = support.recoverToPersistent();

        final var recoveryLog = recovery.recoveryLog;
        recoveryLog.append(new DefaultLogEntry(1, 1, new MockCommand("1")));
        recoveryLog.append(new DefaultLogEntry(2, 1, new MockCommand("2")));
        recoveryLog.append(new DefaultLogEntry(3, 1, new MockCommand("3")));

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

        assertEquals("Journal log size", 2, recoveryLog.size());
        assertEquals("Journal data size", 9, recoveryLog.dataSize());
        assertEquals("Last index", lastIndexDuringSnapshotCapture, recoveryLog.lastIndex());
        assertEquals("Last applied", lastAppliedDuringSnapshotCapture, recoveryLog.getLastApplied());
        assertEquals("Commit index", lastAppliedDuringSnapshotCapture, recoveryLog.getCommitIndex());
        assertEquals("Snapshot term", 1, recoveryLog.getSnapshotTerm());
        assertEquals("Snapshot index", lastAppliedDuringSnapshotCapture, recoveryLog.getSnapshotIndex());

        final var context = createContext();
        assertEquals("Election term", new TermInfo(electionTerm, electionVotedFor), context.termInfo());
        assertFalse("Dynamic server configuration", context.isDynamicServerConfigurationInUse());

        verify(recoveryCohort).applyRecoveredSnapshot(snapshotState);
    }

    @Test
    void testOnRecoveryCompletedWithRemainingBatch() throws Exception {
        recovery = support.recoverToPersistent();

        final var recoveryLog = recovery.recoveryLog;
        recoveryLog.append(new DefaultLogEntry(0, 1, new MockCommand("0")));
        recoveryLog.append(new DefaultLogEntry(1, 1, new MockCommand("1")));

        sendMessageToSupport(new ApplyJournalEntries(1));

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        assertEquals("Last applied", 1, recoveryLog.getLastApplied());
        assertEquals("Commit index", 1, recoveryLog.getCommitIndex());

        final var inOrder = Mockito.inOrder(recoveryCohort);
        inOrder.verify(recoveryCohort).startLogRecoveryBatch(anyInt());

        for (int i = 0; i < recoveryLog.size(); i++) {
            inOrder.verify(recoveryCohort).appendRecoveredCommand((StateCommand) recoveryLog.lookup(i).command());
        }

        inOrder.verify(recoveryCohort).applyCurrentLogRecoveryBatch();
        inOrder.verify(recoveryCohort).getRestoreFromSnapshot();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void testOnRecoveryCompletedWithNoRemainingBatch() throws Exception {
        recovery = support.recoverToTransient();

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        verify(recoveryCohort).getRestoreFromSnapshot();
        verifyNoMoreInteractions(recoveryCohort);
    }

    @Test
    void testOnDeleteEntries() throws Exception {
        recovery = support.recoverToPersistent();

        final var recoveryLog = recovery.recoveryLog;
        recoveryLog.append(new DefaultLogEntry(0, 1, new MockCommand("0")));
        recoveryLog.append(new DefaultLogEntry(1, 1, new MockCommand("1")));
        recoveryLog.append(new DefaultLogEntry(2, 1, new MockCommand("2")));

        sendMessageToSupport(new DeleteEntries(1));

        assertEquals("Journal log size", 1, recoveryLog.size());
        assertEquals("Last index", 0, recoveryLog.lastIndex());
    }

    @Test
    void testUpdateElectionTerm() throws Exception {
        recovery = support.recoverToTransient();

        sendMessageToSupport(new UpdateElectionTerm(5, "member2"));

        assertEquals("Current term", new TermInfo(5, "member2"), createContext().termInfo());
    }

    @Test
    void testDataRecoveredWithPersistenceDisabled() throws Exception {
        doReturn(MockSnapshotState.SUPPORT).when(snapshotCohort).support();
        recovery = support.recoverToTransient();

        doReturn(10L).when(raftActor).lastSequenceNr();

        final var snapshot = Snapshot.create(new MockSnapshotState(List.of(new MockCommand("1"))),
                List.of(), 3, 1, 3, 1, new TermInfo(-1), null);
        final var snapshotOffer = new SnapshotOffer(new SnapshotMetadata("test", 6, 12345), snapshot);

        sendMessageToSupport(snapshotOffer);

        sendMessageToSupport(new UpdateElectionTerm(5, "member2"));

        sendMessageToSupport(new SimpleReplicatedLogEntry(4, 1, new MockCommand("4")));
        sendMessageToSupport(new SimpleReplicatedLogEntry(5, 1, new MockCommand("5")));

        sendMessageToSupport(new ApplyJournalEntries(4));

        sendMessageToSupport(new DeleteEntries(5));

        final var receoveryLog = recovery.recoveryLog;
        assertEquals("Journal log size", 0, receoveryLog.size());
        assertEquals("Last index", -1, receoveryLog.lastIndex());
        assertEquals("Last applied", -1, receoveryLog.getLastApplied());
        assertEquals("Commit index", -1, receoveryLog.getCommitIndex());
        assertEquals("Snapshot term", -1, receoveryLog.getSnapshotTerm());
        assertEquals("Snapshot index", -1, receoveryLog.getSnapshotIndex());

        assertEquals("Current term", new TermInfo(5, "member2"), createContext().termInfo());

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        verify(recoveryCohort, never()).applyRecoveredSnapshot(any());
        verify(recoveryCohort, never()).getRestoreFromSnapshot();
        verifyNoMoreInteractions(recoveryCohort);

        verify(raftActor).deleteMessages(10L);
    }

    @Test
    void testNoDataRecoveredWithPersistenceDisabled() throws Exception {
        recovery = support.recoverToTransient();

        sendMessageToSupport(new UpdateElectionTerm(5, "member2"));

        assertEquals("Current term", new TermInfo(5, "member2"), localAccess.termInfoStore().currentTerm());

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        verify(recoveryCohort).getRestoreFromSnapshot();
        verifyNoMoreInteractions(recoveryCohort);
    }

    @Test
    void testServerConfigurationPayloadApplied() throws Exception {
        final var follower1 = "follower1";
        final var follower2 = "follower2";
        final var follower3 = "follower3";

        peerInfos.addPeer(follower1, null, VotingState.VOTING);
        peerInfos.addPeer(follower2, null, VotingState.VOTING);
        doReturn(peerInfos).when(raftActor).peerInfos();
        recovery = support.recoverToPersistent();

        // add new Server
        sendMessageToSupport(new SimpleReplicatedLogEntry(0, 1, new VotingConfig(
            new ServerInfo(localId, true),
            new ServerInfo(follower1, true),
            new ServerInfo(follower2, false),
            new ServerInfo(follower3, true))));

        // verify new peers
        assertTrue("Dynamic server configuration", peerInfos.dynamicServerConfiguration());
        assertEquals("New peer Ids", Set.of(follower1, follower2, follower3), peerInfos.peerIds());
        assertTrue("follower1 isVoting", peerInfos.lookupPeerInfo(follower1).isVoting());
        assertFalse("follower2 isVoting", peerInfos.lookupPeerInfo(follower2).isVoting());
        assertTrue("follower3 isVoting", peerInfos.lookupPeerInfo(follower3).isVoting());

        sendMessageToSupport(new ApplyJournalEntries(0));

        verify(recoveryCohort, never()).startLogRecoveryBatch(anyInt());
        verify(recoveryCohort, never()).appendRecoveredCommand(any());

        // remove existing follower1
        sendMessageToSupport(new SimpleReplicatedLogEntry(1, 1, new VotingConfig(
            new ServerInfo(localId, true),
            new ServerInfo("follower2", true),
            new ServerInfo("follower3", true))));

        //verify new peers
        assertTrue("Dynamic server configuration", peerInfos.dynamicServerConfiguration());
        assertEquals("New peer Ids", Set.of(follower2, follower3), peerInfos.peerIds());
    }

    @Test
    void testServerConfigurationPayloadAppliedWithPersistenceDisabled() throws Exception {
        doReturn(peerInfos).when(raftActor).peerInfos();
        recovery = support.recoverToTransient();

        final var obj = new VotingConfig(new ServerInfo(localId, true), new ServerInfo("follower", true));

        sendMessageToSupport(new SimpleReplicatedLogEntry(0, 1, obj));

        //verify new peers
        assertEquals("New peer Ids", Set.of("follower"), peerInfos.peerIds());
    }

    @Test
    void testOnSnapshotOfferWithServerConfiguration() throws Exception {
        doReturn(peerInfos).when(raftActor).peerInfos();
        doReturn(MockSnapshotState.SUPPORT).when(snapshotCohort).support();
        recovery = support.recoverToPersistent();

        final var electionTerm = 2;
        final var electionVotedFor = "member-2";
        final var serverPayload = new VotingConfig(
                new ServerInfo(localId, true),
                new ServerInfo("follower1", true),
                new ServerInfo("follower2", true));

        final var snapshotState = new MockSnapshotState(List.of(new MockCommand("1")));
        final var snapshot = Snapshot.create(snapshotState, List.of(), -1, -1, -1, -1, new TermInfo(electionTerm,
            electionVotedFor), serverPayload);

        final var metadata = new SnapshotMetadata("test", 6, 12345);
        final var snapshotOffer = new SnapshotOffer(metadata, snapshot);

        sendMessageToSupport(snapshotOffer);

        assertEquals("Journal log size", 0, recovery.recoveryLog.size());
        assertEquals("Election term", new TermInfo(electionTerm, electionVotedFor),
            localAccess.termInfoStore().currentTerm());
        assertTrue("Dynamic server configuration", peerInfos.dynamicServerConfiguration());
        assertEquals("Peer List", Set.of("follower1", "follower2"), peerInfos.peerIds());
    }
}