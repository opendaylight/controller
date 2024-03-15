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

import io.atomix.storage.journal.index.JournalIndex;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.eclipse.jdt.annotation.NonNull;

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
final class MappedJournalSegmentWriter<E> extends JournalSegmentWriter<E> {
    private final @NonNull MappedByteBuffer mappedBuffer;
    private final JournalSegmentReader<E> reader;
    private final ByteBuffer buffer;

    MappedJournalSegmentWriter(final FileChannel channel, final JournalSegment<E> segment, final int maxEntrySize,
        final JournalIndex index, final JournalSerdes namespace, final int maxSegmentSize) {
        super(channel, segment, maxEntrySize, index, namespace, maxSegmentSize);

        mappedBuffer = mapBuffer(channel, maxSegmentSize);
        buffer = mappedBuffer.slice();
        reader = new JournalSegmentReader<>(segment, new MappedFileReader(segment.file().file().toPath(), mappedBuffer),
            maxEntrySize, namespace, maxSegmentSize);
        reset(0);
    }

    MappedJournalSegmentWriter(final JournalSegmentWriter<E> previous) {
        super(previous);

        mappedBuffer = mapBuffer(channel, maxSegmentSize);
        buffer = mappedBuffer.slice();
        reader = new JournalSegmentReader<>(segment, new MappedFileReader(segment.file().file().toPath(), mappedBuffer),
            maxEntrySize, namespace, maxSegmentSize);
    }

    private static @NonNull MappedByteBuffer mapBuffer(final FileChannel channel, final int maxSegmentSize) {
        try {
            return channel.map(FileChannel.MapMode.READ_WRITE, 0, maxSegmentSize);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    @NonNull MappedByteBuffer buffer() {
        return mappedBuffer;
    }

    @Override
    MappedJournalSegmentWriter<E> toMapped() {
        return this;
    }

    @Override
    DiskJournalSegmentWriter<E> toFileChannel() {
        close();
        return new DiskJournalSegmentWriter<>(this);
    }

    @Override
    JournalSegmentReader<E> reader() {
        return reader;
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
    void writeEmptyHeader(final int position) {
        // Note: we issue a single putLong() instead of two putInt()s.
        buffer.putLong(position, 0L);
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
