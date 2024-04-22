/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
 * Copyright (c) 2024 PANTHEON.tech s.r.o. and others.
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

import java.io.File;

/**
 * Segmented journal.
 */
public final class SegmentedJournal<E> implements Journal<E> {
    private final SegmentedByteJournal byteJournal;
    private final JournalSerializer<E> serializer;
    private final SegmentedJournalWriter<E> writer;

    public SegmentedJournal(final SegmentedByteJournal byteJournal, final JournalSerializer<E> serializer) {
        this.byteJournal = requireNonNull(byteJournal, "byteJournal is required");
        this.serializer = requireNonNull(serializer, "serializer cannot be null");
        writer = new SegmentedJournalWriter<>(byteJournal.writer(), serializer);
    }

    @Override
    public JournalWriter<E> writer() {
        return writer;
    }

    @Override
    public JournalReader<E> openReader(long index) {
        return openReader(index, JournalReader.Mode.ALL);
    }

    /**
     * Opens a new journal reader with the given reader mode.
     *
     * @param index The index from which to begin reading entries.
     * @param mode The mode in which to read entries.
     * @return The journal reader.
     */
    public JournalReader<E> openReader(long index, JournalReader.Mode mode) {
        return new SegmentedJournalReader<>(byteJournal.newReader(index, mode), serializer);
    }

    @Override
    public boolean isOpen() {
        return byteJournal.isOpen();
    }

    @Override
    public void close() {
        byteJournal.close();
    }

    /**
     * Compacts the journal up to the given index.
     * <p>
     * The semantics of compaction are not specified by this interface.
     *
     * @param index The index up to which to compact the journal.
     */
    public void compact(long index) {
        byteJournal.compact(index);
    }

    /**
     * Returns a new segmented journal builder.
     *
     * @return A new segmented journal builder.
     */
    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    public static final class Builder<E> {
        private final SegmentedByteJournal.Builder byteJournalBuilder = SegmentedByteJournal.builder();
        private JournalSerializer<E> serializer;

        private Builder() {
            // on purpose
        }

        /**
         * Sets the journal name.
         *
         * @param name The journal name.
         * @return The journal builder.
         */
        public Builder<E> withName(final String name) {
            byteJournalBuilder.withName(name);
            return this;
        }

        /**
         * Sets the journal storage level.
         * <p>
         * The storage level indicates how individual entries will be persisted in the journal.
         *
         * @param storageLevel The log storage level.
         * @return The journal builder.
         */
        public Builder<E> withStorageLevel(final StorageLevel storageLevel) {
            byteJournalBuilder.withStorageLevel(storageLevel);
            return this;
        }

        /**
         * Sets the journal storage directory.
         * <p>
         * The journal will write segment files into the provided directory.
         *
         * @param directory The journal storage directory.
         * @return The journal builder.
         * @throws NullPointerException If the {@code directory} is {@code null}
         */
        public Builder<E> withDirectory(final String directory) {
            byteJournalBuilder.withDirectory(directory);
            return this;
        }

        /**
         * Sets the journal storage directory.
         * <p>
         * The journal will write segment files into the provided directory.
         *
         * @param directory The journal storage directory.
         * @return The journal builder.
         * @throws NullPointerException If the {@code directory} is {@code null}
         */
        public Builder<E> withDirectory(final File directory) {
             byteJournalBuilder.withDirectory(directory);
            return this;
        }

        /**
         * Sets the journal namespace.
         *
         * @param namespace The journal serializer.
         * @return The journal builder.
         * @deprecated due to serialization refactoring, use {@link Builder#withSerializer(JournalSerializer)} instead
         */
        @Deprecated(forRemoval = true, since="9.0.3")
        public Builder<E> withNamespace(final JournalSerdes namespace) {
            this.serializer = JournalSerializer.wrap(requireNonNull(namespace, "namespace cannot be null"));
            return this;
        }

        /**
         * Sets journal serializer.
         *
         * @param serializer Journal serializer
         * @return The journal builder
         */
        public Builder<E> withSerializer(final JournalSerializer<E> serializer) {
            this.serializer = serializer;
            return this;
        }

        /**
         * Sets the maximum segment size in bytes.
         * <p>
         * The maximum segment size dictates when journal should roll over to new segments. As entries are written
         * to a journal segment, once the size of the segment surpasses the configured maximum segment size, the
         * journal will create a new segment and append new entries to that segment.
         * <p>
         * By default, the maximum segment size is 32M.
         *
         * @param maxSegmentSize The maximum segment size in bytes.
         * @return The storage builder.
         * @throws IllegalArgumentException If the {@code maxSegmentSize} is not positive
         */
        public Builder<E> withMaxSegmentSize(final int maxSegmentSize) {
            byteJournalBuilder.withMaxSegmentSize(maxSegmentSize);
            return this;
        }

        /**
         * Sets the maximum entry size in bytes.
         *
         * @param maxEntrySize the maximum entry size in bytes
         * @return the storage builder
         * @throws IllegalArgumentException if the {@code maxEntrySize} is not positive
         */
        public Builder<E> withMaxEntrySize(final int maxEntrySize) {
            byteJournalBuilder.withMaxEntrySize(maxEntrySize);
            return this;
        }

        /**
         * Sets the maximum number of entries per segment.
         *
         * @param maxEntriesPerSegment The maximum number of entries allowed per segment.
         * @return The journal builder.
         * @deprecated since 3.0.2, no longer used
         */
        @Deprecated
        public Builder<E> withMaxEntriesPerSegment(int maxEntriesPerSegment) {
            // ignore
            return this;
        }

        /**
         * Sets the journal index density.
         * <p>
         * The index density is the frequency at which the position of entries written to the journal will be recorded
         * in an in-memory index for faster seeking.
         *
         * @param indexDensity the index density
         * @return the journal builder
         * @throws IllegalArgumentException if the density is not between 0 and 1
         */
        public Builder<E> withIndexDensity(double indexDensity) {
            byteJournalBuilder.withIndexDensity(indexDensity);
            return this;
        }

        /**
         * Enables flushing buffers to disk when entries are committed to a segment.
         * <p>
         * When flush-on-commit is enabled, log entry buffers will be automatically flushed to disk each time an
         * entry is committed in a given segment.
         *
         * @return The journal builder.
         */
        public Builder<E> withFlushOnCommit() {
            return withFlushOnCommit(true);
        }

        /**
         * Enables flushing buffers to disk when entries are committed to a segment.
         * <p>
         * When flush-on-commit is enabled, log entry buffers will be automatically flushed to disk each time an
         * entry is committed in a given segment.
         *
         * @param flushOnCommit Whether to flush buffers to disk when entries are committed to a segment.
         * @return The journal builder.
         */
        public Builder<E> withFlushOnCommit(boolean flushOnCommit) {
            byteJournalBuilder.withFlushOnCommit(flushOnCommit);
            return this;
        }

        /**
         * Build the {@link SegmentedJournal}.
         *
         * @return {@link SegmentedJournal} instance.
         */
        public SegmentedJournal<E> build() {
            return new SegmentedJournal<>(byteJournalBuilder.build(), serializer);
        }
    }
}
