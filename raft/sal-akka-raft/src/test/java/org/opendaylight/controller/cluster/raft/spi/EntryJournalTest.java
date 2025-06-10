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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opendaylight.controller.cluster.raft.MockCommand;
import org.opendaylight.controller.cluster.raft.ReplicatedLog.Recovery;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.CompressionType;

class EntryJournalTest {
    private static final class TestRecovery implements Recovery {
        List<LogEntry> entries = new ArrayList<>();
        long recoveredIndex;
        long recoveredApplyIndex;
        EntryInfo recoveredSnapshot;
        boolean initialized;
        boolean finished;

        private long nextIndex;

        @Override
        public void initiallizedPosition(EntryInfo lastIncluded, long journalIndex, long applyIndex) {
            assertFalse(initialized);
            assertFalse(finished);
            assertNotNull(lastIncluded);
            recoveredIndex = journalIndex;
            recoveredSnapshot = lastIncluded;
            recoveredApplyIndex = applyIndex;
            nextIndex = lastIncluded.index() + 1;
            initialized = true;
        }

        @Override
        public void recoverEntry(long term, StateMachineCommand command) {
            assertTrue(initialized);
            assertFalse(finished);
            entries.add(new DefaultLogEntry(nextIndex++, term, command));
        }

        @Override
        public void finishRecovery() {
            assertTrue(initialized);
            assertFalse(finished);
            finished = true;
        }
    }

    @TempDir
    private Path directory;

    private EntryJournalV1 journal;

    @BeforeEach
    void beforeEach() throws Exception {
        journal = new EntryJournalV1("test", directory, CompressionType.NONE, false);
    }

    @AfterEach
    void afterEach() {
        journal.close();
    }

    @Test
    void recoverEmpty() throws Exception {
        final var recovery = new TestRecovery();
        journal.recoverTo(recovery, EntryInfo.of(2, 2));
        assertTrue(recovery.finished);
        assertEquals(1, recovery.recoveredIndex);
        assertEquals(0, recovery.recoveredApplyIndex);
        assertEquals(EntryInfo.of(2, 2), recovery.recoveredSnapshot);
        assertEquals(List.of(), recovery.entries);
    }

    @Test
    void testRecoverNonEmpty() throws Exception {
        final var entry1 = new DefaultLogEntry(3, 3, new MockCommand(""));
        final var entry2 = new DefaultLogEntry(4, 3, new MockCommand("CAFEBABE".repeat(256 * 1024)));

        assertEquals(1L, journal.persistEntry(entry1));
        assertEquals(2L, journal.persistEntry(entry2));

        journal.setApplyTo(2L);
        assertEquals(2L, journal.applyTo());
        journal.setReplayFrom(1);
        journal.close();

        journal = new EntryJournalV1("test", directory, CompressionType.NONE, false);
        final var recovery = new TestRecovery();
        journal.recoverTo(recovery, EntryInfo.of(2, 2));

        assertTrue(recovery.finished);
        assertEquals(1, recovery.recoveredIndex);
        assertEquals(2, recovery.recoveredApplyIndex);
        assertEquals(EntryInfo.of(2, 2), recovery.recoveredSnapshot);
        assertEquals(List.of(entry1, entry2), recovery.entries);
    }

    @Test
    void testMismatchIndex() throws Exception {
        final var entry1 = new DefaultLogEntry(1, 1, new MockCommand(""));
        final var entry2 = new DefaultLogEntry(2, 1, new MockCommand("CAFEBABE".repeat(256 * 1024)));

        assertEquals(1L, journal.persistEntry(entry1));
        assertEquals(2L, journal.persistEntry(entry2));
        journal.close();

        journal = new EntryJournalV1("test", directory, CompressionType.NONE, false);
        final var recovery = new TestRecovery();

        final var ex = assertThrows(IOException.class, () -> journal.recoverTo(recovery, EntryInfo.of(2, 2)));
        assertEquals("Mismatched entry index=1 expecting 3 at journalIndex=1", ex.getMessage());
    }
}
