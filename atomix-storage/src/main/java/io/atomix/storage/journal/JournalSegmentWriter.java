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
import io.netty.buffer.ByteBuf;
import java.nio.MappedByteBuffer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JournalSegmentWriter {
    private static final Logger LOG = LoggerFactory.getLogger(JournalSegmentWriter.class);

    private final FileWriter fileWriter;
    final @NonNull JournalSegment segment;
    private final @NonNull JournalIndex index;
    final int maxSegmentSize;
    final int maxEntrySize;

    private int currentPosition;
    private Long lastIndex;

    JournalSegmentWriter(final FileWriter fileWriter, final JournalSegment segment, final int maxEntrySize,
            final JournalIndex index) {
        this.fileWriter = requireNonNull(fileWriter);
        this.segment = requireNonNull(segment);
        this.index = requireNonNull(index);
        maxSegmentSize = segment.file().maxSize();
        this.maxEntrySize = maxEntrySize;
        // adjust lastEntry value
        reset(0);
    }

    JournalSegmentWriter(final JournalSegmentWriter previous, final FileWriter fileWriter) {
        segment = previous.segment;
        index = previous.index;
        maxSegmentSize = previous.maxSegmentSize;
        maxEntrySize = previous.maxEntrySize;
        lastIndex = previous.lastIndex;
        currentPosition = previous.currentPosition;
        this.fileWriter = requireNonNull(fileWriter);
    }

    /**
     * Returns the last written index.
     *
     * @return The last written index.
     */
    long getLastIndex() {
        return lastIndex != null ? lastIndex : segment.firstIndex() - 1;
    }

    /**
     * Returns the next index to be written.
     *
     * @return The next index to be written.
     */
    long getNextIndex() {
        return lastIndex != null ? lastIndex + 1 : segment.firstIndex();
    }

    /**
     * Tries to append a binary data to the journal.
     *
     * @param buf binary data to append
     * @return The index of appended data, or {@code null} if segment has no space
     */
    Long append(final ByteBuf buf) {
        final var length = buf.readableBytes();
        if (length > maxEntrySize) {
            throw new StorageException.TooLarge("Serialized entry size exceeds maximum allowed bytes ("
                + maxEntrySize + ")");
        }

        // Store the entry index.
        final long index = getNextIndex();
        final int position = currentPosition;

        // check space available
        final int nextPosition = position + HEADER_BYTES + length;
        if (nextPosition >= maxSegmentSize) {
            LOG.trace("Not enough space for {} at {}", index, position);
            return null;
        }

        // allocate buffer and write data
        final var writeBuffer = fileWriter.startWrite(position, length + HEADER_BYTES);
        writeBuffer.put(HEADER_BYTES, buf.nioBuffer(), 0, length);

        // Compute the checksum for the entry.
        final var checksum = SegmentEntry.computeChecksum(writeBuffer.slice(HEADER_BYTES, length));

        // Create a single byte[] in memory for the entire entry and write it as a batch to the underlying buffer.
        fileWriter.commitWrite(position, writeBuffer.putInt(0, length).putInt(Integer.BYTES, checksum));

        // Update the last entry with the correct index/term/length.
        currentPosition = nextPosition;
        lastIndex = index;
        this.index.index(index, position);

        return index;
    }

    /**
     * Resets the head of the segment to the given index.
     *
     * @param index the index to which to reset the head of the segment
     */
    void reset(final long index) {
        // acquire ownership of cache and make sure reader does not see anything we've done once we're done
        final var fileReader = fileWriter.reader();
        try {
            resetWithBuffer(fileReader, index);
        } finally {
            // Make sure reader does not see anything we've done
            fileReader.invalidateCache();
        }
    }

    private void resetWithBuffer(final FileReader fileReader, final long index) {
        long nextIndex = segment.firstIndex();

        // Clear the buffer indexes and acquire ownership of the buffer
        currentPosition = JournalSegmentDescriptor.BYTES;
        final var reader = new JournalSegmentReader(segment, fileReader, maxEntrySize);
        reader.setPosition(JournalSegmentDescriptor.BYTES);

        while (index == 0 || nextIndex <= index) {
            final var buf = reader.readBytes(nextIndex);
            if (buf == null) {
                break;
            }

            lastIndex = nextIndex;
            this.index.index(nextIndex, currentPosition);
            nextIndex++;

            // Update the current position for indexing.
            currentPosition += HEADER_BYTES + buf.readableBytes();
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

        // Reset the last written
        lastIndex = null;

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

    @NonNull JournalSegmentWriter toMapped() {
        final var newWriter = fileWriter.toMapped();
        return newWriter == null ? this : new JournalSegmentWriter(this, newWriter);
    }

    @NonNull JournalSegmentWriter toFileChannel() {
        final var newWriter = fileWriter.toDisk();
        return newWriter == null ? this : new JournalSegmentWriter(this, newWriter);
    }
}
