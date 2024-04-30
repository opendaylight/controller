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

import io.netty.buffer.ByteBuf;

/**
 * An abstraction over how to write a {@link JournalSegmentFile}.
 */
sealed interface FileWriter permits DiskFileWriter, MappedFileWriter {

    /**
     * Return the internal {@link FileReader}.
     *
     * @return the internal FileReader
     */
    FileReader reader();

    /**
     * Write {@link SegmentEntry#HEADER_BYTES} worth of zeroes at specified position.
     *
     * @param position position to write to
     */
    void writeEmptyHeader(int position);

    /**
     * Writes entry to specified position. Operation performs writing to entry header
     * including data length and checksum. Returns number of bytes written.
     *
     * @param position position to the entry header
     * @param entry entry content as {@link ByteBuf}
     * @return total number of bytes written incl header bytes
     */
    int append(int position, ByteBuf entry);

    /**
     * Flushes written entries to disk.
     */
    void flush();

    /**
     * Closes this writer.
     */
    void close();
}
