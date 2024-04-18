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

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import java.io.Flushable;
import java.io.IOException;

/**
 * A {@link StorageLevel#MAPPED} {@link FileWriter}.
 */
final class MappedFileWriter extends FileWriter {
    private final MappedFileReader reader;
    private final ByteBuf buffer;
    private final Flushable flush;

    MappedFileWriter(final JournalSegmentFile file, final int maxEntrySize, final ByteBuf buffer,
            final Flushable flush) {
        super(file, maxEntrySize);
        this.buffer = requireNonNull(buffer);
        this.flush = requireNonNull(flush);
        reader = new MappedFileReader(file, buffer);
    }

    @Override
    MappedFileReader reader() {
        return reader;
    }

    @Override
    void writeEmptyHeader(final int position) {
        // Note: we issue a single putLong() instead of two putInt()s.
        buffer.setLong(position, 0L);
    }

    @Override
    ByteBuf startWrite(final int position, final int size) {
        return buffer.slice(position, size);
    }

    @Override
    void commitWrite(final int position, final ByteBuf entry) {
        // No-op, buffer is write-through
    }

    @Override
    void flush() throws IOException {
        flush.flush();
    }
}
