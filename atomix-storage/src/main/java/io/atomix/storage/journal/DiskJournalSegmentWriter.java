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

import com.esotericsoftware.kryo.KryoException;
import io.atomix.storage.journal.index.JournalIndex;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;

/**
 * Segment writer.
 * <p>
 * The format of an entry in the log is as follows:
 * <ul>
 * <li>64-bit index</li>
 * <li>8-bit boolean indicating whether a term change is contained in the entry</li>
 * <li>64-bit optional term</li>
 * <li>32-bit signed entry length, including the entry type ID</li>
 * <li>8-bit signed entry type ID</li>
 * <li>n-bit entry bytes</li>
 * </ul>
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class DiskJournalSegmentWriter<E> extends JournalSegmentWriter<E> {
  private static final ByteBuffer ZERO_ENTRY_HEADER = ByteBuffer.wrap(new byte[SegmentEntry.HEADER_BYTES]);

  private final ByteBuffer memory;
  private Indexed<E> lastEntry;

  private final DiskSegmentEntryReader reader;

  DiskJournalSegmentWriter(
      final FileChannel channel,
      final JournalSegment<E> segment,
      final int maxEntrySize,
      final JournalIndex index,
      final JournalSerdes namespace) {
    super(channel, segment, maxEntrySize, index, namespace);
    memory = allocMemory(maxEntrySize);
    reader = new DiskSegmentEntryReader(channel, maxSegmentSize, maxEntrySize);
    reset(0);
  }

  DiskJournalSegmentWriter(final JournalSegmentWriter<E> previous, final int position) {
    super(previous);
    memory = allocMemory(maxEntrySize);
    lastEntry = previous.getLastEntry();
    reader = new DiskSegmentEntryReader(channel, maxSegmentSize, maxEntrySize);
    reader.reset(position);
  }

  private static ByteBuffer allocMemory(final int maxEntrySize) {
    final var buf = ByteBuffer.allocate((maxEntrySize + SegmentEntry.HEADER_BYTES) * 2);
    buf.limit(0);
    return buf;
  }

  @Override
  MappedByteBuffer buffer() {
    return null;
  }

  @Override
  MappedJournalSegmentWriter<E> toMapped() {
    return new MappedJournalSegmentWriter<>(this, (int) currentPosition);
  }

  @Override
  DiskJournalSegmentWriter<E> toFileChannel() {
    return this;
  }

  @Override
  void reset(final long index) {
    long nextIndex = firstIndex;
    reader.reset();

    while (index == 0 || nextIndex <= index) {
        final var position = reader.currentPosition();
        final Indexed<E> entry;
        try {
            entry = reader.readNextIndexed(namespace, nextIndex);
        } catch (IOException e) {
            throw new StorageException(e);
        }

        if (entry == null) {
            break;
        }

        this.index.index(nextIndex, (int) position);
        lastEntry = entry;
        nextIndex++;
    }
  }

  @Override
  Indexed<E> getLastEntry() {
    return lastEntry;
  }

  @Override
  @SuppressWarnings("unchecked")
  <T extends E> Indexed<T> append(final T entry) {
    // Store the entry index.
    final long index = getNextIndex();

    // Serialize the entry.
    try {
      namespace.serialize(entry, memory.clear().position(SegmentEntry.HEADER_BYTES));
    } catch (KryoException e) {
      throw new StorageException.TooLarge("Entry size exceeds maximum allowed bytes (" + maxEntrySize + ")");
    }
    memory.flip();

    final int length = memory.limit() - SegmentEntry.HEADER_BYTES;

    // Ensure there's enough space left in the buffer to store the entry.
    if (maxSegmentSize - currentPosition < length + SegmentEntry.HEADER_BYTES) {
      throw new BufferOverflowException();
    }

    // If the entry length exceeds the maximum entry size then throw an exception.
    if (length > maxEntrySize) {
      throw new StorageException.TooLarge("Entry size " + length + " exceeds maximum allowed bytes (" + maxEntrySize + ")");
    }

    // Compute the checksum for the entry.
    final CRC32 crc32 = new CRC32();
    crc32.update(memory.array(), SegmentEntry.HEADER_BYTES, memory.limit() - SegmentEntry.HEADER_BYTES);
    final long checksum = crc32.getValue();

    // Create a single byte[] in memory for the entire entry and write it as a batch to the underlying buffer.
    memory.putInt(0, length).putInt(Integer.BYTES, (int) checksum);
    try {
      channel.write(memory, currentPosition);
    } catch (IOException e) {
      throw new StorageException(e);
    }

    // Update the last entry with the correct index/term/length.
    Indexed<E> indexedEntry = new Indexed<>(index, entry, length);
    lastEntry = indexedEntry;
    this.index.index(index, (int) currentPosition);

    currentPosition = currentPosition + SegmentEntry.HEADER_BYTES + length;
    return (Indexed<T>) indexedEntry;
  }

  @Override
  void truncate(final long index) {
    // If the index is greater than or equal to the last index, skip the truncate.
    if (index >= getLastIndex()) {
      return;
    }

    // Reset the last entry.
    lastEntry = null;

    // Truncate the index.
    this.index.truncate(index);

    try {
      if (index < firstIndex) {
        // Reset the writer to the first entry.
        currentPosition = JournalSegmentDescriptor.BYTES;
      } else {
        // Reset the writer to the given index.
        reset(index);
      }

      // Zero the entry header at current channel position.
      channel.write(ZERO_ENTRY_HEADER.asReadOnlyBuffer(), currentPosition);
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  @Override
  void flush() {
    try {
      if (channel.isOpen()) {
        channel.force(true);
      }
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  @Override
  void close() {
    flush();
  }
}
