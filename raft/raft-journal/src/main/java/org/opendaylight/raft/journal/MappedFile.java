/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.journal;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
record MappedFile(ByteBuffer buffer, Arena arena, MemorySegment segment) {
    MappedFile {
        requireNonNull(buffer);
        requireNonNull(arena);
        requireNonNull(segment);
    }

    static MappedFile of(final FileChannel channel, final MapMode mode, final long offset, final int size)
            throws IOException {
        final var arena = Arena.ofShared();
        final MemorySegment segment;
        try {
            segment = channel.map(mode, offset, size, arena);
        } catch (IOException e) {
            arena.close();
            throw e;
        }
        return new MappedFile(segment.asByteBuffer(), arena, segment);
    }

    void sync() {
        segment.force();
    }

    void unmap() {
        arena.close();
    }
}
