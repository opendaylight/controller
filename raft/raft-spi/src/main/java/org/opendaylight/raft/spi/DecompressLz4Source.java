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
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * An {@link StreamSource} performing transparent LZ4 decompression of a backing {@link StreamSource}.
 */
@NonNullByDefault
record DecompressLz4Source(StreamSource compressed) implements StreamSource {
    DecompressLz4Source {
        requireNonNull(compressed);
    }

    @Override
    public InputStream openStream() throws IOException {
        return Lz4Support.newDecompressInputStream(compressed.openStream());
    }
}
