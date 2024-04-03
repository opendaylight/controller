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

import io.netty.buffer.ByteBuf;

/**
 * A {@link JournalReader} traversing all entries.
 */
final class SegmentedJournalReader<E> implements JournalReader<E> {
    private final ByteJournalReader byteJournalReader;
    private final JournalSerializer<E> serializer;

    SegmentedJournalReader(final ByteJournalReader byteJournalReader, final JournalSerializer<E> serializer) {
        this.byteJournalReader = byteJournalReader;
        this.serializer = serializer;
    }

    @Override
    public final long getFirstIndex() {
        return byteJournalReader.firstIndex();
    }

    @Override
    public final Indexed<E> getCurrentEntry() {
        final var buf =  byteJournalReader.lastRead();
        return buf == null ? null : indexed(byteJournalReader.lastIndex(), buf);
    }

    @Override
    public final long getNextIndex() {
        return byteJournalReader.nextIndex();
    }

    @Override
    public final void reset() {
        byteJournalReader.reset();
    }

    @Override
    public final void reset(final long index) {
        byteJournalReader.reset(index);
    }

    @Override
    public Indexed<E> tryNext() {
        final var buf = byteJournalReader.tryNext();
        return buf == null ? null : indexed(byteJournalReader.lastIndex(), buf);
    }

    @Override
    public void close() {
        byteJournalReader.close();
    }

    private Indexed<E> indexed(final long index, final ByteBuf buf) {
        return new Indexed<>(index, serializer.deserialize(buf), buf.readableBytes());
    }
}
