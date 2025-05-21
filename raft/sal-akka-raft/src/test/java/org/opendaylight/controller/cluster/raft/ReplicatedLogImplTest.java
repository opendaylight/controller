/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.util.concurrent.MoreExecutors;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.spi.DefaultLogEntry;
import org.opendaylight.controller.cluster.raft.spi.EntryStore;
import org.opendaylight.controller.cluster.raft.spi.EntryStore.PersistCallback;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;

/**
 * Unit tests for ReplicatedLogImpl.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class ReplicatedLogImplTest {
    private final DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();

    @Mock
    private EntryStore entryStore;
    @Mock
    private PersistenceProvider persistence;
    @Mock
    private RaftActorBehavior behavior;
    @Mock
    private Consumer<ReplicatedLogEntry> callback;
    @Captor
    private ArgumentCaptor<PersistCallback> procedureCaptor;
    @TempDir
    private Path stateDir;

    private RaftActorContext context;

    @BeforeEach
    public void setup() {
        context = new RaftActorContextImpl(null, null, new LocalAccess("test", stateDir), Map.of(), configParams,
            (short) 0, persistence, (identifier, entry) -> { }, MoreExecutors.directExecutor());
    }

    @Test
    void testAppendAndPersistExpectingNoCapture() {
        mockEntryStore();
        final var log = new ReplicatedLogImpl(context);

        final var logEntry1 = new DefaultLogEntry(1, 1, new MockCommand("1"));

        log.appendSubmitted(logEntry1.index(), logEntry1.term(), logEntry1.command().toSerialForm(), callback);

        assertPersist(logEntry1);

        assertEquals(1, log.size());

        reset(entryStore);

        final var logEntry2 = new DefaultLogEntry(2, 1, new MockCommand("2"));
        log.appendSubmitted(logEntry2.index(), logEntry2.term(), logEntry2.command().toSerialForm(), callback);

        assertPersist(logEntry2);

        verify(callback).accept(eq(JournaledLogEntry.of(logEntry2)));

        assertEquals(2, log.size());
    }

    @Test
    void testAppendAndPersisWithDuplicateEntry() {
        mockEntryStore();

        final var log = new ReplicatedLogImpl(context);
        final var logEntry = new DefaultLogEntry(1, 1, new MockCommand("1"));

        log.appendSubmitted(logEntry.index(), logEntry.term(), logEntry.command().toSerialForm(), callback);

        assertPersist(logEntry);

        assertEquals(1, log.size());

        reset(persistence, callback);

        log.appendSubmitted(logEntry.index(), logEntry.term(), logEntry.command().toSerialForm(), callback);

        verifyNoMoreInteractions(persistence, callback);

        assertEquals(1, log.size());
    }

    @Test
    void testAppendAndPersistExpectingCaptureDueToJournalCount() {
        mockEntryStore();
        configParams.setSnapshotBatchCount(2);

        final var log = new ReplicatedLogImpl(context);
        final var logEntry1 = new DefaultLogEntry(2, 1, new MockCommand("2"));
        final var logEntry2 = new DefaultLogEntry(3, 1, new MockCommand("3"));

        log.appendSubmitted(logEntry1.index(), logEntry1.term(), logEntry1.command().toSerialForm(), callback);
        assertPersist(logEntry1);

        reset(entryStore);

        log.appendSubmitted(logEntry2.index(), logEntry2.term(), logEntry2.command().toSerialForm(), callback);
        assertPersist(logEntry2);

        assertEquals(2, log.size());
    }

    @Test
    void testAppendAndPersistExpectingCaptureDueToDataSize() {
        mockEntryStore();
        context.setTotalMemoryRetriever(() -> 100);

        final var log = new ReplicatedLogImpl(context);

        int dataSize = 600;
        var logEntry = new DefaultLogEntry(2, 1, new MockCommand("2", dataSize));

        log.appendSubmitted(logEntry.index(), logEntry.term(), logEntry.command().toSerialForm(), callback);
        assertPersist(logEntry);

        reset(entryStore);

        logEntry = new DefaultLogEntry(3, 1, new MockCommand("3", 5));

        log.appendSubmitted(logEntry.index(), logEntry.term(), logEntry.command().toSerialForm(), callback);
        assertPersist(logEntry);

        assertEquals(2, log.size());
    }

    @Test
    void testRemoveFromAndPersist() {
        mockEntryStore();
        final var log = new ReplicatedLogImpl(context);

        log.append(new DefaultLogEntry(0, 1, new MockCommand("0")));
        log.append(new DefaultLogEntry(1, 1, new MockCommand("1")));
        log.append(new DefaultLogEntry(2, 1, new MockCommand("2")));

        log.trimToReceive(1);
        assertEquals(1, log.size());
        verify(entryStore).discardTail(1);

        // already trimmed, hence no-op
        log.trimToReceive(1);
        assertEquals(1, log.size());
        verifyNoMoreInteractions(persistence);
    }

    @Test
    void testCommitFakeSnapshot() {
        final var log = new ReplicatedLogImpl(context);

        log.append(new DefaultLogEntry(0, 1, new MockCommand("0")));
        final int dataSizeAfterFirstPayload = log.dataSize();

        log.snapshotPreCommit(0, 1);
        log.snapshotCommit(false);

        assertEquals(0, log.size());
        assertEquals(dataSizeAfterFirstPayload, log.dataSize());
        verify(persistence, never()).entryStore();
    }

    private void mockEntryStore() {
        doReturn(entryStore).when(persistence).entryStore();
    }

    private void assertPersist(final LogEntry entry) {
        assertPersist(entry, true);
    }

    private void assertPersist(final LogEntry entry, final boolean async) {
        final var logEntry = JournaledLogEntry.of(entry);
        if (async) {
            verify(entryStore).startPersistEntry(eq(logEntry), procedureCaptor.capture());
            procedureCaptor.getValue().invoke(null, 1L);
        } else {
            verify(entryStore).persistEntry(eq(logEntry), any());
        }
    }
}
