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

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.esotericsoftware.kryo.KryoException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract sealed class JournalSegmentReader<E> permits DiskJournalSegmentReader, MappedJournalSegmentReader {
    private static final Logger LOG = LoggerFactory.getLogger(JournalSegmentReader.class);

    private final JournalSegment<E> segment;
    private final JournalSerdes namespace;
    private final int maxSegmentSize;
    private final int maxEntrySize;

    private int position;

    JournalSegmentReader(final JournalSegment<E> segment, final int maxEntrySize, final JournalSerdes namespace) {
        this.segment = requireNonNull(segment);
        maxSegmentSize = segment.descriptor().maxSegmentSize();
        this.maxEntrySize = maxEntrySize;
        this.namespace = requireNonNull(namespace);
    }

    /**
     * Return the current position.
     *
     * @return current position.
     */
    final int position() {
        return position;
    }

    /**
     * Set the file position.
     *
     * @param position new position
     */
    final void setPosition(final int position) {
        verify(position >= JournalSegmentDescriptor.BYTES && position < maxSegmentSize,
            "Invalid position %s", position);
        this.position = position;
        invalidateCache();
    }

    /**
     * Invalidate any cache that is present, so that the next read is coherent with the backing file.
     */
    abstract void invalidateCache();

    /**
     * Reads the next entry, assigning it specified index.
     *
     * @param index entry index
     * @return The entry, or {@code null}
     */
    final @Nullable Indexed<E> readEntry(final long index) {
        // Check if there is enough in the buffer remaining
        final int remaining = maxSegmentSize - position - SegmentEntry.HEADER_BYTES;
        if (remaining < 0) {
            // Not enough space in the segment, there can never be another entry
            return null;
        }

        // Calculate maximum entry length not exceeding file size nor maxEntrySize
        final var maxLength = Math.min(remaining, maxEntrySize);
        final var buffer = read(position, maxLength + SegmentEntry.HEADER_BYTES);

        // Read the entry length
        final var length = buffer.getInt(0);
        if (length < 1 || length > maxLength) {
            // Invalid length, make sure next read re-tries
            invalidateCache();
            return null;
        }

        // Read the entry checksum
        final int checksum = buffer.getInt(Integer.BYTES);

        // Slice off the entry's bytes
        final var entryBytes = buffer.slice(SegmentEntry.HEADER_BYTES, length);
        // Compute the checksum for the entry bytes.
        final var crc32 = new CRC32();
        crc32.update(entryBytes);

        // If the stored checksum does not equal the computed checksum, do not proceed further
        final var computed = (int) crc32.getValue();
        if (checksum != computed) {
            LOG.warn("Expected checksum {}, computed {}", Integer.toHexString(checksum), Integer.toHexString(computed));
            invalidateCache();
            return null;
        }

        // Attempt to deserialize
        final E entry;
        try {
            entry = namespace.deserialize(entryBytes.rewind());
        } catch (KryoException e) {
            // TODO: promote this to a hard error, as it should never happen
            LOG.debug("Failed to deserialize entry", e);
            invalidateCache();
            return null;
        }

        // We are all set. Update the position.
        position = position + SegmentEntry.HEADER_BYTES + length;
        return new Indexed<>(index, entry, length);
    }

    /**
     * Read the some bytes as specified position. The sum of position and size is guaranteed not to exceed
     * {@link #maxSegmentSize}.
     *
     * @param position position to the entry header
     * @param size to read, guaranteed to not exceed {@link #maxEntrySize}
     * @return resulting buffer
     */
    abstract ByteBuffer read(int position, int size);

    /**
     * Close this reader.
     */
    final void close() {
        segment.closeReader(this);
    }
}
