/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import io.atomix.storage.journal.index.JournalIndex;
import java.nio.channels.FileChannel;

/**
 * A {@link FileAccess} for {@link StorageLevel#DISK}.
 */
final class DiskFileAccess extends FileAccess {
    private FileChannel channel;

    DiskFileAccess(final FileChannel channel, final int size) {
        this.channel = requireNonNull(channel);
    }

    @Override
    <E> JournalSegmentWriter<E> createInitialWriter(final JournalSegment<E> segment, final int maxEntrySize,
            final JournalIndex index, final JournalSerdes namespace) {
        return new FileChannelJournalSegmentWriter<>(channel, segment, maxEntrySize, index, namespace);
    }

    @Override
    <E> JournalSegmentReader<E> createReader(final JournalSegment<E> segment, final int maxEntrySize,
            final JournalIndex index, final JournalSerdes namespace) {
        return new FileChannelJournalSegmentReader<>(channel, this, segment, maxEntrySize, index, namespace);
    }

    @Override
    void cleanup() {
        channel = null;
    }
}
