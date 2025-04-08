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
        private final FromStoreEntryMapperV1 mapper;
        private final EntryReader reader;

        private RecoveryReader(final FromStoreEntryMapperV1 mapper, final EntryReader reader) {
            this.mapper = requireNonNull(mapper);
            this.reader = requireNonNull(reader);
        }

        public @Nullable FromStoreEntry tryNext() throws IOException {
            return reader.tryNext(mapper);
        }

        @Override
        protected void removeRegistration() {
            reader.close();
        }
    }

    // 256KiB, which leads to reasonable buffers
    private static final int INLINE_ENTRY_SIZE = 256 * 1024;
    // We need to be able to store two entries and a descriptor
    private static final long MIN_TARGET_SIZE = INLINE_ENTRY_SIZE * 2 + SegmentDescriptor.BYTES;

    private final FromStoreEntryMapperV1 fromMapper;
    private final RaftJournal journal;
    private final long targetSize;

    // FIXME: maxEntries does not really work: we really want to have a 'long targetCapacity', which is a hint to rotate
    //        logs based on inline + out-of-line capacity. To make that happen we need a journal.writer()-side method to
    //        close the current segment and move to the next one, though.
    //        Perhaps RaftJournal.checkpoint(), hinting at a commit/purge coming in?
    public LogJournal(final Path directory, final StorageLevel storageLevel, final long targetSize) throws IOException {
        fromMapper = new FromStoreEntryMapperV1(directory);

        if (targetSize < MIN_TARGET_SIZE) {
            throw new IOException("Require at least 2 entries (" + MIN_TARGET_SIZE + " ), not " + targetSize);
        }
        this.targetSize = targetSize;

        journal = SegmentedRaftJournal.builder()
            .withDirectory(directory)
            .withName("entries.v1")
            .withMaxEntrySize(INLINE_ENTRY_SIZE)
            // Saturate up to 2GiB. We cannot support segments larger than that anyway
            .withMaxSegmentSize(targetSize < Integer.MAX_VALUE ? (int) targetSize : Integer.MAX_VALUE)
            .withStorageLevel(storageLevel)
            .build();
    }

    public RecoveryReader recoverEntries() {
        return new RecoveryReader(fromMapper, journal.openReader(-1));
    }

    @Override
    public void close() throws IOException {
        journal.close();
    }
}
