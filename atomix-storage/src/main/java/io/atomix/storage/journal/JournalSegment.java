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

import com.google.common.base.MoreObjects;
import io.atomix.storage.journal.index.JournalIndex;
import io.atomix.storage.journal.index.Position;
import io.atomix.storage.journal.index.SparseJournalIndex;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Log segment.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class JournalSegment<E> implements AutoCloseable {
  private final JournalSegmentFile file;
  private final JournalSegmentDescriptor descriptor;
  private final StorageLevel storageLevel;
  private final int maxEntrySize;
  private final JournalIndex journalIndex;
  private final JournalSerdes namespace;
  private final Set<JournalSegmentReader<E>> readers = ConcurrentHashMap.newKeySet();
  private final AtomicInteger references = new AtomicInteger();
  private final FileChannel channel;

  private JournalSegmentWriter<E> writer;
  private boolean open = true;

  JournalSegment(
      JournalSegmentFile file,
      JournalSegmentDescriptor descriptor,
      StorageLevel storageLevel,
      int maxEntrySize,
      double indexDensity,
      JournalSerdes namespace) {
    this.file = file;
    this.descriptor = descriptor;
    this.storageLevel = storageLevel;
    this.maxEntrySize = maxEntrySize;
    this.namespace = namespace;
    journalIndex = new SparseJournalIndex(indexDensity);
    try {
      channel = FileChannel.open(file.file().toPath(),
        StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
    } catch (IOException e) {
      throw new StorageException(e);
    }
    writer = switch (storageLevel) {
        case DISK -> new DiskJournalSegmentWriter<>(channel, this, maxEntrySize, journalIndex, namespace);
        case MAPPED -> new MappedJournalSegmentWriter<>(channel, this, maxEntrySize, journalIndex, namespace)
            .toFileChannel();
    };
  }

  /**
   * Returns the segment's starting index.
   *
   * @return The segment's starting index.
   */
  long index() {
    return descriptor.index();
  }

  /**
   * Returns the last index in the segment.
   *
   * @return The last index in the segment.
   */
  long lastIndex() {
    return writer.getLastIndex();
  }

  /**
   * Returns the size of the segment.
   *
   * @return the size of the segment
   */
  int size() {
    try {
      return (int) channel.size();
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  /**
   * Returns the segment file.
   *
   * @return The segment file.
   */
  JournalSegmentFile file() {
    return file;
  }

  /**
   * Returns the segment descriptor.
   *
   * @return The segment descriptor.
   */
  JournalSegmentDescriptor descriptor() {
    return descriptor;
  }

  /**
   * Looks up the position of the given index.
   *
   * @param index the index to lookup
   * @return the position of the given index or a lesser index, or {@code null}
   */
  @Nullable Position lookup(long index) {
    return journalIndex.lookup(index);
  }

  /**
   * Acquires a reference to the log segment.
   */
  private void acquire() {
    if (references.getAndIncrement() == 0 && storageLevel == StorageLevel.MAPPED) {
      writer = writer.toMapped();
    }
  }

  /**
   * Releases a reference to the log segment.
   */
  private void release() {
    if (references.decrementAndGet() == 0) {
      if (storageLevel == StorageLevel.MAPPED) {
        writer = writer.toFileChannel();
      }
      if (!open) {
        finishClose();
      }
    }
  }

  /**
   * Acquires a reference to the segment writer.
   *
   * @return The segment writer.
   */
  JournalSegmentWriter<E> acquireWriter() {
    checkOpen();
    acquire();

    return writer;
  }

  /**
   * Releases the reference to the segment writer.
   */
  void releaseWriter() {
      release();
  }

  /**
   * Creates a new segment reader.
   *
   * @return A new segment reader.
   */
  JournalSegmentReader<E> createReader() {
    checkOpen();
    acquire();

    final var buffer = writer.buffer();
    final var path = file.file().toPath();
    final var fileReader = buffer != null ? new MappedFileReader(path, buffer)
        : new DiskFileReader(path, channel, descriptor.maxSegmentSize(), maxEntrySize);
    final var reader = new JournalSegmentReader<>(this, fileReader, maxEntrySize, namespace);
    reader.setPosition(JournalSegmentDescriptor.BYTES);
    readers.add(reader);
    return reader;
  }

  /**
   * Closes a segment reader.
   *
   * @param reader the closed segment reader
   */
  void closeReader(JournalSegmentReader<E> reader) {
    if (readers.remove(reader)) {
      release();
    }
  }

  /**
   * Checks whether the segment is open.
   */
  private void checkOpen() {
    if (!open) {
      throw new IllegalStateException("Segment not open");
    }
  }

  /**
   * Returns a boolean indicating whether the segment is open.
   *
   * @return indicates whether the segment is open
   */
  public boolean isOpen() {
    return open;
  }

  /**
   * Closes the segment.
   */
  @Override
  public void close() {
    if (!open) {
      return;
    }

    open = false;
    readers.forEach(JournalSegmentReader::close);
    if (references.get() == 0) {
      finishClose();
    }
  }

  private void finishClose() {
    writer.close();
    try {
      channel.close();
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  /**
   * Deletes the segment.
   */
  void delete() {
    try {
      Files.deleteIfExists(file.file().toPath());
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", descriptor.id())
        .add("version", descriptor.version())
        .add("index", index())
        .toString();
  }
}
