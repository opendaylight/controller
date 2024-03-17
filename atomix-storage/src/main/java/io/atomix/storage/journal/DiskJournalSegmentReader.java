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

import io.atomix.storage.journal.index.JournalIndex;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Log segment reader.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class DiskJournalSegmentReader<E> extends JournalSegmentReader<E> {
    private final DiskSegmentEntryReader entryReader;

    DiskJournalSegmentReader(final FileChannel channel, final JournalSegment<E> segment, final int maxEntrySize,
            final JournalIndex index, final JournalSerdes namespace) {
        super(segment, maxEntrySize, index, namespace);
        entryReader = new DiskSegmentEntryReader(channel, segment.descriptor().maxSegmentSize(), maxEntrySize);
        reset();
    }

    @Override
    void setPosition(final int position) {
        entryReader.reset(position);
    }

    @Override
    Indexed<E> readEntry(final long index) {
        try {
            return entryReader.readNextIndexed(namespace, index);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }
}
