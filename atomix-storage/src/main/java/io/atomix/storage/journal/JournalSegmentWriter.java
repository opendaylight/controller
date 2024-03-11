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

abstract sealed class JournalSegmentWriter<E> permits FileChannelJournalSegmentWriter, MappedJournalSegmentWriter {
    final @NonNull FileChannel channel;
    final @NonNull JournalIndex index;
    final @NonNull JournalSerdes namespace;
    final int maxSegmentSize;
    final int maxEntrySize;
    final long firstIndex;

    JournalSegmentWriter(final FileChannel channel, final JournalSegment<E> segment, final int maxEntrySize,
            final JournalIndex index, final JournalSerdes namespace) {
        this.channel = requireNonNull(channel);
        this.index = requireNonNull(index);
        this.namespace = requireNonNull(namespace);
        this.maxSegmentSize = segment.descriptor().maxSegmentSize();
        this.maxEntrySize = maxEntrySize;
        this.firstIndex = segment.index();
    }

    JournalSegmentWriter(JournalSegmentWriter<E> previous) {
        this.channel = previous.channel;
        this.index = previous.index;
        this.namespace = previous.namespace;
        this.maxSegmentSize = previous.maxSegmentSize;
        this.maxEntrySize = previous.maxEntrySize;
        this.firstIndex = previous.firstIndex;
    }

    /**
     * Returns the last written index.
     *
     * @return The last written index.
     */
    abstract long getLastIndex();

    /**
     * Returns the last entry written.
     *
     * @return The last entry written.
     */
    abstract Indexed<E> getLastEntry();

    /**
     * Returns the next index to be written.
     *
     * @return The next index to be written.
     */
    abstract long getNextIndex();

    /**
     * Appends an entry to the journal.
     *
     * @param entry The entry to append.
     * @return The appended indexed entry.
     */
    abstract <T extends E> Indexed<T> append(T entry);

    /**
     * Appends an indexed entry to the log.
     *
     * @param entry The indexed entry to append.
     */
    abstract void append(Indexed<E> entry);

    /**
     * Resets the head of the segment to the given index.
     *
     * @param index the index to which to reset the head of the segment
     */
    abstract void reset(long index);

    /**
     * Truncates the log to the given index.
     *
     * @param index The index to which to truncate the log.
     */
    abstract void truncate(long index);

    /**
     * Flushes written entries to disk.
     */
    abstract void flush();

    /**
     * Closes this writer.
     */
    abstract void close();

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
