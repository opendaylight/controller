/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
 * Copyright (c) 2024 PANTHEON.tech, s.r.o.
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

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.raft.journal.StorageLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single segment in {@link SegmentedRaftJournal}.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class Segment {
    /**
     * Encapsulation of a {@link Segment}'s state.
     */
    private sealed interface State {
        // Marker interface
    }

    /**
     * Journal segment is active, i.e. there is a associated with it.
     */
    @NonNullByDefault
    private record Active(FileAccess access, FileWriter fileWriter, SegmentWriter writer) implements State {
        Active {
            requireNonNull(access);
            requireNonNull(fileWriter);
            requireNonNull(writer);
        }

        Inactive deactivate() {
            final var inactive = new Inactive(writer.currentPosition());
            fileWriter.release();
            access.close();
            return inactive;
        }
    }

    /**
     * Journal segment is inactive, i.e. there is no writer associated with it.
     */
    @NonNullByDefault
    private record Inactive(int currentPosition) implements State {
        Active activate(final Segment segment) throws IOException {
            final var access = segment.file.newAccess(segment.storageLevel, segment.maxEntrySize);
            final var fileWriter = access.newFileWriter();
            return new Active(access, fileWriter,
                new SegmentWriter(fileWriter, segment, segment.segmentIndex, currentPosition));
        }
    }

    /**
     * Position of an entry in a segment.
     *
     * @param index the entry index
     * @param position the position of the entry header within the segment
     */
    @NonNullByDefault
    record Position(long index, int position) {
        Position(final Entry<Long, Integer> entry) {
            this(entry.getKey(), entry.getValue());
        }

        static @Nullable Position ofNullable(final @Nullable Entry<Long, Integer> entry) {
            return entry == null ? null : new Position(entry);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(Segment.class);

    private final Set<SegmentReader> readers = ConcurrentHashMap.newKeySet();
    private final AtomicInteger references = new AtomicInteger();
    private final @NonNull SegmentFile file;
    private final @NonNull StorageLevel storageLevel;
    private final @NonNull SegmentIndex segmentIndex;
    private final int maxEntrySize;

    private State state;
    private boolean open = true;

    Segment(final SegmentFile file, final StorageLevel storageLevel, final int maxEntrySize, final double indexDensity)
            throws IOException {
        this.file = requireNonNull(file);
        this.storageLevel = requireNonNull(storageLevel);
        this.maxEntrySize = maxEntrySize;

        segmentIndex = new SparseSegmentIndex(indexDensity);

        try (var tmpAccess = file.newAccess(storageLevel, maxEntrySize)) {
            final var fileReader = tmpAccess.newFileReader();
            try {
                state = new Inactive(indexEntries(fileReader, this, maxEntrySize, segmentIndex, Long.MAX_VALUE, null));
            } finally {
                fileReader.release();
            }
        }
    }

    /**
     * Returns the segment's starting index.
     *
     * @return The segment's starting index.
     */
    long firstIndex() {
        return file.firstIndex();
    }

    /**
     * Returns the last index in the segment.
     *
     * @return The last index in the segment.
     */
    long lastIndex() {
        final var lastPosition = segmentIndex.last();
        return lastPosition != null ? lastPosition.index() : firstIndex() - 1;
    }

    /**
     * Returns the segment file.
     *
     * @return The segment file.
     */
    SegmentFile file() {
        return file;
    }

    /**
     * Looks up the position of the given index.
     *
     * @param index the index to lookup
     * @return the position of the given index or a lesser index, or {@code null}
     */
    @Nullable Position lookup(final long index) {
        return segmentIndex.lookup(index);
    }

    /**
     * Acquires a reference to the log segment.
     */
    private Active acquire() {
        return references.getAndIncrement() == 0 ? activate() : (Active) state;
    }

    private Active activate() {
        final Active ret;
        try {
            ret = ((Inactive) state).activate(this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        state = ret;
        return ret;
    }

    /**
     * Releases a reference to the log segment.
     */
    private void release() {
        if (references.decrementAndGet() == 0) {
            state = ((Active) state).deactivate();
            if (!open) {
                finishClose();
            }
        }
    }

    /**
     * Acquires a reference to the segment writer.
     *
     * @return The segment writer
     * @throws IllegalStateException if this writer is closed
     */
    SegmentWriter acquireWriter() {
        checkOpen();
        return acquire().writer();
    }

    /**
     * Releases the reference to the segment writer.
     */
    void releaseWriter() {
        release();
    }

    /**
     * Creates a new segment reader.
     *
     * @return A new segment reader
     * @throws IllegalStateException if this writer is closed
     */
    SegmentReader createReader() {
        checkOpen();

        final var reader = new SegmentReader(this, acquire().access().newFileReader(), maxEntrySize);
        reader.setPosition(SegmentDescriptor.BYTES);
        readers.add(reader);
        return reader;
    }

    /**
     * Closes a segment reader.
     *
     * @param reader the closed segment reader
     */
    void closeReader(final SegmentReader reader) {
        if (readers.remove(reader)) {
            release();
        }
    }

    /**
     * Checks whether the segment is open.
     */
    private void checkOpen() {
        if (!open) {
            throw new IllegalStateException("Segment not open");
        }
    }

    /**
     * Returns a boolean indicating whether the segment is open.
     *
     * @return indicates whether the segment is open
     */
    boolean isOpen() {
        return open;
    }

    /**
     * Closes the segment.
     */
    void close() {
        if (!open) {
            return;
        }

        LOG.debug("Closing segment: {}", this);
        open = false;
        readers.forEach(SegmentReader::close);
        if (references.get() == 0) {
            finishClose();
        }
    }

    private void finishClose() {
        try {
            file.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Deletes the segment.
     */
    void delete() {
        close();
        LOG.debug("Deleting segment: {}", this);
        try {
            Files.deleteIfExists(file.path());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", file.segmentId())
            .add("version", file.version())
            .add("index", file.firstIndex())
            .toString();
    }

    static int indexEntries(final FileWriter fileWriter, final Segment segment, final SegmentIndex journalIndex,
            final long maxNextIndex, final @Nullable Position start) {
        // acquire ownership of cache and make sure reader does not see anything we've done once we're done
        final var fileReader = fileWriter.reader();
        try {
            return indexEntries(fileReader, segment, fileWriter.maxEntrySize(), journalIndex, maxNextIndex, start);
        } finally {
            // Make sure reader does not see anything we've done
            fileReader.invalidateCache();
        }
    }

    private static int indexEntries(final FileReader fileReader, final Segment segment, final int maxEntrySize,
            final SegmentIndex journalIndex, final long maxNextIndex, final @Nullable Position start) {
        int position;
        long nextIndex;
        if (start != null) {
            // look from nearest recovered index
            nextIndex = start.index();
            position = start.position();
        } else {
            // look from very beginning of the segment
            nextIndex = segment.firstIndex();
            position = SegmentDescriptor.BYTES;
        }

        final var reader = new SegmentReader(segment, fileReader, maxEntrySize);
        reader.setPosition(position);

        while (nextIndex <= maxNextIndex) {
            final var buf = reader.readBytes();
            if (buf == null) {
                break;
            }

            journalIndex.index(nextIndex++, position);
            // Update the current position for indexing.
            position += SegmentEntry.HEADER_BYTES + buf.readableBytes();
        }

        return position;
    }
}
