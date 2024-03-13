/*
 * Copyright 2017-present Open Networking Foundation
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
import io.atomix.storage.journal.index.SparseJournalIndex;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Log segment.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class JournalSegment<E> {
    private final Set<JournalSegmentReader<E>> readers = ConcurrentHashMap.newKeySet();
    private final JournalSegmentFile file;
    private final JournalSegmentDescriptor descriptor;
    private final StorageLevel storageLevel;
    private final int maxEntrySize;
    private final JournalIndex index;
    private final JournalSerdes namespace;
    private final FileChannel channel;

    private volatile FileAccess fileAccess;
    private JournalSegmentWriter<E> writer;
    private boolean open;

    JournalSegment(final JournalSegmentFile file, final JournalSegmentDescriptor descriptor,
            final StorageLevel storageLevel, final int maxEntrySize, final double indexDensity,
            final JournalSerdes namespace) {
        this.file = requireNonNull(file);
        this.descriptor = requireNonNull(descriptor);
        this.storageLevel = requireNonNull(storageLevel);
        this.maxEntrySize = maxEntrySize;
        this.namespace = requireNonNull(namespace);
        index = new SparseJournalIndex(indexDensity);
        try {
            channel = FileChannel.open(file.file().toPath(),
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new StorageException(e);
        }

        // Create a FileAccess and index
        fileAccess = createAccess();
        writer = fileAccess.createInitialWriter(this, maxEntrySize, index, namespace);
        open = true;
        releaseWriter();
    }

    /**
     * Returns the segment's starting index.
     *
     * @return The segment's starting index.
     */
    long index() {
        return descriptor.index();
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
    int size() {
        try {
            return (int) channel.size();
        } catch (IOException e) {
            throw new StorageException(e);
        }
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
     * Returns the segment descriptor.
     *
     * @return The segment descriptor.
     */
    JournalSegmentDescriptor descriptor() {
        return descriptor;
    }

    /**
     * Acquires a reference to the segment writer.
     *
     * @return The segment writer.
     */
    JournalSegmentWriter<E> acquireWriter() {
        checkOpen();
        acquireAccess();

        return writer;
    }

    /**
     * Releases the reference to the segment writer.
     */
    void releaseWriter() {
        // FIXME: acquire offset and last entry
        writer.close();
        writer = null;
        fileAccess.release();
    }

    /**
     * Creates a new segment reader.
     *
     * @return A new segment reader.
     */
    JournalSegmentReader<E> createReader() {
        checkOpen();
        final var reader = acquireAccess().createReader(this, maxEntrySize, index, namespace);
        readers.add(reader);
        return reader;
    }

    /**
     * Closes a segment reader.
     *
     * @param reader the closed segment reader
     * @param access
     */
    void removeReader(final JournalSegmentReader<E> reader, final FileAccess access) {
        if (readers.remove(reader)) {
            if (access.release()) {
                // FIXME: check if it is the same access we have now and we are closed
            }
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

        open = false;
        readers.forEach(JournalSegmentReader::close);
        if (references.get() == 0) {
            finishClose();
        }
    }

    private void finishClose() {
        writer.close();
        try {
            channel.close();
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    /**
     * Deletes the segment.
     */
    void delete() {
        try {
            Files.deleteIfExists(file.file().toPath());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", descriptor.id())
            .add("version", descriptor.version())
            .add("index", index())
            .toString();
    }

    /**
     * Acquires a reference to the log segment and perhaps allocates resources needed to access it.
     */
    private FileAccess acquireAccess() {
        final var local = fileAccess;
        return local.acquire() ? local : lockedAcquireAccess();
    }

    private synchronized FileAccess lockedAcquireAccess() {
        // Retry, as fileAccess may have been updated
        final var existing = fileAccess;
        if (existing.acquire()) {
            return existing;
        }

        final var created = createAccess();
        fileAccess = created;
        return created;
    }

    private @NonNull FileAccess createAccess() {
        try {
            return switch (storageLevel) {
                case DISK -> new DiskFileAccess(channel, descriptor.maxSegmentSize());
                case MAPPED -> new MappedFileAccess(channel, descriptor.maxSegmentSize());
            };
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }
}
