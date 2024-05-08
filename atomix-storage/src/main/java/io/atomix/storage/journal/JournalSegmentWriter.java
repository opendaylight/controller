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

import io.atomix.storage.journal.StorageException.TooLarge;
import io.atomix.storage.journal.index.JournalIndex;
import io.netty.buffer.Unpooled;
import java.io.EOFException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JournalSegmentWriter {
    private static final Logger LOG = LoggerFactory.getLogger(JournalSegmentWriter.class);

    private final FileWriter fileWriter;
    final @NonNull JournalSegment segment;
    private final @NonNull JournalIndex journalIndex;
    final int maxSegmentSize;
    final int maxEntrySize;

    private int currentPosition;

    JournalSegmentWriter(final FileWriter fileWriter, final JournalSegment segment, final int maxEntrySize,
            final JournalIndex journalIndex) {
        this.fileWriter = requireNonNull(fileWriter);
        this.segment = requireNonNull(segment);
        this.journalIndex = requireNonNull(journalIndex);
        maxSegmentSize = segment.file().maxSize();
        this.maxEntrySize = maxEntrySize;

        // recover currentPosition and lastIndex
        reset(Long.MAX_VALUE, null);
    }

    JournalSegmentWriter(final JournalSegmentWriter previous, final FileWriter fileWriter) {
        segment = previous.segment;
        journalIndex = previous.journalIndex;
        maxSegmentSize = previous.maxSegmentSize;
        maxEntrySize = previous.maxEntrySize;
        currentPosition = previous.currentPosition;
        this.fileWriter = requireNonNull(fileWriter);
    }

    /**
     * Returns the next index to be written.
     *
     * @return The next index to be written.
     */
    long nextIndex() {
        final var lastPosition = journalIndex.last();
        return lastPosition != null ? lastPosition.index() + 1 : segment.firstIndex();
    }

    /**
     * Tries to append a binary data to the journal.
     *
     * @param mapper the mapper to use
     * @param entry the entry
     * @return the entry size, or {@code null} if segment has no space
     */
    <T> @Nullable Integer append(final ByteBufMapper<T> mapper, final T entry) {
        // we are appending at this index and position
        final long index = nextIndex();
        final int position = currentPosition;

        // Map the entry carefully: we may not have enough segment space to satisfy maxEntrySize, but most entries are
        // way smaller than that.
        final int bodyPosition = position + HEADER_BYTES;
        final int avail = maxSegmentSize - bodyPosition;
        if (avail <= 0) {
            // we do not have enough space for the header and a byte: signal a retry
            LOG.trace("Not enough space for {} at {}", index, position);
            return null;
        }

        // Entry must not exceed maxEntrySize
        final var writeLimit = Math.min(avail, maxEntrySize);

        // Allocate entry space
        final var diskEntry = fileWriter.startWrite(position, writeLimit + HEADER_BYTES);
        // Create a ByteBuf covering the bytes. Note we do not use slice(), as Netty will do the equivalent.
        final var bytes = Unpooled.wrappedBuffer(diskEntry.position(HEADER_BYTES));
        try {
            mapper.objectToBytes(entry, bytes);
        } catch (EOFException e) {
            // We ran out of buffer space: let's decide who's fault it is:
            if (writeLimit == maxEntrySize) {
                // - it is the entry and/or mapper. This is not exactly accurate, as there may be other serialization
                //   fault. This is as good as it gets.
                throw new TooLarge("Serialized entry size exceeds maximum allowed bytes (" + maxEntrySize + ")", e);
            }

            // - it is us, as we do not have the capacity to hold maxEntrySize bytes
            LOG.trace("Tail serialization with {} bytes available failed", writeLimit, e);
            return null;
        } catch (IOException e) {
            throw new StorageException(e);
        }

        // Determine length, trim distEntry and compute checksum. We are okay with computeChecksum() consuming
        // the buffer, as we rewind() it back.
        final var length = bytes.readableBytes();
        final var checksum = SegmentEntry.computeChecksum(
            diskEntry.limit(HEADER_BYTES + length).position(HEADER_BYTES));

        // update the header and commit entry to file
        fileWriter.commitWrite(position, diskEntry.rewind().putInt(0, length).putInt(Integer.BYTES, checksum));

        // Update the last entry with the correct index/term/length.
        currentPosition = bodyPosition + length;
        journalIndex.index(index, position);
        return length;
    }

    /**
     * Truncates the log to the given index.
     *
     * @param index The index to which to truncate the log.
     */
    void truncate(final long index) {
        // If the index is greater than or equal to the last index, skip the truncate.
        if (index >= segment.lastIndex()) {
            return;
        }

        // Truncate the index, find nearest indexed entry
        final var nearest = journalIndex.truncate(index);

        // recover position and last written
        if (index >= segment.firstIndex()) {
            reset(index, nearest);
        } else {
            currentPosition = JournalSegmentDescriptor.BYTES;
        }

        // Zero the entry header at current channel position.
        fileWriter.writeEmptyHeader(currentPosition);
    }

    private void reset(final long maxNextIndex, final @Nullable Position position) {
        // acquire ownership of cache and make sure reader does not see anything we've done once we're done
        final var fileReader = fileWriter.reader();
        try {
            reset(fileReader, maxNextIndex, position);
        } finally {
            // Make sure reader does not see anything we've done
            fileReader.invalidateCache();
        }
    }

    private void reset(final FileReader fileReader, final long maxNextIndex, final @Nullable Position position) {
        long nextIndex;
        if (position != null) {
            // look from nearest recovered index
            nextIndex = position.index();
            currentPosition = position.position();
        } else {
            // look from very beginning of the segment
            nextIndex = segment.firstIndex();
            currentPosition = JournalSegmentDescriptor.BYTES;
        }

        final var reader = new JournalSegmentReader(segment, fileReader, maxEntrySize);
        reader.setPosition(currentPosition);

        while (nextIndex <= maxNextIndex) {
            final var buf = reader.readBytes();
            if (buf == null) {
                break;
            }

            journalIndex.index(nextIndex++, currentPosition);
            // Update the current position for indexing.
            currentPosition += HEADER_BYTES + buf.readableBytes();
        }
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
