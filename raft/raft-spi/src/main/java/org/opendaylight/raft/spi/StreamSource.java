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
@FunctionalInterface
public interface StreamSource {
    /**
     * Open an {@link InputStream}.
     *
     * @return an InputStream
     * @throws IOException if an I/O error occurs
     */
    InputStream openStream() throws IOException;

    /**
     * Open a {@link DataInputStream}.
     *
     * @return a {@link DataInputStream}
     * @throws IOException if an I/O error occurs
     */
    default DataInputStream openDataInput() throws IOException {
        // FIXME: openBufferedStream()?
        final var stream = openStream();
        return stream instanceof DataInputStream dis ? dis : new DataInputStream(stream);
    }
}