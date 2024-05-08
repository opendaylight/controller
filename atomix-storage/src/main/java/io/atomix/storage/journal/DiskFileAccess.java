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
import io.netty.buffer.ByteBufAllocator;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link FileAccess} for {@link StorageLevel#DISK} and {@link StorageLevel#DIRECT}.
 */
@NonNullByDefault
final class DiskFileAccess extends FileAccess {
    /**
     * Lambda helper for buffer allocator method, such as {@link ByteBufAllocator#heapBuffer(int, int)}.
     */
    @FunctionalInterface
    private interface BufferSupplier {
        /**
         * Invoke the method.
         *
         * @param allocator allocator to invoke on
         * @param initialCapacity initial capacity
         * @param maxCapacity maximum capacity
         * @return A {@link ByteBuf}
         */
        ByteBuf getBuffer(ByteBufAllocator allocator, int initialCapacity, int maxCapacity);

        /**
         * Convenience method to allocate a fixed buffer.
         *
         * @param allocator allocator to invoke on
         * @param size initial and maximum capacity
         * @return A {@link ByteBuf}
         */
        default ByteBuf getFixedBuffer(final ByteBufAllocator allocator, final int size) {
            return getBuffer(allocator, size, size);
        }
    }

    /**
     * Just do not bother with IO smaller than this many bytes.
     */
    private static final int MIN_IO_SIZE = 8192;

    private final BufferSupplier bufferSupplier;

    private DiskFileAccess(final JournalSegmentFile file, final int maxEntrySize, final BufferSupplier method) {
        super(file, maxEntrySize);
        this.bufferSupplier = requireNonNull(method);
    }

    static DiskFileAccess direct(final JournalSegmentFile file, final int maxEntrySize) {
        return new DiskFileAccess(file, maxEntrySize, ByteBufAllocator::directBuffer);
    }

    static DiskFileAccess heap(final JournalSegmentFile file, final int maxEntrySize) {
        return new DiskFileAccess(file, maxEntrySize, ByteBufAllocator::heapBuffer);
    }

    @Override
    DiskFileReader newFileReader() {
        return new DiskFileReader(file, allocateBuffer(file, maxEntrySize));
    }

    @Override
    DiskFileWriter newFileWriter() {
        return new DiskFileWriter(file, maxEntrySize, allocateBuffer(file, maxEntrySize));
    }

    @Override
    public void close() {
        // No-op
    }

    private ByteBuf allocateBuffer(final JournalSegmentFile file, final int maxEntrySize) {
        return bufferSupplier.getFixedBuffer(file.allocator(), chooseBufferSize(maxEntrySize, file.maxSize()));
    }

    private static int chooseBufferSize(final int maxEntrySize, final int maxSegmentSize) {
        if (maxSegmentSize <= MIN_IO_SIZE) {
            // just buffer the entire segment
            return maxSegmentSize;
        }

        // one full entry plus its header, or MIN_IO_SIZE, which benefits the read of many small entries
        final int minBufferSize = maxEntrySize + SegmentEntry.HEADER_BYTES;
        return minBufferSize <= MIN_IO_SIZE ? MIN_IO_SIZE : minBufferSize;
    }
}
