/*
 * Copyright 2015-2022 Open Networking Foundation and others.  All rights reserved.
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

import static com.google.common.base.MoreObjects.toStringHelper;

import java.nio.ByteBuffer;

/**
 * Stores information about a {@link JournalSegment} of the log.
 * <p>
 * The segment descriptor manages metadata related to a single segment of the log. Descriptors are stored within the
 * first {@code 64} bytes of each segment in the following order:
 * <ul>
 * <li>{@code id} (64-bit signed integer) - A unique segment identifier. This is a monotonically increasing number within
 * each log. Segments with in-sequence identifiers should contain in-sequence indexes.</li>
 * <li>{@code index} (64-bit signed integer) - The effective first index of the segment. This indicates the index at which
 * the first entry should be written to the segment. Indexes are monotonically increasing thereafter.</li>
 * </ul>
 * The remainder of the 64 segment header bytes are reserved for other metadata.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public record JournalSegmentDescriptor(long id, long index) {
  public static final int BYTES = 64;

  // The positions of fields in the header.
  private static final int ID_POSITION = 4;
  private static final int INDEX_POSITION = 12;

  /**
   * Creates a buffer from descriptor.
   *
   * @return {@link ByteBuffer} from descriptor.
   */
  public ByteBuffer toByteBuffer() {
    final var buffer = ByteBuffer.allocate(JournalSegmentDescriptor.BYTES);
    buffer.putLong(ID_POSITION, id);
    buffer.putLong(INDEX_POSITION, index);
    return buffer;
  }

  /**
   * Creates a descriptor from buffer.
   * @param buffer segment buffer.
   * @return result descriptor.
   */
  public static JournalSegmentDescriptor fromBuffer(ByteBuffer buffer) {
    return new JournalSegmentDescriptor(buffer.getLong(ID_POSITION), buffer.getLong(INDEX_POSITION));
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .add("id", id)
        .add("index", index)
        .toString();
  }
}
