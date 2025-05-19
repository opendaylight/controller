/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.api.EntryInfo;

/**
 * The state part logical content of an InstallSnapshot RPC.
 */
@NonNullByDefault
public interface InstallableSnapshot {
    /**
     * Returns the combined {@code lastIncludedIndex}/{@code lastIncludedTerm}.
     *
     * @return the combined {@code lastIncludedIndex}/{@code lastIncludedTerm}
     */
    EntryInfo lastIncluded();

    /**
     * Returns the source of byte stream carrying user state snapshot.
     *
     * @return the source of byte stream carrying user state snapshot, or {@code null} as there is no snapshot.
     */
    @Nullable SnapshotSource source();
}
