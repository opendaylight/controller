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

import java.io.IOException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.raft.journal.EntryWriter;
import org.opendaylight.controller.raft.journal.ToByteBufMapper;

/**
 * A {@link JournalWriter} backed by a {@link EntryWriter}.
 */
@NonNullByDefault
final class SegmentedJournalWriter<E> implements JournalWriter<E> {
    private final ToByteBufMapper<E> mapper;
    private final EntryWriter writer;

    SegmentedJournalWriter(final EntryWriter writer, final ToByteBufMapper<E> mapper) {
        this.writer = requireNonNull(writer);
        this.mapper = requireNonNull(mapper);
    }

    @Override
    public long getNextIndex() {
        return writer.nextIndex();
    }

    @Override
    public void commit(final long index) throws IOException {
        writer.commit(index);
    }

    @Override
    public <T extends E> Indexed<T> append(final T entry) throws IOException {
        final var index = writer.nextIndex();
        return new Indexed<>(index, entry, writer.append(mapper, entry));
    }

    @Override
    public void reset(final long index) throws IOException {
        writer.reset(index);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }
}
