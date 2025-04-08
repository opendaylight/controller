/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.ReplicatedLog;
import org.opendaylight.raft.journal.EntryReader;
import org.opendaylight.raft.journal.RaftJournal;
import org.opendaylight.raft.journal.SegmentDescriptor;
import org.opendaylight.raft.journal.SegmentedRaftJournal;
import org.opendaylight.raft.journal.StorageLevel;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

/**
 * A {@link RaftJournal} working witn a {@link ReplicatedLog} and local files to store
 * {@link ReplicateLogEntry log entries}
 */
@NonNullByDefault
public final class LogJournal implements Closeable {

    public static final class RecoveryReader extends AbstractRegistration {
        private final EntryReader reader;
        private final Path directory;

        private RecoveryReader(final Path directory, final EntryReader reader) {
            this.directory = requireNonNull(directory);
            this.reader = requireNonNull(reader);
        }

        public @Nullable FromStoreEntry tryNext() {
            return reader.tryNext(FromStoreEntryMapper.INSTANCE);
        }

        @Override
        protected void removeRegistration() {
            reader.close();
        }
    }


    // 256KiB, which leads to reasonable buffers
    public static final int INLINE_ENTRY_SIZE = 256 * 1024;

    private static final int MAX_ENTRIES_LIMIT = (Integer.MAX_VALUE - SegmentDescriptor.BYTES) / INLINE_ENTRY_SIZE;

    private final RaftJournal journal;
    private final Path directory;

    // FIXME: maxEntries does not really work: we really want to have a 'long targetCapacity', which is a hint to rotate
    //        logs based on inline + out-of-line capacity. To make that happen we need a journal.writer()-side method to
    //        close the current segment and move to the next one, though.
    //        Perhaps RaftJournal.checkpoint(), hinting at a commit/purge coming in?
    public LogJournal(final Path directory, final StorageLevel storageLevel, final int maxEntries) throws IOException {
        this.directory = requireNonNull(directory);

        if (maxEntries < 2) {
            throw new IOException("Require at least 2 entries, not " + maxEntries);
        }
        if (maxEntries > MAX_ENTRIES_LIMIT) {
            throw new IOException("Require at most " + MAX_ENTRIES_LIMIT + " entries, not " + maxEntries);
        }

        journal = SegmentedRaftJournal.builder()
            .withDirectory(directory)
            .withName("entries.v1")
            .withMaxEntrySize(INLINE_ENTRY_SIZE)
            .withMaxSegmentSize(maxEntries * INLINE_ENTRY_SIZE + SegmentDescriptor.BYTES)
            .withStorageLevel(storageLevel)
            .build();
    }

    public RecoveryReader recoverEntries() {
        return new RecoveryReader(directory, journal.openReader(-1));
    }

    @Override
    public void close() throws IOException {
        journal.close();
    }
}
