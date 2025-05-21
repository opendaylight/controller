/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.LongConsumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EntryLoader;
import org.opendaylight.raft.journal.SegmentedRaftJournal;
import org.opendaylight.raft.journal.StorageLevel;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link EnabledRaftStorage} backed by Pekko Persistence of an {@link RaftActor}.
 */
// FIXME: remove this class once we have both Snapshots and Entries stored in files
final class PekkoRaftStorage extends EnabledRaftStorage {

    sealed interface JournalAction {

    }

    private record JournalAppendEntry() implements JournalAction {

    }

    private record JournalDeleteEntries() implements JournalAction {

    }

    private record JournalMarkLastApplied() implements JournalAction {

    }

    private record JournalReset() implements JournalAction {

    }


    private static final Logger LOG = LoggerFactory.getLogger(PekkoRaftStorage.class);

    private final BlockingQueue<JournalAction> queue = new ArrayBlockingQueue<>(2048);
    private final boolean mapped;

    // Journal: individual entries
    private SegmentedRaftJournal journal;

    @NonNullByDefault
    PekkoRaftStorage(final RaftActor actor, final Path directory, final CompressionType compression,
            final Configuration streamConfig, final boolean mapped) {
        super(actor.memberId(), actor, directory, compression, streamConfig);
        this.mapped = mapped;
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
            .withName("journal-v1")
            .withMaxEntrySize(JOURNAL_INLINE_ENTRY_SIZE)
            .withMaxSegmentSize(JOURNAL_SEGMENT_SIZE)
            .withStorageLevel(mapped ? StorageLevel.MAPPED : StorageLevel.DISK)
            .build();

        LOG.info("{}: journal open: firstIndex={} lastIndex={}", memberId, journal.firstIndex(), journal.lastIndex());

        new JournalWriterTask(queue);

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
        return new JournalEntryLoader(directory, journal.openReader(-1));
    }

    @Override
    public long persistEntry(final ReplicatedLogEntry entry) throws IOException {
        final var writer = journal.writer();
        final var mapper = new LogEntryMapper(writer.nextIndex());
        writer.append(mapper, entry);

//        final var file = mapper.file;

        throw new UnsupportedOperationException();
    }

    @Override
    public void startPersistEntry(final ReplicatedLogEntry entry, final LongConsumer callback) {
        requireNonNull(callback);
        // FIXME: asynchronous
        final long journalIndex;
        try {
            journalIndex = persistEntry(entry);
        } catch (IOException e) {
            LOG.error("{}: to persist entry", memberId, e);
            throw new UncheckedIOException(e);
        }
        actor.executeInSelf(() -> callback.accept(journalIndex));
    }

    @Override
    public void deleteEntries(final long fromIndex) {
        journal.writer().reset(fromIndex);
    }

    @Override
    public void markLastApplied(final long lastApplied) {
        try {
            journal.writer().append(PekkoRaftStorage::writeLastApplied, lastApplied);
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

//    private static boolean writeLastApplied(final Long lastApplied, final ByteBuf buf) throws IOException {
//        try (var out = new ByteBufOutputStream(buf)) {
//            WritableObjects.writeLong(out, lastApplied, TYPE_LAST_APPLIED & 0xF0);
//        } catch (IndexOutOfBoundsException e) {
//            LOG.trace("Not enough buffer space", e);
//            return false;
//        }
//        return true;
//    }
}
