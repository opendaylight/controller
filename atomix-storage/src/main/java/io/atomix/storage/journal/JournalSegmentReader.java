/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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

import org.eclipse.jdt.annotation.Nullable;

abstract sealed class JournalSegmentReader<E> permits DiskJournalSegmentReader, MappedJournalSegmentReader {
    final int maxEntrySize;
    final JournalSerdes namespace;
    private final JournalSegment<E> segment;

    JournalSegmentReader(final JournalSegment<E> segment, final int maxEntrySize, final JournalSerdes namespace) {
        this.segment = requireNonNull(segment);
        this.maxEntrySize = maxEntrySize;
        this.namespace = requireNonNull(namespace);
    }

    /**
     * Close this reader.
     */
    final void close() {
        segment.closeReader(this);
    }

    /**
     * Set the file position.
     *
     * @param position new position
     */
    abstract void setPosition(int position);

    /**
     * Reads the entry at specified index.
     *
     * @param index entry index
     * @return The entry, or {@code null}
     */
    abstract @Nullable Indexed<E> readEntry(long index);
}
