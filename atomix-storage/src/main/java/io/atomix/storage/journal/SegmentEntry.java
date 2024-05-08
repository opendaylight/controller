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

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import org.eclipse.jdt.annotation.NonNull;

/**
 * An {@link Indexed} entry read from {@link JournalSegment}.
 *
 * @param checksum The {@link CRC32} checksum of data
 * @param bytes Entry bytes
 */
record SegmentEntry(int checksum, @NonNull ByteBuffer bytes) {
    /**
     * The size of the header. It is comprised of
     * <ul>
     *   <li>32-bit signed entry length</li>
     *   <li>32-bit unsigned CRC32 checksum</li>
     * </ul>
     */
    static final int HEADER_BYTES = Integer.BYTES + Integer.BYTES;

    SegmentEntry {
        if (bytes.remaining() < 1) {
            throw new IllegalArgumentException("Invalid entry bytes " + bytes);
        }
    }

    /**
     * Compute the {@link CRC32} checksum of a buffer. Note that the buffer will be consumed during this process.
     *
     * @param bytes buffer to checksum
     * @return the checksum
     */
    static int computeChecksum(final ByteBuffer bytes) {
        final var crc32 = new CRC32();
        crc32.update(bytes);
        return (int) crc32.getValue();
    }
}
