/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link SizedStreamSource} providing access to a slice of a regular file, similar to a {@code ByteBuffer}.
 *
 * @param file backing file
 * @param position position of first byte
 * @param limit position of next-to-last byte
 */
@NonNullByDefault
public record FileStreamSource(Path file, long position, long limit) implements SizedStreamSource {
    /**
     * Default constructor.
     *
     * @param file backing file
     * @param position position of first byte
     * @param limit position of next-to-last byte
     */
    public FileStreamSource {
        requireNonNull(file);
        if (position < 0) {
            throw new IllegalArgumentException("Negative position " + limit);
        }
        if (limit < 0) {
            throw new IllegalArgumentException("Negative limit " + limit);
        }
        if (position > limit) {
            throw new IllegalArgumentException("Position " + position + " greater than limit " + limit);
        }
    }

    @Override
    public InputStream openStream() throws IOException {
        final var size = Files.size(file);
        if (size < limit) {
            throw new IOException(
                "Incomplete file: expected at least " + limit + " bytes, only " + size + " available in " + file);
        }

        final var is = ByteStreams.limit(
            Files.newInputStream(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS),
            limit);
        is.skipNBytes(position);
        return is;
    }

    @Override
    public long size() {
        return limit - position;
    }
}
