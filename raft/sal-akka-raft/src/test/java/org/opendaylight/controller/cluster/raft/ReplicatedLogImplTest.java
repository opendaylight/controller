/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;

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
    private Consumer<LogEntry> mockCallback;
    @Captor
    private ArgumentCaptor<Consumer<Object>> procedureCaptor;
    @TempDir
    private Path stateDir;

    private RaftActorContext context;
    private final DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();

    @BeforeEach
    public void setup() {
        context = new RaftActorContextImpl(null, null, new LocalAccess("test", stateDir), Map.of(), configParams,
            (short) 0, mockPersistence, (identifier, entry) -> { }, MoreExecutors.directExecutor());
    }

    private void verifyPersist(final long index, final long term, final MockCommand command) throws Exception {
        verifyPersist(new ArgumentMatcher<SimpleReplicatedLogEntry>() {
            @Override
            public boolean matches(final SimpleReplicatedLogEntry argument) {
                return index == argument.index() && term == argument.term() && command == argument.command();
            }

            @Override
            public Class<?> type() {
                return SimpleReplicatedLogEntry.class;
            }
        }, true);
    }

    private <T> void verifyPersist(final ArgumentMatcher<T> matcher, final boolean async) {
        final var messageCaptor = ArgumentCaptor.<T>captor();

        if (async) {
            verify(mockPersistence).persistAsync(messageCaptor.capture(), procedureCaptor.capture());
        } else {
            verify(mockPersistence).persist(messageCaptor.capture(), procedureCaptor.capture());
        }

        final var message = messageCaptor.getValue();
        assertTrue(matcher.matches(message));
        procedureCaptor.getValue().accept(message);
    }

    @Test
    void testAppendAndPersistExpectingNoCapture() throws Exception {
        final var log = new ReplicatedLogImpl(context);
        log.setSnapshotIndex(0);

        final var command1 = new MockCommand("1");

        assertEquals(1, log.appendSubmitted(1, command1, null));

        verifyPersist(1, 1, command1);

        assertEquals(1, log.size());

        reset(mockPersistence);


        final var command2 = new MockCommand("2");
        assertEquals(2, log.appendSubmitted(1, command2, mockCallback));

        verifyPersist(2, 1, command2);

        final var captor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockCallback).accept(captor.capture());
        assertSame(command2, captor.getValue().command());

        assertEquals(2, log.size());
    }

    @Test
    void testAppendAndPersistExpectingCaptureDueToJournalCount() throws Exception {
        configParams.setSnapshotBatchCount(2);

        final var log = new ReplicatedLogImpl(context);
        log.setSnapshotIndex(1);

        final var command1 = new MockCommand("2");

        assertEquals(2, log.appendSubmitted(1, command1, null));
        verifyPersist(2, 1, command1);

        reset(mockPersistence);

        final var command2 = new MockCommand("3");
        assertEquals(3, log.appendSubmitted(1, command2, null));
        verifyPersist(3, 1, command2);

        assertEquals(2, log.size());
    }

    @Test
    void testAppendAndPersistExpectingCaptureDueToDataSize() throws Exception {
        context.setTotalMemoryRetriever(() -> 100);

        final var log = new ReplicatedLogImpl(context);
        log.setSnapshotIndex(1);

        int dataSize = 600;
        var command = new MockCommand("2", dataSize);

        assertEquals(2, log.appendSubmitted(1, command, mockCallback));
        assertEquals(1, log.size());
        verifyPersist(2, 1, command);

        reset(mockPersistence);

        command = new MockCommand("3", 5);
        assertEquals(3, log.appendSubmitted(1, command, mockCallback));
        assertEquals(2, log.size());

        verifyPersist(3, 1, command);
    }

    @Test
    void testRemoveFromAndPersist() throws Exception {
        final var log = new ReplicatedLogImpl(context);

        log.append(new SimpleReplicatedLogEntry(0, 1, new MockCommand("0")));
        log.append(new SimpleReplicatedLogEntry(1, 1, new MockCommand("1")));
        log.append(new SimpleReplicatedLogEntry(2, 1, new MockCommand("2")));

        log.removeFromAndPersist(1);

        this.<DeleteEntries>verifyPersist(other -> other.getFromIndex() == 1, false);

        assertEquals(1, log.size());

        reset(mockPersistence);

        log.removeFromAndPersist(1);

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
}
