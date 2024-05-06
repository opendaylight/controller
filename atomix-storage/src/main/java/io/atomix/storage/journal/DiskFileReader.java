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

import static com.google.common.base.Verify.verify;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.eclipse.jdt.annotation.NonNull;

/**
 * A {@link StorageLevel#DISK} implementation of {@link FileReader}. Maintains an internal buffer.
 */
final class DiskFileReader extends FileReader {
    private final FileChannel channel;
    private final ByteBuffer buffer;

    // tracks where memory's first available byte maps to in terms of FileChannel.position()
    private int bufferPosition;

    DiskFileReader(final JournalSegmentFile file, final int maxEntrySize) {
        this(file, file.allocateBuffer(maxEntrySize));
    }

    // Note: take ownership of the buffer
    DiskFileReader(final JournalSegmentFile file, final ByteBuffer buffer) {
        super(file);
        channel = file.channel();
        this.buffer = buffer.flip();
        bufferPosition = 0;
    }

    @Override
    void invalidateCache() {
        buffer.clear().flip();
        bufferPosition = 0;
    }

    @Override
    ByteBuffer read(final int position, final int size) {
        // calculate logical seek distance between buffer's first byte and position and split flow between
        // forward-moving and backwards-moving code paths.
        final int seek = bufferPosition - position;
        return seek >= 0 ? forwardAndRead(seek, position, size) : rewindAndRead(-seek, position, size);
    }

    private @NonNull ByteBuffer forwardAndRead(final int seek, final int position, final int size) {
        final int missing = buffer.limit() - seek - size;
        if (missing <= 0) {
            // fast path: we have the requested region
            return buffer.slice(seek, size).asReadOnlyBuffer();
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

    private @NonNull ByteBuffer rewindAndRead(final int rewindBy, final int position, final int size) {
        // TODO: Lazy solution. To be super crisp, we want to find out how much of the buffer we can salvage and
        //       do all the limit/position fiddling before and after read. Right now let's just flow the buffer up and
        //       read it.
        buffer.clear();
        readAtLeast(position, size);
        return setAndSlice(position, size);
    }

    private void readAtLeast(final int readPosition, final int readAtLeast) {
        final int bytesRead;
        try {
            bytesRead = channel.read(buffer, readPosition);
        } catch (IOException e) {
            throw new StorageException(e);
        }
        verify(bytesRead >= readAtLeast, "Short read %s, expected %s", bytesRead, readAtLeast);
        buffer.flip();
    }

    private @NonNull ByteBuffer setAndSlice(final int position, final int size) {
        bufferPosition = position;
        return buffer.slice(0, size).asReadOnlyBuffer();
    }
}
