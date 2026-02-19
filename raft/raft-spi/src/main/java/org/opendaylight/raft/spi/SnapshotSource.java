/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A source of bytes comprising the contents of a snapshot. It may or may not directly correspond to the serialization
 * format of a snapshot.
 */
@NonNullByDefault
public abstract sealed class SnapshotSource permits PlainSnapshotSource, Lz4SnapshotSource {
    private final StreamSource io;

    /**
     * Default constructor.
     *
     * @param io the {@link StreamSource} backing this {@link SnapshotSource}.
     */
    SnapshotSource(final StreamSource io) {
        this.io = requireNonNull(io);
    }

    /**
     * Returns the equivalent of this source as a {@linkplain PlainSnapshotSource}.
     *
     * @return the equivalent of this source as a {@linkplain PlainSnapshotSource}
     */
    public abstract PlainSnapshotSource toPlainSource();

    /**
     * Returns the {@link StreamSource} backing this {@link SnapshotSource}.
     *
     * @return the {@link StreamSource} backing this {@link SnapshotSource}.
     */
    public final StreamSource io() {
        return io;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("io", io).toString();
    }
}
