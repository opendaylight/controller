/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.journal.EntryReader;
import org.opendaylight.raft.journal.FromByteBufMapper;

/**
 * A {@link JournalReader} backed by a {@link EntryReader}.
 */
@NonNullByDefault
@Deprecated(since = "11.0.0", forRemoval = true)
final class SegmentedJournalReader<E> implements JournalReader<E> {
    private final FromByteBufMapper<E> mapper;
    private final EntryReader reader;

    SegmentedJournalReader(final EntryReader reader, final FromByteBufMapper<E> mapper) {
        this.reader = requireNonNull(reader);
        this.mapper = requireNonNull(mapper);
    }

    @Override
    public long getNextIndex() {
        return reader.nextIndex();
    }

    @Override
    public void reset() {
        reader.reset();
    }

    @Override
    public void reset(final long index) {
        reader.reset(index);
    }

    @Override
    public <T> @Nullable T tryNext(final EntryMapper<E, T> entryMapper) {
        return reader.tryNext((index, buf) -> {
            final var size = buf.readableBytes();
            return requireNonNull(entryMapper.mapEntry(index, mapper.bytesToObject(index, buf), size));
        });
    }

    @Override
    public void close() {
        reader.close();
    }
}
