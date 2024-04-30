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

import static io.atomix.storage.journal.BufUtils.appendBuf;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNull;

/**
 * A {@link StorageLevel#MAPPED} {@link FileWriter}.
 */
final class MappedFileWriter implements FileWriter {
    private final @NonNull MappedByteBuf mappedByteBuf;
    private final MappedFileReader reader;
    private volatile boolean updated;

    MappedFileWriter(final MappedByteBuf mappedByteBuf) {
        this.mappedByteBuf = mappedByteBuf;
        reader = new MappedFileReader(mappedByteBuf);
    }

    @Override
    public MappedFileReader reader() {
        return reader;
    }

    @Override
    public void writeEmptyHeader(final int position) {
        // Note: we issue a single putLong() instead of two putInt()s.
        mappedByteBuf.setLong(position, 0L);
        updated = true;
    }

    @Override
    public int append(final int position, final ByteBuf entry) {
        final var appended = appendBuf(mappedByteBuf, entry, position);
        updated = appended > 0;
        return appended;
    }

    @Override
    public void flush() {
        if (updated) {
            // sync in-memory data with file storage if only there is new data
            // NB extra call for mappedBuffer.force() may cause the msync exception
            mappedByteBuf.force();
            updated = false;
        }
    }

    @Override
    public void close() {
        flush();
    }
}
