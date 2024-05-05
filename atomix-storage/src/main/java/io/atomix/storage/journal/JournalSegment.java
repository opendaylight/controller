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

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import io.atomix.storage.journal.index.JournalIndex;
import io.atomix.storage.journal.index.Position;
import io.atomix.storage.journal.index.SparseJournalIndex;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log segment.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class JournalSegment {
    private static final Logger LOG = LoggerFactory.getLogger(JournalSegment.class);

    private final Set<JournalSegmentReader> readers = ConcurrentHashMap.newKeySet();
    private final AtomicInteger references = new AtomicInteger();
    private final JournalSegmentFile file;
    private final StorageLevel storageLevel;
    private final int maxEntrySize;
    private final JournalIndex journalIndex;

    private JournalSegmentWriter writer;
    private boolean open = true;

    JournalSegment(final JournalSegmentFile file, final StorageLevel storageLevel, final int maxEntrySize,
            final double indexDensity) {
        this.file = requireNonNull(file);
        this.storageLevel = requireNonNull(storageLevel);
        this.maxEntrySize = maxEntrySize;
        journalIndex = new SparseJournalIndex(indexDensity);

        final var fileWriter = switch (storageLevel) {
            case DISK -> new DiskFileWriter(file, maxEntrySize);
            case MAPPED -> new MappedFileWriter(file, maxEntrySize);
        };
        writer = new JournalSegmentWriter(fileWriter, this, maxEntrySize, journalIndex)
            // relinquish mapped memory
            .toFileChannel();
    }

    /**
     * Returns the segment's starting index.
     *
     * @return The segment's starting index.
     */
    long firstIndex() {
        return file.descriptor().index();
    }

    /**
     * Returns the last index in the segment.
     *
     * @return The last index in the segment.
     */
    long lastIndex() {
        return writer.getLastIndex();
    }

    /**
     * Returns the size of the segment.
     *
     * @return the size of the segment
     */
    int size() throws IOException {
        return file.size();
    }

    /**
     * Returns the segment file.
     *
     * @return The segment file.
     */
    JournalSegmentFile file() {
        return file;
    }

    /**
     * Looks up the position of the given index.
     *
     * @param index the index to lookup
     * @return the position of the given index or a lesser index, or {@code null}
     */
    @Nullable Position lookup(final long index) {
        return journalIndex.lookup(index);
    }

    /**
     * Acquires a reference to the log segment.
     */
    private void acquire() {
        if (references.getAndIncrement() == 0 && storageLevel == StorageLevel.MAPPED) {
            writer = writer.toMapped();
        }
    }

    /**
     * Releases a reference to the log segment.
     */
    private void release() {
        if (references.decrementAndGet() == 0) {
            if (storageLevel == StorageLevel.MAPPED) {
                writer = writer.toFileChannel();
            }
            if (!open) {
                finishClose();
            }
        }
    }

    /**
     * Acquires a reference to the segment writer.
     *
     * @return The segment writer.
     */
    JournalSegmentWriter acquireWriter() {
        checkOpen();
        acquire();

        return writer;
    }

    /**
     * Releases the reference to the segment writer.
     */
    void releaseWriter() {
        release();
    }

    /**
     * Creates a new segment reader.
     *
     * @return A new segment reader.
     */
    JournalSegmentReader createReader() {
        checkOpen();
        acquire();

        final var buffer = writer.buffer();
        final var fileReader = buffer != null ? new MappedFileReader(file, buffer)
            : new DiskFileReader(file, maxEntrySize);
        final var reader = new JournalSegmentReader(this, fileReader, maxEntrySize);
        reader.setPosition(JournalSegmentDescriptor.BYTES);
        readers.add(reader);
        return reader;
    }

    /**
     * Closes a segment reader.
     *
     * @param reader the closed segment reader
     */
    void closeReader(final JournalSegmentReader reader) {
        if (readers.remove(reader)) {
            release();
        }
    }

    /**
     * Checks whether the segment is open.
     */
    private void checkOpen() {
        if (!open) {
            throw new IllegalStateException("Segment not open");
        }
    }

    /**
     * Returns a boolean indicating whether the segment is open.
     *
     * @return indicates whether the segment is open
     */
    boolean isOpen() {
        return open;
    }

    /**
     * Closes the segment.
     */
    void close() {
        if (!open) {
            return;
        }

        LOG.debug("Closing segment: {}", this);
        open = false;
        readers.forEach(JournalSegmentReader::close);
        if (references.get() == 0) {
            finishClose();
        }
    }

    private void finishClose() {
        writer.close();
        try {
            file.close();
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    /**
     * Deletes the segment.
     */
    void delete() {
        close();
        LOG.debug("Deleting segment: {}", this);
        try {
            Files.deleteIfExists(file.path());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public String toString() {
        final var descriptor = file.descriptor();
        return MoreObjects.toStringHelper(this)
            .add("id", descriptor.id())
            .add("version", descriptor.version())
            .add("index", descriptor.index())
            .toString();
    }
}
