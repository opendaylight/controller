/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.checkFromIndexSize;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read-side access to {@link Indexed} entries stored in a {@link JournalSegment}.
 */
final class DiskSegmentEntryReader {
    private static final Logger LOG = LoggerFactory.getLogger(DiskSegmentEntryReader.class);

    private final FileChannel channel;
    private final ByteBuffer memory;
    private final int maxSegmentSize;

    private long currentPosition;

    DiskSegmentEntryReader(final FileChannel channel, final int maxSegmentSize, final int maxEntrySize) {
        this.channel = requireNonNull(channel);
        if (maxEntrySize > maxSegmentSize) {
            throw new IllegalArgumentException(
                "Mismatched entry " + maxEntrySize + "and segment " + maxSegmentSize + " size");
        }
        this.maxSegmentSize = maxSegmentSize;
        memory = ByteBuffer.allocate((maxEntrySize + SegmentEntry.HEADER_BYTES) * 2);
        reset();
    }

    long currentPosition() {
        return currentPosition;
    }

    void reset() {
        reset(JournalSegmentDescriptor.BYTES);
    }

    void reset(final long newPosition) {
        checkFromIndexSize(JournalSegmentDescriptor.BYTES, maxSegmentSize, newPosition);
        currentPosition = newPosition;
        memory.clear().flip();
    }

    @Nullable SegmentEntry readNextEntry() throws IOException {
        int remaining = memory.remaining();
        boolean compacted;
        if (remaining < SegmentEntry.HEADER_BYTES) {
            // We do not have the header available. Move the pointer and read.
            channel.read(memory.compact());
            remaining = memory.flip().remaining();
            if (remaining < SegmentEntry.HEADER_BYTES) {
                // could happen with mis-padded segment
                return null;
            }
            compacted = true;
        } else {
            compacted = false;
        }

        int position = memory.position();
        final int length = memory.getInt(position);
        final var need = Integer.BYTES + length;
        if (need > remaining) {
            if (compacted) {
                // we have already compacted the buffer, there is just not enough data
                return null;
            }

            // Try to read more data and check again
            channel.read(memory.compact());
            remaining = memory.flip().remaining();
            if (need > remaining) {
                return null;
            }
            position = memory.position();
        }

        // Read the checksum of the entry.
        final int checksum = memory.getInt(position + Integer.BYTES);
        final var bytes = memory.slice(position + SegmentEntry.HEADER_BYTES, length).asReadOnlyBuffer();

        // compute checksum
        final var crc32 = new CRC32();
        crc32.update(bytes);
        // If the stored checksum does not equal the computed checksum, do not proceed further
        if (checksum != (int) crc32.getValue()) {
            return null;
        }

        // Move to the end of this entry
        final var seekSize = SegmentEntry.HEADER_BYTES + length;
        memory.position(position + seekSize);
        currentPosition += seekSize;

        return new SegmentEntry(checksum, bytes.rewind());
    }

    <E> @Nullable Indexed<E> readNextIndexed(final JournalSerdes serdes, final long index) throws IOException {
        final var position = currentPosition;
        final var segmentEntry = readNextEntry();
        if (segmentEntry == null) {
            return null;
        }

        final var entryBytes = segmentEntry.bytes();
        final E entry;
        try {
            entry = serdes.deserialize(entryBytes);
        } catch (RuntimeException e) {
            LOG.warn("Failed to deserialize {}", segmentEntry, e);
            reset(position);
            return null;
        }

        final var indexed = new Indexed<>(index, entry, entryBytes.limit());
        currentPosition = position;
        return indexed;
    }
}
