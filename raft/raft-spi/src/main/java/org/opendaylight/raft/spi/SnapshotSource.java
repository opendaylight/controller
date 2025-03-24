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
 * A source of bytes comprising the contents of a snapshot. It may or may not directly correspond to the serialization
 * format of a snapshot.
 */
public sealed interface SnapshotSource permits PlainSnapshotSource, Lz4SnapshotSource {
    /**
     * Open this stream as an {@link InputStream}.
     *
     * @return an InputStream
     * @throws IOException if an error occurs
     */
    InputStream openStream() throws IOException;

    /**
     * Returns the equivalent of this source as a {@linkplain PlainSnapshotSource}.
     *
     * @return the equivalent of this source as a {@linkplain PlainSnapshotSource}
     */
    PlainSnapshotSource toPlainSource();
}