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
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link FileAccess} for {@link StorageLevel#DISK}.
 */
@NonNullByDefault
final class DiskFileAccess extends FileAccess {
    /**
     * Just do not bother with IO smaller than this many bytes.
     */
    private static final int MIN_IO_SIZE = 8192;

    DiskFileAccess(final JournalSegmentFile file, final int maxEntrySize) {
        super(file, maxEntrySize);
    }

    @Override
    DiskFileReader newFileReader() {
        return new DiskFileReader(file, allocateBuffer(maxEntrySize, file.maxSize()));
    }

    @Override
    DiskFileWriter newFileWriter() {
        return new DiskFileWriter(file, maxEntrySize, allocateBuffer(maxEntrySize, file.maxSize()));
    }

    @Override
    public void close() {
        // No-op
    }

    private static ByteBuffer allocateBuffer(final int maxEntrySize, final int maxSegmentSize) {
        return ByteBuffer.allocate(chooseBufferSize(maxEntrySize, maxSegmentSize));
    }

    private static int chooseBufferSize(final int maxEntrySize, final int maxSegmentSize) {
        if (maxSegmentSize <= MIN_IO_SIZE) {
            // just buffer the entire segment
            return maxSegmentSize;
        }

        // one full entry plus its header, or MIN_IO_SIZE, which benefits the read of many small entries
        final int minBufferSize = maxEntrySize + SegmentEntry.HEADER_BYTES;
        return minBufferSize <= MIN_IO_SIZE ? MIN_IO_SIZE : minBufferSize;
    }
}
