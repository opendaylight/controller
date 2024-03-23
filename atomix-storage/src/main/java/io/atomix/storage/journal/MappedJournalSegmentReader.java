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

import java.nio.ByteBuffer;

/**
 * Log segment reader.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class MappedJournalSegmentReader<E> extends JournalSegmentReader<E> {
    private final ByteBuffer buffer;

    MappedJournalSegmentReader(final ByteBuffer buffer, final JournalSegment<E> segment, final int maxEntrySize,
            final JournalSerdes namespace) {
        super(segment, maxEntrySize, namespace);
        this.buffer = buffer.slice().asReadOnlyBuffer();
    }

    @Override
    void invalidateCache() {
        // No-op: the mapping is guaranteed to be coherent
    }

    @Override
    ByteBuffer read(final int position, final int size) {
        return buffer.slice(position, size);
    }
}
