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

import static io.atomix.storage.journal.SegmentEntry.HEADER_BYTES;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import io.atomix.storage.journal.index.JournalIndex;
import io.atomix.storage.journal.index.Position;
import io.atomix.storage.journal.index.SparseJournalIndex;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log segment.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class JournalSegment {
    /**
     * Encapsulation of a {@link JournalSegment}'s state.
     */
    sealed interface State {
        // Marker interface
    }

    /**
     * Journal segment is active, i.e. there is a associated with it.
     */
    @NonNullByDefault
    record Active(FileAccess access, JournalSegmentWriter writer) implements State {
        Active {
            requireNonNull(access);
            requireNonNull(writer);
        }

        Inactive deactivate() {
            final var inactive = new Inactive(writer.currentPosition());
            access.close();
            return inactive;
        }
    }

    /**
     * Journal segment is inactive, i.e. there is no writer associated with it.
     */
    @NonNullByDefault
    record Inactive(int position) implements State {
        Active activate(final JournalSegment segment) throws IOException {
            final var access = segment.file.newAccess(segment.storageLevel, segment.maxEntrySize);
            return new Active(access, new JournalSegmentWriter(access.newFileWriter(), segment, segment.journalIndex,
                this));
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(JournalSegment.class);

    private final Set<JournalSegmentReader> readers = ConcurrentHashMap.newKeySet();
    private final AtomicInteger references = new AtomicInteger();
    private final @NonNull JournalSegmentFile file;
    private final @NonNull StorageLevel storageLevel;
    private final @NonNull JournalIndex journalIndex;
    private final int maxEntrySize;

    private State state;
    private boolean open = true;

    JournalSegment(
        final JournalSegmentFile file,
        final StorageLevel storageLevel,
        final int maxEntrySize,
        final double indexDensity) {
        this.file = requireNonNull(file);
        this.storageLevel = requireNonNull(storageLevel);
        this.maxEntrySize = maxEntrySize;

        journalIndex = new SparseJournalIndex(indexDensity);

        try (var tmpAccess = file.newAccess(storageLevel, maxEntrySize)) {
            final var fileReader = tmpAccess.newFileReader();
            try {
                state = new Inactive(indexEntries(fileReader, this, maxEntrySize, journalIndex, Long.MAX_VALUE, null));
            } finally {
                fileReader.release();
            }
        } catch (IOException e) {
            throw new StorageException(e);
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
        final var lastPosition = journalIndex.last();
        return lastPosition != null ? lastPosition.index() : firstIndex() - 1;
    }

    /**
     * Returns the segment file.
     *
     * @return The segment file.
     */
    JournalSegmentFile file() {
        return file;
    }

    /**
     * Looks up the position of the given index.
     *
     * @param index the index to lookup
     * @return the position of the given index or a lesser index, or {@code null}
     */
    @Nullable Position lookup(final long index) {
        return journalIndex.lookup(index);
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
            throw new StorageException(e);
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
     * @return The segment writer.
     */
    JournalSegmentWriter acquireWriter() {
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
     * @return A new segment reader.
     */
    JournalSegmentReader createReader() {
        checkOpen();

        final var reader = new JournalSegmentReader(this, acquire().access().newFileReader(), maxEntrySize);
        reader.setPosition(JournalSegmentDescriptor.BYTES);
        readers.add(reader);
        return reader;
    }

    /**
     * Closes a segment reader.
     *
     * @param reader the closed segment reader
     */
    void closeReader(final JournalSegmentReader reader) {
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
        readers.forEach(JournalSegmentReader::close);
        if (references.get() == 0) {
            finishClose();
        }
    }

    private void finishClose() {
        try {
            file.close();
        } catch (IOException e) {
            throw new StorageException(e);
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
            throw new StorageException(e);
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

    static int indexEntries(final FileWriter fileWriter, final JournalSegment segment, final JournalIndex journalIndex,
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

    private static int indexEntries(final FileReader fileReader, final JournalSegment segment, final int maxEntrySize,
            final JournalIndex journalIndex, final long maxNextIndex, final @Nullable Position start) {
        int position;
        long nextIndex;
        if (start != null) {
            // look from nearest recovered index
            nextIndex = start.index();
            position = start.position();
        } else {
            // look from very beginning of the segment
            nextIndex = segment.firstIndex();
            position = JournalSegmentDescriptor.BYTES;
        }

        final var reader = new JournalSegmentReader(segment, fileReader, maxEntrySize);
        reader.setPosition(position);

        while (nextIndex <= maxNextIndex) {
            final var buf = reader.readBytes();
            if (buf == null) {
                break;
            }

            journalIndex.index(nextIndex++, position);
            // Update the current position for indexing.
            position += HEADER_BYTES + buf.readableBytes();
        }

        return position;
    }
}
