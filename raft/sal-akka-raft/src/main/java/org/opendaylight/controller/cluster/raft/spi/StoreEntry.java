/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.io.DataOutput;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.raft.api.EntryMeta;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * A storage reflection of a {@link LogEntry}, either in the direction {@link FromStoreEntry from EntryStore} or
 * {@link ToStoreEntry to EntryStore}. In both cases we know the {@link #meta() metadata} and
 * {@link #size() serialized size}.
 */
@NonNullByDefault
sealed interface StoreEntry permits FromStoreEntry, ToStoreEntry {
    /**
     * Minimum entry size: 3 bytes to encode {@link WritableObjects#writeLongs(DataOutput, long, long)} and 1 header
     * byte.
     */
    int MIN_SIZE = 3 + 1;

    /**
     * Returns the {@link EntryMeta} of this entry.
     *
     * @return the {@link EntryMeta} of this entry
     */
    EntryMeta meta();

    /**
     * Returns the serialized size of this entry.
     *
     * @return the serialized size of this entry
     */
    long size();
}
