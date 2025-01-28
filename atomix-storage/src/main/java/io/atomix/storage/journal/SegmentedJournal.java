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

import com.google.common.base.MoreObjects;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.raft.journal.FromByteBufMapper;
import org.opendaylight.controller.raft.journal.RaftJournal;
import org.opendaylight.controller.raft.journal.ToByteBufMapper;

/**
 * A {@link Journal} implementation based on a {@link RaftJournal}.
 */
public final class SegmentedJournal<E> implements Journal<E> {
    private final @NonNull SegmentedJournalWriter<E> writer;
    private final @NonNull FromByteBufMapper<E> readMapper;
    private final @NonNull RaftJournal journal;

    public SegmentedJournal(final RaftJournal journal, final FromByteBufMapper<E> readMapper,
            final ToByteBufMapper<E> writeMapper) {
        this.journal = requireNonNull(journal, "journal is required");
        this.readMapper = requireNonNull(readMapper, "readMapper cannot be null");
        writer = new SegmentedJournalWriter<>(journal.writer(),
            requireNonNull(writeMapper, "writeMapper cannot be null"));
    }

    @Override
    public long firstIndex() {
        return journal.firstIndex();
    }

    @Override
    public long lastIndex() {
        return journal.lastIndex();
    }

    @Override
    public JournalWriter<E> writer() {
        return writer;
    }

    @Override
    public JournalReader<E> openReader(final long index) throws IOException {
        return openReader(index, JournalReader.Mode.ALL);
    }

    /**
     * Opens a new journal reader with the given reader mode.
     *
     * @param index The index from which to begin reading entries.
     * @param mode The mode in which to read entries.
     * @return The journal reader.
     */
    @Override
    public JournalReader<E> openReader(final long index, final JournalReader.Mode mode) throws IOException {
        final var byteReader = switch (mode) {
            case ALL -> journal.openReader(index);
            case COMMITS -> journal.openCommitsReader(index);
        };
        return new SegmentedJournalReader<>(byteReader, readMapper);
    }

    @Override
    public void compact(final long index) throws IOException {
        journal.compact(index);
    }

    @Override
    public void close() {
        journal.close();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("journal", journal).toString();
    }
}
