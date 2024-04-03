/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

/**
 * A {@link ByteJournalReader} implementation.
 */
sealed class SegmentedByteJournalReader implements ByteJournalReader
        permits SegmentedByteJournalReader.Commits {
    final SegmentedByteJournal journal;

    private JournalSegment currentSegment;
    private JournalSegmentReader currentReader;
    private byte[] lastRead;
    private long nextIndex;

    SegmentedByteJournalReader(final SegmentedByteJournal journal, final JournalSegment segment) {
        this.journal = requireNonNull(journal);
        currentSegment = requireNonNull(segment);
        currentReader = segment.createReader();
        nextIndex = currentSegment.firstIndex();
        lastRead = null;
    }

    @Override
    public long firstIndex() {
        return journal.firstSegment().firstIndex();
    }

    @Override
    public byte[] lastRead() {
        return lastRead;
    }

    @Override
    public long nextIndex() {
        return nextIndex;
    }

    @Override
    public void reset() {
        currentReader.close();
        currentSegment = journal.firstSegment();
        currentReader = currentSegment.createReader();
        nextIndex = currentSegment.firstIndex();
        lastRead = null;
    }

    @Override
    public void reset(final long index) {
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
            JournalSegment segment = journal.segment(index - 1);
            if (segment != null) {
                currentReader.close();

                currentSegment = segment;
                currentReader = currentSegment.createReader();
            }
        }

        resetCurrentReader(index);
    }

    @Override
    public byte[] tryNext() {
        var bytes = currentReader.readBytes(nextIndex);
        if (bytes == null) {
            final var nextSegment = journal.nextSegment(currentSegment.firstIndex());
            if (nextSegment == null || nextSegment.firstIndex() != nextIndex) {
                return null;
            }
            currentReader.close();
            currentSegment = nextSegment;
            currentReader = currentSegment.createReader();
            bytes = currentReader.readBytes(nextIndex);
            if (bytes == null) {
                return null;
            }
        }
        nextIndex++;
        return bytes;
    }

    @Override
    public void close() {
        currentReader.close();
        journal.closeReader(this);
    }

    static final class Commits extends SegmentedByteJournalReader {
        Commits(final SegmentedByteJournal journal, final JournalSegment segment) {
            super(journal, segment);
        }

        @Override
        public byte[] tryNext() {
            return nextIndex() <= journal.getCommitIndex() ? super.tryNext() : null;
        }
    }
}
