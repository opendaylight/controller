/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.storage.journal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiFunction;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ByteBufJournal} Implementation.
 */
public final class SegmentedByteBufJournal implements ByteBufJournal {
    private static final Logger LOG = LoggerFactory.getLogger(SegmentedByteBufJournal.class);
    private static final int SEGMENT_BUFFER_FACTOR = 3;

    private final ConcurrentNavigableMap<Long, JournalSegment> segments = new ConcurrentSkipListMap<>();
    private final Collection<ByteBufReader> readers = ConcurrentHashMap.newKeySet();
    private final String name;
    private final StorageLevel storageLevel;
    private final File directory;
    private final int maxSegmentSize;
    private final int maxEntrySize;
    @Deprecated(forRemoval = true)
    private final int maxEntriesPerSegment;
    private final double indexDensity;
    private final boolean flushOnCommit;
    private final @NonNull ByteBufWriter writer;

    // null when closed
    private JournalSegment currentSegment;
    private volatile long commitIndex;

    SegmentedByteBufJournal(final String name, final StorageLevel storageLevel, final File directory,
            final int maxSegmentSize, final int maxEntrySize, final int maxEntriesPerSegment, final double indexDensity,
            final boolean flushOnCommit) {
        this.name = requireNonNull(name, "name cannot be null");
        this.storageLevel = requireNonNull(storageLevel, "storageLevel cannot be null");
        this.directory = requireNonNull(directory, "directory cannot be null");
        this.maxSegmentSize = maxSegmentSize;
        this.maxEntrySize = maxEntrySize;
        this.maxEntriesPerSegment = maxEntriesPerSegment;
        this.indexDensity = indexDensity;
        this.flushOnCommit = flushOnCommit;

        // Load existing log segments from disk.
        for (var segment : loadSegments()) {
            segments.put(segment.firstIndex(), segment);
        }
        currentSegment = ensureLastSegment();

        writer = new SegmentedByteBufWriter(this);
    }

    /**
     * Returns the total size of the journal.
     *
     * @return the total size of the journal
     */
    public long size() {
        return segments.values().stream()
            .mapToLong(segment -> {
                try {
                    return segment.file().size();
                } catch (IOException e) {
                    throw new StorageException(e);
                }
            })
            .sum();
    }

    @Override
    public long lastIndex() {
        return lastSegment().lastIndex();
    }

    @Override
    public ByteBufWriter writer() {
        return writer;
    }

    @Override
    public ByteBufReader openReader(final long index) {
        return openReader(index, SegmentedByteBufReader::new);
    }

    @NonNullByDefault
    private ByteBufReader openReader(final long index,
            final BiFunction<SegmentedByteBufJournal, JournalSegment, ByteBufReader> constructor) {
        final var reader = constructor.apply(this, segment(index));
        reader.reset(index);
        readers.add(reader);
        return reader;
    }

    @Override
    public ByteBufReader openCommitsReader(final long index) {
        return openReader(index, SegmentedCommitsByteBufReader::new);
    }

    /**
     * Asserts that the manager is open.
     *
     * @throws IllegalStateException if the segment manager is not open
     */
    private void assertOpen() {
        checkState(currentSegment != null, "journal not open");
    }

    /**
     * Asserts that enough disk space is available to allocate a new segment.
     */
    private void assertDiskSpace() {
        if (directory.getUsableSpace() < maxSegmentSize * SEGMENT_BUFFER_FACTOR) {
            throw new StorageException.OutOfDiskSpace("Not enough space to allocate a new journal segment");
        }
    }

    /**
     * Resets and returns the first segment in the journal.
     *
     * @param index the starting index of the journal
     * @return the first segment
     */
    JournalSegment resetSegments(final long index) {
        assertOpen();

        // If the index already equals the first segment index, skip the reset.
        final var firstSegment = firstSegment();
        if (index == firstSegment.firstIndex()) {
            return firstSegment;
        }

        segments.values().forEach(JournalSegment::delete);
        segments.clear();
        final var newSegment = createInitialSegment();
        currentSegment = newSegment;
        return newSegment;
    }

    /**
     * Returns the first segment in the log.
     *
     * @throws IllegalStateException if the segment manager is not open
     */
    JournalSegment firstSegment() {
        assertOpen();
        return segments.firstEntry().getValue();
    }

    /**
     * Returns the last segment in the log.
     *
     * @throws IllegalStateException if the segment manager is not open
     */
    JournalSegment lastSegment() {
        assertOpen();
        return segments.lastEntry().getValue();
    }

    /**
     * Returns the segment following the segment with the given ID.
     *
     * @param index The segment index with which to look up the next segment.
     * @return The next segment for the given index, or {@code null} if no such segment exists
     */
    @Nullable JournalSegment tryNextSegment(final long index) {
        final var higherEntry = segments.higherEntry(index);
        return higherEntry != null ? higherEntry.getValue() : null;
    }

    /**
     * Creates and returns the next segment.
     *
     * @return The next segment.
     * @throws IllegalStateException if the segment manager is not open
     */
    synchronized @NonNull JournalSegment createNextSegment() {
        assertOpen();
        assertDiskSpace();

        // FIXME: lastSegment should equal currentSegment. We should be asserting that.
        final var index = currentSegment.lastIndex() + 1;
        final var lastSegment = lastSegment();
        final var nextSegment = createSegment(lastSegment.file().segmentId() + 1, index);
        segments.put(index, nextSegment);
        currentSegment = nextSegment;
        return nextSegment;
    }

    /**
     * Returns the segment for the given index.
     *
     * @param index The index for which to return the segment.
     * @throws IllegalStateException if the segment manager is not open
     */
    synchronized JournalSegment segment(final long index) {
        assertOpen();
        // Check if the current segment contains the given index first in order to prevent an unnecessary map lookup.
        if (currentSegment != null && index > currentSegment.firstIndex()) {
            return currentSegment;
        }

        // If the index is in another segment, get the entry with the next lowest first index.
        final var segment = segments.floorEntry(index);
        return segment != null ? segment.getValue() : firstSegment();
    }

    /**
     * Removes a segment.
     *
     * @param segment The segment to remove.
     */
    synchronized void removeSegment(final JournalSegment segment) {
        segments.remove(segment.firstIndex());
        segment.delete();

        // Reset current segment to last segment
        currentSegment = ensureLastSegment();
    }

    /**
     * Creates a new segment.
     *
     * @param segmentId the segment ID
     * @param firstIndex index of first entry
     * @param A new segment
     */
    private @NonNull JournalSegment createSegment(final long segmentId, final long firstIndex) {
        final JournalSegmentFile file;
        try {
            file = JournalSegmentFile.createNew(name, directory, JournalSegmentDescriptor.builder()
                .withId(segmentId)
                .withIndex(firstIndex)
                .withMaxSegmentSize(maxSegmentSize)
                .withMaxEntries(maxEntriesPerSegment)
                .withUpdated(System.currentTimeMillis())
                .build());
        } catch (IOException e) {
            throw new StorageException(e);
        }

        final var segment = new JournalSegment(file, storageLevel, maxEntrySize, indexDensity);
        LOG.debug("Created segment: {}", segment);
        return segment;
    }

    private @NonNull JournalSegment createInitialSegment() {
        final var segment = createSegment(1, 1);
        segments.put(1L, segment);
        return segment;
    }

    /**
     * Make sure there is a last segment and return it.
     *
     * @return the last segment
     */
    private JournalSegment ensureLastSegment() {
        final var lastEntry = segments.lastEntry();
        // if there is no segment, create an initial segment starting at index 1.
        return lastEntry != null ? lastEntry.getValue() : createInitialSegment();
    }

    /**
     * Loads all segments from disk.
     *
     * @return A collection of segments for the log.
     */
    private Collection<JournalSegment> loadSegments() {
        // Ensure log directories are created.
        directory.mkdirs();

        final var segmentsMap = new TreeMap<Long, JournalSegment>();

        // Iterate through all files in the log directory.
        for (var file : directory.listFiles(File::isFile)) {

            // If the file looks like a segment file, attempt to load the segment.
            if (JournalSegmentFile.isSegmentFile(name, file)) {
                final JournalSegmentFile segmentFile;
                try {
                    segmentFile = JournalSegmentFile.openExisting(file.toPath());
                } catch (IOException e) {
                    throw new StorageException(e);
                }

                // Load the segment.
                LOG.debug("Loaded disk segment: {} ({})", segmentFile.segmentId(), segmentFile.path());

                // Add the segment to the segments list.
                final var segment = new JournalSegment(segmentFile, storageLevel, maxEntrySize, indexDensity);
                segmentsMap.put(segment.firstIndex(), segment);
            }
        }

        // Verify that all the segments in the log align with one another.
        JournalSegment previousSegment = null;
        boolean corrupted = false;
        for (var iterator =  segmentsMap.entrySet().iterator(); iterator.hasNext();  ) {
            final var segment = iterator.next().getValue();
            if (previousSegment != null && previousSegment.lastIndex() != segment.firstIndex() - 1) {
                LOG.warn("Journal is inconsistent. {} is not aligned with prior segment {}", segment.file().path(),
                    previousSegment.file().path());
                corrupted = true;
            }
            if (corrupted) {
                segment.delete();
                iterator.remove();
            }
            previousSegment = segment;
        }

        return segmentsMap.values();
    }

    /**
     * Resets journal readers to the given head.
     *
     * @param index The index at which to reset readers.
     */
    void resetHead(final long index) {
        for (var reader : readers) {
            if (reader.nextIndex() < index) {
                reader.reset(index);
            }
        }
    }

    /**
     * Resets journal readers to the given tail.
     *
     * @param index The index at which to reset readers.
     */
    void resetTail(final long index) {
        for (var reader : readers) {
            if (reader.nextIndex() >= index) {
                reader.reset(index);
            }
        }
    }

    void closeReader(final SegmentedByteBufReader reader) {
        readers.remove(reader);
    }

    /**
     * Returns a boolean indicating whether a segment can be removed from the journal prior to the given index.
     *
     * @param index the index from which to remove segments
     * @return indicates whether a segment can be removed from the journal
     */
    public boolean isCompactable(final long index) {
        final var firstIndex = getCompactableIndex(index);
        return firstIndex != 0 && !segments.headMap(firstIndex).isEmpty();
    }

    /**
     * Returns the index of the last segment in the log.
     *
     * @param index the compaction index
     * @return the starting index of the last segment in the log
     */
    public long getCompactableIndex(final long index) {
        final var segmentEntry = segments.floorEntry(index);
        return segmentEntry != null ? segmentEntry.getValue().firstIndex() : 0;
    }

    @Override
    public void compact(final long index) {
        final var firstIndex = getCompactableIndex(index);
        if (firstIndex != 0) {
            final var compactSegments = segments.headMap(firstIndex);
            if (!compactSegments.isEmpty()) {
                LOG.debug("{} - Compacting {} segment(s)", name, compactSegments.size());
                compactSegments.values().forEach(JournalSegment::delete);
                compactSegments.clear();
                resetHead(firstIndex);
            }
        }
    }

    @Override
    public void close() {
        if (currentSegment != null) {
            currentSegment = null;
            segments.values().forEach(JournalSegment::close);
            segments.clear();
        }
    }

    /**
     * Returns whether {@code flushOnCommit} is enabled for the log.
     *
     * @return Indicates whether {@code flushOnCommit} is enabled for the log.
     */
    boolean isFlushOnCommit() {
        return flushOnCommit;
    }

    /**
     * Updates commit index to the given value.
     *
     * @param index The index value.
     */
    void setCommitIndex(final long index) {
        commitIndex = index;
    }

    /**
     * Returns the journal last commit index.
     *
     * @return The journal last commit index.
     */
    long getCommitIndex() {
        return commitIndex;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Segmented byte journal builder.
     */
    public static final class Builder {
        private static final boolean DEFAULT_FLUSH_ON_COMMIT = false;
        private static final String DEFAULT_NAME = "atomix";
        private static final String DEFAULT_DIRECTORY = System.getProperty("user.dir");
        private static final int DEFAULT_MAX_SEGMENT_SIZE = 1024 * 1024 * 32;
        private static final int DEFAULT_MAX_ENTRY_SIZE = 1024 * 1024;
        private static final int DEFAULT_MAX_ENTRIES_PER_SEGMENT = 1024 * 1024;
        private static final double DEFAULT_INDEX_DENSITY = .005;

        private String name = DEFAULT_NAME;
        private StorageLevel storageLevel = StorageLevel.DISK;
        private File directory = new File(DEFAULT_DIRECTORY);
        private int maxSegmentSize = DEFAULT_MAX_SEGMENT_SIZE;
        private int maxEntrySize = DEFAULT_MAX_ENTRY_SIZE;
        private int maxEntriesPerSegment = DEFAULT_MAX_ENTRIES_PER_SEGMENT;
        private double indexDensity = DEFAULT_INDEX_DENSITY;
        private boolean flushOnCommit = DEFAULT_FLUSH_ON_COMMIT;

        private Builder() {
            // on purpose
        }

        /**
         * Sets the journal name.
         *
         * @param name The journal name.
         * @return The builder instance
         */
        public Builder withName(final String name) {
            this.name = requireNonNull(name, "name cannot be null");
            return this;
        }

        /**
         * Sets the storage level.
         *
         * @param storageLevel The storage level.
         * @return The builder instance
         */
        public Builder withStorageLevel(final StorageLevel storageLevel) {
            this.storageLevel = requireNonNull(storageLevel, "storageLevel cannot be null");
            return this;
        }

        /**
         * Sets the journal directory.
         *
         * @param directory The log directory.
         * @return The builder instance
         * @throws NullPointerException If the {@code directory} is {@code null}
         */
        public Builder withDirectory(final String directory) {
            return withDirectory(new File(requireNonNull(directory, "directory cannot be null")));
        }

        /**
         * Sets the journal directory.
         *
         * @param directory The log directory.
         * @return The builder instance
         * @throws NullPointerException If the {@code directory} is {@code null}
         */
        public Builder withDirectory(final File directory) {
            this.directory = requireNonNull(directory, "directory cannot be null");
            return this;
        }

        /**
         * Sets the maximum segment size in bytes.
         * By default, the maximum segment size is {@code 1024 * 1024 * 32}.
         *
         * @param maxSegmentSize The maximum segment size in bytes.
         * @return The builder instance
         * @throws IllegalArgumentException If the {@code maxSegmentSize} is not positive
         */
        public Builder withMaxSegmentSize(final int maxSegmentSize) {
            checkArgument(maxSegmentSize > JournalSegmentDescriptor.BYTES,
                "maxSegmentSize must be greater than " + JournalSegmentDescriptor.BYTES);
            this.maxSegmentSize = maxSegmentSize;
            return this;
        }

        /**
         * Sets the maximum entry size in bytes.
         *
         * @param maxEntrySize the maximum entry size in bytes
         * @return the builder instance
         * @throws IllegalArgumentException if the {@code maxEntrySize} is not positive
         */
        public Builder withMaxEntrySize(final int maxEntrySize) {
            checkArgument(maxEntrySize > 0, "maxEntrySize must be positive");
            this.maxEntrySize = maxEntrySize;
            return this;
        }

        /**
         * Sets the maximum number of allows entries per segment, returning the builder for method chaining.
         *
         * <p>
         * The maximum entry count dictates when logs should roll over to new segments. As entries are written to a
         * segment of the log, if the entry count in that segment meets the configured maximum entry count, the log will
         * create a new segment and append new entries to that segment.
         *
         * <p>
         * By default, the maximum entries per segment is {@code 1024 * 1024}.
         *
         * @param maxEntriesPerSegment The maximum number of entries allowed per segment.
         * @return The storage builder.
         * @throws IllegalArgumentException If the {@code maxEntriesPerSegment} not greater than the default max entries
         *     per segment
         * @deprecated This option has no effect and is scheduled for removal.
         */
        @Deprecated(forRemoval = true, since = "9.0.3")
        public Builder withMaxEntriesPerSegment(final int maxEntriesPerSegment) {
            checkArgument(maxEntriesPerSegment > 0, "max entries per segment must be positive");
            checkArgument(maxEntriesPerSegment <= DEFAULT_MAX_ENTRIES_PER_SEGMENT,
                "max entries per segment cannot be greater than " + DEFAULT_MAX_ENTRIES_PER_SEGMENT);
            this.maxEntriesPerSegment = maxEntriesPerSegment;
            return this;
        }

        /**
         * Sets the journal index density.
         *
         * <p>
         * The index density is the frequency at which the position of entries written to the journal will be
         * recorded in an in-memory index for faster seeking.
         *
         * @param indexDensity the index density
         * @return the builder instance
         * @throws IllegalArgumentException if the density is not between 0 and 1
         */
        public Builder withIndexDensity(final double indexDensity) {
            checkArgument(indexDensity > 0 && indexDensity < 1, "index density must be between 0 and 1");
            this.indexDensity = indexDensity;
            return this;
        }

        /**
         * Enables flushing buffers to disk when entries are committed to a segment.
         *
         * <p>
         * When flush-on-commit is enabled, log entry buffers will be automatically flushed to disk each time
         * an entry is committed in a given segment.
         *
         * @return The builder instance
         */
        public Builder withFlushOnCommit() {
            return withFlushOnCommit(true);
        }

        /**
         * Sets whether to flush buffers to disk when entries are committed to a segment.
         *
         * <p>
         * When flush-on-commit is enabled, log entry buffers will be automatically flushed to disk each time
         * an entry is committed in a given segment.
         *
         * @param flushOnCommit Whether to flush buffers to disk when entries are committed to a segment.
         * @return The builder instance
         */
        public Builder withFlushOnCommit(final boolean flushOnCommit) {
            this.flushOnCommit = flushOnCommit;
            return this;
        }

        /**
         * Build the {@link SegmentedByteBufJournal}.
         *
         * @return {@link SegmentedByteBufJournal} instance built.
         */
        public SegmentedByteBufJournal build() {
            return new SegmentedByteBufJournal(name, storageLevel, directory, maxSegmentSize, maxEntrySize,
                maxEntriesPerSegment, indexDensity, flushOnCommit);
        }
    }
}
