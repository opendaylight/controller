/*
 * Copyright 2018-present Open Networking Foundation
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

import io.atomix.storage.journal.index.JournalIndex;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Mappable log segment writer.
 */
final class MappableJournalSegmentWriter<E> implements JournalWriter<E> {
  private final FileChannel channel;
  private final JournalSegment<E> segment;
  private JournalSegmentWriter<E> writer;

  MappableJournalSegmentWriter(
      FileChannel channel,
      JournalSegment<E> segment,
      int maxEntrySize,
      JournalIndex index,
      JournalSerdes namespace) {
    this.channel = channel;
    this.segment = segment;
    this.writer = new FileChannelJournalSegmentWriter<>(channel, segment, maxEntrySize, index, namespace);
  }

  /**
   * Maps the segment writer into memory, returning the mapped buffer.
   *
   * @return the buffer that was mapped into memory
   */
  MappedByteBuffer map() {
    final var mapped = writer.toMapped();
    writer = mapped;
    return mapped.buffer();
  }

  /**
   * Unmaps the mapped buffer.
   */
  void unmap() {
    writer = writer.toFileChannel();
  }

  MappedByteBuffer buffer() {
    return writer.buffer();
  }

  /**
   * Returns the writer's first index.
   *
   * @return the writer's first index
   */
  public long firstIndex() {
    return segment.index();
  }

  /**
   * Returns the size of the segment.
   *
   * @return the size of the segment
   */
  public int size() {
    try {
      return (int) channel.size();
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public long getLastIndex() {
    return writer.getLastIndex();
  }

  @Override
  public Indexed<E> getLastEntry() {
    return writer.getLastEntry();
  }

  @Override
  public long getNextIndex() {
    return writer.getNextIndex();
  }

  @Override
  public <T extends E> Indexed<T> append(T entry) {
    return writer.append(entry);
  }

  @Override
  public void append(Indexed<E> entry) {
    writer.append(entry);
  }

  @Override
  public void commit(long index) {
    writer.commit(index);
  }

  @Override
  public void reset(long index) {
    writer.reset(index);
  }

  @Override
  public void truncate(long index) {
    writer.truncate(index);
  }

  @Override
  public void flush() {
    writer.flush();
  }

  @Override
  public void close() {
    writer.close();
    try {
      channel.close();
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }
}
