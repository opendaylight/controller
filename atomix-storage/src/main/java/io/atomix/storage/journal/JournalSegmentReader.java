/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import io.atomix.storage.journal.index.JournalIndex;

abstract sealed class JournalSegmentReader<E> implements JournalReader<E>
        permits FileChannelJournalSegmentReader, MappedJournalSegmentReader {
    final int maxEntrySize;
    final JournalIndex index;
    final JournalSerdes namespace;
    final long firstIndex;

    JournalSegmentReader(final JournalSegment<E> segment, final int maxEntrySize, final JournalIndex index,
            final JournalSerdes namespace) {
        this.maxEntrySize = maxEntrySize;
        this.index = requireNonNull(index);
        this.namespace = requireNonNull(namespace);
        this.firstIndex = segment.index();
    }

    @Override
    public final long getFirstIndex() {
        return firstIndex;
    }

    @Override
    public final void close() {
        // FIXME: CONTROLLER-2098: remove this method
    }
}
