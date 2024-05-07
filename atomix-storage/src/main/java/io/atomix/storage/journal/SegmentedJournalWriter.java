/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
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

/**
 * Raft log writer.
 */
final class SegmentedJournalWriter<E> implements JournalWriter<E> {
  private final SegmentedJournal<E> journal;
  private JournalSegment currentSegment;
  private JournalSegmentWriter currentWriter;

  SegmentedJournalWriter(SegmentedJournal<E> journal) {
    this.journal = journal;
    this.currentSegment = journal.getLastSegment();
    this.currentWriter = currentSegment.acquireWriter();
  }

  @Override
  public long getLastIndex() {
    return currentWriter.getLastIndex();
  }

  @Override
  public long getNextIndex() {
    return currentWriter.getNextIndex();
  }

  @Override
  public void commit(long index) {
    if (index > journal.getCommitIndex()) {
      journal.setCommitIndex(index);
      if (journal.isFlushOnCommit()) {
        flush();
      }
    }
  }

  @Override
  public <T extends E> Indexed<T> append(T entry) {
    final var bytes = journal.serializer().serialize(entry);
    var index = currentWriter.append(bytes);
    if (index != null) {
      return new Indexed<>(index, entry, bytes.readableBytes());
    }

    //  Slow path: we do not have enough capacity
    currentWriter.flush();
    currentSegment.releaseWriter();
    currentSegment = journal.getNextSegment();
    currentWriter = currentSegment.acquireWriter();
    final var newIndex = verifyNotNull(currentWriter.append(bytes));
    return new Indexed<>(newIndex, entry, bytes.readableBytes());
  }

  @Override
  public void reset(long index) {
    final long commitIndex = journal.getCommitIndex();
    if (index <= commitIndex) {
      // also catches index == 0, which is not a valid next index
      throw new IndexOutOfBoundsException("Cannot reset to: " + index + ", committed index: " + commitIndex);
    }

    if (index > currentSegment.firstIndex()) {
      currentSegment.releaseWriter();
      currentSegment = journal.resetSegments(index);
      currentWriter = currentSegment.acquireWriter();
    } else {
      checkedTruncate(index - 1);
    }
    journal.resetHead(index);
  }

  @Override
  public void truncate(long index) {
    if (index < journal.getCommitIndex()) {
      throw new IndexOutOfBoundsException("Cannot truncate committed index: " + index);
    }
    checkedTruncate(index);
  }

  private void checkedTruncate(long index) {
    // Delete all segments with first indexes greater than the given index.
    while (index < currentSegment.firstIndex() && currentSegment != journal.getFirstSegment()) {
      currentSegment.releaseWriter();
      journal.removeSegment(currentSegment);
      currentSegment = journal.getLastSegment();
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
