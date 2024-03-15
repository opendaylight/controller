/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import io.atomix.storage.journal.index.JournalIndex;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

abstract sealed class JournalSegmentWriter<E> permits DiskJournalSegmentWriter, MappedJournalSegmentWriter {
    final @NonNull FileChannel channel;
    final @NonNull JournalSegment<E> segment;
    final @NonNull JournalIndex index;
    final @NonNull JournalSerdes namespace;
    final int maxSegmentSize;
    final int maxEntrySize;
    final long firstIndex;

    JournalSegmentWriter(final FileChannel channel, final JournalSegment<E> segment, final int maxEntrySize,
            final JournalIndex index, final JournalSerdes namespace, final int maxSegmentSize) {
        this.channel = requireNonNull(channel);
        this.segment = requireNonNull(segment);
        this.index = requireNonNull(index);
        this.namespace = requireNonNull(namespace);
        this.maxSegmentSize = maxSegmentSize;
        this.maxEntrySize = maxEntrySize;
        firstIndex = segment.index();
    }

    JournalSegmentWriter(final JournalSegmentWriter<E> previous) {
        channel = previous.channel;
        segment = previous.segment;
        index = previous.index;
        namespace = previous.namespace;
        maxSegmentSize = previous.maxSegmentSize;
        maxEntrySize = previous.maxEntrySize;
        firstIndex = previous.firstIndex;
    }

    /**
     * Returns the last written index.
     *
     * @return The last written index.
     */
    final long getLastIndex() {
        final Indexed<?> lastEntry;
        return (lastEntry = getLastEntry()) != null ? lastEntry.index() : firstIndex - 1;
    }

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
    final long getNextIndex() {
        final Indexed<?> lastEntry;
        return (lastEntry = getLastEntry()) != null ? lastEntry.index() + 1 : firstIndex;
    }

    /**
     * Appends an entry to the journal.
     *
     * @param entry The entry to append.
     * @return The appended indexed entry.
     */
    abstract <T extends E> Indexed<T> append(T entry);

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

    abstract @NonNull DiskJournalSegmentWriter<E> toFileChannel();
}
