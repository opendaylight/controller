/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.io;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSink;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.concepts.Immutable;

@Beta
@NonNullByDefault
public final class ChunkedByteArray implements Immutable {
    private final ImmutableList<byte[]> chunks;
    private final int size;

    ChunkedByteArray(final int size, final ImmutableList<byte[]> chunks) {
        this.size = size;
        this.chunks = requireNonNull(chunks);
    }

    public static ChunkedByteArray readFrom(final ObjectInput in, final int size, final int chunkSize)
            throws IOException {
        final List<byte[]> chunks = new ArrayList<>(requiredChunks(size, chunkSize));
        int remaining = size;
        do {
            final byte[] buffer = new byte[Math.min(remaining, chunkSize)];
            in.readFully(buffer);
            chunks.add(buffer);
            remaining -= buffer.length;
        } while (remaining != 0);

        return new ChunkedByteArray(size, ImmutableList.copyOf(chunks));
    }

    public int size() {
        return size;
    }

    public InputStream openStream() {
        return new ChunkedInputStream(size, chunks.iterator());
    }

    public void copyTo(final DataOutput output) throws IOException {
        for (byte[] chunk : chunks) {
            output.write(chunk, 0, chunk.length);
        }
    }

    public void copyTo(final ByteSink output) throws IOException {
        for (byte[] chunk : chunks) {
            output.write(chunk);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("size", size).add("chunkCount", chunks.size()).toString();
    }

    @VisibleForTesting
    ImmutableList<byte[]> getChunks() {
        return chunks;
    }

    private static int requiredChunks(final int size, final int chunkSize) {
        final int div = size / chunkSize;
        return size % chunkSize == 0 ? div : div + 1;
    }
}
