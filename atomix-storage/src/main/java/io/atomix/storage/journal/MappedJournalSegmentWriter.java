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
import io.atomix.storage.journal.index.JournalIndex;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;
import org.eclipse.jdt.annotation.NonNull;

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
final class MappedJournalSegmentWriter<E> extends JournalSegmentWriter<E> {
  private final @NonNull MappedByteBuffer mappedBuffer;
  private final ByteBuffer buffer;

  private Indexed<E> lastEntry;
  private int currentPosition;

  MappedJournalSegmentWriter(
      final FileChannel channel,
      final JournalSegment<E> segment,
      final int maxEntrySize,
      final JournalIndex index,
      final JournalSerdes namespace) {
    super(channel, segment, maxEntrySize, index, namespace);
    mappedBuffer = mapBuffer(channel, maxSegmentSize);
    buffer = mappedBuffer.slice();
    reset(0);
  }

  MappedJournalSegmentWriter(final JournalSegmentWriter<E> previous, final int position) {
    super(previous);
    mappedBuffer = mapBuffer(channel, maxSegmentSize);
    buffer = mappedBuffer.slice();
    currentPosition = position;
    lastEntry = previous.getLastEntry();
  }

  private static @NonNull MappedByteBuffer mapBuffer(final FileChannel channel, final int maxSegmentSize) {
    try {
      return channel.map(FileChannel.MapMode.READ_WRITE, 0, maxSegmentSize);
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  @Override
  @NonNull MappedByteBuffer buffer() {
    return mappedBuffer;
  }

  @Override
  MappedJournalSegmentWriter<E> toMapped() {
    return this;
  }

  @Override
  DiskJournalSegmentWriter<E> toFileChannel() {
    close();
    return new DiskJournalSegmentWriter<>(this, currentPosition);
  }

  @Override
  void reset(final long index) {
    long nextIndex = firstIndex;

    // Clear the buffer indexes.
    currentPosition = JournalSegmentDescriptor.BYTES;

    int length = buffer.getInt(currentPosition);

    // If the length is non-zero, read the entry.
    while (0 < length && length <= maxEntrySize && (index == 0 || nextIndex <= index)) {

      // Read the checksum of the entry.
      final long checksum = buffer.getInt(currentPosition + Integer.BYTES);

      // Slice off the entry's bytes
      final var entryBytes = buffer.slice(currentPosition + SegmentEntry.HEADER_BYTES, length);

      // Compute the checksum for the entry bytes.
      final var crc32 = new CRC32();
      crc32.update(entryBytes);

      // If the stored checksum does not equal the computed checksum, do not proceed further
      if (checksum != (int) crc32.getValue()) {
          break;
      }

      entryBytes.rewind();
      final E entry = namespace.deserialize(entryBytes);
      lastEntry = new Indexed<>(nextIndex, entry, length);
      this.index.index(nextIndex, currentPosition);
      nextIndex++;

      // Update the current position for indexing.
      currentPosition = currentPosition + SegmentEntry.HEADER_BYTES + length;

      if (currentPosition + SegmentEntry.HEADER_BYTES >= maxSegmentSize) {
          break;
      }
      length = buffer.getInt(currentPosition);
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
    final int bodyPosition = currentPosition + HEADER_BYTES;
    final int avail = maxSegmentSize - bodyPosition;
    if (avail < 0) {
      throw new BufferOverflowException();
    }

    final var entryBytes = buffer.slice(bodyPosition, Math.min(avail, maxEntrySize));
    try {
      namespace.serialize(entry, entryBytes);
    } catch (KryoException e) {
      if (entryBytes.capacity() != maxEntrySize) {
        // We have not provided enough capacity, signal to roll to next segment
        throw new BufferOverflowException();
      }

      // Just reset the buffer. There's no need to zero the bytes since we haven't written the length or checksum.
      throw new StorageException.TooLarge("Entry size exceeds maximum allowed bytes (" + maxEntrySize + ")");
    }

    final int length = entryBytes.position();

    // Compute the checksum for the entry.
    final var crc32 = new CRC32();
    crc32.update(entryBytes.flip());

    // Create a single byte[] in memory for the entire entry and write it as a batch to the underlying buffer.
    buffer.putInt(currentPosition, length).putInt(currentPosition + Integer.BYTES, (int) crc32.getValue());

    // Update the last entry with the correct index/term/length.
    Indexed<E> indexedEntry = new Indexed<>(index, entry, length);
    lastEntry = indexedEntry;
    this.index.index(index, currentPosition);

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

    if (index < firstIndex) {
      // Reset the writer to the first entry.
      currentPosition = JournalSegmentDescriptor.BYTES;
    } else {
      // Reset the writer to the given index.
      reset(index);
    }

    // Zero the entry header at current buffer position.
    // Note: we issue a single putLong() instead of two putInt()s.
    buffer.putLong(currentPosition, 0L);
  }

  @Override
  void flush() {
    mappedBuffer.force();
  }

  @Override
  void close() {
    flush();
    try {
      BufferCleaner.freeBuffer(mappedBuffer);
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }
}
