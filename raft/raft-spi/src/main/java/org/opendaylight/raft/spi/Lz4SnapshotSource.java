/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A <a href="https://en.wikipedia.org/wiki/LZ4_(compression_algorithm)">LZ4</a>-compressed {@link SnapshotSource}.
 */
@NonNullByDefault
public final class Lz4SnapshotSource extends SnapshotSource {
    private final InputStreamProvider provider;

    /**
     * Default constructor.
     *
     * @param provider the {@link InputStreamProvider}
     */
    public Lz4SnapshotSource(final InputStreamProvider provider) {
        this.provider = requireNonNull(provider);
    }

    @Override
    public InputStream openStream() throws IOException {
        return provider.openStream();
    }

    @Override
    public PlainSnapshotSource toPlainSource() {
        return new PlainSnapshotSource(new AdaptingInputStreamProvider(provider, Lz4Support::newDecompressInputStream));
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("provider", provider);
    }
}
