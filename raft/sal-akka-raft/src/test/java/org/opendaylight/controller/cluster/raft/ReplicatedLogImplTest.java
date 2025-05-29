/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;

/**
 * Unit tests for ReplicatedLogImpl.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class ReplicatedLogImplTest {
    @Mock
    private PersistenceProvider mockPersistence;
    @Mock
    private RaftActorBehavior mockBehavior;
    @Mock
    private Consumer<ReplicatedLogEntry> mockCallback;
    @Captor
    private ArgumentCaptor<Consumer<ReplicatedLogEntry>> procedureCaptor;
    @TempDir
    private Path stateDir;

    private RaftActorContext context;
    private final DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();

    @BeforeEach
    public void setup() {
        context = new RaftActorContextImpl(null, null, new LocalAccess("test", stateDir), Map.of(), configParams,
            (short) 0, mockPersistence, (identifier, entry) -> { }, MoreExecutors.directExecutor());
    }

    private void verifyPersist(final ReplicatedLogEntry entry) {
        verifyPersist(entry, true);
    }

    private void verifyPersist(final ReplicatedLogEntry entry, final boolean async) {
        if (async) {
            verify(mockPersistence).startPersistEntry(eq(entry), procedureCaptor.capture());
        } else {
            verify(mockPersistence).persistEntry(eq(entry), procedureCaptor.capture());
        }
        procedureCaptor.getValue().accept(entry);
    }

    @Test
    void testAppendAndPersistExpectingNoCapture() {
        final var log = new ReplicatedLogImpl(context);

        final var logEntry1 = new SimpleReplicatedLogEntry(1, 1, new MockCommand("1"));

        log.appendSubmitted(logEntry1.index(), logEntry1.term(), logEntry1.command(), null);

        verifyPersist(logEntry1);

        assertEquals(1, log.size());

        reset(mockPersistence);

        final var logEntry2 = new SimpleReplicatedLogEntry(2, 1, new MockCommand("2"));
        log.appendSubmitted(logEntry2.index(), logEntry2.term(), logEntry2.command(), mockCallback);

        verifyPersist(logEntry2);

        verify(mockCallback).accept(eq(logEntry2));

        assertEquals(2, log.size());
    }

    @Test
    void testAppendAndPersisWithDuplicateEntry() {
        final var log = new ReplicatedLogImpl(context);

        final var logEntry = new SimpleReplicatedLogEntry(1, 1, new MockCommand("1"));

        log.appendSubmitted(logEntry.index(), logEntry.term(), logEntry.command(), mockCallback);

        verifyPersist(logEntry);

        assertEquals(1, log.size());

        reset(mockPersistence, mockCallback);

        log.appendSubmitted(logEntry.index(), logEntry.term(), logEntry.command(), mockCallback);

        verifyNoMoreInteractions(mockPersistence, mockCallback);

        assertEquals(1, log.size());
    }

    @Test
    void testAppendAndPersistExpectingCaptureDueToJournalCount() {
        configParams.setSnapshotBatchCount(2);

        final var log = new ReplicatedLogImpl(context);

        final var logEntry1 = new SimpleReplicatedLogEntry(2, 1, new MockCommand("2"));
        final var logEntry2 = new SimpleReplicatedLogEntry(3, 1, new MockCommand("3"));

        log.appendSubmitted(logEntry1.index(), logEntry1.term(), logEntry1.command(), null);
        verifyPersist(logEntry1);

        reset(mockPersistence);

        log.appendSubmitted(logEntry2.index(), logEntry2.term(), logEntry2.command(), null);
        verifyPersist(logEntry2);

        assertEquals(2, log.size());
    }

    @Test
    void testAppendAndPersistExpectingCaptureDueToDataSize() {
        context.setTotalMemoryRetriever(() -> 100);

        final var log = new ReplicatedLogImpl(context);

        int dataSize = 600;
        var logEntry = new SimpleReplicatedLogEntry(2, 1, new MockCommand("2", dataSize));

        log.appendSubmitted(logEntry.index(), logEntry.term(), logEntry.command(), null);
        verifyPersist(logEntry);

        reset(mockPersistence);

        logEntry = new SimpleReplicatedLogEntry(3, 1, new MockCommand("3", 5));

        log.appendSubmitted(logEntry.index(), logEntry.term(), logEntry.command(), null);
        verifyPersist(logEntry);

        assertEquals(2, log.size());
    }

    @Test
    void testRemoveFromAndPersist() {
        final var log = new ReplicatedLogImpl(context);

        log.append(new SimpleReplicatedLogEntry(0, 1, new MockCommand("0")));
        log.append(new SimpleReplicatedLogEntry(1, 1, new MockCommand("1")));
        log.append(new SimpleReplicatedLogEntry(2, 1, new MockCommand("2")));

        log.trimToReceive(1);
        assertEquals(1, log.size());
        verify(mockPersistence).deleteEntries(1);

        // already trimmed, hence no-op
        log.trimToReceive(1);
        assertEquals(1, log.size());
        verifyNoMoreInteractions(mockPersistence);
    }

    @Test
    void testCommitFakeSnapshot() {
        final var log = new ReplicatedLogImpl(context);

        log.append(new SimpleReplicatedLogEntry(0, 1, new MockCommand("0")));
        final int dataSizeAfterFirstPayload = log.dataSize();

        log.snapshotPreCommit(0,1);
        log.snapshotCommit(false);

        assertEquals(0, log.size());
        assertEquals(dataSizeAfterFirstPayload, log.dataSize());
    }

    private static ArgumentMatcher<DeleteEntries> match(final DeleteEntries actual) {
        return other -> actual.getFromIndex() == other.getFromIndex();
    }
}
