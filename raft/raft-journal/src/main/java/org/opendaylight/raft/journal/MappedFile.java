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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
abstract sealed class MappedFile<T extends ByteBuffer>
        permits MappedFileSupport.MappedFile21, MappedFileSupport.MappedFile22 {
    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(MappedFile.class, "buffer", ByteBuffer.class);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private volatile T buffer;

    MappedFile(final T buffer) {
        this.buffer = requireNonNull(buffer);
    }

    /**
     * {@return the mapped {@link ByteBuffer}}
     */
    final T buffer() {
        return verifyNotNull((T) VH.getAcquire(this));
    }

    /**
     * Synchronize the mapping.
     *
     * @throws UncheckedIOExcpetion if an I/O error occurs
     */
    final void sync() {
        sync(buffer());
    }

    abstract void sync(T buffer);

    /**
     * Unmap the this object.
     */
    final void unmap() {
        final var prev = (T) VH.getAndSet(this, null);
        if (prev != null) {
            unmap(prev);
        }
    }

    /**
     * Unmap the now-unreachanable buffer. This method is guaranteed to be invoked at most once.
     *
     * @param buffer the buffer
     */
    abstract void unmap(T buffer);
}
