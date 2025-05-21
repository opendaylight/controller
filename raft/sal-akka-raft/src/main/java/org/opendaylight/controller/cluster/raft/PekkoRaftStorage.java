/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.apache.pekko.persistence.DeleteMessagesSuccess;
import org.apache.pekko.persistence.DeleteSnapshotsSuccess;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.StateMachineCommand;
import org.opendaylight.raft.journal.EntryReader;
import org.opendaylight.raft.journal.FromByteBufMapper;
import org.opendaylight.raft.journal.SegmentedRaftJournal;
import org.opendaylight.raft.journal.StorageLevel;
import org.opendaylight.raft.journal.ToByteBufMapper;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.WritableObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link EnabledRaftStorage} backed by Pekko Persistence of an {@link RaftActor}.
 */
// FIXME: remove this class once we have both Snapshots and Entries stored in files
final class PekkoRaftStorage extends EnabledRaftStorage {
    @NonNullByDefault
    private static final class PekkoEntryLoader extends AbstractRegistration implements EntryLoader {
        private static final FromByteBufMapper<LoadedEntry> FROMBUF = (index, bytes) -> {
            try (var in = new ByteBufInputStream(bytes)) {
                final var header = in.readByte();
                final var type = header & TYPE_MASK;
                switch (type) {
                    case TYPE_ENTRY -> {
                        final var hdr = WritableObjects.readLongHeader(in);
                        final var entryIndex = WritableObjects.readFirstLong(in, hdr);
                        final var entryTerm = WritableObjects.readSecondLong(in, hdr);

                        final StateMachineCommand command;
                        try (var ois = new ObjectInputStream(in)) {
                            try {
                                command = (StateMachineCommand) ois.readObject();
                            } catch (ClassNotFoundException e) {
                                throw new IOException("Cannot read command", e);
                            }
                        }
                        return new LoadedLogEntry(index, entryIndex, entryTerm, command);
                    }
                    case TYPE_LAST_APPLIED -> {
                        return new LoadedLastApplied(index, WritableObjects.readLongBody(in, header));
                    }
                    default -> {
                        throw new IllegalArgumentException("Unhandled type " + type);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        private final EntryReader reader;

        PekkoEntryLoader(final EntryReader reader) {
            this.reader = requireNonNull(reader);
        }

        @Override
        public @Nullable LoadedEntry loadNext() {
            return reader.tryNext(FROMBUF);
        }

        @Override
        protected void removeRegistration() {
            reader.close();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PekkoRaftStorage.class);

    private static final String JOURNAL_NAME = "entries-v1";

    // 256KiB, which leads to reasonable buffers
    private static final int INLINE_ENTRY_SIZE = 256 * 1024;
    // We need to be able to store two entries and a descriptor
    //  private static final long MIN_TARGET_SIZE = INLINE_ENTRY_SIZE * 2 + SegmentDescriptor.BYTES;

    // Allocation of two bits, indicating the type of stored entry. These are top two bits, so as to allow combined use
    // with WritableObjects.writeLong(). This allows us to read the header byte and determine the dispatch accordingly.
    private static final byte TYPE_ENTRY         = (byte) 0x00;
    private static final byte TYPE_ENTRY_FILE    = (byte) 0x40;
    private static final byte TYPE_LAST_APPLIED  = (byte) 0x80;
    private static final byte TYPE_MASK          = (byte) 0xC0;

    @NonNullByDefault
    private static final @NonNull ToByteBufMapper<LogEntry> TOBUF_ENTRY = (obj, buf) -> {
        try (var out = new ByteBufOutputStream(buf)) {
            out.writeByte(TYPE_ENTRY);
            WritableObjects.writeLongs(out, obj.index(), obj.term());
            try (var oos = new ObjectOutputStream(out)) {
                oos.writeObject(obj.command().toSerialForm());
            }
        } catch (IndexOutOfBoundsException e) {
            final var eof = new EOFException();
            eof.initCause(e);
            throw eof;
        }
    };

    private static final @NonNull ToByteBufMapper<Long> TOBUF_LAST_APPLIED = (obj, buf) -> {
        try (var out = new ByteBufOutputStream(buf)) {
            WritableObjects.writeLong(out, obj, TYPE_LAST_APPLIED);
        }
    };

    private final @NonNull StorageLevel journalLevel;
    private final int journalSegmentSize;

    private SegmentedRaftJournal journal;

    @NonNullByDefault
    PekkoRaftStorage(final RaftActor actor, final Path directory, final CompressionType compression,
            final Configuration streamConfig) {
        super(actor.memberId(), actor, directory, compression, streamConfig);
        journalSegmentSize = 128 * 1024 * 1024;
        journalLevel = StorageLevel.MAPPED;
    }

    // TODO: at least
    //   - creates the directory if not present
    //   - creates a 'lock' file and java.nio.channels.FileChannel.tryLock()s it
    //   - scans the directory to:
    //     - clean up any temporary files
    //     - determine nextSequence

    @Override
    protected void postStart() throws IOException {
        journal = SegmentedRaftJournal.builder()
            .withDirectory(directory)
            .withName(JOURNAL_NAME)
            .withMaxEntrySize(INLINE_ENTRY_SIZE)
            .withMaxSegmentSize(journalSegmentSize)
            .withStorageLevel(journalLevel)
            .build();
        LOG.info("{}: journal open: firstIndex={} lastIndex={}", memberId, journal.firstIndex(), journal.lastIndex());
    }

    // FIXME:  and more: more things:
    //   - terminates any incomplete operations, reporting a CancellationException to them
    //   - unlocks the file
    // - stop() that:
    // - a lava.lang.ref.Clearner which does the same as stop()
    // For scalability this locking should really be done on top-level stateDir so we have one file open for all shards,
    // not one file per shard.

    @Override
    protected void preStop() {
        journal.close();
        journal = null;
        LOG.info("{}: journal closed", memberId);
    }

    @Override
    public EntryLoader openLoader() {
        return new PekkoEntryLoader(journal.openReader(-1));
    }

    @Override
    public void persistEntry(final ReplicatedLogEntry entry) throws IOException {
        journal.writer().append(TOBUF_ENTRY, entry);
    }

    @Override
    public void startPersistEntry(final ReplicatedLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
        // FIXME: asynchronous
        try {
            persistEntry(entry);
        } catch (IOException e) {
            LOG.error("{}: to persist entry", memberId, e);
            throw new UncheckedIOException(e);
        }
        executeInSelf.executeInSelf(() -> callback.accept(entry));
    }

    @Override
    public void deleteEntries(final long fromIndex) {
        journal.writer().reset(fromIndex);
    }

    @Override
    public void markLastApplied(final long lastApplied) {
        try {
            journal.writer().append(TOBUF_LAST_APPLIED, lastApplied);
        } catch (IOException e) {
            LOG.error("{}: failed to mark last applied index", memberId, e);
            throw new UncheckedIOException(e);
        }
        LOG.debug("{}: update commit-index to {}", memberId, lastApplied);
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        journal.writer().commit(sequenceNumber);
        journal.compact(sequenceNumber);
    }

    @Override
    public long lastSequenceNumber() {
        return journal.lastIndex();
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        return response instanceof DeleteMessagesSuccess;
    }

    @Override
    public boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
        return response instanceof DeleteSnapshotsSuccess;
    }
}
