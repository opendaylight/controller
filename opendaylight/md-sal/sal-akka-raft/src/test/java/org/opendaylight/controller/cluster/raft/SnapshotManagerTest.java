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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import akka.actor.ActorRef;
import akka.japi.Procedure;
import akka.persistence.SnapshotSelectionCriteria;
import akka.testkit.TestActorRef;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.SnapshotManager.LastAppliedTermInformationReader;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.slf4j.LoggerFactory;

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
    private Procedure<Void> mockProcedure;

    @Mock
    private ElectionTerm mockElectionTerm;

    private SnapshotManager snapshotManager;

    private TestActorFactory factory;

    private TestActorRef<MessageCollectorActor> actorRef;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);

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

        snapshotManager = new SnapshotManager(mockRaftActorContext, LoggerFactory.getLogger(this.getClass()));
        factory = new TestActorFactory(getSystem());

        actorRef = factory.createTestActor(MessageCollectorActor.props(), factory.generateActorId("test-"));
        doReturn(actorRef).when(mockRaftActorContext).getActor();

        snapshotManager.setCreateSnapshotCallable(mockProcedure);
    }

    @After
    public void tearDown(){
        factory.close();
    }

    @Test
    public void testConstruction(){
        assertEquals(false, snapshotManager.isCapturing());
    }

    @Test
    public void testCaptureToInstall() throws Exception {

        // Force capturing toInstall = true
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(1, 0,
                new MockRaftActorContext.MockPayload()), 0, "follower-1");

        assertEquals(true, snapshotManager.isCapturing());

        verify(mockProcedure).apply(null);

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
        actorRef.underlyingActor().clear();
    }

    @Test
    public void testCapture() throws Exception {
        boolean capture = snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(1,9,
                new MockRaftActorContext.MockPayload()), 9);

        assertTrue(capture);

        assertEquals(true, snapshotManager.isCapturing());

        verify(mockProcedure).apply(null);

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

        actorRef.underlyingActor().clear();

    }

    @Test
    public void testCaptureWithNullLastLogEntry() throws Exception {
        boolean capture = snapshotManager.capture(null, 1);

        assertTrue(capture);

        assertEquals(true, snapshotManager.isCapturing());

        verify(mockProcedure).apply(null);

        CaptureSnapshot captureSnapshot = snapshotManager.getCaptureSnapshot();

        System.out.println(captureSnapshot);

        // LastIndex and LastTerm are picked up from the lastLogEntry
        assertEquals(-1L, captureSnapshot.getLastIndex());
        assertEquals(-1L, captureSnapshot.getLastTerm());

        // Since the actor does not have any followers (no peer addresses) lastApplied will be from lastLogEntry
        assertEquals(-1L, captureSnapshot.getLastAppliedIndex());
        assertEquals(-1L, captureSnapshot.getLastAppliedTerm());

        //
        assertEquals(-1L, captureSnapshot.getReplicatedToAllIndex());
        assertEquals(-1L, captureSnapshot.getReplicatedToAllTerm());
        actorRef.underlyingActor().clear();

    }

    @Test
    public void testCaptureWithCreateProcedureError () throws Exception {
        doThrow(new Exception("mock")).when(mockProcedure).apply(null);

        boolean capture = snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(1,9,
                new MockRaftActorContext.MockPayload()), 9);

        assertFalse(capture);

        assertEquals(false, snapshotManager.isCapturing());

        verify(mockProcedure).apply(null);
    }

    @Test
    public void testIllegalCapture() throws Exception {
        boolean capture = snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(1,9,
                new MockRaftActorContext.MockPayload()), 9);

        assertTrue(capture);

        verify(mockProcedure).apply(null);

        reset(mockProcedure);

        // This will not cause snapshot capture to start again
        capture = snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(1,9,
                new MockRaftActorContext.MockPayload()), 9);

        assertFalse(capture);

        verify(mockProcedure, never()).apply(null);
    }

    @Test
    public void testPersistWhenReplicatedToAllIndexMinusOne(){
        doReturn(7L).when(mockReplicatedLog).getSnapshotIndex();
        doReturn(1L).when(mockReplicatedLog).getSnapshotTerm();

        doReturn(true).when(mockRaftActorContext).hasFollowers();

        doReturn(8L).when(mockRaftActorContext).getLastApplied();

        MockRaftActorContext.MockReplicatedLogEntry lastLogEntry = new MockRaftActorContext.MockReplicatedLogEntry(
                3L, 9L, new MockRaftActorContext.MockPayload());

        MockRaftActorContext.MockReplicatedLogEntry lastAppliedEntry = new MockRaftActorContext.MockReplicatedLogEntry(
                2L, 8L, new MockRaftActorContext.MockPayload());

        doReturn(lastAppliedEntry).when(mockReplicatedLog).get(8L);
        doReturn(Arrays.asList(lastLogEntry)).when(mockReplicatedLog).getFrom(9L);

        // when replicatedToAllIndex = -1
        snapshotManager.capture(lastLogEntry, -1);

        byte[] bytes = new byte[] {1,2,3,4,5,6,7,8,9,10};
        snapshotManager.persist(bytes, Runtime.getRuntime().totalMemory());

        ArgumentCaptor<Snapshot> snapshotArgumentCaptor = ArgumentCaptor.forClass(Snapshot.class);
        verify(mockDataPersistenceProvider).saveSnapshot(snapshotArgumentCaptor.capture());

        Snapshot snapshot = snapshotArgumentCaptor.getValue();

        assertEquals("getLastTerm", 3L, snapshot.getLastTerm());
        assertEquals("getLastIndex", 9L, snapshot.getLastIndex());
        assertEquals("getLastAppliedTerm", 2L, snapshot.getLastAppliedTerm());
        assertEquals("getLastAppliedIndex", 8L, snapshot.getLastAppliedIndex());
        assertArrayEquals("getState", bytes, snapshot.getState());
        assertEquals("getUnAppliedEntries", Arrays.asList(lastLogEntry), snapshot.getUnAppliedEntries());
        assertEquals("electionTerm", mockElectionTerm.getCurrentTerm(), snapshot.getElectionTerm());
        assertEquals("electionVotedFor", mockElectionTerm.getVotedFor(), snapshot.getElectionVotedFor());

        verify(mockReplicatedLog).snapshotPreCommit(7L, 1L);
    }

    @Test
    public void testPersistWhenReplicatedToAllIndexNotMinus(){
        doReturn(45L).when(mockReplicatedLog).getSnapshotIndex();
        doReturn(6L).when(mockReplicatedLog).getSnapshotTerm();
        ReplicatedLogEntry replicatedLogEntry = mock(ReplicatedLogEntry.class);
        doReturn(replicatedLogEntry).when(mockReplicatedLog).get(9);
        doReturn(6L).when(replicatedLogEntry).getTerm();
        doReturn(9L).when(replicatedLogEntry).getIndex();

        // when replicatedToAllIndex != -1
        snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(6,9,
                new MockRaftActorContext.MockPayload()), 9);

        byte[] bytes = new byte[] {1,2,3,4,5,6,7,8,9,10};
        snapshotManager.persist(bytes, Runtime.getRuntime().totalMemory());

        ArgumentCaptor<Snapshot> snapshotArgumentCaptor = ArgumentCaptor.forClass(Snapshot.class);
        verify(mockDataPersistenceProvider).saveSnapshot(snapshotArgumentCaptor.capture());

        Snapshot snapshot = snapshotArgumentCaptor.getValue();

        assertEquals("getLastTerm", 6L, snapshot.getLastTerm());
        assertEquals("getLastIndex", 9L, snapshot.getLastIndex());
        assertEquals("getLastAppliedTerm", 6L, snapshot.getLastAppliedTerm());
        assertEquals("getLastAppliedIndex", 9L, snapshot.getLastAppliedIndex());
        assertArrayEquals("getState", bytes, snapshot.getState());
        assertEquals("getUnAppliedEntries size", 0, snapshot.getUnAppliedEntries().size());

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior).setReplicatedToAllIndex(9);
    }

    @Test
    public void testPersistWhenReplicatedLogDataSizeGreaterThanThreshold(){
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(6,9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.persist(new byte[]{}, Runtime.getRuntime().totalMemory());

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
        doReturn(replicatedLogEntry).when(mockReplicatedLog).get(replicatedToAllIndex);
        doReturn(6L).when(replicatedLogEntry).getTerm();
        doReturn(replicatedToAllIndex).when(replicatedLogEntry).getIndex();

        snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), replicatedToAllIndex);

        snapshotManager.persist(new byte[]{}, 2000000L);

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior).setReplicatedToAllIndex(replicatedToAllIndex);
    }

    @Test
    public void testPersistSendInstallSnapshot(){
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        boolean capture = snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1, "follower-1");

        assertTrue(capture);

        byte[] bytes = new byte[] {1,2,3,4,5,6,7,8,9,10};

        snapshotManager.persist(bytes, Runtime.getRuntime().totalMemory());

        assertEquals(true, snapshotManager.isCapturing());

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        ArgumentCaptor<SendInstallSnapshot> sendInstallSnapshotArgumentCaptor
                = ArgumentCaptor.forClass(SendInstallSnapshot.class);

        verify(mockRaftActorBehavior).handleMessage(any(ActorRef.class), sendInstallSnapshotArgumentCaptor.capture());

        SendInstallSnapshot sendInstallSnapshot = sendInstallSnapshotArgumentCaptor.getValue();

        assertTrue(Arrays.equals(bytes, sendInstallSnapshot.getSnapshot().getState()));
    }

    @Test
    public void testCallingPersistWithoutCaptureWillDoNothing(){
        snapshotManager.persist(new byte[]{}, Runtime.getRuntime().totalMemory());

        verify(mockDataPersistenceProvider, never()).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog, never()).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior, never()).handleMessage(any(ActorRef.class), any(SendInstallSnapshot.class));
    }
    @Test
    public void testCallingPersistTwiceWillDoNoHarm(){
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1, "follower-1");

        snapshotManager.persist(new byte[]{}, Runtime.getRuntime().totalMemory());

        snapshotManager.persist(new byte[]{}, Runtime.getRuntime().totalMemory());

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior).handleMessage(any(ActorRef.class), any(SendInstallSnapshot.class));
    }

    @Test
    public void testCommit(){
        doReturn(50L).when(mockDataPersistenceProvider).getLastSequenceNumber();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1, "follower-1");

        snapshotManager.persist(new byte[]{}, Runtime.getRuntime().totalMemory());

        assertEquals(true, snapshotManager.isCapturing());

        snapshotManager.commit(100L);

        assertEquals(false, snapshotManager.isCapturing());

        verify(mockReplicatedLog).snapshotCommit();

        verify(mockDataPersistenceProvider).deleteMessages(50L);

        ArgumentCaptor<SnapshotSelectionCriteria> criteriaCaptor = ArgumentCaptor.forClass(SnapshotSelectionCriteria.class);

        verify(mockDataPersistenceProvider).deleteSnapshots(criteriaCaptor.capture());

        assertEquals(90, criteriaCaptor.getValue().maxSequenceNr()); // sequenceNumber = 100
                                                                     // config snapShotBatchCount = 10
                                                                     // therefore maxSequenceNumber = 90

        MessageCollectorActor.expectFirstMatching(actorRef, SnapshotComplete.class);
    }

    @Test
    public void testCommitBeforePersist(){
        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1, "follower-1");

        snapshotManager.commit(100L);

        verify(mockReplicatedLog, never()).snapshotCommit();

        verify(mockDataPersistenceProvider, never()).deleteMessages(100L);

        verify(mockDataPersistenceProvider, never()).deleteSnapshots(any(SnapshotSelectionCriteria.class));

    }

    @Test
    public void testCommitBeforeCapture(){
        snapshotManager.commit(100L);

        verify(mockReplicatedLog, never()).snapshotCommit();

        verify(mockDataPersistenceProvider, never()).deleteMessages(anyLong());

        verify(mockDataPersistenceProvider, never()).deleteSnapshots(any(SnapshotSelectionCriteria.class));

    }

    @Test
    public void testCallingCommitMultipleTimesCausesNoHarm(){
        doReturn(50L).when(mockDataPersistenceProvider).getLastSequenceNumber();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1, "follower-1");

        snapshotManager.persist(new byte[]{}, Runtime.getRuntime().totalMemory());

        snapshotManager.commit(100L);

        snapshotManager.commit(100L);

        verify(mockReplicatedLog, times(1)).snapshotCommit();

        verify(mockDataPersistenceProvider, times(1)).deleteMessages(50L);

        verify(mockDataPersistenceProvider, times(1)).deleteSnapshots(any(SnapshotSelectionCriteria.class));
    }

    @Test
    public void testRollback(){
        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1, "follower-1");

        snapshotManager.persist(new byte[]{}, Runtime.getRuntime().totalMemory());

        snapshotManager.rollback();

        verify(mockReplicatedLog).snapshotRollback();

        MessageCollectorActor.expectFirstMatching(actorRef, SnapshotComplete.class);
    }


    @Test
    public void testRollbackBeforePersist(){
        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1, "follower-1");

        snapshotManager.rollback();

        verify(mockReplicatedLog, never()).snapshotRollback();
    }

    @Test
    public void testRollbackBeforeCapture(){
        snapshotManager.rollback();

        verify(mockReplicatedLog, never()).snapshotRollback();
    }

    @Test
    public void testCallingRollbackMultipleTimesCausesNoHarm(){
        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1, "follower-1");

        snapshotManager.persist(new byte[]{}, Runtime.getRuntime().totalMemory());

        snapshotManager.rollback();

        snapshotManager.rollback();

        verify(mockReplicatedLog, times(1)).snapshotRollback();
    }

    @Test
    public void testTrimLogWhenTrimIndexLessThanLastApplied() {
        doReturn(20L).when(mockRaftActorContext).getLastApplied();

        ReplicatedLogEntry replicatedLogEntry = mock(ReplicatedLogEntry.class);
        doReturn(true).when(mockReplicatedLog).isPresent(10);
        doReturn(replicatedLogEntry).when((mockReplicatedLog)).get(10);
        doReturn(5L).when(replicatedLogEntry).getTerm();

        long retIndex = snapshotManager.trimLog(10);
        assertEquals("return index", 10L, retIndex);

        verify(mockReplicatedLog).snapshotPreCommit(10, 5);
        verify(mockReplicatedLog).snapshotCommit();

        verify(mockRaftActorBehavior, never()).setReplicatedToAllIndex(anyLong());
    }

    @Test
    public void testTrimLogWhenLastAppliedNotSet() {
        doReturn(-1L).when(mockRaftActorContext).getLastApplied();

        ReplicatedLogEntry replicatedLogEntry = mock(ReplicatedLogEntry.class);
        doReturn(true).when(mockReplicatedLog).isPresent(10);
        doReturn(replicatedLogEntry).when((mockReplicatedLog)).get(10);
        doReturn(5L).when(replicatedLogEntry).getTerm();

        long retIndex = snapshotManager.trimLog(10);
        assertEquals("return index", -1L, retIndex);

        verify(mockReplicatedLog, never()).snapshotPreCommit(anyLong(), anyLong());
        verify(mockReplicatedLog, never()).snapshotCommit();

        verify(mockRaftActorBehavior, never()).setReplicatedToAllIndex(anyLong());
    }

    @Test
    public void testTrimLogWhenLastAppliedZero() {
        doReturn(0L).when(mockRaftActorContext).getLastApplied();

        ReplicatedLogEntry replicatedLogEntry = mock(ReplicatedLogEntry.class);
        doReturn(true).when(mockReplicatedLog).isPresent(10);
        doReturn(replicatedLogEntry).when((mockReplicatedLog)).get(10);
        doReturn(5L).when(replicatedLogEntry).getTerm();

        long retIndex = snapshotManager.trimLog(10);
        assertEquals("return index", -1L, retIndex);

        verify(mockReplicatedLog, never()).snapshotPreCommit(anyLong(), anyLong());
        verify(mockReplicatedLog, never()).snapshotCommit();

        verify(mockRaftActorBehavior, never()).setReplicatedToAllIndex(anyLong());
    }

    @Test
    public void testTrimLogWhenTrimIndexNotPresent() {
        doReturn(20L).when(mockRaftActorContext).getLastApplied();

        doReturn(false).when(mockReplicatedLog).isPresent(10);

        long retIndex = snapshotManager.trimLog(10);
        assertEquals("return index", -1L, retIndex);

        verify(mockReplicatedLog, never()).snapshotPreCommit(anyLong(), anyLong());
        verify(mockReplicatedLog, never()).snapshotCommit();

        // Trim index is greater than replicatedToAllIndex so should update it.
        verify(mockRaftActorBehavior).setReplicatedToAllIndex(10L);
    }

    @Test
    public void testTrimLogAfterCapture(){
        boolean capture = snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(1,9,
                new MockRaftActorContext.MockPayload()), 9);

        assertTrue(capture);

        assertEquals(true, snapshotManager.isCapturing());

        ReplicatedLogEntry replicatedLogEntry = mock(ReplicatedLogEntry.class);
        doReturn(20L).when(mockRaftActorContext).getLastApplied();
        doReturn(true).when(mockReplicatedLog).isPresent(10);
        doReturn(replicatedLogEntry).when((mockReplicatedLog)).get(10);
        doReturn(5L).when(replicatedLogEntry).getTerm();

        snapshotManager.trimLog(10);

        verify(mockReplicatedLog, never()).snapshotPreCommit(anyLong(), anyLong());
        verify(mockReplicatedLog, never()).snapshotCommit();

    }

    @Test
    public void testTrimLogAfterCaptureToInstall(){
        boolean capture = snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(1,9,
                new MockRaftActorContext.MockPayload()), 9, "follower-1");

        assertTrue(capture);

        assertEquals(true, snapshotManager.isCapturing());

        ReplicatedLogEntry replicatedLogEntry = mock(ReplicatedLogEntry.class);
        doReturn(20L).when(mockRaftActorContext).getLastApplied();
        doReturn(true).when(mockReplicatedLog).isPresent(10);
        doReturn(replicatedLogEntry).when((mockReplicatedLog)).get(10);
        doReturn(5L).when(replicatedLogEntry).getTerm();

        snapshotManager.trimLog(10);

        verify(mockReplicatedLog, never()).snapshotPreCommit(10, 5);
        verify(mockReplicatedLog, never()).snapshotCommit();

    }

    @Test
    public void testLastAppliedTermInformationReader() {

        LastAppliedTermInformationReader reader = new LastAppliedTermInformationReader();

        doReturn(4L).when(mockReplicatedLog).getSnapshotTerm();
        doReturn(7L).when(mockReplicatedLog).getSnapshotIndex();

        ReplicatedLogEntry lastLogEntry = new MockRaftActorContext.MockReplicatedLogEntry(6L, 9L,
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
        doReturn(new MockRaftActorContext.MockReplicatedLogEntry(5L, 8L,
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
