/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
 * Copyright (c) 2024 PANTHEON.tech, s.r.o.
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

import io.atomix.storage.journal.index.JournalIndex;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Segment writer.
 * <p>
 * The format of an entry in the log is as follows:
 * <ul>
 * <li>64-bit index</li>
 * <li>8-bit boolean indicating whether a term change is contained in the entry</li>
 * <li>64-bit optional term</li>
 * <li>32-bit signed entry length, including the entry type ID</li>
 * <li>8-bit signed entry type ID</li>
 * <li>n-bit entry bytes</li>
 * </ul>
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class DiskJournalSegmentWriter<E> extends JournalSegmentWriter<E> {
    private static final ByteBuffer ZERO_ENTRY_HEADER = ByteBuffer.wrap(new byte[HEADER_BYTES]);

    private final JournalSegmentReader<E> reader;
    private final ByteBuffer buffer;

    DiskJournalSegmentWriter(final FileChannel channel, final JournalSegment<E> segment, final int maxEntrySize,
        final JournalIndex index, final JournalSerdes namespace) {
        super(channel, segment, maxEntrySize, index, namespace);

        buffer = DiskFileReader.allocateBuffer(maxSegmentSize, maxEntrySize);
        reader = new JournalSegmentReader<>(segment,
            new DiskFileReader(segment.file().file().toPath(), channel, maxSegmentSize, maxEntrySize), maxEntrySize,
            namespace);
        reset(0);
    }

    DiskJournalSegmentWriter(final JournalSegmentWriter<E> previous) {
        super(previous);

        buffer = DiskFileReader.allocateBuffer(maxSegmentSize, maxEntrySize);
        reader = new JournalSegmentReader<>(segment,
            new DiskFileReader(segment.file().file().toPath(), channel, maxSegmentSize, maxEntrySize), maxEntrySize,
            namespace);
    }

    @Override
    MappedByteBuffer buffer() {
        return null;
    }

    @Override
    MappedJournalSegmentWriter<E> toMapped() {
        return new MappedJournalSegmentWriter<>(this);
    }

    @Override
    DiskJournalSegmentWriter<E> toFileChannel() {
        return this;
    }

    @Override
    JournalSegmentReader<E> reader() {
        return reader;
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
    void writeEmptyHeader(final int position) {
        try {
            channel.write(ZERO_ENTRY_HEADER.asReadOnlyBuffer(), position);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    void flush() {
        try {
            if (channel.isOpen()) {
                channel.force(true);
            }
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    void close() {
        flush();
    }
}
