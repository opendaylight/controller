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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

abstract sealed class JournalSegmentWriter<E> implements JournalWriter<E>
        permits FileChannelJournalSegmentWriter, MappedJournalSegmentWriter {
    final @NonNull FileChannel channel;
    final @NonNull JournalSegment<E> segment;
    final int maxEntrySize;
    final @NonNull JournalIndex index;
    final @NonNull JournalSerdes namespace;
    final long firstIndex;

    JournalSegmentWriter(final FileChannel channel, final JournalSegment<E> segment, final int maxEntrySize,
            final JournalIndex index, final JournalSerdes namespace) {
        this.channel = requireNonNull(channel);
        this.segment = requireNonNull(segment);
        this.maxEntrySize = maxEntrySize;
        this.index = requireNonNull(index);
        this.namespace = requireNonNull(namespace);
        this.firstIndex = segment.index();
    }

    @Override
    public final void commit(final long index) {
        // FIXME: CONTROLLER-2098: eliminate the need for this method
    }

    /**
     * Returns the mapped buffer underlying the segment writer, or {@code null} if the writer does not have such a
     * buffer.
     *
     * @return the mapped buffer underlying the segment writer, or {@code null}.
     */
    abstract @Nullable MappedByteBuffer buffer();

    abstract @NonNull MappedJournalSegmentWriter<E> toMapped();

    abstract @NonNull FileChannelJournalSegmentWriter<E> toFileChannel();
}
