/*
 * Copyright 2017-present Open Networking Foundation
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
  private JournalSegment<E> currentSegment;
  private Indexed<E> previousEntry;
  private JournalSegmentReader<E> currentReader;

  SegmentedJournalReader(SegmentedJournal<E> journal, JournalSegment<E> segment) {
    this.journal = requireNonNull(journal);
    currentSegment = requireNonNull(segment);
    currentReader = segment.createReader();
  }

  @Override
  public final long getFirstIndex() {
    return journal.getFirstSegment().index();
  }

  @Override
  public final long getCurrentIndex() {
    final var currentEntry = currentReader.getCurrentEntry();
    if (currentEntry != null) {
      final long currentIndex = currentEntry.index();
      if (currentIndex != 0) {
        return currentIndex;
      }
    }
    return previousEntry != null ? previousEntry.index() : 0;
  }

  @Override
  public final Indexed<E> getCurrentEntry() {
    // If previousEntry was the last in the previous segment, we may have moved currentReader to the next segment.
    // That segment may be empty, though, in which case we need to report the previousEntry.
    final Indexed<E> currentEntry;
    return (currentEntry = currentReader.getCurrentEntry()) != null ? currentEntry : previousEntry;
  }

  @Override
  public final long getNextIndex() {
    return currentReader.getNextIndex();
  }

  @Override
  public final void reset() {
    previousEntry = null;
    currentReader.close();

    currentSegment = journal.getFirstSegment();
    currentReader = currentSegment.createReader();
  }

  @Override
  public final void reset(long index) {
    // If the current segment is not open, it has been replaced. Reset the segments.
    if (!currentSegment.isOpen()) {
      reset();
    }

    final var nextIndex = currentReader.getNextIndex();
    if (index < nextIndex) {
      rewind(index);
    } else if (index > nextIndex) {
      forward(index);
    } else {
      currentReader.reset(index);
    }
  }

  /**
   * Rewinds the journal to the given index.
   */
  private void rewind(long index) {
    if (currentSegment.index() >= index) {
      JournalSegment<E> segment = journal.getSegment(index - 1);
      if (segment != null) {
        currentReader.close();

        currentSegment = segment;
        currentReader = currentSegment.createReader();
      }
    }

    currentReader.reset(index);
    previousEntry = currentReader.getCurrentEntry();
  }

  /**
   * Fast forwards the journal to the given index.
   */
  private void forward(long index) {
    while (getNextIndex() < index && tryNext() != null) {
      // Nothing else
    }
  }

  @Override
  public Indexed<E> tryNext() {
    if (currentReader.hasNext()) {
      previousEntry = currentReader.getCurrentEntry();
      return currentReader.next();
    }

    final var nextSegment = journal.getNextSegment(currentSegment.index());
    if (nextSegment == null || nextSegment.index() != getNextIndex()) {
      return null;
    }

    previousEntry = currentReader.getCurrentEntry();
    currentReader.close();

    currentSegment = nextSegment;
    currentReader = currentSegment.createReader();
    return currentReader.hasNext() ? currentReader.next() : null;
  }

  @Override
  public final void close() {
    currentReader.close();
    journal.closeReader(this);
  }
}
