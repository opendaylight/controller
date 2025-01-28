/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import io.atomix.storage.journal.SegmentedByteBufJournal;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.StorageLevel;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.spi.RaftEntryMeta;
import org.opendaylight.controller.raft.journal.FromByteBufMapper;
import org.opendaylight.controller.raft.journal.ToByteBufMapper;

/**
 * A {@link ReplicatedLog} implemented on top of a SegmentedJournal.
 */
final class SegmentedReplicatedLog extends AbstractReplicatedLog {
    @NonNullByDefault
    private static final class EntryMapper
            implements FromByteBufMapper<ReplicatedLogEntry>, ToByteBufMapper<ReplicatedLogEntry> {
        static final EntryMapper INSTANCE = new EntryMapper();

        @Override
        public ReplicatedLogEntry bytesToObject(final long index, final ByteBuf bytes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void objectToBytes(final ReplicatedLogEntry obj, final ByteBuf buf) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    private final SegmentedJournal<@NonNull ReplicatedLogEntry> journal;

    SegmentedReplicatedLog(final String logContext, final long snapshotIndex, final long snapshotTerm,
            final List<ReplicatedLogEntry> unAppliedEntries, final Path directory, final StorageLevel storage,
            final int maxEntrySize, final int maxSegmentSize) {
        super(snapshotIndex, snapshotTerm, unAppliedEntries, logContext);
        journal = new SegmentedJournal<>(SegmentedByteBufJournal.builder()
            .withDirectory(directory.toFile())
            .withName("entries")
            .withStorageLevel(storage)
            .withMaxEntrySize(maxEntrySize)
            .withMaxSegmentSize(maxSegmentSize)
            .build(), EntryMapper.INSTANCE, EntryMapper.INSTANCE);
    }

    @Override
    public boolean removeFromAndPersist(final long index) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean appendAndPersist(final ReplicatedLogEntry replicatedLogEntry,
            final Consumer<ReplicatedLogEntry> callback, final boolean doAsync) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void captureSnapshotIfReady(final RaftEntryMeta replicatedLogEntry) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean shouldCaptureSnapshot(final long logIndex) {
        // TODO Auto-generated method stub
        return false;
    }
}
