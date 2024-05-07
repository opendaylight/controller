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
import static java.util.Objects.requireNonNull;

/**
 * A {@link ByteBufWriter} implementation.
 */
final class SegmentedByteBufWriter implements ByteBufWriter {
    private final SegmentedByteBufJournal journal;

    private JournalSegment currentSegment;
    private JournalSegmentWriter currentWriter;

    SegmentedByteBufWriter(final SegmentedByteBufJournal journal) {
        this.journal = requireNonNull(journal);
        currentSegment = journal.lastSegment();
        currentWriter = currentSegment.acquireWriter();
    }

    @Override
    public long nextIndex() {
        return currentWriter.nextIndex();
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
    public <T> int append(final ByteBufMapper<T> mapper, final T entry) {
        final var size = currentWriter.append(mapper, entry);
        return size != null ? size : appendToNextSegment(mapper, entry);
    }

    //  Slow path: we do not have enough capacity
    private <T> int appendToNextSegment(final ByteBufMapper<T> mapper, final T entry) {
        currentWriter.flush();
        currentSegment.releaseWriter();
        currentSegment = journal.createNextSegment();
        currentWriter = currentSegment.acquireWriter();
        return verifyNotNull(currentWriter.append(mapper, entry));
    }

    @Override
    public void reset(final long index) {
        final var commitIndex = journal.getCommitIndex();
        if (index <= commitIndex) {
            // also catches index == 0, which is not a valid next index
            throw new IndexOutOfBoundsException("Cannot reset to: " + index + ", committed index: " + commitIndex);
        }

        final var lastIndex = currentSegment.lastIndex();
        final var prevIndex = index - 1;
        if (prevIndex == lastIndex) {
            // already at the correct position: no-op
            return;
        }
        if (prevIndex > lastIndex) {
            // cannot seek past last written entry
            throw new IndexOutOfBoundsException("Cannot reset to: " + index + ", lastIndex: " + lastIndex);
        }

        // move back:
        // 1. delete all segments with first indexes greater than the given index.
        while (prevIndex < currentSegment.firstIndex() && currentSegment != journal.firstSegment()) {
            currentSegment.releaseWriter();
            journal.removeSegment(currentSegment);
            currentSegment = journal.lastSegment();
            currentWriter = currentSegment.acquireWriter();
        }
        // 2. truncate the current index.
        currentWriter.truncate(prevIndex);

        // 3. reset segment readers.
        journal.resetTail(index);
        journal.resetHead(index);
    }

    @Override
    public void flush() {
        currentWriter.flush();
    }
}
