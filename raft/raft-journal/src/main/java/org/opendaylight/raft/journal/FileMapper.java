/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.journal;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * File mapper implementation. This class acts as a runtime-constant indirection to a backing implementation.
 */
@NonNullByDefault
final class FileMapper {
    private FileMapper() {
        // Hidden on purpose
    }

    static MappedFile map(final FileChannel channel, final MapMode mode, final long offset, final int size)
            throws IOException {
        final var arena = Arena.ofShared();
        try {
            return new MappedFile(arena, channel.map(mode, offset, size, arena));
        } catch (IOException e) {
            arena.close();
            throw e;
        }
    }
}
