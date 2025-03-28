/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import com.google.common.base.MoreObjects;
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
 * The moral equivalent of a {@code byte[]}, but perhaps allocated in chunks so as to be GC-friendly.
 */
@NonNullByDefault
public abstract sealed class ByteArray implements SizedDataSource permits ChunkedByteArray, WrappedByteArray {
    private final int size;

    ByteArray(final int size) {
        this.size = size;
    }

    @Override
    public final int size() {
        return size;
    }

    @Override
    public abstract InputStream openStream();

    /**
     * Copy this array into specified {@link DataOutput}.
     *
     * @param output the data output
     * @throws IOException if an I/O error occurs
     */
    public abstract void copyTo(DataOutput output) throws IOException;

    /**
     * Copy this array into specified {@link DataOutput}.
     *
     * @param output the data output
     * @throws IOException if an I/O error occurs
     */
    public abstract void copyTo(OutputStream output) throws IOException;

    /**
     * Returns the list of chunks.
     *
     * @return the list of chunks
     */
    public abstract List<byte[]> chunks();

    /**
     * Returns an empty {@link ByteArray}.
     *
     * @return an empty {@link ByteArray}
     */
    public static final ByteArray empty() {
        return WrappedByteArray.EMPTY;
    }

    /**
     * Wrap a {@code byte[]} in a {@link ByteArray}.
     *
     * @param bytes bytes to wrap
     * @return a {@link ByteArray}
     */
    public static final ByteArray wrap(final byte[] bytes) {
        return WrappedByteArray.of(bytes);
    }

    /**
     * Read a {@link ByteArray} from a {@link DataInput}.
     *
     * @param in the input
     * @param size size of input
     * @param chunkSize target chunk size
     * @return a {@link ByteArray}
     * @throws IOException if an I/O error occurs
     */
    public static ByteArray readFrom(final DataInput in, final int size, final int chunkSize)
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

        return chunks.size() == 1 ? ByteArray.wrap(chunks.getFirst()) : new ChunkedByteArray(size, List.copyOf(chunks));
    }

    // FIXME: is this a Math operation? :)
    private static int requiredChunks(final int size, final int chunkSize) {
        final int div = size / chunkSize;
        return size % chunkSize == 0 ? div : div + 1;
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("size", size);
    }
}
