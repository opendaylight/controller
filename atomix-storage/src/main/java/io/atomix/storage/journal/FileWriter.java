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

import com.google.common.base.MoreObjects;
import io.netty.buffer.ByteBuf;
import java.io.IOException;

/**
 * An abstraction over how to write a {@link JournalSegmentFile}.
 */
abstract sealed class FileWriter permits DiskFileWriter, MappedFileWriter {
    private final JournalSegmentFile file;
    private final int maxEntrySize;

    FileWriter(final JournalSegmentFile file, final int maxEntrySize) {
        this.file = requireNonNull(file);
        this.maxEntrySize = maxEntrySize;
    }

    final JournalSegmentFile file() {
        return file;
    }

    final int maxEntrySize() {
        return maxEntrySize;
    }

    /**
     * Return the internal {@link FileReader}.
     *
     * @return the internal FileReader
     */
    abstract FileReader reader();

    /**
     * Write {@link SegmentEntry#HEADER_BYTES} worth of zeroes at specified position.
     *
     * @param position position to write to
     */
    abstract void writeEmptyHeader(int position);

    /**
     * Allocate file space. Note that the allocated space may be a buffer disconnected from the file. Any modifications
     * to the returned buffer need to be committed via {@link #commitWrite(int, ByteBuf)}.
     *
     * @param position position to start from
     * @param size the size to allocate
     * @return A {@link ByteBuf} covering the allocated area
     */
    abstract ByteBuf startWrite(int position, int size);

    abstract void commitWrite(int position, ByteBuf entry);

    /**
     * Flushes written entries to disk.
     */
    abstract void flush() throws IOException;

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("path", file.path()).toString();
    }
}
