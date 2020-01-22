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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotOffer;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.PersistentDataProvider;
import org.opendaylight.controller.cluster.raft.MockRaftActor.MockSnapshotState;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for RaftActorRecoverySupport.
 *
 * @author Thomas Pantelis
 */
public class RaftActorRecoverySupportTest {

    private static final Logger LOG = LoggerFactory.getLogger(RaftActorRecoverySupportTest.class);
    private static final int INCREMENTAL_RECOVERY_CHUNK = 50;

    @Mock
    private DataPersistenceProvider mockPersistence;

    @Mock
    private RaftActorRecoveryCohort mockCohort;

    @Mock
    PersistentDataProvider mockPersistentProvider;

    ActorRef mockActorRef;

    ActorSystem mockActorSystem;

    private RaftActorRecoverySupport support;

    private RaftActorContext context;
    private final DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
    private final String localId = "leader";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockActorSystem = ActorSystem.create();
        mockActorRef = mockActorSystem.actorOf(Props.create(DoNothingActor.class));
        context = new RaftActorContextImpl(mockActorRef, null, localId,
                new ElectionTermImpl(mockPersistentProvider, "test", LOG), -1, -1,
                Collections.<String, String>emptyMap(), configParams, mockPersistence, applyState -> {
        }, LOG, MoreExecutors.directExecutor());

        support = new RaftActorRecoverySupport(context, mockCohort);

        doReturn(true).when(mockPersistence).isRecoveryApplicable();

        context.setReplicatedLog(ReplicatedLogImpl.newInstance(context));
    }

    private void sendMessageToSupport(final Object message) {
        sendMessageToSupport(message, false);
    }

    private void sendMessageToSupport(final Object message, final boolean expComplete) {
        boolean complete = support.handleRecoveryMessage(message, mockPersistentProvider);
        assertEquals("complete", expComplete, complete);
    }

    @Test
    public void testOnReplicatedLogEntry() {
        ReplicatedLogEntry logEntry = new SimpleReplicatedLogEntry(1, 1, new MockRaftActorContext.MockPayload("1", 5));

        sendMessageToSupport(logEntry);

        assertEquals("Journal log size", 1, context.getReplicatedLog().size());
        assertEquals("Journal data size", 5, context.getReplicatedLog().dataSize());
        assertEquals("Last index", 1, context.getReplicatedLog().lastIndex());
        assertEquals("Last applied", -1, context.getLastApplied());
        assertEquals("Commit index", -1, context.getCommitIndex());
        assertEquals("Snapshot term", -1, context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Snapshot index", -1, context.getReplicatedLog().getSnapshotIndex());
    }

    @Test
    public void testOnApplyJournalEntries() {
        configParams.setJournalRecoveryLogBatchSize(5);

        ReplicatedLog replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new SimpleReplicatedLogEntry(0, 1, new MockRaftActorContext.MockPayload("0")));
        replicatedLog.append(new SimpleReplicatedLogEntry(1, 1, new MockRaftActorContext.MockPayload("1")));
        replicatedLog.append(new SimpleReplicatedLogEntry(2, 1, new MockRaftActorContext.MockPayload("2")));
        replicatedLog.append(new SimpleReplicatedLogEntry(3, 1, new MockRaftActorContext.MockPayload("3")));
        replicatedLog.append(new SimpleReplicatedLogEntry(4, 1, new MockRaftActorContext.MockPayload("4")));
        replicatedLog.append(new SimpleReplicatedLogEntry(5, 1, new MockRaftActorContext.MockPayload("5")));

        sendMessageToSupport(new ApplyJournalEntries(2));

        assertEquals("Last applied", 2, context.getLastApplied());
        assertEquals("Commit index", 2, context.getCommitIndex());

        sendMessageToSupport(new ApplyJournalEntries(4));

        assertEquals("Last applied", 4, context.getLastApplied());
        assertEquals("Last applied", 4, context.getLastApplied());

        sendMessageToSupport(new ApplyJournalEntries(5));

        assertEquals("Last index", 5, context.getReplicatedLog().lastIndex());
        assertEquals("Last applied", 5, context.getLastApplied());
        assertEquals("Commit index", 5, context.getCommitIndex());
        assertEquals("Snapshot term", -1, context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Snapshot index", -1, context.getReplicatedLog().getSnapshotIndex());

        InOrder inOrder = Mockito.inOrder(mockCohort);
        inOrder.verify(mockCohort).startLogRecoveryBatch(5);

        for (int i = 0; i < replicatedLog.size() - 1; i++) {
            inOrder.verify(mockCohort).appendRecoveredLogEntry(replicatedLog.get(i).getData());
        }

        inOrder.verify(mockCohort).applyCurrentLogRecoveryBatch();
        inOrder.verify(mockCohort).startLogRecoveryBatch(5);
        inOrder.verify(mockCohort).appendRecoveredLogEntry(replicatedLog.get(replicatedLog.size() - 1).getData());

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testIncrementalRecovery() {
        configParams.setRecoveredEntriesBeforeSnapshot(INCREMENTAL_RECOVERY_CHUNK);
        context.getSnapshotManager().setCreateSnapshotConsumer(outputStream -> { });

        final int numberOfChunks = 5;
        ReplicatedLog replicatedLog = context.getReplicatedLog();

        for (int i = 0; i <= numberOfChunks * INCREMENTAL_RECOVERY_CHUNK; i++) {
            replicatedLog.append(new SimpleReplicatedLogEntry(i, 1,
                    new MockRaftActorContext.MockPayload(String.valueOf(i))));
        }

        for (int i = 0; i <= numberOfChunks * INCREMENTAL_RECOVERY_CHUNK; i++) {
            sendMessageToSupport(new ApplyJournalEntries(i));
        }

        verify(mockPersistence, times(numberOfChunks)).deleteMessages(anyLong());
    }

    @Test
    public void testOnSnapshotOffer() {

        ReplicatedLog replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new SimpleReplicatedLogEntry(1, 1, new MockRaftActorContext.MockPayload("1")));
        replicatedLog.append(new SimpleReplicatedLogEntry(2, 1, new MockRaftActorContext.MockPayload("2")));
        replicatedLog.append(new SimpleReplicatedLogEntry(3, 1, new MockRaftActorContext.MockPayload("3")));

        ReplicatedLogEntry unAppliedEntry1 = new SimpleReplicatedLogEntry(4, 1,
                new MockRaftActorContext.MockPayload("4", 4));

        ReplicatedLogEntry unAppliedEntry2 = new SimpleReplicatedLogEntry(5, 1,
                new MockRaftActorContext.MockPayload("5", 5));

        long lastAppliedDuringSnapshotCapture = 3;
        long lastIndexDuringSnapshotCapture = 5;
        long electionTerm = 2;
        String electionVotedFor = "member-2";

        MockSnapshotState snapshotState = new MockSnapshotState(Arrays.asList(new MockPayload("1")));
        Snapshot snapshot = Snapshot.create(snapshotState,
                Arrays.asList(unAppliedEntry1, unAppliedEntry2), lastIndexDuringSnapshotCapture, 1,
                lastAppliedDuringSnapshotCapture, 1, electionTerm, electionVotedFor, null);

        SnapshotMetadata metadata = new SnapshotMetadata("test", 6, 12345);
        SnapshotOffer snapshotOffer = new SnapshotOffer(metadata, snapshot);

        sendMessageToSupport(snapshotOffer);

        assertEquals("Journal log size", 2, context.getReplicatedLog().size());
        assertEquals("Journal data size", 9, context.getReplicatedLog().dataSize());
        assertEquals("Last index", lastIndexDuringSnapshotCapture, context.getReplicatedLog().lastIndex());
        assertEquals("Last applied", lastAppliedDuringSnapshotCapture, context.getLastApplied());
        assertEquals("Commit index", lastAppliedDuringSnapshotCapture, context.getCommitIndex());
        assertEquals("Snapshot term", 1, context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Snapshot index", lastAppliedDuringSnapshotCapture, context.getReplicatedLog().getSnapshotIndex());
        assertEquals("Election term", electionTerm, context.getTermInformation().getCurrentTerm());
        assertEquals("Election votedFor", electionVotedFor, context.getTermInformation().getVotedFor());
        assertFalse("Dynamic server configuration", context.isDynamicServerConfigurationInUse());

        verify(mockCohort).applyRecoverySnapshot(snapshotState);
    }

    @Test
    public void testOnRecoveryCompletedWithRemainingBatch() {
        ReplicatedLog replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new SimpleReplicatedLogEntry(0, 1, new MockRaftActorContext.MockPayload("0")));
        replicatedLog.append(new SimpleReplicatedLogEntry(1, 1, new MockRaftActorContext.MockPayload("1")));

        sendMessageToSupport(new ApplyJournalEntries(1));

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        assertEquals("Last applied", 1, context.getLastApplied());
        assertEquals("Commit index", 1, context.getCommitIndex());

        InOrder inOrder = Mockito.inOrder(mockCohort);
        inOrder.verify(mockCohort).startLogRecoveryBatch(anyInt());

        for (int i = 0; i < replicatedLog.size(); i++) {
            inOrder.verify(mockCohort).appendRecoveredLogEntry(replicatedLog.get(i).getData());
        }

        inOrder.verify(mockCohort).applyCurrentLogRecoveryBatch();
        inOrder.verify(mockCohort).getRestoreFromSnapshot();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testOnRecoveryCompletedWithNoRemainingBatch() {
        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        verify(mockCohort).getRestoreFromSnapshot();
        verifyNoMoreInteractions(mockCohort);
    }

    @Test
    public void testOnDeleteEntries() {
        ReplicatedLog replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new SimpleReplicatedLogEntry(0, 1, new MockRaftActorContext.MockPayload("0")));
        replicatedLog.append(new SimpleReplicatedLogEntry(1, 1, new MockRaftActorContext.MockPayload("1")));
        replicatedLog.append(new SimpleReplicatedLogEntry(2, 1, new MockRaftActorContext.MockPayload("2")));

        sendMessageToSupport(new DeleteEntries(1));

        assertEquals("Journal log size", 1, context.getReplicatedLog().size());
        assertEquals("Last index", 0, context.getReplicatedLog().lastIndex());
    }

    @Test
    public void testUpdateElectionTerm() {

        sendMessageToSupport(new UpdateElectionTerm(5, "member2"));

        assertEquals("Current term", 5, context.getTermInformation().getCurrentTerm());
        assertEquals("Voted For", "member2", context.getTermInformation().getVotedFor());
    }

    @Test
    public void testDataRecoveredWithPersistenceDisabled() {
        doNothing().when(mockCohort).applyRecoverySnapshot(any());
        doReturn(false).when(mockPersistence).isRecoveryApplicable();
        doReturn(10L).when(mockPersistentProvider).getLastSequenceNumber();

        Snapshot snapshot = Snapshot.create(new MockSnapshotState(Arrays.asList(new MockPayload("1"))),
                Collections.<ReplicatedLogEntry>emptyList(), 3, 1, 3, 1, -1, null, null);
        SnapshotOffer snapshotOffer = new SnapshotOffer(new SnapshotMetadata("test", 6, 12345), snapshot);

        sendMessageToSupport(snapshotOffer);

        sendMessageToSupport(new UpdateElectionTerm(5, "member2"));

        sendMessageToSupport(new SimpleReplicatedLogEntry(4, 1, new MockRaftActorContext.MockPayload("4")));
        sendMessageToSupport(new SimpleReplicatedLogEntry(5, 1, new MockRaftActorContext.MockPayload("5")));

        sendMessageToSupport(new ApplyJournalEntries(4));

        sendMessageToSupport(new DeleteEntries(5));

        assertEquals("Journal log size", 0, context.getReplicatedLog().size());
        assertEquals("Last index", -1, context.getReplicatedLog().lastIndex());
        assertEquals("Last applied", -1, context.getLastApplied());
        assertEquals("Commit index", -1, context.getCommitIndex());
        assertEquals("Snapshot term", -1, context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Snapshot index", -1, context.getReplicatedLog().getSnapshotIndex());

        assertEquals("Current term", 5, context.getTermInformation().getCurrentTerm());
        assertEquals("Voted For", "member2", context.getTermInformation().getVotedFor());

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        verify(mockCohort, never()).applyRecoverySnapshot(any());
        verify(mockCohort, never()).getRestoreFromSnapshot();
        verifyNoMoreInteractions(mockCohort);

        verify(mockPersistentProvider).deleteMessages(10L);
    }

    static UpdateElectionTerm updateElectionTerm(final long term, final String votedFor) {
        return ArgumentMatchers.argThat(other ->
                term == other.getCurrentTerm() && votedFor.equals(other.getVotedFor()));
    }

    @Test
    public void testNoDataRecoveredWithPersistenceDisabled() {
        doReturn(false).when(mockPersistence).isRecoveryApplicable();

        sendMessageToSupport(new UpdateElectionTerm(5, "member2"));

        assertEquals("Current term", 5, context.getTermInformation().getCurrentTerm());
        assertEquals("Voted For", "member2", context.getTermInformation().getVotedFor());

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        verify(mockCohort).getRestoreFromSnapshot();
        verifyNoMoreInteractions(mockCohort, mockPersistentProvider);
    }

    @Test
    public void testServerConfigurationPayloadApplied() {
        String follower1 = "follower1";
        String follower2 = "follower2";
        String follower3 = "follower3";

        context.addToPeers(follower1, null, VotingState.VOTING);
        context.addToPeers(follower2, null, VotingState.VOTING);

        //add new Server
        ServerConfigurationPayload obj = new ServerConfigurationPayload(Arrays.asList(
                new ServerInfo(localId, true),
                new ServerInfo(follower1, true),
                new ServerInfo(follower2, false),
                new ServerInfo(follower3, true)));

        sendMessageToSupport(new SimpleReplicatedLogEntry(0, 1, obj));

        //verify new peers
        assertTrue("Dynamic server configuration", context.isDynamicServerConfigurationInUse());
        assertEquals("New peer Ids", Sets.newHashSet(follower1, follower2, follower3),
                Sets.newHashSet(context.getPeerIds()));
        assertEquals("follower1 isVoting", true, context.getPeerInfo(follower1).isVoting());
        assertEquals("follower2 isVoting", false, context.getPeerInfo(follower2).isVoting());
        assertEquals("follower3 isVoting", true, context.getPeerInfo(follower3).isVoting());

        sendMessageToSupport(new ApplyJournalEntries(0));

        verify(mockCohort, never()).startLogRecoveryBatch(anyInt());
        verify(mockCohort, never()).appendRecoveredLogEntry(any(Payload.class));

        //remove existing follower1
        obj = new ServerConfigurationPayload(Arrays.asList(
                new ServerInfo(localId, true),
                new ServerInfo("follower2", true),
                new ServerInfo("follower3", true)));

        sendMessageToSupport(new SimpleReplicatedLogEntry(1, 1, obj));

        //verify new peers
        assertTrue("Dynamic server configuration", context.isDynamicServerConfigurationInUse());
        assertEquals("New peer Ids", Sets.newHashSet(follower2, follower3), Sets.newHashSet(context.getPeerIds()));
    }

    @Test
    public void testServerConfigurationPayloadAppliedWithPersistenceDisabled() {
        doReturn(false).when(mockPersistence).isRecoveryApplicable();

        String follower = "follower";
        ServerConfigurationPayload obj = new ServerConfigurationPayload(Arrays.asList(
                new ServerInfo(localId, true), new ServerInfo(follower, true)));

        sendMessageToSupport(new SimpleReplicatedLogEntry(0, 1, obj));

        //verify new peers
        assertEquals("New peer Ids", Sets.newHashSet(follower), Sets.newHashSet(context.getPeerIds()));
    }

    @Test
    public void testOnSnapshotOfferWithServerConfiguration() {
        long electionTerm = 2;
        String electionVotedFor = "member-2";
        ServerConfigurationPayload serverPayload = new ServerConfigurationPayload(Arrays.asList(
                new ServerInfo(localId, true),
                new ServerInfo("follower1", true),
                new ServerInfo("follower2", true)));

        MockSnapshotState snapshotState = new MockSnapshotState(Arrays.asList(new MockPayload("1")));
        Snapshot snapshot = Snapshot.create(snapshotState, Collections.<ReplicatedLogEntry>emptyList(),
                -1, -1, -1, -1, electionTerm, electionVotedFor, serverPayload);

        SnapshotMetadata metadata = new SnapshotMetadata("test", 6, 12345);
        SnapshotOffer snapshotOffer = new SnapshotOffer(metadata, snapshot);

        sendMessageToSupport(snapshotOffer);

        assertEquals("Journal log size", 0, context.getReplicatedLog().size());
        assertEquals("Election term", electionTerm, context.getTermInformation().getCurrentTerm());
        assertEquals("Election votedFor", electionVotedFor, context.getTermInformation().getVotedFor());
        assertTrue("Dynamic server configuration", context.isDynamicServerConfigurationInUse());
        assertEquals("Peer List", Sets.newHashSet("follower1", "follower2"),
                Sets.newHashSet(context.getPeerIds()));
    }
}