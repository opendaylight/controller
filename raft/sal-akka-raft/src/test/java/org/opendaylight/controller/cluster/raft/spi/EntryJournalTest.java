/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opendaylight.controller.cluster.raft.MockCommand;
import org.opendaylight.raft.spi.CompressionType;

class EntryJournalTest {
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
    void emptyReplay() throws Exception {
        assertEquals(0, journal.applyToJournalIndex());
        try (var reader = journal.openReader()) {
            assertEquals(1, reader.nextJournalIndex());
            assertNull(reader.nextEntry());
        }
    }

    @Test
    void nonEmptyReplay() throws Exception {
        final var entry1 = new DefaultLogEntry(3, 3, new MockCommand(""));
        final var entry2 = new DefaultLogEntry(4, 3, new MockCommand("CAFEBABE".repeat(256 * 1024)));

        assertEquals(1, journal.nextToWrite());
        assertEquals(121, journal.appendEntry(entry1));
        assertEquals(2, journal.nextToWrite());
        assertEquals(2_097_279, journal.appendEntry(entry2));

        journal.setApplyToJournalIndex(2);
        assertEquals(2, journal.applyToJournalIndex());
        journal.discardHead(1);
        journal.close();

        assertTrue(Files.notExists(directory.resolve("entry-v1-0000000000000001")));
        assertEquals(2_097_279, Files.size(directory.resolve("entry-v1-0000000000000002")));

        journal = new EntryJournalV1("test", directory, CompressionType.NONE, false);
        assertEquals(2, journal.applyToJournalIndex());

        try (var reader = journal.openReader()) {
            assertEquals(1, reader.nextJournalIndex());
            final var je1 = reader.nextEntry();
            assertNotNull(je1);
            assertEquals(entry1, je1.toLogEntry());

            assertEquals(2, reader.nextJournalIndex());
            final var je2 = reader.nextEntry();
            assertNotNull(je2);
            assertEquals(entry2, je2.toLogEntry());

            assertEquals(3, reader.nextJournalIndex());
            assertNull(reader.nextEntry());
        }
    }

    @Test
    void compressedEntry() throws Exception {
        journal.close();
        journal = new EntryJournalV1("test", directory, CompressionType.LZ4, false);

        assertEquals(1, journal.nextToWrite());
        final var entry = new DefaultLogEntry(4, 3, new MockCommand("CAFEBABE".repeat(256 * 1024)));
        assertEquals(8548, journal.appendEntry(entry));

        assertTrue(Files.notExists(directory.resolve("entry-v1-0000000000000001")));

        try (var reader = journal.openReader()) {
            assertEquals(1, reader.nextJournalIndex());
            final var je = reader.nextEntry();
            assertNotNull(je);
            assertEquals(entry, je.toLogEntry());

            assertEquals(2, reader.nextJournalIndex());
            assertNull(reader.nextEntry());
        }
    }
}
