/*
 * Copyright 2017-2021 Open Networking Foundation
 * Copyright 2023 PANTHEON.tech, s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.storage.journal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Base journal test.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
@RunWith(Parameterized.class)
public abstract class AbstractJournalTest {
    private static final JournalSerdes NAMESPACE = JournalSerdes.builder()
        .register(new TestEntrySerdes(), TestEntry.class)
        .register(new ByteArraySerdes(), byte[].class)
        .build();

    protected static final TestEntry ENTRY = new TestEntry(32);
    private static final Path PATH = Paths.get("target/test-logs/");

    private final StorageLevel storageLevel;
    private final int maxSegmentSize;
    protected final int entriesPerSegment;

    protected AbstractJournalTest(final StorageLevel storageLevel, final int maxSegmentSize) {
        this.storageLevel = storageLevel;
        this.maxSegmentSize = maxSegmentSize;
        int entryLength = NAMESPACE.serialize(ENTRY).length + 8;
        entriesPerSegment = (maxSegmentSize - 64) / entryLength;
    }

    @Parameterized.Parameters
    public static List<Object[]> primeNumbers() {
        var runs = new ArrayList<Object[]>();
        for (int i = 1; i <= 10; i++) {
            for (int j = 1; j <= 10; j++) {
                runs.add(new Object[] { 64 + i * (NAMESPACE.serialize(ENTRY).length + 8) + j });
            }
        }
        return runs;
    }

    protected SegmentedJournal<TestEntry> createJournal() {
        return SegmentedJournal.<TestEntry>builder()
            .withName("test")
            .withDirectory(PATH.toFile())
            .withNamespace(NAMESPACE)
            .withStorageLevel(storageLevel)
            .withMaxSegmentSize(maxSegmentSize)
            .withIndexDensity(.2)
            .build();
    }

    @Test
    public void testCloseMultipleTimes() {
        // given
        final Journal<TestEntry> journal = createJournal();

        // when
        journal.close();

        // then
        journal.close();
    }

    @Test
    public void testWriteRead() throws Exception {
        try (Journal<TestEntry> journal = createJournal()) {
            JournalWriter<TestEntry> writer = journal.writer();
            JournalReader<TestEntry> reader = journal.openReader(1);

            // Append a couple entries.
            assertEquals(1, writer.getNextIndex());
            var indexed = writer.append(ENTRY);
            assertEquals(1, indexed.index());

            assertEquals(2, writer.getNextIndex());
            writer.append(ENTRY);
            reader.reset(2);
            indexed = reader.tryNext();
            assertNotNull(indexed);
            assertEquals(2, indexed.index());
            assertNull(reader.tryNext());

            // Test reading an entry
            reader.reset();
            var entry1 = reader.tryNext();
            assertNotNull(entry1);
            assertEquals(1, entry1.index());
            assertEquals(entry1, reader.getCurrentEntry());

            // Test reading a second entry
            assertEquals(2, reader.getNextIndex());
            var entry2 = reader.tryNext();
            assertNotNull(entry2);
            assertEquals(2, entry2.index());
            assertEquals(entry2, reader.getCurrentEntry());
            assertEquals(3, reader.getNextIndex());
            assertNull(reader.tryNext());

            // Test opening a new reader and reading from the journal.
            reader = journal.openReader(1);
            entry1 = reader.tryNext();
            assertNotNull(entry1);
            assertEquals(1, entry1.index());
            assertEquals(entry1, reader.getCurrentEntry());

            assertEquals(2, reader.getNextIndex());
            entry2 = reader.tryNext();
            assertNotNull(entry2);
            assertEquals(2, entry2.index());
            assertEquals(entry2, reader.getCurrentEntry());
            assertNull(reader.tryNext());

            // Reset the reader.
            reader.reset();

            // Test opening a new reader and reading from the journal.
            reader = journal.openReader(1);
            entry1 = reader.tryNext();
            assertNotNull(entry1);
            assertEquals(1, entry1.index());
            assertEquals(entry1, reader.getCurrentEntry());

            assertEquals(2, reader.getNextIndex());
            entry2 = reader.tryNext();
            assertNotNull(entry2);
            assertEquals(2, entry2.index());
            assertEquals(entry2, reader.getCurrentEntry());
            assertNull(reader.tryNext());

            // Truncate the journal and write a different entry.
            writer.truncate(1);
            assertEquals(2, writer.getNextIndex());
            writer.append(ENTRY);
            reader.reset(2);
            indexed = reader.tryNext();
            assertNotNull(indexed);
            assertEquals(2, indexed.index());

            // Reset the reader to a specific index and read the last entry again.
            reader.reset(2);

            final var current = reader.getCurrentEntry();
            assertNotNull(current);
            assertEquals(1, current.index());
            assertEquals(2, reader.getNextIndex());
            entry2 = reader.tryNext();
            assertNotNull(entry2);
            assertEquals(2, entry2.index());
            assertEquals(entry2, reader.getCurrentEntry());
            assertNull(reader.tryNext());
        }
    }

    @Test
    public void testResetTruncateZero() throws Exception {
        try (SegmentedJournal<TestEntry> journal = createJournal()) {
            JournalWriter<TestEntry> writer = journal.writer();
            JournalReader<TestEntry> reader = journal.openReader(1);

            assertEquals(0, writer.getLastIndex());
            writer.append(ENTRY);
            writer.append(ENTRY);
            writer.reset(1);
            assertEquals(0, writer.getLastIndex());
            writer.append(ENTRY);

            var indexed = reader.tryNext();
            assertNotNull(indexed);
            assertEquals(1, indexed.index());
            writer.reset(1);
            assertEquals(0, writer.getLastIndex());
            writer.append(ENTRY);
            assertEquals(1, writer.getLastIndex());
            assertEquals(1, writer.getLastEntry().index());

            indexed = reader.tryNext();
            assertNotNull(indexed);
            assertEquals(1, indexed.index());

            writer.truncate(0);
            assertEquals(0, writer.getLastIndex());
            assertNull(writer.getLastEntry());
            writer.append(ENTRY);
            assertEquals(1, writer.getLastIndex());
            assertEquals(1, writer.getLastEntry().index());

            indexed = reader.tryNext();
            assertNotNull(indexed);
            assertEquals(1, indexed.index());
        }
    }

    @Test
    public void testTruncateRead() throws Exception {
        int i = 10;
        try (Journal<TestEntry> journal = createJournal()) {
            JournalWriter<TestEntry> writer = journal.writer();
            JournalReader<TestEntry> reader = journal.openReader(1);

            for (int j = 1; j <= i; j++) {
                assertEquals(j, writer.append(new TestEntry(32)).index());
            }

            for (int j = 1; j <= i - 2; j++) {
                final var indexed = reader.tryNext();
                assertNotNull(indexed);
                assertEquals(j, indexed.index());
            }

            writer.truncate(i - 2);

            assertNull(reader.tryNext());
            assertEquals(i - 1, writer.append(new TestEntry(32)).index());
            assertEquals(i, writer.append(new TestEntry(32)).index());

            Indexed<TestEntry> entry = reader.tryNext();
            assertNotNull(entry);
            assertEquals(i - 1, entry.index());
            entry = reader.tryNext();
            assertNotNull(entry);
            assertEquals(i, entry.index());
        }
    }

    @Test
    public void testWriteReadEntries() throws Exception {
        try (Journal<TestEntry> journal = createJournal()) {
            JournalWriter<TestEntry> writer = journal.writer();
            JournalReader<TestEntry> reader = journal.openReader(1);

            for (int i = 1; i <= entriesPerSegment * 5; i++) {
                writer.append(ENTRY);
                var entry = reader.tryNext();
                assertNotNull(entry);
                assertEquals(i, entry.index());
                assertEquals(32, entry.entry().bytes().length);
                reader.reset(i);
                entry = reader.tryNext();
                assertNotNull(entry);
                assertEquals(i, entry.index());
                assertEquals(32, entry.entry().bytes().length);

                if (i > 6) {
                    reader.reset(i - 5);
                    final var current = reader.getCurrentEntry();
                    assertNotNull(current);
                    assertEquals(i - 6, current.index());
                    assertEquals(i - 5, reader.getNextIndex());
                    reader.reset(i + 1);
                }

                writer.truncate(i - 1);
                writer.append(ENTRY);

                assertNotNull(reader.tryNext());
                reader.reset(i);
                entry = reader.tryNext();
                assertNotNull(entry);
                assertEquals(i, entry.index());
                assertEquals(32, entry.entry().bytes().length);
            }
        }
    }

    @Test
    public void testWriteReadCommittedEntries() throws Exception {
        try (Journal<TestEntry> journal = createJournal()) {
            JournalWriter<TestEntry> writer = journal.writer();
            JournalReader<TestEntry> reader = journal.openReader(1, JournalReader.Mode.COMMITS);

            for (int i = 1; i <= entriesPerSegment * 5; i++) {
                writer.append(ENTRY);
                assertNull(reader.tryNext());
                writer.commit(i);
                var entry = reader.tryNext();
                assertNotNull(entry);
                assertEquals(i, entry.index());
                assertEquals(32, entry.entry().bytes().length);
                reader.reset(i);
                entry = reader.tryNext();
                assertNotNull(entry);
                assertEquals(i, entry.index());
                assertEquals(32, entry.entry().bytes().length);
            }
        }
    }

    @Test
    public void testReadAfterCompact() throws Exception {
        try (SegmentedJournal<TestEntry> journal = createJournal()) {
            JournalWriter<TestEntry> writer = journal.writer();
            JournalReader<TestEntry> uncommittedReader = journal.openReader(1, JournalReader.Mode.ALL);
            JournalReader<TestEntry> committedReader = journal.openReader(1, JournalReader.Mode.COMMITS);

            for (int i = 1; i <= entriesPerSegment * 10; i++) {
                assertEquals(i, writer.append(ENTRY).index());
            }

            assertEquals(1, uncommittedReader.getNextIndex());
            assertEquals(1, committedReader.getNextIndex());

            // This creates asymmetry, as uncommitted reader will move one step ahead...
            assertNotNull(uncommittedReader.tryNext());
            assertEquals(2, uncommittedReader.getNextIndex());
            assertNull(committedReader.tryNext());
            assertEquals(1, committedReader.getNextIndex());

            writer.commit(entriesPerSegment * 9);

            // ... so here we catch up ...
            assertNotNull(committedReader.tryNext());
            assertEquals(2, committedReader.getNextIndex());

            // ... and continue from the second entry
            for (int i = 2; i <= entriesPerSegment * 2.5; i++) {
                var entry = uncommittedReader.tryNext();
                assertNotNull(entry);
                assertEquals(i, entry.index());

                entry = committedReader.tryNext();
                assertNotNull(entry);
                assertEquals(i, entry.index());
            }

            journal.compact(entriesPerSegment * 5 + 1);

            assertNull(uncommittedReader.getCurrentEntry());
            assertEquals(entriesPerSegment * 5 + 1, uncommittedReader.getNextIndex());
            var entry = uncommittedReader.tryNext();
            assertNotNull(entry);
            assertEquals(entriesPerSegment * 5 + 1, entry.index());

            assertNull(committedReader.getCurrentEntry());
            assertEquals(entriesPerSegment * 5 + 1, committedReader.getNextIndex());
            entry = committedReader.tryNext();
            assertNotNull(entry);
            assertEquals(entriesPerSegment * 5 + 1, entry.index());
        }
    }

    /**
     * Tests reading from a compacted journal.
     */
    @Test
    public void testCompactAndRecover() throws Exception {
        try (var journal = createJournal()) {
            // Write three segments to the journal.
            final var writer = journal.writer();
            for (int i = 0; i < entriesPerSegment * 3; i++) {
                writer.append(ENTRY);
            }

            // Commit the entries and compact the first segment.
            writer.commit(entriesPerSegment * 3);
            journal.compact(entriesPerSegment + 1);
        }

        // Reopen the journal and create a reader.
        try (var journal = createJournal()) {
            final var writer = journal.writer();
            final var reader = journal.openReader(1, JournalReader.Mode.COMMITS);
            writer.append(ENTRY);
            writer.append(ENTRY);
            writer.commit(entriesPerSegment * 3);

            // Ensure the reader starts at the first physical index in the journal.
            assertEquals(entriesPerSegment + 1, reader.getNextIndex());
            assertEquals(reader.getFirstIndex(), reader.getNextIndex());
            final var indexed = reader.tryNext();
            assertNotNull(indexed);
            assertEquals(entriesPerSegment + 1, indexed.index());
            assertEquals(entriesPerSegment + 2, reader.getNextIndex());
        }
    }

    @Before
    @After
    public void cleanupStorage() throws IOException {
        if (Files.exists(PATH)) {
            Files.walkFileTree(PATH, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
