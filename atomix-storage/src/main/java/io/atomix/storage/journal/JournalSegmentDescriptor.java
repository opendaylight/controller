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

import com.google.common.annotations.VisibleForTesting;

import java.nio.ByteBuffer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

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
public record JournalSegmentDescriptor(
        long id,
        long index) {
  public static final int BYTES = 64;

  // Current segment version.
  @VisibleForTesting
  static final int VERSION = 1;

  // The lengths of each field in the header.
  private static final int VERSION_LENGTH = Integer.BYTES;     // 32-bit signed integer
  private static final int ID_LENGTH = Long.BYTES;             // 64-bit signed integer

  // The positions of each field in the header.
  private static final int VERSION_POSITION = 0;                                         // 0
  private static final int ID_POSITION = VERSION_POSITION + VERSION_LENGTH;              // 4
  private static final int INDEX_POSITION = ID_POSITION + ID_LENGTH;                     // 12

  /**
   * Creates a buffer from descriptor.
   */
  public ByteBuffer asBuffer() {
    ByteBuffer buffer = ByteBuffer.allocate(JournalSegmentDescriptor.BYTES);
    buffer.putLong(ID_POSITION, id);
    buffer.putLong(INDEX_POSITION, index);
    return buffer;
  }

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
