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
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An abstraction over how to write a {@link JournalSegmentFile}.
 */
abstract sealed class FileWriter permits DiskFileWriter, MappedFileWriter {
    final Path path;
    final FileChannel channel;
    final int maxSegmentSize;
    final int maxEntrySize;

    FileWriter(final Path path, final FileChannel channel, final int maxSegmentSize, final int maxEntrySize) {
        this.path = requireNonNull(path);
        this.channel = requireNonNull(channel);
        this.maxSegmentSize = maxSegmentSize;
        this.maxEntrySize = maxEntrySize;
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

    abstract ByteBuffer startWrite(int position, int size);

    abstract void commitWrite(int position, ByteBuffer entry);

    /**
     * Flushes written entries to disk.
     */
    abstract void flush();

    /**
     * Closes this writer.
     */
    abstract void close();

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("path", path).toString();
    }

    abstract @Nullable MappedByteBuffer buffer();

    abstract @Nullable MappedFileWriter toMapped();

    abstract @Nullable DiskFileWriter toDisk();
}
