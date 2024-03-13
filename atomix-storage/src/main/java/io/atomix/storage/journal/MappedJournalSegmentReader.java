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

import io.atomix.storage.journal.index.JournalIndex;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * Log segment reader.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class MappedJournalSegmentReader<E> extends JournalSegmentReader<E> {
  private final ByteBuffer buffer;

  MappedJournalSegmentReader(
      ByteBuffer buffer,
      JournalSegment<E> segment,
      int maxEntrySize,
      JournalIndex index,
      JournalSerdes namespace) {
    super(segment, maxEntrySize, index, namespace);
    this.buffer = requireNonNull(buffer);
    reset();
  }

  @Override
  void setPosition(int position) {
    buffer.position(position);
  }

  @Override
  Indexed<E> readEntry(final long index) {
    // Mark the buffer so it can be reset if necessary.
    buffer.mark();

    try {
      // Read the length of the entry.
      final int length = buffer.getInt();

      // If the buffer length is zero then return.
      if (length <= 0 || length > maxEntrySize) {
        buffer.reset();
        return null;
      }

      // Read the checksum of the entry.
      long checksum = buffer.getInt() & 0xFFFFFFFFL;

      // Compute the checksum for the entry bytes.
      final CRC32 crc32 = new CRC32();
      ByteBuffer slice = buffer.slice();
      slice.limit(length);
      crc32.update(slice);

      // If the stored checksum equals the computed checksum, return the entry.
      if (checksum == crc32.getValue()) {
        slice.rewind();
        E entry = namespace.deserialize(slice);
        buffer.position(buffer.position() + length);
        return new Indexed<>(index, entry, length);
      } else {
        buffer.reset();
        return null;
      }
    } catch (BufferUnderflowException e) {
      buffer.reset();
      return null;
    }
  }
}
