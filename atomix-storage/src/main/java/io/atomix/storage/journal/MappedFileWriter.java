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

import java.io.IOException;
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

    MappedFileWriter(final Path path, final FileChannel channel, final int maxSegmentSize, final int maxEntrySize) {
        super(path, channel, maxSegmentSize, maxEntrySize);

        mappedBuffer = mapBuffer(channel, maxSegmentSize);
        buffer = mappedBuffer.slice();
        reader = new MappedFileReader(path, mappedBuffer);
    }

    private static @NonNull MappedByteBuffer mapBuffer(final FileChannel channel, final int maxSegmentSize) {
        try {
            return channel.map(FileChannel.MapMode.READ_WRITE, 0, maxSegmentSize);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    MappedFileReader reader() {
        return reader;
    }

    @Override
    MappedByteBuffer buffer() {
        return mappedBuffer;
    }

    @Override
    MappedFileWriter toMapped() {
        return null;
    }

    @Override
    DiskFileWriter toDisk() {
        return new DiskFileWriter(path, channel, maxSegmentSize, maxEntrySize);
    }

    @Override
    void writeEmptyHeader(final int position) {
        // Note: we issue a single putLong() instead of two putInt()s.
        buffer.putLong(position, 0L);
    }

    @Override
    ByteBuffer startWrite(final int position, final int size) {
        return buffer.slice(position, size);
    }

    @Override
    void commitWrite(final int position, final ByteBuffer entry) {
        // No-op, buffer is write-through
    }

    @Override
    void flush() {
        mappedBuffer.force();
    }

    @Override
    void close() {
        flush();
        try {
            BufferCleaner.freeBuffer(mappedBuffer);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }
}
