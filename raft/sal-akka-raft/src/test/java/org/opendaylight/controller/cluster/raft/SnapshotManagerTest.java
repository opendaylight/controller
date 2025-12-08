/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opendaylight.controller.cluster.raft.SnapshotManager.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.SnapshotManager.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.persisted.ByteStateSnapshotCohort;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.EntryStore;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.ByteArray;
import org.opendaylight.raft.spi.InstallableSnapshot;
import org.opendaylight.raft.spi.InstallableSnapshotSource;
import org.opendaylight.raft.spi.PlainSnapshotSource;

@ExtendWith(MockitoExtension.class)
// FIXME: remove this line
@MockitoSettings(strictness = Strictness.LENIENT)
class SnapshotManagerTest extends AbstractActorTest {
    @TempDir
    private Path tempDir;
    @Mock
    private RaftActorContext mockRaftActorContext;
    @Mock
    private ConfigParams mockConfigParams;
    @Mock
    private AbstractReplicatedLog<ReplicatedLogEntry> mockReplicatedLog;
    @Mock
    private EntryStore mockEntryStore;
    @Mock
    private SnapshotStore mockSnapshotStore;
    @Mock
    private Leader mockRaftActorBehavior;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ByteStateSnapshotCohort mockCohort;
    @Mock
    private ReplicatedLogEntry replicatedLogEntry;
    @Captor
    private ArgumentCaptor<OutputStream> outputStreamCaptor;
    @Captor
    private ArgumentCaptor<ToStorage<?>> snapshotCaptor;
    @Captor
    private ArgumentCaptor<EntryInfo> entryInfoCaptor;
    @Captor
    private ArgumentCaptor<RaftSnapshot> raftSnapshotCaptor;
    @Captor
    private ArgumentCaptor<RaftCallback<InstallableSnapshot>> callbackCaptor;

    private SnapshotManager snapshotManager;
    private TestActorFactory factory;
    private MessageCollector actorRef;

    @BeforeEach
    void beforeEach() {
        doReturn(false).when(mockRaftActorContext).hasFollowers();
        doReturn(mockConfigParams).when(mockRaftActorContext).getConfigParams();
        doReturn(10L).when(mockConfigParams).getSnapshotBatchCount();
        doReturn(70).when(mockConfigParams).getSnapshotDataThresholdPercentage();
        doReturn(mockReplicatedLog).when(mockRaftActorContext).getReplicatedLog();
        doReturn("123").when(mockRaftActorContext).getId();
        doCallRealMethod().when(mockReplicatedLog).lookupMeta(anyLong());
        doReturn(mockEntryStore).when(mockRaftActorContext).entryStore();
        doReturn(mockSnapshotStore).when(mockRaftActorContext).snapshotStore();
        doReturn(mockRaftActorBehavior).when(mockRaftActorContext).getCurrentBehavior();
        doCallRealMethod().when(mockReplicatedLog).newCaptureSnapshot(any(), anyLong(), anyBoolean(), anyBoolean());

        snapshotManager = new SnapshotManager(mockRaftActorContext);
        factory = new TestActorFactory(getSystem());

        actorRef = MessageCollector.ofPrefix(factory, "test-");
        doReturn(actorRef).when(mockRaftActorContext).getActor();

        snapshotManager.setSnapshotCohort(mockCohort);
    }

    @AfterEach
    void afterEach() {
        factory.close();
    }

    @Test
    void testConstruction() {
        assertFalse(snapshotManager.isCapturing());
    }

    @Test
    void testCaptureToInstall() throws Exception {
        // Force capturing toInstall = true
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();
        snapshotManager.captureToInstall(EntryInfo.of(0, 1), 0, "follower-1");

        assertTrue(snapshotManager.isCapturing());
        verify(mockCohort).takeSnapshot();

        final var captureSnapshot = snapshotManager.getCaptureSnapshot();

        // LastIndex and LastTerm are picked up from the lastLogEntry
        assertEquals(0L, captureSnapshot.getLastIndex());
        assertEquals(1L, captureSnapshot.getLastTerm());

        // Since the actor does not have any followers (no peer addresses) lastApplied will be from lastLogEntry
        assertEquals(0L, captureSnapshot.getLastAppliedIndex());
        assertEquals(1L, captureSnapshot.getLastAppliedTerm());

        //
        assertEquals(-1L, captureSnapshot.getReplicatedToAllIndex());
        assertEquals(-1L, captureSnapshot.getReplicatedToAllTerm());
        actorRef.clearMessages();
    }

    @Test
    void testCapture() {
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();
        boolean capture = snapshotManager.capture(EntryInfo.of(9, 1), 9);
        assertTrue(capture);
        assertTrue(snapshotManager.isCapturing());
        verify(mockCohort).takeSnapshot();

        CaptureSnapshot captureSnapshot = snapshotManager.getCaptureSnapshot();
        assertNotNull(captureSnapshot);

        // LastIndex and LastTerm are picked up from the lastLogEntry
        assertEquals(9L, captureSnapshot.getLastIndex());
        assertEquals(1L, captureSnapshot.getLastTerm());

        // Since the actor does not have any followers (no peer addresses) lastApplied will be from lastLogEntry
        assertEquals(9L, captureSnapshot.getLastAppliedIndex());
        assertEquals(1L, captureSnapshot.getLastAppliedTerm());

        //
        assertEquals(-1L, captureSnapshot.getReplicatedToAllIndex());
        assertEquals(-1L, captureSnapshot.getReplicatedToAllTerm());

        actorRef.clearMessages();
    }

    @Test
    void testCaptureWithNullLastLogEntry() {
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();
        boolean capture = snapshotManager.capture(null, 1);
        assertTrue(capture);
        assertTrue(snapshotManager.isCapturing());
        verify(mockCohort).takeSnapshot();

        CaptureSnapshot captureSnapshot = snapshotManager.getCaptureSnapshot();
        assertNotNull(captureSnapshot);

        // LastIndex and LastTerm are picked up from the lastLogEntry
        assertEquals(0, captureSnapshot.getLastIndex());
        assertEquals(0, captureSnapshot.getLastTerm());

        // Since the actor does not have any followers (no peer addresses) lastApplied will be from lastLogEntry
        assertEquals(0, captureSnapshot.getLastAppliedIndex());
        assertEquals(0, captureSnapshot.getLastAppliedTerm());

        //
        assertEquals(-1L, captureSnapshot.getReplicatedToAllIndex());
        assertEquals(-1L, captureSnapshot.getReplicatedToAllTerm());

        actorRef.clearMessages();
    }

    @Test
    void testIllegalCapture() {
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();
        assertTrue(snapshotManager.capture(EntryInfo.of(9, 1), 9));
        verify(mockCohort).takeSnapshot();

        // This will not cause snapshot capture to start again
        reset(mockCohort);
        assertFalse(snapshotManager.capture(EntryInfo.of(9, 1), 9));
        verifyNoInteractions(mockCohort);
    }

    @Test
    void testPersistWhenReplicatedToAllIndexMinusOne() {
        doReturn(7L).when(mockReplicatedLog).getSnapshotIndex();
        doReturn(1L).when(mockReplicatedLog).getSnapshotTerm();

        doReturn(true).when(mockRaftActorContext).hasFollowers();

        doReturn(8L).when(mockReplicatedLog).getLastApplied();

        final var lastLogEntry = new SimpleReplicatedLogEntry(9L, 3L, new MockCommand(""));
        final var lastAppliedEntry = new SimpleReplicatedLogEntry(8L, 2L, new MockCommand(""));

        doReturn(lastAppliedEntry).when(mockReplicatedLog).lookup(8L);
        doReturn(List.of(lastLogEntry)).when(mockReplicatedLog).getFrom(9L);

        // when replicatedToAllIndex = -1
        final var snapshotState = ByteState.of(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        doReturn(snapshotState).when(mockCohort).takeSnapshot();
        snapshotManager.capture(lastLogEntry, -1);

        verify(mockSnapshotStore).saveSnapshot(raftSnapshotCaptor.capture(), entryInfoCaptor.capture(),
            snapshotCaptor.capture(), any(RaftCallback.class));

        final var raftSnapshot = raftSnapshotCaptor.getValue();
        assertEquals(List.of(), raftSnapshot.unappliedEntries());

        assertEquals(EntryInfo.of(8, 2), entryInfoCaptor.getValue());
        assertEquals(snapshotState, snapshotCaptor.getValue().snapshot());

        verify(mockReplicatedLog).snapshotPreCommit(7L, 1L);
    }

    @Test
    void testPersistWhenReplicatedToAllIndexNotMinus() {
        doReturn(45L).when(mockReplicatedLog).getSnapshotIndex();
        doReturn(6L).when(mockReplicatedLog).getSnapshotTerm();
        doReturn(replicatedLogEntry).when(mockReplicatedLog).lookup(9);
        doReturn(6L).when(replicatedLogEntry).term();
        doReturn(9L).when(replicatedLogEntry).index();

        final var snapshotState = ByteState.of(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        doReturn(snapshotState).when(mockCohort).takeSnapshot();

        // when replicatedToAllIndex != -1
        snapshotManager.capture(EntryInfo.of(9, 6), 9);

        verify(mockSnapshotStore).saveSnapshot(raftSnapshotCaptor.capture(), entryInfoCaptor.capture(),
            snapshotCaptor.capture(), any(RaftCallback.class));

        assertEquals(List.of(), raftSnapshotCaptor.getValue().unappliedEntries());
        assertEquals(EntryInfo.of(9, 6), entryInfoCaptor.getValue());
        assertEquals(snapshotState, snapshotCaptor.getValue().snapshot());

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior).setReplicatedToAllIndex(9);
    }

    @Test
    void testPersistWhenReplicatedLogDataSizeGreaterThanThreshold() {
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();
        snapshotManager.capture(EntryInfo.of(9, 6), -1);

        verify(mockSnapshotStore).saveSnapshot(any(), any(), any(), any(RaftCallback.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior, never()).setReplicatedToAllIndex(anyLong());
    }

    @Test
    void testPersistWhenReplicatedLogSizeExceedsSnapshotBatchCount() {
        doReturn(10L).when(mockReplicatedLog).size(); // matches snapshotBatchCount
        doReturn(100).when(mockReplicatedLog).dataSize();

        doReturn(5L).when(mockReplicatedLog).getSnapshotIndex();
        doReturn(5L).when(mockReplicatedLog).getSnapshotTerm();

        long replicatedToAllIndex = 1;
        doReturn(replicatedLogEntry).when(mockReplicatedLog).lookup(replicatedToAllIndex);
        doReturn(6L).when(replicatedLogEntry).term();
        doReturn(replicatedToAllIndex).when(replicatedLogEntry).index();

        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();
        snapshotManager.capture(EntryInfo.of(9, 6), replicatedToAllIndex);

        verify(mockSnapshotStore).saveSnapshot(any(), any(), any(), any(RaftCallback.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior).setReplicatedToAllIndex(replicatedToAllIndex);
    }

    @Test
    void testPersistSendInstallSnapshot() throws Exception {

        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();
        ByteState snapshotState = ByteState.of(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        doReturn(snapshotState).when(mockCohort).takeSnapshot();
        doCallRealMethod().when(mockCohort).support();

        // when replicatedToAllIndex = -1
        boolean capture = snapshotManager.captureToInstall(EntryInfo.of(9, 6), -1, "follower-1");

        assertTrue(capture);

        verify(mockCohort).takeSnapshot();

        final var toStorageCaptor = ArgumentCaptor.<ToStorage<?>>captor();
        final var lastIncludedCaptor = ArgumentCaptor.forClass(EntryInfo.class);
        verify(mockSnapshotStore).streamToInstall(lastIncludedCaptor.capture(), toStorageCaptor.capture(),
            callbackCaptor.capture());

        final var baos = new ByteArrayOutputStream();
        toStorageCaptor.getValue().writeTo(baos);
        final var result = ByteArray.wrap(baos.toByteArray());

        callbackCaptor.getValue().invoke(null, new InstallableSnapshotSource(lastIncludedCaptor.getValue(),
            new PlainSnapshotSource(result)));

        assertTrue(snapshotManager.isCapturing());

        verify(mockSnapshotStore).saveSnapshot(any(), any(), any(), any(RaftCallback.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        final var installCaptor = ArgumentCaptor.forClass(InstallableSnapshot.class);
        verify(mockRaftActorBehavior).sendInstallSnapshot(installCaptor.capture());
        final var install = installCaptor.getValue();
        assertEquals(EntryInfo.of(9, 6), install.lastIncluded());

        final var source = install.source();
        assertNotNull(source);

        assertEquals("state", snapshotState, ByteState.SUPPORT.reader().readSnapshot(source.io().openStream()));
    }

    @Test
    void testCallingPersistWithoutCaptureWillDoNothing() {
        snapshotManager.persist(ByteState.empty(), null);

        verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any(RaftCallback.class));

        verify(mockReplicatedLog, never()).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior, never()).sendInstallSnapshot(any());
    }

    @Test
    void testCallingPersistTwiceWillDoNoHarm() {
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(EntryInfo.of(9, 6), -1, "xyzzy");

        final var snapshot = new InstallableSnapshotSource(9, 6, new PlainSnapshotSource(ByteArray.wrap(new byte[0])));

        snapshotManager.persist(ByteState.empty(), snapshot);
        snapshotManager.persist(ByteState.empty(), snapshot);

        verify(mockSnapshotStore).saveSnapshot(any(), any(), any(), any(RaftCallback.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);
    }

    @Test
    void testCommit() throws Exception {
        // when replicatedToAllIndex = -1
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();
        snapshotManager.capture(EntryInfo.of(9, 6), -1);

        assertTrue(snapshotManager.isCapturing());

        final var timestamp = Instant.ofEpochMilli(1234);
        snapshotManager.commit(timestamp);

        assertFalse(snapshotManager.isCapturing());

        verify(mockReplicatedLog).snapshotCommit();

        verify(mockEntryStore).discardHead(1L);

        actorRef.expectFirstMatching(SnapshotComplete.class);
    }

    @Test
    void testCommitBeforePersist() throws Exception {
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(EntryInfo.of(9, 6), -1, "xyzzy");

        snapshotManager.commit(Instant.EPOCH);

        verify(mockReplicatedLog, never()).snapshotCommit();
        verify(mockEntryStore, never()).discardHead(100L);
    }

    @Test
    void testCommitBeforeCapture() throws Exception {
        snapshotManager.commit(Instant.EPOCH);

        verify(mockReplicatedLog, never()).snapshotCommit();
        verify(mockEntryStore, never()).discardHead(anyLong());

    }

    @Test
    void testCallingCommitMultipleTimesCausesNoHarm() throws Exception {
        // when replicatedToAllIndex = -1
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();
        snapshotManager.capture(EntryInfo.of(9, 6), -1);

        snapshotManager.commit(Instant.EPOCH);

        snapshotManager.commit(Instant.EPOCH);

        verify(mockReplicatedLog, times(1)).snapshotCommit();
        verify(mockEntryStore, times(1)).discardHead(1L);
    }

    @Test
    void testRollback() {
        // when replicatedToAllIndex = -1
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();
        snapshotManager.capture(EntryInfo.of(9, 6), -1);

        snapshotManager.rollback();

        verify(mockReplicatedLog).snapshotRollback();

        actorRef.expectFirstMatching(SnapshotComplete.class);
    }

    @Test
    void testRollbackBeforePersist() {
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(EntryInfo.of(9, 6), -1, "xyzzy");

        snapshotManager.rollback();

        verify(mockReplicatedLog, never()).snapshotRollback();
    }

    @Test
    void testRollbackBeforeCapture() {
        snapshotManager.rollback();

        verify(mockReplicatedLog, never()).snapshotRollback();
    }

    @Test
    void testCallingRollbackMultipleTimesCausesNoHarm() {
        // when replicatedToAllIndex = -1
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();
        snapshotManager.capture(EntryInfo.of(9, 6), -1);

        snapshotManager.rollback();
        snapshotManager.rollback();

        verify(mockReplicatedLog, times(1)).snapshotRollback();
    }

    @Test
    void testTrimLogWhenTrimIndexLessThanLastApplied() {
        doReturn(20L).when(mockReplicatedLog).getLastApplied();

        doReturn(true).when(mockReplicatedLog).isPresent(10);
        doReturn(replicatedLogEntry).when(mockReplicatedLog).lookup(10);
        doReturn(5L).when(replicatedLogEntry).term();

        long retIndex = snapshotManager.trimLog(10);
        assertEquals("return index", 10L, retIndex);

        verify(mockReplicatedLog).snapshotPreCommit(10, 5);
        verify(mockReplicatedLog).snapshotCommit(false);

        verify(mockRaftActorBehavior, never()).setReplicatedToAllIndex(anyLong());
    }

    @Test
    void testTrimLogWhenLastAppliedNotSet() {
        doReturn(-1L).when(mockReplicatedLog).getLastApplied();

        long retIndex = snapshotManager.trimLog(10);
        assertEquals("return index", -1L, retIndex);

        verify(mockReplicatedLog, never()).snapshotPreCommit(anyLong(), anyLong());
        verify(mockReplicatedLog, never()).snapshotCommit(false);

        verify(mockRaftActorBehavior, never()).setReplicatedToAllIndex(anyLong());
    }

    @Test
    void testTrimLogWhenLastAppliedZero() {
        doReturn(0L).when(mockReplicatedLog).getLastApplied();

        long retIndex = snapshotManager.trimLog(10);
        assertEquals("return index", -1L, retIndex);

        verify(mockReplicatedLog, never()).snapshotPreCommit(anyLong(), anyLong());
        verify(mockReplicatedLog, never()).snapshotCommit(false);

        verify(mockRaftActorBehavior, never()).setReplicatedToAllIndex(anyLong());
    }

    @Test
    void testTrimLogWhenTrimIndexNotPresent() {
        doReturn(20L).when(mockReplicatedLog).getLastApplied();

        doReturn(false).when(mockReplicatedLog).isPresent(10);

        long retIndex = snapshotManager.trimLog(10);
        assertEquals("return index", -1L, retIndex);

        verify(mockReplicatedLog, never()).snapshotPreCommit(anyLong(), anyLong());
        verify(mockReplicatedLog, never()).snapshotCommit(false);

        // Trim index is greater than replicatedToAllIndex so should update it.
        verify(mockRaftActorBehavior).setReplicatedToAllIndex(10L);
    }

    @Test
    void testTrimLogAfterCapture() {
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();
        boolean capture = snapshotManager.capture(EntryInfo.of(9, 1), 9);

        assertTrue(capture);

        assertTrue(snapshotManager.isCapturing());

        snapshotManager.trimLog(10);

        verify(mockReplicatedLog).snapshotPreCommit(0, 0);
        verify(mockReplicatedLog, never()).snapshotCommit(false);
    }

    @Test
    void testTrimLogAfterCaptureToInstall() {
        doReturn(ByteState.empty()).when(mockCohort).takeSnapshot();

        boolean capture = snapshotManager.captureToInstall(EntryInfo.of(9, 1), 9, "xyzzy");

        assertTrue(capture);

        assertTrue(snapshotManager.isCapturing());

        snapshotManager.trimLog(10);

        verify(mockReplicatedLog, never()).snapshotPreCommit(10, 5);
        verify(mockReplicatedLog, never()).snapshotCommit();
    }

    // FIXME: move to AbstractReplicatedLogTest
    @Test
    void testLastAppliedTermInformationReader() {
        doReturn(4L).when(mockReplicatedLog).getSnapshotTerm();
        doReturn(7L).when(mockReplicatedLog).getSnapshotIndex();

        final var lastLogEntry = EntryInfo.of(9, 6);

        // No followers and valid lastLogEntry
        var reader = AbstractReplicatedLog.computeLastAppliedEntry(mockReplicatedLog, 1L, lastLogEntry, false);
        assertEquals("getTerm", 6L, reader.term());
        assertEquals("getIndex", 9L, reader.index());

        // No followers and null lastLogEntry
        reader = AbstractReplicatedLog.computeLastAppliedEntry(mockReplicatedLog, 1L, null, false);
        assertEquals("getTerm", -1L, reader.term());
        assertEquals("getIndex", -1L, reader.index());

        // Followers and valid originalIndex entry
        doReturn(new SimpleReplicatedLogEntry(8L, 5L, new MockCommand("")))
            .when(mockReplicatedLog).lookup(8L);
        reader = AbstractReplicatedLog.computeLastAppliedEntry(mockReplicatedLog, 8L, lastLogEntry, true);
        assertEquals("getTerm", 5L, reader.term());
        assertEquals("getIndex", 8L, reader.index());

        // Followers and null originalIndex entry and valid snapshot index
        reader = AbstractReplicatedLog.computeLastAppliedEntry(mockReplicatedLog, 7L, lastLogEntry, true);
        assertEquals("getTerm", 4L, reader.term());
        assertEquals("getIndex", 7L, reader.index());

        // Followers and null originalIndex entry and invalid snapshot index
        doReturn(-1L).when(mockReplicatedLog).getSnapshotIndex();
        reader = AbstractReplicatedLog.computeLastAppliedEntry(mockReplicatedLog, 7L, lastLogEntry, true);
        assertEquals("getTerm", -1L, reader.term());
        assertEquals("getIndex", -1L, reader.index());
    }
}
