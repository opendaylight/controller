/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.journal;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
final class MappedFile {
    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(MappedFile.class, "buffer", ByteBuffer.class);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Arena arena;
    private final MemorySegment segment;

    @SuppressFBWarnings(value = "", justification = "")
    private volatile ByteBuffer buffer;

    MappedFile(final Arena arena, final MemorySegment segment) {
        this.arena = requireNonNull(arena);
        this.segment = requireNonNull(segment);
        buffer = segment.asByteBuffer();
    }

    /**
     * {@return the mapped {@link ByteBuffer}}
     */
    ByteBuffer buffer() {
        return verifyNotNull((ByteBuffer) VH.get(this));
    }

    /**
     * Synchronize the mapping.
     *
     * @throws UncheckedIOExcpetion if an I/O error occurs
     */
    void sync() {
        VH.getVolatile(this);
        segment.force();
    }

    /**
     * Unmap the this object.
     */
    void unmap() {
        final var prev = VH.getAndSet(this, null);
        if (prev != null) {
            arena.close();
        }
    }
}
