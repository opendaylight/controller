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
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.RaftEntryMeta;
import org.opendaylight.controller.raft.journal.FromByteBufMapper;
import org.opendaylight.controller.raft.journal.ToByteBufMapper;
import org.opendaylight.yangtools.concepts.WritableObjects;

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
            try (var ois = new ObjectInputStream(new ByteBufInputStream(bytes))) {
                final var header = WritableObjects.readLongHeader(ois);
                return new SimpleReplicatedLogEntry(
                    WritableObjects.readFirstLong(ois, header), WritableObjects.readSecondLong(ois, header),
                    (Payload) ois.readObject());
            } catch (ClassNotFoundException e) {
                throw new UncheckedIOException(new IOException("Failed to read payload", e));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void objectToBytes(final ReplicatedLogEntry obj, final ByteBuf buf) throws IOException {
            try (var oos = new ObjectOutputStream(new ByteBufOutputStream(buf))) {
                WritableObjects.writeLongs(oos, obj.index(), obj.term());
                oos.writeObject(obj.getData());
            }
        }
    }

    private final SegmentedJournal<@NonNull ReplicatedLogEntry> entries;

    SegmentedReplicatedLog(final String logId, final long snapshotIndex, final long snapshotTerm,
            final List<ReplicatedLogEntry> unAppliedEntries, final Path directory, final StorageLevel storage,
            final int maxEntrySize, final int maxSegmentSize) {
        super(logId, snapshotIndex, snapshotTerm, unAppliedEntries);
        entries = new SegmentedJournal<>(SegmentedByteBufJournal.builder()
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
