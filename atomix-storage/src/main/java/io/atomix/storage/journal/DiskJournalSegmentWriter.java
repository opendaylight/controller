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
import com.google.common.annotations.VisibleForTesting;
import io.atomix.storage.journal.index.JournalIndex;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.zip.CRC32;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Logger LOG = LoggerFactory.getLogger(DiskJournalSegmentWriter.class);
  private static final ByteBuffer ZERO_ENTRY_HEADER = ByteBuffer.wrap(new byte[ENTRY_HEADER_BYTES]);

  private final ByteBuffer memory;
  private Indexed<E> lastEntry;
  private long currentPosition;

  DiskJournalSegmentWriter(
      FileChannel channel,
      JournalSegment<E> segment,
      int maxEntrySize,
      JournalIndex index,
      JournalSerdes namespace) {
    super(channel, segment, maxEntrySize, index, namespace);
    memory = allocMemory(maxEntrySize);
    reset(0);
  }

  DiskJournalSegmentWriter(JournalSegmentWriter<E> previous, int position) {
    super(previous);
    memory = allocMemory(maxEntrySize);
    lastEntry = previous.getLastEntry();
    currentPosition = position;
  }

  private static ByteBuffer allocMemory(int maxEntrySize) {
    final var buf = ByteBuffer.allocate((maxEntrySize + ENTRY_HEADER_BYTES) * 2);
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

      // Clear the buffer indexes.
      currentPosition = JournalSegmentDescriptor.BYTES;

      try {
          // Clear memory buffer and read fist chunk
          channel.read(memory.clear(), JournalSegmentDescriptor.BYTES);
          memory.flip();

          while (index == 0 || nextIndex <= index) {
              final var entry = prepareNextEntry(channel, memory, maxEntrySize);
              if (entry == null) {
                  break;
              }

              final var bytes = entry.bytes();
              final var length = bytes.remaining();
              try {
                  lastEntry = new Indexed<>(nextIndex, namespace.<E>deserialize(bytes), length);
              } catch (KryoException e) {
                  // No-op, position is only updated on success
                  LOG.debug("Failed to serialize entry", e);
                  break;
              }

              this.index.index(nextIndex, (int) currentPosition);
              nextIndex++;

              // Update the current position for indexing.
              currentPosition = currentPosition + ENTRY_HEADER_BYTES + length;
              memory.position(memory.position() + length);
          }
      } catch (IOException e) {
          throw new StorageException(e);
      }
  }

  @VisibleForTesting
  static @Nullable SegmentEntry prepareNextEntry(final SeekableByteChannel channel, final ByteBuffer memory,
          final int maxEntrySize) throws IOException {
      int remaining = memory.remaining();
      boolean compacted;
      if (remaining < ENTRY_HEADER_BYTES) {
          // We do not have the header available. Move the pointer and read.
          channel.read(memory.compact());
          remaining = memory.flip().remaining();
          if (remaining < ENTRY_HEADER_BYTES) {
              // could happen with mis-padded segment
              return null;
          }
          compacted = true;
      } else {
          compacted = false;
      }

      int length;
      while (true) {
          length = memory.mark().getInt();
          if (length < 1 || length > maxEntrySize) {
              // Invalid length,
              memory.reset();
              return null;
          }

          if (remaining >= Integer.BYTES + length) {
              // Fast path: we have the entry properly positioned
              break;
          }

          // Not enough data for entry, to header start
          memory.reset();
          if (compacted) {
              // we have already compacted the buffer, there is just not enough data
              return null;
          }

          // Try to read more data and check again
          channel.read(memory.compact());
          remaining = memory.flip().remaining();
          compacted = true;
      }

      // Read the checksum of the entry.
      final int checksum = memory.getInt();

      // Slice off the entry's bytes
      final var entryBytes = memory.slice();
      entryBytes.limit(length);

      // Compute the checksum for the entry bytes.
      final var crc32 = new CRC32();
      crc32.update(entryBytes);

      // If the stored checksum does not equal the computed checksum, do not proceed further
      final var computed = (int) crc32.getValue();
      if (checksum != computed) {
          LOG.warn("Expected checksum {}, computed {}", Integer.toHexString(checksum), Integer.toHexString(computed));
          memory.reset();
          return null;
      }

      return new SegmentEntry(checksum, entryBytes.rewind());
  }

  @Override
  Indexed<E> getLastEntry() {
    return lastEntry;
  }

  @Override
  @SuppressWarnings("unchecked")
  <T extends E> Indexed<T> append(T entry) {
    // Store the entry index.
    final long index = getNextIndex();

    // Serialize the entry.
    try {
      namespace.serialize(entry, memory.clear().position(ENTRY_HEADER_BYTES));
    } catch (KryoException e) {
      throw new StorageException.TooLarge("Entry size exceeds maximum allowed bytes (" + maxEntrySize + ")");
    }
    memory.flip();

    final int length = memory.limit() - ENTRY_HEADER_BYTES;

    // Ensure there's enough space left in the buffer to store the entry.
    if (maxSegmentSize - currentPosition < length + ENTRY_HEADER_BYTES) {
      throw new BufferOverflowException();
    }

    // If the entry length exceeds the maximum entry size then throw an exception.
    if (length > maxEntrySize) {
      throw new StorageException.TooLarge("Entry size " + length + " exceeds maximum allowed bytes (" + maxEntrySize + ")");
    }

    // Compute the checksum for the entry.
    final CRC32 crc32 = new CRC32();
    crc32.update(memory.array(), ENTRY_HEADER_BYTES, memory.limit() - ENTRY_HEADER_BYTES);
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
    this.lastEntry = indexedEntry;
    this.index.index(index, (int) currentPosition);

    currentPosition = currentPosition + ENTRY_HEADER_BYTES + length;
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
