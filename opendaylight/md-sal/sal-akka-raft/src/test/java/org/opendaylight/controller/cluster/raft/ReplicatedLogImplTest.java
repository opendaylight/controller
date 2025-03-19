/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
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
import org.mockito.internal.matchers.Same;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;

/**
 * Unit tests for ReplicatedLogImpl.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class ReplicatedLogImplTest {
    @Mock
    private DataPersistenceProvider mockPersistence;
    @Mock
    private RaftActorBehavior mockBehavior;
    @Mock
    private Consumer<ReplicatedLogEntry> mockCallback;
    @Captor
    private ArgumentCaptor<Consumer<Object>> procedureCaptor;
    @TempDir
    private Path stateDir;

    private RaftActorContext context;
    private final DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();

    @BeforeEach
    public void setup() {
        context = new RaftActorContextImpl(null, null, new LocalAccess("test", stateDir), -1, -1, Map.of(),
            configParams, (short) 0, mockPersistence, applyState -> { }, MoreExecutors.directExecutor());
    }

    private void verifyPersist(final Object message) throws Exception {
        verifyPersist(message, new Same(message), true);
    }

    private void verifyPersist(final Object message, final ArgumentMatcher<?> matcher, final boolean async)
            throws Exception {
        if (async) {
            verify(mockPersistence).persistAsync(argThat(matcher), procedureCaptor.capture());
        } else {
            verify(mockPersistence).persist(argThat(matcher), procedureCaptor.capture());
        }
        procedureCaptor.getValue().accept(message);
    }

    @Test
    void testAppendAndPersistExpectingNoCapture() throws Exception {
        final var log = ReplicatedLogImpl.newInstance(context);

        final var logEntry1 = new SimpleReplicatedLogEntry(1, 1, new MockPayload("1"));

        log.appendAndPersist(logEntry1, null, true);

        verifyPersist(logEntry1);

        assertEquals(1, log.size());

        reset(mockPersistence);

        final var logEntry2 = new SimpleReplicatedLogEntry(2, 1, new MockPayload("2"));
        log.appendAndPersist(logEntry2, mockCallback, true);

        verifyPersist(logEntry2);

        verify(mockCallback).accept(same(logEntry2));

        assertEquals(2, log.size());
    }

    @Test
    void testAppendAndPersisWithDuplicateEntry() throws Exception {
        final var log = ReplicatedLogImpl.newInstance(context);

        final var logEntry = new SimpleReplicatedLogEntry(1, 1, new MockPayload("1"));

        log.appendAndPersist(logEntry, mockCallback, true);

        verifyPersist(logEntry);

        assertEquals(1, log.size());

        reset(mockPersistence, mockCallback);

        log.appendAndPersist(logEntry, mockCallback, true);

        verifyNoMoreInteractions(mockPersistence, mockCallback);

        assertEquals(1, log.size());
    }

    @Test
    void testAppendAndPersistExpectingCaptureDueToJournalCount() throws Exception {
        configParams.setSnapshotBatchCount(2);

        final var log = ReplicatedLogImpl.newInstance(context);

        final var logEntry1 = new SimpleReplicatedLogEntry(2, 1, new MockPayload("2"));
        final var logEntry2 = new SimpleReplicatedLogEntry(3, 1, new MockPayload("3"));

        log.appendAndPersist(logEntry1, null, true);
        verifyPersist(logEntry1);

        reset(mockPersistence);

        log.appendAndPersist(logEntry2, null, true);
        verifyPersist(logEntry2);

        assertEquals(2, log.size());
    }

    @Test
    void testAppendAndPersistExpectingCaptureDueToDataSize() throws Exception {
        context.setTotalMemoryRetriever(() -> 100);

        final var log = ReplicatedLogImpl.newInstance(context);

        int dataSize = 600;
        var logEntry = new SimpleReplicatedLogEntry(2, 1, new MockPayload("2", dataSize));

        log.appendAndPersist(logEntry, null, true);
        verifyPersist(logEntry);

        reset(mockPersistence);

        logEntry = new SimpleReplicatedLogEntry(3, 1, new MockPayload("3", 5));

        log.appendAndPersist(logEntry, null, true);
        verifyPersist(logEntry);

        assertEquals(2, log.size());
    }

    @Test
    void testRemoveFromAndPersist() throws Exception {

        final var log = ReplicatedLogImpl.newInstance(context);

        log.append(new SimpleReplicatedLogEntry(0, 1, new MockPayload("0")));
        log.append(new SimpleReplicatedLogEntry(1, 1, new MockPayload("1")));
        log.append(new SimpleReplicatedLogEntry(2, 1, new MockPayload("2")));

        log.removeFromAndPersist(1);

        final var deleteEntries = new DeleteEntries(1);
        verifyPersist(deleteEntries, match(deleteEntries), false);

        assertEquals(1, log.size());

        reset(mockPersistence);

        log.removeFromAndPersist(1);

        verifyNoMoreInteractions(mockPersistence);
    }

    @Test
    void testCommitFakeSnapshot() {
        final var log = ReplicatedLogImpl.newInstance(context);

        log.append(new SimpleReplicatedLogEntry(0, 1, new MockPayload("0")));
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
