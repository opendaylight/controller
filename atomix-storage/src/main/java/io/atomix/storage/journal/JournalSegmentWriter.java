/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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

import io.atomix.storage.journal.index.JournalIndex;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

abstract sealed class JournalSegmentWriter<E> permits DiskJournalSegmentWriter, MappedJournalSegmentWriter {
    final @NonNull FileChannel channel;
    final @NonNull JournalSegment<E> segment;
    final @NonNull JournalIndex index;
    final @NonNull JournalSerdes namespace;
    final int maxSegmentSize;
    final int maxEntrySize;

    // FIXME: hide these two fields
    Indexed<E> lastEntry;
    int currentPosition;

    JournalSegmentWriter(final FileChannel channel, final JournalSegment<E> segment, final int maxEntrySize,
            final JournalIndex index, final JournalSerdes namespace) {
        this.channel = requireNonNull(channel);
        this.segment = requireNonNull(segment);
        this.index = requireNonNull(index);
        this.namespace = requireNonNull(namespace);
        maxSegmentSize = segment.descriptor().maxSegmentSize();
        this.maxEntrySize = maxEntrySize;
    }

    JournalSegmentWriter(final JournalSegmentWriter<E> previous) {
        channel = previous.channel;
        segment = previous.segment;
        index = previous.index;
        namespace = previous.namespace;
        maxSegmentSize = previous.maxSegmentSize;
        maxEntrySize = previous.maxEntrySize;
        lastEntry = previous.lastEntry;
        currentPosition = previous.currentPosition;
    }

    /**
     * Returns the last written index.
     *
     * @return The last written index.
     */
    final long getLastIndex() {
        return lastEntry != null ? lastEntry.index() : segment.firstIndex() - 1;
    }

    /**
     * Returns the last entry written.
     *
     * @return The last entry written.
     */
    final Indexed<E> getLastEntry() {
        return lastEntry;
    }

    /**
     * Returns the next index to be written.
     *
     * @return The next index to be written.
     */
    final long getNextIndex() {
        return lastEntry != null ? lastEntry.index() + 1 : segment.firstIndex();
    }

    /**
     * Appends an entry to the journal.
     *
     * @param entry The entry to append.
     * @return The appended indexed entry.
     */
    abstract <T extends E> Indexed<T> append(T entry);

    /**
     * Resets the head of the segment to the given index.
     *
     * @param index the index to which to reset the head of the segment
     */
    final void reset(final long index) {
        // acquire ownership of cache and make sure reader does not see anything we've done once we're done
        final var reader = reader();
        reader.invalidateCache();
        try {
            resetWithBuffer(reader, index);
        } finally {
            // Make sure reader does not see anything we've done
            reader.invalidateCache();
        }
    }

    abstract JournalSegmentReader<E> reader();

    private void resetWithBuffer(final JournalSegmentReader<E> reader, final long index) {
        long nextIndex = segment.firstIndex();

        // Clear the buffer indexes and acquire ownership of the buffer
        currentPosition = JournalSegmentDescriptor.BYTES;
        reader.setPosition(JournalSegmentDescriptor.BYTES);

        while (index == 0 || nextIndex <= index) {
            final var entry = reader.readEntry(nextIndex);
            if (entry == null) {
                break;
            }

            lastEntry = entry;
            this.index.index(nextIndex, currentPosition);
            nextIndex++;

            // Update the current position for indexing.
            currentPosition = currentPosition + HEADER_BYTES + entry.size();
        }
    }

    /**
     * Truncates the log to the given index.
     *
     * @param index The index to which to truncate the log.
     */
    final void truncate(final long index) {
        // If the index is greater than or equal to the last index, skip the truncate.
        if (index >= getLastIndex()) {
          return;
        }

        // Reset the last entry.
        lastEntry = null;

        // Truncate the index.
        this.index.truncate(index);

        if (index < segment.firstIndex()) {
          // Reset the writer to the first entry.
          currentPosition = JournalSegmentDescriptor.BYTES;
        } else {
          // Reset the writer to the given index.
          reset(index);
        }

        // Zero the entry header at current channel position.
        writeEmptyHeader(currentPosition);
    }

    /**
     * Write {@link SegmentEntry#HEADER_BYTES} worth of zeroes at specified position.
     *
     * @param position position to write to
     */
    abstract void writeEmptyHeader(int position);

    /**
     * Flushes written entries to disk.
     */
    abstract void flush();

    /**
     * Closes this writer.
     */
    abstract void close();

    /**
     * Returns the mapped buffer underlying the segment writer, or {@code null} if the writer does not have such a
     * buffer.
     *
     * @return the mapped buffer underlying the segment writer, or {@code null}.
     */
    abstract @Nullable MappedByteBuffer buffer();

    abstract @NonNull MappedJournalSegmentWriter<E> toMapped();

    abstract @NonNull DiskJournalSegmentWriter<E> toFileChannel();
}
