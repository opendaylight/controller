/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.journal;

import static java.util.Objects.requireNonNull;

import io.netty.util.internal.PlatformDependent;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import org.eclipse.jdt.annotation.NonNullByDefault;

final class MappedFileSupport {
    @NonNullByDefault
    sealed interface Impl {

        MappedFile<?> map(FileChannel channel, MapMode mode, long offset, int size) throws IOException;
    }

    @NonNullByDefault
    private record Java21() implements Impl {
        @Override
        public MappedFile21 map(final FileChannel channel, final MapMode mode, final long offset, final int size)
                throws IOException {
            return new MappedFile21(channel.map(mode, offset, size));
        }
    }

    @NonNullByDefault
    private record Java22() implements Impl {
        @Override
        public MappedFile22 map(final FileChannel channel, final MapMode mode, final long offset, final int size)
                throws IOException {
            final var arena = Arena.ofShared();
            try {
                return new MappedFile22(channel.map(mode, offset, size, arena), arena);
            } catch (IOException e) {
                arena.close();
                throw e;
            }
        }
    }

    /**
     * A {@link MappedFile} implementation for Java <22, where we operate on plain {@link ByteBuffer}.
     */
    @NonNullByDefault
    static final class MappedFile21 extends MappedFile<MappedByteBuffer> {
        private MappedFile21(final MappedByteBuffer buffer) {
            super(buffer);
        }

        @Override
        void unmap(final MappedByteBuffer buffer) {
            PlatformDependent.freeDirectBuffer(buffer);
        }

        @Override
        void sync(final MappedByteBuffer buffer) {
            buffer.force();
        }
    }

    /**
     * A {@link MappedFile} implementation for Java 22+, where we have {@link Arena} available.
     */
    @NonNullByDefault
    static final class MappedFile22 extends MappedFile<ByteBuffer> {
        private final MemorySegment segment;
        private final Arena arena;

        private MappedFile22(final MemorySegment segment, final Arena arena) {
            super(segment.asByteBuffer());
            this.segment = requireNonNull(segment);
            this.arena = requireNonNull(arena);
        }

        @Override
        void unmap(final ByteBuffer buffer) {
            arena.close();
        }

        @Override
        void sync(final ByteBuffer buffer) {
            segment.force();
        }
    }

    private static final Impl IMPL = new Java22();

    private MappedFileSupport() {
        // Hidden on purpose
    }

    @NonNullByDefault
    static MappedFile<?> map(final FileChannel channel, final MapMode mode, final long offset, final int size)
            throws IOException {
        return IMPL.map(channel, mode, offset, size);
    }
}
