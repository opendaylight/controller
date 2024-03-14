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

import com.esotericsoftware.kryo.KryoException;
import io.atomix.storage.journal.index.JournalIndex;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
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
final class FileChannelJournalSegmentWriter<E> extends JournalSegmentWriter<E> {
  private static final ByteBuffer ZERO_ENTRY_HEADER = ByteBuffer.wrap(new byte[Integer.BYTES + Integer.BYTES]);

  private final ByteBuffer memory;
  private Indexed<E> lastEntry;
  private long currentPosition;

  FileChannelJournalSegmentWriter(
      FileChannel channel,
      JournalSegment<E> segment,
      int maxEntrySize,
      JournalIndex index,
      JournalSerdes namespace) {
    super(channel, segment, maxEntrySize, index, namespace);
    memory = allocMemory(maxEntrySize);
    reset(0);
  }

  FileChannelJournalSegmentWriter(JournalSegmentWriter<E> previous, int position) {
    super(previous);
    memory = allocMemory(maxEntrySize);
    lastEntry = previous.getLastEntry();
    currentPosition = position;
  }

  private static ByteBuffer allocMemory(int maxEntrySize) {
    final var buf = ByteBuffer.allocate((maxEntrySize + Integer.BYTES + Integer.BYTES) * 2);
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
  FileChannelJournalSegmentWriter<E> toFileChannel() {
    return this;
  }

  @Override
  void reset(long index) {
    long nextIndex = firstIndex;

    // Clear the buffer indexes.
    currentPosition = JournalSegmentDescriptor.BYTES;

    try {
      // Clear memory buffer and read fist chunk
      memory.clear();
      channel.read(memory, JournalSegmentDescriptor.BYTES);
      memory.flip();

      // Read the entry length.
      int length = memory.getInt();

      // If the length is non-zero, read the entry.
      while (0 < length && length <= maxEntrySize && (index == 0 || nextIndex <= index)) {

        // Read the checksum of the entry.
        final long checksum = memory.getInt() & 0xFFFFFFFFL;

        // Slice off the entry's bytes
        final ByteBuffer entryBytes = memory.slice();
        entryBytes.limit(length);

        // Compute the checksum for the entry bytes.
        final CRC32 crc32 = new CRC32();
        crc32.update(entryBytes);

        // If the stored checksum does not equal the computed checksum, do not proceed further
        if (checksum != crc32.getValue()) {
          break;
        }

        entryBytes.rewind();
        final E entry = namespace.deserialize(entryBytes);
        lastEntry = new Indexed<>(nextIndex, entry, length);
        this.index.index(nextIndex, (int) currentPosition);
        nextIndex++;

        // Update the current position for indexing.
        currentPosition = currentPosition + Integer.BYTES + Integer.BYTES + length;
        memory.position(memory.position() + length);

        // Read more bytes from the segment if necessary.
        if (memory.remaining() < maxEntrySize) {
          memory.compact();
          channel.read(memory);
          memory.flip();
        }

        length = memory.getInt();
      }
    } catch (BufferUnderflowException e) {
      // No-op, position is only updated on success
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  @Override
  long getLastIndex() {
    return lastEntry != null ? lastEntry.index() : firstIndex - 1;
  }

  @Override
  Indexed<E> getLastEntry() {
    return lastEntry;
  }

  @Override
  long getNextIndex() {
    if (lastEntry != null) {
      return lastEntry.index() + 1;
    } else {
      return firstIndex;
    }
  }

  @Override
  void append(Indexed<E> entry) {
    final long nextIndex = getNextIndex();

    // If the entry's index is greater than the next index in the segment, skip some entries.
    if (entry.index() > nextIndex) {
      throw new IndexOutOfBoundsException("Entry index is not sequential");
    }

    // If the entry's index is less than the next index, truncate the segment.
    if (entry.index() < nextIndex) {
      truncate(entry.index() - 1);
    }
    append(entry.entry());
  }

  @Override
  @SuppressWarnings("unchecked")
  <T extends E> Indexed<T> append(T entry) {
    // Store the entry index.
    final long index = getNextIndex();

    // Serialize the entry.
    memory.clear();
    memory.position(Integer.BYTES + Integer.BYTES);
    try {
      namespace.serialize(entry, memory);
    } catch (KryoException e) {
      throw new StorageException.TooLarge("Entry size exceeds maximum allowed bytes (" + maxEntrySize + ")");
    }
    memory.flip();

    final int length = memory.limit() - (Integer.BYTES + Integer.BYTES);

    // Ensure there's enough space left in the buffer to store the entry.
    if (maxSegmentSize - currentPosition < length + Integer.BYTES + Integer.BYTES) {
      throw new BufferOverflowException();
    }

    // If the entry length exceeds the maximum entry size then throw an exception.
    if (length > maxEntrySize) {
      throw new StorageException.TooLarge("Entry size " + length + " exceeds maximum allowed bytes (" + maxEntrySize + ")");
    }

    // Compute the checksum for the entry.
    final CRC32 crc32 = new CRC32();
    crc32.update(memory.array(), Integer.BYTES + Integer.BYTES, memory.limit() - (Integer.BYTES + Integer.BYTES));
    final long checksum = crc32.getValue();

    // Create a single byte[] in memory for the entire entry and write it as a batch to the underlying buffer.
    memory.putInt(0, length);
    memory.putInt(Integer.BYTES, (int) checksum);
    try {
      channel.write(memory, currentPosition);
    } catch (IOException e) {
      throw new StorageException(e);
    }

    // Update the last entry with the correct index/term/length.
    Indexed<E> indexedEntry = new Indexed<>(index, entry, length);
    this.lastEntry = indexedEntry;
    this.index.index(index, (int) currentPosition);

    currentPosition = currentPosition + Integer.BYTES + Integer.BYTES + length;
    return (Indexed<T>) indexedEntry;
  }

  @Override
  void truncate(long index) {
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
