/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.MockCommand;
import org.opendaylight.raft.spi.CompressionType;

@ExtendWith(MockitoExtension.class)
class JournalWriteTaskTest {
    private final ArrayList<Runnable> actorMessages = new ArrayList<>();

    @TempDir
    private Path directory;
    @Mock
    private RaftCallback<Long> longCallback;
    @Captor
    private ArgumentCaptor<Exception> exCaptor;

    private EntryJournalV1 journal;
    private JournalWriteTask task;

    @BeforeEach
    void beforeEach() throws Exception {
        journal = new EntryJournalV1("test", directory, CompressionType.NONE, false);
        task = new JournalWriteTask(new RaftStorageCompleter("test", actorMessages::add), journal, 32);
    }

    @AfterEach
    void afterEach() {
        journal.close();
    }

    @Test
    void processAndTerminateCausesSubsequentCancellation() throws Exception {
        task.processAndTerminate();
        task.appendEntry(new DefaultLogEntry(1, 1, new MockCommand("")), longCallback);
        task.run();

        assertEquals(1, runActor());
        verify(longCallback).invoke(exCaptor.capture(), isNull());
        final var ex = assertInstanceOf(CancellationException.class, exCaptor.getValue());
        assertEquals("Graceful termination", ex.getMessage());
    }

    @Test
    @Timeout(value = 1)
    void processAndTerminateExitsThread() throws Exception {
        final var thread = Thread.ofVirtual().start(task);
        task.processAndTerminate();
        thread.join();
        assertFalse(thread.isAlive());
    }

    @Test
    void cancelAndTerminateCausesImmediateCancellation() throws Exception {
        task.appendEntry(new DefaultLogEntry(1, 1, new MockCommand("first")), longCallback);
        task.cancelAndTerminate();
        task.appendEntry(new DefaultLogEntry(2, 1, new MockCommand("second")), longCallback);
        task.run();

        assertEquals(1, runActor());
        verify(longCallback, times(2)).invoke(exCaptor.capture(), isNull());
        exCaptor.getAllValues().forEach(ex -> {
            assertInstanceOf(CancellationException.class, ex);
            assertEquals("Abrupt termination", ex.getMessage());
        });
    }

    @Test
    @Timeout(value = 1)
    void cancelAndTerminateExitsThread() throws Exception {
        final var thread = Thread.ofVirtual().start(task);
        task.cancelAndTerminate();
        thread.join();
        assertFalse(thread.isAlive());
    }

    private int runActor() {
        final var ret = actorMessages.size();
        actorMessages.forEach(Runnable::run);
        actorMessages.clear();
        return ret;
    }
}
