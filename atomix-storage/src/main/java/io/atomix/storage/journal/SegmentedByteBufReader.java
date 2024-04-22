/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.
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

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNull;

/**
 * A {@link ByteBufReader} implementation.
 */
sealed class SegmentedByteBufReader implements ByteBufReader permits SegmentedCommitsByteBufReader {
    final @NonNull SegmentedByteBufJournal journal;

    private JournalSegment currentSegment;
    private JournalSegmentReader currentReader;
    private long nextIndex;

    SegmentedByteBufReader(final SegmentedByteBufJournal journal, final JournalSegment segment) {
        this.journal = requireNonNull(journal);
        currentSegment = requireNonNull(segment);
        currentReader = segment.createReader();
        nextIndex = currentSegment.firstIndex();
    }

    @Override
    public final long firstIndex() {
        return journal.firstSegment().firstIndex();
    }

    @Override
    public final long nextIndex() {
        return nextIndex;
    }

    @Override
    public final void reset() {
        currentReader.close();
        currentSegment = journal.firstSegment();
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
            forwardTo(index);
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
        forwardTo(index);
    }

    /**
     * Rewinds the journal to the given index.
     */
    private void rewind(final long index) {
        if (currentSegment.firstIndex() >= index) {
            final var segment = journal.segment(index - 1);
            if (segment != null) {
                currentReader.close();
                currentSegment = segment;
                currentReader = currentSegment.createReader();
            }
        }
        resetCurrentReader(index);
    }

    private void forwardTo(final long index) {
        while (nextIndex < index && tryAdvance(nextIndex) != null) {
            // No-op -- nextIndex value is updated in tryAdvance()
        }
    }

    @Override
    public final <T> T tryNext(final EntryMapper<T> entryMapper) {
        final var index = nextIndex;
        final var bytes = tryAdvance(index);
        return bytes == null ? null : entryMapper.mapEntry(index, bytes);
    }

    /**
     * Attempt to read the next entry. {@code index} here is really {@code nextIndex} passed by callers, which already
     * check it for invariants. If non-null is returned, {@code nextIndex} has already been set to {@code index + 1}.
     *
     * <p>
     * This method is shared between 'all entries' and 'committed entries only' variants. The distinction is made by
     * an additional check in {@link SegmentedCommitsByteBufReader#tryAdvance(long)}.
     *
     * @param index next index
     * @return Entry bytes, or {@code null}
     */
    ByteBuf tryAdvance(final long index) {
        var buf = currentReader.readBytes();
        if (buf == null) {
            final var nextSegment = journal.nextSegment(currentSegment.firstIndex());
            if (nextSegment == null || nextSegment.firstIndex() != index) {
                return null;
            }
            currentReader.close();
            currentSegment = nextSegment;
            currentReader = currentSegment.createReader();
            buf = currentReader.readBytes();
            if (buf == null) {
                return null;
            }
        }
        nextIndex = index + 1;
        return buf;
    }

    @Override
    public final void close() {
        currentReader.close();
        journal.closeReader(this);
    }
}
