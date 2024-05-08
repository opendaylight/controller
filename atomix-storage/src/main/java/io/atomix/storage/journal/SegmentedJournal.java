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

/**
 * A {@link Journal} implementation based on a {@link ByteBufJournal}.
 */
public final class SegmentedJournal<E> implements Journal<E> {
    private final SegmentedJournalWriter<E> writer;
    private final ByteBufMapper<E> mapper;
    private final ByteBufJournal journal;

    public SegmentedJournal(final ByteBufJournal journal, final ByteBufMapper<E> mapper) {
        this.journal = requireNonNull(journal, "journal is required");
        this.mapper = requireNonNull(mapper, "mapper cannot be null");
        writer = new SegmentedJournalWriter<>(journal.writer(), mapper);
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
    public JournalReader<E> openReader(final long index) {
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
    public JournalReader<E> openReader(final long index, final JournalReader.Mode mode) {
        final var byteReader = switch (mode) {
            case ALL -> journal.openReader(index);
            case COMMITS -> journal.openCommitsReader(index);
        };
        return new SegmentedJournalReader<>(byteReader, mapper);
    }

    @Override
    public void compact(final long index) {
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
