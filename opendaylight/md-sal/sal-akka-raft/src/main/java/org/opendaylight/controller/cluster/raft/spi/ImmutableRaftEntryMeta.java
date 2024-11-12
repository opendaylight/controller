/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * An immutable {@link RaftEntryMeta}.
 */
@Beta
// TODO: use JEP-401 when available
public record ImmutableRaftEntryMeta(long index, long term) implements RaftEntryMeta, Immutable {
    /**
     * Return an immutable copy of a source {@link RaftEntryMeta}.
     *
     * @param entryMeta source {@link RaftEntryMeta}
     * @return An {@link ImmutableRaftEntryMeta}
     */
    public static @NonNull ImmutableRaftEntryMeta copyOf(final RaftEntryMeta entryMeta) {
        return switch (entryMeta) {
            case ImmutableRaftEntryMeta immutable -> immutable;
            default -> new ImmutableRaftEntryMeta(entryMeta.index(), entryMeta.term());
        };
    }
}