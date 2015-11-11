/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import akka.japi.Procedure;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotOffer;
import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.PersistentDataProvider;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.DeleteEntries;
import org.opendaylight.controller.cluster.raft.base.messages.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.ServerConfigurationPayload.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for RaftActorRecoverySupport.
 *
 * @author Thomas Pantelis
 */
public class RaftActorRecoverySupportTest {

    private static final Logger LOG = LoggerFactory.getLogger(RaftActorRecoverySupportTest.class);

    @Mock
    private DataPersistenceProvider mockPersistence;

    @Mock
    private RaftActorBehavior mockBehavior;

    @Mock
    private RaftActorRecoveryCohort mockCohort;

    @Mock
    PersistentDataProvider mockPersistentProvider;

    private RaftActorRecoverySupport support;

    private RaftActorContext context;
    private final DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        context = new RaftActorContextImpl(null, null, "test", new ElectionTermImpl(mockPersistentProvider, "test", LOG),
                -1, -1, Collections.<String,String>emptyMap(), configParams, mockPersistence, LOG);

        support = new RaftActorRecoverySupport(context, mockBehavior , mockCohort);

        doReturn(true).when(mockPersistence).isRecoveryApplicable();

        context.setReplicatedLog(ReplicatedLogImpl.newInstance(context, mockBehavior));
    }

    private void sendMessageToSupport(Object message) {
        sendMessageToSupport(message, false);
    }

    private void sendMessageToSupport(Object message, boolean expComplete) {
        boolean complete = support.handleRecoveryMessage(message, mockPersistentProvider);
        assertEquals("complete", expComplete, complete);
    }

    @Test
    public void testOnReplicatedLogEntry() {
        MockRaftActorContext.MockReplicatedLogEntry logEntry = new MockRaftActorContext.MockReplicatedLogEntry(1,
                1, new MockRaftActorContext.MockPayload("1", 5));

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
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                0, new MockRaftActorContext.MockPayload("0")));
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                1, new MockRaftActorContext.MockPayload("1")));
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                2, new MockRaftActorContext.MockPayload("2")));
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                3, new MockRaftActorContext.MockPayload("3")));
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                4, new MockRaftActorContext.MockPayload("4")));
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                5, new MockRaftActorContext.MockPayload("5")));

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

        for(int i = 0; i < replicatedLog.size() - 1; i++) {
            inOrder.verify(mockCohort).appendRecoveredLogEntry(replicatedLog.get(i).getData());
        }

        inOrder.verify(mockCohort).applyCurrentLogRecoveryBatch();
        inOrder.verify(mockCohort).startLogRecoveryBatch(5);
        inOrder.verify(mockCohort).appendRecoveredLogEntry(replicatedLog.get(replicatedLog.size() - 1).getData());

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testOnApplyLogEntries() {
        ReplicatedLog replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                0, new MockRaftActorContext.MockPayload("0")));

        sendMessageToSupport(new ApplyLogEntries(0));

        assertEquals("Last applied", 0, context.getLastApplied());
        assertEquals("Commit index", 0, context.getCommitIndex());
    }

    @Test
    public void testOnSnapshotOffer() {

        ReplicatedLog replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                1, new MockRaftActorContext.MockPayload("1")));
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                2, new MockRaftActorContext.MockPayload("2")));
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                3, new MockRaftActorContext.MockPayload("3")));

        byte[] snapshotBytes = {1,2,3,4,5};

        ReplicatedLogEntry unAppliedEntry1 = new MockRaftActorContext.MockReplicatedLogEntry(1,
                4, new MockRaftActorContext.MockPayload("4", 4));

        ReplicatedLogEntry unAppliedEntry2 = new MockRaftActorContext.MockReplicatedLogEntry(1,
                5, new MockRaftActorContext.MockPayload("5", 5));

        long lastAppliedDuringSnapshotCapture = 3;
        long lastIndexDuringSnapshotCapture = 5;
        long electionTerm = 2;
        String electionVotedFor = "member-2";

        ServerConfigurationPayload serverPayload = new ServerConfigurationPayload(Arrays.asList(
                                                        new ServerInfo("leader", true),
                                                        new ServerInfo("follower1", true),
                                                        new ServerInfo("follower2", true)));

        Snapshot snapshot = Snapshot.create(snapshotBytes, Arrays.asList(unAppliedEntry1, unAppliedEntry2),
                lastIndexDuringSnapshotCapture, 1, lastAppliedDuringSnapshotCapture, 1, electionTerm,
                electionVotedFor, serverPayload);

        SnapshotMetadata metadata = new SnapshotMetadata("test", 6, 12345);
        SnapshotOffer snapshotOffer = new SnapshotOffer(metadata , snapshot);

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
        assertEquals("Peers", Sets.newHashSet("leader", "follower1", "follower2"), Sets.newHashSet(context.getPeerIds()));

        verify(mockCohort).applyRecoverySnapshot(snapshotBytes);
    }

    @Test
    public void testOnRecoveryCompletedWithRemainingBatch() {
        ReplicatedLog replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                0, new MockRaftActorContext.MockPayload("0")));
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                1, new MockRaftActorContext.MockPayload("1")));

        sendMessageToSupport(new ApplyJournalEntries(1));

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        assertEquals("Last applied", 1, context.getLastApplied());
        assertEquals("Commit index", 1, context.getCommitIndex());

        InOrder inOrder = Mockito.inOrder(mockCohort);
        inOrder.verify(mockCohort).startLogRecoveryBatch(anyInt());

        for(int i = 0; i < replicatedLog.size(); i++) {
            inOrder.verify(mockCohort).appendRecoveredLogEntry(replicatedLog.get(i).getData());
        }

        inOrder.verify(mockCohort).applyCurrentLogRecoveryBatch();

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testOnRecoveryCompletedWithNoRemainingBatch() {
        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        verifyNoMoreInteractions(mockCohort);
    }

    @Test
    public void testOnDeprecatedDeleteEntries() {
        ReplicatedLog replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                0, new MockRaftActorContext.MockPayload("0")));
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                1, new MockRaftActorContext.MockPayload("1")));
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                2, new MockRaftActorContext.MockPayload("2")));

        sendMessageToSupport(new org.opendaylight.controller.cluster.raft.RaftActor.DeleteEntries(1));

        assertEquals("Journal log size", 1, context.getReplicatedLog().size());
        assertEquals("Last index", 0, context.getReplicatedLog().lastIndex());
    }

    @Test
    public void testOnDeleteEntries() {
        ReplicatedLog replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                0, new MockRaftActorContext.MockPayload("0")));
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                1, new MockRaftActorContext.MockPayload("1")));
        replicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,
                2, new MockRaftActorContext.MockPayload("2")));

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
    public void testDeprecatedUpdateElectionTerm() {

        sendMessageToSupport(new org.opendaylight.controller.cluster.raft.RaftActor.UpdateElectionTerm(5, "member2"));

        assertEquals("Current term", 5, context.getTermInformation().getCurrentTerm());
        assertEquals("Voted For", "member2", context.getTermInformation().getVotedFor());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDataRecoveredWithPersistenceDisabled() {
        doReturn(false).when(mockPersistence).isRecoveryApplicable();
        doReturn(10L).when(mockPersistentProvider).getLastSequenceNumber();

        sendMessageToSupport(new UpdateElectionTerm(5, "member2"));

        Snapshot snapshot = Snapshot.create(new byte[]{1}, Collections.<ReplicatedLogEntry>emptyList(), 3, 1, 3, 1);
        SnapshotOffer snapshotOffer = new SnapshotOffer(new SnapshotMetadata("test", 6, 12345), snapshot);

        sendMessageToSupport(snapshotOffer);

        sendMessageToSupport(new MockRaftActorContext.MockReplicatedLogEntry(1,
                4, new MockRaftActorContext.MockPayload("4")));
        sendMessageToSupport(new MockRaftActorContext.MockReplicatedLogEntry(1,
                5, new MockRaftActorContext.MockPayload("5")));

        sendMessageToSupport(new ApplyJournalEntries(4));

        sendMessageToSupport(new DeleteEntries(5));

        sendMessageToSupport(new org.opendaylight.controller.cluster.raft.RaftActor.DeleteEntries(5));

        assertEquals("Journal log size", 0, context.getReplicatedLog().size());
        assertEquals("Last index", -1, context.getReplicatedLog().lastIndex());
        assertEquals("Last applied", -1, context.getLastApplied());
        assertEquals("Commit index", -1, context.getCommitIndex());
        assertEquals("Snapshot term", -1, context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Snapshot index", -1, context.getReplicatedLog().getSnapshotIndex());

        assertEquals("Current term", 5, context.getTermInformation().getCurrentTerm());
        assertEquals("Voted For", "member2", context.getTermInformation().getVotedFor());

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        verifyNoMoreInteractions(mockCohort);

        verify(mockPersistentProvider).deleteMessages(10L);
        verify(mockPersistentProvider).deleteSnapshots(any(SnapshotSelectionCriteria.class));
        verify(mockPersistentProvider).persist(updateElectionTerm(5, "member2"), any(Procedure.class));
    }

    static UpdateElectionTerm updateElectionTerm(final long term, final String votedFor) {
        return Matchers.argThat(new ArgumentMatcher<UpdateElectionTerm>() {
            @Override
            public boolean matches(Object argument) {
                UpdateElectionTerm other = (UpdateElectionTerm) argument;
                return term == other.getCurrentTerm() && votedFor.equals(other.getVotedFor());
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(new UpdateElectionTerm(term, votedFor));
            }
        });
    }

    @Test
    public void testNoDataRecoveredWithPersistenceDisabled() {
        doReturn(false).when(mockPersistence).isRecoveryApplicable();

        sendMessageToSupport(new UpdateElectionTerm(5, "member2"));

        assertEquals("Current term", 5, context.getTermInformation().getCurrentTerm());
        assertEquals("Voted For", "member2", context.getTermInformation().getVotedFor());

        sendMessageToSupport(RecoveryCompleted.getInstance(), true);

        verifyNoMoreInteractions(mockCohort, mockPersistentProvider);
    }

    @Test
    public void testUpdatePeerIds() {

            String leader = "Leader";
            String follower1 = "follower1";
            String follower2 = "follower2";
            String follower3 = "follower3";

            Map<String, String> peerAddresses = new HashMap<>();

            peerAddresses.put(leader, null);
            peerAddresses.put(follower1, null);
            peerAddresses.put(follower2, null);

            context.addToPeers(leader,null,VotingState.VOTING);
            context.addToPeers(follower1,null,VotingState.VOTING);
            context.addToPeers(follower2,null,VotingState.VOTING);

            assertEquals("Size", 3, context.getPeers().size());

            //add new Server
            ServerConfigurationPayload obj = new ServerConfigurationPayload(Arrays.asList(
                                                           new ServerInfo(leader, true),
                                                           new ServerInfo(follower1, true),
                                                           new ServerInfo(follower2, true),
                                                           new ServerInfo(follower3, true)));

            MockRaftActorContext.MockReplicatedLogEntry logEntry = new MockRaftActorContext.MockReplicatedLogEntry(1,
                1, obj);

            sendMessageToSupport(logEntry);
            //verify size and names
            assertEquals("Size", 4, context.getPeers().size());
            assertEquals("New follower matched", true , context.getPeerIds().contains(follower3));

            //remove existing follower1
            obj = new ServerConfigurationPayload(Arrays.asList(
                                                           new ServerInfo("Leader", true),
                                                           new ServerInfo("follower2", true),
                                                           new ServerInfo("follower3", true)));

            logEntry = new MockRaftActorContext.MockReplicatedLogEntry(1, 1, obj);

            sendMessageToSupport(logEntry);
            //verify size and names
            assertEquals("Size", 3, context.getPeers().size());
            assertEquals("Removed follower matched", false, context.getPeerIds().contains(follower1));
    }
}
