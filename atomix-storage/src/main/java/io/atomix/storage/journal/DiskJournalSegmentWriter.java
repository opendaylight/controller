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

import static io.atomix.storage.journal.SegmentEntry.HEADER_BYTES;

import com.esotericsoftware.kryo.KryoException;
import com.google.common.annotations.VisibleForTesting;
import io.atomix.storage.journal.StorageException.TooLarge;
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
  private static final ByteBuffer ZERO_ENTRY_HEADER = ByteBuffer.wrap(new byte[HEADER_BYTES]);

  private final JournalSegmentReader<E> reader;
  private final ByteBuffer buffer;

  private Indexed<E> lastEntry;
  private long currentPosition;

  DiskJournalSegmentWriter(final FileChannel channel, final JournalSegment<E> segment, final int maxEntrySize,
          final JournalIndex index, final JournalSerdes namespace) {
    super(channel, segment, maxEntrySize, index, namespace);

    buffer = DiskFileReader.allocateBuffer(maxSegmentSize, maxEntrySize);
    final var fileReader = new DiskFileReader(segment.file().file().toPath(), channel, maxSegmentSize, maxEntrySize);
    reader = new JournalSegmentReader<>(segment, fileReader, maxEntrySize, namespace);
    reset(0);
  }

  DiskJournalSegmentWriter(final JournalSegmentWriter<E> previous, final int position) {
    super(previous);

    buffer = DiskFileReader.allocateBuffer(maxSegmentSize, maxEntrySize);
    final var fileReader = new DiskFileReader(segment.file().file().toPath(), channel, maxSegmentSize, maxEntrySize);
    reader = new JournalSegmentReader<>(segment, fileReader, maxEntrySize, namespace);
    lastEntry = previous.getLastEntry();
    currentPosition = position;
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
      // acquire ownership of cache and make sure reader does not see anything we've done once we're done
      reader.invalidateCache();
      try {
          resetWithBuffer(index);
      } finally {
          // Make sure reader does not see anything we've done
          reader.invalidateCache();
      }
  }

  private void resetWithBuffer(final long index) {
      long nextIndex = firstIndex;

      // Clear the buffer indexes and acquire ownership of the buffer
      currentPosition = JournalSegmentDescriptor.BYTES;
      reader.setPosition(JournalSegmentDescriptor.BYTES);

      while (index == 0 || nextIndex <= index) {
          final var entry = reader.readEntry(nextIndex);
          if (entry == null) {
              break;
          }

          lastEntry = entry;
          this.index.index(nextIndex, (int) currentPosition);
          nextIndex++;

          // Update the current position for indexing.
          currentPosition = currentPosition + HEADER_BYTES + entry.size();
      }
  }

  @VisibleForTesting
  static @Nullable SegmentEntry prepareNextEntry(final SeekableByteChannel channel, final ByteBuffer memory,
          final int maxEntrySize) throws IOException {
      int remaining = memory.remaining();
      boolean compacted;
      if (remaining < HEADER_BYTES) {
          // We do not have the header available. Move the pointer and read.
          channel.read(memory.compact());
          remaining = memory.flip().remaining();
          if (remaining < HEADER_BYTES) {
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
  <T extends E> Indexed<T> append(final T entry) {
      // Store the entry index.
      final long index = getNextIndex();

      // Serialize the entry.
      try {
          namespace.serialize(entry, buffer.clear().position(HEADER_BYTES));
      } catch (KryoException e) {
          throw new StorageException.TooLarge("Entry size exceeds maximum allowed bytes (" + maxEntrySize + ")");
      }
      buffer.flip();

      final int length = buffer.limit() - HEADER_BYTES;
      // Ensure there's enough space left in the buffer to store the entry.
      if (maxSegmentSize - currentPosition < length + HEADER_BYTES) {
          throw new BufferOverflowException();
      }

      // If the entry length exceeds the maximum entry size then throw an exception.
      if (length > maxEntrySize) {
          throw new TooLarge("Entry size " + length + " exceeds maximum allowed bytes (" + maxEntrySize + ")");
      }

      // Compute the checksum for the entry.
      final var crc32 = new CRC32();
      crc32.update(buffer.slice(HEADER_BYTES, length));

      // Create a single byte[] in memory for the entire entry and write it as a batch to the underlying buffer.
      buffer.putInt(0, length).putInt(Integer.BYTES, (int) crc32.getValue());
      try {
          channel.write(buffer, currentPosition);
      } catch (IOException e) {
          throw new StorageException(e);
      }

      // Update the last entry with the correct index/term/length.
      final var indexedEntry = new Indexed<E>(index, entry, length);
      lastEntry = indexedEntry;
      this.index.index(index, (int) currentPosition);

      currentPosition = currentPosition + HEADER_BYTES + length;
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
