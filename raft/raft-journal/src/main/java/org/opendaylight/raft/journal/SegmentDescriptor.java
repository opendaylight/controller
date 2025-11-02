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
package org.opendaylight.raft.journal;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Stores information about a {@link Segment} of the log. The segment descriptor manages metadata related to a single
 * segment of the log. Descriptors are stored within the first {@code 64} bytes of each segment in the following order:
 * <ul>
 *   <li>{@code id} (64-bit signed integer) - A unique segment identifier. This is a monotonically increasing number
 *       within each log. Segments with in-sequence identifiers should contain in-sequence indexes.</li>
 *   <li>{@code index} (64-bit signed integer) - The effective first index of the segment. This indicates the index at
 *       which the first entry should be written to the segment. Indexes are monotonically increasing thereafter.</li>
 *   <li>{@code version} (64-bit signed integer) - The version of the segment. Versions are monotonically increasing
 *       starting at {@code 1}. Versions will only be incremented whenever the segment is rewritten to another
 *       memory/disk space, e.g. after log compaction.</li>
 *   <li>{@code maxSegmentSize} (32-bit unsigned integer) - The maximum number of bytes allowed in the segment.</li>
 *   <li>{@code maxEntries} (32-bit signed integer) - The total number of expected entries in the segment. This is the
 *       final number of entries allowed within the segment both before and after compaction. This entry count is used
 *       to determine the count of internal indexing and deduplication facilities.</li>
 *   <li>{@code updated} (64-bit signed integer) - The last update to the segment in terms of milliseconds since the
 *       epoch.
 *       When the segment is first constructed, the {@code updated} time is {@code 0}. Once all entries in the segment
 *       have been committed, the {@code updated} time should be set to the current time. Log compaction should not
 *       result in a change to {@code updated}.</li>
 *   <li>{@code locked} (8-bit boolean) - A boolean indicating whether the segment is locked. Segments will be locked
 *       once all entries have been committed to the segment. The lock state of each segment is used to determine log
 *       compaction and recovery behavior.</li>
 * </ul>
 * The remainder of the 64 segment header bytes are reserved for future metadata.
 *
 * @param version the version
 * @param id segment identifier
 * @param index first index stored in this segment
 * @param maxSegmentSize maximum size of a single segment file
 * @param maxEntries maximum number of entries
 * @param updated last updated, as epoch milliseconds
 * @param locked true if the segment is locked
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public record SegmentDescriptor(
        int version,
        long id,
        long index,
        int maxSegmentSize,
        int maxEntries,
        long updated,
        boolean locked) {
    public static final int BYTES = 64;

    // Current segment version.
    @VisibleForTesting
    static final int VERSION = 1;

    /**
     * Read a JournalSegmentDescriptor from a {@link ReadableByteChannel}.
     *
     * @param channel channel to read from
     * @return A {@link SegmentDescriptor}
     * @throws IOException if an I/O error occurs or there is not enough data
     */
    public static @NonNull SegmentDescriptor readFrom(final ReadableByteChannel channel) throws IOException {
        final var buffer = ByteBuffer.allocate(BYTES);
        final var read = channel.read(buffer);
        if (read != BYTES) {
            throw new IOException("Need " + BYTES + " bytes, only " + read + " available");
        }

        buffer.flip();
        return new SegmentDescriptor(
            buffer.getInt(),
            buffer.getLong(),
            buffer.getLong(),
            buffer.getInt(),
            buffer.getInt(),
            buffer.getLong(),
            buffer.get() == 1);
    }

    /**
     * Returns the segment version. Versions are monotonically increasing starting at {@code 1}.
     *
     * @return The segment version.
     */
    @Override
    public int version() {
        return version;
    }

    /**
     * Returns the segment identifier. The segment ID is a monotonically increasing number within each log. Segments
     * with in-sequence identifiers should contain in-sequence indexes.
     *
     * @return The segment identifier.
     */
    @Override
    public long id() {
        return id;
    }

    /**
     * Returns the segment index. The index indicates the index at which the first entry should be written to the
     * segment. Indexes are monotonically increasing thereafter.
     *
     * @return The segment index.
     */
    @Override
    public long index() {
        return index;
    }

    /**
     * Returns the maximum count of the segment.
     *
     * @return The maximum allowed count of the segment.
     */
    @Override
    public int maxSegmentSize() {
        return maxSegmentSize;
    }

    /**
     * Returns the maximum number of entries allowed in the segment.
     *
     * @return The maximum number of entries allowed in the segment.
     */
    @Override
    public int maxEntries() {
        return maxEntries;
    }

    /**
     * Returns last time the segment was updated.
     *
     * <p>When the segment is first constructed, the {@code updated} time is {@code 0}. Once all entries in the segment
     * have been committed, the {@code updated} time should be set to the current time. Log compaction should not result
     * in a change to {@code updated}.
     *
     * @return The last time the segment was updated in terms of milliseconds since the epoch.
     */
    @Override
    public long updated() {
        return updated;
    }

    /**
     * Returns this segment as an array of bytes.
     *
     * @return bytes
     */
    byte @NonNull [] toArray() {
        final var bytes = new byte[BYTES];
        ByteBuffer.wrap(bytes)
            .putInt(version)
            .putLong(id)
            .putLong(index)
            .putInt(maxSegmentSize)
            .putInt(maxEntries)
            .putLong(updated)
            .put(locked ? (byte) 1 : (byte) 0);
        return bytes;
    }

    /**
     * Returns a descriptor builder. The descriptor builder will write segment metadata to a {@code 48} byte in-memory
     * buffer.
     *
     * @return The descriptor builder.
     */
    public static Builder builder() {
        return builder(VERSION);
    }

    /**
     * Returns a descriptor builder for the given descriptor buffer.
     *
     * @param version version to build
     * @return The descriptor builder.
     * @throws NullPointerException if {@code buffer} is null
     */
    public static Builder builder(final int version) {
        return new Builder(version);
    }

    /**
     * Segment descriptor builder.
     */
    public static final class Builder {
        private final int version;

        private Long id;
        private Long index;
        private Integer maxSegmentSize;
        private Integer maxEntries;
        private Long updated;

        Builder(final int version) {
            this.version = version;
        }

        /**
         * Sets the segment identifier.
         *
         * @param id The segment identifier.
         * @return The segment descriptor builder.
         */
        @SuppressWarnings("checkstyle:hiddenField")
        public Builder withId(final long id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the segment index.
         *
         * @param index The segment starting index.
         * @return The segment descriptor builder.
         */
        @SuppressWarnings("checkstyle:hiddenField")
        public Builder withIndex(final long index) {
            this.index = index;
            return this;
        }

        /**
         * Sets maximum count of the segment.
         *
         * @param maxSegmentSize The maximum count of the segment.
         * @return The segment descriptor builder.
         */
        @SuppressWarnings("checkstyle:hiddenField")
        public Builder withMaxSegmentSize(final int maxSegmentSize) {
            this.maxSegmentSize = maxSegmentSize;
            return this;
        }

        /**
         * Sets the maximum number of entries in the segment.
         *
         * @param maxEntries The maximum number of entries in the segment.
         * @return The segment descriptor builder.
         * @deprecated since 3.0.2
         */
        @Deprecated
        @SuppressWarnings("checkstyle:hiddenField")
        public Builder withMaxEntries(final int maxEntries) {
            this.maxEntries = maxEntries;
            return this;
        }

        /**
         * Sets updated timestamp.
         *
         * @param updated Epoch milliseconds
         * @return The segment descriptor builder.
         */
        @SuppressWarnings("checkstyle:hiddenField")
        public Builder withUpdated(final long updated) {
            this.updated = updated;
            return this;
        }

        /**
         * Builds the segment descriptor.
         *
         * @return The built segment descriptor.
         */
        public SegmentDescriptor build() {
            return new SegmentDescriptor(version,
                checkSet(id, "id"),
                checkSet(index, "index"),
                checkSet(maxSegmentSize, "maxSegmentSize"),
                checkSet(maxEntries, "maxEntries"),
                checkSet(updated, "updated"),
                false);
        }

        private static <T> @NonNull T checkSet(final @Nullable T obj, final String name) {
            if (obj != null) {
                return obj;
            }
            throw new IllegalArgumentException(name + " not set");
        }
    }
}
