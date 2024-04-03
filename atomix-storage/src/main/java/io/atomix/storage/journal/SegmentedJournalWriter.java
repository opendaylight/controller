/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
 * Copyright (c) 2024 PANTHEON.tech s.r.o. All rights reserved.
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

/**
 * Segmented Journal Writer.
 */
final class SegmentedJournalWriter<E> implements JournalWriter<E> {
    private final ByteJournalWriter byteJournalWriter;
    private final JournalSerializer<E> serializer;

    public SegmentedJournalWriter(final ByteJournalWriter byteJournalWriter,
            final JournalSerializer<E> serializer) {
        this.byteJournalWriter = byteJournalWriter;
        this.serializer = serializer;
    }

    @Override
    public long getLastIndex() {
        return byteJournalWriter.lastIndex();
    }

    @Override
    public Indexed<E> getLastEntry() {
        final var buf = byteJournalWriter.lastWritten();
        return buf == null ? null
            : new Indexed<>(byteJournalWriter.lastIndex(), serializer.deserialize(buf), buf.readableBytes());
    }

    @Override
    public long getNextIndex() {
        return byteJournalWriter.nextIndex();
    }

    @Override
    public void reset(long index) {
        byteJournalWriter.reset(index);
    }

    @Override
    public void commit(long index) {
        byteJournalWriter.commit(index);
    }

    @Override
    public <T extends E> Indexed<T> append(T entry) {
        final var buf = serializer.serialize(entry);
        return new Indexed<>(byteJournalWriter.append(buf), entry, buf.readableBytes());
    }

    @Override
    public void truncate(long index) {
        byteJournalWriter.truncate(index);
    }

    @Override
    public void flush() {
        byteJournalWriter.flush();
    }
}
