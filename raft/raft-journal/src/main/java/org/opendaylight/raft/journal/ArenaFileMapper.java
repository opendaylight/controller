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
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The usual mapper using
 * <a href="https://openjdk.org/jeps/442">JEP 442: Foreign Function & Memory API (Third Preview)</a>.
 */
@NonNullByDefault
record ArenaFileMapper(
        MethodHandle arenaOfShared,
        MethodHandle arenaClose,
        MethodHandle segmentAsByteBuffer,
        MethodHandle segmentForce,
        MethodHandle fileChannelMap) implements FileMapper.Impl {
    ArenaFileMapper {
        requireNonNull(arenaOfShared);
        requireNonNull(arenaClose);
        requireNonNull(segmentAsByteBuffer);
        requireNonNull(segmentForce);
        requireNonNull(fileChannelMap);
    }

    @Override
    @SuppressWarnings({ "checkstyle:illegalCatch", "checkstyle:avoidHidingCauseException" })
    public ArenaMappedFile map(final FileChannel channel, final MapMode mode, final long offset, final int size)
            throws IOException {
        try {
            return mapImpl(channel, mode, offset, size);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } catch (Error | IOException | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("checkstyle:illegalThrows")
    private ArenaMappedFile mapImpl(final FileChannel channel, final MapMode mode, final long offset,
            final long size) throws Throwable {
        final var arena = arenaOfShared.invokeExact();
        final var segment = fileChannelMap.invokeExact(channel, mode, offset, size, arena);
        final var byfeBuffer = (ByteBuffer) segmentAsByteBuffer.invokeExact(segment);

        return new ArenaMappedFile(byfeBuffer, this, arena, segment);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    void closeArena(final Object arena) {
        try {
            arenaClose.invokeExact(arena);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    void syncSegment(final Object segment) {
        try {
            segmentForce.invokeExact(segment);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}