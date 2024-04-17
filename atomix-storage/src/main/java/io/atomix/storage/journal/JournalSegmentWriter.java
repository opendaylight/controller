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
import java.util.zip.CRC32;
import org.eclipse.jdt.annotation.NonNull;
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
    private ByteBuf lastWritten;

    JournalSegmentWriter(final FileWriter fileWriter, final JournalSegment segment, final int maxEntrySize,
            final JournalIndex index) {
        this.fileWriter = requireNonNull(fileWriter);
        this.segment = requireNonNull(segment);
        this.index = requireNonNull(index);
        maxSegmentSize = segment.descriptor().maxSegmentSize();
        this.maxEntrySize = maxEntrySize;
        // recover position and last written
        reset(JournalSegmentDescriptor.BYTES, segment.firstIndex(), Long.MAX_VALUE);
    }

    JournalSegmentWriter(final JournalSegmentWriter previous, final FileWriter fileWriter) {
        segment = previous.segment;
        index = previous.index;
        maxSegmentSize = previous.maxSegmentSize;
        maxEntrySize = previous.maxEntrySize;
        lastWritten = previous.lastWritten;
        lastIndex = previous.lastIndex;
        currentPosition = previous.currentPosition;
        this.fileWriter = requireNonNull(fileWriter);
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
    final ByteBuf getLastWritten() {
        return lastWritten == null ? null : lastWritten.slice();
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
     * @param buf binary data to append
     * @return The index of appended data, or {@code null} if segment has no space
     */
    final Long append(final ByteBuf buf) {
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
        final var writeBuffer = fileWriter.startWrite(position, length + HEADER_BYTES).position(HEADER_BYTES);
        writeBuffer.put(buf.nioBuffer());

        // Compute the checksum for the entry.
        final var crc32 = new CRC32();
        crc32.update(writeBuffer.flip().position(HEADER_BYTES));

        // Create a single byte[] in memory for the entire entry and write it as a batch to the underlying buffer.
        writeBuffer.putInt(0, length).putInt(Integer.BYTES, (int) crc32.getValue());
        fileWriter.commitWrite(position, writeBuffer.rewind());

        // Update the last entry with the correct index/term/length.
        currentPosition = nextPosition;
        lastWritten = buf;
        lastIndex = index;
        this.index.index(index, position);

        return index;
    }

    private void reset(final int startPosition, final long startIndex, final long endIndex) {
        // acquire ownership of cache and make sure reader does not see anything we've done once we're done
        final var fileReader = fileWriter.reader();
        try {
            reset(fileReader, startPosition, startIndex, endIndex);
        } finally {
            // Make sure reader does not see anything we've done
            fileReader.invalidateCache();
        }
    }

    private void reset(final FileReader fileReader, final int startPosition, final long startIndex,
            final long endIndex) {
        long currentIndex = startIndex;
        currentPosition = startPosition;
        final var reader = new JournalSegmentReader(segment, fileReader, maxEntrySize);
        reader.setPosition(startPosition);

        ByteBuf buf;
        while (currentIndex <= endIndex && (buf = reader.readBytes(currentIndex)) != null) {
            lastWritten = buf;
            lastIndex = currentIndex;
            this.index.index(currentIndex++, currentPosition);
            currentPosition += HEADER_BYTES + buf.readableBytes();
        }
    }

    /**
     * Truncates the log to the given index.
     *
     * @param endIndex The index to which to truncate the log.
     */
    void truncate(final long endIndex) {
        // If the index is greater than or equal to the last index, skip the truncate.
        if (endIndex >= getLastIndex()) {
            return;
        }

        // Reset the state
        lastIndex = null;
        lastWritten = null;
        currentPosition = JournalSegmentDescriptor.BYTES;

        // Truncate the index, find nearest indexed entry
        final var nearest = index.truncate(endIndex);

        // recover position and last written
        if (endIndex >= segment.firstIndex()) {
            if (nearest == null) {
                reset(JournalSegmentDescriptor.BYTES, segment.firstIndex(), endIndex);
            } else {
                reset(nearest.position(), nearest.index(), endIndex);
            }
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
}
