/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
 * Copyright (c) 2024 PANTHEON.tech, s.r.o.
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

/**
 * A {@link JournalReader} traversing all entries.
 */
sealed class SegmentedJournalReader<E> implements JournalReader<E> permits CommitsSegmentJournalReader {
    final SegmentedJournal<E> journal;

    private JournalSegment currentSegment;
    private JournalSegmentReader currentReader;
    private long nextIndex;

    SegmentedJournalReader(final SegmentedJournal<E> journal, final JournalSegment segment) {
        this.journal = requireNonNull(journal);
        currentSegment = requireNonNull(segment);
        currentReader = segment.createReader();
        nextIndex = currentSegment.firstIndex();
    }

    @Override
    public final long getFirstIndex() {
        return journal.getFirstSegment().firstIndex();
    }

    @Override
    public final long getNextIndex() {
        return nextIndex;
    }

    @Override
    public final void reset() {
        currentReader.close();

        currentSegment = journal.getFirstSegment();
        currentReader = currentSegment.createReader();
        nextIndex = currentSegment.firstIndex();
    }

    @Override
    public final void reset(final long index) {
        // If the current segment is not open, it has been replaced. Reset the segments.
        if (!currentSegment.isOpen()) {
            reset();
        }

        if (index < nextIndex) {
            rewind(index);
        } else if (index > nextIndex) {
            while (index > nextIndex && tryNext() != null) {
                // Nothing else
            }
        } else {
            resetCurrentReader(index);
        }
    }

    private void resetCurrentReader(final long index) {
        final var position = currentSegment.lookup(index - 1);
        if (position != null) {
            nextIndex = position.index();
            currentReader.setPosition(position.position());
        } else {
            nextIndex = currentSegment.firstIndex();
            currentReader.setPosition(JournalSegmentDescriptor.BYTES);
        }
        while (nextIndex < index && tryNext() != null) {
            // Nothing else
        }
    }

    /**
     * Rewinds the journal to the given index.
     */
    private void rewind(final long index) {
        if (currentSegment.firstIndex() >= index) {
            JournalSegment segment = journal.getSegment(index - 1);
            if (segment != null) {
                currentReader.close();

                currentSegment = segment;
                currentReader = currentSegment.createReader();
            }
        }

        resetCurrentReader(index);
    }

    @Override
    public Indexed<E> tryNext() {
        final var index = nextIndex;
        var buf = currentReader.readBytes(index);
        if (buf == null) {
            final var nextSegment = journal.getNextSegment(currentSegment.firstIndex());
            if (nextSegment == null || nextSegment.firstIndex() != index) {
                return null;
            }

            currentReader.close();

            currentSegment = nextSegment;
            currentReader = currentSegment.createReader();
            buf = currentReader.readBytes(index);
            if (buf == null) {
                return null;
            }
        }

        final var entry = journal.serializer().deserialize(buf);
        nextIndex = index + 1;
        return new Indexed<>(index, entry, buf.readableBytes());
    }

    @Override
    public final void close() {
        currentReader.close();
        journal.closeReader(this);
    }
}
