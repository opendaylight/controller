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

import io.netty.util.internal.PlatformDependent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import org.eclipse.jdt.annotation.NonNull;

/**
 * A {@link StorageLevel#MAPPED} {@link FileWriter}.
 */
final class MappedFileWriter extends FileWriter {
    private final @NonNull MappedByteBuffer mappedBuffer;
    private final MappedFileReader reader;
    private final ByteBuffer buffer;

    private boolean needForce;

    MappedFileWriter(final JournalSegmentFile file, final int maxEntrySize) {
        super(file, maxEntrySize);

        try {
            mappedBuffer = file.map();
        } catch (IOException e) {
            throw new StorageException(e);
        }
        buffer = mappedBuffer.slice();
        reader = new MappedFileReader(file, mappedBuffer);
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
        close();
        return new DiskFileWriter(file, maxEntrySize);
    }

    @Override
    void writeEmptyHeader(final int position) {
        // Note: we issue a single putLong() instead of two putInt()s.
        buffer.putLong(position, 0L);
        needForce = true;
    }

    @Override
    ByteBuffer startWrite(final int position, final int size) {
        return buffer.slice(position, size);
    }

    @Override
    void commitWrite(final int position, final ByteBuffer entry) {
        needForce = true;
    }

    @Override
    void flush() {
        // sync in-memory data with file storage if only there is new data
        if (needForce) {
            // NB extra call for mappedBuffer.force() may cause the msync(2) exception
            needForce = false;
            mappedBuffer.force();
        }
    }

    @Override
    void close() {
        flush();
        PlatformDependent.freeDirectBuffer(mappedBuffer);
    }
}
