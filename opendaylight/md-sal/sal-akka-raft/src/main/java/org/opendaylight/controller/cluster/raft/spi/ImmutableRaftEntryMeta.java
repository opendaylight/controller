/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.annotations.Beta;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * An immutable {@link RaftEntryMeta}.
 */
@Beta
@NonNullByDefault
// TODO: use JEP-401 when available
public record ImmutableRaftEntryMeta(long index, long term) implements RaftEntryMeta, Immutable {
    /**
     * Default constructor.
     *
     * @param index the index of the journal entry
     * @param term the of the journal entry
     * @deprecated Use {@link #of(long, long)} instead.
     */
    @Deprecated(forRemoval = true)
    public ImmutableRaftEntryMeta {
        // Nothing here
    }

    /**
     * Return an immutable copy of a source {@link RaftEntryMeta}.
     *
     * @param entryMeta source {@link RaftEntryMeta}
     * @return An {@link ImmutableRaftEntryMeta}
     */
    public static ImmutableRaftEntryMeta copyOf(final RaftEntryMeta entryMeta) {
        return switch (entryMeta) {
            case ImmutableRaftEntryMeta immutable -> immutable;
            default -> of(entryMeta.index(), entryMeta.term());
        };
    }

    /**
     * Return a {@link ImmutableRaftEntryMeta} with specified index and term.
     *
     * @param index the {@link #index()}
     * @param term the {@link #term()}
     * @return An {@link ImmutableRaftEntryMeta}
     */
    public static ImmutableRaftEntryMeta of(final long index, final long term) {
        return new ImmutableRaftEntryMeta(index, term);
    }

    public static @Nullable ImmutableRaftEntryMeta ofNullable(final @Nullable RaftEntryMeta entryMeta) {
        return entryMeta == null ? null : copyOf(entryMeta);
    }
}