/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.io.IOException;
import java.io.InputStream;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.xxhash.XXHashFactory;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link PlainSnapshotSource} backed by a {@link Lz4SnapshotSource}.
 */
@NonNullByDefault
public final class Lz4PlainSnapshotStream extends DelegatedSnapshotSource<Lz4SnapshotSource>
        implements PlainSnapshotSource {
    private static final LZ4Factory LZ4_FACTORY = LZ4Factory.fastestInstance();
    private static final XXHashFactory HASH_FACTORY = XXHashFactory.fastestInstance();

    public Lz4PlainSnapshotStream(final Lz4SnapshotSource delegate) {
        super(delegate);
    }

    /**
     * {@inheritDoc}
     *
     * Returned stream performs transparent decompression from the delegate source.
     */
    @Override
    public InputStream openStream() throws IOException {
        return new LZ4FrameInputStream(delegate().openStream(), LZ4_FACTORY.safeDecompressor(), HASH_FACTORY.hash32());
    }
}
