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

import static io.atomix.storage.journal.SegmentEntry.HEADER_BYTES;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * A {@link StorageLevel#DISK} {@link FileWriter}.
 */
final class DiskFileWriter extends FileWriter {
    private static final ByteBuffer ZERO_ENTRY_HEADER = ByteBuffer.wrap(new byte[HEADER_BYTES]);

    private final DiskFileReader reader;
    private final FileChannel channel;
    private final ByteBuffer buffer;

    DiskFileWriter(final JournalSegmentFile file, final int maxEntrySize, final ByteBuffer buffer) {
        super(file, maxEntrySize);
        this.buffer = requireNonNull(buffer);
        channel = file.channel();
        reader = new DiskFileReader(file, buffer);
    }

    @Override
    DiskFileReader reader() {
        return reader;
    }

    @Override
    void writeEmptyHeader(final int position) {
        try {
            channel.write(ZERO_ENTRY_HEADER.asReadOnlyBuffer(), position);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    ByteBuffer startWrite(final int position, final int size) {
        return buffer.clear().slice(0, size);
    }

    @Override
    void commitWrite(final int position, final ByteBuffer entry) {
        try {
            channel.write(entry, position);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    void flush() {
        if (channel.isOpen()) {
            try {
                channel.force(true);
            } catch (IOException e) {
                throw new StorageException(e);
            }
        }
    }
}
