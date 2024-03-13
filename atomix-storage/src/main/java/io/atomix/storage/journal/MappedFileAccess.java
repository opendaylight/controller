/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import io.atomix.storage.journal.index.JournalIndex;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link FileAccess} for {@link StorageLevel#MAPPED}.
 */
final class MappedFileAccess extends FileAccess {
    private static final Logger LOG = LoggerFactory.getLogger(MappedFileAccess.class);

    private MappedByteBuffer buffer;

    MappedFileAccess(final FileChannel channel, final long size) throws IOException {
        buffer = channel.map(MapMode.READ_WRITE, 0, size);
    }

    @Override
    <E> JournalSegmentWriter<E> createInitialWriter(final JournalSegment<E> segment, final int maxEntrySize,
            final JournalIndex index, final JournalSerdes namespace) {
        return new MappedJournalSegmentWriter<>(buffer.slice(), segment, maxEntrySize, index, namespace);
    }
    
    @Override
    <E> JournalSegmentReader<E> createReader(final JournalSegment<E> segment, final int maxEntrySize,
            final JournalIndex index, final JournalSerdes namespace) {
        return new MappedJournalSegmentReader<>(buffer.slice(), segment, maxEntrySize, index, namespace);
    }

    @Override
    void cleanup() {
        final var toFree = buffer;
        buffer = null;
        if (toFree != null) {
            try {
                BufferCleaner.freeBuffer(toFree);
            } catch (IOException e) {
                LOG.warn("Failed to clean buffer", e);
            }
        }
    }
}
