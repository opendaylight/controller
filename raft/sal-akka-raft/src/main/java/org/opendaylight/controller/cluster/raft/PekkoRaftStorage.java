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
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.JournalWriterTask.JournalAction;
import org.opendaylight.controller.cluster.raft.JournalWriterTask.JournalAppendEntry;
import org.opendaylight.controller.cluster.raft.JournalWriterTask.JournalApplyTo;
import org.opendaylight.controller.cluster.raft.JournalWriterTask.JournalReplayFrom;
import org.opendaylight.controller.cluster.raft.JournalWriterTask.JournalReset;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EntryJournalV1;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link EnabledRaftStorage} backed by Pekko Persistence of an {@link RaftActor}.
 */
// FIXME: remove this class once we have both Snapshots and Entries stored in files
final class PekkoRaftStorage extends EnabledRaftStorage {
    private static final Logger LOG = LoggerFactory.getLogger(PekkoRaftStorage.class);

    private final BlockingQueue<JournalAction<?>> queue = new ArrayBlockingQueue<>(2048);
    private final boolean mapped;

    private JournalWriterTask task;

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
        final var journal = new EntryJournalV1(memberId, directory, compression, mapped);
        LOG.info("{}: journal open: applyTo={}", memberId, journal.applyTo());
        task = new JournalWriterTask(actor, journal, queue);
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
        // FIXME: terminate task
        task.journal.close();
        task = null;
        LOG.info("{}: journal closed", memberId);
    }

    @Override
    public long persistEntry(final ReplicatedLogEntry entry) throws IOException {
        final var future = new CompletableFuture<Long>();
        startPersistEntry(entry, (failure, success) -> {
            if (failure != null) {
                future.completeExceptionally(failure);
            } else {
                future.complete(success);
            }
        });

        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new IOException("Failed to persist entry " + entry, e);
        }
    }

    @Override
    @NonNullByDefault
    public void startPersistEntry(final ReplicatedLogEntry entry, final RaftCallback<Long> callback) {
        requireNonNull(callback);
        queue.add(new JournalAppendEntry(entry, callback));
    }

    @Override
    public void deleteEntries(final long fromIndex) {
        queue.add(new JournalReset(fromIndex));
    }

    @Override
    public void markLastApplied(final long lastApplied) {
        queue.add(new JournalApplyTo(lastApplied));
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        // FIXME: not quite
        queue.add(new JournalReplayFrom(sequenceNumber));
    }

    @Override
    public long lastSequenceNumber() {
        return journal.lastIndex();
    }
}
