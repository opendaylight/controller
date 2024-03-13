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

import com.esotericsoftware.kryo.KryoException;
import io.atomix.storage.journal.index.JournalIndex;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
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
final class MappedJournalSegmentWriter<E> extends JournalSegmentWriter<E> {
  private final ByteBuffer buffer;

  private Indexed<E> lastEntry;

  MappedJournalSegmentWriter(
      ByteBuffer buffer,
      JournalSegment<E> segment,
      int maxEntrySize,
      JournalIndex index,
      JournalSerdes namespace) {
    super(segment, maxEntrySize, index, namespace);
    this.buffer = requireNonNull(buffer);
    reset(0);
  }

  MappedJournalSegmentWriter(JournalSegmentWriter<E> previous, ByteBuffer buffer, int position) {
    super(previous);
    this.buffer = requireNonNull(buffer);
    lastEntry = previous.getLastEntry();
    buffer.position(position);
  }

  @Override
  void reset(long index) {
    long nextIndex = firstIndex;

    // Clear the buffer indexes.
    buffer.position(JournalSegmentDescriptor.BYTES);

    // Record the current buffer position.
    int position = buffer.position();

    // Read the entry length.
    buffer.mark();

    try {
      int length = buffer.getInt();

      // If the length is non-zero, read the entry.
      while (0 < length && length <= maxEntrySize && (index == 0 || nextIndex <= index)) {

        // Read the checksum of the entry.
        final long checksum = buffer.getInt() & 0xFFFFFFFFL;

        // Slice off the entry's bytes
        final ByteBuffer entryBytes = buffer.slice();
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
        this.index.index(nextIndex, position);
        nextIndex++;

        // Update the current position for indexing.
        position = buffer.position() + length;
        buffer.position(position);

        buffer.mark();
        length = buffer.getInt();
      }

      // Reset the buffer to the previous mark.
      buffer.reset();
    } catch (BufferUnderflowException e) {
      buffer.reset();
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
  @SuppressWarnings("unchecked")
  <T extends E> Indexed<T> append(T entry) {
    // Store the entry index.
    final long index = getNextIndex();

    // Serialize the entry.
    int position = buffer.position();
    if (position + Integer.BYTES + Integer.BYTES > buffer.limit()) {
      throw new BufferOverflowException();
    }

    buffer.position(position + Integer.BYTES + Integer.BYTES);

    try {
      namespace.serialize(entry, buffer);
    } catch (KryoException e) {
      throw new BufferOverflowException();
    }

    final int length = buffer.position() - (position + Integer.BYTES + Integer.BYTES);

    // If the entry length exceeds the maximum entry size then throw an exception.
    if (length > maxEntrySize) {
      // Just reset the buffer. There's no need to zero the bytes since we haven't written the length or checksum.
      buffer.position(position);
      throw new StorageException.TooLarge("Entry size " + length + " exceeds maximum allowed bytes (" + maxEntrySize + ")");
    }

    // Compute the checksum for the entry.
    final CRC32 crc32 = new CRC32();
    buffer.position(position + Integer.BYTES + Integer.BYTES);
    ByteBuffer slice = buffer.slice();
    slice.limit(length);
    crc32.update(slice);
    final long checksum = crc32.getValue();

    // Create a single byte[] in memory for the entire entry and write it as a batch to the underlying buffer.
    buffer.position(position);
    buffer.putInt(length);
    buffer.putInt((int) checksum);
    buffer.position(position + Integer.BYTES + Integer.BYTES + length);

    // Update the last entry with the correct index/term/length.
    Indexed<E> indexedEntry = new Indexed<>(index, entry, length);
    this.lastEntry = indexedEntry;
    this.index.index(index, position);
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

    if (index < firstIndex) {
      // Reset the writer to the first entry.
      buffer.position(JournalSegmentDescriptor.BYTES);
    } else {
      // Reset the writer to the given index.
      reset(index);
    }

    // Zero the entry header at current buffer position.
    int position = buffer.position();
    // Note: we issue a single putLong() instead of two putInt()s.
    buffer.putLong(0);
    buffer.position(position);
  }

  @Override
  void flush() {
    access.syncBuffer();
  }

  @Override
  void close() {
    flush();
    buffer = null;
    final var toRelease = access;
    access = null;
    toRelease.release();
  }
}
