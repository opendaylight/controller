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
package org.opendaylight.raft.journal;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SegmentReader {
    private static final Logger LOG = LoggerFactory.getLogger(SegmentReader.class);

    private final Segment segment;
    private final int maxSegmentSize;
    private final int maxEntrySize;

    private FileReader fileReader;
    private int position;

    SegmentReader(final Segment segment, final FileReader fileReader, final int maxEntrySize) {
        this.segment = requireNonNull(segment);
        this.fileReader = requireNonNull(fileReader);
        maxSegmentSize = segment.file().maxSize();
        this.maxEntrySize = maxEntrySize;
    }

    /**
     * Return the current position.
     *
     * @return current position.
     */
    int position() {
        return position;
    }

    /**
     * Set the file position.
     *
     * @param position new position
     */
    void setPosition(final int position) {
        verify(position >= SegmentDescriptor.BYTES && position < maxSegmentSize, "Invalid position %s", position);
        this.position = position;
        fileReader.invalidateCache();
    }

    /**
     * Invalidate any cache that is present, so that the next read is coherent with the backing file.
     */
    void invalidateCache() {
        fileReader.invalidateCache();
    }

    /**
     * Reads the next binary data block.
     *
     * @return The binary data, or {@code null}
     */
    @Nullable ByteBuf readBytes() {
        // Check if there is enough in the buffer remaining
        final int remaining = maxSegmentSize - position - SegmentEntry.HEADER_BYTES;
        if (remaining < 0) {
            // Not enough space in the segment, there can never be another entry
            return null;
        }

        // Calculate maximum entry length not exceeding file size nor maxEntrySize
        final var maxLength = Math.min(remaining, maxEntrySize);
        final var buffer = fileReader.read(position, maxLength + SegmentEntry.HEADER_BYTES);

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
        final var entryBuffer = buffer.slice(SegmentEntry.HEADER_BYTES, length);
        // If the stored checksum does not equal the computed checksum, do not proceed further
        final var computed = SegmentEntry.computeChecksum(entryBuffer.nioBuffer());
        if (checksum != computed) {
            LOG.warn("Expected checksum {}, computed {}", Integer.toHexString(checksum), Integer.toHexString(computed));
            invalidateCache();
            return null;
        }

        // update position and return
        position += SegmentEntry.HEADER_BYTES + length;
        return entryBuffer;
    }

    /**
     * Close this reader.
     */
    void close() {
        final var local = fileReader;
        if (local != null) {
            fileReader = null;
            local.release();
            segment.closeReader(this);
        }
    }
}
