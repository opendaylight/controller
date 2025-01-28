/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

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
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.EntryStore;
import org.opendaylight.controller.raft.journal.FromByteBufMapper;
import org.opendaylight.controller.raft.journal.ToByteBufMapper;
import org.opendaylight.yangtools.concepts.WritableObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link EntryStore} implemented on top of a {@link SegmentedJournal}.
 */
@NonNullByDefault
final class SegmentedEntryStore implements EntryStore {
    // FIXME: CONTROLLER-2044: this should get some input from Shard
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

    private static final Logger LOG = LoggerFactory.getLogger(SegmentedEntryStore.class);

    private final String logId;
    private final SegmentedJournal<ReplicatedLogEntry> journal;

    SegmentedEntryStore(final String logId, final Path directory, final StorageLevel storage,
            final int maxEntrySize, final int maxSegmentSize) {
        this.logId = requireNonNull(logId);
        journal = new SegmentedJournal<>(SegmentedByteBufJournal.builder()
            .withDirectory(directory.toFile())
            .withName("entries")
            .withStorageLevel(storage)
            .withMaxEntrySize(maxEntrySize)
            .withMaxSegmentSize(maxSegmentSize)
            .build(), EntryMapper.INSTANCE, EntryMapper.INSTANCE);
    }

    @Override
    public void removeFrom(final long fromIndex) {
        LOG.trace("{}: resetting writer to {}", logId, fromIndex);
        journal.writer().reset(fromIndex);
    }

    @Override
    public void applyTo(final long toIndex) {
        LOG.trace("{}: committing writer to {}", logId, toIndex);
        journal.writer().commit(toIndex);
        // FIXME: smarter policy?
        journal.compact(toIndex);
    }

    @Override
    public void persist(final ReplicatedLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
        // FIXME: the same thing for now
        persistAndSync(entry, callback);
    }

    @Override
    public void persistAndSync(final ReplicatedLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
        LOG.trace("{}: persisting index {} term {}", logId, entry.index(), entry.term());
        journal.writer().append(entry);
        journal.writer().flush();
        callback.accept(entry);
    }
}
