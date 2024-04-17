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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNull;

/**
 * A {@link StorageLevel#MAPPED} {@link FileWriter}.
 */
final class MappedFileWriter extends FileWriter {
    private final @NonNull MappedByteBuffer mappedBuffer;
    private final MappedFileReader reader;
    private final ByteBuffer buffer;
    private volatile boolean updated;

    MappedFileWriter(final Path path, final FileChannel channel, final MappedByteBuffer mappedBuffer,
        final int maxSegmentSize, final int maxEntrySize) {
        super(path, channel, maxSegmentSize, maxEntrySize);

        this.mappedBuffer = mappedBuffer;
        buffer = mappedBuffer.slice();
        reader = new MappedFileReader(path, mappedBuffer);
    }

    @Override
    MappedFileReader reader() {
        return reader;
    }

    @Override
    void writeEmptyHeader(final int position) {
        // Note: we issue a single putLong() instead of two putInt()s.
        buffer.putLong(position, 0L);
        updated = true;
    }

    @Override
    ByteBuffer startWrite(final int position, final int size) {
        return buffer.slice(position, size);
    }

    @Override
    void commitWrite(final int position, final ByteBuffer entry) {
        // indicate the data requires memory to file system sync
        updated = true;
    }

    @Override
    void flush() {
        if (updated) {
            // sync in-memory data with file storage if only there is new data
            mappedBuffer.force();
            updated = false;
        }
    }

    @Override
    void close() {
        flush();
    }
}
