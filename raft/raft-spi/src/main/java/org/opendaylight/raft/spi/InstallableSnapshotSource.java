/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.raft.api.EntryInfo;

/**
 * An {@link InstallableSnapshot} backed by a {@link SnapshotSource}.
 *
 * @param lastIncluded last entry included in this snapshot
 * @param source the {@link SnapshotSource}
 */
@NonNullByDefault
public record InstallableSnapshotSource(EntryInfo lastIncluded, SnapshotSource source) implements InstallableSnapshot {
    /**
     * Default constructor.
     *
     * @param lastIncluded last entry included in this snapshot
     * @param source the {@link SnapshotSource}
     */
    public InstallableSnapshotSource {
        requireNonNull(lastIncluded);
        requireNonNull(source);
    }

    /**
     * Convenience constructor.
     *
     * @param index index of last included entry
     * @param term term of last included entry
     * @param source the {@link SnapshotSource}
     */
    public InstallableSnapshotSource(final long index, final long term, final SnapshotSource source) {
        this(EntryInfo.of(index, term), source);
    }
}
