/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.journal;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link MappedFile} implementation using reflection where we have {@code Arena} available.
 */
@NonNullByDefault
final class ArenaMappedFile extends MappedFile<ByteBuffer> {
    private final ArenaFileMapper impl;
    private final Object arena;
    private final Object segment;

    ArenaMappedFile(final ByteBuffer byteBuffer, final ArenaFileMapper impl, final Object arena, final Object segment) {
        super(byteBuffer);
        this.impl = requireNonNull(impl);
        this.arena = requireNonNull(arena);
        this.segment = requireNonNull(segment);
    }

    @Override
    void sync(final ByteBuffer buffer) {
        impl.syncSegment(segment);
    }

    @Override
    void unmap(final ByteBuffer buffer) {
        impl.closeArena(arena);
    }
}