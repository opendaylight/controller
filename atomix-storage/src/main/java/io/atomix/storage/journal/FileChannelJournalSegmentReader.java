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

import io.atomix.storage.journal.index.JournalIndex;
import io.atomix.storage.journal.index.Position;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.NoSuchElementException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Log segment reader.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class FileChannelJournalSegmentReader<E> extends JournalSegmentReader<E> {
  private final FileChannel channel;
  private final ByteBuffer memory;
  private Indexed<E> currentEntry;
  private Indexed<E> nextEntry;
  private long currentPosition;

  FileChannelJournalSegmentReader(
      FileChannel channel,
      JournalSegment<E> segment,
      int maxEntrySize,
      JournalIndex index,
      JournalSerdes namespace) {
    super(segment, maxEntrySize, index, namespace);
    this.channel = channel;
    this.memory = ByteBuffer.allocate((maxEntrySize + Integer.BYTES + Integer.BYTES) * 2);
    reset();
  }

  @Override
  public long getCurrentIndex() {
    return currentEntry != null ? currentEntry.index() : 0;
  }

  @Override
  public Indexed<E> getCurrentEntry() {
    return currentEntry;
  }

  @Override
  public long getNextIndex() {
    return currentEntry != null ? currentEntry.index() + 1 : firstIndex;
  }

  @Override
  public void reset(long index) {
    reset();
    Position position = this.index.lookup(index - 1);
    if (position != null) {
      currentEntry = new Indexed<>(position.index() - 1, null, 0);
      currentPosition = position.position();
      memory.clear().flip();
      readNext();
    }
    while (getNextIndex() < index && hasNext()) {
      next();
    }
  }

  @Override
  public void reset() {
    currentPosition = JournalSegmentDescriptor.BYTES;
    memory.clear().limit(0);
    currentEntry = null;
    nextEntry = null;
    readNext();
  }

  @Override
  public boolean hasNext() {
    // If the next entry is null, check whether a next entry exists.
    if (nextEntry == null) {
      readNext();
    }
    return nextEntry != null;
  }

  @Override
  public Indexed<E> next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    // Set the current entry to the next entry.
    currentEntry = nextEntry;

    // Reset the next entry to null.
    nextEntry = null;

    // Read the next entry in the segment.
    readNext();

    // Return the current entry.
    return currentEntry;
  }

  /**
   * Reads the next entry in the segment.
   */
  private void readNext() {
    // Compute the index of the next entry in the segment.
    final long index = getNextIndex();

    try {
      // Read more bytes from the segment if necessary.
      if (memory.remaining() < maxEntrySize) {
        long position = currentPosition + memory.position();
        memory.clear();
        channel.read(memory, position);
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
          nextEntry = null;
          return;
        }

        // Read the checksum of the entry.
        long checksum = memory.getInt() & 0xFFFFFFFFL;

        // Compute the checksum for the entry bytes.
        final Checksum crc32 = new CRC32();
        crc32.update(memory.array(), memory.position(), length);

        // If the stored checksum equals the computed checksum, return the entry.
        if (checksum == crc32.getValue()) {
          int limit = memory.limit();
          memory.limit(memory.position() + length);
          E entry = namespace.deserialize(memory);
          memory.limit(limit);
          nextEntry = new Indexed<>(index, entry, length);
        } else {
          memory.reset().limit(memory.position());
          nextEntry = null;
        }
      } catch (BufferUnderflowException e) {
        memory.reset().limit(memory.position());
        nextEntry = null;
      }
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }
}
