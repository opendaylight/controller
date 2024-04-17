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

import static io.atomix.storage.journal.DiskFileReader.ioBufferSize;

import com.google.common.base.MoreObjects;
import io.atomix.storage.journal.index.JournalIndex;
import io.atomix.storage.journal.index.Position;
import io.atomix.storage.journal.index.SparseJournalIndex;
import io.netty.util.internal.PlatformDependent;
import java.io.IOException;
import java.nio.MappedByteBuffer;
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
final class JournalSegment implements AutoCloseable {
  private final JournalSegmentFile file;
  private final JournalSegmentDescriptor descriptor;
  private final StorageLevel storageLevel;
  private final int maxSegmentSize;
  private final int maxEntrySize;
  private final int ioBufferSize;
  private final JournalIndex journalIndex;
  private final Set<JournalSegmentReader> readers = ConcurrentHashMap.newKeySet();
  private final AtomicInteger references = new AtomicInteger();
  private final FileChannel channel;
  private final MappedByteBuffer mappedBuffer;

  private JournalSegmentWriter writer;
  private boolean open = true;

  JournalSegment(
      final JournalSegmentFile file,
      final JournalSegmentDescriptor descriptor,
      final StorageLevel storageLevel,
      final int maxSegmentSize,
      final int maxEntrySize,
      final double indexDensity) {
    this.file = file;
    this.descriptor = descriptor;
    this.storageLevel = storageLevel;
    this.maxSegmentSize = maxSegmentSize;
    this.maxEntrySize = maxEntrySize;
    this.ioBufferSize = ioBufferSize(maxSegmentSize, maxEntrySize);
    journalIndex = new SparseJournalIndex(indexDensity);
    try {
      channel = FileChannel.open(file.file().toPath(),
        StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
      mappedBuffer = storageLevel == StorageLevel.MAPPED
          ? channel.map(FileChannel.MapMode.READ_WRITE, 0, maxSegmentSize) : null;
    } catch (IOException e) {
      throw new StorageException(e);
    }

    final var fileWriter = switch (storageLevel) {
        case DISK -> new DiskFileWriter(channel, ioBufferSize);
        case MAPPED -> new MappedFileWriter(mappedBuffer);
    };
    writer = new JournalSegmentWriter(fileWriter, this, journalIndex);
  }

  /**
   * Returns the segment's starting index.
   *
   * @return The segment's starting index.
   */
  long firstIndex() {
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
   * Returns max entry size.
   *
   * @return max entry size
   */
  int maxEntrySize() {
    return maxEntrySize;
  }

  /**
   * Returns max segment size.
   *
   * @return max segment size
   */
  int maxSegmentSize() {
    return maxSegmentSize;
  }

  /**
   * Looks up the position of the given index.
   *
   * @param index the index to lookup
   * @return the position of the given index or a lesser index, or {@code null}
   */
  @Nullable Position lookup(final long index) {
    return journalIndex.lookup(index);
  }

  /**
   * Acquires a reference to the log segment.
   */
  private void acquire() {
    references.incrementAndGet();
  }

  /**
   * Releases a reference to the log segment.
   */
  private void release() {
    if (references.decrementAndGet() == 0 && !open) {
        finishClose();
    }
  }

  /**
   * Acquires a reference to the segment writer.
   *
   * @return The segment writer.
   */
  JournalSegmentWriter acquireWriter() {
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
  JournalSegmentReader createReader() {
    checkOpen();
    acquire();

    final var fileReader = switch (storageLevel) {
      case DISK -> new DiskFileReader(channel, ioBufferSize);
      case MAPPED -> new MappedFileReader(mappedBuffer);
    };
    final var reader = new JournalSegmentReader(this, fileReader);
    reader.setPosition(JournalSegmentDescriptor.BYTES);
    readers.add(reader);
    return reader;
  }

  /**
   * Closes a segment reader.
   *
   * @param reader the closed segment reader
   */
  void closeReader(JournalSegmentReader reader) {
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
      if (mappedBuffer != null) {
        PlatformDependent.freeDirectBuffer(mappedBuffer);
      }
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
        .add("index", firstIndex())
        .toString();
  }
}
