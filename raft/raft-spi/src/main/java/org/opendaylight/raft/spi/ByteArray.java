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
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The moral equivalent of a {@code byte[]}, but perhaps allocated in chunks so as to be GC-friendly.
 */
@NonNullByDefault
public abstract sealed class ByteArray implements InputStreamProvider permits ChunkedByteArray, WrappedByteArray {
    ByteArray() {
        // Hidden on purpose
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
     * Returns the size of this array.
     *
     * @return the size of this array
     */
    public abstract int size();

    /**
     * Returns the list of chunks.
     *
     * @return the list of chunks
     */
    public abstract List<byte[]> chunks();

    /**
     * Wrap a {@code byte[]} in a {@link ByteArray}.
     *
     * @param bytes bytes to wrap
     * @return a {@link ByteArray}
     */
    public static final ByteArray wrap(final byte[] bytes) {
        return new WrappedByteArray(bytes);
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("size", size());
    }
}
