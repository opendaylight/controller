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
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link PlainSnapshotSource} backed by a {@link Lz4SnapshotSource}.
 */
@NonNullByDefault
public final class Lz4PlainSnapshotStream extends DelegatedSnapshotSource<Lz4SnapshotSource>
        implements PlainSnapshotSource {
    /**
     * Default constructor.
     *
     * @param delegate source {@link Lz4SnapshotSource}
     */
    public Lz4PlainSnapshotStream(final Lz4SnapshotSource delegate) {
        super(delegate);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returned stream performs transparent decompression from the delegate source.
     */
    @Override
    public InputStream openStream() throws IOException {
        return Lz4Support.newDecompressingInputStream(delegate().openStream());
    }
}
