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
import io.atomix.storage.journal.index.Position;
import java.util.NoSuchElementException;
import org.eclipse.jdt.annotation.Nullable;

abstract sealed class JournalSegmentReader<E> permits DiskJournalSegmentReader, MappedJournalSegmentReader {
    final int maxEntrySize;
    private final JournalIndex index;
    final JournalSerdes namespace;
    private final long firstIndex;
    private final JournalSegment<E> segment;

    private Indexed<E> currentEntry;
    private Indexed<E> nextEntry;

    JournalSegmentReader(final JournalSegment<E> segment, final int maxEntrySize, final JournalIndex index,
            final JournalSerdes namespace) {
        this.segment = requireNonNull(segment);
        this.maxEntrySize = maxEntrySize;
        this.index = requireNonNull(index);
        this.namespace = requireNonNull(namespace);
        firstIndex = segment.index();
    }

    /**
     * Returns the last read entry.
     *
     * @return The last read entry.
     */
    final Indexed<E> getCurrentEntry() {
        return currentEntry;
    }

    /**
     * Returns the next reader index.
     *
     * @return The next reader index.
     */
    final long getNextIndex() {
        return currentEntry != null ? currentEntry.index() + 1 : firstIndex;
    }

    /**
     * Returns whether the reader has a next entry to read.
     *
     * @return Whether the reader has a next entry to read.
     */
    final boolean hasNext() {
        return nextEntry != null || (nextEntry = readNext()) != null;
    }

    /**
     * Returns the next entry in the reader.
     *
     * @return The next entry in the reader.
     * @throws UnsupportedOperationException if there is no such entry
     */
    final Indexed<E> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        // Set the current entry to the next entry.
        currentEntry = nextEntry;

        // Reset the next entry to null.
        nextEntry = null;

        // Read the next entry in the segment.
        nextEntry = readNext();

        // Return the current entry.
        return currentEntry;
    }

    /**
     * Resets the reader to the start of the segment.
     */
    final void reset() {
        currentEntry = null;
        nextEntry = null;
        setPosition(JournalSegmentDescriptor.BYTES);
        nextEntry = readNext();
    }

    /**
     * Resets the reader to the given index.
     *
     * @param index The index to which to reset the reader.
     */
    final void reset(final long index) {
        reset();
        Position position = this.index.lookup(index - 1);
        if (position != null) {
            // FIXME: why do we need a 'null'-based entry here?
            currentEntry = new Indexed<>(position.index() - 1, null, 0);
            setPosition(position.position());
            nextEntry = readNext();
        }
        while (getNextIndex() < index && hasNext()) {
            next();
        }
    }

    /**
     * Close this reader.
     */
    final void close() {
        segment.closeReader(this);
    }

    /**
     * Set the file position.
     *
     * @param position new position
     */
    abstract void setPosition(int position);

    /**
     * Reads the entry at specified index.
     *
     * @param index entry index
     * @return The entry, or {@code null}
     */
    abstract @Nullable Indexed<E> readEntry(long index);

    private @Nullable Indexed<E> readNext() {
        // Compute the index of the next entry in the segment.
        return readEntry(getNextIndex());
    }
}