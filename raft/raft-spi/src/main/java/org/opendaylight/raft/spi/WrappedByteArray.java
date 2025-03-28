/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * An {@link ByteArray} backed by a single byte array.
 */
@NonNullByDefault
final class WrappedByteArray extends ByteArray {
    private final byte[] bytes;

    /**
     * Default constructor.
     *
     * @param bytes backing byte array
     */
    WrappedByteArray(final byte[] bytes) {
        this.bytes = requireNonNull(bytes);
    }

    @Override
    public int size() {
        return bytes.length;
    }

    @Override
    public List<byte[]> chunks() {
        return List.of(bytes);
    }

    @Override
    public InputStream openStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void copyTo(final DataOutput output) throws IOException {
        output.write(bytes);
    }

    @Override
    public void copyTo(final OutputStream output) throws IOException {
        output.write(bytes, 0, bytes.length);
    }
}
