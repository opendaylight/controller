/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.api;

import java.io.ObjectStreamException;
import java.io.Serializable;
import javax.management.ConstructorParameters;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An immutable {@link EntryMeta}.
 *
 * @param index log entry index
 * @param term log entry term
 */
// TODO: dedicated type for index/term to ensure unsigned without paying the memory cost (JEP-401)
@NonNullByDefault
public record EntryInfo(long index, long term) implements EntryMeta, Serializable {
    /**
     * Default constructor.
     *
     * @param index log entry index
     * @param term log entry term
     */
    @ConstructorParameters({"index", "term"})
    public EntryInfo {
        // Nothing else
    }

    /**
     * Return an {@link EntryInfo} with specified index and term.
     *
     * @param index the index
     * @param term the term
     * @return an {@link EntryInfo}
     */
    public static EntryInfo of(final long index, final long term) {
        return new EntryInfo(index, term);
    }

    /**
     * Return an {@link EntryInfo} corresponding to an {@link EntryMeta}.
     *
     * @param meta an {@link EntryMeta}
     * @return an {@link EntryInfo}
     */
    public static EntryInfo of(final EntryMeta meta) {
        return meta instanceof EntryInfo info ? info : new EntryInfo(meta.index(), meta.term());
    }

    /**
     * Return an {@link EntryInfo} corresponding to an {@link EntryMeta}.
     *
     * @param meta an {@link EntryMeta}
     * @return an {@link EntryInfo}, or {@code null}
     */
    public static @Nullable EntryInfo ofNullable(final @Nullable EntryMeta meta) {
        return meta != null ? of(meta) : null;
    }

    /**
     * Return the serialization proxy.
     * @return the serialization proxy
     * @throws ObjectStreamException never
     */
    @java.io.Serial
    private Object writeReplace() throws ObjectStreamException {
        return new EIv1(index, term);
    }
}
