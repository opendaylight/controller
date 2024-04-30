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
import static io.atomix.storage.journal.SegmentEntry.HEADER_BYTES;
import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.Nullable;

final class JournalSegmentReader {
    private final JournalSegment segment;
    private final FileReader fileReader;
    private final int maxSegmentSize;

    private int position;

    JournalSegmentReader(final JournalSegment segment, final FileReader fileReader) {
        this.segment = requireNonNull(segment);
        this.fileReader = requireNonNull(fileReader);
        this.maxSegmentSize = segment.maxSegmentSize();
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
        verify(position >= JournalSegmentDescriptor.BYTES && position < maxSegmentSize,
            "Invalid position %s", position);
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
     * Reads the next binary data block
     *
     * @return The binary data, or {@code null}
     */
    @Nullable ByteBuf readNext() {
        if (position >= maxSegmentSize - HEADER_BYTES) {
            // no data expected to read due to reaching segment end
            return null;
        }
        final var entry = fileReader.read(position);
        if (entry != null) {
            // update position
            position += HEADER_BYTES + entry.readableBytes();
        }
        return entry;
    }

    /**
     * Close this reader.
     */
    void close() {
        segment.closeReader(this);
    }
}
