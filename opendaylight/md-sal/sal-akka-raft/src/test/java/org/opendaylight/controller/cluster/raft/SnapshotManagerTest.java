/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.OutputStream;
import java.util.List;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.io.FileBackedOutputStreamFactory;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.spi.ImmutableRaftEntryMeta;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SnapshotManagerTest extends AbstractActorTest {
    @Mock
    private RaftActorContext mockRaftActorContext;
    @Mock
    private ConfigParams mockConfigParams;
    @Mock
    private ReplicatedLog mockReplicatedLog;
    @Mock
    private DataPersistenceProvider mockDataPersistenceProvider;
    @Mock
    private RaftActorBehavior mockRaftActorBehavior;
    @Mock
    private RaftActorSnapshotCohort<?> mockCohort;
    @Mock
    private ReplicatedLogEntry replicatedLogEntry;
    @Captor
    private ArgumentCaptor<OutputStream> outputStreamCaptor;
    @Captor
    private ArgumentCaptor<Snapshot> snapshotCaptor;

    private final TermInfo mockTermInfo = new TermInfo(5, "member5");

    private SnapshotManager snapshotManager;
    private TestActorFactory factory;
    private ActorRef actorRef;

    @Before
    public void setUp() {
        doReturn(false).when(mockRaftActorContext).hasFollowers();
        doReturn(mockConfigParams).when(mockRaftActorContext).getConfigParams();
        doReturn(10L).when(mockConfigParams).getSnapshotBatchCount();
        doReturn(70).when(mockConfigParams).getSnapshotDataThresholdPercentage();
        doReturn(mockReplicatedLog).when(mockRaftActorContext).getReplicatedLog();
        doReturn("123").when(mockRaftActorContext).getId();
        doCallRealMethod().when(mockReplicatedLog).lookupMeta(anyLong());
        doReturn(mockDataPersistenceProvider).when(mockRaftActorContext).getPersistenceProvider();
        doReturn(mockRaftActorBehavior).when(mockRaftActorContext).getCurrentBehavior();
        doReturn("123").when(mockRaftActorBehavior).getLeaderId();

        doReturn(mockTermInfo).when(mockRaftActorContext).termInfo();

        doReturn(new FileBackedOutputStreamFactory(10000000, "target"))
                .when(mockRaftActorContext).getFileBackedOutputStreamFactory();

        snapshotManager = new SnapshotManager(mockRaftActorContext);
        factory = new TestActorFactory(getSystem());

        actorRef = factory.createActor(MessageCollectorActor.props(), factory.generateActorId("test-"));
        doReturn(actorRef).when(mockRaftActorContext).getActor();

        snapshotManager.setSnapshotCohort(mockCohort);
    }

    @After
    public void tearDown() {
        factory.close();
    }

    @Test
    public void testConstruction() {
        assertFalse(snapshotManager.isCapturing());
    }

    @Test
    public void testCaptureToInstall() {

        // Force capturing toInstall = true
        snapshotManager.captureToInstall(ImmutableRaftEntryMeta.of(0, 1), 0, "follower-1");

        assertTrue(snapshotManager.isCapturing());

        verify(mockCohort).createSnapshot(any(), outputStreamCaptor.capture());
        assertNotNull(outputStreamCaptor.getValue());

        CaptureSnapshot captureSnapshot = snapshotManager.getCaptureSnapshot();

        // LastIndex and LastTerm are picked up from the lastLogEntry
        assertEquals(0L, captureSnapshot.getLastIndex());
        assertEquals(1L, captureSnapshot.getLastTerm());

        // Since the actor does not have any followers (no peer addresses) lastApplied will be from lastLogEntry
        assertEquals(0L, captureSnapshot.getLastAppliedIndex());
        assertEquals(1L, captureSnapshot.getLastAppliedTerm());

        //
        assertEquals(-1L, captureSnapshot.getReplicatedToAllIndex());
        assertEquals(-1L, captureSnapshot.getReplicatedToAllTerm());
        MessageCollectorActor.clearMessages(actorRef);
    }

    @Test
    public void testCapture() {
        boolean capture = snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 1), 9);

        assertTrue(capture);

        assertTrue(snapshotManager.isCapturing());

        verify(mockCohort).createSnapshot(any(), outputStreamCaptor.capture());
        assertNull(outputStreamCaptor.getValue());

        CaptureSnapshot captureSnapshot = snapshotManager.getCaptureSnapshot();

        // LastIndex and LastTerm are picked up from the lastLogEntry
        assertEquals(9L, captureSnapshot.getLastIndex());
        assertEquals(1L, captureSnapshot.getLastTerm());

        // Since the actor does not have any followers (no peer addresses) lastApplied will be from lastLogEntry
        assertEquals(9L, captureSnapshot.getLastAppliedIndex());
        assertEquals(1L, captureSnapshot.getLastAppliedTerm());

        //
        assertEquals(-1L, captureSnapshot.getReplicatedToAllIndex());
        assertEquals(-1L, captureSnapshot.getReplicatedToAllTerm());

        MessageCollectorActor.clearMessages(actorRef);
    }

    @Test
    public void testCaptureWithNullLastLogEntry() {
        boolean capture = snapshotManager.capture(null, 1);

        assertTrue(capture);

        assertTrue(snapshotManager.isCapturing());

        verify(mockCohort).createSnapshot(any(), outputStreamCaptor.capture());
        assertNull(outputStreamCaptor.getValue());

        CaptureSnapshot captureSnapshot = snapshotManager.getCaptureSnapshot();

        // LastIndex and LastTerm are picked up from the lastLogEntry
        assertEquals(0, captureSnapshot.getLastIndex());
        assertEquals(0, captureSnapshot.getLastTerm());

        // Since the actor does not have any followers (no peer addresses) lastApplied will be from lastLogEntry
        assertEquals(0, captureSnapshot.getLastAppliedIndex());
        assertEquals(0, captureSnapshot.getLastAppliedTerm());

        //
        assertEquals(-1L, captureSnapshot.getReplicatedToAllIndex());
        assertEquals(-1L, captureSnapshot.getReplicatedToAllTerm());
        MessageCollectorActor.clearMessages(actorRef);
    }

    @Test
    public void testCaptureWithCreateProcedureError() {
        doThrow(new RuntimeException("mock")).when(mockCohort).createSnapshot(any(), any());

        boolean capture = snapshotManager.captureToInstall(ImmutableRaftEntryMeta.of(9, 1), 9, "xyzzy");

        assertFalse(capture);

        assertFalse(snapshotManager.isCapturing());

        verify(mockCohort).createSnapshot(any(), any());
    }

    @Test
    public void testIllegalCapture() {
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();
        assertTrue(snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 1), 9));
        verify(mockCohort).takeSnapshot();

        // This will not cause snapshot capture to start again
        reset(mockCohort);
        assertFalse(snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 1), 9));
        verifyNoInteractions(mockCohort);
    }

    @Test
    public void testPersistWhenReplicatedToAllIndexMinusOne() {
        doReturn(7L).when(mockReplicatedLog).getSnapshotIndex();
        doReturn(1L).when(mockReplicatedLog).getSnapshotTerm();

        doReturn(true).when(mockRaftActorContext).hasFollowers();

        doReturn(8L).when(mockRaftActorContext).getLastApplied();

        final var lastLogEntry = new SimpleReplicatedLogEntry(9L, 3L, new MockRaftActorContext.MockPayload());
        final var lastAppliedEntry = new SimpleReplicatedLogEntry(8L, 2L, new MockRaftActorContext.MockPayload());

        doReturn(lastAppliedEntry).when(mockReplicatedLog).get(8L);
        doReturn(List.of(lastLogEntry)).when(mockReplicatedLog).getFrom(9L);

        // when replicatedToAllIndex = -1
        final var snapshotState = ByteState.of(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        doReturn(snapshotState).when(mockCohort).takeSnapshot();
        snapshotManager.capture(lastLogEntry, -1);

        verify(mockDataPersistenceProvider).saveSnapshot(snapshotCaptor.capture());

        final var snapshot = snapshotCaptor.getValue();

        assertEquals("getLastTerm", 3L, snapshot.getLastTerm());
        assertEquals("getLastIndex", 9L, snapshot.getLastIndex());
        assertEquals("getLastAppliedTerm", 2L, snapshot.getLastAppliedTerm());
        assertEquals("getLastAppliedIndex", 8L, snapshot.getLastAppliedIndex());
        assertEquals("getState", snapshotState, snapshot.getState());
        assertEquals("getUnAppliedEntries", List.of(lastLogEntry), snapshot.getUnAppliedEntries());
        assertEquals(mockTermInfo, snapshot.termInfo());

        verify(mockReplicatedLog).snapshotPreCommit(7L, 1L);
    }

    @Test
    public void testPersistWhenReplicatedToAllIndexNotMinus() {
        doReturn(45L).when(mockReplicatedLog).getSnapshotIndex();
        doReturn(6L).when(mockReplicatedLog).getSnapshotTerm();
        doReturn(replicatedLogEntry).when(mockReplicatedLog).get(9);
        doReturn(6L).when(replicatedLogEntry).term();
        doReturn(9L).when(replicatedLogEntry).index();

        final var snapshotState = ByteState.of(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        doReturn(snapshotState).when(mockCohort).takeSnapshot();

        // when replicatedToAllIndex != -1
        snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 6), 9);

        verify(mockDataPersistenceProvider).saveSnapshot(snapshotCaptor.capture());

        final var snapshot = snapshotCaptor.getValue();

        assertEquals("getLastTerm", 6L, snapshot.getLastTerm());
        assertEquals("getLastIndex", 9L, snapshot.getLastIndex());
        assertEquals("getLastAppliedTerm", 6L, snapshot.getLastAppliedTerm());
        assertEquals("getLastAppliedIndex", 9L, snapshot.getLastAppliedIndex());
        assertEquals("getState", snapshotState, snapshot.getState());
        assertEquals("getUnAppliedEntries size", 0, snapshot.getUnAppliedEntries().size());

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior).setReplicatedToAllIndex(9);
    }

    @Test
    public void testPersistWhenReplicatedLogDataSizeGreaterThanThreshold() {
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 6), -1);

        snapshotManager.persist(ByteState.empty(), null);

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior, never()).setReplicatedToAllIndex(anyLong());
    }

    @Test
    public void testPersistWhenReplicatedLogSizeExceedsSnapshotBatchCount() {
        doReturn(10L).when(mockReplicatedLog).size(); // matches snapshotBatchCount
        doReturn(100).when(mockReplicatedLog).dataSize();

        doReturn(5L).when(mockReplicatedLog).getSnapshotIndex();
        doReturn(5L).when(mockReplicatedLog).getSnapshotTerm();

        long replicatedToAllIndex = 1;
        doReturn(replicatedLogEntry).when(mockReplicatedLog).get(replicatedToAllIndex);
        doReturn(6L).when(replicatedLogEntry).term();
        doReturn(replicatedToAllIndex).when(replicatedLogEntry).index();

        snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 6), replicatedToAllIndex);

        snapshotManager.persist(ByteState.empty(), null);

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior).setReplicatedToAllIndex(replicatedToAllIndex);
    }

    @Test
    public void testPersistSendInstallSnapshot() throws Exception {
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();
        doNothing().when(mockCohort).createSnapshot(any(), any());

        // when replicatedToAllIndex = -1
        boolean capture = snapshotManager.captureToInstall(ImmutableRaftEntryMeta.of(9, 6), -1, "follower-1");

        assertTrue(capture);

        ByteState snapshotState = ByteState.of(new byte[] {1,2,3,4,5,6,7,8,9,10});

        verify(mockCohort).createSnapshot(any(), outputStreamCaptor.capture());

        final var installSnapshotStream = outputStreamCaptor.getValue();
        assertNotNull(installSnapshotStream);

        installSnapshotStream.write(snapshotState.bytes());

        snapshotManager.persist(snapshotState, installSnapshotStream);

        assertTrue(snapshotManager.isCapturing());

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        final var sendInstallSnapshotArgumentCaptor = ArgumentCaptor.forClass(SendInstallSnapshot.class);

        verify(mockRaftActorBehavior).handleMessage(any(ActorRef.class), sendInstallSnapshotArgumentCaptor.capture());

        SendInstallSnapshot sendInstallSnapshot = sendInstallSnapshotArgumentCaptor.getValue();

        assertEquals("state", snapshotState, sendInstallSnapshot.getSnapshot().getState());
        assertArrayEquals("state", snapshotState.bytes(), sendInstallSnapshot.getSnapshotBytes().read());
    }

    @Test
    public void testCallingPersistWithoutCaptureWillDoNothing() {
        snapshotManager.persist(ByteState.empty(), null);

        verify(mockDataPersistenceProvider, never()).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog, never()).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior, never()).handleMessage(any(ActorRef.class), any(SendInstallSnapshot.class));
    }

    @Test
    public void testCallingPersistTwiceWillDoNoHarm() {
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 6), -1);

        snapshotManager.persist(ByteState.empty(), null);

        snapshotManager.persist(ByteState.empty(), null);

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);
    }

    @Test
    public void testCommit() {
        doReturn(50L).when(mockDataPersistenceProvider).getLastSequenceNumber();

        // when replicatedToAllIndex = -1
        snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 6), -1);

        snapshotManager.persist(ByteState.empty(), null);

        assertTrue(snapshotManager.isCapturing());

        snapshotManager.commit(100L, 1234L);

        assertFalse(snapshotManager.isCapturing());

        verify(mockReplicatedLog).snapshotCommit();

        verify(mockDataPersistenceProvider).deleteMessages(50L);

        final var criteriaCaptor = ArgumentCaptor.forClass(SnapshotSelectionCriteria.class);

        verify(mockDataPersistenceProvider).deleteSnapshots(criteriaCaptor.capture());

        assertEquals(Long.MAX_VALUE, criteriaCaptor.getValue().maxSequenceNr());
        assertEquals(1233L, criteriaCaptor.getValue().maxTimestamp());

        MessageCollectorActor.expectFirstMatching(actorRef, SnapshotComplete.class);
    }

    @Test
    public void testCommitBeforePersist() {
        // when replicatedToAllIndex = -1
        snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 6), -1);

        snapshotManager.commit(100L, 0);

        verify(mockReplicatedLog, never()).snapshotCommit();

        verify(mockDataPersistenceProvider, never()).deleteMessages(100L);

        verify(mockDataPersistenceProvider, never()).deleteSnapshots(any(SnapshotSelectionCriteria.class));
    }

    @Test
    public void testCommitBeforeCapture() {
        snapshotManager.commit(100L, 0);

        verify(mockReplicatedLog, never()).snapshotCommit();

        verify(mockDataPersistenceProvider, never()).deleteMessages(anyLong());

        verify(mockDataPersistenceProvider, never()).deleteSnapshots(any(SnapshotSelectionCriteria.class));

    }

    @Test
    public void testCallingCommitMultipleTimesCausesNoHarm() {
        doReturn(50L).when(mockDataPersistenceProvider).getLastSequenceNumber();

        // when replicatedToAllIndex = -1
        snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 6), -1);

        snapshotManager.persist(ByteState.empty(), null);

        snapshotManager.commit(100L, 0);

        snapshotManager.commit(100L, 0);

        verify(mockReplicatedLog, times(1)).snapshotCommit();

        verify(mockDataPersistenceProvider, times(1)).deleteMessages(50L);

        verify(mockDataPersistenceProvider, times(1)).deleteSnapshots(any(SnapshotSelectionCriteria.class));
    }

    @Test
    public void testRollback() {
        // when replicatedToAllIndex = -1
        snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 6), -1);

        snapshotManager.persist(ByteState.empty(), null);

        snapshotManager.rollback();

        verify(mockReplicatedLog).snapshotRollback();

        MessageCollectorActor.expectFirstMatching(actorRef, SnapshotComplete.class);
    }


    @Test
    public void testRollbackBeforePersist() {
        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(ImmutableRaftEntryMeta.of(9, 6), -1, "xyzzy");

        snapshotManager.rollback();

        verify(mockReplicatedLog, never()).snapshotRollback();
    }

    @Test
    public void testRollbackBeforeCapture() {
        snapshotManager.rollback();

        verify(mockReplicatedLog, never()).snapshotRollback();
    }

    @Test
    public void testCallingRollbackMultipleTimesCausesNoHarm() {
        // when replicatedToAllIndex = -1
        snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 6), -1);

        snapshotManager.persist(ByteState.empty(), null);

        snapshotManager.rollback();

        snapshotManager.rollback();

        verify(mockReplicatedLog, times(1)).snapshotRollback();
    }

    @Test
    public void testTrimLogWhenTrimIndexLessThanLastApplied() {
        doReturn(20L).when(mockRaftActorContext).getLastApplied();

        doReturn(true).when(mockReplicatedLog).isPresent(10);
        doReturn(replicatedLogEntry).when(mockReplicatedLog).get(10);
        doReturn(5L).when(replicatedLogEntry).term();

        long retIndex = snapshotManager.trimLog(10);
        assertEquals("return index", 10L, retIndex);

        verify(mockReplicatedLog).snapshotPreCommit(10, 5);
        verify(mockReplicatedLog).snapshotCommit(false);

        verify(mockRaftActorBehavior, never()).setReplicatedToAllIndex(anyLong());
    }

    @Test
    public void testTrimLogWhenLastAppliedNotSet() {
        doReturn(-1L).when(mockRaftActorContext).getLastApplied();

        long retIndex = snapshotManager.trimLog(10);
        assertEquals("return index", -1L, retIndex);

        verify(mockReplicatedLog, never()).snapshotPreCommit(anyLong(), anyLong());
        verify(mockReplicatedLog, never()).snapshotCommit(false);

        verify(mockRaftActorBehavior, never()).setReplicatedToAllIndex(anyLong());
    }

    @Test
    public void testTrimLogWhenLastAppliedZero() {
        doReturn(0L).when(mockRaftActorContext).getLastApplied();

        long retIndex = snapshotManager.trimLog(10);
        assertEquals("return index", -1L, retIndex);

        verify(mockReplicatedLog, never()).snapshotPreCommit(anyLong(), anyLong());
        verify(mockReplicatedLog, never()).snapshotCommit(false);

        verify(mockRaftActorBehavior, never()).setReplicatedToAllIndex(anyLong());
    }

    @Test
    public void testTrimLogWhenTrimIndexNotPresent() {
        doReturn(20L).when(mockRaftActorContext).getLastApplied();

        doReturn(false).when(mockReplicatedLog).isPresent(10);

        long retIndex = snapshotManager.trimLog(10);
        assertEquals("return index", -1L, retIndex);

        verify(mockReplicatedLog, never()).snapshotPreCommit(anyLong(), anyLong());
        verify(mockReplicatedLog, never()).snapshotCommit(false);

        // Trim index is greater than replicatedToAllIndex so should update it.
        verify(mockRaftActorBehavior).setReplicatedToAllIndex(10L);
    }

    @Test
    public void testTrimLogAfterCapture() {
        boolean capture = snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 1), 9);

        assertTrue(capture);

        assertTrue(snapshotManager.isCapturing());

        snapshotManager.trimLog(10);

        verify(mockReplicatedLog, never()).snapshotPreCommit(anyLong(), anyLong());
        verify(mockReplicatedLog, never()).snapshotCommit(false);
    }

    @Test
    public void testTrimLogAfterCaptureToInstall() {
        boolean capture = snapshotManager.capture(ImmutableRaftEntryMeta.of(9, 1), 9);

        assertTrue(capture);

        assertTrue(snapshotManager.isCapturing());

        snapshotManager.trimLog(10);

        verify(mockReplicatedLog, never()).snapshotPreCommit(10, 5);
        verify(mockReplicatedLog, never()).snapshotCommit();
    }

    @Test
    public void testLastAppliedTermInformationReader() {
        doReturn(4L).when(mockReplicatedLog).getSnapshotTerm();
        doReturn(7L).when(mockReplicatedLog).getSnapshotIndex();

        final var lastLogEntry = ImmutableRaftEntryMeta.of(9, 6);

        // No followers and valid lastLogEntry
        var reader = SnapshotManager.computeLastAppliedEntry(mockReplicatedLog, 1L, lastLogEntry, false);
        assertEquals("getTerm", 6L, reader.term());
        assertEquals("getIndex", 9L, reader.index());

        // No followers and null lastLogEntry
        reader = SnapshotManager.computeLastAppliedEntry(mockReplicatedLog, 1L, null, false);
        assertEquals("getTerm", -1L, reader.term());
        assertEquals("getIndex", -1L, reader.index());

        // Followers and valid originalIndex entry
        doReturn(new SimpleReplicatedLogEntry(8L, 5L, new MockRaftActorContext.MockPayload()))
            .when(mockReplicatedLog).get(8L);
        reader = SnapshotManager.computeLastAppliedEntry(mockReplicatedLog, 8L, lastLogEntry, true);
        assertEquals("getTerm", 5L, reader.term());
        assertEquals("getIndex", 8L, reader.index());

        // Followers and null originalIndex entry and valid snapshot index
        reader = SnapshotManager.computeLastAppliedEntry(mockReplicatedLog, 7L, lastLogEntry, true);
        assertEquals("getTerm", 4L, reader.term());
        assertEquals("getIndex", 7L, reader.index());

        // Followers and null originalIndex entry and invalid snapshot index
        doReturn(-1L).when(mockReplicatedLog).getSnapshotIndex();
        reader = SnapshotManager.computeLastAppliedEntry(mockReplicatedLog, 7L, lastLogEntry, true);
        assertEquals("getTerm", -1L, reader.term());
        assertEquals("getIndex", -1L, reader.index());
    }
}
