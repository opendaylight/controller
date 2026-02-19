/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface capturing the ability to open {@link InputStream}s.
 */
@NonNullByDefault
public sealed interface StreamSource permits UnsizedStreamSource, SizedStreamSource {
    /**
     * Open an {@link InputStream}.
     *
     * @return an InputStream
     * @throws IOException if an I/O error occurs
     */
    InputStream openStream() throws IOException;

    /**
     * Open an {@link InputStream} which is reasonably buffered for small accesses. Default implementation defers to
     * {@link #openStream()}.
     *
     * @return an InputStream
     * @throws IOException if an I/O error occurs
     */
    default InputStream openBufferedStream() throws IOException {
        return openStream();
    }

    /**
     * Return the {@link SizedStreamSource} equivalent of this {@link StreamSource}.
     *
     * @return the {@link SizedStreamSource} equivalent of this {@link StreamSource}
     * @throws IOException if an I/O error occurs
     */
    SizedStreamSource toSizedStreamSource() throws IOException;

    /**
     * Open a {@link DataInputStream}, performing buffering over {@link #openStream()} if needed.
     *
     * @return a {@link DataInputStream}
     * @throws IOException if an I/O error occurs
     */
    default DataInputStream openDataInput() throws IOException {
        return new DataInputStream(openBufferedStream());
    }
}
