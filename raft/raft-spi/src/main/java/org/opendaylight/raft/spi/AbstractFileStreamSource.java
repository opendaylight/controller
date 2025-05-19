/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import com.google.common.base.MoreObjects;
import com.google.common.io.ByteStreams;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link SizedStreamSource} providing access to a slice of a regular file, similar to a {@code ByteBuffer}.
 */
@NonNullByDefault
public abstract sealed class AbstractFileStreamSource implements SizedStreamSource
        permits TransientFileStreamSource, FileStreamSource {
    private final long position;
    private final long limit;

    AbstractFileStreamSource(final long position, final long limit) {
        if (position < 0) {
            throw new IllegalArgumentException("Negative position " + limit);
        }
        if (limit < 0) {
            throw new IllegalArgumentException("Negative limit " + limit);
        }
        if (position >= limit) {
            throw new IllegalArgumentException("Position " + position + " greater than or equal to limit " + limit);
        }
        this.position = position;
        this.limit = limit;
    }

    /**
     * Returns this source's file path.
     *
     * @return this source's file path
     */
    public abstract Path file();

    /**
     * Returns the starting position.
     *
     * @return the starting position
     */
    public final long position() {
        return position;
    }

    /**
     * Returns the limit.
     *
     * @return the limit
     */
    public final long limit() {
        return limit;
    }

    @Override
    public final InputStream openStream() throws IOException {
        final var file = file();
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
    public final InputStream openBufferedStream() throws IOException {
        return new BufferedInputStream(openStream());
    }

    @Override
    public final long size() {
        return limit - position;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this)
            .add("file", file())
            .add("position", position)
            .add("limit", limit)
            .toString();
    }
}
