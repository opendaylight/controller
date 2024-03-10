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
import io.atomix.storage.journal.index.Position;
import java.util.NoSuchElementException;
import org.eclipse.jdt.annotation.Nullable;

abstract sealed class JournalSegmentReader<E> implements JournalReader<E>
        permits FileChannelJournalSegmentReader, MappedJournalSegmentReader {
    final int maxEntrySize;
    private final JournalIndex index;
    final JournalSerdes namespace;
    private final long firstIndex;

    private Indexed<E> currentEntry;
    private Indexed<E> nextEntry;

    JournalSegmentReader(final JournalSegment<E> segment, final int maxEntrySize, final JournalIndex index,
            final JournalSerdes namespace) {
        this.maxEntrySize = maxEntrySize;
        this.index = requireNonNull(index);
        this.namespace = requireNonNull(namespace);
        firstIndex = segment.index();
    }

    @Override
    public final long getFirstIndex() {
        return firstIndex;
    }

    @Override
    public final long getCurrentIndex() {
        return currentEntry != null ? currentEntry.index() : 0;
    }

    @Override
    public final Indexed<E> getCurrentEntry() {
        return currentEntry;
    }

    @Override
    public final long getNextIndex() {
        return currentEntry != null ? currentEntry.index() + 1 : firstIndex;
    }

    @Override
    public final boolean hasNext() {
        return nextEntry != null || (nextEntry = readNext()) != null;
    }

    @Override
    public final Indexed<E> next() {
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

    @Override
    public final void reset() {
        currentEntry = null;
        nextEntry = null;
        setPosition(JournalSegmentDescriptor.BYTES);
        nextEntry = readNext();
    }

    @Override
    public final void reset(final long index) {
        reset();
        Position position = this.index.lookup(index - 1);
        if (position != null) {
            currentEntry = new Indexed<>(position.index() - 1, null, 0);
            setPosition(position.position());
            nextEntry = readNext();
        }
        while (getNextIndex() < index && hasNext()) {
            next();
        }
    }

    @Override
    public final void close() {
        // FIXME: CONTROLLER-2098: remove this method
    }

    /**
     * Set the file position.
     *
     * @param position new position
     */
    abstract void setPosition(int position);

    /**
     * Reads the next entry in the segment.
     *
     * @return Next entry, or {@code null}
     */
    abstract @Nullable Indexed<E> readNext();
}
