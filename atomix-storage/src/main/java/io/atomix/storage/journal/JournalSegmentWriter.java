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

import com.esotericsoftware.kryo.KryoException;
import io.atomix.storage.journal.index.JournalIndex;
import java.nio.MappedByteBuffer;
import java.util.zip.CRC32;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JournalSegmentWriter<E> {
    private static final Logger LOG = LoggerFactory.getLogger(JournalSegmentWriter.class);

    private final FileWriter fileWriter;
    final @NonNull JournalSegment<E> segment;
    private final @NonNull JournalIndex index;
    final @NonNull JournalSerdes namespace;
    final int maxSegmentSize;
    final int maxEntrySize;

    private Indexed<E> lastEntry;
    private int currentPosition;

    JournalSegmentWriter(final FileWriter fileWriter, final JournalSegment<E> segment, final int maxEntrySize,
            final JournalIndex index, final JournalSerdes namespace) {
        this.fileWriter = requireNonNull(fileWriter);
        this.segment = requireNonNull(segment);
        this.index = requireNonNull(index);
        this.namespace = requireNonNull(namespace);
        maxSegmentSize = segment.descriptor().maxSegmentSize();
        this.maxEntrySize = maxEntrySize;
    }

    // FIXME: this is baad
    JournalSegmentWriter(final JournalSegmentWriter<E> previous, final FileWriter fileWriter) {
        segment = previous.segment;
        index = previous.index;
        namespace = previous.namespace;
        maxSegmentSize = previous.maxSegmentSize;
        maxEntrySize = previous.maxEntrySize;
        lastEntry = previous.lastEntry;
        currentPosition = previous.currentPosition;
        this.fileWriter = requireNonNull(fileWriter);
    }

    /**
     * Returns the last written index.
     *
     * @return The last written index.
     */
    long getLastIndex() {
        return lastEntry != null ? lastEntry.index() : segment.firstIndex() - 1;
    }

    /**
     * Returns the last entry written.
     *
     * @return The last entry written.
     */
    Indexed<E> getLastEntry() {
        return lastEntry;
    }

    /**
     * Returns the next index to be written.
     *
     * @return The next index to be written.
     */
    long getNextIndex() {
        return lastEntry != null ? lastEntry.index() + 1 : segment.firstIndex();
    }

    /**
     * Tries to append an entry to the journal.
     *
     * @param entry The entry to append.
     * @return The appended indexed entry, or {@code null} if there is not enough space available
     */
    <T extends E> @Nullable Indexed<T> append(final T entry) {
        // Store the entry index.
        final long index = getNextIndex();
        final int position = currentPosition;

        // Serialize the entry.
        final int bodyPosition = position + HEADER_BYTES;
        final int avail = maxSegmentSize - bodyPosition;
        if (avail < 0) {
            LOG.trace("Not enough space for {} at {}", index, position);
            return null;
        }

        final var writeLimit = Math.min(avail, maxEntrySize);
        final var diskEntry = fileWriter.startWrite(position, writeLimit + HEADER_BYTES).position(HEADER_BYTES);
        try {
            namespace.serialize(entry, diskEntry);
        } catch (KryoException e) {
            if (writeLimit != maxEntrySize) {
                // We have not provided enough capacity, signal to roll to next segment
                LOG.trace("Tail serialization with {} bytes available failed", writeLimit, e);
                return null;
            }

            // Just reset the buffer. There's no need to zero the bytes since we haven't written the length or checksum.
            throw new StorageException.TooLarge("Entry size exceeds maximum allowed bytes (" + maxEntrySize + ")", e);
        }

        final int length = diskEntry.position() - HEADER_BYTES;

        // Compute the checksum for the entry.
        final var crc32 = new CRC32();
        crc32.update(diskEntry.flip().position(HEADER_BYTES));

        // Create a single byte[] in memory for the entire entry and write it as a batch to the underlying buffer.
        diskEntry.putInt(0, length).putInt(Integer.BYTES, (int) crc32.getValue());
        fileWriter.commitWrite(position, diskEntry.rewind());

        // Update the last entry with the correct index/term/length.
        final var indexedEntry = new Indexed<E>(index, entry, length);
        lastEntry = indexedEntry;
        this.index.index(index, position);

        currentPosition = bodyPosition + length;

        @SuppressWarnings("unchecked")
        final var ugly = (Indexed<T>) indexedEntry;
        return ugly;
    }

    /**
     * Resets the head of the segment to the given index.
     *
     * @param index the index to which to reset the head of the segment
     */
    void reset(final long index) {
        // acquire ownership of cache and make sure reader does not see anything we've done once we're done
        final var reader = fileWriter.reader();
        reader.invalidateCache();
        try {
            resetWithBuffer(new JournalSegmentReader<>(segment, reader, maxEntrySize, namespace), index);
        } finally {
            // Make sure reader does not see anything we've done
            reader.invalidateCache();
        }
    }

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
    void truncate(final long index) {
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
        fileWriter.writeEmptyHeader(currentPosition);
    }

    /**
     * Flushes written entries to disk.
     */
    void flush() {
        fileWriter.flush();
    }

    /**
     * Closes this writer.
     */
    void close() {
        fileWriter.close();
    }

    /**
     * Returns the mapped buffer underlying the segment writer, or {@code null} if the writer does not have such a
     * buffer.
     *
     * @return the mapped buffer underlying the segment writer, or {@code null}.
     */
    @Nullable MappedByteBuffer buffer() {
        return fileWriter.buffer();
    }

    @NonNull JournalSegmentWriter<E> toMapped() {
        final var newWriter = fileWriter.toMapped();
        return newWriter == null ? this : new JournalSegmentWriter<>(this, newWriter);
    }

    @NonNull JournalSegmentWriter<E> toFileChannel() {
        final var newWriter = fileWriter.toDisk();
        return newWriter == null ? this : new JournalSegmentWriter<>(this, newWriter);
    }
}
