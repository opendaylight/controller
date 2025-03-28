/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link ByteArray} containing multiple chunks so as to be GC-friendly.
 */
// FIXME: this should be an internal implementation record of an API: sealed interfaces allows us to do so cleanly via
//        the static factory method.
// FIXME: Java 22+: we have FFM (https://openjdk.org/jeps/454): this class is obsolete with the availability of
//                  java.lang.foreign.MemorySegment, which let's us store these off-heap.
//
@NonNullByDefault
public final class ChunkedByteArray extends ByteArray {
    private final List<byte[]> chunks;
    private final int size;

    /**
     * Default constructor.
     *
     * @param size the size
     * @param chunks the chunks
     */
    ChunkedByteArray(final int size, final List<byte[]> chunks) {
        this.size = size;
        this.chunks = requireNonNull(chunks);
    }

    /**
     * Read a {@link ChunkedByteArray} from a {@link DataInput}.
     *
     * @param in the input
     * @param size size of input
     * @param chunkSize target chunk size
     * @return a {@link ChunkedByteArray}
     * @throws IOException if an I/O error occurs
     */
    public static ChunkedByteArray readFrom(final DataInput in, final int size, final int chunkSize)
            throws IOException {
        final var chunks = new ArrayList<byte[]>(requiredChunks(size, chunkSize));
        int remaining = size;
        do {
            final var buffer = new byte[Math.min(remaining, chunkSize)];
            in.readFully(buffer);
            // TODO: watch for https://openjdk.org/jeps/8261007 for possible improvement
            // FIXME: for the cost of an object (ByteBuf, ByteBuffer) we could express this quite easily: let's try
            //        to prototype how that looks
            // FIXME: once we have Java 25+,
            chunks.add(buffer);
            remaining -= buffer.length;
        } while (remaining != 0);

        return new ChunkedByteArray(size, List.copyOf(chunks));
    }

    @Override
    public InputStream openStream() {
        return new ChunkedInputStream(size, chunks.iterator());
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public List<byte[]> chunks() {
        return chunks;
    }

    @Override
    public void copyTo(final DataOutput output) throws IOException {
        for (var chunk : chunks) {
            output.write(chunk, 0, chunk.length);
        }
    }

    @Override
    public void copyTo(final OutputStream output) throws IOException {
        for (var chunk : chunks) {
            output.write(chunk);
        }
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("chunks", chunks.size());
    }

    // FIXME: is this a Math operation? :)
    private static int requiredChunks(final int size, final int chunkSize) {
        final int div = size / chunkSize;
        return size % chunkSize == 0 ? div : div + 1;
    }
}
