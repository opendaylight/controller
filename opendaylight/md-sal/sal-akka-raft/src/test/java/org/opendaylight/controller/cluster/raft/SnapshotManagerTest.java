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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import akka.actor.ActorRef;
import akka.persistence.SnapshotSelectionCriteria;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.io.FileBackedOutputStreamFactory;
import org.opendaylight.controller.cluster.raft.SnapshotManager.LastAppliedTermInformationReader;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.slf4j.LoggerFactory;

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
    private Consumer<Optional<OutputStream>> mockProcedure;

    @Mock
    private ElectionTerm mockElectionTerm;

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
        doReturn(mockDataPersistenceProvider).when(mockRaftActorContext).getPersistenceProvider();
        doReturn(mockRaftActorBehavior).when(mockRaftActorContext).getCurrentBehavior();
        doReturn("123").when(mockRaftActorBehavior).getLeaderId();

        doReturn(mockElectionTerm).when(mockRaftActorContext).getTermInformation();
        doReturn(5L).when(mockElectionTerm).getCurrentTerm();
        doReturn("member5").when(mockElectionTerm).getVotedFor();

        doReturn(new FileBackedOutputStreamFactory(10000000, "target"))
                .when(mockRaftActorContext).getFileBackedOutputStreamFactory();

        snapshotManager = new SnapshotManager(mockRaftActorContext, LoggerFactory.getLogger(this.getClass()));
        factory = new TestActorFactory(getSystem());

        actorRef = factory.createActor(MessageCollectorActor.props(), factory.generateActorId("test-"));
        doReturn(actorRef).when(mockRaftActorContext).getActor();

        snapshotManager.setCreateSnapshotConsumer(mockProcedure);
    }

    @After
    public void tearDown() {
        factory.close();
    }

    @Test
    public void testConstruction() {
        assertFalse(snapshotManager.isCapturing());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCaptureToInstall() {

        // Force capturing toInstall = true
        snapshotManager.captureToInstall(new SimpleReplicatedLogEntry(0, 1,
                new MockRaftActorContext.MockPayload()), 0, "follower-1");

        assertTrue(snapshotManager.isCapturing());

        ArgumentCaptor<Optional> outputStream = ArgumentCaptor.forClass(Optional.class);
        verify(mockProcedure).accept(outputStream.capture());
        assertEquals("isPresent", true, outputStream.getValue().isPresent());

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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testCapture() {
        boolean capture = snapshotManager.capture(new SimpleReplicatedLogEntry(9, 1,
                new MockRaftActorContext.MockPayload()), 9);

        assertTrue(capture);

        assertTrue(snapshotManager.isCapturing());

        ArgumentCaptor<Optional> outputStream = ArgumentCaptor.forClass(Optional.class);
        verify(mockProcedure).accept(outputStream.capture());
        assertEquals("isPresent", false, outputStream.getValue().isPresent());

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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCaptureWithNullLastLogEntry() {
        boolean capture = snapshotManager.capture(null, 1);

        assertTrue(capture);

        assertTrue(snapshotManager.isCapturing());

        ArgumentCaptor<Optional> outputStream = ArgumentCaptor.forClass(Optional.class);
        verify(mockProcedure).accept(outputStream.capture());
        assertEquals("isPresent", false, outputStream.getValue().isPresent());

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
        doThrow(new RuntimeException("mock")).when(mockProcedure).accept(any());

        boolean capture = snapshotManager.capture(new SimpleReplicatedLogEntry(9, 1,
                new MockRaftActorContext.MockPayload()), 9);

        assertFalse(capture);

        assertFalse(snapshotManager.isCapturing());

        verify(mockProcedure).accept(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIllegalCapture() {
        boolean capture = snapshotManager.capture(new SimpleReplicatedLogEntry(9, 1,
                new MockRaftActorContext.MockPayload()), 9);

        assertTrue(capture);

        verify(mockProcedure).accept(any());

        reset(mockProcedure);

        // This will not cause snapshot capture to start again
        capture = snapshotManager.capture(new SimpleReplicatedLogEntry(9, 1,
                new MockRaftActorContext.MockPayload()), 9);

        assertFalse(capture);

        verify(mockProcedure, never()).accept(any());
    }

    @Test
    public void testPersistWhenReplicatedToAllIndexMinusOne() {
        doReturn(7L).when(mockReplicatedLog).getSnapshotIndex();
        doReturn(1L).when(mockReplicatedLog).getSnapshotTerm();

        doReturn(true).when(mockRaftActorContext).hasFollowers();

        doReturn(8L).when(mockRaftActorContext).getLastApplied();

        ReplicatedLogEntry lastLogEntry = new SimpleReplicatedLogEntry(9L, 3L, new MockRaftActorContext.MockPayload());

        ReplicatedLogEntry lastAppliedEntry = new SimpleReplicatedLogEntry(
                8L, 2L, new MockRaftActorContext.MockPayload());

        doReturn(lastAppliedEntry).when(mockReplicatedLog).get(8L);
        doReturn(List.of(lastLogEntry)).when(mockReplicatedLog).getFrom(9L);

        // when replicatedToAllIndex = -1
        snapshotManager.capture(lastLogEntry, -1);

        ByteState snapshotState = ByteState.of(new byte[] {1,2,3,4,5,6,7,8,9,10});
        snapshotManager.persist(snapshotState, Optional.empty(), Runtime.getRuntime().totalMemory());

        ArgumentCaptor<Snapshot> snapshotArgumentCaptor = ArgumentCaptor.forClass(Snapshot.class);
        verify(mockDataPersistenceProvider).saveSnapshot(snapshotArgumentCaptor.capture());

        Snapshot snapshot = snapshotArgumentCaptor.getValue();

        assertEquals("getLastTerm", 3L, snapshot.getLastTerm());
        assertEquals("getLastIndex", 9L, snapshot.getLastIndex());
        assertEquals("getLastAppliedTerm", 2L, snapshot.getLastAppliedTerm());
        assertEquals("getLastAppliedIndex", 8L, snapshot.getLastAppliedIndex());
        assertEquals("getState", snapshotState, snapshot.getState());
        assertEquals("getUnAppliedEntries", List.of(lastLogEntry), snapshot.getUnAppliedEntries());
        assertEquals("electionTerm", mockElectionTerm.getCurrentTerm(), snapshot.getElectionTerm());
        assertEquals("electionVotedFor", mockElectionTerm.getVotedFor(), snapshot.getElectionVotedFor());

        verify(mockReplicatedLog).snapshotPreCommit(7L, 1L);
    }

    @Test
    public void testPersistWhenReplicatedToAllIndexNotMinus() {
        doReturn(45L).when(mockReplicatedLog).getSnapshotIndex();
        doReturn(6L).when(mockReplicatedLog).getSnapshotTerm();
        ReplicatedLogEntry replicatedLogEntry = mock(ReplicatedLogEntry.class);
        doReturn(null).when(mockReplicatedLog).get(0);
        doReturn(replicatedLogEntry).when(mockReplicatedLog).get(9);
        doReturn(6L).when(replicatedLogEntry).getTerm();
        doReturn(9L).when(replicatedLogEntry).getIndex();

        // when replicatedToAllIndex != -1
        snapshotManager.capture(new SimpleReplicatedLogEntry(9, 6, new MockRaftActorContext.MockPayload()), 9);

        ByteState snapshotState = ByteState.of(new byte[] {1,2,3,4,5,6,7,8,9,10});
        snapshotManager.persist(snapshotState, Optional.empty(), Runtime.getRuntime().totalMemory());

        ArgumentCaptor<Snapshot> snapshotArgumentCaptor = ArgumentCaptor.forClass(Snapshot.class);
        verify(mockDataPersistenceProvider).saveSnapshot(snapshotArgumentCaptor.capture());

        Snapshot snapshot = snapshotArgumentCaptor.getValue();

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
        snapshotManager.capture(new SimpleReplicatedLogEntry(9, 6, new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.persist(ByteState.empty(), Optional.empty(), Runtime.getRuntime().totalMemory());

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
        ReplicatedLogEntry replicatedLogEntry = mock(ReplicatedLogEntry.class);
        doReturn(null).when(mockReplicatedLog).get(0);
        doReturn(replicatedLogEntry).when(mockReplicatedLog).get(replicatedToAllIndex);
        doReturn(6L).when(replicatedLogEntry).getTerm();
        doReturn(replicatedToAllIndex).when(replicatedLogEntry).getIndex();

        snapshotManager.capture(new SimpleReplicatedLogEntry(9, 6,
                new MockRaftActorContext.MockPayload()), replicatedToAllIndex);

        snapshotManager.persist(ByteState.empty(), Optional.empty(), 2000000L);

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior).setReplicatedToAllIndex(replicatedToAllIndex);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testPersistSendInstallSnapshot() throws Exception {
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();
        doNothing().when(mockProcedure).accept(any());

        // when replicatedToAllIndex = -1
        boolean capture = snapshotManager.captureToInstall(new SimpleReplicatedLogEntry(9, 6,
                new MockRaftActorContext.MockPayload()), -1, "follower-1");

        assertTrue(capture);

        ByteState snapshotState = ByteState.of(new byte[] {1,2,3,4,5,6,7,8,9,10});

        ArgumentCaptor<Optional> installSnapshotStreamCapture = ArgumentCaptor.forClass(Optional.class);
        verify(mockProcedure).accept(installSnapshotStreamCapture.capture());

        Optional<OutputStream> installSnapshotStream = installSnapshotStreamCapture.getValue();
        assertEquals("isPresent", true, installSnapshotStream.isPresent());

        installSnapshotStream.orElseThrow().write(snapshotState.getBytes());

        snapshotManager.persist(snapshotState, installSnapshotStream, Runtime.getRuntime().totalMemory());

        assertTrue(snapshotManager.isCapturing());

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        ArgumentCaptor<SendInstallSnapshot> sendInstallSnapshotArgumentCaptor
                = ArgumentCaptor.forClass(SendInstallSnapshot.class);

        verify(mockRaftActorBehavior).handleMessage(any(ActorRef.class), sendInstallSnapshotArgumentCaptor.capture());

        SendInstallSnapshot sendInstallSnapshot = sendInstallSnapshotArgumentCaptor.getValue();

        assertEquals("state", snapshotState, sendInstallSnapshot.getSnapshot().getState());
        assertArrayEquals("state", snapshotState.getBytes(), sendInstallSnapshot.getSnapshotBytes().read());
    }

    @Test
    public void testCallingPersistWithoutCaptureWillDoNothing() {
        snapshotManager.persist(ByteState.empty(), Optional.empty(), Runtime.getRuntime().totalMemory());

        verify(mockDataPersistenceProvider, never()).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog, never()).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior, never()).handleMessage(any(ActorRef.class), any(SendInstallSnapshot.class));
    }

    @Test
    public void testCallingPersistTwiceWillDoNoHarm() {
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.capture(new SimpleReplicatedLogEntry(9, 6, new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.persist(ByteState.empty(), Optional.empty(), Runtime.getRuntime().totalMemory());

        snapshotManager.persist(ByteState.empty(), Optional.empty(), Runtime.getRuntime().totalMemory());

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);
    }

    @Test
    public void testCommit() {
        doReturn(50L).when(mockDataPersistenceProvider).getLastSequenceNumber();

        // when replicatedToAllIndex = -1
        snapshotManager.capture(new SimpleReplicatedLogEntry(9, 6, new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.persist(ByteState.empty(), Optional.empty(), Runtime.getRuntime().totalMemory());

        assertTrue(snapshotManager.isCapturing());

        snapshotManager.commit(100L, 1234L);

        assertFalse(snapshotManager.isCapturing());

        verify(mockReplicatedLog).snapshotCommit();

        verify(mockDataPersistenceProvider).deleteMessages(50L);

        ArgumentCaptor<SnapshotSelectionCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(SnapshotSelectionCriteria.class);

        verify(mockDataPersistenceProvider).deleteSnapshots(criteriaCaptor.capture());

        assertEquals(Long.MAX_VALUE, criteriaCaptor.getValue().maxSequenceNr());
        assertEquals(1233L, criteriaCaptor.getValue().maxTimestamp());

        MessageCollectorActor.expectFirstMatching(actorRef, SnapshotComplete.class);
    }

    @Test
    public void testCommitBeforePersist() {
        // when replicatedToAllIndex = -1
        snapshotManager.capture(new SimpleReplicatedLogEntry(9, 6, new MockRaftActorContext.MockPayload()), -1);

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
        snapshotManager.capture(new SimpleReplicatedLogEntry(9, 6, new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.persist(ByteState.empty(), Optional.empty(), Runtime.getRuntime().totalMemory());

        snapshotManager.commit(100L, 0);

        snapshotManager.commit(100L, 0);

        verify(mockReplicatedLog, times(1)).snapshotCommit();

        verify(mockDataPersistenceProvider, times(1)).deleteMessages(50L);

        verify(mockDataPersistenceProvider, times(1)).deleteSnapshots(any(SnapshotSelectionCriteria.class));
    }

    @Test
    public void testRollback() {
        // when replicatedToAllIndex = -1
        snapshotManager.capture(new SimpleReplicatedLogEntry(9, 6, new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.persist(ByteState.empty(), Optional.empty(), Runtime.getRuntime().totalMemory());

        snapshotManager.rollback();

        verify(mockReplicatedLog).snapshotRollback();

        MessageCollectorActor.expectFirstMatching(actorRef, SnapshotComplete.class);
    }


    @Test
    public void testRollbackBeforePersist() {
        // when replicatedToAllIndex = -1
        snapshotManager.capture(new SimpleReplicatedLogEntry(9, 6, new MockRaftActorContext.MockPayload()), -1);

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
        snapshotManager.capture(new SimpleReplicatedLogEntry(9, 6, new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.persist(ByteState.empty(), Optional.empty(), Runtime.getRuntime().totalMemory());

        snapshotManager.rollback();

        snapshotManager.rollback();

        verify(mockReplicatedLog, times(1)).snapshotRollback();
    }

    @Test
    public void testTrimLogWhenTrimIndexLessThanLastApplied() {
        doReturn(20L).when(mockRaftActorContext).getLastApplied();

        ReplicatedLogEntry replicatedLogEntry = mock(ReplicatedLogEntry.class);
        doReturn(true).when(mockReplicatedLog).isPresent(10);
        doReturn(replicatedLogEntry).when(mockReplicatedLog).get(10);
        doReturn(5L).when(replicatedLogEntry).getTerm();

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
        boolean capture = snapshotManager.capture(new SimpleReplicatedLogEntry(9, 1,
                new MockRaftActorContext.MockPayload()), 9);

        assertTrue(capture);

        assertTrue(snapshotManager.isCapturing());

        snapshotManager.trimLog(10);

        verify(mockReplicatedLog, never()).snapshotPreCommit(anyLong(), anyLong());
        verify(mockReplicatedLog, never()).snapshotCommit(false);
    }

    @Test
    public void testTrimLogAfterCaptureToInstall() {
        boolean capture = snapshotManager.capture(new SimpleReplicatedLogEntry(9, 1,
                new MockRaftActorContext.MockPayload()), 9);

        assertTrue(capture);

        assertTrue(snapshotManager.isCapturing());

        snapshotManager.trimLog(10);

        verify(mockReplicatedLog, never()).snapshotPreCommit(10, 5);
        verify(mockReplicatedLog, never()).snapshotCommit();
    }

    @Test
    public void testLastAppliedTermInformationReader() {

        LastAppliedTermInformationReader reader = new LastAppliedTermInformationReader();

        doReturn(4L).when(mockReplicatedLog).getSnapshotTerm();
        doReturn(7L).when(mockReplicatedLog).getSnapshotIndex();

        ReplicatedLogEntry lastLogEntry = new SimpleReplicatedLogEntry(9L, 6L,
                new MockRaftActorContext.MockPayload());

        // No followers and valid lastLogEntry
        reader.init(mockReplicatedLog, 1L, lastLogEntry, false);

        assertEquals("getTerm", 6L, reader.getTerm());
        assertEquals("getIndex", 9L, reader.getIndex());

        // No followers and null lastLogEntry
        reader.init(mockReplicatedLog, 1L, null, false);

        assertEquals("getTerm", -1L, reader.getTerm());
        assertEquals("getIndex", -1L, reader.getIndex());

        // Followers and valid originalIndex entry
        doReturn(new SimpleReplicatedLogEntry(8L, 5L,
                new MockRaftActorContext.MockPayload())).when(mockReplicatedLog).get(8L);
        reader.init(mockReplicatedLog, 8L, lastLogEntry, true);

        assertEquals("getTerm", 5L, reader.getTerm());
        assertEquals("getIndex", 8L, reader.getIndex());

        // Followers and null originalIndex entry and valid snapshot index
        reader.init(mockReplicatedLog, 7L, lastLogEntry, true);

        assertEquals("getTerm", 4L, reader.getTerm());
        assertEquals("getIndex", 7L, reader.getIndex());

        // Followers and null originalIndex entry and invalid snapshot index
        doReturn(-1L).when(mockReplicatedLog).getSnapshotIndex();
        reader.init(mockReplicatedLog, 7L, lastLogEntry, true);

        assertEquals("getTerm", -1L, reader.getTerm());
        assertEquals("getIndex", -1L, reader.getIndex());
    }
}
