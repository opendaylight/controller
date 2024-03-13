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
import org.eclipse.jdt.annotation.NonNull;

abstract sealed class JournalSegmentWriter<E> permits FileChannelJournalSegmentWriter, MappedJournalSegmentWriter {
    final @NonNull JournalIndex index;
    final @NonNull JournalSerdes namespace;
    final int maxSegmentSize;
    final int maxEntrySize;
    final long firstIndex;

    JournalSegmentWriter(final JournalSegment<E> segment, final int maxEntrySize, final JournalIndex index,
            final JournalSerdes namespace) {
        this.index = requireNonNull(index);
        this.namespace = requireNonNull(namespace);
        this.maxSegmentSize = segment.descriptor().maxSegmentSize();
        this.maxEntrySize = maxEntrySize;
        this.firstIndex = segment.index();
    }

    JournalSegmentWriter(JournalSegmentWriter<E> previous) {
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
}
