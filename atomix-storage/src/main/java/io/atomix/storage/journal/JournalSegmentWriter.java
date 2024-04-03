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
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract sealed class JournalSegmentWriter permits DiskJournalSegmentWriter, MappedJournalSegmentWriter {
    private static final Logger LOG = LoggerFactory.getLogger(JournalSegmentWriter.class);

    final @NonNull FileChannel channel;
    final @NonNull JournalSegment segment;
    private final @NonNull JournalIndex index;
    final int maxSegmentSize;
    final int maxEntrySize;

    private int currentPosition;
    private Long lastIndex;
    private byte[] lastBytes;

    JournalSegmentWriter(final FileChannel channel, final JournalSegment segment, final int maxEntrySize,
            final JournalIndex index) {
        this.channel = requireNonNull(channel);
        this.segment = requireNonNull(segment);
        this.index = requireNonNull(index);
        maxSegmentSize = segment.descriptor().maxSegmentSize();
        this.maxEntrySize = maxEntrySize;
    }

    JournalSegmentWriter(final JournalSegmentWriter previous) {
        channel = previous.channel;
        segment = previous.segment;
        index = previous.index;
        maxSegmentSize = previous.maxSegmentSize;
        maxEntrySize = previous.maxEntrySize;
        lastBytes = previous.lastBytes;
        lastIndex = previous.lastIndex;
        currentPosition = previous.currentPosition;
    }

    /**
     * Returns the last written index.
     *
     * @return The last written index.
     */
    final long getLastIndex() {
        return lastIndex != null ? lastIndex : segment.firstIndex() - 1;
    }

    /**
     * Returns the last data written.
     *
     * @return The last data written.
     */
    final byte[] getLastBytes() {
        return lastBytes;
    }

    /**
     * Returns the next index to be written.
     *
     * @return The next index to be written.
     */
    final long getNextIndex() {
        return lastIndex != null ? lastIndex + 1 : segment.firstIndex();
    }

    /**
     * Tries to append a binary data to the journal.
     *
     * @param bytes binary data to append
     * @return The index of appended data, or {@code null} if segment has no space
     */
    final Long append(final byte[] bytes) {
        if (bytes.length > maxEntrySize) {
            throw new StorageException.TooLarge("Serialized entry size exceeds maximum allowed bytes ("
                + maxEntrySize + ")");
        }

        // Store the entry index.
        final long index = getNextIndex();
        final int position = currentPosition;

        // check space available
        final int nextPosition = position + HEADER_BYTES + bytes.length;
        if (nextPosition >= maxSegmentSize) {
            LOG.trace("Not enough space for {} at {}", index, position);
            return null;
        }

        // allocate buffer and write data
        final var writeBuffer = startWrite(position, bytes.length + HEADER_BYTES).position(HEADER_BYTES);
        writeBuffer.put(bytes);

        // Compute the checksum for the entry.
        final var crc32 = new CRC32();
        crc32.update(writeBuffer.flip().position(HEADER_BYTES));

        // Create a single byte[] in memory for the entire entry and write it as a batch to the underlying buffer.
        writeBuffer.putInt(0, bytes.length).putInt(Integer.BYTES, (int) crc32.getValue());
        commitWrite(position, writeBuffer.rewind());

        // Update the last entry with the correct index/term/length.
        currentPosition = nextPosition;
        lastBytes = bytes;
        lastIndex = index;
        this.index.index(index, position);

        return index;
    }

    abstract ByteBuffer startWrite(int position, int size);

    abstract void commitWrite(int position, ByteBuffer entry);

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

    abstract JournalSegmentReader reader();

    private void resetWithBuffer(final JournalSegmentReader reader, final long index) {
        long nextIndex = segment.firstIndex();

        // Clear the buffer indexes and acquire ownership of the buffer
        currentPosition = JournalSegmentDescriptor.BYTES;
        reader.setPosition(JournalSegmentDescriptor.BYTES);

        while (index == 0 || nextIndex <= index) {
            final var bytes = reader.readBytes(nextIndex);
            if (bytes == null) {
                break;
            }

            lastBytes = bytes;
            lastIndex = nextIndex;
            this.index.index(nextIndex, currentPosition);
            nextIndex++;

            // Update the current position for indexing.
            currentPosition += HEADER_BYTES + bytes.length;
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

        // Reset the last written
        lastIndex = null;
        lastBytes = null;

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

    abstract @NonNull MappedJournalSegmentWriter toMapped();

    abstract @NonNull DiskJournalSegmentWriter toFileChannel();
}
