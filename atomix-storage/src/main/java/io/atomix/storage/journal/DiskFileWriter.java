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
import static io.atomix.storage.journal.SegmentEntry.HEADER_BYTES;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * A {@link StorageLevel#DISK} {@link FileWriter}.
 */
final class DiskFileWriter implements FileWriter {
    private static final ByteBuffer ZERO_ENTRY_HEADER = ByteBuffer.wrap(new byte[HEADER_BYTES]);

    private final FileChannel channel;
    private final DiskFileReader reader;
    private final ByteBuffer buffer;

    DiskFileWriter(final FileChannel channel, final int bufferSize) {
        this.channel = channel;
        this.buffer = ByteBuffer.allocate(bufferSize);
        reader = new DiskFileReader(channel, buffer);
    }

    @Override
    public FileReader reader() {
        return reader;
    }

    @Override
    public void writeEmptyHeader(final int position) {
        try {
            channel.write(ZERO_ENTRY_HEADER.asReadOnlyBuffer(), position);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int append(final int position, final ByteBuf entry) {
        final var writeBuf = Unpooled.wrappedBuffer(buffer.clear().slice());
        final var appended = appendBuf(writeBuf, entry, 0);
        if (appended > 0) {
            try {
                channel.write(buffer.slice(0, appended), position);
            } catch (IOException e) {
                throw new StorageException(e);
            }
        }
        return appended;
    }

    @Override
    public void flush() {
        if (channel.isOpen()) {
            try {
                channel.force(true);
            } catch (IOException e) {
                throw new StorageException(e);
            }
        }
    }

    @Override
    public void close() {
        flush();
    }
}
