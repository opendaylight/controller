/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * An {@link UnsizedStreamSource} performing transparent LZ4 decompression of a backing {@link StreamSource}.
 */
@NonNullByDefault
record UnsizedDecompressLz4(StreamSource compressed) implements UnsizedStreamSource {
    UnsizedDecompressLz4 {
        requireNonNull(compressed);
    }

    @Override
    public InputStream openStream() throws IOException {
        return Lz4Support.newDecompressInputStream(compressed.openStream());
    }

    @Override
    public InputStream openBufferedStream() throws IOException {
        return openStream();
    }

    @Override
    public SizedStreamSource toSizedStreamSource() throws IOException {
        final long size;
        try (var is = openStream()) {
            size = is.transferTo(OutputStream.nullOutputStream());
        }
        return new SizedDecompressLz4(this, size);
    }
}
