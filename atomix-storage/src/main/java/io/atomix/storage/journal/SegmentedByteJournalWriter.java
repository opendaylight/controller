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

import static com.google.common.base.Verify.verifyNotNull;

import io.netty.buffer.ByteBuf;

/**
 * A {@link ByteJournalWriter} implementation.
 */
final class SegmentedByteJournalWriter implements ByteJournalWriter {
    private final SegmentedByteJournal journal;
    private JournalSegment currentSegment;
    private JournalSegmentWriter currentWriter;

    SegmentedByteJournalWriter(SegmentedByteJournal journal) {
        this.journal = journal;
        currentSegment = journal.lastSegment();
        currentWriter = currentSegment.acquireWriter();
    }

    @Override
    public long lastIndex() {
        return currentWriter.getLastIndex();
    }

    @Override
    public long nextIndex() {
        return currentWriter.getNextIndex();
    }

    @Override
    public void reset(final long index) {
        if (index > currentSegment.firstIndex()) {
            currentSegment.releaseWriter();
            currentSegment = journal.resetSegments(index);
            currentWriter = currentSegment.acquireWriter();
        } else {
            truncate(index - 1);
        }
        journal.resetHead(index);
    }

    @Override
    public void commit(final long index) {
        if (index > journal.getCommitIndex()) {
            journal.setCommitIndex(index);
            if (journal.isFlushOnCommit()) {
                flush();
            }
        }
    }

    @Override
    public long append(final ByteBuf buf) {
        var index = currentWriter.append(buf);
        if (index != null) {
            return index;
        }
        //  Slow path: we do not have enough capacity
        currentWriter.flush();
        currentSegment.releaseWriter();
        currentSegment = journal.nextSegment();
        currentWriter = currentSegment.acquireWriter();
        return verifyNotNull(currentWriter.append(buf));
    }

    @Override
    public void truncate(long index) {
        if (index < journal.getCommitIndex()) {
            throw new IndexOutOfBoundsException("Cannot truncate committed index: " + index);
        }

        // Delete all segments with first indexes greater than the given index.
        while (index < currentSegment.firstIndex() && currentSegment != journal.firstSegment()) {
            currentSegment.releaseWriter();
            journal.removeSegment(currentSegment);
            currentSegment = journal.lastSegment();
            currentWriter = currentSegment.acquireWriter();
        }

        // Truncate the current index.
        currentWriter.truncate(index);

        // Reset segment readers.
        journal.resetTail(index + 1);
    }

    @Override
    public void flush() {
        currentWriter.flush();
    }
}
