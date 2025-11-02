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
package org.opendaylight.raft.journal;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBufAllocator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiFunction;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RaftJournal} Implementation.
 */
public final class SegmentedRaftJournal implements RaftJournal {
    private static final Logger LOG = LoggerFactory.getLogger(SegmentedRaftJournal.class);
    private static final int SEGMENT_BUFFER_FACTOR = 3;

    private final ConcurrentSkipListMap<@NonNull Long, org.opendaylight.raft.journal.Segment> segments;
    private final Collection<EntryReader> readers = ConcurrentHashMap.newKeySet();
    private final @NonNull ByteBufAllocator allocator;
    private final @NonNull StorageLevel storageLevel;
    private final @NonNull Path directory;
    private final @NonNull String name;
    private final @NonNull SegmentedEntryWriter writer;
    private final int maxSegmentSize;
    private final int maxEntrySize;
    @Deprecated(forRemoval = true)
    private final int maxEntriesPerSegment;
    private final double indexDensity;

    // null when closed
    @SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
    private Segment currentSegment;
    private volatile long commitIndex;

    SegmentedRaftJournal(final String name, final StorageLevel storageLevel, final Path directory,
            final int maxSegmentSize, final int maxEntrySize, final int maxEntriesPerSegment, final double indexDensity,
            final ByteBufAllocator allocator) throws IOException {
        this.name = requireNonNull(name, "name cannot be null");
        this.storageLevel = requireNonNull(storageLevel, "storageLevel cannot be null");
        this.directory = requireNonNull(directory, "directory cannot be null");
        this.allocator = requireNonNull(allocator, "allocator cannot be null");
        this.maxSegmentSize = maxSegmentSize;
        this.maxEntrySize = maxEntrySize;
        this.maxEntriesPerSegment = maxEntriesPerSegment;
        this.indexDensity = indexDensity;

        // Load existing log segments from disk.
        segments = loadSegments();
        currentSegment = ensureLastSegment();
        writer = new SegmentedEntryWriter(this, currentSegment);
    }

    /**
     * Returns the total size of the journal.
     *
     * @return the total size of the journal
     * @throws IOException if an I/O error occurs
     */
    public long size() throws IOException {
        long size = 0;
        for (var segment : segments.values()) {
            size += segment.file().size();
        }
        return size;
    }

    @Override
    public long firstIndex() {
        return firstSegment().firstIndex();
    }

    @Override
    public long lastIndex() {
        return lastSegment().lastIndex();
    }

    @Override
    public EntryWriter writer() {
        return writer;
    }

    @Override
    public EntryReader openReader(final long index) {
        return openReader(index, SegmentedEntryReader::new);
    }

    @NonNullByDefault
    private EntryReader openReader(final long index,
            final BiFunction<SegmentedRaftJournal, Segment, EntryReader> constructor) {
        final var reader = constructor.apply(this, segment(index));
        reader.reset(index);
        readers.add(reader);
        return reader;
    }

    @Override
    public EntryReader openCommitsReader(final long index) {
        return openReader(index, SegmentedCommitsEntryReader::new);
    }

    /**
     * Asserts that the manager is open.
     *
     * @throws IllegalStateException if the segment manager is not open
     */
    private void assertOpen() {
        if (currentSegment == null) {
            throw new IllegalStateException("journal not open");
        }
    }

    /**
     * Asserts that enough disk space is available to allocate a new segment.
     */
    private void assertDiskSpace() throws StorageExhaustedException {
        // FIXME: Use FileStore.getUsableSpace() instead
        if (directory.toFile().getUsableSpace() < maxSegmentSize * SEGMENT_BUFFER_FACTOR) {
            throw new StorageExhaustedException("Not enough space to allocate a new journal segment");
        }
    }

    /**
     * Resets and returns the first segment in the journal.
     *
     * @param index the starting index of the journal
     * @return the first segment
     * @throws IOException when an I/O error occurs
     */
    @NonNull Segment resetSegments(final long index) throws IOException {
        assertOpen();

        // If the index already equals the first segment index, skip the reset.
        final var firstSegment = firstSegment();
        if (index == firstSegment.firstIndex()) {
            return firstSegment;
        }

        segments.values().forEach(Segment::delete);
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
    @NonNull Segment firstSegment() {
        assertOpen();
        return segments.firstEntry().getValue();
    }

    /**
     * Returns the last segment in the log.
     *
     * @throws IllegalStateException if the segment manager is not open
     */
    @NonNull Segment lastSegment() {
        assertOpen();
        return segments.lastEntry().getValue();
    }

    /**
     * Returns the segment following the segment with the given ID.
     *
     * @param index The segment index with which to look up the next segment.
     * @return The next segment for the given index, or {@code null} if no such segment exists
     */
    @Nullable Segment tryNextSegment(final long index) {
        final var higherEntry = segments.higherEntry(index);
        return higherEntry != null ? higherEntry.getValue() : null;
    }

    /**
     * Creates and returns the next segment.
     *
     * @return The next segment.
     * @throws IllegalStateException if the segment manager is not open
     * @throws IOException when an I/O error occurs
     */
    synchronized @NonNull Segment createNextSegment() throws IOException {
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
    synchronized Segment segment(final long index) {
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
     * @throws IOException when an I/O error occurs
     */
    synchronized void removeSegment(final Segment segment) throws IOException {
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
     * @return A new segment
     * @throws IOException when an I/O error occurs
     */
    private @NonNull Segment createSegment(final long segmentId, final long firstIndex) throws IOException {
        final var descriptor = SegmentDescriptor.builder()
            .withId(segmentId)
            .withIndex(firstIndex)
            .withMaxSegmentSize(maxSegmentSize)
            .withMaxEntries(maxEntriesPerSegment)
            .withUpdated(System.currentTimeMillis())
            .build();
        final var file = SegmentFile.createNew(name, directory, allocator, descriptor);
        final var segment = new Segment(file, storageLevel, maxEntrySize, indexDensity);
        LOG.debug("Created segment: {}", segment);
        return segment;
    }

    private @NonNull Segment createInitialSegment() throws IOException {
        final var segment = createSegment(1, 1);
        segments.put(1L, segment);
        return segment;
    }

    /**
     * Make sure there is a last segment and return it.
     *
     * @return the last segment
     * @throws IOException when an I/O error occurs
     */
    private @NonNull Segment ensureLastSegment() throws IOException {
        final var lastEntry = segments.lastEntry();
        // if there is no segment, create an initial segment starting at index 1.
        return lastEntry != null ? lastEntry.getValue() : createInitialSegment();
    }

    /**
     * Loads all segments from disk.
     *
     * @return A collection of segments for the log.
     * @throws IOException if an I/O error occurs
     */
    @NonNullByDefault
    private ConcurrentSkipListMap<Long, Segment> loadSegments() throws IOException {
        // Ensure log directories are created.
        Files.createDirectories(directory);

        final var segmentsMap = new TreeMap<Long, Segment>();

        // Iterate through all files in the log directory.
        for (var file : directory.toFile().listFiles(File::isFile)) {

            // If the file looks like a segment file, attempt to load the segment.
            final var filePath = file.toPath();
            if (SegmentFile.isSegmentFile(name, filePath)) {
                final SegmentFile segmentFile;
                try {
                    segmentFile = SegmentFile.openExisting(filePath, allocator);
                } catch (IOException e) {
                    segmentsMap.values().forEach(Segment::close);
                    throw e;
                }

                // Load the segment.
                LOG.debug("Loaded disk segment: {} ({})", segmentFile.segmentId(), segmentFile.path());

                // Add the segment to the segments list.
                final Segment segment;
                try {
                    segment = new Segment(segmentFile, storageLevel, maxEntrySize, indexDensity);
                } catch (IOException e) {
                    segmentFile.close();
                    segmentsMap.values().forEach(Segment::close);
                    throw e;
                }
                segmentsMap.put(segment.firstIndex(), segment);
            }
        }

        // Verify that all the segments in the log align with one another.
        Segment previousSegment = null;
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

        return new ConcurrentSkipListMap<>(segmentsMap);
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

    void closeReader(final SegmentedEntryReader reader) {
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
                compactSegments.values().forEach(Segment::delete);
                compactSegments.clear();
                resetHead(firstIndex);
            }
        }
    }

    @Override
    public void close() {
        if (currentSegment != null) {
            currentSegment = null;
            writer.close();
            segments.values().forEach(Segment::close);
            segments.clear();
        }
    }

    /**
     * Updates commit index to the given value. If the current commitIndex is greater than the proposed value, this
     * method does nothing.
     *
     * @param newCommitIndex The index value.
     */
    void setCommitIndex(final long newCommitIndex) {
        if (newCommitIndex > commitIndex) {
            commitIndex = newCommitIndex;
        }
    }

    /**
     * Returns the journal last commit index.
     *
     * @return The journal last commit index.
     */
    long getCommitIndex() {
        return commitIndex;
    }

    /**
     * Returns a new {@link Builder}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Segmented byte journal builder.
     */
    public static final class Builder {
        private static final String DEFAULT_NAME = "atomix";
        private static final String DEFAULT_DIRECTORY = System.getProperty("user.dir");
        private static final int DEFAULT_MAX_SEGMENT_SIZE = 1024 * 1024 * 32;
        private static final int DEFAULT_MAX_ENTRY_SIZE = 1024 * 1024;
        private static final int DEFAULT_MAX_ENTRIES_PER_SEGMENT = 1024 * 1024;
        private static final double DEFAULT_INDEX_DENSITY = .005;

        private String name = DEFAULT_NAME;
        private StorageLevel storageLevel = StorageLevel.DISK;
        private Path directory = Path.of(DEFAULT_DIRECTORY);
        private int maxSegmentSize = DEFAULT_MAX_SEGMENT_SIZE;
        private int maxEntrySize = DEFAULT_MAX_ENTRY_SIZE;
        private int maxEntriesPerSegment = DEFAULT_MAX_ENTRIES_PER_SEGMENT;
        private double indexDensity = DEFAULT_INDEX_DENSITY;
        private ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;

        private Builder() {
            // on purpose
        }

        /**
         * Sets the journal name.
         *
         * @param name The journal name.
         * @return The builder instance
         */
        @SuppressWarnings("checkstyle:hiddenField")
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
        @SuppressWarnings("checkstyle:hiddenField")
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
        @SuppressWarnings("checkstyle:hiddenField")
        public Builder withDirectory(final String directory) {
            return withDirectory(Path.of(requireNonNull(directory, "directory cannot be null")));
        }

        /**
         * Sets the journal directory.
         *
         * @param directory The log directory.
         * @return The builder instance
         * @throws NullPointerException If the {@code directory} is {@code null}
         */
        @SuppressWarnings("checkstyle:hiddenField")
        public Builder withDirectory(final Path directory) {
            this.directory = requireNonNull(directory, "directory cannot be null");
            return this;
        }

        /**
         * Sets the journal directory.
         *
         * @param directory The log directory.
         * @return The builder instance
         * @throws NullPointerException If the {@code directory} is {@code null}
         * @deprecated Use {@link #withDirectory(Path)} instead
         */
        @Deprecated
        @SuppressWarnings("checkstyle:hiddenField")
        public Builder withDirectory(final File directory) {
            return withDirectory(requireNonNull(directory, "directory cannot be null").toPath());
        }

        /**
         * Sets the maximum segment size in bytes.
         * By default, the maximum segment size is {@code 1024 * 1024 * 32}.
         *
         * @param maxSegmentSize The maximum segment size in bytes.
         * @return The builder instance
         * @throws IllegalArgumentException If the {@code maxSegmentSize} is not positive
         */
        @SuppressWarnings("checkstyle:hiddenField")
        public Builder withMaxSegmentSize(final int maxSegmentSize) {
            checkArgument(maxSegmentSize > SegmentDescriptor.BYTES,
                "maxSegmentSize must be greater than " + SegmentDescriptor.BYTES);
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
        @SuppressWarnings("checkstyle:hiddenField")
        public Builder withMaxEntrySize(final int maxEntrySize) {
            checkArgument(maxEntrySize > 0, "maxEntrySize must be positive");
            this.maxEntrySize = maxEntrySize;
            return this;
        }

        /**
         * Sets the maximum number of allows entries per segment, returning the builder for method chaining.
         *
         * <p>The maximum entry count dictates when logs should roll over to new segments. As entries are written to a
         * segment of the log, if the entry count in that segment meets the configured maximum entry count, the log will
         * create a new segment and append new entries to that segment.
         *
         * <p>By default, the maximum entries per segment is {@code 1024 * 1024}.
         *
         * @param maxEntriesPerSegment The maximum number of entries allowed per segment.
         * @return The storage builder.
         * @throws IllegalArgumentException If the {@code maxEntriesPerSegment} not greater than the default max entries
         *     per segment
         * @deprecated This option has no effect and is scheduled for removal.
         */
        @Deprecated(forRemoval = true, since = "9.0.3")
        @SuppressWarnings("checkstyle:hiddenField")
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
         * <p>The index density is the frequency at which the position of entries written to the journal will be
         * recorded in an in-memory index for faster seeking.
         *
         * @param indexDensity the index density
         * @return the builder instance
         * @throws IllegalArgumentException if the density is not between 0 and 1
         */
        @SuppressWarnings("checkstyle:hiddenField")
        public Builder withIndexDensity(final double indexDensity) {
            checkArgument(indexDensity > 0 && indexDensity < 1, "index density must be between 0 and 1");
            this.indexDensity = indexDensity;
            return this;
        }

        /**
         * Sets the {@link ByteBufAllocator} to use for allocating various buffers.
         *
         * @param byteBufAllocator the allocator to use
         * @return The builder instance
         */
        @SuppressWarnings("checkstyle:hiddenField")
        public Builder withByteBufAllocator(final ByteBufAllocator byteBufAllocator) {
            this.byteBufAllocator = requireNonNull(byteBufAllocator);
            return this;
        }

        /**
         * Build the {@link SegmentedRaftJournal}.
         *
         * @return {@link SegmentedRaftJournal} instance built.
         * @throws IOException when an I/O error occurs
         */
        public SegmentedRaftJournal build() throws IOException {
            return new SegmentedRaftJournal(name, storageLevel, directory, maxSegmentSize, maxEntrySize,
                maxEntriesPerSegment, indexDensity, byteBufAllocator);
        }
    }
}
