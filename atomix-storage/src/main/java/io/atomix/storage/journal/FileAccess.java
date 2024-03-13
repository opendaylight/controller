/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import io.atomix.storage.journal.index.JournalIndex;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility for accessing the contents of a {@link JournalSegmentFile}.
 */
abstract sealed class FileAccess permits DiskFileAccess, MappedFileAccess {
    // We start off with "1" for keep initial indexing simpler
    private final AtomicInteger refCount = new AtomicInteger(1);

    /**
     * Acquire a reference to to this access.
     *
     * @return {@code true} if the reference was successfully acquired.
     */
    final boolean acquire() {
        return refCount.getAndIncrement() != 0 || acquireFirst();
    }

    /**
     * Release a reference to to this access.
     *
     * @return {@code true} if this call resulted in this access becoming inoperable.
     */
    final boolean release() {
        return refCount.decrementAndGet() == 0 && releaseLast();
    }

    abstract <E> JournalSegmentWriter<E> createInitialWriter(JournalSegment<E> segment, int maxEntrySize,
        JournalIndex index, JournalSerdes namespace);

    abstract <E> JournalSegmentReader<E> createReader(JournalSegment<E> segment, int maxEntrySize, JournalIndex index,
        JournalSerdes namespace);

    private synchronized boolean acquireFirst() {
        // We have observed the reference count going from 0 to 1, indicating we are racing with a release() going from
        // 1 to 0. If it is still non-zero here, we have won and the other thread will abort in releaseLast() below.
        return refCount.get() != 0;
    }

    private synchronized boolean releaseLast() {
        // We have observed the reference count going from 1 to 0, indicating we can clean up. Unfortunately there could
        // another thread which flipped the reference count back from 0 to 1. Therefore re-check the reference count
        // again and if it is non-zero, do not clean up.
        if (refCount.get() == 0) {
            cleanup();
            return true;
        }
        return false;
    }

    /**
     * Release resources associated with this object, rendering it inoperable.
     */
    abstract void cleanup();
}
