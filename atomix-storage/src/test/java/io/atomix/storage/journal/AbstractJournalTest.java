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

import static org.junit.Assert.assertArrayEquals;
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
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
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
    @Deprecated(forRemoval = true, since = "9.0.3")
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

    private SegmentedJournal<TestEntry> createJournal() {
        try {
            return new SegmentedJournal<>(SegmentedByteBufJournal.builder()
                .withName("test")
                .withDirectory(PATH.toFile())
                .withStorageLevel(storageLevel)
                .withMaxSegmentSize(maxSegmentSize)
                .withIndexDensity(.2)
                .build(), NAMESPACE.toReadMapper(), NAMESPACE.toWriteMapper());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
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
        try (var journal = createJournal()) {
            final var writer = journal.writer();
            final var reader = journal.openReader(1);

            // Append a couple entries.
            assertEquals(1, writer.getNextIndex());
            var indexed = writer.append(ENTRY);
            assertEquals(1, indexed.index());

            assertEquals(2, writer.getNextIndex());
            writer.append(ENTRY);
            reader.reset(2);
            indexed = assertNext(reader);
            assertEquals(2, indexed.index());
            assertNoNext(reader);

            // Test reading an entry
            reader.reset();
            var entry1 = assertNext(reader);
            assertEquals(1, entry1.index());

            // Test reading a second entry
            assertEquals(2, reader.getNextIndex());
            var entry2 = assertNext(reader);
            assertEquals(2, entry2.index());
            assertEquals(3, reader.getNextIndex());
            assertNoNext(reader);

            // Test opening a new reader and reading from the journal.
            final var reader2 = journal.openReader(1);
            entry1 = assertNext(reader2);
            assertEquals(1, entry1.index());

            assertEquals(2, reader2.getNextIndex());
            entry2 = assertNext(reader2);
            assertEquals(2, entry2.index());
            assertNoNext(reader2);

            // Reset the reader.
            reader2.reset();

            // Test opening a new reader and reading from the journal.
            final var reader3 = journal.openReader(1);
            entry1 = assertNext(reader3);
            assertEquals(1, entry1.index());

            assertEquals(2, reader3.getNextIndex());
            entry2 = assertNext(reader3);
            assertEquals(2, entry2.index());
            assertNoNext(reader3);

            // Truncate the journal and write a different entry.
            writer.reset(2);
            assertEquals(2, writer.getNextIndex());
            writer.append(ENTRY);
            reader3.reset(2);
            indexed = assertNext(reader3);
            assertEquals(2, indexed.index());

            // Reset the reader to a specific index and read the last entry again.
            reader3.reset(2);

            assertEquals(2, reader3.getNextIndex());
            entry2 = assertNext(reader3);
            assertEquals(2, entry2.index());
            assertNoNext(reader3);
        }
    }

    @Test
    public void testResetTruncateZero() throws Exception {
        try (var journal = createJournal()) {
            final var writer = journal.writer();
            final var reader = journal.openReader(1);

            assertEquals(0, journal.lastIndex());
            assertEquals(1, writer.getNextIndex());
            writer.append(ENTRY);
            writer.append(ENTRY);

            writer.reset(1);
            assertEquals(0, journal.lastIndex());
            assertEquals(1, writer.getNextIndex());
            // Repeat to assert this is a no-op
            writer.reset(1);
            assertEquals(0, journal.lastIndex());
            assertEquals(1, writer.getNextIndex());

            writer.append(ENTRY);

            var indexed = assertNext(reader);
            assertEquals(1, indexed.index());
            writer.reset(1);
            assertEquals(0, journal.lastIndex());
            assertEquals(1, writer.getNextIndex());
            indexed = writer.append(ENTRY);
            assertEquals(1, journal.lastIndex());
            assertEquals(2, writer.getNextIndex());
            assertEquals(1, indexed.index());

            indexed = assertNext(reader);
            assertEquals(1, indexed.index());

            writer.reset(1);
            assertEquals(0, journal.lastIndex());
            assertEquals(1, writer.getNextIndex());
            indexed = writer.append(ENTRY);
            assertEquals(1, journal.lastIndex());
            assertEquals(2, writer.getNextIndex());
            assertEquals(1, indexed.index());

            indexed = assertNext(reader);
            assertEquals(1, indexed.index());
        }
    }

    @Test
    public void testTruncateRead() throws Exception {
        final int cnt = 10;
        try (Journal<TestEntry> journal = createJournal()) {
            JournalWriter<TestEntry> writer = journal.writer();
            JournalReader<TestEntry> reader = journal.openReader(1);

            for (int i = 1; i <= cnt; i++) {
                assertEquals(i, writer.append(new TestEntry(32)).index());
            }

            for (int i = 1; i <= cnt - 2; i++) {
                assertEquals(i, assertNext(reader).index());
            }

            writer.reset(cnt - 1);

            assertNoNext(reader);
            assertEquals(cnt - 1, writer.append(new TestEntry(32)).index());
            assertEquals(cnt, writer.append(new TestEntry(32)).index());

            var entry = assertNext(reader);
            assertEquals(cnt - 1, entry.index());
            entry = assertNext(reader);
            assertNotNull(entry);
            assertEquals(cnt, entry.index());
        }
    }

    @Test
    public void testWriteReadEntries() throws Exception {
        try (Journal<TestEntry> journal = createJournal()) {
            JournalWriter<TestEntry> writer = journal.writer();
            JournalReader<TestEntry> reader = journal.openReader(1);

            for (int i = 1; i <= entriesPerSegment * 5; i++) {
                writer.append(ENTRY);
                var entry = assertNext(reader);
                assertEquals(i, entry.index());
                assertArrayEquals(ENTRY.bytes(), entry.entry().bytes());
                reader.reset(i);
                entry = assertNext(reader);
                assertEquals(i, entry.index());
                assertArrayEquals(ENTRY.bytes(), entry.entry().bytes());

                if (i > 6) {
                    reader.reset(i - 5);
                    assertEquals(i - 5, reader.getNextIndex());
                    assertNext(reader);
                    reader.reset(i + 1);
                }

                writer.reset(i);
                writer.append(ENTRY);

                assertNext(reader);
                reader.reset(i);
                entry = assertNext(reader);
                assertEquals(i, entry.index());
                assertArrayEquals(ENTRY.bytes(), entry.entry().bytes());
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
                assertNoNext(reader);
                writer.commit(i);
                var entry = assertNext(reader);
                assertEquals(i, entry.index());
                assertArrayEquals(ENTRY.bytes(), entry.entry().bytes());
                reader.reset(i);
                entry = assertNext(reader);
                assertEquals(i, entry.index());
                assertArrayEquals(ENTRY.bytes(), entry.entry().bytes());
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
            assertNext(uncommittedReader);
            assertEquals(2, uncommittedReader.getNextIndex());
            assertNoNext(committedReader);
            assertEquals(1, committedReader.getNextIndex());

            writer.commit(entriesPerSegment * 9);

            // ... so here we catch up ...
            assertNext(committedReader);
            assertEquals(2, committedReader.getNextIndex());

            // ... and continue from the second entry
            for (int i = 2; i <= entriesPerSegment * 2.5; i++) {
                var entry = assertNext(uncommittedReader);
                assertEquals(i, entry.index());

                entry = assertNext(committedReader);
                assertEquals(i, entry.index());
            }

            journal.compact(entriesPerSegment * 5 + 1);

            assertEquals(entriesPerSegment * 5 + 1, uncommittedReader.getNextIndex());
            var entry = assertNext(uncommittedReader);
            assertEquals(entriesPerSegment * 5 + 1, entry.index());

            assertEquals(entriesPerSegment * 5 + 1, committedReader.getNextIndex());
            entry = assertNext(committedReader);
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
            assertEquals(journal.firstIndex(), reader.getNextIndex());
            assertEquals(entriesPerSegment + 1, assertNext(reader).index());
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

    private static @NonNull Indexed<TestEntry> assertNext(final JournalReader<TestEntry> reader) {
        final var ret = tryNext(reader);
        assertNotNull(ret);
        return ret;
    }

    private static void assertNoNext(final JournalReader<TestEntry> reader) {
        assertNull(tryNext(reader));
    }

    private static @Nullable Indexed<TestEntry> tryNext(final JournalReader<TestEntry> reader) {
        try {
            return reader.tryNext(Indexed::new);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
