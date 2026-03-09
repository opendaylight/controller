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
record ArenaMappedFile(ByteBuffer buffer, ArenaFileMapper impl, Object arena, Object segment) implements MappedFile {
    ArenaMappedFile {
        requireNonNull(buffer);
        requireNonNull(impl);
        requireNonNull(arena);
        requireNonNull(segment);
    }

    @Override
    public void sync() {
        impl.syncSegment(segment);
    }

    @Override
    public void unmap() {
        impl.closeArena(arena);
    }
}