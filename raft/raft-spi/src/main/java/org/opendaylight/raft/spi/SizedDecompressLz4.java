/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link SizedStreamSource} performing transparent LZ4 decompression of a backing {@link StreamSource}.
 */
record SizedDecompressLz4(UnsizedDecompressLz4 unsized, long size) implements SizedStreamSource {
    @Override
    public InputStream openStream() throws IOException {
        return unsized.openStream();
    }
}
