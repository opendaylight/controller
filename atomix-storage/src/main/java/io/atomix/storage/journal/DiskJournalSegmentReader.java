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

import io.atomix.storage.journal.index.JournalIndex;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;

/**
 * Log segment reader.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class DiskJournalSegmentReader<E> extends JournalSegmentReader<E> {
  private final FileChannel channel;
  private final ByteBuffer memory;
  private long currentPosition;

  DiskJournalSegmentReader(
      FileChannel channel,
      JournalSegment<E> segment,
      int maxEntrySize,
      JournalIndex index,
      JournalSerdes namespace) {
    super(segment, maxEntrySize, index, namespace);
    this.channel = channel;
    this.memory = ByteBuffer.allocate((maxEntrySize + JournalSegmentWriter.ENTRY_HEADER_BYTES) * 2);
    reset();
  }

  @Override
  void setPosition(int position) {
    currentPosition = position;
    memory.clear().flip();
  }

  @Override
  Indexed<E> readEntry(final long index) {
    try {
      // Read more bytes from the segment if necessary.
      if (memory.remaining() < maxEntrySize) {
        long position = currentPosition + memory.position();
        channel.read(memory.clear(), position);
        currentPosition = position;
        memory.flip();
      }

      // Mark the buffer so it can be reset if necessary.
      memory.mark();

      try {
        // Read the length of the entry.
        final int length = memory.getInt();

        // If the buffer length is zero then return.
        if (length <= 0 || length > maxEntrySize) {
          memory.reset().limit(memory.position());
          return null;
        }

        // Read the checksum of the entry.
        long checksum = memory.getInt() & 0xFFFFFFFFL;

        // Compute the checksum for the entry bytes.
        final CRC32 crc32 = new CRC32();
        crc32.update(memory.array(), memory.position(), length);

        // If the stored checksum equals the computed checksum, return the entry.
        if (checksum == crc32.getValue()) {
          int limit = memory.limit();
          memory.limit(memory.position() + length);
          E entry = namespace.deserialize(memory);
          memory.limit(limit);
          return new Indexed<>(index, entry, length);
        } else {
          memory.reset().limit(memory.position());
          return null;
        }
      } catch (BufferUnderflowException e) {
        memory.reset().limit(memory.position());
        return null;
      }
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }
}
