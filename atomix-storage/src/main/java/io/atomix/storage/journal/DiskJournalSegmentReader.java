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

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Log segment reader.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class DiskJournalSegmentReader<E> extends JournalSegmentReader<E> {
    private final FileChannel channel;
    private final ByteBuffer buffer;

    // tracks where memory's first available byte maps to in terms of FileChannel.position()
    private int bufferPosition;

    DiskJournalSegmentReader(final FileChannel channel, final JournalSegment<E> segment, final int maxEntrySize,
            final JournalSerdes namespace) {
        super(segment, maxEntrySize, namespace);
        this.channel = requireNonNull(channel);
        buffer = ByteBuffer.allocate((maxEntrySize + SegmentEntry.HEADER_BYTES) * 2).flip();
        bufferPosition = 0;
    }

    @Override void invalidateCache() {
        buffer.clear().flip();
        bufferPosition = 0;
    }

    @Override ByteBuffer read(final int position, final int size) {
        // calculate logical seek distance between buffer's first byte and position and split flow between
        // forward-moving and backwards-moving code paths.
        final int seek = bufferPosition - position;
        return seek >= 0 ? forwardAndRead(seek, position, size) : rewindAndRead(-seek, position, size);
    }

    private ByteBuffer forwardAndRead(final int seek, final int position, final int size) {
        final int missing = buffer.limit() - seek - size;
        if (missing <= 0) {
            // fast path: we have the requested region
            // FIXME: CONTROLLER-2109: .asReadOnlyBuffer()
            return buffer.slice(seek, size);
        }

        // We need to read more data, but let's salvage what we can:
        // - set buffer position to seek, which means it points to the same as position
        // - run compact, which moves everything between position and limit onto the beginning of buffer and
        //   sets it up to receive more bytes
        // - start the read accounting for the seek
        buffer.position(seek).compact();
        readAtLeast(position + seek, missing);
        return setAndSlice(position, size);
    }

    ByteBuffer rewindAndRead(final int rewindBy, final int position, final int size) {
        // TODO: Lazy solution. To be super crisp, we want to find out how much of the buffer we can salvage and
        //       do all the limit/position fiddling before and after read. Right now let's just flow the buffer up and
        //       read it.
        buffer.clear();
        readAtLeast(position, size);
        return setAndSlice(position, size);
    }

    void readAtLeast(final int readPosition, final int readAtLeast) {
        final int bytesRead;
        try {
            bytesRead = channel.read(buffer, readPosition);
        } catch (IOException e) {
            throw new StorageException(e);
        }
        verify(bytesRead >= readAtLeast, "Short read %s, expected %s", bytesRead, readAtLeast);
        buffer.flip();
    }

    private ByteBuffer setAndSlice(final int position, final int size) {
        bufferPosition = position;
        // FIXME: CONTROLLER-2109: .asReadOnlyBuffer()
        return buffer.slice(0, size);
    }
}
