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

import io.atomix.storage.journal.JournalSegment.Inactive;
import io.atomix.storage.journal.StorageException.TooLarge;
import io.atomix.storage.journal.index.JournalIndex;
import java.io.EOFException;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JournalSegmentWriter {
    private static final Logger LOG = LoggerFactory.getLogger(JournalSegmentWriter.class);

    private final FileWriter fileWriter;
    final @NonNull JournalSegment segment;
    private final @NonNull JournalIndex journalIndex;

    private int currentPosition;

    JournalSegmentWriter(final FileWriter fileWriter, final JournalSegment segment, final JournalIndex journalIndex,
            final int currentPosition) {
        this.fileWriter = requireNonNull(fileWriter);
        this.segment = requireNonNull(segment);
        this.journalIndex = requireNonNull(journalIndex);
        this.currentPosition = currentPosition;
    }

    JournalSegmentWriter(final FileWriter fileWriter, final JournalSegment segment, final JournalIndex journalIndex,
            final Inactive segmentState) {
        this.fileWriter = requireNonNull(fileWriter);
        this.segment = requireNonNull(segment);
        this.journalIndex = requireNonNull(journalIndex);
        currentPosition = segmentState.position();
    }

    int currentPosition() {
        return currentPosition;
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
        final int avail = segment.file().maxSize() - bodyPosition;
        if (avail <= 0) {
            // we do not have enough space for the header and a byte: signal a retry
            LOG.trace("Not enough space for {} at {}", index, position);
            return null;
        }

        // Entry must not exceed maxEntrySize
        final var maxEntrySize = fileWriter.maxEntrySize();
        final var writeLimit = Math.min(avail, maxEntrySize);

        // Allocate entry space
        final var diskEntry = fileWriter.startWrite(position, writeLimit + HEADER_BYTES);
        // Create a ByteBuf covering the bytes
        final var bytes = diskEntry.slice(HEADER_BYTES, writeLimit);
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

        // Determine length, trim disktEntry and compute checksum.
        final var length = bytes.readableBytes();
        diskEntry.writerIndex(diskEntry.readerIndex() + HEADER_BYTES + length);

        // Compute the checksum
        final var checksum = SegmentEntry.computeChecksum(diskEntry.nioBuffer(HEADER_BYTES, length));

        // update the header and commit entry to file
        fileWriter.commitWrite(position, diskEntry.setInt(0, length).setInt(Integer.BYTES, checksum));

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

        currentPosition = index < segment.firstIndex() ? JournalSegmentDescriptor.BYTES
            // recover position and last written
            : JournalSegment.indexEntries(fileWriter, segment, journalIndex, index, nearest);

        // Zero the entry header at current channel position.
        fileWriter.writeEmptyHeader(currentPosition);
    }

    /**
     * Flushes written entries to disk.
     */
    void flush() {
        try {
            fileWriter.flush();
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }
}
